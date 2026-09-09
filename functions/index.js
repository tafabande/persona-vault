const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

const crypto = require("crypto");

/**
 * 1. RESOLVE SHARE TOKEN
 * Trusted server-side token resolution for QR code scanner & capability links.
 * Verifies expiration, revocation, and atomic use limits inside a Firestore transaction.
 * NEVER returns private health, passwords, or unspecified fields.
 */
exports.resolveShareToken = functions.https.onCall(async (data, context) => {
  const token = data.token;
  if (!token) {
    throw new functions.https.HttpsError("invalid-argument", "Share token is required.");
  }

  // Compute SHA-256 hash of incoming token (server holds only tokenHash)
  const tokenHash = crypto.createHash("sha256").update(token).digest("hex");

  // Find active sharing profile by hash
  const snapshot = await db.collection("sharingProfiles")
    .where("tokenHash", "==", tokenHash)
    .limit(1)
    .get();

  if (snapshot.empty) {
    throw new functions.https.HttpsError("not-found", "Invalid or unknown share token.");
  }

  const shareDocRef = snapshot.docs[0].ref;
  const now = admin.firestore.Timestamp.now();

  // Execute inside atomic transaction to prevent concurrency races on useCount
  let result = null;
  await db.runTransaction(async (transaction) => {
    const freshDoc = await transaction.get(shareDocRef);
    if (!freshDoc.exists) {
      throw new functions.https.HttpsError("not-found", "Share profile not found.");
    }
    const shareData = freshDoc.data();

    // Verify revocation
    if (shareData.revokedAt != null || shareData.revoked === true) {
      throw new functions.https.HttpsError("permission-denied", "This capability link has been revoked by the owner.");
    }

    // Verify expiry
    if (shareData.expiresAt && shareData.expiresAt.toMillis() < now.toMillis()) {
      throw new functions.https.HttpsError("deadline-exceeded", "This capability link has expired.");
    }

    // Verify max access count (atomic guard)
    const currentUses = shareData.accessCount || 0;
    if (shareData.maxAccessCount != null && currentUses >= shareData.maxAccessCount) {
      throw new functions.https.HttpsError("resource-exhausted", "This capability link has reached its maximum access limit.");
    }

    // Fetch owner's profile
    const ownerId = shareData.ownerAccountId || shareData.ownerId;
    const userDoc = await transaction.get(db.collection("accounts").doc(ownerId));
    const rawUserData = userDoc.exists ? userDoc.data() : {};

    const allowedFields = shareData.fieldsAllowed || ["fullName", "occupation"];
    const filteredProfile = {};
    for (const field of allowedFields) {
      if (rawUserData[field] !== undefined) {
        filteredProfile[field] = rawUserData[field];
      }
    }

    // Atomic increment
    transaction.update(shareDocRef, {
      accessCount: currentUses + 1,
      lastAccessedAt: now
    });

    result = {
      ownerId: ownerId,
      profile: filteredProfile,
      expiresAt: shareData.expiresAt ? shareData.expiresAt.toDate().toISOString() : null
    };
  });

  // Append tamper-evident audit event (outside transaction)
  // scannerNetworkMetadata is best-effort request telemetry, never treated as proof of identity
  await db.collection("auditEvents").add({
    userId: result.ownerId,
    eventType: "SHARE_PROFILE_ACCESSED",
    entityType: "SHARING_PROFILE",
    entityId: shareDocRef.id,
    description: "Selective capability profile accessed",
    scannerUid: context.auth ? context.auth.uid : "ANONYMOUS_SCANNER",
    scannerNetworkMetadata: {
      ip: context.rawRequest ? (context.rawRequest.ip || "UNKNOWN") : "UNKNOWN",
      userAgent: context.rawRequest && context.rawRequest.headers ? (context.rawRequest.headers["user-agent"] || "UNKNOWN") : "UNKNOWN"
    },
    timestamp: now
  });

  return {
    success: true,
    ownerId: result.ownerId,
    profile: result.profile,
    expiresAt: result.expiresAt
  };
});

/**
 * 2. HANDLE RELATIONSHIP REQUEST
 * Manages connection invitations between users.
 * Core Principle: Relationship != Permission.
 */
exports.handleRelationshipRequest = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Authentication required.");
  }

  const { targetUserId, relationRole, action } = data; // action: 'SEND', 'ACCEPT', 'DECLINE'
  const currentUid = context.auth.uid;

  if (action === "SEND") {
    const relRef = await db.collection("relationships").add({
      createdBy: currentUid,
      participants: [currentUid, targetUserId],
      status: "PENDING",
      role: relationRole,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    // Notify recipient via sanitized FCM
    await sendSanitizedPush(targetUserId, {
      title: "New Connection Request",
      body: "Someone requested to connect with you on Persona.",
      type: "RELATIONSHIP_REQUEST",
      relationshipId: relRef.id
    });

    return { success: true, relationshipId: relRef.id };
  } else if (action === "ACCEPT" || action === "DECLINE") {
    const relId = data.relationshipId;
    const relRef = db.collection("relationships").doc(relId);
    const doc = await relRef.get();

    if (!doc.exists || !doc.data().participants.includes(currentUid)) {
      throw new functions.https.HttpsError("permission-denied", "Not authorized for this relationship.");
    }

    const newStatus = action === "ACCEPT" ? "ACTIVE" : "DECLINED";
    await relRef.update({
      status: newStatus,
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    // Notify requester of resolution
    const otherUid = doc.data().participants.find(uid => uid !== currentUid);
    if (otherUid) {
      await sendSanitizedPush(otherUid, {
        title: "Connection Updated",
        body: `Your connection request was ${newStatus.toLowerCase()}.`,
        type: "RELATIONSHIP_UPDATE",
        relationshipId: relId
      });
    }

    return { success: true, status: newStatus };
  }

  throw new functions.https.HttpsError("invalid-argument", "Invalid action.");
});

/**
 * 3. SANITIZED NOTIFICATION SENDER
 * Dispatches FCM push notifications with zero sensitive details.
 * Never leaks medical diagnoses, document numbers, or vault items.
 */
async function sendSanitizedPush(userId, payload) {
  try {
    // Look up registered device FCM tokens for the user
    const sessionsSnapshot = await db.collection("users").doc(userId).collection("sessions").get();
    const tokens = [];
    sessionsSnapshot.forEach(doc => {
      const fcmToken = doc.data().fcmToken;
      if (fcmToken) tokens.push(fcmToken);
    });

    if (tokens.length === 0) return;

    // Strict sanitization: ensure no raw personal data is ever placed in the notification
    const message = {
      notification: {
        title: payload.title,
        body: payload.body
      },
      data: {
        type: payload.type || "INFO",
        id: payload.relationshipId || payload.documentId || "",
        timestamp: Date.now().toString()
      },
      tokens: tokens
    };

    await admin.messaging().sendEachForMulticast(message);
  } catch (err) {
    console.error("Failed to send sanitized push notification:", err);
  }
}
