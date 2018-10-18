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

import android.content.Context;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.core.PreferenceController;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.widget.SummaryUpdater;
import com.android.settings.widget.MasterSwitchPreference;
import com.android.settings.widget.MasterSwitchController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

public class EthernetMasterSwitchPreferenceController extends PreferenceController
        implements SummaryUpdater.OnSummaryChangeListener,
        LifecycleObserver, OnResume, OnPause, OnStart, OnStop {

    public static final String KEY_TOGGLE_ETHERNET = "toggle_ethernet";

    private MasterSwitchPreference mEthernetPreference;
    private EthernetEnabler mEthernetEnabler;
    private final EthernetSummaryUpdater mSummaryHelper;
    private final MetricsFeatureProvider mMetricsFeatureProvider;

    public EthernetMasterSwitchPreferenceController(Context context,
            MetricsFeatureProvider metricsFeatureProvider) {
        super(context);
        mMetricsFeatureProvider = metricsFeatureProvider;
        mSummaryHelper = new EthernetSummaryUpdater(mContext, this);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mEthernetPreference = (MasterSwitchPreference) screen.findPreference(KEY_TOGGLE_ETHERNET);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_TOGGLE_ETHERNET;
    }

    @Override
    public void onResume() {
        mSummaryHelper.register(true);
        if (mEthernetEnabler != null) {
            mEthernetEnabler.resume(mContext);
        }
    }

    @Override
    public void onPause() {
        if (mEthernetEnabler != null) {
            mEthernetEnabler.pause();
        }
        mSummaryHelper.register(false);
    }

    @Override
    public void onStart() {
        mEthernetEnabler = new EthernetEnabler(mContext, new MasterSwitchController(mEthernetPreference),
            mMetricsFeatureProvider);
    }

    @Override
    public void onStop() {
        if (mEthernetEnabler != null) {
            mEthernetEnabler.teardownSwitchController();
        }
    }

    @Override
    public void onSummaryChanged(String summary) {
        if (mEthernetPreference != null) {
            mEthernetPreference.setSummary(summary);
        }
    }

}
