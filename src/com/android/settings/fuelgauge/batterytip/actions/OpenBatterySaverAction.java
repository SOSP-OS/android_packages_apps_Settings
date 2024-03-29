/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.fuelgauge.batterytip.actions;

import android.app.settings.SettingsEnums;
import android.content.Context;

import com.android.settings.core.SubSettingLauncher;
import com.android.settings.fuelgauge.batterysaver.BatterySaverSettings;

/** Action to open the {@link com.android.settings.fuelgauge.batterysaver.BatterySaverSettings} */
public class OpenBatterySaverAction extends BatteryTipAction {

    public OpenBatterySaverAction(Context context) {
        super(context);
    }

    /** Handle the action when user clicks positive button */
    @Override
    public void handlePositiveAction(int metricsKey) {
        mMetricsFeatureProvider.action(
                mContext, SettingsEnums.ACTION_TIP_OPEN_BATTERY_SAVER_PAGE, metricsKey);
        new SubSettingLauncher(mContext)
                .setDestination(BatterySaverSettings.class.getName())
                .setSourceMetricsCategory(metricsKey)
                .launch();
    }
}
