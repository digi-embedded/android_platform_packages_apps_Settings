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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.EthernetManager;
import android.provider.Settings;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.widget.SwitchWidgetController;

import java.util.concurrent.atomic.AtomicBoolean;

public class EthernetEnabler implements SwitchWidgetController.OnSwitchChangeListener {

    // Variables
    private final SwitchWidgetController mSwitchWidget;

    private final EthernetManager mEthernetManager;

    private final MetricsFeatureProvider mMetricsFeatureProvider;

    private Context mContext;

    private AtomicBoolean mConnected = new AtomicBoolean(false);
    private boolean mStateMachineEvent;
    private boolean mListeningToOnSwitchChange = false;

    private final IntentFilter mIntentFilter;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
                mConnected.set(mEthernetManager.isEnabled(mEthernetManager.getInterfaceName()));
                handleEthernetStateChanged(mEthernetManager.isEnabled(mEthernetManager.getInterfaceName()));
            }
        }
    };

    public EthernetEnabler(Context context, SwitchWidgetController switchWidget,
        MetricsFeatureProvider metricsFeatureProvider) {
        mContext = context;
        mSwitchWidget = switchWidget;
        mSwitchWidget.setListener(this);
        mMetricsFeatureProvider = metricsFeatureProvider;
        mEthernetManager = (EthernetManager) mContext.getSystemService(Context.ETHERNET_SERVICE);

        mIntentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);

        setupSwitchController();
    }

    public void setupSwitchController() {
        final boolean state = mEthernetManager.isEnabled(mEthernetManager.getInterfaceName());
        handleEthernetStateChanged(state);
        if (!mListeningToOnSwitchChange) {
            mSwitchWidget.startListening();
            mListeningToOnSwitchChange = true;
        }
        mSwitchWidget.setupView();
    }

    public void teardownSwitchController() {
        if (mListeningToOnSwitchChange) {
            mSwitchWidget.stopListening();
            mListeningToOnSwitchChange = false;
        }
        mSwitchWidget.teardownView();
    }

    public void resume(Context context) {
        mContext = context;
        // Let the receiver update UI
        mContext.registerReceiver(mReceiver, mIntentFilter);
        if (!mListeningToOnSwitchChange) {
            mSwitchWidget.startListening();
            mListeningToOnSwitchChange = true;
        }
    }

    public void pause() {
        mContext.unregisterReceiver(mReceiver);
        if (mListeningToOnSwitchChange) {
            mSwitchWidget.stopListening();
            mListeningToOnSwitchChange = false;
        }
    }

    private void handleEthernetStateChanged(boolean state) {
        // Clear any previous state
        mSwitchWidget.setDisabledByAdmin(null);

        setSwitchBarChecked(state);
        mSwitchWidget.setEnabled(true);
    }

    private void setSwitchBarChecked(boolean checked) {
        mStateMachineEvent = true;
        mSwitchWidget.setChecked(checked);
        mStateMachineEvent = false;
    }

    @Override
    public boolean onSwitchToggled(boolean isChecked) {
        // Do nothing if called as a result of a state machine event
        if (mStateMachineEvent) {
            return true;
        }

        mSwitchWidget.setEnabled(false);

        if (isChecked) {
            mMetricsFeatureProvider.action(mContext, MetricsEvent.ACTION_ETHERNET_ON);
        } else {
            // Log if user was connected at the time of switching off.
            mMetricsFeatureProvider.action(mContext, MetricsEvent.ACTION_ETHERNET_OFF,
                    mConnected.get());
        }
        mEthernetManager.setEnabled(mEthernetManager.getInterfaceName(), isChecked);

        return true;
    }
}
