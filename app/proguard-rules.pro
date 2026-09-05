# ---------------------------------------------------------
# PIMS Vault R8 / Proguard Rules (Release Hardening)
# ---------------------------------------------------------

# Keep SQLCipher native JNI & classes
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }
-dontwarn net.sqlcipher.**

# Keep Room entities and DAOs
-keep class com.pims.vault.data.local.entity.** { *; }
-keep class com.pims.vault.data.local.relation.** { *; }
-keep class com.pims.vault.data.local.converter.** { *; }
-keep class * extends androidx.room.RoomDatabase

# Keep Domain Models & Enums
-keepclassmembers enum com.pims.vault.core.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Crypto & Security structures
-keep class com.pims.vault.core.crypto.EncryptedPayload { *; }
-keep class com.pims.vault.core.crypto.SecretBytes { *; }
-keep class com.pims.vault.core.crypto.KeySecurityLevel { *; }

# Strip all standard logging in Release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}

# Obfuscate internal presentation, ViewModel, and business logic
-repackageclasses 'com.pims.vault.internal'
-allowaccessmodification
