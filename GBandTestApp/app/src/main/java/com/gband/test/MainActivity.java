package com.gband.test;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.inuker.bluetooth.library.Code;
import com.inuker.bluetooth.library.model.BleGattProfile;
import com.inuker.bluetooth.library.search.SearchResult;
import com.inuker.bluetooth.library.search.response.SearchResponse;
import com.inuker.bluetooth.library.utils.BluetoothUtils;
import com.veepoo.protocol.listener.base.IABluetoothStateListener;
import com.veepoo.protocol.VPOperateManager;
import com.veepoo.protocol.listener.base.IBleWriteResponse;
import com.veepoo.protocol.listener.base.IConnectResponse;
import com.veepoo.protocol.listener.base.INotifyResponse;
import com.veepoo.protocol.listener.data.IBatteryDataListener;
import com.veepoo.protocol.listener.data.IDeviceFuctionDataListener;
import com.veepoo.protocol.listener.data.IHeartDataListener;
import com.veepoo.protocol.listener.data.IOriginDataListener;
import com.veepoo.protocol.listener.data.IPwdDataListener;
import com.veepoo.protocol.listener.data.ISportDataListener;
import com.veepoo.protocol.model.datas.BatteryData;
import com.veepoo.protocol.model.datas.HeartData;
import com.veepoo.protocol.model.datas.DeviceFunctionPackage1;
import com.veepoo.protocol.model.datas.DeviceFunctionPackage2;
import com.veepoo.protocol.model.datas.DeviceFunctionPackage3;
import com.veepoo.protocol.model.datas.DeviceFunctionPackage4;
import com.veepoo.protocol.model.datas.DeviceFunctionPackage5;
import com.veepoo.protocol.model.datas.FunctionDeviceSupportData;
import com.veepoo.protocol.model.datas.OriginData;
import com.veepoo.protocol.model.datas.OriginHalfHourData;
import com.veepoo.protocol.model.datas.PwdData;
import com.veepoo.protocol.model.datas.SleepData;
import com.veepoo.protocol.model.datas.SportData;
import com.veepoo.protocol.listener.data.IAllHealthDataListener;
import com.veepoo.protocol.listener.data.ISleepDataListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 100;
    
    private Button btnScan;
    private Button btnConnect;
    private Button btnReadData;
    private Button btnHeartLive;
    private Button btnDisconnect;
    private TextView tvStatus;
    private TextView tvSteps;
    private TextView tvHeartRate;
    private TextView tvBattery;
    private TextView tvSpO2;
    
    private List<SearchResult> deviceList = new ArrayList<>();
    private String selectedDeviceMac = null;
    private String selectedDeviceName = null;
    private boolean isConnected = false;
    
    private Handler handler = new Handler();
    private StringBuilder logBuilder = new StringBuilder();
    
    private void log(String msg) {
        logBuilder.append("> ").append(msg).append("\n");
        if (tvStatus != null) {
            runOnUiThread(() -> tvStatus.setText(logBuilder.toString()));
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        VPOperateManager.getInstance().init(this);
        VPOperateManager.getInstance().registerBluetoothStateListener(new IABluetoothStateListener() {
            @Override
            public void onBluetoothStateChanged(boolean openOrClosed) {
                runOnUiThread(() -> {
                    if (openOrClosed) {
                        log("Bluetooth ON. Tap Scan.");
                    } else {
                        log("Bluetooth OFF!");
                    }
                });
            }
        });
        checkPermissions();
    }
    
    private void initViews() {
        btnScan = findViewById(R.id.btnScan);
        btnConnect = findViewById(R.id.btnConnect);
        btnReadData = findViewById(R.id.btnReadData);
        btnHeartLive = findViewById(R.id.btnHeartLive);
        btnDisconnect = findViewById(R.id.btnDisconnect);
        tvStatus = findViewById(R.id.tvStatus);
        tvSteps = findViewById(R.id.tvSteps);
        tvHeartRate = findViewById(R.id.tvHeartRate);
        tvBattery = findViewById(R.id.tvBattery);
        tvSpO2 = findViewById(R.id.tvSpO2);
        
        btnScan.setOnClickListener(v -> startScan());
        btnConnect.setOnClickListener(v -> connectToDevice());
        btnReadData.setOnClickListener(v -> readMetrics());
        btnHeartLive.setOnClickListener(v -> startHeartLive());
        btnDisconnect.setOnClickListener(v -> disconnectDevice());
    }
    
    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) 
                    != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) 
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT},
                    PERMISSION_REQUEST_CODE);
                return;
            }
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                PERMISSION_REQUEST_CODE);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show();
    }
    
    private void startScan() {
        deviceList.clear();
        logBuilder.setLength(0);
        log("=== Starting Scan ===");
        log("Scanning...");
        
        VPOperateManager.getInstance().stopScanDevice();
        VPOperateManager.getInstance().disconnectWatch(code -> {});
        
        VPOperateManager.getInstance().startScanDevice(new SearchResponse() {
            @Override
            public void onSearchStarted() {
                runOnUiThread(() -> log("Scan started..."));
            }
            
            @Override
            public void onDeviceFounded(SearchResult device) {
                String name = device.getName();
                String displayName = (name != null && !name.isEmpty()) ? name : "Unknown Device";
                
                boolean exists = false;
                for (SearchResult d : deviceList) {
                    if (d.getAddress().equals(device.getAddress())) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    deviceList.add(device);
                    runOnUiThread(() -> {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Found devices:\n");
                        for (SearchResult d : deviceList) {
                            String n = d.getName();
                            sb.append((n != null && !n.isEmpty()) ? n : "Unknown")
                              .append(" - ").append(d.getAddress()).append("\n");
                        }
                        log(sb.toString());
                    });
                }
            }
            
            @Override
            public void onSearchStopped() {
                runOnUiThread(() -> {
                    if (deviceList.isEmpty()) {
                        log("No devices found. Tap to scan again.");
                    } else {
                        log("Select a device from the list above.");
                    }
                });
            }
            
            @Override
            public void onSearchCanceled() {
                runOnUiThread(() -> log("Scan cancelled"));
            }
        });
        
        handler.postDelayed(() -> {
            VPOperateManager.getInstance().stopScanDevice();
        }, 10000);
    }
    
    private void connectToDevice() {
        if (!BluetoothUtils.isBluetoothEnabled()) {
            Toast.makeText(this, "Please enable Bluetooth!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (deviceList.isEmpty()) {
            Toast.makeText(this, "No devices found. Scan first!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (selectedDeviceMac == null && !deviceList.isEmpty()) {
            selectedDeviceMac = deviceList.get(0).getAddress();
            selectedDeviceName = deviceList.get(0).getName();
        }
        
        if (selectedDeviceMac == null) {
            Toast.makeText(this, "Please select a device", Toast.LENGTH_SHORT).show();
            return;
        }
        
        log("Connecting to " + selectedDeviceName + "...");
        
        VPOperateManager.getInstance().stopScanDevice();
        
        VPOperateManager.getInstance().connectDevice(selectedDeviceMac, selectedDeviceName, new IConnectResponse() {
            @Override
            public void connectState(int code, BleGattProfile profile, boolean isOadModel) {
                runOnUiThread(() -> {
                    if (code == Code.REQUEST_SUCCESS) {
                        log("Connected! Setting up notification...");
                    } else {
                        log("Connection failed, code: " + code);
                        isConnected = false;
                    }
                });
            }
        }, new INotifyResponse() {
            @Override
            public void notifyState(int state) {
                runOnUiThread(() -> {
                    if (state == Code.REQUEST_SUCCESS) {
                        log("Notify OK! Waiting for device...");
                        new Handler().postDelayed(() -> {
                            verifyPassword();
                        }, 1000);
                    } else {
                        log("Notify failed, code: " + state);
                    }
                });
            }
        });
    }
    
    private void verifyPassword() {
        VPOperateManager.getInstance().confirmDevicePwd(
            new IBleWriteResponse() {
                @Override
                public void onResponse(int code) {
                    if (code != Code.REQUEST_SUCCESS) {
                        runOnUiThread(() -> log("Password confirm failed: " + code));
                    }
                }
            },
            new IPwdDataListener() {
                @Override
                public void onPwdDataChange(PwdData pwdData) {
                    runOnUiThread(() -> {
                        String status = "Status: " + pwdData.getmStatus();
                        String deviceNum = "Device#: " + pwdData.getDeviceNumber();
                        String version = "Version: " + pwdData.getDeviceVersion();
                        log(status + "\n" + deviceNum + "\n" + version);
                        
                        // TIME_SUCESS means device responded - auth worked
                        String statusStr = pwdData.getmStatus().toString();
                        if (statusStr.contains("SUCESS") || statusStr.contains("SUCCESS")) {
                            log("Auth SUCCESS! Device connected: " + selectedDeviceName);
                            isConnected = true;
                            btnScan.setEnabled(false);
                            btnConnect.setText("Connected");
                            btnConnect.setEnabled(false);
                            btnReadData.setEnabled(true);
                            btnHeartLive.setEnabled(true);
                            btnDisconnect.setVisibility(View.VISIBLE);
                        } else {
                            log("Auth status: " + statusStr);
                        }
                    });
                }
            },
            new IDeviceFuctionDataListener() {
                @Override
                public void onFunctionSupportDataChange(FunctionDeviceSupportData functionSupport) {}
                @Override
                public void onDeviceFunctionPackage1Report(DeviceFunctionPackage1 deviceFunctionPackage1) {}
                @Override
                public void onDeviceFunctionPackage2Report(DeviceFunctionPackage2 deviceFunctionPackage2) {}
                @Override
                public void onDeviceFunctionPackage3Report(DeviceFunctionPackage3 deviceFunctionPackage3) {}
                @Override
                public void onDeviceFunctionPackage4Report(DeviceFunctionPackage4 deviceFunctionPackage4) {}
                @Override
                public void onDeviceFunctionPackage5Report(DeviceFunctionPackage5 deviceFunctionPackage5) {}
            },
            null, null, "0000", false
        );
    }
    
    private void readMetrics() {
        if (!isConnected) {
            Toast.makeText(this, "Not connected!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        log("=== Reading ALL Device Data ===");
        
        // 1. Read Steps
        log("1. Reading Steps...");
        VPOperateManager.getInstance().readSportStep(new IBleWriteResponse() {
            @Override
            public void onResponse(int code) {
                log("Steps code: " + code);
            }
        }, new ISportDataListener() {
            @Override
            public void onSportDataChange(SportData sportData) {
                log("STEPS: " + sportData.getStep());
                tvSteps.setText(String.valueOf(sportData.getStep()));
            }
        });
        
        // 2. Read Battery
        log("2. Reading Battery...");
        VPOperateManager.getInstance().readBattery(new IBleWriteResponse() {
            @Override
            public void onResponse(int code) {
                log("Battery code: " + code);
            }
        }, new IBatteryDataListener() {
            @Override
            public void onDataChange(BatteryData batteryData) {
                String battery = batteryData.isPercent() ? batteryData.getBatteryPercent() + "%" : batteryData.getBatteryLevel() + "/4";
                log("BATTERY: " + battery);
                tvBattery.setText(battery);
            }
        });
        
        // 3. Read Sleep
        log("3. Reading Sleep...");
        VPOperateManager.getInstance().readSleepData(new IBleWriteResponse() {
            @Override
            public void onResponse(int code) {
                log("Sleep code: " + code);
            }
        }, new ISleepDataListener() {
            @Override
            public void onSleepDataChange(String day, SleepData sleepData) {
                log("SLEEP: " + sleepData.toString());
            }
            @Override
            public void onSleepProgress(float progress) {}
            @Override
            public void onSleepProgressDetail(String day, int packagenumber) {}
            @Override
            public void onReadSleepComplete() {}
        }, 7);
        
        // 4. Read Origin Data (heart rate, etc)
        log("4. Reading Health Data...");
        VPOperateManager.getInstance().readOriginData(new IBleWriteResponse() {
            @Override
            public void onResponse(int code) {
                log("Origin code: " + code);
            }
        }, new IOriginDataListener() {
            @Override
            public void onOringinFiveMinuteDataChange(OriginData originData) {
                log("ORIGIN: " + originData.toString());
                if (originData.getRateValue() > 0) {
                    tvHeartRate.setText(String.valueOf(originData.getRateValue()));
                }
            }
            @Override
            public void onOringinHalfHourDataChange(OriginHalfHourData data) {}
            @Override
            public void onReadOriginProgressDetail(int day, String date, int allPkg, int currPkg) {}
            @Override
            public void onReadOriginProgress(float progress) {}
            @Override
            public void onReadOriginComplete() {
                log("=== All Data Read Complete ===");
            }
        }, 3);
    }
    
    private void startHeartLive() {
        if (!isConnected) {
            Toast.makeText(this, "Not connected!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        log("=== Starting Heart Rate Live ===");
        
        VPOperateManager.getInstance().startDetectHeart(new IBleWriteResponse() {
            @Override
            public void onResponse(int code) {
                runOnUiThread(() -> log("Heart start code: " + code));
            }
        }, new IHeartDataListener() {
            @Override
            public void onDataChange(HeartData heartData) {
                runOnUiThread(() -> {
                    String dataStr = heartData.toString();
                    log("Heart: " + dataStr);
                    // Extract data value - format: HeartData{heartStatus=...,data=79}
                    int hrValue = 0;
                    try {
                        if (dataStr.contains("data=")) {
                            String part = dataStr.split("data=")[1];
                            String num = part.split("[,\\}]")[0];
                            hrValue = Integer.parseInt(num.trim());
                        }
                    } catch (Exception e) {}
                    if (hrValue > 0) {
                        tvHeartRate.setText(String.valueOf(hrValue));
                    }
                });
            }
        });
    }
    
    private void disconnectDevice() {
        log("Disconnecting...");
        VPOperateManager.getInstance().disconnectWatch(code -> {
            runOnUiThread(() -> {
                log("Disconnected!");
                isConnected = false;
                btnScan.setEnabled(true);
                btnConnect.setText("Connect");
                btnConnect.setEnabled(true);
                btnReadData.setEnabled(false);
                btnHeartLive.setEnabled(false);
                btnDisconnect.setVisibility(View.GONE);
            });
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isConnected) {
            VPOperateManager.getInstance().disconnectWatch(code -> {
            });
        }
    }
}
