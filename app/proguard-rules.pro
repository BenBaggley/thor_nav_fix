# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in defaultProguardFile("proguard-android-optimize.txt").

# Keep Shizuku UserService and AIDL stubs
-keep class dev.benkidd.thornav.service.UserService { *; }
-keep class dev.benkidd.thornav.IUserService$Stub { *; }
