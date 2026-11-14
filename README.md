# Relaxing Ocean TV

Android TV app for [Relaxing Ocean](https://relaxingocean.cc.cd/). It opens the live page full screen, and it can also run as the television screensaver.

The remote moves a ring over the page buttons. Center presses the focused control. On the manual-control button, Up swims forward, Left and Right turn, and Down stops. Back leaves manual control. Back again leaves the app.

## Screensaver

The dream service is `cc.cd.relaxingocean/.OceanDream`. On the television, open Settings, Device Preferences, Screensaver, and choose 海洋之歌. The ocean stays on the bright screen. Any remote key leaves the screensaver.

## Build

Android SDK 35, Java 17, Gradle 8.9.

```
gradle wrapper
gradlew assembleDebug
```

Install `app/build/outputs/apk/debug/app-debug.apk` on an Android TV.

## 海洋之歌電視版

這是 [海洋之歌](https://relaxingocean.cc.cd/) 的 Android TV app，全螢幕打開線上頁面，也可以當成電視的螢幕保護程式。

遙控器移動按鈕上的光圈，中鍵按下。進入手動控制後，上鍵往前游，左右轉向，下鍵停止。返回鍵離開手動控制，再按一次離開 app。

在電視上打開設定、裝置偏好設定、螢幕保護程式，選擇海洋之歌。畫面保持亮著，按任何遙控器鍵就離開。
