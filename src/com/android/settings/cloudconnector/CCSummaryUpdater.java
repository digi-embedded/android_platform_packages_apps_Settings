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

import android.content.Context;
import android.support.annotation.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.widget.SummaryUpdater;

import com.digi.android.cloudconnector.CloudConnectorManager;
import com.digi.android.cloudconnector.ICloudConnectorEventListener;

/**
 * Helper class that listeners to Cloud Connector callback and notify client
 * when there is update in Cloud Connector summary info.
 */
public final class CCSummaryUpdater extends SummaryUpdater implements ICloudConnectorEventListener {

    // Variables
    private final CloudConnectorManager mCCManager;

    public CCSummaryUpdater(Context context, OnSummaryChangeListener listener) {
        this(context, listener, new CloudConnectorManager(context));
    }

    @VisibleForTesting
    public CCSummaryUpdater(Context context, OnSummaryChangeListener listener,
        CloudConnectorManager ccManager) {
        super(context, listener);
        mCCManager = ccManager;
    }

    @Override
    public void register(boolean register) {
        if (register)
            mCCManager.registerEventListener(this);
        else
            mCCManager.unregisterEventListener(this);
    }

    @Override
    public String getSummary() {
        if (!mCCManager.isConnected())
            return mContext.getString(R.string.switch_off_text);
        return mContext.getString(R.string.switch_on_text);
    }

    @Override
    public void connected() {
        notifyChangeIfNeeded();
    }

    @Override
    public void disconnected() {
        notifyChangeIfNeeded();
    }

    @Override
    public void connectionError(String errorMessage) {
        notifyChangeIfNeeded();
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
