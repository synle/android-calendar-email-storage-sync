# Keep Google API models
-keep class com.google.api.services.gmail.** { *; }
-keep class com.google.api.client.** { *; }

# Keep Jakarta Mail
-keep class jakarta.mail.** { *; }
-keep class org.eclipse.angus.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
