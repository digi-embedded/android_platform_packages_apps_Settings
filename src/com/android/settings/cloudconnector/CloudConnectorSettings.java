/*
 * Copyright 2018, Digi International Inc.
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
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

import java.util.regex.Pattern;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.widget.SwitchBar;

import android.app.ProgressDialog;
import android.cloudconnector.CloudConnectorEventListenerImpl;
import android.cloudconnector.CloudConnectorHandler;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Handler;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.SwitchPreference;
import android.widget.Switch;
import android.widget.Toast;

/**
 * Main fragment of the Cloud Connector settings preference page.
 */
public class CloudConnectorSettings extends SettingsPreferenceFragment
        implements SwitchBar.OnSwitchChangeListener, OnSharedPreferenceChangeListener, CloudConnectorEventListenerImpl {

    // Constants.
    private final static String PREF_DEVICE_ID = "device_id";
    private final static String PREF_VENDOR_ID = "vendor_id";
    private final static String PREF_DEVICE_NAME = "device_type";
    private final static String PREF_DESCRIPTION = "description";
    private final static String PREF_CONTACT = "contact";
    private final static String PREF_URL = "url";
    private final static String PREF_ENABLE_AUTO_CONNECT = "enable_auto_connect";
    private final static String PREF_ENABLE_SECURE_CONNECTION = "enable_secure_connection";
    private final static String PREF_ENABLE_COMPRESSION = "enable_compression";
    private final static String PREF_ENABLE_SYSTEM_MONITOR = "enable_system_monitor";
    private final static String PREF_SYSTEM_MONITOR_SAMPLE_RATE = "system_monitor_sample_rate";
    private final static String PREF_SYSTEM_MONITOR_UPLOAD_SAMPLES_SIZE = "system_monitor_upload_samples_size";
    private final static String PREF_SYSTEM_MONITOR_ENABLE_MEMORY_SAMPLING = "system_monitor_enable_memory_sampling";
    private final static String PREF_SYSTEM_MONITOR_ENABLE_CPU_LOAD_SAMPLING = "system_monitor_enable_cpu_load_sampling";
    private final static String PREF_SYSTEM_MONITOR_ENABLE_CPU_TEMPERATURE_SAMPLING = "system_monitor_enable_cpu_temperature_sampling";

    private final static boolean DEFAULT_ENABLE_SYSTEM_MONITOR = false;
    private final static int DEFAULT_SYSTEM_MONITOR_SAMPLE_RATE = 10;
    private final static int DEFAULT_SYSTEM_MONITOR_UPLOAD_SAMPLES_SIZE = 6;
    private final static boolean DEFAULT_SYSTEM_MONITOR_ENABLE_MEMORY_SAMPLING = true;
    private final static boolean DEFAULT_SYSTEM_MONITOR_ENABLE_CPU_LOAD_SAMPLING = true;
    private final static boolean DEFAULT_SYSTEM_MONITOR_ENABLE_CPU_TEMPERATURE_SAMPLING = true;

    private final static String VENDOR_ID_PATTERN = "0[xX][0-9a-fA-F]{8}";
    private final static String URL_PATTERN = ".+";
    private final static String DEVICE_NAME_PATTERN = ".+";

    private final static String ERROR_INVALID_VENDOR_ID = "Vendor ID is invalid, it must follow this format: 0x01234567";
    private final static String ERROR_INVALID_DEVICE_NAME = "Device name cannot be empty";
    private final static String ERROR_INVALID_URL = "Invalid URL";

    // Variables.
    private Context context;

    private SwitchBar switchBar;
    private Switch switchCc;

    private EditTextPreference vendorIdText;
    private EditTextPreference nameText;
    private EditTextPreference descText;
    private EditTextPreference contactText;
    private EditTextPreference urlText;
    private EditTextPreference systemMonitorSampleRateText;
    private EditTextPreference systemMonitorUploadSamplesSizeText;

    private CloudConnectorHandler connector;

    private ProgressDialog dialog;

    private Handler handler = new Handler();

    private OnPreferenceChangeListener prefChangeListener = new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            boolean valid = false;
            switch (preference.getKey()) {
            case PREF_VENDOR_ID:
                valid = Pattern.matches(VENDOR_ID_PATTERN, (String) newValue);
                if (!valid)
                    Toast.makeText(context, ERROR_INVALID_VENDOR_ID, Toast.LENGTH_LONG).show();
                return valid;
            case PREF_DEVICE_NAME:
                valid = Pattern.matches(DEVICE_NAME_PATTERN , (String) newValue);
                if (!valid)
                    Toast.makeText(context, ERROR_INVALID_DEVICE_NAME, Toast.LENGTH_LONG).show();
                return valid;
            case PREF_URL:
                valid = Pattern.matches(URL_PATTERN, (String) newValue);
                if (!valid)
                    Toast.makeText(context, ERROR_INVALID_URL, Toast.LENGTH_LONG).show();
                return valid;
            case PREF_SYSTEM_MONITOR_SAMPLE_RATE:
                try {
                    int sampleRate = Integer.valueOf((String) newValue);
                    if (sampleRate > 0)
                        return true;
                } catch (Exception e) { }
                Toast.makeText(context, CloudConnectorHandler.ERROR_INVALID_SYSTEM_MONITOR_SAMPLE_RATE, Toast.LENGTH_LONG).show();
                return valid;
            case PREF_SYSTEM_MONITOR_UPLOAD_SAMPLES_SIZE:
                try {
                    int uploadSize = Integer.valueOf((String) newValue);
                    if (uploadSize > 0 && uploadSize <= CloudConnectorHandler.MAXIMUM_UPLOAD_SAMPLES_SIZE)
                        return true;
                } catch (Exception e) { }
                Toast.makeText(context, CloudConnectorHandler.ERROR_INVALID_SYSTEM_MONITOR_UPLOAD_SAMPLES_SIZE, Toast.LENGTH_LONG).show();
                return valid;
            default:
                return true;
            }
        }
    };

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        switchBar = ((SettingsActivity) getActivity()).getSwitchBar();
        switchCc = switchBar.getSwitch();

        vendorIdText = (EditTextPreference) findPreference(PREF_VENDOR_ID);
        vendorIdText.setOnPreferenceChangeListener(prefChangeListener);
        nameText = (EditTextPreference) findPreference(PREF_DEVICE_NAME);
        nameText.setOnPreferenceChangeListener(prefChangeListener);
        descText = (EditTextPreference) findPreference(PREF_DESCRIPTION);
        contactText = (EditTextPreference) findPreference(PREF_CONTACT);
        urlText = (EditTextPreference) findPreference(PREF_URL);
        urlText.setOnPreferenceChangeListener(prefChangeListener);
        systemMonitorSampleRateText = (EditTextPreference) findPreference(PREF_SYSTEM_MONITOR_SAMPLE_RATE);
        systemMonitorSampleRateText.setOnPreferenceChangeListener(prefChangeListener);
        systemMonitorUploadSamplesSizeText = (EditTextPreference) findPreference(PREF_SYSTEM_MONITOR_UPLOAD_SAMPLES_SIZE);
        systemMonitorUploadSamplesSizeText.setOnPreferenceChangeListener(prefChangeListener);

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

        addPreferencesFromResource(R.xml.cloud_connector_settings);

        context = getActivity();
        connector = (CloudConnectorHandler) context.getSystemService(Context.CLOUD_CONNECTOR_SERVICE);

        dialog = new ProgressDialog(context);
        dialog.setMessage(context.getString(R.string.cc_connection_dialog_message));
        dialog.setCancelable(false);
        dialog.setIndeterminate(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Update switch.
        switchCc.setChecked(connector.isConnected());
        // Register listeners.
        switchBar.addOnSwitchChangeListener(this);
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        connector.registerEventListener(this);
        // Update values.
        new Thread(new Runnable() {
            @Override
            public void run() {
                updateUI();
            }
        }).start();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Unregister listeners.
        switchBar.removeOnSwitchChangeListener(this);
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        connector.unregisterEventListener(this);
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        // Show the progress dialog.
        dialog.setTitle(context.getString(
                isChecked ? R.string.cc_connection_on_dialog_title : R.string.cc_connection_off_dialog_title));
        dialog.show();

        if (isChecked)
            connector.connect();
        else
            connector.disconnect();
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                switch (key) {
                case PREF_DEVICE_ID:
                case PREF_VENDOR_ID:
                case PREF_DEVICE_NAME:
                case PREF_DESCRIPTION:
                case PREF_CONTACT:
                case PREF_URL:
                    connector.writePreference(key, sharedPreferences.getString(key, ""));
                    break;
                case PREF_ENABLE_AUTO_CONNECT:
                case PREF_ENABLE_SECURE_CONNECTION:
                case PREF_ENABLE_COMPRESSION:
                    connector.writePreference(key, String.valueOf(sharedPreferences.getBoolean(key, false)));
                    break;
                case PREF_ENABLE_SYSTEM_MONITOR:
                    connector.enableSystemMonitor(sharedPreferences.getBoolean(key, DEFAULT_ENABLE_SYSTEM_MONITOR));
                    break;
                case PREF_SYSTEM_MONITOR_SAMPLE_RATE:
                    connector.setSystemMonitorSampleRate(Integer.valueOf(sharedPreferences.getString(key, "" + DEFAULT_SYSTEM_MONITOR_SAMPLE_RATE)));
                    break;
                case PREF_SYSTEM_MONITOR_UPLOAD_SAMPLES_SIZE:
                    connector.setSystemMonitorUploadSamplesSize(Integer.valueOf(sharedPreferences.getString(key, "" + DEFAULT_SYSTEM_MONITOR_UPLOAD_SAMPLES_SIZE)));
                    break;
                case PREF_SYSTEM_MONITOR_ENABLE_MEMORY_SAMPLING:
                    connector.enableSystemMonitorMemorySampling(sharedPreferences.getBoolean(key, DEFAULT_SYSTEM_MONITOR_ENABLE_MEMORY_SAMPLING));
                    break;
                case PREF_SYSTEM_MONITOR_ENABLE_CPU_LOAD_SAMPLING:
                    connector.enableSystemMonitorCPULoadSampling(sharedPreferences.getBoolean(key, DEFAULT_SYSTEM_MONITOR_ENABLE_CPU_LOAD_SAMPLING));
                    break;
                case PREF_SYSTEM_MONITOR_ENABLE_CPU_TEMPERATURE_SAMPLING:
                    connector.enableSystemMonitorCPUTemperatureSampling(sharedPreferences.getBoolean(key, DEFAULT_SYSTEM_MONITOR_ENABLE_CPU_TEMPERATURE_SAMPLING));
                    break;
                }
                updateUI();
            }
        }).start();
    }

    @Override
    public void connected() {
        dialog.dismiss();
        Toast.makeText(context, "Connected to Remote Manager", Toast.LENGTH_LONG).show();
        updateSwitchCc(true);
    }

    @Override
    public void disconnected() {
        dialog.dismiss();
        Toast.makeText(context, "Disconnected from Remote Manager", Toast.LENGTH_LONG).show();
        updateSwitchCc(false);
    }

    @Override
    public void connectionError(String errorMessage) {
        dialog.dismiss();
        Toast.makeText(context, "Error connecting to Remote Manager: " + errorMessage, Toast.LENGTH_LONG).show();
    }

    @Override
    public void sendDataPointsSuccess() {
        // Do nothing.
    }

    @Override
    public void sendDataPointsError(String errorMessage) {
        // Do nothing.
    }

    /**
     * Update the Device Cloud connection switch state to the given one.
     *
     * <p>If the current state of the switch is the same provided, this method
     * does nothing.</p>
     *
     * <p>This method takes care of removing the switch listener before changing
     * the state and adding it again.</p>
     *
     * @param connected {@code true} to check the switch, {@code false} to
     *                  uncheck it.
     */
    private void updateSwitchCc(boolean connected) {
        if (connected != switchCc.isChecked()) {
            switchBar.removeOnSwitchChangeListener(this);
            switchCc.setChecked(connected);
            switchBar.addOnSwitchChangeListener(this);
        }
    }

    /**
     * Update the values of Cloud Connector.
     */
    private void updateUI() {
        final String deviceId = connector.getDeviceID();
        final String vendorId = connector.readPreference(PREF_VENDOR_ID);
        final String deviceName = connector.readPreference(PREF_DEVICE_NAME);
        final String description = connector.readPreference(PREF_DESCRIPTION);
        final String contact = connector.readPreference(PREF_CONTACT);
        final String url = connector.readPreference(PREF_URL);
        final String systemMonitorSampleRate = "" + connector.getSystemMonitorSampleRate();
        final String systemMonitorUploadSamplesSize = "" + connector.getSystemMonitorUploadSamplesSize();
        final boolean autoConnect = Boolean.parseBoolean(connector.readPreference(PREF_ENABLE_AUTO_CONNECT));
        final boolean secureConnection = Boolean.parseBoolean(connector.readPreference(PREF_ENABLE_SECURE_CONNECTION));
        final boolean compression = Boolean.parseBoolean(connector.readPreference(PREF_ENABLE_COMPRESSION));
        final boolean enableSystemMonitor = connector.isSystemMonitorEnabled();
        final boolean systemMonitorEnableMemorySampling = connector.isSystemMonitorMemorySamplingEnabled();
        final boolean systemMonitorEnableCPULoadSampling = connector.isSystemMonitorCPULoadSamplingEnabled();
        final boolean systemMonitorEnableCPUTemperatureSampling = connector.isSystemMonitorCPUTemperatureSamplingEnabled();

        handler.post(new Runnable() {
            @Override
            public void run() {
                SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();

                // Unregister the preference change listener to not save again
                // the value of the checkbox preferences.
                prefs.unregisterOnSharedPreferenceChangeListener(CloudConnectorSettings.this);

                Editor editor = prefs.edit();

                findPreference(PREF_DEVICE_ID).setSummary(deviceId);
                editor.putString(PREF_DEVICE_ID, deviceId);

                vendorIdText.setSummary(vendorId);
                vendorIdText.setText(vendorId);
                editor.putString(PREF_VENDOR_ID, vendorId);

                nameText.setSummary(deviceName);
                nameText.setText(deviceName);
                editor.putString(PREF_DEVICE_NAME, deviceName);

                descText.setSummary(description);
                descText.setText(description);
                editor.putString(PREF_DESCRIPTION, description);

                contactText.setSummary(contact);
                contactText.setText(contact);
                editor.putString(PREF_CONTACT, contact);

                urlText.setSummary(url);
                urlText.setText(url);
                editor.putString(PREF_URL, url);

                systemMonitorSampleRateText.setSummary(systemMonitorSampleRate);
                systemMonitorSampleRateText.setText(systemMonitorSampleRate);
                editor.putString(PREF_SYSTEM_MONITOR_SAMPLE_RATE, systemMonitorSampleRate);

                systemMonitorUploadSamplesSizeText.setSummary(systemMonitorUploadSamplesSize);
                systemMonitorUploadSamplesSizeText.setText(systemMonitorUploadSamplesSize);
                editor.putString(PREF_SYSTEM_MONITOR_UPLOAD_SAMPLES_SIZE, systemMonitorUploadSamplesSize);

                editor.commit();

                ((SwitchPreference) findPreference(PREF_ENABLE_AUTO_CONNECT)).setChecked(autoConnect);
                ((SwitchPreference) findPreference(PREF_ENABLE_SECURE_CONNECTION)).setChecked(secureConnection);
                ((SwitchPreference) findPreference(PREF_ENABLE_COMPRESSION)).setChecked(compression);
                ((SwitchPreference) findPreference(PREF_ENABLE_SYSTEM_MONITOR)).setChecked(enableSystemMonitor);
                ((SwitchPreference) findPreference(PREF_SYSTEM_MONITOR_ENABLE_MEMORY_SAMPLING)).setChecked(systemMonitorEnableMemorySampling);
                ((SwitchPreference) findPreference(PREF_SYSTEM_MONITOR_ENABLE_CPU_LOAD_SAMPLING)).setChecked(systemMonitorEnableCPULoadSampling);
                ((SwitchPreference) findPreference(PREF_SYSTEM_MONITOR_ENABLE_CPU_TEMPERATURE_SAMPLING)).setChecked(systemMonitorEnableCPUTemperatureSampling);

                // Register again the preference change listener.
                prefs.registerOnSharedPreferenceChangeListener(CloudConnectorSettings.this);
            }
        });
    }
}
