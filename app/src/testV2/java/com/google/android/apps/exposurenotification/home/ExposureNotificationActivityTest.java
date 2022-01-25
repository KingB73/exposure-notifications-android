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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import android.content.Context;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.work.Configuration;
import androidx.work.WorkManager;
import androidx.work.impl.utils.SynchronousExecutor;
import androidx.work.testing.WorkManagerTestInitHelper;
import com.google.android.libraries.privateanalytics.PrivateAnalyticsRemoteConfig;
import com.google.android.libraries.privateanalytics.RemoteConfigs;
import com.google.android.apps.exposurenotification.testsupport.ExposureNotificationRules;
import com.google.common.util.concurrent.Futures;
import com.google.firebase.FirebaseApp;
import dagger.hilt.android.testing.HiltAndroidTest;
import dagger.hilt.android.testing.HiltTestApplication;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

/**
 * Tests of {@link ExposureNotificationActivity}.
 */
@HiltAndroidTest
@LooperMode(LooperMode.Mode.PAUSED)
@RunWith(RobolectricTestRunner.class)
@Config(application = HiltTestApplication.class)
public class ExposureNotificationActivityTest {

  private final Context context = ApplicationProvider.getApplicationContext();

  @Mock
  PrivateAnalyticsRemoteConfig privateAnalyticsRemoteConfig;

  @Rule
  public ExposureNotificationRules rules =
      ExposureNotificationRules.forTest(this).withMocks().build();

  WorkManager workManager;

  @Before
  public void setUp() {
    Configuration config = new Configuration.Builder()
        .setExecutor(new SynchronousExecutor())
        .build();
    WorkManagerTestInitHelper.initializeTestWorkManager(
        context, config);
    workManager = WorkManager.getInstance(context);
  }

  @Test
  public void setupActivity_isNotNull() {
    FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext());
    when(privateAnalyticsRemoteConfig.fetchUpdatedConfigs()).thenReturn(Futures.immediateFuture(
        RemoteConfigs.newBuilder().build()));
    ActivityScenario<ExposureNotificationActivity> scenario = ActivityScenario
        .launch(ExposureNotificationActivity.class);
    scenario.onActivity(activity -> assertThat(activity).isNotNull());
  }

}