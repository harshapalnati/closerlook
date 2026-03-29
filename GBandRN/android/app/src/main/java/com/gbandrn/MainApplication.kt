package com.gbandrn

import android.app.Application
import com.facebook.react.PackageList
import com.facebook.react.ReactApplication
import com.facebook.react.ReactHost
import com.facebook.react.ReactNativeApplicationEntryPoint.loadReactNative
import com.facebook.react.defaults.DefaultReactHost.getDefaultReactHost
import com.veepoo.protocol.VPOperateManager

class MainApplication : Application(), ReactApplication {

  override val reactHost: ReactHost by lazy {
    getDefaultReactHost(
      context = applicationContext,
      packageList =
        PackageList(this).packages.apply {
          add(GBandPackage())
        },
    )
  }

  override fun onCreate() {
    super.onCreate()
    VPOperateManager.getInstance().init(this)   // must be first — initialises BLE SDK
    loadReactNative(this)
  }
}
