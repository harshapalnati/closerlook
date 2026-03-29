package com.gband.test.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.gband.test.AppData;
import com.gband.test.R;
import com.inuker.bluetooth.library.Code;
import com.inuker.bluetooth.library.model.BleGattProfile;
import com.inuker.bluetooth.library.search.SearchResult;
import com.inuker.bluetooth.library.search.response.SearchResponse;
import com.inuker.bluetooth.library.utils.BluetoothUtils;
import com.veepoo.protocol.VPOperateManager;
import com.veepoo.protocol.listener.base.IBleWriteResponse;
import com.veepoo.protocol.listener.base.IConnectResponse;
import com.veepoo.protocol.listener.base.INotifyResponse;
import com.veepoo.protocol.listener.data.IBatteryDataListener;
import com.veepoo.protocol.listener.data.IDeviceFuctionDataListener;
import com.veepoo.protocol.listener.data.IPwdDataListener;
import com.veepoo.protocol.model.datas.BatteryData;
import com.veepoo.protocol.model.datas.DeviceFunctionPackage1;
import com.veepoo.protocol.model.datas.DeviceFunctionPackage2;
import com.veepoo.protocol.model.datas.DeviceFunctionPackage3;
import com.veepoo.protocol.model.datas.DeviceFunctionPackage4;
import com.veepoo.protocol.model.datas.DeviceFunctionPackage5;
import com.veepoo.protocol.model.datas.FunctionDeviceSupportData;
import com.veepoo.protocol.model.datas.PwdData;

import java.util.ArrayList;
import java.util.List;

public class DeviceFragment extends Fragment {

    private static final int PERM_CODE = 101;

    private TextView tvStatus, tvDeviceName, tvBattery, tvFirmware, tvMac, tvLog;
    private TextView tvCapBp, tvCapSpo2, tvCapTemp, tvCapHrv, tvCapStress,
                     tvCapResp, tvCapEcg, tvCapSleep;
    private View statusDot;
    private LinearLayout cardDeviceInfo, containerDevices;
    private androidx.cardview.widget.CardView cardCapabilities;
    private Button btnScan, btnDisconnect;

    private final List<SearchResult> foundDevices = new ArrayList<>();
    private final Handler handler = new Handler();
    private final StringBuilder logBuf = new StringBuilder();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_device, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvStatus        = view.findViewById(R.id.tv_connection_status);
        tvDeviceName    = view.findViewById(R.id.tv_device_name);
        tvBattery       = view.findViewById(R.id.tv_battery);
        tvFirmware      = view.findViewById(R.id.tv_firmware);
        tvMac           = view.findViewById(R.id.tv_mac);
        tvLog           = view.findViewById(R.id.tv_log);
        statusDot       = view.findViewById(R.id.status_dot);
        cardDeviceInfo  = view.findViewById(R.id.card_device_info);
        cardCapabilities = view.findViewById(R.id.card_capabilities);
        containerDevices = view.findViewById(R.id.container_devices);
        btnScan         = view.findViewById(R.id.btn_scan);
        btnDisconnect   = view.findViewById(R.id.btn_disconnect);
        tvCapBp    = view.findViewById(R.id.tv_cap_bp);
        tvCapSpo2  = view.findViewById(R.id.tv_cap_spo2);
        tvCapTemp  = view.findViewById(R.id.tv_cap_temp);
        tvCapHrv   = view.findViewById(R.id.tv_cap_hrv);
        tvCapStress = view.findViewById(R.id.tv_cap_stress);
        tvCapResp  = view.findViewById(R.id.tv_cap_resp);
        tvCapEcg   = view.findViewById(R.id.tv_cap_ecg);
        tvCapSleep = view.findViewById(R.id.tv_cap_sleep);

        btnScan.setOnClickListener(v -> startScan());
        btnDisconnect.setOnClickListener(v -> disconnect());

        refreshConnectionUI();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshConnectionUI();
        if (AppData.getInstance().capabilitiesLoaded) showCapabilities();
    }

    // ── UI helpers ────────────────────────────────────────────────────────
    private void refreshConnectionUI() {
        AppData d = AppData.getInstance();
        if (d.isConnected) {
            setStatus("Connected", "#02B875");
            tvDeviceName.setText(d.deviceName.isEmpty() ? "G Band" : d.deviceName);
            cardDeviceInfo.setVisibility(View.VISIBLE);
            btnScan.setVisibility(View.GONE);
            btnDisconnect.setVisibility(View.VISIBLE);
            if (d.batteryPercent >= 0) tvBattery.setText(d.batteryPercent + "%");
            if (!d.firmwareVersion.isEmpty()) tvFirmware.setText(d.firmwareVersion);
            if (!d.deviceMac.isEmpty()) tvMac.setText(d.deviceMac);
        } else {
            setStatus("Not Connected", "#E8384F");
            tvDeviceName.setText("No device paired");
            cardDeviceInfo.setVisibility(View.GONE);
            btnScan.setVisibility(View.VISIBLE);
            btnDisconnect.setVisibility(View.GONE);
        }
    }

    private void setStatus(String label, String hexColor) {
        tvStatus.setText(label);
        tvStatus.setTextColor(Color.parseColor(hexColor));
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(Color.parseColor(hexColor));
        statusDot.setBackground(dot);
    }

    private void log(String msg) {
        logBuf.append(msg).append("\n");
        if (tvLog != null) {
            requireActivity().runOnUiThread(() -> tvLog.setText(logBuf.toString()));
        }
    }

    // ── Scan ──────────────────────────────────────────────────────────────
    private void startScan() {
        if (!hasPermissions()) {
            requestPermissions();
            return;
        }
        if (!BluetoothUtils.isBluetoothEnabled()) {
            Toast.makeText(getContext(), "Please enable Bluetooth", Toast.LENGTH_SHORT).show();
            return;
        }

        foundDevices.clear();
        containerDevices.removeAllViews();
        containerDevices.setVisibility(View.GONE);
        logBuf.setLength(0);
        log("Scanning for G Band...");
        btnScan.setEnabled(false);
        btnScan.setText("Scanning...");
        setStatus("Scanning\u2026", "#F5B731");

        VPOperateManager.getInstance().stopScanDevice();
        VPOperateManager.getInstance().disconnectWatch(code -> {});

        VPOperateManager.getInstance().startScanDevice(new SearchResponse() {
            @Override
            public void onSearchStarted() {
                requireActivity().runOnUiThread(() -> log("Scan started."));
            }

            @Override
            public void onDeviceFounded(SearchResult device) {
                for (SearchResult d : foundDevices) {
                    if (d.getAddress().equals(device.getAddress())) return;
                }
                foundDevices.add(device);
                requireActivity().runOnUiThread(() -> addDeviceRow(device));
            }

            @Override
            public void onSearchStopped() {
                requireActivity().runOnUiThread(() -> {
                    btnScan.setEnabled(true);
                    btnScan.setText(getString(R.string.btn_scan));
                    if (foundDevices.isEmpty()) {
                        log(getString(R.string.no_devices_found));
                        setStatus("Not Connected", "#E8384F");
                    } else {
                        log("Found " + foundDevices.size() + " device(s). Tap one to connect.");
                        containerDevices.setVisibility(View.VISIBLE);
                    }
                });
            }

            @Override
            public void onSearchCanceled() {
                requireActivity().runOnUiThread(() -> {
                    btnScan.setEnabled(true);
                    btnScan.setText(getString(R.string.btn_scan));
                });
            }
        });

        // Auto-stop scan after 12 seconds
        handler.postDelayed(() -> VPOperateManager.getInstance().stopScanDevice(), 12000);
    }

    private void addDeviceRow(SearchResult device) {
        String name = device.getName();
        if (name == null || name.isEmpty()) name = "Unknown Device";

        Button row = new Button(requireContext());
        row.setText(name + "\n" + device.getAddress());
        row.setTextSize(16f);
        row.setTextColor(Color.WHITE);
        row.setBackgroundColor(Color.parseColor("#1A1A1F"));
        row.setPadding(24, 24, 24, 24);
        row.setMinHeight(56);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 0, 0, 8);
        row.setLayoutParams(lp);

        final String mac  = device.getAddress();
        final String dname = name;
        row.setOnClickListener(v -> connectTo(mac, dname));
        containerDevices.addView(row);
    }

    // ── Connect ───────────────────────────────────────────────────────────
    private void connectTo(String mac, String name) {
        log("Connecting to " + name + "...");
        setStatus("Connecting\u2026", "#F5B731");
        VPOperateManager.getInstance().stopScanDevice();

        VPOperateManager.getInstance().connectDevice(mac, name,
            new IConnectResponse() {
                @Override
                public void connectState(int code, BleGattProfile profile, boolean isOad) {
                    requireActivity().runOnUiThread(() -> {
                        if (code == Code.REQUEST_SUCCESS) {
                            log("BLE connected. Authenticating...");
                        } else {
                            log("Connection failed (code " + code + ")");
                            setStatus("Not Connected", "#E8384F");
                        }
                    });
                }
            },
            new INotifyResponse() {
                @Override
                public void notifyState(int state) {
                    requireActivity().runOnUiThread(() -> {
                        if (state == Code.REQUEST_SUCCESS) {
                            handler.postDelayed(() -> verifyPassword(mac, name), 1000);
                        } else {
                            log("Notify failed (code " + state + ")");
                        }
                    });
                }
            }
        );
    }

    private void verifyPassword(String mac, String name) {
        VPOperateManager.getInstance().confirmDevicePwd(
            code -> {},
            new IPwdDataListener() {
                @Override
                public void onPwdDataChange(PwdData pwdData) {
                    requireActivity().runOnUiThread(() -> {
                        String status = pwdData.getmStatus().toString();
                        if (status.contains("SUCESS") || status.contains("SUCCESS")) {
                            AppData d = AppData.getInstance();
                            d.isConnected = true;
                            d.deviceName = name;
                            d.deviceMac = mac;
                            d.firmwareVersion = pwdData.getDeviceVersion() != null
                                ? pwdData.getDeviceVersion() : "";
                            log("Connected! Device: " + name);
                            refreshConnectionUI();
                            readBattery();
                        } else {
                            log("Auth status: " + status);
                        }
                    });
                }
            },
            new IDeviceFuctionDataListener() {
                @Override
                public void onFunctionSupportDataChange(FunctionDeviceSupportData caps) {
                    // This is the device telling us exactly what hardware it has.
                    // Store every flag we care about in AppData.
                    AppData d = AppData.getInstance();
                    d.supportsBp           = caps.isSupportBp();
                    d.supportsSpo2         = caps.isSupportSpoH();
                    d.supportsTemp         = caps.isSupportTemperatureFunction();
                    d.supportsEcg          = caps.isSupportECG();
                    d.supportsHrv          = caps.isSupportHRV();
                    d.supportsPrecisionSleep = caps.isSupportPrecisionSleep();
                    d.supportsStress       = caps.isSupportStressDetect();
                    d.supportsRespRate     = caps.isSupportBeathFunction();
                    d.supportsBloodGlucose = caps.isSupportBloodGlucose();
                    d.capabilitiesLoaded   = true;

                    // Log what the device actually supports
                    StringBuilder sb = new StringBuilder("Device capabilities:\n");
                    sb.append("Blood Pressure: ").append(d.supportsBp).append("\n");
                    sb.append("SpO2:           ").append(d.supportsSpo2).append("\n");
                    sb.append("Temperature:    ").append(d.supportsTemp).append("\n");
                    sb.append("ECG:            ").append(d.supportsEcg).append("\n");
                    sb.append("HRV:            ").append(d.supportsHrv).append("\n");
                    sb.append("Precision Sleep:").append(d.supportsPrecisionSleep).append("\n");
                    sb.append("Stress:         ").append(d.supportsStress).append("\n");
                    sb.append("Resp Rate:      ").append(d.supportsRespRate).append("\n");
                    sb.append("Blood Glucose:  ").append(d.supportsBloodGlucose);
                    log(sb.toString());
                }
                @Override public void onDeviceFunctionPackage1Report(DeviceFunctionPackage1 d) {}
                @Override public void onDeviceFunctionPackage2Report(DeviceFunctionPackage2 d) {}
                @Override public void onDeviceFunctionPackage3Report(DeviceFunctionPackage3 d) {}
                @Override public void onDeviceFunctionPackage4Report(DeviceFunctionPackage4 d) {}
                @Override public void onDeviceFunctionPackage5Report(DeviceFunctionPackage5 d) {}
            },
            null, null, "0000", false
        );
    }

    private void readBattery() {
        VPOperateManager.getInstance().readBattery(
            code -> {},
            new IBatteryDataListener() {
                @Override
                public void onDataChange(BatteryData data) {
                    int pct = data.isPercent() ? data.getBatteryPercent() : data.getBatteryLevel() * 25;
                    AppData.getInstance().batteryPercent = pct;
                    requireActivity().runOnUiThread(() -> {
                        tvBattery.setText(pct + "%");
                        cardDeviceInfo.setVisibility(View.VISIBLE);
                        // Show capabilities if already loaded (they arrive just before battery)
                        if (AppData.getInstance().capabilitiesLoaded) showCapabilities();
                    });
                }
            }
        );
    }

    /** Render the capability card — green tick or grey cross per feature */
    private void showCapabilities() {
        AppData d = AppData.getInstance();
        cardCapabilities.setVisibility(View.VISIBLE);
        setCapRow(tvCapBp,    "Blood Pressure",       d.supportsBp);
        setCapRow(tvCapSpo2,  "Blood Oxygen (SpO2)",  d.supportsSpo2);
        setCapRow(tvCapTemp,  "Body Temperature",     d.supportsTemp);
        setCapRow(tvCapHrv,   "HRV",                  d.supportsHrv);
        setCapRow(tvCapStress,"Stress Detection",     d.supportsStress);
        setCapRow(tvCapResp,  "Respiratory Rate",     d.supportsRespRate);
        setCapRow(tvCapEcg,   "ECG",                  d.supportsEcg);
        setCapRow(tvCapSleep, "Precision Sleep (REM)",d.supportsPrecisionSleep);
    }

    private void setCapRow(TextView tv, String label, boolean supported) {
        if (supported) {
            tv.setText("\u2713  " + label);
            tv.setTextColor(Color.parseColor("#02B875")); // green
        } else {
            tv.setText("\u2715  " + label);
            tv.setTextColor(Color.parseColor("#5A5C65")); // grey
        }
    }

    // ── Disconnect ────────────────────────────────────────────────────────
    private void disconnect() {
        VPOperateManager.getInstance().disconnectWatch(code -> {
            AppData.getInstance().isConnected = false;
            AppData.getInstance().resetReadings();
            requireActivity().runOnUiThread(() -> {
                log("Disconnected.");
                refreshConnectionUI();
            });
        });
    }

    // ── Permissions ───────────────────────────────────────────────────────
    private boolean hasPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }
        return ContextCompat.checkSelfPermission(requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(requireActivity(),
                new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                }, PERM_CODE);
        } else {
            ActivityCompat.requestPermissions(requireActivity(),
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERM_CODE);
        }
    }
}
