/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.google.android.apps.exposurenotification.home;

import static com.google.common.truth.Truth.assertThat;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import com.google.android.apps.exposurenotification.home.ExposureNotificationViewModel.ExposureNotificationState;
import com.google.android.apps.exposurenotification.onboarding.OnboardingEnTurndownForRegionFragment;
import com.google.android.apps.exposurenotification.onboarding.OnboardingEnTurndownFragment;
import com.google.android.apps.exposurenotification.onboarding.OnboardingPermissionDisabledFragment;
import com.google.android.apps.exposurenotification.onboarding.OnboardingPermissionEnabledFragment;
import com.google.android.apps.exposurenotification.onboarding.OnboardingPrivateAnalyticsFragment;
import com.google.android.apps.exposurenotification.testsupport.ExposureNotificationRules;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import dagger.hilt.android.testing.HiltAndroidTest;
import dagger.hilt.android.testing.HiltTestApplication;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@HiltAndroidTest
@RunWith(RobolectricTestRunner.class)
@Config(application = HiltTestApplication.class)
public class SplashNextFragmentLiveDataTest {

  @Rule
  public ExposureNotificationRules rules = ExposureNotificationRules.forTest(this).build();

  private final Set<ExposureNotificationState> notTurndownStates = ImmutableSet.of(
      ExposureNotificationState.PAUSED_BLE,
      ExposureNotificationState.PAUSED_LOCATION,
      ExposureNotificationState.PAUSED_LOCATION_BLE,
      ExposureNotificationState.PAUSED_HW_NOT_SUPPORT,
      ExposureNotificationState.PAUSED_USER_PROFILE_NOT_SUPPORT,
      ExposureNotificationState.STORAGE_LOW,
      ExposureNotificationState.FOCUS_LOST,
      ExposureNotificationState.DISABLED,
      ExposureNotificationState.ENABLED
  );
  private final MutableLiveData<Boolean> isEnabledLiveData = new MutableLiveData<>(false);
  private final MutableLiveData<ExposureNotificationState> enStateLiveData =
      new MutableLiveData<>();
  private final MutableLiveData<Boolean> isOnboardingStateSetLiveData =
      new MutableLiveData<>(false);
  private final MutableLiveData<Boolean> isPrivateAnalyticsSetLiveData =
      new MutableLiveData<>(false);
  private final MutableLiveData<Boolean> isPrivateAnalyticsSupportedAndConfiguredLiveData =
      new MutableLiveData<>(false);

  @Test
  public void enabled_notOnboarded_returnsOnboardingPermissionEnabledFragment() {
    assertEnumeratedCases(
        /* isEnabledStates= */ Sets.newHashSet(true),
        /* enStates= */ notTurndownStates,
        /* isOnboardingStateSetStates= */ Sets.newHashSet(false),
        /* isPrivateAnalyticsSetStates= */ Sets.newHashSet(true, false),
        /* isPrivateAnalyticsSupportedAndConfiguredStates= */ Sets.newHashSet(true, false),
        /* onboardAsMigratingUser= */ Sets.newHashSet(true, false),
        /* expected= */ OnboardingPermissionEnabledFragment.class);
  }

  @Test
  public void notEnabled_notOnboarded_notEnTurndown_returnsOnboardingPermissionDisabledFragment() {
    assertEnumeratedCases(
        /* isEnabledStates= */ Sets.newHashSet(false),
        /* enStates= */ notTurndownStates,
        /* isOnboardingStateSetStates= */ Sets.newHashSet(false),
        /* isPrivateAnalyticsSetStates= */ Sets.newHashSet(true, false),
        /* isPrivateAnalyticsSupportedAndConfiguredStates= */ Sets.newHashSet(true, false),
        /* onboardAsMigratingUser= */ Sets.newHashSet(true, false),
        /* expected= */ OnboardingPermissionDisabledFragment.class);
  }

  @Test
  public void enabled_onboarded_notMigratingApp_ENPASupportedAndConfiguredButNotSet_returnsOnboardingPrivateAnalyticsFragment() {
    assertEnumeratedCases(
        /* isEnabledStates= */ Sets.newHashSet(true),
        /* enStates= */ notTurndownStates,
        /* isOnboardingStateSetStates= */ Sets.newHashSet(true),
        /* isPrivateAnalyticsSetStates= */ Sets.newHashSet(false),
        /* isPrivateAnalyticsSupportedAndConfiguredStates= */ Sets.newHashSet(true),
        /* onboardAsMigratingUser= */ Sets.newHashSet(false),
        /* expected= */ OnboardingPrivateAnalyticsFragment.class);
  }

  @Test
  public void notEnabled_onboarded_notMigratingApp_returnsHomeFragment() {
    assertEnumeratedCases(
        /* isEnabledStates= */ Sets.newHashSet(false),
        /* enStates= */ notTurndownStates,
        /* isOnboardingStateSetStates= */ Sets.newHashSet(true),
        /* isPrivateAnalyticsSetStates= */ Sets.newHashSet(true, false),
        /* isPrivateAnalyticsSupportedAndConfiguredStates= */ Sets.newHashSet(true, false),
        /* onboardAsMigratingUser= */ Sets.newHashSet(false),
        /* expected= */ SinglePageHomeFragment.class);
  }

  @Test
  public void notEnabled_onboarded_onboardAsMigratingUser_returnsOnboardingPermissionDisabledFragment() {
    assertEnumeratedCases(
        /* isEnabledStates= */ Sets.newHashSet(false),
        /* enStates= */ notTurndownStates,
        /* isOnboardingStateSetStates= */ Sets.newHashSet(true),
        /* isPrivateAnalyticsSetStates= */ Sets.newHashSet(true, false),
        /* isPrivateAnalyticsSupportedAndConfiguredStates= */ Sets.newHashSet(true, false),
        /* onboardAsMigratingUser= */ Sets.newHashSet(true),
        /* expected= */ OnboardingPermissionDisabledFragment.class);
  }

  @Test
  public void enabled_onboarded_onboardAsMigratingUser_returnsOnboardingPermissionEnabledFragment() {
    assertEnumeratedCases(
        /* isEnabledStates= */ Sets.newHashSet(true),
        /* enStates= */ notTurndownStates,
        /* isOnboardingStateSetStates= */ Sets.newHashSet(true),
        /* isPrivateAnalyticsSetStates= */ Sets.newHashSet(true, false),
        /* isPrivateAnalyticsSupportedAndConfiguredStates= */ Sets.newHashSet(true, false),
        /* onboardAsMigratingUser= */ Sets.newHashSet(true),
        /* expected= */ OnboardingPermissionEnabledFragment.class);
  }

  @Test
  public void enabled_onboarded_notMigratingApp_ENPASetAndSupportedAndConfigured_returnsHomeFragment() {
    assertEnumeratedCases(
        /* isEnabledStates= */ Sets.newHashSet(true),
        /* enStates= */ notTurndownStates,
        /* isOnboardingStateSetStates= */ Sets.newHashSet(true),
        /* isPrivateAnalyticsSetStates= */ Sets.newHashSet(true),
        /* isPrivateAnalyticsSupportedAndConfiguredStates= */ Sets.newHashSet(true),
        /* onboardAsMigratingUser= */ Sets.newHashSet(false),
        /* expected= */ SinglePageHomeFragment.class);
  }

  @Test
  public void enabled_onboarded_notMigratingApp_ENPANotSupportedAndConfigured_returnsHomeFragment() {
    assertEnumeratedCases(
        /* isEnabledStates= */ Sets.newHashSet(true),
        /* enStates= */ notTurndownStates,
        /* isOnboardingStateSetStates= */ Sets.newHashSet(true),
        /* isPrivateAnalyticsSetStates= */ Sets.newHashSet(true, false),
        /* isPrivateAnalyticsSupportedAndConfiguredStates= */ Sets.newHashSet(false),
        /* onboardAsMigratingUser= */ Sets.newHashSet(false),
        /* expected= */ SinglePageHomeFragment.class);
  }

  @Test
  public void notEnabled_notOnboarded_enTurndownForRegion_returnsOnboardingEnTurndownForRegionFragment() {
    assertEnumeratedCases(
        /* isEnabledStates= */ Sets.newHashSet(false),
        /* enStates= */ Sets.newHashSet(ExposureNotificationState.PAUSED_NOT_IN_ALLOWLIST),
        /* isOnboardingStateSetStates= */ Sets.newHashSet(false),
        /* isPrivateAnalyticsSetStates= */ Sets.newHashSet(true, false),
        /* isPrivateAnalyticsSupportedAndConfiguredStates= */ Sets.newHashSet(true, false),
        /* onboardAsMigratingUser= */ Sets.newHashSet(true, false),
        /* expected= */ OnboardingEnTurndownForRegionFragment.class);
  }

  @Test
  public void notEnabled_notOnboarded_enTurndown_returnsOnboardingEnTurndownFragment() {
    assertEnumeratedCases(
        /* isEnabledStates= */ Sets.newHashSet(false),
        /* enStates= */ Sets.newHashSet(ExposureNotificationState.PAUSED_EN_NOT_SUPPORT),
        /* isOnboardingStateSetStates= */ Sets.newHashSet(false),
        /* isPrivateAnalyticsSetStates= */ Sets.newHashSet(true, false),
        /* isPrivateAnalyticsSupportedAndConfiguredStates= */ Sets.newHashSet(false),
        /* onboardAsMigratingUser= */ Sets.newHashSet(true, false),
        /* expected= */ OnboardingEnTurndownFragment.class);
  }

  /**
   * This method checks if the next fragment after SplashFragment is as expected using Sets of
   * booleans and {@link ExposureNotificationState} objects to test many scenarios under which
   * SplashNextFragmentLiveData is created.
   */
  private void assertEnumeratedCases(
      Set<Boolean> isEnabledStates,
      Set<ExposureNotificationState> enStates,
      Set<Boolean> isOnboardingStateSetStates,
      Set<Boolean> isPrivateAnalyticsSetStates,
      Set<Boolean> isPrivateAnalyticsSupportedAndConfiguredStates,
      Set<Boolean> onboardAsMigratingUserStates,
      Class expected) {
    for (boolean onboardAsMigratingUser : onboardAsMigratingUserStates) {
      SplashNextFragmentLiveData splashNextFragmentLiveData = SplashNextFragmentLiveData.create(
          isEnabledLiveData,
          enStateLiveData,
          isOnboardingStateSetLiveData,
          isPrivateAnalyticsSetLiveData,
          isPrivateAnalyticsSupportedAndConfiguredLiveData,
          onboardAsMigratingUser);
      AtomicReference<Fragment> current = new AtomicReference<>();
      splashNextFragmentLiveData.observeForever(current::set);

      Set<List<Boolean>> states =
          Sets.cartesianProduct(isEnabledStates, isOnboardingStateSetStates,
              isPrivateAnalyticsSetStates, isPrivateAnalyticsSupportedAndConfiguredStates);
      for (List<Boolean> state : states) {
        isEnabledLiveData.setValue(state.get(0));
        isOnboardingStateSetLiveData.setValue(state.get(1));
        isPrivateAnalyticsSetLiveData.setValue(state.get(2));
        isPrivateAnalyticsSupportedAndConfiguredLiveData.setValue(state.get(3));
        for (ExposureNotificationState enState : enStates) {
          enStateLiveData.setValue(enState);
          assertThat(current.get()).isInstanceOf(expected);
        }
      }
    }
  }

}