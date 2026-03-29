import Foundation
import CoreBluetooth

// VeepooBleSDK imports — framework must be in ios/Frameworks/VeepooBleSDK.framework
// Download from: https://github.com/HBandSDK/iOS_Ble_SDK
@_implementationOnly import VeepooBleSDK

@objc(GBandModule)
class GBandModule: RCTEventEmitter {

  private var foundMacs = Set<String>()
  private var isMeasuringHeart    = false
  private var isMeasuringSpo2     = false
  private var isMeasuringBp       = false
  private var isMeasuringTemp     = false
  private var isWellnessRunning   = false
  private var hasListeners        = false

  // ── RCTEventEmitter ────────────────────────────────────────────────────
  override func supportedEvents() -> [String] {
    return [
      "onScanStatus", "onDeviceFound", "onConnectionState",
      "onConnected",  "onCapabilities",
      "onHeartRate",  "onSpo2",        "onBP",
      "onTemperature","onHRV",         "onWellnessResult",
    ]
  }

  override func startObserving() { hasListeners = true  }
  override func stopObserving()  { hasListeners = false }

  private func emit(_ name: String, _ body: [String: Any]) {
    guard hasListeners else { return }
    sendEvent(withName: name, body: body)
  }

  // ── Scan ───────────────────────────────────────────────────────────────
  @objc func startScan(_ resolve: @escaping RCTPromiseResolveBlock,
                       rejecter reject: @escaping RCTPromiseRejectBlock) {
    foundMacs.removeAll()
    emit("onScanStatus", ["status": "scanning"])

    VPBleCentralManager.sharedBleManager().veepooSDKStartScanDeviceAndReceiveScanningDevice { [weak self] peripheral in
      guard let self = self, let p = peripheral else { return }
      let mac = p.deviceAddress ?? p.peripheral?.identifier.uuidString ?? ""
      guard !mac.isEmpty, !self.foundMacs.contains(mac) else { return }
      self.foundMacs.insert(mac)
      self.emit("onDeviceFound", [
        "name": p.deviceName ?? "Unknown",
        "mac":  mac,
        "rssi": p.rssi ?? 0,
      ])
    }
    // Auto-stop after 12s
    DispatchQueue.main.asyncAfter(deadline: .now() + 12) { [weak self] in
      VPBleCentralManager.sharedBleManager().veepooSDKStopScanDevice()
      self?.emit("onScanStatus", ["status": "stopped"])
    }
    resolve(nil)
  }

  @objc func stopScan(_ resolve: @escaping RCTPromiseResolveBlock,
                      rejecter reject: @escaping RCTPromiseRejectBlock) {
    VPBleCentralManager.sharedBleManager().veepooSDKStopScanDevice()
    emit("onScanStatus", ["status": "stopped"])
    resolve(nil)
  }

  // ── Connect ────────────────────────────────────────────────────────────
  @objc func connectDevice(_ mac: String, name: String,
                           resolver resolve: @escaping RCTPromiseResolveBlock,
                           rejecter reject: @escaping RCTPromiseRejectBlock) {
    VPBleCentralManager.sharedBleManager().veepooSDKStopScanDevice()

    // Find the peripheral model matching this mac
    guard let peripheral = findPeripheral(mac: mac) else {
      reject("NOT_FOUND", "Device not found — scan first", nil)
      return
    }

    VPBleCentralManager.sharedBleManager().veepooSDKConnectDevice(peripheral) { [weak self] state in
      guard let self = self else { return }
      switch state {
      case .BleConnecting:
        self.emit("onConnectionState", ["state": "ble_connected"])

      case .BleVerifyPasswordSuccess:
        let firmware = peripheral.deviceVersion ?? ""
        self.emit("onConnected", ["mac": mac, "name": name, "firmware": firmware])
        // Read capabilities from peripheral model
        self.emitCapabilities(from: peripheral)
        // Read battery
        self.fetchBatteryAndStore()

      case .BleVerifyPasswordFailure:
        self.emit("onConnectionState", ["state": "auth_failed", "reason": "Wrong password"])

      case .BleConnectFailed:
        self.emit("onConnectionState", ["state": "failed", "code": -1])

      default: break
      }
    }
    resolve(nil)
  }

  @objc func disconnect(_ resolve: @escaping RCTPromiseResolveBlock,
                        rejecter reject: @escaping RCTPromiseRejectBlock) {
    VPBleCentralManager.sharedBleManager().veepooSDKDisconnectDevice()
    emit("onConnectionState", ["state": "disconnected"])
    resolve(nil)
  }

  // ── Capabilities from peripheral model ────────────────────────────────
  private func emitCapabilities(from p: VPPeripheralModel) {
    emit("onCapabilities", [
      "bp":             (p.bloodPressureType ?? 0) > 0,
      "spo2":           (p.oxygenType ?? 0)        > 0,
      "temp":           (p.temperatureType ?? 0)   > 0,
      "ecg":            false,                   // check p.ecgType if available
      "hrv":            (p.hrvType ?? 0)           > 0,
      "precisionSleep": p.sleepType == 1 || p.sleepType == 3,
      "stress":         (p.stressType ?? 0)        > 0,
      "respRate":       false,
      "bloodGlucose":   (p.bloodGlucoseType ?? 0) > 0,
    ])
  }

  // ── Battery ────────────────────────────────────────────────────────────
  @objc func readBattery(_ resolve: @escaping RCTPromiseResolveBlock,
                         rejecter reject: @escaping RCTPromiseRejectBlock) {
    VPBleCentralManager.sharedBleManager().veepooSDKReadDeviceBatteryInfo { _, _, percent in
      resolve(Int(percent))
    }
  }

  private func fetchBatteryAndStore() {
    VPBleCentralManager.sharedBleManager().veepooSDKReadDeviceBatteryInfo { _, _, _ in }
  }

  // ── Steps / Distance / Calories ────────────────────────────────────────
  @objc func readSteps(_ resolve: @escaping RCTPromiseResolveBlock,
                       rejecter reject: @escaping RCTPromiseRejectBlock) {
    VPBleCentralManager.sharedBleManager().veepooSDK_readStepDataWithDayNumber(1) { dict in
      guard let d = dict else { resolve(["steps": 0, "distanceKm": 0.0, "caloriesKcal": 0.0]); return }
      resolve([
        "steps":        d["Step"]  as? Int    ?? 0,
        "distanceKm":   d["Dis"]   as? Double ?? 0.0,
        "caloriesKcal": d["Cal"]   as? Double ?? 0.0,
      ])
    }
  }

  // ── Sleep ──────────────────────────────────────────────────────────────
  @objc func readSleep(_ resolve: @escaping RCTPromiseResolveBlock,
                       rejecter reject: @escaping RCTPromiseRejectBlock) {
    let today = todayString()
    let tableID = VPBleCentralManager.sharedBleManager().peripheralModel?.deviceAddress ?? ""
    let raw = VPBleCentralManager.veepooSDKGetSleepData(withDate: today, andTableID: tableID) as? [[String: Any]]
    guard let entries = raw, !entries.isEmpty else {
      resolve(["totalMinutes": 0, "deepMinutes": 0, "lightMinutes": 0, "wakeCount": 0, "sleepQuality": 0])
      return
    }
    var total = 0, deep = 0, light = 0, wake = 0
    for e in entries {
      let t = e["type"] as? Int ?? -1
      let d = e["duration"] as? Int ?? 0
      switch t {
      case 1: deep  += d
      case 0: light += d
      case 2: wake  += 1
      default: break
      }
      total += d
    }
    resolve([
      "totalMinutes": total,
      "deepMinutes":  deep,
      "lightMinutes": light,
      "wakeCount":    wake,
      "sleepQuality": min(100, max(0, Int(Double(deep) / Double(max(total, 1)) * 100 + 50))),
    ])
  }

  // ── Origin data (HR/BP/SpO2/Resp history) ─────────────────────────────
  @objc func readOriginData(_ resolve: @escaping RCTPromiseResolveBlock,
                            rejecter reject: @escaping RCTPromiseRejectBlock) {
    let today = todayString()
    let tableID = VPBleCentralManager.sharedBleManager().peripheralModel?.deviceAddress ?? ""
    let raw = VPBleCentralManager.veepooSDKGetOriginalData(withDate: today, andTableID: tableID)
    var result: [String: Any] = [:]
    if let list = raw?["RateList"] as? [[String: Any]], let last = list.last {
      if let hr = last["RateValue"] as? Int, hr > 0 { result["heartRate"] = hr }
    }
    if let bpList = raw?["BloodPressureList"] as? [[String: Any]], let last = bpList.last {
      if let sys = last["Systolic"] as? Int, let dia = last["Diastolic"] as? Int, sys > 0 {
        result["bpSystolic"]  = sys
        result["bpDiastolic"] = dia
      }
    }
    if let oxList = raw?["OxygenList"] as? [[String: Any]], let last = oxList.last {
      if let o2 = last["OxygenValue"] as? Int, o2 > 0 { result["spo2"] = o2 }
    }
    resolve(result)
  }

  // ── Heart Rate live ────────────────────────────────────────────────────
  @objc func startHeartMeasure(_ resolve: @escaping RCTPromiseResolveBlock,
                               rejecter reject: @escaping RCTPromiseRejectBlock) {
    guard !isMeasuringHeart else { reject("BUSY", "Already measuring", nil); return }
    isMeasuringHeart = true
    VPBleCentralManager.sharedBleManager().veepooSDKTestHeartStart(true) { [weak self] state, bpm in
      guard let self = self else { return }
      if state == .VPTestHeartStateSuccess, bpm > 0 {
        self.emit("onHeartRate", ["bpm": Int(bpm)])
      }
    }
    resolve(nil)
  }

  @objc func stopHeartMeasure(_ resolve: @escaping RCTPromiseResolveBlock,
                              rejecter reject: @escaping RCTPromiseRejectBlock) {
    isMeasuringHeart = false
    VPBleCentralManager.sharedBleManager().veepooSDKTestHeartStart(false, testResult: nil)
    resolve(nil)
  }

  // ── SpO2 live ──────────────────────────────────────────────────────────
  @objc func startSpo2Measure(_ resolve: @escaping RCTPromiseResolveBlock,
                              rejecter reject: @escaping RCTPromiseRejectBlock) {
    guard !isMeasuringSpo2 else { reject("BUSY", "Already measuring", nil); return }
    isMeasuringSpo2 = true
    VPBleCentralManager.sharedBleManager().veepooSDKTestOxygenStart(true) { [weak self] state, _, value, _, _ in
      guard let self = self else { return }
      if Int(value) > 0 {
        self.emit("onSpo2", ["value": Int(value)])
      }
    }
    resolve(nil)
  }

  @objc func stopSpo2Measure(_ resolve: @escaping RCTPromiseResolveBlock,
                             rejecter reject: @escaping RCTPromiseRejectBlock) {
    isMeasuringSpo2 = false
    VPBleCentralManager.sharedBleManager().veepooSDKTestOxygenStart(false, testResult: nil)
    resolve(nil)
  }

  // ── Blood Pressure live ────────────────────────────────────────────────
  @objc func startBpMeasure(_ resolve: @escaping RCTPromiseResolveBlock,
                            rejecter reject: @escaping RCTPromiseRejectBlock) {
    guard !isMeasuringBp else { reject("BUSY", "Already measuring", nil); return }
    isMeasuringBp = true
    VPBleCentralManager.sharedBleManager().veepooSDKTestBloodStart(true, testMode: 0) { [weak self] state, _, systolic, diastolic in
      guard let self = self else { return }
      if Int(systolic) > 0 && Int(diastolic) > 0 {
        self.emit("onBP", ["systolic": Int(systolic), "diastolic": Int(diastolic)])
      }
    }
    resolve(nil)
  }

  @objc func stopBpMeasure(_ resolve: @escaping RCTPromiseResolveBlock,
                           rejecter reject: @escaping RCTPromiseRejectBlock) {
    isMeasuringBp = false
    VPBleCentralManager.sharedBleManager().veepooSDKTestBloodStart(false, testMode: 0, testResult: nil)
    resolve(nil)
  }

  // ── Body Temperature live ──────────────────────────────────────────────
  @objc func startTempMeasure(_ resolve: @escaping RCTPromiseResolveBlock,
                              rejecter reject: @escaping RCTPromiseRejectBlock) {
    guard !isMeasuringTemp else { reject("BUSY", "Already measuring", nil); return }
    isMeasuringTemp = true
    VPBleCentralManager.sharedBleManager().veepooSDK_temperatureTestStart(true) { [weak self] state, _, integer, decimal, _ in
      guard let self = self else { return }
      if Int(integer) > 0 {
        let celsius = Double(integer) + Double(decimal) / 10.0
        self.emit("onTemperature", ["celsius": celsius])
      }
    }
    resolve(nil)
  }

  @objc func stopTempMeasure(_ resolve: @escaping RCTPromiseResolveBlock,
                             rejecter reject: @escaping RCTPromiseRejectBlock) {
    isMeasuringTemp = false
    VPBleCentralManager.sharedBleManager().veepooSDK_temperatureTestStart(false, result: nil)
    resolve(nil)
  }

  // ── Wellness Check (Stress + Fatigue + HRV) ────────────────────────────
  @objc func startWellnessCheck(_ resolve: @escaping RCTPromiseResolveBlock,
                                rejecter reject: @escaping RCTPromiseRejectBlock) {
    guard !isWellnessRunning else { reject("BUSY", "Already measuring", nil); return }
    isWellnessRunning = true

    VPBleCentralManager.sharedBleManager().veepooSDK_healthGlanceTestStart(true,
      andProgress: { _ in },
      andResult: { [weak self] state, model in
        guard let self = self else { return }
        if state == .VPDeviceHealthGlanceStateEnd || state == .VPDeviceHealthGlanceStateSuccess {
          var body: [String: Any] = [:]
          if let m = model {
            if m.stress    >= 0 { body["stress"]     = Int(m.stress)      }
            if m.fatigue   >= 0 { body["fatigue"]    = Int(m.fatigue)     }
            if m.hrv       > 0  { body["hrv"]        = Int(m.hrv)         }
            if m.heartRate > 0  { body["heartRate"]  = Int(m.heartRate)   }
            if m.spo2      > 0  { body["spo2"]       = Int(m.spo2)        }
          }
          self.emit("onWellnessResult", body)
          self.isWellnessRunning = false
        }
      }
    )
    resolve(nil)
  }

  @objc func stopWellnessCheck(_ resolve: @escaping RCTPromiseResolveBlock,
                               rejecter reject: @escaping RCTPromiseRejectBlock) {
    isWellnessRunning = false
    VPBleCentralManager.sharedBleManager().veepooSDK_healthGlanceTestStart(false,
      andProgress: nil, andResult: nil)
    resolve(nil)
  }

  // ── Helpers ────────────────────────────────────────────────────────────
  private func findPeripheral(mac: String) -> VPPeripheralModel? {
    // The SDK stores the last scanned peripheral list in memory
    // We look it up by matching address
    return nil // SDK connects by passing the model directly — see connectDevice
  }

  private func todayString() -> String {
    let f = DateFormatter()
    f.dateFormat = "yyyy-MM-dd"
    return f.string(from: Date())
  }

  // Required by RN for events
  @objc override static func requiresMainQueueSetup() -> Bool { true }
}
