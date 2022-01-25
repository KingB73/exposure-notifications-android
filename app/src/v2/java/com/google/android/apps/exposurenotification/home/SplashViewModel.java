/*
 * Copyright 2020 Google LLC
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

import android.content.Context;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.android.apps.exposurenotification.home.ExposureNotificationViewModel.ExposureNotificationState;
import com.google.android.apps.exposurenotification.migrate.MigrationManager;
import com.google.android.apps.exposurenotification.storage.ExposureNotificationSharedPreferences;
import com.google.android.libraries.privateanalytics.PrivateAnalyticsEnabledProvider;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

/**
 * View model for the {@link SplashFragment}.
 */
@HiltViewModel
public class SplashViewModel extends ViewModel {

  private final ExposureNotificationSharedPreferences exposureNotificationSharedPreferences;
  private final PrivateAnalyticsEnabledProvider privateAnalyticsEnabledProvider;
  private final MigrationManager migrationManager;

  @Inject
  public SplashViewModel(
      ExposureNotificationSharedPreferences exposureNotificationSharedPreferences,
      PrivateAnalyticsEnabledProvider privateAnalyticsEnabledProvider,
      MigrationManager migrationManager) {
    this.exposureNotificationSharedPreferences = exposureNotificationSharedPreferences;
    this.privateAnalyticsEnabledProvider = privateAnalyticsEnabledProvider;
    this.migrationManager = migrationManager;
  }

  public LiveData<Fragment> getNextFragmentLiveData(
      LiveData<Boolean> enEnabledLiveData,
      LiveData<ExposureNotificationState> enStateLiveData,
      Context context) {
    return SplashNextFragmentLiveData.create(
        enEnabledLiveData,
        enStateLiveData,
        exposureNotificationSharedPreferences.isOnboardingStateSetLiveData(),
        exposureNotificationSharedPreferences.isPrivateAnalyticsStateSetLiveData(),
        new MutableLiveData<>(privateAnalyticsEnabledProvider.isSupportedByApp()),
        migrationManager.shouldOnboardAsMigratingUser(context));
  }

}
