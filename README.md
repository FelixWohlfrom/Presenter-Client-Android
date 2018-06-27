## Presenter App for Android [![Build Status](https://travis-ci.org/FelixWohlfrom/Presenter-Client-Android.svg?branch=master)](https://travis-ci.org/FelixWohlfrom/Presenter-Client-Android) [![codecov](https://codecov.io/gh/FelixWohlfrom/Presenter-Client-Android/branch/master/graph/badge.svg)](https://codecov.io/gh/FelixWohlfrom/Presenter-Client-Android)

This app can be used to remote control a presentation e.g. on your notebook.

**It requires a server software running on the machine that is controlled.**
A sample server can be found [here](https://github.com/FelixWohlfrom/Presenter-Server).

Connection can currently established using bluetooth. Wifi support is currently planned.

In this document, you will find the information how to build and install the app.

### Requirements
For installation:
- An android device with minimum api level 21 or higher. This is Android 5.1 "Lollipop" or higher.
- [Android platform tools](https://developer.android.com/studio/releases/platform-tools.html)

For building the app:
- [Android Studio](https://developer.android.com/studio/index.html).

### Building the app
To build the app, import the source code in Android Studio.
There you can build and install it [like any other Android app](https://developer.android.com/training/basics/firstapp/running-app.html).

### Installation
If you want to just install an [official release](../../releases), you need to enable usb debugging on your device.
See [the Android documentation](https://developer.android.com/training/basics/firstapp/running-app.html#RealDevice) how to do this.

Now you can execute the following command to install the downloaded apk file:
```<path/to/extracted/>adb install </path/to/downloaded/>Presenter.apk```

Replace the paths accordingly where you downloaded the apk and where you extracted the platform tools.