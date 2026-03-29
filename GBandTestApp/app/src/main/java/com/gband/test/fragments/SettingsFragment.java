package com.gband.test.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.gband.test.AppData;
import com.gband.test.R;
import com.google.android.material.textfield.TextInputEditText;
import com.veepoo.protocol.VPOperateManager;
import com.veepoo.protocol.model.datas.PersonInfoData;

public class SettingsFragment extends Fragment {

    private static final String PREFS = "user_profile";

    private TextInputEditText etName, etAge, etHeight, etWeight, etStepGoal, etSleepGoal;
    private RadioGroup rgSex;
    private RadioButton rbMale, rbFemale;
    private Button btnSave;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etName      = view.findViewById(R.id.et_name);
        etAge       = view.findViewById(R.id.et_age);
        etHeight    = view.findViewById(R.id.et_height);
        etWeight    = view.findViewById(R.id.et_weight);
        etStepGoal  = view.findViewById(R.id.et_step_goal);
        etSleepGoal = view.findViewById(R.id.et_sleep_goal);
        rgSex       = view.findViewById(R.id.rg_sex);
        rbMale      = view.findViewById(R.id.rb_male);
        rbFemale    = view.findViewById(R.id.rb_female);
        btnSave     = view.findViewById(R.id.btn_save);

        loadSaved();

        btnSave.setOnClickListener(v -> saveSettings());
    }

    private void loadSaved() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        etName.setText(prefs.getString("name", ""));
        int age = prefs.getInt("age", 0);
        if (age > 0) etAge.setText(String.valueOf(age));
        int height = prefs.getInt("height", 0);
        if (height > 0) etHeight.setText(String.valueOf(height));
        int weight = prefs.getInt("weight", 0);
        if (weight > 0) etWeight.setText(String.valueOf(weight));
        int stepGoal = prefs.getInt("step_goal", 8000);
        etStepGoal.setText(String.valueOf(stepGoal));
        int sleepGoal = prefs.getInt("sleep_goal", 8);
        etSleepGoal.setText(String.valueOf(sleepGoal));
        boolean isMale = prefs.getBoolean("is_male", true);
        if (isMale) rbMale.setChecked(true);
        else rbFemale.setChecked(true);
    }

    private void saveSettings() {
        String name    = etName.getText() != null ? etName.getText().toString().trim() : "";
        String ageStr  = etAge.getText() != null ? etAge.getText().toString().trim() : "";
        String htStr   = etHeight.getText() != null ? etHeight.getText().toString().trim() : "";
        String wtStr   = etWeight.getText() != null ? etWeight.getText().toString().trim() : "";
        String sgStr   = etStepGoal.getText() != null ? etStepGoal.getText().toString().trim() : "";
        String sleepStr = etSleepGoal.getText() != null ? etSleepGoal.getText().toString().trim() : "";
        boolean isMale = rbMale.isChecked();

        int age    = ageStr.isEmpty()   ? 0 : Integer.parseInt(ageStr);
        int height = htStr.isEmpty()    ? 0 : Integer.parseInt(htStr);
        int weight = wtStr.isEmpty()    ? 0 : Integer.parseInt(wtStr);
        int stepGoal  = sgStr.isEmpty() ? 8000 : Integer.parseInt(sgStr);
        int sleepGoal = sleepStr.isEmpty() ? 8 : Integer.parseInt(sleepStr);

        // Save to SharedPreferences
        requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString("name", name)
            .putInt("age", age)
            .putInt("height", height)
            .putInt("weight", weight)
            .putInt("step_goal", stepGoal)
            .putInt("sleep_goal", sleepGoal)
            .putBoolean("is_male", isMale)
            .apply();

        // Sync to device if connected (improves calorie/step accuracy)
        if (AppData.getInstance().isConnected && height > 0 && weight > 0 && age > 0) {
            syncToDevice(age, height, weight, isMale, stepGoal);
        }

        Toast.makeText(getContext(), getString(R.string.settings_saved), Toast.LENGTH_SHORT).show();
    }

    private void syncToDevice(int age, int height, int weight, boolean isMale, int stepGoal) {
        PersonInfoData info = new PersonInfoData(
            isMale ? 1 : 0,   // sex: 1=male, 0=female
            age,
            height,
            weight,
            stepGoal,
            480  // sleep target minutes (8h default)
        );
        VPOperateManager.getInstance().syncPersonInfo(
            code -> {},
            null,
            info
        );
    }
}
