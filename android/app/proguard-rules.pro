# libwebrtc calls back into java over jni, r8 cannot see those uses
-keep class org.webrtc.** { *; }
-keep interface org.webrtc.** { *; }
-dontwarn org.webrtc.**
-keep class org.jni_zero.** { *; }
-dontwarn org.jni_zero.**
