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
import android.support.annotation.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.widget.SummaryUpdater;

import com.digi.android.ethernet.EthernetManager;

/**
 * Helper class that listeners to ethernet callback and notify client when there is update in
 * ethernet summary info.
 */
public final class EthernetSummaryUpdater extends SummaryUpdater {

    // Variables
    private final EthernetManager mEthernetManager;

    private final BroadcastReceiver mReceiver;

    private static final IntentFilter INTENT_FILTER;
    static {
        INTENT_FILTER = new IntentFilter();
        INTENT_FILTER.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
    }

    public EthernetSummaryUpdater(Context context, OnSummaryChangeListener listener) {
        this(context, listener, new EthernetManager(context));
    }

    @VisibleForTesting
    public EthernetSummaryUpdater(Context context, OnSummaryChangeListener listener,
        EthernetManager ethernetManager) {
        super(context, listener);
        mEthernetManager = ethernetManager;
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                notifyChangeIfNeeded();
            }
        };
    }

    @Override
    public void register(boolean register) {
        if (register)
            mContext.registerReceiver(mReceiver, INTENT_FILTER);
        else
            mContext.unregisterReceiver(mReceiver);
    }

    @Override
    public String getSummary() {
        if (!mEthernetManager.isEnabled())
            return mContext.getString(R.string.switch_off_text);
        return mContext.getString(R.string.switch_on_text);
    }
}
