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

package com.android.settings.cloudconnector;

import android.cloudconnector.CloudConnectorEventListenerImpl;
import android.cloudconnector.CloudConnectorHandler;
import android.content.Context;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.widget.SwitchWidgetController;

import java.util.concurrent.atomic.AtomicBoolean;

public class CCEnabler implements SwitchWidgetController.OnSwitchChangeListener, CloudConnectorEventListenerImpl {

    // Variables
    private final SwitchWidgetController mSwitchWidget;

    private final CloudConnectorHandler mCCManager;

    private final MetricsFeatureProvider mMetricsFeatureProvider;

    private Context mContext;

    private AtomicBoolean mConnected = new AtomicBoolean(false);
    private boolean mStateMachineEvent;
    private boolean mListeningToOnSwitchChange = false;

    public CCEnabler(Context context, SwitchWidgetController switchWidget,
        MetricsFeatureProvider metricsFeatureProvider) {
        mContext = context;
        mSwitchWidget = switchWidget;
        mSwitchWidget.setListener(this);
        mMetricsFeatureProvider = metricsFeatureProvider;
        mCCManager = (CloudConnectorHandler) mContext.getSystemService(Context.CLOUD_CONNECTOR_SERVICE);

        setupSwitchController();
    }

    public void setupSwitchController() {
        final boolean state = mCCManager.isConnected();
        handleCCStateChanged(state);
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
        mCCManager.registerEventListener(this);
        if (!mListeningToOnSwitchChange) {
            mSwitchWidget.startListening();
            mListeningToOnSwitchChange = true;
        }
    }

    public void pause() {
        mCCManager.unregisterEventListener(this);
        if (mListeningToOnSwitchChange) {
            mSwitchWidget.stopListening();
            mListeningToOnSwitchChange = false;
        }
    }

    private void handleCCStateChanged(boolean state) {
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
            mMetricsFeatureProvider.action(mContext, MetricsEvent.ACTION_CC_ON);
            mCCManager.connect();
        } else {
            // Log if user was connected at the time of switching off.
            mMetricsFeatureProvider.action(mContext, MetricsEvent.ACTION_CC_OFF,
                    mConnected.get());
            mCCManager.disconnect();
        }

        return true;
    }

    @Override
    public void connected() {
        mConnected.set(true);
        handleCCStateChanged(true);
    }

    @Override
    public void disconnected() {
        mConnected.set(false);
        handleCCStateChanged(false);
    }

    @Override
    public void connectionError(String errorMessage) {
        mConnected.set(false);
        handleCCStateChanged(false);
    }

    @Override
    public void sendDataPointsSuccess() {
        // Do nothing.
    }

    @Override
    public void sendDataPointsError(String errorMessage) {
        // Do nothing.
    }
}
