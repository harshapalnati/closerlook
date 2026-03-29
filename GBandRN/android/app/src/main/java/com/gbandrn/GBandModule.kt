package com.gbandrn

import android.os.Handler
import android.os.Looper
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.inuker.bluetooth.library.search.SearchResult
import com.inuker.bluetooth.library.search.response.SearchResponse
import com.inuker.bluetooth.library.Code
import com.inuker.bluetooth.library.model.BleGattProfile
import com.inuker.bluetooth.library.utils.BluetoothUtils
import com.veepoo.protocol.VPOperateManager
import com.veepoo.protocol.listener.base.IConnectResponse
import com.veepoo.protocol.listener.base.INotifyResponse
import com.veepoo.protocol.listener.data.*
import com.veepoo.protocol.model.datas.*

class GBandModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    override fun getName() = "GBandModule"

    private val mainHandler = Handler(Looper.getMainLooper())

    // ── Measurement state (SDK is serial — one at a time) ─────────────────
    private var isMeasuringHeart    = false
    private var isMeasuringSpo2     = false
    private var isMeasuringBp       = false
    private var isMeasuringTemp     = false
    private var isWellnessRunning   = false

    // ── Event emitter helper ──────────────────────────────────────────────
    private fun emit(event: String, data: Any?) {
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(event, data)
    }

    private fun emitMap(event: String, block: WritableMap.() -> Unit) {
        val map = Arguments.createMap()
        map.block()
        emit(event, map)
    }

    // ── Scan ──────────────────────────────────────────────────────────────
    @ReactMethod
    fun startScan(promise: Promise) {
        try {
            if (!BluetoothUtils.isBluetoothEnabled()) {
                promise.reject("BT_DISABLED", "Bluetooth is not enabled")
                return
            }
            VPOperateManager.getInstance().stopScanDevice()
            VPOperateManager.getInstance().disconnectWatch {}
            VPOperateManager.getInstance().startScanDevice(object : SearchResponse {
                override fun onSearchStarted() {
                    emitMap("onScanStatus") { putString("status", "scanning") }
                }
                override fun onDeviceFounded(device: SearchResult) {
                    emitMap("onDeviceFound") {
                        putString("name", device.name ?: "Unknown")
                        putString("mac", device.address)
                        putInt("rssi", device.rssi)
                    }
                }
                override fun onSearchStopped() {
                    emitMap("onScanStatus") { putString("status", "stopped") }
                }
                override fun onSearchCanceled() {
                    emitMap("onScanStatus") { putString("status", "canceled") }
                }
            })
            // Auto-stop after 12s
            mainHandler.postDelayed({ VPOperateManager.getInstance().stopScanDevice() }, 12000)
            promise.resolve(null)
        } catch (e: Exception) {
            promise.reject("SCAN_ERR", e.message)
        }
    }

    @ReactMethod
    fun stopScan(promise: Promise) {
        VPOperateManager.getInstance().stopScanDevice()
        promise.resolve(null)
    }

    // ── Connect ───────────────────────────────────────────────────────────
    @ReactMethod
    fun connectDevice(mac: String, name: String, promise: Promise) {
        try {
            VPOperateManager.getInstance().stopScanDevice()
            VPOperateManager.getInstance().connectDevice(
                mac, name,
                IConnectResponse { code, _, _ ->
                    if (code == Code.REQUEST_SUCCESS) {
                        emitMap("onConnectionState") { putString("state", "ble_connected") }
                    } else {
                        emitMap("onConnectionState") {
                            putString("state", "failed")
                            putInt("code", code)
                        }
                    }
                },
                INotifyResponse { state ->
                    if (state == Code.REQUEST_SUCCESS) {
                        mainHandler.postDelayed({ verifyPassword(mac, name) }, 1000)
                    }
                }
            )
            promise.resolve(null)
        } catch (e: Exception) {
            promise.reject("CONNECT_ERR", e.message)
        }
    }

    private fun verifyPassword(mac: String, name: String) {
        VPOperateManager.getInstance().confirmDevicePwd(
            {},
            IPwdDataListener { pwdData ->
                val status = pwdData.mStatus?.toString() ?: ""
                if (status.contains("SUCESS") || status.contains("SUCCESS")) {
                    emitMap("onConnected") {
                        putString("mac", mac)
                        putString("name", name)
                        putString("firmware", pwdData.deviceVersion ?: "")
                    }
                } else {
                    emitMap("onConnectionState") {
                        putString("state", "auth_failed")
                        putString("reason", status)
                    }
                }
            },
            object : IDeviceFuctionDataListener {
                override fun onFunctionSupportDataChange(caps: FunctionDeviceSupportData) {
                    emitMap("onCapabilities") {
                        putBoolean("bp",           caps.isSupportBp)
                        putBoolean("spo2",         caps.isSupportSpoH)
                        putBoolean("temp",         caps.isSupportTemperatureFunction)
                        putBoolean("ecg",          caps.isSupportECG)
                        putBoolean("hrv",          caps.isSupportHRV)
                        putBoolean("precisionSleep", caps.isSupportPrecisionSleep)
                        putBoolean("stress",       caps.isSupportStressDetect)
                        putBoolean("respRate",     caps.isSupportBeathFunction)
                        putBoolean("bloodGlucose", caps.isSupportBloodGlucose)
                    }
                }
                override fun onDeviceFunctionPackage1Report(d: DeviceFunctionPackage1?) {}
                override fun onDeviceFunctionPackage2Report(d: DeviceFunctionPackage2?) {}
                override fun onDeviceFunctionPackage3Report(d: DeviceFunctionPackage3?) {}
                override fun onDeviceFunctionPackage4Report(d: DeviceFunctionPackage4?) {}
                override fun onDeviceFunctionPackage5Report(d: DeviceFunctionPackage5?) {}
            },
            null, null, "0000", false
        )
    }

    @ReactMethod
    fun disconnect(promise: Promise) {
        VPOperateManager.getInstance().disconnectWatch {
            emitMap("onConnectionState") { putString("state", "disconnected") }
        }
        promise.resolve(null)
    }

    // ── Battery ───────────────────────────────────────────────────────────
    @ReactMethod
    fun readBattery(promise: Promise) {
        VPOperateManager.getInstance().readBattery({}) { data ->
            val pct = if (data.isPercent) data.batteryPercent else data.batteryLevel * 25
            promise.resolve(pct)
        }
    }

    // ── Steps / Distance / Calories ───────────────────────────────────────
    @ReactMethod
    fun readSteps(promise: Promise) {
        VPOperateManager.getInstance().readSportStep({}) { data ->
            val map = Arguments.createMap()
            map.putInt("steps", data.step)
            map.putDouble("distanceKm", data.dis.toDouble())
            map.putDouble("caloriesKcal", data.kcal.toDouble())
            promise.resolve(map)
        }
    }

    // ── Sleep ─────────────────────────────────────────────────────────────
    @ReactMethod
    fun readSleep(promise: Promise) {
        VPOperateManager.getInstance().readSleepData(
            {},
            object : ISleepDataListener {
                override fun onSleepDataChange(day: String, data: SleepData) {
                    val map = Arguments.createMap()
                    map.putInt("totalMinutes",  data.allSleepTime)
                    map.putInt("deepMinutes",   data.deepSleepTime)
                    map.putInt("lightMinutes",  data.lowSleepTime)
                    map.putInt("wakeCount",     data.wakeCount)
                    map.putInt("sleepQuality",  data.sleepQulity)
                    promise.resolve(map)
                }
                override fun onSleepProgress(p: Float) {}
                override fun onSleepProgressDetail(day: String, pkg: Int) {}
                override fun onReadSleepComplete() {}
            },
            1
        )
    }

    // ── OriginData3 (HR history, BP history, SpO2 history, Resp, HRV) ─────
    @ReactMethod
    fun readOriginData(promise: Promise) {
        VPOperateManager.getInstance().readOriginData(
            { _ -> },
            object : IOriginData3Listener {
                private var lastHr = -1
                private var lastSys = -1
                private var lastDia = -1
                private var lastResp = -1
                private var lastOxy = -1

                override fun onOriginData3Change(date: String, dataList: List<OriginData3>?) {
                    dataList?.forEach { d3 ->
                        if (d3.rateValue > 0) lastHr = d3.rateValue
                        if (d3.highValue > 0) lastSys = d3.highValue
                        if (d3.lowValue > 0)  lastDia = d3.lowValue
                        d3.resRates?.forEach { r -> if (r > 0) lastResp = r }
                        d3.oxygens?.forEach { o -> if (o > 0) lastOxy = o }
                    }
                }

                override fun onOriginHRVOriginListDataChange(list: List<HRVOriginData>?) {
                    list?.lastOrNull()?.let { last ->
                        val hrv = last.hrv
                        if (hrv in 1..254) {
                            emitMap("onHRV") { putInt("hrv", hrv) }
                        }
                    }
                }

                override fun onOringinHalfHourDataChange(d: OriginHalfHourData?) {}
                override fun onReadOriginProgressDetail(day: Int, date: String, all: Int, curr: Int) {}
                override fun onReadOriginProgress(p: Float) {}

                override fun onReadOriginComplete() {
                    val map = Arguments.createMap()
                    if (lastHr   > 0) map.putInt("heartRate", lastHr)
                    if (lastSys  > 0) map.putInt("bpSystolic", lastSys)
                    if (lastDia  > 0) map.putInt("bpDiastolic", lastDia)
                    if (lastResp > 0) map.putInt("respirationRate", lastResp)
                    if (lastOxy  > 0) map.putInt("spo2", lastOxy)
                    promise.resolve(map)
                }
            },
            1
        )
    }

    // ── Heart Rate live measurement ───────────────────────────────────────
    @ReactMethod
    fun startHeartMeasure(promise: Promise) {
        if (isMeasuringHeart) { promise.reject("BUSY", "Already measuring"); return }
        isMeasuringHeart = true
        VPOperateManager.getInstance().startDetectHeart({}) { data ->
            val bpm = data.rateValue
            if (bpm > 0) emitMap("onHeartRate") { putInt("bpm", bpm) }
        }
        promise.resolve(null)
    }

    @ReactMethod
    fun stopHeartMeasure(promise: Promise) {
        isMeasuringHeart = false
        VPOperateManager.getInstance().stopDetectHeart {}
        promise.resolve(null)
    }

    // ── SpO2 live measurement ─────────────────────────────────────────────
    @ReactMethod
    fun startSpo2Measure(promise: Promise) {
        if (isMeasuringSpo2) { promise.reject("BUSY", "Already measuring"); return }
        isMeasuringSpo2 = true
        VPOperateManager.getInstance().startDetectSPO2H({}) { data ->
            val o2 = data.oxygen
            if (o2 > 0) emitMap("onSpo2") { putInt("value", o2) }
        }
        promise.resolve(null)
    }

    @ReactMethod
    fun stopSpo2Measure(promise: Promise) {
        isMeasuringSpo2 = false
        VPOperateManager.getInstance().stopDetectSPO2H {}
        promise.resolve(null)
    }

    // ── Blood Pressure live measurement ───────────────────────────────────
    @ReactMethod
    fun startBpMeasure(promise: Promise) {
        if (isMeasuringBp) { promise.reject("BUSY", "Already measuring"); return }
        isMeasuringBp = true
        VPOperateManager.getInstance().startDetectBP({}) { data ->
            val hi = data.highValue; val lo = data.lowValue
            if (hi > 0 && lo > 0) emitMap("onBP") {
                putInt("systolic", hi)
                putInt("diastolic", lo)
            }
        }
        promise.resolve(null)
    }

    @ReactMethod
    fun stopBpMeasure(promise: Promise) {
        isMeasuringBp = false
        VPOperateManager.getInstance().stopDetectBP {}
        promise.resolve(null)
    }

    // ── Body Temperature ──────────────────────────────────────────────────
    @ReactMethod
    fun startTempMeasure(promise: Promise) {
        if (isMeasuringTemp) { promise.reject("BUSY", "Already measuring"); return }
        isMeasuringTemp = true
        VPOperateManager.getInstance().startDetectTempture({}) { data ->
            val temp = data.tempture
            if (temp > 0f) emitMap("onTemperature") { putDouble("celsius", temp.toDouble()) }
        }
        promise.resolve(null)
    }

    @ReactMethod
    fun stopTempMeasure(promise: Promise) {
        isMeasuringTemp = false
        VPOperateManager.getInstance().stopDetectTempture {}
        promise.resolve(null)
    }

    // ── Wellness Check (Stress + Fatigue + HRV via MiniCheckup) ──────────
    @ReactMethod
    fun startWellnessCheck(promise: Promise) {
        if (isWellnessRunning) { promise.reject("BUSY", "Already measuring"); return }
        isWellnessRunning = true
        VPOperateManager.getInstance().startMiniCheckup({}) { result ->
            val map = Arguments.createMap()
            if (result.stressLevel >= 0)   map.putInt("stress",      result.stressLevel)
            if (result.fatigueDegree >= 0) map.putInt("fatigue",     result.fatigueDegree)
            if (result.hrv in 1..254)      map.putInt("hrv",         result.hrv)
            if (result.heartRate > 0)      map.putInt("heartRate",   result.heartRate)
            if (result.oxygen > 0)         map.putInt("spo2",        result.oxygen)
            if (result.temperature > 0f)   map.putDouble("temp",     result.temperature.toDouble())
            if (result.systolic > 0)       map.putInt("bpSystolic",  result.systolic)
            if (result.diastolic > 0)      map.putInt("bpDiastolic", result.diastolic)
            emitMap("onWellnessResult") {
                if (result.stressLevel >= 0)   putInt("stress",      result.stressLevel)
                if (result.fatigueDegree >= 0) putInt("fatigue",     result.fatigueDegree)
                if (result.hrv in 1..254)      putInt("hrv",         result.hrv)
                if (result.heartRate > 0)      putInt("heartRate",   result.heartRate)
                if (result.oxygen > 0)         putInt("spo2",        result.oxygen)
                if (result.temperature > 0f)   putDouble("temp",     result.temperature.toDouble())
                if (result.systolic > 0)       putInt("bpSystolic",  result.systolic)
                if (result.diastolic > 0)      putInt("bpDiastolic", result.diastolic)
            }
        }
        promise.resolve(null)
    }

    @ReactMethod
    fun stopWellnessCheck(promise: Promise) {
        isWellnessRunning = false
        VPOperateManager.getInstance().stopMiniCheckup {}
        promise.resolve(null)
    }

    // ── Required override for RN event emitter ────────────────────────────
    @ReactMethod
    fun addListener(eventName: String) {}

    @ReactMethod
    fun removeListeners(count: Int) {}
}
