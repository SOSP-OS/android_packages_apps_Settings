/*
 * Copyright 2018 The Android Open Source Project
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
package com.android.settings.connecteddevice;

import static com.android.settingslib.Utils.isAudioModeOngoingCall;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcastAssistant;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.accessibility.HearingAidUtils;
import com.android.settings.bluetooth.AvailableMediaBluetoothDeviceUpdater;
import com.android.settings.bluetooth.BluetoothDeviceUpdater;
import com.android.settings.bluetooth.Utils;
import com.android.settings.connecteddevice.audiosharing.AudioSharingUtils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Controller to maintain the {@link androidx.preference.PreferenceGroup} for all available media
 * devices. It uses {@link DevicePreferenceCallback} to add/remove {@link Preference}
 */
public class AvailableMediaDeviceGroupController extends BasePreferenceController
        implements LifecycleObserver, OnStart, OnStop, DevicePreferenceCallback, BluetoothCallback {
    private static final boolean DEBUG = BluetoothUtils.D;

    private static final String TAG = "AvailableMediaDeviceGroupController";
    private static final String KEY = "available_device_list";

    @VisibleForTesting PreferenceGroup mPreferenceGroup;
    @VisibleForTesting LocalBluetoothManager mLocalBluetoothManager;
    private final Executor mExecutor;
    private BluetoothDeviceUpdater mBluetoothDeviceUpdater;
    private FragmentManager mFragmentManager;
    private BluetoothLeBroadcastAssistant.Callback mAssistantCallback =
            new BluetoothLeBroadcastAssistant.Callback() {
                @Override
                public void onSearchStarted(int reason) {}

                @Override
                public void onSearchStartFailed(int reason) {}

                @Override
                public void onSearchStopped(int reason) {}

                @Override
                public void onSearchStopFailed(int reason) {}

                @Override
                public void onSourceFound(@NonNull BluetoothLeBroadcastMetadata source) {}

                @Override
                public void onSourceAdded(@NonNull BluetoothDevice sink, int sourceId, int reason) {
                    mBluetoothDeviceUpdater.forceUpdate();
                }

                @Override
                public void onSourceAddFailed(
                        @NonNull BluetoothDevice sink,
                        @NonNull BluetoothLeBroadcastMetadata source,
                        int reason) {}

                @Override
                public void onSourceModified(
                        @NonNull BluetoothDevice sink, int sourceId, int reason) {}

                @Override
                public void onSourceModifyFailed(
                        @NonNull BluetoothDevice sink, int sourceId, int reason) {}

                @Override
                public void onSourceRemoved(
                        @NonNull BluetoothDevice sink, int sourceId, int reason) {
                    mBluetoothDeviceUpdater.forceUpdate();
                }

                @Override
                public void onSourceRemoveFailed(
                        @NonNull BluetoothDevice sink, int sourceId, int reason) {}

                @Override
                public void onReceiveStateChanged(
                        BluetoothDevice sink,
                        int sourceId,
                        BluetoothLeBroadcastReceiveState state) {}
            };

    public AvailableMediaDeviceGroupController(Context context) {
        super(context, KEY);
        mLocalBluetoothManager = Utils.getLocalBtManager(mContext);
        mExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void onStart() {
        if (mLocalBluetoothManager == null) {
            Log.e(TAG, "onStart() Bluetooth is not supported on this device");
            return;
        }
        if (AudioSharingUtils.isFeatureEnabled()) {
            LocalBluetoothLeBroadcastAssistant assistant =
                    mLocalBluetoothManager
                            .getProfileManager()
                            .getLeAudioBroadcastAssistantProfile();
            if (assistant != null) {
                if (DEBUG) {
                    Log.d(TAG, "onStart() Register callbacks for assistant.");
                }
                assistant.registerServiceCallBack(mExecutor, mAssistantCallback);
            }
        }
        mBluetoothDeviceUpdater.registerCallback();
        mLocalBluetoothManager.getEventManager().registerCallback(this);
        mBluetoothDeviceUpdater.refreshPreference();
    }

    @Override
    public void onStop() {
        if (mLocalBluetoothManager == null) {
            Log.e(TAG, "onStop() Bluetooth is not supported on this device");
            return;
        }
        if (AudioSharingUtils.isFeatureEnabled()) {
            LocalBluetoothLeBroadcastAssistant assistant =
                    mLocalBluetoothManager
                            .getProfileManager()
                            .getLeAudioBroadcastAssistantProfile();
            if (assistant != null) {
                if (DEBUG) {
                    Log.d(TAG, "onStop() Register callbacks for assistant.");
                }
                assistant.unregisterServiceCallBack(mAssistantCallback);
            }
        }
        mBluetoothDeviceUpdater.unregisterCallback();
        mLocalBluetoothManager.getEventManager().unregisterCallback(this);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreferenceGroup = screen.findPreference(KEY);
        mPreferenceGroup.setVisible(false);

        if (isAvailable()) {
            updateTitle();
            mBluetoothDeviceUpdater.setPrefContext(screen.getContext());
            mBluetoothDeviceUpdater.forceUpdate();
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
                ? AVAILABLE_UNSEARCHABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void onDeviceAdded(Preference preference) {
        if (mPreferenceGroup.getPreferenceCount() == 0) {
            mPreferenceGroup.setVisible(true);
        }
        mPreferenceGroup.addPreference(preference);
    }

    @Override
    public void onDeviceRemoved(Preference preference) {
        mPreferenceGroup.removePreference(preference);
        if (mPreferenceGroup.getPreferenceCount() == 0) {
            mPreferenceGroup.setVisible(false);
        }
    }

    public void init(DashboardFragment fragment) {
        mFragmentManager = fragment.getParentFragmentManager();
        mBluetoothDeviceUpdater =
                new AvailableMediaBluetoothDeviceUpdater(
                        fragment.getContext(),
                        AvailableMediaDeviceGroupController.this,
                        fragment.getMetricsCategory());
    }

    @VisibleForTesting
    public void setFragmentManager(FragmentManager fragmentManager) {
        mFragmentManager = fragmentManager;
    }

    @VisibleForTesting
    public void setBluetoothDeviceUpdater(BluetoothDeviceUpdater bluetoothDeviceUpdater) {
        mBluetoothDeviceUpdater = bluetoothDeviceUpdater;
    }

    @Override
    public void onAudioModeChanged() {
        updateTitle();
    }

    @Override
    public void onActiveDeviceChanged(CachedBluetoothDevice activeDevice, int bluetoothProfile) {
        // exclude inactive device
        if (activeDevice == null) {
            return;
        }

        if (bluetoothProfile == BluetoothProfile.HEARING_AID) {
            HearingAidUtils.launchHearingAidPairingDialog(
                    mFragmentManager, activeDevice, getMetricsCategory());
        }
    }

    private void updateTitle() {
        if (isAudioModeOngoingCall(mContext)) {
            // in phone call
            mPreferenceGroup.setTitle(
                    mContext.getString(R.string.connected_device_call_device_title));
        } else {
            // without phone call
            mPreferenceGroup.setTitle(
                    mContext.getString(R.string.connected_device_media_device_title));
        }
    }
}
