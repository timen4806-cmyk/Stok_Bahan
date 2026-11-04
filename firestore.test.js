const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

test("Unauthenticated user: cannot read materials", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("bahan").get());
});

test("Cross-user: Alice cannot read Bob's materials", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).collection("bahan").doc("item1").set({
      nama: "HOLO 10x14",
      nilai1: "25",
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("bahan").doc("item1").get());
});

test("Authenticated user: Alice can create, read and update her own materials", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection("users").doc(ALICE_UID).collection("bahan").doc("item1");

  await assertSucceeds(docRef.set({
    nama: "PIPA 6mm",
    nilai1: "50",
  }));

  await assertSucceeds(docRef.get());

  await assertSucceeds(docRef.update({
    nilai1: "75",
  }));
});

test("Validation: Alice cannot create material with empty name", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection("users").doc(ALICE_UID).collection("bahan").doc("item2");

  await assertFails(docRef.set({
    nama: "",
  }));
});

test("Settings: Alice can read and update her own column settings", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const settingsRef = aliceDb.collection("users").doc(ALICE_UID).collection("settings").doc("config");

  await assertSucceeds(settingsRef.set({
    judul: "STOK BAHAN SINAR MAS",
    kolom1: "GUDANG A",
    kolom2: "GUDANG B",
  }));

  await assertSucceeds(settingsRef.get());
});
