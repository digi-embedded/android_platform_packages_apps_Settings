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

import com.android.settings.R;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Toast;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

import com.digi.android.ethernet.EthernetManager;
import com.digi.android.ethernet.EthernetConfiguration;
import com.digi.android.ethernet.EthernetConnectionMode;

/**
 * Dialog to configure the Ethernet settings.
 */
public class EthernetConfigDialog extends AlertDialog {

	// Constants.
	private final String TAG = "EthernetConfigDialog";

	private static final String IP_FORMAT = "(?:(?:[0-1]?[0-9])?[0-9]|2[0-4][0-9]|25[0-5])"
			+ "\\.(?:(?:[0-1]?[0-9])?[0-9]|2[0-4][0-9]|25[0-5])"
			+ "\\.(?:(?:[0-1]?[0-9])?[0-9]|2[0-4][0-9]|25[0-5])"
			+ "\\.(?:(?:[0-1]?[0-9])?[0-9]|2[0-4][0-9]|25[0-5])";

	private static final String ERROR_NO_INTERFACES = "Could not find any Ethernet interface";
	private static final String ERROR_IP_VALUE = "IP address value is invalid. Interface will not be reconfigured";
	private static final String ERROR_GATEWAY_VALUE = "Gateway address value is invalid. Interface will not be reconfigured";
	private static final String ERROR_NETMASK_VALUE = "Netmask value is invalid. Interface will not be reconfigured";
	private static final String ERROR_DNS1_VALUE = "DNS address 1 value is invalid. Interface will not be reconfigured.";
	private static final String ERROR_DNS2_VALUE = "IP address value is invalid. Interface will not be reconfigured";

	// Variables.
	private View view;
	private LinearLayout enterpriseWrapper;
	private EditText dev;
	private RadioGroup conType;
	private EditText ipAddr;
	private EditText dns1;
	private EditText dns2;
	private EditText gateway;
	private EditText netMask;

	private EthernetManager ethManager;

	private Context context;

	private EthernetConfigDialog dialog;

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			Toast.makeText(context, msg.obj.toString(), Toast.LENGTH_LONG).show();
		}
	};

	/**
	 * Class constructor. Creates a new {@code EthernetConfigDialog}.
	 * 
	 * @param context Application context.
	 * @param manager Ethernet manager.
	 */
	public EthernetConfigDialog(Context context, EthernetManager ethManager) {
		super(context);
		this.context = context;
		this.ethManager = ethManager;
		this.dialog = this;
		initDialog();
	}

	/**
	 * Initializes the configuration dialog.
	 */
	public void initDialog() {
		this.setTitle(R.string.eth_config_title);
		this.setView(view = getLayoutInflater().inflate(R.layout.eth_configure, null));

		// Hide the keyboard.
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

		dev = (EditText) view.findViewById(R.id.eth_dev);
		conType = (RadioGroup) view.findViewById(R.id.con_type);
		enterpriseWrapper = (LinearLayout) view.findViewById(R.id.enterprise_wrapper);
		ipAddr = (EditText) view.findViewById(R.id.ipaddr_edit);
		netMask = (EditText) view.findViewById(R.id.netmask_edit);
		dns1 = (EditText) view.findViewById(R.id.eth_dns1_edit);
		dns2 = (EditText) view.findViewById(R.id.eth_dns2_edit);
		gateway = (EditText) view.findViewById(R.id.eth_gw_edit);

		conType.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				if (checkedId == R.id.dhcp_radio) {
					enterpriseWrapper.setVisibility(View.GONE);
				} else if (checkedId == R.id.manual_radio) {
					enterpriseWrapper.setVisibility(View.VISIBLE);
				}
			}
		});

		this.setInverseBackgroundForced(true);
		this.setButton(BUTTON_POSITIVE, context.getText(R.string.menu_save), (OnClickListener)null);
		this.setButton(BUTTON_NEGATIVE, context.getText(R.string.menu_cancel), new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		this.setOnShowListener(new OnShowListener() {
			@Override
			public void onShow(DialogInterface d) {
				Button b = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
				b.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						handleSaveConfig();
					}
				});
			}
		});
	}

	@Override
	protected void onStart() {
		super.onStart();
		// Update the fields of the dialog.
		String iface = ethManager.getInterfaceName();
		if (iface == null) {
			Log.e(TAG, ERROR_NO_INTERFACES);
			Toast.makeText(context, ERROR_NO_INTERFACES, Toast.LENGTH_LONG).show();
			dismiss();
			return;
		}
		dev.setText(iface);
		if (ethManager.getConnectionMode() == EthernetConnectionMode.DHCP) {
			conType.check(R.id.dhcp_radio);
			enterpriseWrapper.setVisibility(View.GONE);
		} else {
			conType.check(R.id.manual_radio);
			enterpriseWrapper.setVisibility(View.VISIBLE);
			ipAddr.setText(ethManager.getIp() != null ? ethManager.getIp().getHostAddress() : "");
			dns1.setText(ethManager.getDns1() != null ? ethManager.getDns1().getHostAddress() : "");
			dns2.setText(ethManager.getDns2() != null ? ethManager.getDns2().getHostAddress() : "");
			netMask.setText(ethManager.getNetmask() != null ? ethManager.getNetmask().getHostAddress() : "");
			gateway.setText(ethManager.getGateway() != null ? ethManager.getGateway().getHostAddress() : "");
		}
	}

	/**
	 * Handles what happens when the dialog's Save button is pressed.
	 */
	private void handleSaveConfig() {
		// Show a dialog and update the network information.
		final ProgressDialog pd = new ProgressDialog(context);
		pd.setTitle(context.getString(R.string.eth_apply_dialog_title));
		pd.setMessage(context.getString(R.string.eth_turn_dialog_message));
		pd.setCancelable(false);
		pd.setIndeterminate(true);
		pd.show();

		new Thread(new Runnable() {
			@Override
			public void run() {
				EthernetConfiguration info = new EthernetConfiguration();
				info.setInterfaceName(dev.getText().toString());
				Log.v(TAG, "Config device for " + dev.getText().toString());

				try {
					if (conType.getCheckedRadioButtonId() == R.id.dhcp_radio) {
						Log.v(TAG, "Config device for DHCP ");
						info.setConnectionMode(EthernetConnectionMode.DHCP);
					} else {
						Log.v(TAG, "Config device for static, IP = " + ipAddr.getText().toString() + ", mask = " + 
								netMask.getText().toString() + ", DNS1 = " + dns1.getText().toString() + ", DNS2 = " +
								dns2.getText().toString() + ", gateway = " + gateway.getText().toString());
						if (checkStaticIpValues()) {
							info.setConnectionMode(EthernetConnectionMode.STATIC);
							info.setIpAddress(InetAddress.getByName(ipAddr.getText().toString()));
							info.setNetMask(InetAddress.getByName(netMask.getText().toString()));
							info.setDns1Addr(InetAddress.getByName(dns1.getText().toString()));
							if (!dns2.getText().toString().isEmpty())
								info.setDns2Addr(InetAddress.getByName(dns2.getText().toString()));
							info.setGateway(InetAddress.getByName(gateway.getText().toString()));
						} else {
							Log.e(TAG, "Config device for static failed becuase of wrong formated data");
							pd.dismiss();
							return;
						}
					}

					ethManager.configureInterface(info);

					// Send a broadcast to the Ethernet settings activity to update new network settings.
					context.sendBroadcast(new Intent(EthernetSettings.UPDATE_NETWORK_DATA));

					// Close the dialogs.
					pd.dismiss();
					dialog.dismiss();
				} catch (UnknownHostException e) {
					e.printStackTrace();
					Message msg = new Message();
					msg.obj = "Error setting the configuration: " + e.getMessage();
					handler.sendMessage(msg);
					pd.dismiss();
				}
			}
		}).start();
	}

	/**
	 * Verifies that the values of the fields have the IP format.
	 * 
	 * @return {@code true} if the values are correct, {@code false} otherwise.
	 */
	private boolean checkStaticIpValues() {
		Pattern pat = Pattern.compile(IP_FORMAT);
		Message msg = new Message();

		if (!pat.matcher(ipAddr.getText().toString()).matches()) {
			msg.obj = ERROR_IP_VALUE;
			handler.sendMessage(msg);
			return false;
		}

		if (!pat.matcher(netMask.getText().toString()).matches()) {
			msg.obj = ERROR_NETMASK_VALUE;
			handler.sendMessage(msg);
			return false;
		}

		if (!pat.matcher(dns1.getText().toString()).matches()) {
			msg.obj = ERROR_DNS1_VALUE;
			handler.sendMessage(msg);
			return false;
		}

		if (!dns2.getText().toString().isEmpty()) {
			if (!pat.matcher(dns2.getText().toString()).matches()) {
				msg.obj = ERROR_DNS2_VALUE;
				handler.sendMessage(msg);
				return false;
			}
		}

		if (!pat.matcher(gateway.getText().toString()).matches()) {
			msg.obj = ERROR_GATEWAY_VALUE;
			handler.sendMessage(msg);
			return false;
		}

		return true;
	}
}
