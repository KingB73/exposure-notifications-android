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

package com.google.android.apps.exposurenotification.common;

import static com.google.android.apps.exposurenotification.common.StringUtils.ELLIPSIS;
import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.style.BulletSpan;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.apps.exposurenotification.common.time.Clock;
import com.google.android.apps.exposurenotification.common.time.RealTimeModule;
import com.google.android.apps.exposurenotification.riskcalculation.ExposureClassification;
import com.google.android.apps.exposurenotification.testsupport.ExposureNotificationRules;
import com.google.android.apps.exposurenotification.testsupport.FakeClock;
import dagger.hilt.android.testing.BindValue;
import dagger.hilt.android.testing.HiltAndroidTest;
import dagger.hilt.android.testing.HiltTestApplication;
import dagger.hilt.android.testing.UninstallModules;
import java.util.Locale;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.threeten.bp.Duration;
import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.OffsetDateTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;

/**
 * Tests for {@link StringUtils} utility function helper class.
 */
@RunWith(AndroidJUnit4.class)
@HiltAndroidTest
@Config(application = HiltTestApplication.class)
@UninstallModules({RealTimeModule.class})
public class StringUtilsTest {

  private static final long TIMESTAMP_APR_10_MS = 1586476800000L;

  @Rule
  public ExposureNotificationRules rules = ExposureNotificationRules.forTest(this).withMocks()
      .build();

  @BindValue
  Clock clock = new FakeClock();

  Context context = ApplicationProvider.getApplicationContext();

  @Before
  public void setup() {
    rules.hilt().inject();
  }

  @Test
  public void epochTimestampToMediumUTCDateString_localeUS() {
    String formattedDate = StringUtils
        .epochTimestampToMediumUTCDateString(TIMESTAMP_APR_10_MS, Locale.US);

    assertThat(formattedDate).isEqualTo("April 10, 2020");
  }

  @Test
  public void epochTimestampToLongUTCDateString_localeUS() {
    String formattedDate = StringUtils
        .epochTimestampToLongUTCDateTimeString(TIMESTAMP_APR_10_MS, Locale.US);

    assertThat(formattedDate).isEqualTo("2020-04-10, 00:00:00 UTC");
  }

  @Test
  public void epochTimestampToMediumUTCDateString_localeFRANCE() {
    String formattedDate = StringUtils
        .epochTimestampToMediumUTCDateString(TIMESTAMP_APR_10_MS, Locale.FRANCE);

    assertThat(formattedDate).isEqualTo("10 avril 2020");
  }

  @Test
  public void epochTimestampToMediumUTCDateString_localeJAPAN() {
    String formattedDate = StringUtils
        .epochTimestampToMediumUTCDateString(TIMESTAMP_APR_10_MS, Locale.JAPAN);

    assertThat(formattedDate).isEqualTo("2020年4月10日");
  }

  @Test
  public void calculateTransitionResolutionMs_zonePST() {
    // GIVEN
    ((FakeClock) clock).setZoneId(ZoneId.of("America/Los_Angeles"));
    Instant now = clock.now(); // Mar 3, 1973 at 09:46:40 (UTC date & time)
    ZonedDateTime zonedNow = clock.zonedNow(); // Mar 3, 1973 at 01:46:40 (PST date & time, UTC-08)
    /*
     * StringUtils.calculateTransitionResolutionMs() returns transition resolution, which is time
     * elapsed since yesterday's midnight (in ms) for a given zone. So, as for the PST zone (UTC-08)
     * the current time is 01:46:40, the transition resolution is 24 hours (time elapsed for
     * yesterday) plus 1 hour, 46 minutes, and 40 seconds (time elapsed for today).
     */
    long expectedTransitionResolutionMs =
        Duration.ofHours(25).plus(Duration.ofMinutes(46)).plus(Duration.ofSeconds(40)).toMillis();

    // WHEN
    long transitionResolutionMs = StringUtils.calculateTransitionResolutionMs(now, zonedNow);

    // THEN
    assertThat(transitionResolutionMs).isEqualTo(expectedTransitionResolutionMs);
  }

  @Test
  public void calculateTransitionResolutionMs_zoneJST() {
    // GIVEN
    ((FakeClock) clock).setZoneId(ZoneId.of("Asia/Tokyo"));
    Instant now = clock.now(); // Mar 3, 1973 at 09:46:40 (UTC date & time)
    ZonedDateTime zonedNow = clock.zonedNow(); // Mar 3, 1973 at 18:46:40 (JST date & time, UTC+09)
    /*
     * StringUtils.calculateTransitionResolutionMs() returns transition resolution, which is time
     * elapsed since yesterday's midnight (in ms) for a given zone. So, as for the JST zone (UTC+09)
     * the current time is 18:46:40, the transition resolution is 24 hours (time elapsed for
     * yesterday) plus 18 hours, 46 minutes, and 40 seconds (time elapsed for today).
     */
    long expectedTransitionResolutionMs =
        Duration.ofHours(42).plus(Duration.ofMinutes(46)).plus(Duration.ofSeconds(40)).toMillis();

    // WHEN
    long transitionResolutionMs = StringUtils.calculateTransitionResolutionMs(now, zonedNow);

    // THEN
    assertThat(transitionResolutionMs).isEqualTo(expectedTransitionResolutionMs);
  }

  @Test
  public void randomBase64Data_zeroLength() {
    String base64Data = StringUtils.randomBase64Data(0);

    assertThat(base64Data).hasLength(0);
  }

  @Test
  public void randomBase64Data_notSame() {
    String base64Data1 = StringUtils.randomBase64Data(100);
    String base64Data2 = StringUtils.randomBase64Data(100);

    assertThat(base64Data1).isNotEqualTo(base64Data2);
  }

  @Test
  public void truncateWithEllipsis_normal() {
    String truncated = StringUtils.truncateWithEllipsis("1234567890", 7);

    assertThat(truncated).isEqualTo("123456" + ELLIPSIS);
  }

  @Test
  public void truncateWithEllipsis_lengthLessThanOne() {
    String truncated = StringUtils.truncateWithEllipsis("1234567890", 0);

    assertThat(truncated).isEqualTo(ELLIPSIS);
  }

  @Test
  public void calculateDaysFromStartOfExposure_6Feb3pm_9Feb3pm_3days() {
    Instant exposure = roundOffZonedDateTimeToUTCStartOfDay(ZonedDateTime
        .of(2021, 2 ,6, 15,0,0,0, ZoneId.of("America/Los_Angeles")));
    Instant now = roundOffZonedDateTimeToUTCStartOfDay(ZonedDateTime
        .of(2021, 2 ,9, 15,0,0,0, ZoneId.of("America/Los_Angeles")));

    long timeSinceExposure = StringUtils.calculateDaysFromStartOfExposure(exposure, now);

    assertThat(timeSinceExposure).isEqualTo(3);
  }

  @Test
  public void calculateDaysFromStartOfExposure_6Feb3pm_9Feb6pm_4days() {
    Instant exposure = roundOffZonedDateTimeToUTCStartOfDay(ZonedDateTime
        .of(2021, 2 ,6, 15,0,0,0, ZoneId.of("America/Los_Angeles")));
    Instant now = roundOffZonedDateTimeToUTCStartOfDay(ZonedDateTime
        .of(2021, 2 ,9, 18,0,0,0, ZoneId.of("America/Los_Angeles")));

    long timeSinceExposure = StringUtils.calculateDaysFromStartOfExposure(exposure, now);

    assertThat(timeSinceExposure).isEqualTo(4);
  }

  @Test
  public void calculateDaysFromStartOfExposure_6Feb6pm_9Feb3pm_2days() {
    Instant exposure = roundOffZonedDateTimeToUTCStartOfDay(ZonedDateTime
        .of(2021, 2 ,6, 18,0,0,0, ZoneId.of("America/Los_Angeles")));
    Instant now = roundOffZonedDateTimeToUTCStartOfDay(ZonedDateTime
        .of(2021, 2 ,9, 15,0,0,0, ZoneId.of("America/Los_Angeles")));

    long timeSinceExposure = StringUtils.calculateDaysFromStartOfExposure(exposure, now);

    assertThat(timeSinceExposure).isEqualTo(2);
  }

  @Test
  public void calculateDaysFromStartOfExposure_6Feb6pm_9Feb6pm_3days() {
    Instant exposure = roundOffZonedDateTimeToUTCStartOfDay(ZonedDateTime
        .of(2021, 2 ,6, 18,0,0,0, ZoneId.of("America/Los_Angeles")));
    Instant now = roundOffZonedDateTimeToUTCStartOfDay(ZonedDateTime
        .of(2021, 2 ,9, 18,0,0,0, ZoneId.of("America/Los_Angeles")));

    long timeSinceExposure = StringUtils.calculateDaysFromStartOfExposure(exposure, now);

    assertThat(timeSinceExposure).isEqualTo(3);
  }

  // We don't permit 0 days, but always return at least 1
  @Test
  public void calculateDaysFromStartOfExposure_6Feb6pm_6Feb6pm_1day() {
    Instant exposure = Instant.from(OffsetDateTime
        .of(2021, 2 ,6, 18,0,0,0, ZoneOffset.UTC));
    Instant now = Instant.from(OffsetDateTime
        .of(2021, 2 ,6, 18,0,0,0, ZoneOffset.UTC));

    long timeSinceExposure = StringUtils.calculateDaysFromStartOfExposure(exposure, now);

    assertThat(timeSinceExposure).isEqualTo(1);
  }

  // The following 2 tests check the logic across timezones.
  @Test
  public void calculateDaysFromStartOfExposure_exposure6FebMidnightUTC_9Feb459pmPT_3days() {
    Instant exposure = Instant.from(OffsetDateTime
        .of(2021, 2 ,6, 0,0,0,0, ZoneOffset.UTC));
    Instant now = Instant.from(ZonedDateTime
        .of(2021, 2 ,9, 15,59,0,0, ZoneId.of("America/Los_Angeles")));

    long timeSinceExposure = StringUtils.calculateDaysFromStartOfExposure(exposure, now);

    assertThat(timeSinceExposure).isEqualTo(3);
  }

  @Test
  public void calculateDaysFromStartOfExposure_exposure6FebMidnightUTC_9Feb501pmPT_4days() {
    Instant exposure = Instant.from(OffsetDateTime
        .of(2021, 2 ,6, 0,0,0,0, ZoneOffset.UTC));
    Instant now = Instant.from(ZonedDateTime
        .of(2021, 2 ,9, 16,1,0,0, ZoneId.of("America/Los_Angeles")));

    long timeSinceExposure = StringUtils.calculateDaysFromStartOfExposure(exposure, now);

    assertThat(timeSinceExposure).isEqualTo(4);
  }

  // E2E: test string output and make sure timezone-logic is sane when using ExposureClassification
  @Test
  public void daysFromStartOfExposure_exposure6FebMidnightUTC_9Feb459pmPT_3days() {
    ExposureClassification exposureDate =
        createExposureClassificationWithUTCDate(LocalDate.of(2021,2,6));
    Instant now = Instant.from(ZonedDateTime
        .of(2021, 2 ,9, 15,59,0,0, ZoneId.of("America/Los_Angeles")));

    String result = StringUtils.daysFromStartOfExposure(exposureDate, now, context);

    assertThat(result).isEqualTo("3 days ago");
  }

  @Test
  public void daysFromStartOfExposure_exposure6FebMidnightUTC_9Feb501pmPT_4days() {
    ExposureClassification exposureDate =
        createExposureClassificationWithUTCDate(LocalDate.of(2021,2,6));
    Instant now = Instant.from(ZonedDateTime
        .of(2021, 2 ,9, 16,1,0,0, ZoneId.of("America/Los_Angeles")));

    String result = StringUtils.daysFromStartOfExposure(exposureDate, now, context);

    assertThat(result).isEqualTo("4 days ago");
  }

  @Test
  public void daysFromStartOfExposure_exposure6FebMidnightUTC_6Feb1pmPT_1day() {
    ExposureClassification exposureDate =
        createExposureClassificationWithUTCDate(LocalDate.of(2021,2,6));
    Instant now = Instant.from(ZonedDateTime
        .of(2021, 2 ,6, 13,0,0,0, ZoneId.of("America/Los_Angeles")));

    String result = StringUtils.daysFromStartOfExposure(exposureDate, now, context);

    assertThat(result).isEqualTo("1 day ago");
  }

  private static ExposureClassification createExposureClassificationWithUTCDate(LocalDate date) {
    return ExposureClassification
        .create(0,ExposureClassification.NO_EXPOSURE_CLASSIFICATION_NAME, date.toEpochDay());
  }

  private Instant roundOffZonedDateTimeToUTCStartOfDay(ZonedDateTime zonedDateTime) {
    // Convert to the same time in UTC
    LocalDateTime localDateTimeUTC =
        LocalDateTime.ofInstant(zonedDateTime.toInstant(), ZoneOffset.UTC);
    // Round of the UTC time to start of UTC day, return resulting instant
    return localDateTimeUTC.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
  }

}
