const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const firebaseCompat = require("/usr/local/lib/node_modules/@firebase/rules-unit-testing/node_modules/firebase/compat/app");
require("/usr/local/lib/node_modules/@firebase/rules-unit-testing/node_modules/firebase/compat/firestore");
const serverTimestamp = () => firebaseCompat.firestore.FieldValue.serverTimestamp();

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfile(uid) {
  return {
    userId: uid,
    explorerName: "Zuri Vance",
    displayName: "Alice Explorer",
    email: "alice@example.com",
    selectedHeroId: "zuri",
    heroLevel: 1,
    heroExp: 0,
    equippedRelicId: "solar_visor",
    unlockedRelicsCsv: "solar_visor",
    completedQuestsCsv: "",
    discoveredLoreCsv: "solari_spires,moss_hollow_chronicle",
    nectar: 15,
    moonstones: 2,
    memoryXp: 0,
    logicXp: 0,
    spatialXp: 0,
    deductionXp: 0,
    observationXp: 0,
    currentStreak: 0,
    bestStreak: 0,
    totalPuzzlesSolved: 0,
    gentleMode: true,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

function validCreature(uid, creatureId = "lumafern") {
  return {
    userId: uid,
    creatureId,
    discovered: true,
    bondLevel: 1,
    bondXp: 15,
    treatsFed: 0,
    lastInteractionMillis: 0,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

function validPuzzleStats(uid, puzzleType = "firefly_echo") {
  return {
    userId: uid,
    puzzleType,
    currentLevel: 1,
    totalStars: 0,
    playedCount: 0,
    solvedCount: 0,
    bestScore: 0,
    bestStreak: 0,
    lastSolvedMillis: 0,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write profiles, creatures, or puzzle stats", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID)));
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("creatures").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("puzzle_stats").get());
});

test("Authenticated user can create, read, and update their own profile and subcollections in a batch", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const batch = aliceDb.batch();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);
  const creatureRef = profileRef.collection("creatures").doc("lumafern");
  const statsRef = profileRef.collection("puzzle_stats").doc("firefly_echo");

  batch.set(profileRef, validProfile(ALICE_UID));
  batch.set(creatureRef, validCreature(ALICE_UID, "lumafern"));
  batch.set(statsRef, validPuzzleStats(ALICE_UID, "firefly_echo"));
  await assertSucceeds(batch.commit());

  await assertSucceeds(profileRef.get());
  await assertSucceeds(profileRef.collection("creatures").get());
  await assertSucceeds(profileRef.collection("puzzle_stats").get());

  await assertSucceeds(
    profileRef.update({
      nectar: 35,
      heroExp: 45,
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    creatureRef.update({
      bondXp: 45,
      treatsFed: 1,
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    statsRef.update({
      currentLevel: 2,
      totalStars: 3,
      playedCount: 1,
      solvedCount: 1,
      bestScore: 150,
      updatedAt: serverTimestamp(),
    })
  );
});

test("Cross-user isolation: Bob cannot read, list, create, update, or delete Alice's data", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID));
    await db.collection("users").doc(ALICE_UID).collection("creatures").doc("lumafern").set(validCreature(ALICE_UID));
    await db.collection("users").doc(ALICE_UID).collection("puzzle_stats").doc("firefly_echo").set(validPuzzleStats(ALICE_UID));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("creatures").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("puzzle_stats").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ nectar: 999, updatedAt: serverTimestamp() }));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).delete());
});

test("Schema & identity hardening: rejects ghost fields, negative counters, mismatched IDs, and orphan subcollections", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);

  // Orphan creature without parent profile
  await assertFails(
    profileRef.collection("creatures").doc("lumafern").set(validCreature(ALICE_UID, "lumafern"))
  );

  // Shadow/ghost field on create
  await assertFails(
    profileRef.set({ ...validProfile(ALICE_UID), isAdmin: true })
  );

  // Spoofed userId field
  await assertFails(
    profileRef.set({ ...validProfile(ALICE_UID), userId: BOB_UID })
  );

  // Create valid profile first
  await assertSucceeds(profileRef.set(validProfile(ALICE_UID)));

  // Mismatched creatureId path vs document body
  await assertFails(
    profileRef.collection("creatures").doc("lumafern").set(validCreature(ALICE_UID, "dewdrake"))
  );

  // Value poisoning: negative nectar
  await assertFails(
    profileRef.update({ nectar: -10, updatedAt: serverTimestamp() })
  );

  // Ghost field on update
  await assertFails(
    profileRef.update({ ghostFlag: true, updatedAt: serverTimestamp() })
  );

  // Attempting to mutate immutable createdAt
  await assertFails(
    profileRef.update({ createdAt: serverTimestamp(), updatedAt: serverTimestamp() })
  );
});
