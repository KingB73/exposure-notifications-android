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

package com.google.android.apps.exposurenotification.storage;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.AnyThread;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import com.google.android.apps.exposurenotification.common.BooleanSharedPreferenceLiveData;
import com.google.android.apps.exposurenotification.common.ContainsSharedPreferenceLiveData;
import com.google.android.apps.exposurenotification.common.SharedPreferenceLiveData;
import com.google.android.apps.exposurenotification.common.logging.Logger;
import com.google.android.apps.exposurenotification.common.time.Clock;
import com.google.android.apps.exposurenotification.home.ExposureNotificationViewModel.ExposureNotificationState;
import com.google.android.apps.exposurenotification.riskcalculation.ExposureClassification;
import com.google.android.apps.exposurenotification.storage.DiagnosisEntity.TestResult;
import com.google.common.base.Optional;
import java.security.SecureRandom;
import java.util.Calendar;
import org.threeten.bp.Duration;
import org.threeten.bp.Instant;

/**
 * Key value storage for ExposureNotification.
 *
 * <p>Partners should implement a daily TTL/expiry, for on-device storage of this data, and must
 * ensure compliance with all applicable laws and requirements with respect to encryption, storage,
 * and retention polices for end user data.
 */
public class ExposureNotificationSharedPreferences {

  private static final Logger logger = Logger.getLogger("Preferences");

  private static final String SHARED_PREFERENCES_FILE =
      "ExposureNotificationSharedPreferences.SHARED_PREFERENCES_FILE";

  private static final String ONBOARDING_STATE_KEY =
      "ExposureNotificationSharedPreferences.ONBOARDING_STATE_KEY";
  private static final String SHARE_ANALYTICS_KEY =
      "ExposureNotificationSharedPreferences.SHARE_ANALYTICS_KEY";
  private static final String IS_ENABLED_CACHE_KEY =
      "ExposureNotificationSharedPreferences.IS_ENABLED_CACHE_KEY";
  private static final String EN_STATE_CACHE_KEY =
      "ExposureNotificationSharedPreferences.EN_STATE_CACHE_KEY";
  private static final String ATTENUATION_THRESHOLD_1_KEY =
      "ExposureNotificationSharedPreferences.ATTENUATION_THRESHOLD_1_KEY";
  private static final String ATTENUATION_THRESHOLD_2_KEY =
      "ExposureNotificationSharedPreferences.ATTENUATION_THRESHOLD_2_KEY";
  private static final String EXPOSURE_CLASSIFICATION_INDEX_KEY =
      "ExposureNotificationSharedPreferences.EXPOSURE_CLASSIFICATION_INDEX_KEY";
  private static final String EXPOSURE_CLASSIFICATION_NAME_KEY =
      "ExposureNotificationSharedPreferences.EXPOSURE_CLASSIFICATION_NAME_KEY";
  private static final String EXPOSURE_CLASSIFICATION_DATE_KEY =
      "ExposureNotificationSharedPreferences.EXPOSURE_CLASSIFICATION_DATE_KEY";
  private static final String EXPOSURE_CLASSIFICATION_IS_REVOKED_KEY =
      "ExposureNotificationSharedPreferences.EXPOSURE_CLASSIFICATION_IS_REVOKED_KEY";
  private static final String EXPOSURE_CLASSIFICATION_IS_CLASSIFICATION_NEW_KEY =
      "ExposureNotificationSharedPreferences.EXPOSURE_CLASSIFICATION_IS_CLASSIFICATION_NEW_KEY";
  private static final String EXPOSURE_CLASSIFICATION_IS_DATE_NEW_KEY =
      "ExposureNotificationSharedPreferences.EXPOSURE_CLASSIFICATION_IS_DATE_NEW_KEY";
  private static final String ANALYTICS_LOGGING_LAST_TIMESTAMP =
      "ExposureNotificationSharedPreferences.ANALYTICS_LOGGING_LAST_TIMESTAMP";
  private static final String PROVIDED_DIAGNOSIS_KEY_HEX_TO_LOG_KEY =
      "ExposureNotificationSharedPreferences.PROVIDE_DIAGNOSIS_KEY_TO_LOG_KEY";
  private static final String HAS_PENDING_RESTORE_NOTIFICATION =
      "ExposureNotificationSharedPreferences.HAS_PENDING_RESTORE_NOTIFICATION";
  private static final String BLE_LOC_OFF_NOTIFICATION_SEEN =
      "ExposureNotificationSharedPreferences.BLE_LOC_OFF_NOTIFICATION_SEEN";
  private static final String BEGIN_TIMESTAMP_BLE_LOC_OFF =
      "ExposureNotificationSharedPreferences.BEGIN_TIMESTAMP_BLE_LOC_OFF";
  private static final String IS_IN_APP_SMS_NOTICE_SEEN =
      "ExposureNotificationSharedPreferences.IS_IN_APP_SMS_NOTICE_SEEN";
  private static final String IS_PLAY_SMS_NOTICE_SEEN =
      "ExposureNotificationSharedPreferences.IS_PLAY_SMS_NOTICE_SEEN";
  private static final String HAS_DISPLAYED_ONBOARDING_FOR_MIGRATING_USERS =
      "ExposureNotificationSharedPreferences.HAS_DISPLAYED_ONBOARDING_FOR_MIGRATING_USERS";
  // Private analytics
  private static final String SHARE_PRIVATE_ANALYTICS_KEY =
      "ExposureNotificationSharedPreferences.SHARE_PRIVATE_ANALYTICS_KEY";
  // The constant value uses the old constant name so that data is not lost when updating the app.
  private static final String PRIVATE_ANALYTICS_LAST_WORKER_RUN_TIME_FOR_DAILY =
      "ExposureNotificationSharedPreferences.PRIVATE_ANALYTICS_LAST_WORKER_RUN_TIME";
  private static final String PRIVATE_ANALYTICS_LAST_WORKER_RUN_TIME_FOR_BIWEEKLY =
      "ExposureNotificationSharedPreferences.PRIVATE_ANALYTICS_LAST_WORKER_RUN_TIME_FOR_BIWEEKLY";
  private static final String EXPOSURE_NOTIFICATION_LAST_SHOWN_TIME =
      "ExposureNotificationSharedPreferences.EXPOSURE_NOTIFICATION_LAST_SHOWN_TIME_KEY";
  private static final String EXPOSURE_NOTIFICATION_LAST_SHOWN_CLASSIFICATION =
      "ExposureNotificationSharedPreferences.EXPOSURE_NOTIFICATION_LAST_SHOWN_CLASSIFICATION_KEY";
  private static final String EXPOSURE_NOTIFICATION_LAST_INTERACTION_TIME =
      "ExposureNotificationSharedPreferences.EXPOSURE_NOTIFICATION_ACTIVE_INTERACTION_TIME_KEY";
  private static final String EXPOSURE_NOTIFICATION_LAST_INTERACTION_TYPE =
      "ExposureNotificationSharedPreferences.EXPOSURE_NOTIFICATION_ACTIVE_INTERACTION_TYPE_KEY";
  private static final String EXPOSURE_NOTIFICATION_LAST_INTERACTION_CLASSIFICATION =
      "ExposureNotificationSharedPreferences.EXPOSURE_NOTIFICATION_INTERACTION_CLASSIFICATION_KEY";
  private static final String PRIVATE_ANALYTICS_VERIFICATION_CODE_TIME =
      "ExposureNotificationSharedPreferences.PRIVATE_ANALYTICS_VERIFICATION_CODE_TIME";
  private static final String PRIVATE_ANALYTICS_SUBMITTED_KEYS_TIME =
      "ExposureNotificationSharedPreferences.PRIVATE_ANALYTICS_SUBMITTED_KEYS_TIME";
  private static final String PRIVATE_ANALYTICS_LAST_EXPOSURE_TIME =
      "ExposureNotificationSharedPreferences.PRIVATE_ANALYTICS_LAST_EXPOSURE_TIME";
  private static final String PRIVATE_ANALYTICS_LAST_REPORT_TYPE =
      "ExposureNotificationSharedPreferences.PRIVATE_ANALYTICS_LAST_REPORT_TYPE";
  private static final String EXPOSURE_NOTIFICATION_LAST_VACCINATION_STATUS =
      "ExposureNotificationSharedPreferences.EXPOSURE_NOTIFICATION_LAST_VACCINATION_STATUS";
  private static final String EXPOSURE_NOTIFICATION_LAST_VACCINATION_STATUS_RESPONSE_TIME_MS =
      "ExposureNotificationSharedPreferences.EXPOSURE_NOTIFICATION_LAST_VACCINATION_STATUS_TIME_MS";
  private static final String BIWEEKLY_METRICS_UPLOAD_DAY =
      "ExposureNotificationSharedPreferences.BIWEEKLY_METRICS_UPLOAD_DAY";

  private static final String MIGRATION_RUN_OR_NOT_NEEDED =
      "ExposureNotificationSharedPreferences.MIGRATION_RUN_OR_NOT_NEEDED";

  private final SharedPreferences sharedPreferences;
  private final Clock clock;
  private final SecureRandom random;
  private static AnalyticsStateListener analyticsStateListener;

  private final LiveData<Boolean> appAnalyticsStateLiveData;
  private final LiveData<Boolean> privateAnalyticsStateLiveData;
  private final LiveData<Boolean> isExposureClassificationRevokedLiveData;
  private final LiveData<Boolean> isOnboardingStateSetLiveData;
  private final LiveData<Boolean> isPrivateAnalyticsStateSetLiveData;
  private final LiveData<ExposureClassification> exposureClassificationLiveData;
  private final LiveData<BadgeStatus> isExposureClassificationNewLiveData;
  private final LiveData<BadgeStatus> isExposureClassificationDateNewLiveData;
  private final LiveData<String> providedDiagnosisKeyHexToLogLiveData;
  private final LiveData<Boolean> inAppSmsNoticeSeenLiveData;

  /**
   * Enum for onboarding status.
   */
  public enum OnboardingStatus {
    UNKNOWN(0),
    ONBOARDED(1),
    SKIPPED(2);

    private final int value;

    OnboardingStatus(int value) {
      this.value = value;
    }

    public int value() {
      return value;
    }

    public static OnboardingStatus fromValue(int value) {
      switch (value) {
        case 1:
          return ONBOARDED;
        case 2:
          return SKIPPED;
        default:
          return UNKNOWN;
      }
    }
  }

  /**
   * Enum for "new" badge status.
   */
  public enum BadgeStatus {
    NEW(0),
    SEEN(1),
    DISMISSED(2);

    private final int value;

    BadgeStatus(int value) {
      this.value = value;
    }

    public int value() {
      return value;
    }

    public static BadgeStatus fromValue(int value) {
      switch (value) {
        case 1:
          return SEEN;
        case 2:
          return DISMISSED;
        default:
          return NEW;
      }
    }
  }

  /**
   * Enum for Vaccination Status.
   */
  public enum VaccinationStatus {
    UNKNOWN(0),
    VACCINATED(1),
    NOT_VACCINATED(2);

    private final int value;

    VaccinationStatus(int value) {
      this.value = value;
    }

    public int value() {
      return value;
    }

    public static VaccinationStatus fromValue(int value) {
      switch (value) {
        case 1:
          return VACCINATED;
        case 2:
          return NOT_VACCINATED;
        default:
          return UNKNOWN;
      }
    }
  }

  /**
   * Enum for network handling.
   */
  public enum NetworkMode {
    // Uses live but test instances of the diagnosis verification, key upload and download servers.
    LIVE,
    // Bypasses diagnosis verification, key uploads and downloads; no actual network calls.
    // Useful to test other components of Exposure Notifications in isolation from the servers.
    DISABLED
  }

  /**
   * Enum for onboarding status.
   */
  public enum NotificationInteraction {
    UNKNOWN(0),
    CLICKED(1),
    DISMISSED(2);

    private final int value;

    NotificationInteraction(int value) {
      this.value = value;
    }

    public int value() {
      return value;
    }

    public static NotificationInteraction fromValue(int value) {
      switch (value) {
        case 1:
          return CLICKED;
        case 2:
          return DISMISSED;
        default:
          return UNKNOWN;
      }
    }
  }

  ExposureNotificationSharedPreferences(Context context, Clock clock, SecureRandom random) {
    // These shared preferences are stored in {@value Context#MODE_PRIVATE} to be made only
    // accessible by the app.
    sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
    this.clock = clock;
    this.random = random;

    this.appAnalyticsStateLiveData =
        new BooleanSharedPreferenceLiveData(sharedPreferences, SHARE_ANALYTICS_KEY, false);
    this.privateAnalyticsStateLiveData =
        new BooleanSharedPreferenceLiveData(sharedPreferences, SHARE_PRIVATE_ANALYTICS_KEY, false);
    this.isExposureClassificationRevokedLiveData =
        new BooleanSharedPreferenceLiveData(
            sharedPreferences, EXPOSURE_CLASSIFICATION_IS_REVOKED_KEY, false);
    this.isOnboardingStateSetLiveData =
        new ContainsSharedPreferenceLiveData(sharedPreferences, ONBOARDING_STATE_KEY);
    this.isPrivateAnalyticsStateSetLiveData =
        new ContainsSharedPreferenceLiveData(sharedPreferences, SHARE_PRIVATE_ANALYTICS_KEY);
    this.inAppSmsNoticeSeenLiveData = new BooleanSharedPreferenceLiveData(
        sharedPreferences, IS_IN_APP_SMS_NOTICE_SEEN, false);

    this.exposureClassificationLiveData =
        new SharedPreferenceLiveData<ExposureClassification>(
            this.sharedPreferences,
            EXPOSURE_CLASSIFICATION_INDEX_KEY,
            EXPOSURE_CLASSIFICATION_NAME_KEY,
            EXPOSURE_CLASSIFICATION_DATE_KEY) {
          @Override
          protected void updateValue() {
            setValue(getExposureClassification());
          }
        };

    this.isExposureClassificationNewLiveData = new SharedPreferenceLiveData<BadgeStatus>(
        this.sharedPreferences,
        EXPOSURE_CLASSIFICATION_IS_CLASSIFICATION_NEW_KEY) {
      @Override
      protected void updateValue() {
        setValue(getIsExposureClassificationNew());
      }
    };

    this.isExposureClassificationDateNewLiveData = new SharedPreferenceLiveData<BadgeStatus>(
        this.sharedPreferences,
        EXPOSURE_CLASSIFICATION_IS_DATE_NEW_KEY) {
      @Override
      protected void updateValue() {
        setValue(getIsExposureClassificationDateNew());
      }
    };

    this.providedDiagnosisKeyHexToLogLiveData = new SharedPreferenceLiveData<String>(
        this.sharedPreferences,
        PROVIDED_DIAGNOSIS_KEY_HEX_TO_LOG_KEY) {
      @Override
      protected void updateValue() {
        setValue(getProvidedDiagnosisKeyHexToLog());
      }
    };
  }

  public void setHasPendingRestoreNotificationState(boolean enabled) {
    sharedPreferences.edit().putBoolean(HAS_PENDING_RESTORE_NOTIFICATION, enabled).commit();
  }

  public boolean hasPendingRestoreNotification() {
    return sharedPreferences.getBoolean(HAS_PENDING_RESTORE_NOTIFICATION, false);
  }

  public void removeHasPendingRestoreNotificationState() {
    sharedPreferences.edit().remove(HAS_PENDING_RESTORE_NOTIFICATION).commit();
  }

  public void setOnboardedState(boolean onboardedState) {
    sharedPreferences
        .edit()
        .putInt(
            ONBOARDING_STATE_KEY,
            onboardedState ? OnboardingStatus.ONBOARDED.value() : OnboardingStatus.SKIPPED.value())
        .apply();
  }

  public OnboardingStatus getOnboardedState() {
    return OnboardingStatus.fromValue(sharedPreferences.getInt(ONBOARDING_STATE_KEY, 0));
  }

  public LiveData<Boolean> isOnboardingStateSetLiveData() {
    return Transformations.distinctUntilChanged(isOnboardingStateSetLiveData);
  }

  public LiveData<Boolean> getAppAnalyticsStateLiveData() {
    return appAnalyticsStateLiveData;
  }

  public void setAppAnalyticsState(boolean isEnabled) {
    sharedPreferences.edit().putBoolean(SHARE_ANALYTICS_KEY, isEnabled).commit();
    if (analyticsStateListener != null) {
      analyticsStateListener.onChanged(isEnabled);
    }
  }

  public synchronized void setAnalyticsStateListener(AnalyticsStateListener listener) {
    analyticsStateListener = listener;
  }

  public boolean getAppAnalyticsState() {
    return sharedPreferences.getBoolean(SHARE_ANALYTICS_KEY, false);
  }

  public Optional<Instant> maybeGetAnalyticsLoggingLastTimestamp() {
    if (!sharedPreferences.contains(ANALYTICS_LOGGING_LAST_TIMESTAMP)) {
      return Optional.absent();
    }
    return Optional.of(
        Instant.ofEpochMilli(sharedPreferences.getLong(ANALYTICS_LOGGING_LAST_TIMESTAMP, 0L)));
  }

  public void resetAnalyticsLoggingLastTimestamp() {
    sharedPreferences.edit().putLong(ANALYTICS_LOGGING_LAST_TIMESTAMP, clock.now().toEpochMilli())
        .commit();
  }

  public void clearAnalyticsLoggingLastTimestamp() {
    sharedPreferences.edit().remove(ANALYTICS_LOGGING_LAST_TIMESTAMP).commit();
  }

  public boolean isAppAnalyticsSet() {
    return sharedPreferences.contains(SHARE_ANALYTICS_KEY);
  }

  public LiveData<Boolean> getPrivateAnalyticsStateLiveData() {
    return privateAnalyticsStateLiveData;
  }

  public boolean getPrivateAnalyticState() {
    return sharedPreferences.getBoolean(SHARE_PRIVATE_ANALYTICS_KEY, false);
  }

  public void setPrivateAnalyticsState(boolean isEnabled) {
    logger.d("PrivateAnalyticsState changed, isEnabled= " + isEnabled);
    sharedPreferences.edit().putBoolean(SHARE_PRIVATE_ANALYTICS_KEY, isEnabled).commit();
  }

  public LiveData<Boolean> isPrivateAnalyticsStateSetLiveData() {
    return Transformations.distinctUntilChanged(isPrivateAnalyticsStateSetLiveData);
  }

  public boolean isPrivateAnalyticsStateSet() {
    return sharedPreferences.contains(SHARE_PRIVATE_ANALYTICS_KEY);
  }

  public int getAttenuationThreshold1(int defaultThreshold) {
    return sharedPreferences.getInt(ATTENUATION_THRESHOLD_1_KEY, defaultThreshold);
  }

  public void setAttenuationThreshold1(int threshold) {
    sharedPreferences.edit().putInt(ATTENUATION_THRESHOLD_1_KEY, threshold).commit();
  }

  public int getAttenuationThreshold2(int defaultThreshold) {
    return sharedPreferences.getInt(ATTENUATION_THRESHOLD_2_KEY, defaultThreshold);
  }

  public boolean getIsEnabledCache() {
    return sharedPreferences.getBoolean(IS_ENABLED_CACHE_KEY, false);
  }

  public void setIsEnabledCache(boolean isEnabled) {
    sharedPreferences.edit().putBoolean(IS_ENABLED_CACHE_KEY, isEnabled).apply();
  }

  public int getEnStateCache() {
    return sharedPreferences.getInt(
        EN_STATE_CACHE_KEY, ExposureNotificationState.DISABLED.ordinal());
  }

  public void setEnStateCache(int enState) {
    sharedPreferences.edit().putInt(EN_STATE_CACHE_KEY, enState).apply();
  }

  public void setExposureClassification(ExposureClassification exposureClassification) {
    sharedPreferences
        .edit()
        .putInt(
            EXPOSURE_CLASSIFICATION_INDEX_KEY,
            exposureClassification.getClassificationIndex())
        .putString(
            EXPOSURE_CLASSIFICATION_NAME_KEY,
            exposureClassification.getClassificationName()
        )
        .putLong(
            EXPOSURE_CLASSIFICATION_DATE_KEY,
            exposureClassification.getClassificationDate()
        )
        .commit();
  }

  public void deleteExposureInformation() {
    sharedPreferences.edit()
        .remove(EXPOSURE_CLASSIFICATION_INDEX_KEY)
        .remove(EXPOSURE_CLASSIFICATION_NAME_KEY)
        .remove(EXPOSURE_CLASSIFICATION_DATE_KEY)
        .remove(EXPOSURE_CLASSIFICATION_IS_REVOKED_KEY)
        .remove(EXPOSURE_CLASSIFICATION_IS_CLASSIFICATION_NEW_KEY)
        .remove(EXPOSURE_CLASSIFICATION_IS_DATE_NEW_KEY)
        .commit();
  }

  public ExposureClassification getExposureClassification() {
    return ExposureClassification.create(
        sharedPreferences.getInt(EXPOSURE_CLASSIFICATION_INDEX_KEY,
            ExposureClassification.NO_EXPOSURE_CLASSIFICATION_INDEX),
        sharedPreferences.getString(EXPOSURE_CLASSIFICATION_NAME_KEY,
            ExposureClassification.NO_EXPOSURE_CLASSIFICATION_NAME),
        sharedPreferences.getLong(EXPOSURE_CLASSIFICATION_DATE_KEY,
            ExposureClassification.NO_EXPOSURE_CLASSIFICATION_DATE));
  }

  public LiveData<ExposureClassification> getExposureClassificationLiveData() {
    return exposureClassificationLiveData;
  }

  public void setIsExposureClassificationRevoked(boolean isRevoked) {
    sharedPreferences.edit().putBoolean(EXPOSURE_CLASSIFICATION_IS_REVOKED_KEY, isRevoked).commit();
  }

  public boolean getIsExposureClassificationRevoked() {
    return sharedPreferences.getBoolean(EXPOSURE_CLASSIFICATION_IS_REVOKED_KEY, false);
  }

  public LiveData<Boolean> getIsExposureClassificationRevokedLiveData() {
    return isExposureClassificationRevokedLiveData;
  }

  public void setIsExposureClassificationNewAsync(BadgeStatus badgeStatus) {
    sharedPreferences.edit()
        .putInt(EXPOSURE_CLASSIFICATION_IS_CLASSIFICATION_NEW_KEY, badgeStatus.value()).apply();
  }

  // Vaccine Status for Private Analytics
  public void setLastVaccinationResponse(Instant responseTime,
      VaccinationStatus vaccinationStatus) {
    if (getPrivateAnalyticState()) {
      sharedPreferences.edit()
          .putInt(EXPOSURE_NOTIFICATION_LAST_VACCINATION_STATUS, vaccinationStatus.value())
          .putLong(EXPOSURE_NOTIFICATION_LAST_VACCINATION_STATUS_RESPONSE_TIME_MS,
              responseTime.toEpochMilli())
          .apply();
    }
  }

  public VaccinationStatus getLastVaccinationStatus() {
    return VaccinationStatus
        .fromValue(sharedPreferences.getInt(EXPOSURE_NOTIFICATION_LAST_VACCINATION_STATUS,
            VaccinationStatus.UNKNOWN.value()));
  }

  public Instant getLastVaccinationStatusResponseTime() {
    return Instant
        .ofEpochMilli(sharedPreferences.getLong(
            EXPOSURE_NOTIFICATION_LAST_VACCINATION_STATUS_RESPONSE_TIME_MS, 0L));
  }

  /*
   * If bleLocNotificationSeen is set to true, we clear BEGIN_TIMESTAMP_BLE_LOC_OFF, too,
   * since it is not required anymore.
   */
  public void setBleLocNotificationSeen(boolean bleLocNotificationSeen) {
    sharedPreferences.edit()
        .putBoolean(BLE_LOC_OFF_NOTIFICATION_SEEN, bleLocNotificationSeen).apply();
    if (bleLocNotificationSeen) {
      sharedPreferences.edit().remove(BEGIN_TIMESTAMP_BLE_LOC_OFF).apply();
    }
  }

  public boolean getBleLocNotificationSeen() {
    return sharedPreferences.getBoolean(BLE_LOC_OFF_NOTIFICATION_SEEN, false);
  }

  /*
   * Sets a timestamp for the first time we see Ble/Location off.
   * If beginTimestampBleLocOff is Optional.absent(), any previously stored value is cleared.
   */
  public void setBeginTimestampBleLocOff(Optional<Instant> beginTimestampBleLocOff) {
    if (beginTimestampBleLocOff.isPresent()) {
      sharedPreferences.edit()
          .putLong(BEGIN_TIMESTAMP_BLE_LOC_OFF, beginTimestampBleLocOff.get().toEpochMilli())
          .apply();
    } else {
      sharedPreferences.edit().remove(BEGIN_TIMESTAMP_BLE_LOC_OFF).apply();
    }
  }

  public Optional<Instant> getBeginTimestampBleLocOff() {
    long longBeginTimestampBleLocOff = sharedPreferences.getLong(
        BEGIN_TIMESTAMP_BLE_LOC_OFF, -1L);
    return longBeginTimestampBleLocOff != -1
        ? Optional.of(Instant.ofEpochMilli(longBeginTimestampBleLocOff))
        : Optional.absent();
  }

  // Notifications for Private Analytics.
  public void setExposureNotificationLastShownClassification(Instant exposureNotificationTime,
      ExposureClassification exposureClassification) {
    if (getPrivateAnalyticState() && exposureClassification.getClassificationIndex() > 0) {
      sharedPreferences.edit()
          .putInt(EXPOSURE_NOTIFICATION_LAST_SHOWN_CLASSIFICATION,
              exposureClassification.getClassificationIndex())
          .putLong(EXPOSURE_NOTIFICATION_LAST_SHOWN_TIME, exposureNotificationTime.toEpochMilli())
          .putLong(PRIVATE_ANALYTICS_LAST_EXPOSURE_TIME,
              exposureClassification.getClassificationDate())
          .apply();
    }
  }

  public int getExposureNotificationLastShownClassification() {
    return sharedPreferences.getInt(EXPOSURE_NOTIFICATION_LAST_SHOWN_CLASSIFICATION,
        ExposureClassification.NO_EXPOSURE_CLASSIFICATION_INDEX);
  }

  public Instant getExposureNotificationLastShownTime() {
    return Instant
        .ofEpochMilli(sharedPreferences.getLong(EXPOSURE_NOTIFICATION_LAST_SHOWN_TIME, 0L));
  }

  public Instant getPrivateAnalyticsLastExposureTime() {
    // The date of exposure is stored at a day granularity
    return Instant.EPOCH
        .plus(Duration.ofDays(sharedPreferences.getLong(PRIVATE_ANALYTICS_LAST_EXPOSURE_TIME, 0L)));
  }

  // Interaction for Private Analytics.
  public void setExposureNotificationLastInteraction(Instant exposureNotificationInteractionTime,
      NotificationInteraction interaction,
      int classificationIndex) {
    if (getPrivateAnalyticState() && classificationIndex > 0) {
      sharedPreferences.edit()
          .putLong(EXPOSURE_NOTIFICATION_LAST_INTERACTION_TIME,
              exposureNotificationInteractionTime.toEpochMilli())
          .putInt(EXPOSURE_NOTIFICATION_LAST_INTERACTION_TYPE, interaction.value())
          .putInt(EXPOSURE_NOTIFICATION_LAST_INTERACTION_CLASSIFICATION, classificationIndex)
          .apply();
    }
  }

  public Instant getExposureNotificationLastInteractionTime() {
    return Instant
        .ofEpochMilli(sharedPreferences.getLong(EXPOSURE_NOTIFICATION_LAST_INTERACTION_TIME, 0));
  }

  public NotificationInteraction getExposureNotificationLastInteractionType() {
    return NotificationInteraction.fromValue(sharedPreferences.getInt(
        EXPOSURE_NOTIFICATION_LAST_INTERACTION_TYPE,
        NotificationInteraction.UNKNOWN.value()));
  }

  public int getExposureNotificationLastInteractionClassification() {
    return sharedPreferences.getInt(EXPOSURE_NOTIFICATION_LAST_INTERACTION_CLASSIFICATION,
        ExposureClassification.NO_EXPOSURE_CLASSIFICATION_INDEX);
  }

  // Verification code time for Private Analytics.
  public void setPrivateAnalyticsLastSubmittedCodeTime(Instant submittedCodeTime) {
    if (getPrivateAnalyticState()) {
      sharedPreferences.edit()
          .putLong(PRIVATE_ANALYTICS_VERIFICATION_CODE_TIME,
              submittedCodeTime.toEpochMilli()).apply();
    }
  }

  public Instant getPrivateAnalyticsLastSubmittedCodeTime() {
    return Instant
        .ofEpochMilli(sharedPreferences.getLong(PRIVATE_ANALYTICS_VERIFICATION_CODE_TIME, 0));
  }

  // Submitted keys time for Private Analytics.
  public void setPrivateAnalyticsLastSubmittedKeysTime(Instant submittedCodeTime) {
    if (getPrivateAnalyticState()) {
      sharedPreferences.edit()
          .putLong(PRIVATE_ANALYTICS_SUBMITTED_KEYS_TIME,
              submittedCodeTime.toEpochMilli()).apply();
    }
  }

  public Instant getPrivateAnalyticsLastSubmittedKeysTime() {
    return Instant
        .ofEpochMilli(sharedPreferences.getLong(PRIVATE_ANALYTICS_SUBMITTED_KEYS_TIME, 0));
  }

  // Last report type for Private Analytics.
  public void setPrivateAnalyticsLastReportType(@Nullable TestResult testResult) {
    if (getPrivateAnalyticState()) {
      int testResultOrdinal = testResult != null ? testResult.ordinal() : -1;
      sharedPreferences.edit().putInt(PRIVATE_ANALYTICS_LAST_REPORT_TYPE, testResultOrdinal)
          .apply();
    }
  }

  @Nullable
  public TestResult getPrivateAnalyticsLastReportType() {
    int testResultOrdinal = sharedPreferences.getInt(PRIVATE_ANALYTICS_LAST_REPORT_TYPE, -1);

    if (testResultOrdinal < 0 || testResultOrdinal >= TestResult.values().length) {
      return null;
    }
    return TestResult.values()[testResultOrdinal];
  }

  // Clear the Private Analytics fields.
  public void clearPrivateAnalyticsFields() {
    sharedPreferences.edit()
        .remove(EXPOSURE_NOTIFICATION_LAST_INTERACTION_TIME)
        .remove(EXPOSURE_NOTIFICATION_LAST_INTERACTION_TYPE)
        .remove(EXPOSURE_NOTIFICATION_LAST_INTERACTION_CLASSIFICATION)
        .remove(EXPOSURE_NOTIFICATION_LAST_SHOWN_TIME)
        .remove(EXPOSURE_NOTIFICATION_LAST_SHOWN_CLASSIFICATION)
        .remove(PRIVATE_ANALYTICS_LAST_EXPOSURE_TIME)
        .remove(PRIVATE_ANALYTICS_LAST_REPORT_TYPE)
        .remove(PRIVATE_ANALYTICS_VERIFICATION_CODE_TIME)
        .remove(PRIVATE_ANALYTICS_SUBMITTED_KEYS_TIME)
        .remove(EXPOSURE_NOTIFICATION_LAST_VACCINATION_STATUS)
        .remove(EXPOSURE_NOTIFICATION_LAST_VACCINATION_STATUS_RESPONSE_TIME_MS)
        .apply();
  }

  public void clearPrivateAnalyticsDailyFieldsBefore(Instant date) {
    SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
    if (getExposureNotificationLastShownTime().isBefore(date)) {
      sharedPreferencesEditor.remove(PRIVATE_ANALYTICS_LAST_EXPOSURE_TIME);
    }
    if (getExposureNotificationLastInteractionTime().isBefore(date)) {
      sharedPreferencesEditor.remove(EXPOSURE_NOTIFICATION_LAST_INTERACTION_TIME)
          .remove(EXPOSURE_NOTIFICATION_LAST_INTERACTION_TYPE)
          .remove(EXPOSURE_NOTIFICATION_LAST_INTERACTION_CLASSIFICATION);
    }
    sharedPreferencesEditor.apply();
  }

  public void clearPrivateAnalyticsBiweeklyFieldsBefore(Instant date) {
    SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
    if (getExposureNotificationLastShownTime().isBefore(date)) {
      sharedPreferencesEditor.remove(EXPOSURE_NOTIFICATION_LAST_SHOWN_TIME)
          .remove(EXPOSURE_NOTIFICATION_LAST_SHOWN_CLASSIFICATION);
    }
    if (getPrivateAnalyticsLastSubmittedCodeTime().isBefore(date)) {
      sharedPreferencesEditor.remove(PRIVATE_ANALYTICS_VERIFICATION_CODE_TIME)
          .remove(PRIVATE_ANALYTICS_LAST_REPORT_TYPE);
    }
    if (getPrivateAnalyticsLastSubmittedKeysTime().isBefore(date)) {
      sharedPreferencesEditor.remove(PRIVATE_ANALYTICS_SUBMITTED_KEYS_TIME);
    }
    if (getLastVaccinationStatusResponseTime().isBefore(date)) {
      sharedPreferencesEditor
          .remove(EXPOSURE_NOTIFICATION_LAST_VACCINATION_STATUS_RESPONSE_TIME_MS);
      sharedPreferencesEditor.remove(EXPOSURE_NOTIFICATION_LAST_VACCINATION_STATUS);
    }
    sharedPreferencesEditor.apply();
  }


  /**
   * Returns the last time the private analytics worker ran all daily metrics.
   * <p>
   * NB: The existence of this value only means the private analytics are enabled in the
   * configuration, and does not indicate whether the user enabled or disabled private analytics. It
   * only captures when the worker has been running last (it aborts early when the user opted out of
   * private analytics).
   */
  public void setPrivateAnalyticsWorkerLastTimeForDaily(Instant privateAnalyticsWorkerTime) {
    if (getPrivateAnalyticState()) {
      sharedPreferences.edit().putLong(PRIVATE_ANALYTICS_LAST_WORKER_RUN_TIME_FOR_DAILY,
          privateAnalyticsWorkerTime.toEpochMilli()).apply();
    }
  }

  /**
   * Returns the last time the private analytics worker ran all biweekly metrics.
   * <p>
   * NB: The existence of this value only means the private analytics are enabled in the
   * configuration, and does not indicate whether the user enabled or disabled private analytics. It
   * only captures when the worker has been running last (it aborts early when the user opted out of
   * private analytics).
   */
  public void setPrivateAnalyticsWorkerLastTimeForBiweekly(Instant privateAnalyticsWorkerTime) {
    if (getPrivateAnalyticState()) {
      sharedPreferences.edit().putLong(PRIVATE_ANALYTICS_LAST_WORKER_RUN_TIME_FOR_BIWEEKLY,
          privateAnalyticsWorkerTime.toEpochMilli()).apply();
    }
  }

  public Instant getPrivateAnalyticsWorkerLastTimeForDaily() {
    return Instant
        .ofEpochMilli(
            sharedPreferences.getLong(PRIVATE_ANALYTICS_LAST_WORKER_RUN_TIME_FOR_DAILY, 0));
  }

  public Instant getPrivateAnalyticsWorkerLastTimeForBiweekly() {
    return Instant
        .ofEpochMilli(
            sharedPreferences.getLong(PRIVATE_ANALYTICS_LAST_WORKER_RUN_TIME_FOR_BIWEEKLY, 0));
  }

  public BadgeStatus getIsExposureClassificationNew() {
    return BadgeStatus.fromValue(
        sharedPreferences
            .getInt(EXPOSURE_CLASSIFICATION_IS_CLASSIFICATION_NEW_KEY, BadgeStatus.NEW.value()));
  }

  public LiveData<BadgeStatus> getIsExposureClassificationNewLiveData() {
    return isExposureClassificationNewLiveData;
  }

  public void setIsExposureClassificationDateNewAsync(BadgeStatus badgeStatus) {
    sharedPreferences.edit()
        .putInt(EXPOSURE_CLASSIFICATION_IS_DATE_NEW_KEY, badgeStatus.value()).apply();
  }

  public BadgeStatus getIsExposureClassificationDateNew() {
    return BadgeStatus.fromValue(
        sharedPreferences
            .getInt(EXPOSURE_CLASSIFICATION_IS_DATE_NEW_KEY, BadgeStatus.NEW.value()));
  }

  public LiveData<BadgeStatus> getIsExposureClassificationDateNewLiveData() {
    return isExposureClassificationDateNewLiveData;
  }

  public void setProvidedDiagnosisKeyHexToLog(String keyHex) {
    sharedPreferences.edit()
        .putString(PROVIDED_DIAGNOSIS_KEY_HEX_TO_LOG_KEY, keyHex)
        .commit();
  }

  public String getProvidedDiagnosisKeyHexToLog() {
    return sharedPreferences.getString(PROVIDED_DIAGNOSIS_KEY_HEX_TO_LOG_KEY, "");
  }

  public LiveData<String> getProvidedDiagnosisKeyHexToLogLiveData() {
    return providedDiagnosisKeyHexToLogLiveData;
  }

  @AnyThread
  public void markInAppSmsNoticeSeenAsync() {
    sharedPreferences.edit().putBoolean(IS_IN_APP_SMS_NOTICE_SEEN, true).apply();
  }

  @WorkerThread
  public void markInAppSmsNoticeSeen() {
    sharedPreferences.edit().putBoolean(IS_IN_APP_SMS_NOTICE_SEEN, true)
        .commit();
  }

  public boolean isInAppSmsNoticeSeen() {
    return sharedPreferences.getBoolean(IS_IN_APP_SMS_NOTICE_SEEN, false);
  }

  public LiveData<Boolean> isInAppSmsNoticeSeenLiveData() {
    return inAppSmsNoticeSeenLiveData;
  }

  @AnyThread
  public void setPlaySmsNoticeSeenAsync(boolean isSeen) {
    sharedPreferences.edit().putBoolean(IS_PLAY_SMS_NOTICE_SEEN, isSeen).apply();
  }

  @WorkerThread
  public void setPlaySmsNoticeSeen(boolean isSeen) {
    sharedPreferences.edit().putBoolean(IS_PLAY_SMS_NOTICE_SEEN, isSeen)
        .commit();
  }

  public boolean isPlaySmsNoticeSeen() {
    return sharedPreferences.getBoolean(IS_PLAY_SMS_NOTICE_SEEN, false);
  }

  public void setBiweeklyMetricsUploadDay(Calendar calendar) {
    // We want the SharedPreferences field to match:
    // field % 7 + 1 == calendar.day_of_week
    // and field % 2 == calendar.week_of_year % 2
    int weekDayIndex = calendar.get(Calendar.DAY_OF_WEEK) - 1;
    int weekNumberParity = calendar.get(Calendar.WEEK_OF_YEAR) % 2;
    sharedPreferences.edit().putInt(BIWEEKLY_METRICS_UPLOAD_DAY,
        weekDayIndex + weekNumberParity * 7).apply();
  }

  public int getBiweeklyMetricsUploadDay() {
    // we want to upload some metrics biweekly, but don't want everyone to upload on the same day
    // (for example, when the new code gets rolled out), so instead we pick and memorize a random
    // fortnightly day.
    if (!sharedPreferences.contains(BIWEEKLY_METRICS_UPLOAD_DAY)) {
      // Pick a value between 0 and 13 included
      // Day of week will be (value % 7 + 1) (Calendar DAY_OF_WEEK has values between 1 and 7)
      // and we only upload if current week number % 2 == value / 7
      int randomDay = random.nextInt(14);
      sharedPreferences.edit().putInt(BIWEEKLY_METRICS_UPLOAD_DAY, randomDay).commit();
    }
    return sharedPreferences.getInt(BIWEEKLY_METRICS_UPLOAD_DAY, 0);
  }

  public boolean isMigrationRunOrNotNeeded() {
    return sharedPreferences.getBoolean(MIGRATION_RUN_OR_NOT_NEEDED, false);
  }

  public void markMigrationAsRunOrNotNeeded() {
    sharedPreferences.edit().putBoolean(MIGRATION_RUN_OR_NOT_NEEDED, true).apply();
  }

  @AnyThread
  public void markMigratingUserAsOnboardedAsync() {
    sharedPreferences.edit().putBoolean(HAS_DISPLAYED_ONBOARDING_FOR_MIGRATING_USERS, true).apply();
  }

  public boolean isMigratingUserOnboarded() {
    return sharedPreferences.getBoolean(HAS_DISPLAYED_ONBOARDING_FOR_MIGRATING_USERS, false);
  }

  public interface AnalyticsStateListener {

    void onChanged(boolean analyticsEnabled);
  }

}
