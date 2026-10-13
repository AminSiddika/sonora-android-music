# Sonora release rules.
#
# The debug build is what CI publishes; these rules keep the release build
# (minify + resource shrinking) safe for the libraries Sonora uses.

# --- kotlinx.serialization ---------------------------------------------------
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class dev.sonora.core.model.**$$serializer { *; }
-keepclassmembers class dev.sonora.core.model.** {
    *** Companion;
}
-keepclasseswithmembers class dev.sonora.core.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Media3 -----------------------------------------------------------------
# Media3 ships its own consumer rules; keep the error code names Sonora maps to
# user facing messages.
-keepclassmembers class androidx.media3.common.PlaybackException {
    <fields>;
}

# --- Coroutines -------------------------------------------------------------
-dontwarn kotlinx.coroutines.**
