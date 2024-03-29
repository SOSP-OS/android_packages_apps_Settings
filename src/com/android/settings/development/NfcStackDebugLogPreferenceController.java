/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.development;

import android.content.Context;
import android.os.SystemProperties;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class NfcStackDebugLogPreferenceController extends
        DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener,
        PreferenceControllerMixin {

    private static final String NFC_STACK_DEBUGLOG_ENABLED_KEY =
            "nfc_stack_debuglog_enabled";
    @VisibleForTesting
    static final String NFC_STACK_DEBUGLOG_ENABLED_PROPERTY =
            "persist.nfc.debug_enabled";

    public NfcStackDebugLogPreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return NFC_STACK_DEBUGLOG_ENABLED_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isEnabled = (Boolean) newValue;
        try {
            SystemProperties.set(NFC_STACK_DEBUGLOG_ENABLED_PROPERTY,
                    isEnabled ? "true" : "false");
        } catch (RuntimeException e) {
            Log.e(TAG, "Fail to set nfc system property: " + e.getMessage());
        }
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        try {
            final boolean isEnabled = SystemProperties.getBoolean(
                    NFC_STACK_DEBUGLOG_ENABLED_PROPERTY, false /* default */);
            ((TwoStatePreference) mPreference).setChecked(isEnabled);
        } catch (RuntimeException e) {
            Log.e(TAG, "Fail to get nfc system property: " + e.getMessage());
        }
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        try {
            SystemProperties.set(NFC_STACK_DEBUGLOG_ENABLED_PROPERTY, "false");
            ((TwoStatePreference) mPreference).setChecked(false);
        } catch (RuntimeException e) {
            Log.e(TAG, "Fail to set nfc system property: " + e.getMessage());
        }
    }
}
