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

package com.android.settings.wifi.tether;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.WifiManager;
import android.os.Looper;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class WifiTetherMaximizeCompatibilityPreferenceControllerTest {

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock
    private WifiManager mWifiManager;
    @Mock
    private WifiTetherBasePreferenceController.OnTetherConfigUpdateListener mListener;

    private WifiTetherMaximizeCompatibilityPreferenceController mController;
    private SwitchPreference mPreference;
    private SoftApConfiguration mConfig;

    @Before
    public void setUp() {
        final Context context = spy(ApplicationProvider.getApplicationContext());
        mConfig = new SoftApConfiguration.Builder()
                .setSsid("test_Ssid")
                .setPassphrase(null, SoftApConfiguration.SECURITY_TYPE_OPEN)
                .setBridgedModeOpportunisticShutdownEnabled(true)
                .build();
        doReturn(mWifiManager).when(context).getSystemService(Context.WIFI_SERVICE);
        doReturn(true).when(mWifiManager).isBridgedApConcurrencySupported();
        doReturn(mConfig).when(mWifiManager).getSoftApConfiguration();

        mController = new WifiTetherMaximizeCompatibilityPreferenceController(context, mListener);
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        final PreferenceManager preferenceManager = new PreferenceManager(context);
        final PreferenceScreen screen = preferenceManager.createPreferenceScreen(context);
        mPreference = new SwitchPreference(context);
        mPreference.setKey(WifiTetherMaximizeCompatibilityPreferenceController.PREF_KEY);
        screen.addPreference(mPreference);
        mController.displayPreference(screen);
    }

    @Test
    public void getPreferenceKey_shouldBeCorrect() {
        assertThat(mController.getPreferenceKey())
                .isEqualTo(WifiTetherMaximizeCompatibilityPreferenceController.PREF_KEY);
    }

    @Test
    public void updateDisplay_notSupport5GHzBand_setPreferenceDisabled() {
        doReturn(false).when(mWifiManager).is5GHzBandSupported();

        mController.updateDisplay();

        assertThat(mPreference.isEnabled()).isEqualTo(false);
    }

    @Test
    public void updateDisplay_getNullCountryCode_setPreferenceDisabled() {
        doReturn(null).when(mWifiManager).getCountryCode();

        mController.updateDisplay();

        assertThat(mPreference.isEnabled()).isEqualTo(false);
    }

    @Test
    public void updateDisplay_supported5GHzBandAndCountryCodeIsNotNull_setPreferenceEnabled() {
        doReturn(true).when(mWifiManager).is5GHzBandSupported();
        doReturn("US").when(mWifiManager).getCountryCode();

        mController.updateDisplay();

        assertThat(mPreference.isEnabled()).isEqualTo(true);
    }

    @Test
    public void onPreferenceChange_callbackOnTetherConfigUpdated() {
        mController.onPreferenceChange(mPreference, true);
        verify(mListener).onTetherConfigUpdated(any());
    }

    @Test
    public void isMaximizeCompatibilityEnabled_concurrencySupportedAndEnabled_returnTure() {
        // The preconditions are ready in setup().

        assertThat(mController.isMaximizeCompatibilityEnabled()).isEqualTo(true);
    }

    @Test
    public void isMaximizeCompatibilityEnabled_concurrencySupportedAndDisabled_returnFalse() {
        SoftApConfiguration config = new SoftApConfiguration.Builder()
                .setBridgedModeOpportunisticShutdownEnabled(false)
                .build();
        doReturn(config).when(mWifiManager).getSoftApConfiguration();

        assertThat(mController.isMaximizeCompatibilityEnabled()).isEqualTo(false);
    }

    @Test
    public void isMaximizeCompatibilityEnabled_noConcurrencyAndGetBand2gOnly_returnFalse() {
        doReturn(false).when(mWifiManager).isBridgedApConcurrencySupported();
        SoftApConfiguration config = new SoftApConfiguration.Builder()
                .setBand(SoftApConfiguration.BAND_2GHZ)
                .build();
        doReturn(config).when(mWifiManager).getSoftApConfiguration();

        assertThat(mController.isMaximizeCompatibilityEnabled()).isEqualTo(false);
    }

    @Test
    public void isMaximizeCompatibilityEnabled_noConcurrencyAndGetBand5gOnly_returnTrue() {
        doReturn(false).when(mWifiManager).isBridgedApConcurrencySupported();
        SoftApConfiguration config = new SoftApConfiguration.Builder()
                .setBand(SoftApConfiguration.BAND_5GHZ)
                .build();
        doReturn(config).when(mWifiManager).getSoftApConfiguration();

        assertThat(mController.isMaximizeCompatibilityEnabled()).isEqualTo(true);
    }

    @Test
    public void isMaximizeCompatibilityEnabled_noConcurrencyAndGetBand2gAnd5g_returnTrue() {
        doReturn(false).when(mWifiManager).isBridgedApConcurrencySupported();
        SoftApConfiguration config = new SoftApConfiguration.Builder()
                .setBand(SoftApConfiguration.BAND_2GHZ | SoftApConfiguration.BAND_5GHZ)
                .build();
        doReturn(config).when(mWifiManager).getSoftApConfiguration();

        assertThat(mController.isMaximizeCompatibilityEnabled()).isEqualTo(true);
    }

    @Test
    public void setupMaximizeCompatibility_concurrencySupportedAndDisabled_setDisabled() {
        // The precondition of the concurrency supported is ready in setup().
        mController.onPreferenceChange(mPreference, false);

        SoftApConfiguration.Builder builder = new SoftApConfiguration.Builder();
        mController.setupMaximizeCompatibility(builder);

        assertThat(builder.build().isBridgedModeOpportunisticShutdownEnabled()).isEqualTo(false);
    }

    @Test
    public void setupMaximizeCompatibility_concurrencySupportedAndEnabled_setEnabled() {
        // The precondition of the concurrency supported is ready in setup().
        mController.onPreferenceChange(mPreference, true);

        SoftApConfiguration.Builder builder = new SoftApConfiguration.Builder();
        mController.setupMaximizeCompatibility(builder);

        assertThat(builder.build().isBridgedModeOpportunisticShutdownEnabled()).isEqualTo(true);
    }

    @Test
    public void setupMaximizeCompatibility_noConcurrencyAndSetDisabled_setBand2gOnly() {
        doReturn(false).when(mWifiManager).isBridgedApConcurrencySupported();
        mController.onPreferenceChange(mPreference, false);

        SoftApConfiguration.Builder builder = new SoftApConfiguration.Builder();
        mController.setupMaximizeCompatibility(builder);

        assertThat(builder.build().getBand()).isEqualTo(SoftApConfiguration.BAND_2GHZ);
    }

    @Test
    public void setupMaximizeCompatibility_noConcurrencyAndSetEnabled_setBand2gAnd5g() {
        doReturn(false).when(mWifiManager).isBridgedApConcurrencySupported();
        mController.onPreferenceChange(mPreference, true);

        SoftApConfiguration.Builder builder = new SoftApConfiguration.Builder();
        mController.setupMaximizeCompatibility(builder);

        assertThat(builder.build().getBand())
                .isEqualTo(SoftApConfiguration.BAND_2GHZ | SoftApConfiguration.BAND_5GHZ);
    }
}
