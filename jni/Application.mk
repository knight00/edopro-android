# //kdiy///////////
# APP_ABI := armeabi-v7a arm64-v8a x86 #x86_64
APP_ABI := armeabi-v7a arm64-v8a x86 x86_64
# //kdiy///////////
APP_PLATFORM := android-16
APP_STL := c++_static
APP_CPPFLAGS := -std=c++1z -fno-rtti -fexceptions
APP_CFLAGS := -fPIC -pipe -fno-stack-protector
APP_OPTIM := release

# //kdiy///////////
# Explicitly tell ndk-build to build both modules as top-level targets
APP_MODULES := EDOProClient ocgcore
# //kdiy///////////