package com.gband.test;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.gband.test.fragments.DeviceFragment;
import com.gband.test.fragments.HomeFragment;
import com.gband.test.fragments.SettingsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.veepoo.protocol.VPOperateManager;
import com.veepoo.protocol.listener.base.IABluetoothStateListener;

public class MainActivity extends AppCompatActivity {

    private final HomeFragment     homeFragment     = new HomeFragment();
    private final DeviceFragment   deviceFragment   = new DeviceFragment();
    private final SettingsFragment settingsFragment = new SettingsFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Init SDK once here
        VPOperateManager.getInstance().init(this);
        VPOperateManager.getInstance().registerBluetoothStateListener(
            new IABluetoothStateListener() {
                @Override
                public void onBluetoothStateChanged(boolean isOn) {
                    if (!isOn) {
                        AppData.getInstance().isConnected = false;
                    }
                }
            }
        );

        // Show Home by default
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, homeFragment)
                .commit();
        }

        BottomNavigationView nav = findViewById(R.id.bottom_nav);
        nav.setOnItemSelectedListener(item -> {
            Fragment target;
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                target = homeFragment;
            } else if (id == R.id.nav_device) {
                target = deviceFragment;
            } else {
                target = settingsFragment;
            }
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, target)
                .commit();
            return true;
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (AppData.getInstance().isConnected) {
            VPOperateManager.getInstance().disconnectWatch(code -> {});
        }
    }
}
