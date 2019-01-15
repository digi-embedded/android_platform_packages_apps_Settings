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

import java.util.regex.Pattern;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.widget.SwitchBar;

import android.app.ProgressDialog;
import android.cloudconnector.CloudConnectorHandler;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.widget.Switch;
import android.widget.Toast;

import com.digi.android.cloudconnector.CloudConnectorManager;
import com.digi.android.cloudconnector.CloudConnectorPreferencesManager;
import com.digi.android.cloudconnector.ICloudConnectorEventListener;

/**
 * Main fragment of the Cloud Connector settings preference page.
 */
public class CloudConnectorSettings extends SettingsPreferenceFragment
        implements SwitchBar.OnSwitchChangeListener, ICloudConnectorEventListener {

    // Constants.
    private static final String TAG = "CloudConnectorSetting";

    private final static String SHARED_PREFERENCES_NAME = "cloud_connector_settings";
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

    private final static String VENDOR_ID_PATTERN = "0[xX][0-9a-fA-F]{8}";
    private final static String URL_PATTERN = ".+";
    private final static String DEVICE_NAME_PATTERN = ".+";

    private final static String ERROR_INVALID_VENDOR_ID = "Vendor ID is invalid, it must follow this format: 0x01234567";
    private final static String ERROR_INVALID_DEVICE_NAME = "Device name cannot be empty";
    private final static String ERROR_INVALID_STRING_LENGTH = "Device %s is invalid, it must be between %d and %d characters";
    private final static String ERROR_INVALID_SYSTEM_MONITOR_SAMPLE_RATE = "Sample rate must be equal or greater than " + CloudConnectorPreferencesManager.MINIMUM_SAMPLE_RATE + " seconds";
    private final static String ERROR_INVALID_SYSTEM_MONITOR_UPLOAD_SAMPLES_SIZE = "The number of samples to store before uploading must be greater than 0 and lower than " + CloudConnectorPreferencesManager.MAXIMUM_UPLOAD_SAMPLES_SIZE;
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
    private SwitchPreference enableAutoConnectSwitch;
    private SwitchPreference enableSecureConnectionSwitch;
    private SwitchPreference enableCompressionSwitch;
    private SwitchPreference systemMonitorSwitch;
    private SwitchPreference systemMonitorMemorySwitch;
    private SwitchPreference systemMonitorCPULoadSwitch;
    private SwitchPreference systemMonitorCPUTempSwitch;

    private CloudConnectorManager connector;
    private CloudConnectorPreferencesManager ccPrefsManager;

    private ProgressDialog dialog;

    private Handler handler = new Handler();

    private OnPreferenceChangeListener prefChangeListener = new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            boolean valid = true;
            switch (preference.getKey()) {
            case PREF_DEVICE_ID:
                break;
            case PREF_VENDOR_ID:
                valid = Pattern.matches(VENDOR_ID_PATTERN, (String) newValue);
                if (!valid) {
                    Toast.makeText(context, ERROR_INVALID_VENDOR_ID, Toast.LENGTH_LONG).show();
                    return false;
                }
                ccPrefsManager.setVendorID((String) newValue);
                break;
            case PREF_DEVICE_NAME:
                String name = (String) newValue;
                valid = Pattern.matches(DEVICE_NAME_PATTERN , name);
                if (!valid || name.length() > CloudConnectorPreferencesManager.DEVICE_NAME_MAXIMUM_LENGTH || name.trim().length() == 0) {
                    Toast.makeText(context, String.format(ERROR_INVALID_STRING_LENGTH, "name", 1, CloudConnectorPreferencesManager.DEVICE_NAME_MAXIMUM_LENGTH), Toast.LENGTH_LONG).show();
                    return false;
                }
                ccPrefsManager.setDeviceName(name);
                break;
            case PREF_DESCRIPTION:
                if (((String) newValue).length() > CloudConnectorPreferencesManager.DESCRIPTION_MAXIMUM_LENGTH) {
                    Toast.makeText(context, String.format(ERROR_INVALID_STRING_LENGTH, "description", 1, CloudConnectorPreferencesManager.DESCRIPTION_MAXIMUM_LENGTH), Toast.LENGTH_LONG).show();
                    return false;
                }
                ccPrefsManager.setDeviceDescription((String) newValue);
                break;
            case PREF_CONTACT:
                if (((String) newValue).length() > CloudConnectorPreferencesManager.CONTACT_MAXIMUM_LENGTH) {
                    Toast.makeText(context, String.format(ERROR_INVALID_STRING_LENGTH, "contact", 1, CloudConnectorPreferencesManager.CONTACT_MAXIMUM_LENGTH), Toast.LENGTH_LONG).show();
                    return false;
                }
                ccPrefsManager.setDeviceContactInformation((String) newValue);
                break;
            case PREF_URL:
                valid = Pattern.matches(URL_PATTERN, (String) newValue);
                if (!valid) {
                    Toast.makeText(context, ERROR_INVALID_URL, Toast.LENGTH_LONG).show();
                    return false;
                }
                ccPrefsManager.setURL((String) newValue);
                break;
            case PREF_ENABLE_AUTO_CONNECT:
                ccPrefsManager.setAutoConnectEnabled(((Boolean) newValue).booleanValue());
                break;
            case PREF_ENABLE_SECURE_CONNECTION:
                ccPrefsManager.setSecureConnectionEnabled(((Boolean) newValue).booleanValue());
                break;
            case PREF_ENABLE_COMPRESSION:
                ccPrefsManager.setCompressionEnabled(((Boolean) newValue).booleanValue());
                break;
            case PREF_ENABLE_SYSTEM_MONITOR:
                ccPrefsManager.enableSystemMonitor(((Boolean) newValue).booleanValue());
                break;
            case PREF_SYSTEM_MONITOR_SAMPLE_RATE:
                try {
                    int sampleRate = Integer.parseInt((String) newValue);
                    if (sampleRate < CloudConnectorPreferencesManager.MINIMUM_SAMPLE_RATE) {
                        Toast.makeText(context, ERROR_INVALID_SYSTEM_MONITOR_SAMPLE_RATE, Toast.LENGTH_LONG).show();
                        return false;
                    }
                    ccPrefsManager.setSystemMonitorSampleRate(sampleRate);
                    break;
                } catch (Exception e) { }
                Toast.makeText(context, CloudConnectorHandler.ERROR_INVALID_SYSTEM_MONITOR_SAMPLE_RATE, Toast.LENGTH_LONG).show();
                return false;
            case PREF_SYSTEM_MONITOR_UPLOAD_SAMPLES_SIZE:
                try {
                    int uploadSize = Integer.parseInt((String) newValue);
                    if (uploadSize <= 0 || uploadSize > CloudConnectorPreferencesManager.MAXIMUM_UPLOAD_SAMPLES_SIZE) {
                        Toast.makeText(context, ERROR_INVALID_SYSTEM_MONITOR_UPLOAD_SAMPLES_SIZE, Toast.LENGTH_LONG).show();
                        return false;
                    }
                    ccPrefsManager.setSystemMonitorUploadSamplesSize(uploadSize);
                    break;
                } catch (Exception e) { }
                Toast.makeText(context, CloudConnectorHandler.ERROR_INVALID_SYSTEM_MONITOR_UPLOAD_SAMPLES_SIZE, Toast.LENGTH_LONG).show();
                return false;
            case PREF_SYSTEM_MONITOR_ENABLE_MEMORY_SAMPLING:
                ccPrefsManager.enableSystemMonitorMemorySampling(((Boolean) newValue).booleanValue());
                break;
            case PREF_SYSTEM_MONITOR_ENABLE_CPU_LOAD_SAMPLING:
                ccPrefsManager.enableSystemMonitorCPULoadSampling(((Boolean) newValue).booleanValue());
                break;
            case PREF_SYSTEM_MONITOR_ENABLE_CPU_TEMPERATURE_SAMPLING:
                ccPrefsManager.enableSystemMonitorCPUTemperatureSampling(((Boolean) newValue).booleanValue());
                break;
            default:
                break;
            }
            updateUI();
            return valid;
        }
    };

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.CLOUD_CONNECTOR;
    }

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
        descText.setOnPreferenceChangeListener(prefChangeListener);
        contactText = (EditTextPreference) findPreference(PREF_CONTACT);
        contactText.setOnPreferenceChangeListener(prefChangeListener);
        urlText = (EditTextPreference) findPreference(PREF_URL);
        urlText.setOnPreferenceChangeListener(prefChangeListener);
        enableAutoConnectSwitch = (SwitchPreference) findPreference(PREF_ENABLE_AUTO_CONNECT);
        enableAutoConnectSwitch.setOnPreferenceChangeListener(prefChangeListener);
        enableSecureConnectionSwitch = (SwitchPreference) findPreference(PREF_ENABLE_SECURE_CONNECTION);
        enableSecureConnectionSwitch.setOnPreferenceChangeListener(prefChangeListener);
        enableCompressionSwitch = (SwitchPreference) findPreference(PREF_ENABLE_COMPRESSION);
        enableCompressionSwitch.setOnPreferenceChangeListener(prefChangeListener);
        systemMonitorSampleRateText = (EditTextPreference) findPreference(PREF_SYSTEM_MONITOR_SAMPLE_RATE);
        systemMonitorSampleRateText.setOnPreferenceChangeListener(prefChangeListener);
        systemMonitorUploadSamplesSizeText = (EditTextPreference) findPreference(PREF_SYSTEM_MONITOR_UPLOAD_SAMPLES_SIZE);
        systemMonitorUploadSamplesSizeText.setOnPreferenceChangeListener(prefChangeListener);
        systemMonitorSwitch = (SwitchPreference) findPreference(PREF_ENABLE_SYSTEM_MONITOR);
        systemMonitorSwitch.setOnPreferenceChangeListener(prefChangeListener);
        systemMonitorMemorySwitch = (SwitchPreference) findPreference(PREF_SYSTEM_MONITOR_ENABLE_MEMORY_SAMPLING);
        systemMonitorMemorySwitch.setOnPreferenceChangeListener(prefChangeListener);
        systemMonitorCPULoadSwitch = (SwitchPreference) findPreference(PREF_SYSTEM_MONITOR_ENABLE_CPU_LOAD_SAMPLING);
        systemMonitorCPULoadSwitch.setOnPreferenceChangeListener(prefChangeListener);
        systemMonitorCPUTempSwitch = (SwitchPreference) findPreference(PREF_SYSTEM_MONITOR_ENABLE_CPU_TEMPERATURE_SAMPLING);
        systemMonitorCPUTempSwitch.setOnPreferenceChangeListener(prefChangeListener);

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
        connector = new CloudConnectorManager(context);
        ccPrefsManager = connector.getPreferencesManager();

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
        connector.unregisterEventListener(this);
    }

    @Override
    public void onSwitchChanged(Switch switchView, final boolean isChecked) {
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
    public void connected() {
        dialog.dismiss();
        Toast.makeText(context, "Connected to Remote Manager", Toast.LENGTH_LONG).show();
        updateInfo(true);
    }

    @Override
    public void disconnected() {
        dialog.dismiss();
        Toast.makeText(context, "Disconnected from Remote Manager", Toast.LENGTH_LONG).show();
        updateInfo(false);
    }

    @Override
    public void connectionError(String errorMessage) {
        dialog.dismiss();
        Toast.makeText(context, "Error connecting to Remote Manager: " + errorMessage, Toast.LENGTH_LONG).show();
        updateInfo(false);
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
     * Update the Remote Manager connection switch state to the given one.
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
     * Update the Remote Manager connection state and configuration.
     *
     * @param connected {@code true} to update to connected state, {@code false}
     *                  otherwise.
     */
    private void updateInfo(boolean connected) {
        updateSwitchCc(connected);
        new Thread(new Runnable() {
            @Override
            public void run() {
                updateUI();
            }
        }).start();
    }

    /**
     * Update the values of Cloud Connector.
     */
    private void updateUI() {
        final String deviceId = ccPrefsManager.getDeviceID();
        final String vendorId = ccPrefsManager.getVendorID();
        final String deviceName = ccPrefsManager.getDeviceName();
        final String description = ccPrefsManager.getDeviceDescription();
        final String contact = ccPrefsManager.getDeviceContactInformation();
        final String url = ccPrefsManager.getURL();
        final String systemMonitorSampleRate = "" + ccPrefsManager.getSystemMonitorSampleRate();
        final String systemMonitorUploadSamplesSize = "" + ccPrefsManager.getSystemMonitorUploadSamplesSize();
        final boolean autoConnect = ccPrefsManager.isAutoConnectEnabled();
        final boolean secureConnection = ccPrefsManager.isSecureConnectionEnabled();
        final boolean compression = ccPrefsManager.isCompressionEnabled();
        final boolean enableSystemMonitor = ccPrefsManager.isSystemMonitorEnabled();
        final boolean systemMonitorEnableMemorySampling = ccPrefsManager.isSystemMonitorMemorySamplingEnabled();
        final boolean systemMonitorEnableCPULoadSampling = ccPrefsManager.isSystemMonitorCPULoadSamplingEnabled();
        final boolean systemMonitorEnableCPUTemperatureSampling = ccPrefsManager.isSystemMonitorCPUTemperatureSamplingEnabled();

        handler.post(new Runnable() {
            @Override
            public void run() {
                SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);

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

                enableAutoConnectSwitch.setChecked(autoConnect);
                enableSecureConnectionSwitch.setChecked(secureConnection);
                enableCompressionSwitch.setChecked(compression);
                systemMonitorSwitch.setChecked(enableSystemMonitor);
                systemMonitorSwitch.setSummary(context.getString(enableSystemMonitor ? R.string.switch_on_text : R.string.switch_off_text));
                systemMonitorMemorySwitch.setChecked(systemMonitorEnableMemorySampling);
                systemMonitorMemorySwitch.setSummary(context.getString(systemMonitorEnableMemorySampling ? R.string.switch_on_text : R.string.switch_off_text));
                systemMonitorCPULoadSwitch.setChecked(systemMonitorEnableCPULoadSampling);
                systemMonitorCPULoadSwitch.setSummary(context.getString(systemMonitorEnableCPULoadSampling ? R.string.switch_on_text : R.string.switch_off_text));
                systemMonitorCPUTempSwitch.setChecked(systemMonitorEnableCPUTemperatureSampling);
                systemMonitorCPUTempSwitch.setSummary(context.getString(systemMonitorEnableCPUTemperatureSampling ? R.string.switch_on_text : R.string.switch_off_text));
            }
        });

        // Ensure the switch is accordingly configured with the connector state.
        if (connector.isConnected() != switchCc.isChecked()) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    updateSwitchCc(connector.isConnected());
                }
            });
        }
    }
}
