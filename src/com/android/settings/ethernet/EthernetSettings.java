/*
 * Copyright 2018, Digi International Inc.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.android.settings.ethernet;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.widget.SwitchBar;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo.State;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Switch;
import android.widget.Toast;

import com.digi.android.ethernet.EthernetManager;
import com.digi.android.ethernet.EthernetConnectionMode;

/**
 * Main fragment of the Ethernet settings preference page.
 */
public class EthernetSettings extends SettingsPreferenceFragment
		implements SwitchBar.OnSwitchChangeListener {

	// Constants.
	public static final String UPDATE_NETWORK_DATA = "com.digi.android.network.update";

	private static final String TAG = "EthernetSetting";

	private static final String PREF_CONFIG = "eth_config";
	private static final String PREF_CON_TYPE = "con_type";
	private static final String PREF_IP = "ip_address";
	private static final String PREF_NETMASK = "netmask_address";
	private static final String PREF_DNS1 = "dns1_address";
	private static final String PREF_DNS2 = "dns2_address";
	private static final String PREF_GATEWAY = "gateway_address";
	private static final String PREF_MAC = "mac_address";

	private static final String ERROR_NO_INTERFACES = "Could not find any Ethernet interface";

	private static final int MAX_TIMEOUT = 10000;

	private final Object lock = new Object();

	// Variables.
	private EthernetManager ethManager;
	private ConnectivityManager connManager;
	private EthernetConfigDialog configDialog;
	private Preference configPref;
	private Context context;

	private SwitchBar switchBar;
	private Switch switchEth;

	private String iface;

	private boolean notified = false;

	private Handler handler = new Handler();

	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(UPDATE_NETWORK_DATA))
				handleUpdateDataEvent();
			else if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION))
				handleConnectivityEvent();
		}
	};

	@Override
	public int getMetricsCategory() {
		return MetricsEvent.ETHERNET;
	}

	@Override
	public boolean onPreferenceTreeClick(Preference preference) {
		super.onPreferenceTreeClick(preference);

		if (preference == configPref)
			configDialog.show();
		return false;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		switchBar = ((SettingsActivity) getActivity()).getSwitchBar();
		switchEth = switchBar.getSwitch();
		switchBar.show();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		switchBar.hide();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.ethernet_settings);

		context = getActivity();

		ethManager = new EthernetManager(context);
		connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

		iface = ethManager.getInterfaceName();
		if (iface == null) {
			Log.e(TAG, ERROR_NO_INTERFACES);
			Toast.makeText(context, ERROR_NO_INTERFACES, Toast.LENGTH_LONG).show();
			return;
		}

		final PreferenceScreen preferenceScreen = getPreferenceScreen();
		configPref = preferenceScreen.findPreference(PREF_CONFIG);

		configDialog = new EthernetConfigDialog(context, ethManager);
	}

	@Override
	public void onResume() {
		super.onResume();
		// Register broadcast receiver.
		IntentFilter filter = new IntentFilter(UPDATE_NETWORK_DATA);
		filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		context.registerReceiver(receiver, filter);
		// Update switch.
		switchEth.setChecked(ethManager.isEnabled() ? true : false);
		switchBar.addOnSwitchChangeListener(this);
		// Update Ethernet configuration.
		new Thread(new Runnable() {
			@Override
			public void run() {
				updateInfo();
			}
		}).start();
	}

	@Override
	public void onPause() {
		super.onPause();
		// Unregister broadcast receiver.
		context.unregisterReceiver(receiver);
		// Unregister listener.
		switchBar.removeOnSwitchChangeListener(this);
	}

	@Override
	public void onSwitchChanged(Switch switchView, final boolean isChecked) {
		// Create and show a progress dialog.
		final ProgressDialog dialog = new ProgressDialog(context);
		dialog.setTitle(context.getString(R.string.eth_turn_dialog_title));
		dialog.setMessage(context.getString(R.string.eth_turn_dialog_message));
		dialog.setCancelable(false);
		dialog.setIndeterminate(true);
		dialog.show();

		// Change the state of the interface and close the dialog.
		new Thread (new Runnable() {
			@Override
			public void run() {
				notified = false;
				ethManager.setEnabled(isChecked);
				// Wait until a connectivity change event is received or the 
				// timeout expires.
				try {
					synchronized (lock) {
						lock.wait(MAX_TIMEOUT);
					}
				} catch (InterruptedException e) { }
				// If the Ethernet cable is not connected we didn't receive any
				// event, so update the info.
				if (!notified)
					updateInfo();
				dialog.dismiss();
			}
		}).start();
	}

	/**
	 * Updates the preferences page with the stored Ethernet settings.
	 */
	private void updateInfo() {
		final boolean isEnabled = ethManager.isEnabled();
		if (isEnabled) {
			final EthernetConnectionMode type = ethManager.getConnectionMode();
			final String ip = ethManager.getIp() != null ? 
					ethManager.getIp().getHostAddress() : null;
			final String netmask = ethManager.getNetmask() != null ? 
					ethManager.getNetmask().getHostAddress() : null;
			final String dns1 = ethManager.getDns1() != null ? 
					ethManager.getDns1().getHostAddress() : null;
			final String dns2 = ethManager.getDns2() != null ? 
					ethManager.getDns2().getHostAddress() : null;
			final String gateway = ethManager.getGateway() != null ? 
					ethManager.getGateway().getHostAddress() : null;
			final String mac = ethManager.getMacAddress();

			handler.post(new Runnable() {
				@Override
				public void run() {
					findPreference(PREF_CONFIG).setEnabled(true);

					findPreference(PREF_CON_TYPE).setSummary(type != null ?
							type.getDescription() : context.getString(R.string.status_unavailable));
					findPreference(PREF_IP).setSummary(!TextUtils.isEmpty(ip) ?
							ip : context.getString(R.string.status_unavailable));
					findPreference(PREF_NETMASK).setSummary(!TextUtils.isEmpty(netmask) ?
							netmask : context.getString(R.string.status_unavailable));
					findPreference(PREF_DNS1).setSummary(!TextUtils.isEmpty(dns1) ?
							dns1 : context.getString(R.string.status_unavailable));
					findPreference(PREF_DNS2).setSummary(!TextUtils.isEmpty(dns2) ?
							dns2 : context.getString(R.string.status_unavailable));
					findPreference(PREF_GATEWAY).setSummary(!TextUtils.isEmpty(gateway) ?
							gateway : context.getString(R.string.status_unavailable));
					findPreference(PREF_MAC).setSummary(!TextUtils.isEmpty(mac) ?
							mac : context.getString(R.string.status_unavailable));
				}
			});
		} else {
			handler.post(new Runnable() {
				@Override
				public void run() {
					findPreference(PREF_CONFIG).setEnabled(false);

					findPreference(PREF_CON_TYPE).setSummary(context.getString(R.string.status_unavailable));
					findPreference(PREF_IP).setSummary(context.getString(R.string.status_unavailable));
					findPreference(PREF_NETMASK).setSummary(context.getString(R.string.status_unavailable));
					findPreference(PREF_DNS1).setSummary(context.getString(R.string.status_unavailable));
					findPreference(PREF_DNS2).setSummary(context.getString(R.string.status_unavailable));
					findPreference(PREF_GATEWAY).setSummary(context.getString(R.string.status_unavailable));
					findPreference(PREF_MAC).setSummary(context.getString(R.string.status_unavailable));
				}
			});
		}

		// Ensure the switch is accordingly configured with the interface state.
		if (isEnabled != switchEth.isChecked()) {
			handler.post(new Runnable() {
				@Override
				public void run() {
					switchBar.removeOnSwitchChangeListener(EthernetSettings.this);
					switchEth.setChecked(isEnabled);
					switchBar.addOnSwitchChangeListener(EthernetSettings.this);
				}
			});
		}
	}

	/**
	 * Handles what happens when an Update Network Data event is received.
	 */
	private void handleUpdateDataEvent() {
		Log.i(TAG, "Update network data");
		// Show a dialog and update the network information.
		final ProgressDialog dialog = new ProgressDialog(context);
		dialog.setTitle(context.getString(R.string.eth_apply_dialog_title));
		dialog.setMessage(context.getString(R.string.eth_turn_dialog_message));
		dialog.setCancelable(false);
		dialog.setIndeterminate(true);
		dialog.show();

		new Thread(new Runnable() {
			@Override
			public void run() {
				updateInfo();
				dialog.dismiss();
			}
		}).start();
	}

	/**
	 * Handles what happens when a connectivity change event is received.
	 */
	private void handleConnectivityEvent() {
		State state = connManager.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET).getState();
		Log.i(TAG, "Connectivity changed: " + state.toString());
		new Thread(new Runnable() {
			@Override
			public void run() {
				updateInfo();
				synchronized (lock) {
					notified = true;
					lock.notify();
				}
			}
		}).start();
	}
}
