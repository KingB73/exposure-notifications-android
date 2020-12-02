package com.google.android.apps.exposurenotification.privateanalytics;

import static java.util.UUID.randomUUID;

import android.os.Build.VERSION_CODES;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import com.google.android.apps.exposurenotification.common.Qualifiers.ScheduledExecutor;
import com.google.android.apps.exposurenotification.common.TaskToFutureAdapter;
import com.google.android.apps.exposurenotification.common.time.Clock;
import com.google.android.apps.exposurenotification.proto.CreatePacketsResponse;
import com.google.android.apps.exposurenotification.proto.PrioAlgorithmParameters;
import com.google.android.apps.exposurenotification.proto.ResponseStatus.StatusCode;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.io.BaseEncoding;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import org.threeten.bp.Duration;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.format.DateTimeFormatter;


/**
 * Repository to abstract the interactions with Firestore.
 */
public class PrivateAnalyticsFirestoreRepository {

  // Document keys:
  static final String UUID = "uuid";
  static final String CREATED = "created";
  static final String PRIO_PARAMS = "prioParams";
  static final String SCHEMA_VERSION_KEY = "schemaVersion";

  private static final String TAG = "PrioFirestoreRepository"; // Logging TAG
  private static final String ENCRYPTED_DATA_SHARES = "encryptedDataShares";
  // Params keys:
  private static final String BINS = "bins";
  private static final String EPSILON = "epsilon";
  private static final String HAMMING_WEIGHT = "hammingWeight";
  private static final String NUMBER_SERVERS = "numberServers";
  private static final String PRIME = "prime";
  // Response keys:
  private static final String PAYLOAD = "payload";
  private static final String ENCRYPTION_KEY_ID = "encryptionKeyId";
  private static final int SCHEMA_VERSION = 2;
  private static final Duration FIRESTORE_UPLOAD_TIMEOUT = Duration.ofMinutes(5);
  private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter
      .ofPattern("yyyy-MM-dd-HH", Locale.US)
      .withZone(ZoneOffset.UTC);

  private static final BaseEncoding BASE64 = BaseEncoding.base64();

  private final PrivateAnalyticsDeviceAttestation deviceAttestation;
  private final FirebaseFirestore db;

  private final Clock clock;
  private final ScheduledExecutorService scheduledExecutor;
  private final SecureRandom random;


  @Inject
  PrivateAnalyticsFirestoreRepository(
      PrivateAnalyticsDeviceAttestation deviceAttestation,
      Clock clock, @Nullable FirebaseFirestore firebaseFireStore,
      @ScheduledExecutor ScheduledExecutorService scheduledExecutor, SecureRandom random) {
    this.deviceAttestation = deviceAttestation;
    this.db = firebaseFireStore;
    this.clock = clock;
    this.scheduledExecutor = scheduledExecutor;
    this.random = random;
    if (firebaseFireStore != null) {
      FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
          .setPersistenceEnabled(false)
          .build();
      db.setFirestoreSettings(settings);
    }
  }

  @RequiresApi(api = VERSION_CODES.N)
  public ListenableFuture<Void> writeNewPacketsResponse(String metricName,
      PrioPacketPayload prioPacketPayload, RemoteConfigs remoteConfigs) {
    return FluentFuture.from(TaskToFutureAdapter.getFutureWithTimeout(
        writeNewPacketsResponseTask(metricName, prioPacketPayload, remoteConfigs),
        FIRESTORE_UPLOAD_TIMEOUT,
        scheduledExecutor));
  }

  @RequiresApi(api = VERSION_CODES.N)
  private Task<Void> writeNewPacketsResponseTask(
      String metricName, PrioPacketPayload prioPacketPayload, RemoteConfigs remoteConfigs) {
    boolean isDeviceAttestationRequired = remoteConfigs.deviceAttestationRequired();
    long collectionFrequencyHours = remoteConfigs.collectionFrequencyHours();
    CreatePacketsResponse response = prioPacketPayload.createPacketsResponse();
    PrioAlgorithmParameters params =
        prioPacketPayload.createPacketsParameters().getPrioParameters();
    if (response.getResponseStatus().getStatusCode() != StatusCode.OK) {
      Log.w(TAG, "Cannot write failed response: " + response.getResponseStatus().getErrorDetails());
      return Tasks.forCanceled();
    }

    // Create document with fresh uuid
    String uuid = generateUuid();
    // TODO: Consider refactoring to a POJO, if not handle casting appropriately downstream.
    Map<String, Object> document = new HashMap<>();
    try {
      Map<String, Object> payload = createPayload(prioPacketPayload, uuid, remoteConfigs);
      document.put("payload", payload);
      if (!deviceAttestation
          .signPayload(metricName, document, params, response, collectionFrequencyHours)) {
        return Tasks.forCanceled();
      }
    } catch (Exception e) {
      Log.w(TAG, "Device attestation failed, requireAttestation=" + isDeviceAttestationRequired, e);
      if (isDeviceAttestationRequired) {
        return Tasks.forException(e);
      }
    }

    Log.d(TAG, "Writing packets to Firestore for metric=" + metricName);

    // Sharding top-level Firestore prefix to improve back-end performance:
    String rootCollection = UUID + random.nextInt(100);

    CollectionReference collection = db.collection(rootCollection)
        .document(uuid)
        .collection(getFormattedDate());
    return
        collection
            .document(metricName)
            .set(document);
  }

  @RequiresApi(api = VERSION_CODES.N)
  private Map<String, Object> createPayload(
      PrioPacketPayload prioPacketPayload,
      String uuid,
      RemoteConfigs remoteConfigs) throws Exception {
    PrioAlgorithmParameters prioParams =
        prioPacketPayload.createPacketsParameters().getPrioParameters();
    Map<String, Object> newDoc = new HashMap<>();
    newDoc.put(UUID, uuid);
    newDoc.put(CREATED, FieldValue.serverTimestamp());
    newDoc.put(PRIO_PARAMS, convertPrioParamsToMap(prioParams));
    newDoc.put(SCHEMA_VERSION_KEY, SCHEMA_VERSION);
    newDoc.put(ENCRYPTED_DATA_SHARES, convertDataSharesToList(prioPacketPayload, remoteConfigs));
    return newDoc;
  }

  private String getFormattedDate() {
    return dateTimeFormatter.format(clock.now());
  }

  private ImmutableList<ImmutableMap<String, String>> convertDataSharesToList(
      PrioPacketPayload payload,
      RemoteConfigs remoteConfigs) throws NoSuchAlgorithmException {
    CreatePacketsResponse response = payload.createPacketsResponse();
    ImmutableList.Builder<ImmutableMap<String, String>> listBuilder = ImmutableList.builder();

    String phaKeyId = remoteConfigs.phaEncryptionKeyId();
    listBuilder.add(ImmutableMap.of(
        PAYLOAD,
        BASE64.encode(response.getShares(0).toByteArray()),
        ENCRYPTION_KEY_ID,
        phaKeyId));
    Log.d(TAG, "PHA encryption key id:: " + phaKeyId);

    String facilitatorKeyId = remoteConfigs.facilitatorEncryptionKeyId();
    listBuilder.add(ImmutableMap.of(
        PAYLOAD,
        BASE64.encode(response.getShares(1).toByteArray()),
        ENCRYPTION_KEY_ID,
        facilitatorKeyId));
    Log.d(TAG, "Facilitator encryption key id: " + facilitatorKeyId);
    return listBuilder.build();
  }

  private static String generateUuid() {
    return randomUUID().toString();
  }

  private static ImmutableMap<String, Object> convertPrioParamsToMap(
      PrioAlgorithmParameters prioParams) throws Exception {
    ImmutableMap.Builder<String, Object> map = ImmutableMap.builder();
    storePrioParam(map, BINS, prioParams.hasBins(), prioParams.getBins());
    storePrioParam(map, PRIME, prioParams.hasPrime(), prioParams.getPrime());
    storePrioParam(map, EPSILON, prioParams.hasEpsilon(), prioParams.getEpsilon());
    storePrioParam(map, NUMBER_SERVERS, prioParams.hasNumberServers(),
        prioParams.getNumberServers());
    if (prioParams.hasHammingWeight()) { // Hamming weight is optional.
      map.put(HAMMING_WEIGHT, prioParams.getHammingWeight());
    }
    return map.build();
  }

  private static <T> void storePrioParam(Builder<String, Object> map, String key, boolean hasValue,
      T value) throws Exception {
    if (hasValue) {
      map.put(key, value);
    } else {
      throw new Exception("Prio params missing: " + key);
    }
  }
}
