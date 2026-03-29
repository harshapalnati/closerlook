package com.gband.test.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.gband.test.AppData;
import com.gband.test.R;
import com.veepoo.protocol.VPOperateManager;
import com.veepoo.protocol.listener.data.IBatteryDataListener;
import com.veepoo.protocol.listener.data.IBPDataListener;
import com.veepoo.protocol.listener.data.IHeartDataListener;
import com.veepoo.protocol.listener.data.IMiniCheckupDataListener;
import com.veepoo.protocol.listener.data.IOriginDataListener;
import com.veepoo.protocol.listener.data.IOriginData3Listener;
import com.veepoo.protocol.listener.data.ISleepDataListener;
import com.veepoo.protocol.listener.data.ISpo2HDataListener;
import com.veepoo.protocol.listener.data.ISportDataListener;
import com.veepoo.protocol.listener.data.ITemptureDataListener;
import com.veepoo.protocol.model.datas.BatteryData;
import com.veepoo.protocol.model.datas.BpData;
import com.veepoo.protocol.model.datas.HeartData;
import com.veepoo.protocol.model.datas.MiniCheckupResultData;
import com.veepoo.protocol.model.datas.OriginData;
import com.veepoo.protocol.model.datas.OriginData3;
import com.veepoo.protocol.model.datas.OriginHalfHourData;
import com.veepoo.protocol.model.datas.SleepData;
import com.veepoo.protocol.model.datas.SportData;
import com.veepoo.protocol.model.datas.Spo2HData;
import com.veepoo.protocol.model.datas.TemptureDetectData;

import java.util.List;

public class HomeFragment extends Fragment {

    // ── UI references ─────────────────────────────────────────────────────
    private TextView tvHeartRate, tvHeartStatus;
    private TextView tvSteps, tvDistance, tvCalories;
    private TextView tvSleepQuality, tvSleepTotal, tvSleepDeep, tvSleepLight, tvWakeCount;
    private TextView tvSpo2, tvSpo2Status, tvBp, tvBpStatus;
    private TextView tvTemperature, tvTempStatus;
    private TextView tvHrv, tvRespRate;
    private TextView tvStress, tvFatigue, tvWellnessStatus;
    private TextView tvBatteryPct;
    private LinearLayout notConnectedBanner;
    private Button btnRefresh, btnMeasureHeart, btnMeasureSpo2, btnMeasureBp;
    private Button btnMeasureTemp, btnWellnessCheck;

    // ── Measurement state (only one active at a time — SDK is serial) ─────
    private boolean isMeasuringHeart    = false;
    private boolean isMeasuringSpo2     = false;
    private boolean isMeasuringBp       = false;
    private boolean isMeasuringTemp     = false;
    private boolean isWellnessRunning   = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        wireButtons();
    }

    private void bindViews(View v) {
        tvHeartRate       = v.findViewById(R.id.tv_heart_rate);
        tvHeartStatus     = v.findViewById(R.id.tv_heart_status);
        tvSteps           = v.findViewById(R.id.tv_steps);
        tvDistance        = v.findViewById(R.id.tv_distance);
        tvCalories        = v.findViewById(R.id.tv_calories);
        tvSleepQuality    = v.findViewById(R.id.tv_sleep_quality);
        tvSleepTotal      = v.findViewById(R.id.tv_sleep_total);
        tvSleepDeep       = v.findViewById(R.id.tv_sleep_deep);
        tvSleepLight      = v.findViewById(R.id.tv_sleep_light);
        tvWakeCount       = v.findViewById(R.id.tv_wake_count);
        tvSpo2            = v.findViewById(R.id.tv_spo2);
        tvSpo2Status      = v.findViewById(R.id.tv_spo2_status);
        tvBp              = v.findViewById(R.id.tv_bp);
        tvBpStatus        = v.findViewById(R.id.tv_bp_status);
        tvTemperature     = v.findViewById(R.id.tv_temperature);
        tvTempStatus      = v.findViewById(R.id.tv_temp_status);
        tvHrv             = v.findViewById(R.id.tv_hrv);
        tvRespRate        = v.findViewById(R.id.tv_resp_rate);
        tvStress          = v.findViewById(R.id.tv_stress);
        tvFatigue         = v.findViewById(R.id.tv_fatigue);
        tvWellnessStatus  = v.findViewById(R.id.tv_wellness_status);
        tvBatteryPct      = v.findViewById(R.id.tv_battery_pct);
        notConnectedBanner = v.findViewById(R.id.not_connected_banner);
        btnRefresh        = v.findViewById(R.id.btn_refresh);
        btnMeasureHeart   = v.findViewById(R.id.btn_measure_heart);
        btnMeasureSpo2    = v.findViewById(R.id.btn_measure_spo2);
        btnMeasureBp      = v.findViewById(R.id.btn_measure_bp);
        btnMeasureTemp    = v.findViewById(R.id.btn_measure_temp);
        btnWellnessCheck  = v.findViewById(R.id.btn_wellness_check);
    }

    private void wireButtons() {
        btnRefresh.setOnClickListener(v -> startRefreshChain());
        btnMeasureHeart.setOnClickListener(v -> toggleHeartMeasurement());
        btnMeasureSpo2.setOnClickListener(v -> toggleSpo2Measurement());
        btnMeasureBp.setOnClickListener(v -> toggleBpMeasurement());
        btnMeasureTemp.setOnClickListener(v -> toggleTemperatureMeasurement());
        btnWellnessCheck.setOnClickListener(v -> toggleWellnessCheck());
    }

    @Override
    public void onResume() {
        super.onResume();
        renderFromCache();
    }

    // ── Render AppData to all UI fields ───────────────────────────────────
    private void renderFromCache() {
        AppData d = AppData.getInstance();
        notConnectedBanner.setVisibility(d.isConnected ? View.GONE : View.VISIBLE);

        // Battery
        tvBatteryPct.setText(d.batteryPercent >= 0 ? String.valueOf(d.batteryPercent) : "--");

        // Heart
        tvHeartRate.setText(d.heartRate > 0 ? String.valueOf(d.heartRate) : "--");

        // Activity
        tvSteps.setText(d.steps >= 0 ? String.valueOf(d.steps) : "--");
        tvDistance.setText(d.distanceKm >= 0 ? String.format("%.1f", d.distanceKm) : "--");
        tvCalories.setText(d.caloriesKcal >= 0 ? String.valueOf((int) d.caloriesKcal) : "--");

        // Sleep
        if (d.sleepQuality >= 0) {
            tvSleepQuality.setText(String.valueOf(d.sleepQuality));
            applySleepQualityColor(d.sleepQuality);
        } else {
            tvSleepQuality.setText("--");
        }
        tvSleepTotal.setText(d.sleepTotalMinutes >= 0
            ? (d.sleepTotalMinutes / 60) + "h " + (d.sleepTotalMinutes % 60) + "m"
            : "-- h --m");
        tvSleepDeep.setText(d.sleepDeepMinutes >= 0 ? String.valueOf(d.sleepDeepMinutes) : "--");
        tvSleepLight.setText(d.sleepLightMinutes >= 0 ? String.valueOf(d.sleepLightMinutes) : "--");
        tvWakeCount.setText(d.sleepWakeCount >= 0 ? String.valueOf(d.sleepWakeCount) : "--");

        // Vitals
        tvSpo2.setText(d.spo2Percent > 0 ? String.valueOf(d.spo2Percent) : "--");
        tvBp.setText(d.bpSystolic > 0 && d.bpDiastolic > 0
            ? d.bpSystolic + "/" + d.bpDiastolic : "--/--");
        tvTemperature.setText(d.temperature > 0 ? String.format("%.1f", d.temperature) : "--");

        // Wellness
        tvHrv.setText(d.hrv > 0 ? String.valueOf(d.hrv) : "--");
        tvRespRate.setText(d.respirationRate > 0 ? String.valueOf(d.respirationRate) : "--");
        tvStress.setText(d.stressLevel >= 0 ? String.valueOf(d.stressLevel) : "--");
        tvFatigue.setText(d.fatigue >= 0 ? String.valueOf(d.fatigue) : "--");
    }

    private void applySleepQualityColor(int score) {
        int color;
        if (score >= 80)      color = getColor(R.color.accent_green);
        else if (score >= 60) color = getColor(R.color.accent_yellow);
        else                  color = getColor(R.color.accent_red);
        tvSleepQuality.setTextColor(color);
    }

    private int getColor(int resId) {
        return requireContext().getResources().getColor(resId, null);
    }

    private boolean checkConnected() {
        if (!AppData.getInstance().isConnected) {
            Toast.makeText(getContext(), "Connect your G Band first", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private boolean noActiveMeasurement() {
        if (isMeasuringHeart || isMeasuringSpo2 || isMeasuringBp
                || isMeasuringTemp || isWellnessRunning) {
            Toast.makeText(getContext(), "Stop the current measurement first",
                Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    // ── Serial refresh chain: steps → battery → sleep → origin3 ─────────
    private void startRefreshChain() {
        if (!checkConnected() || !noActiveMeasurement()) return;
        btnRefresh.setEnabled(false);
        btnRefresh.setText("Loading\u2026");
        readSteps();
    }

    private void readSteps() {
        VPOperateManager.getInstance().readSportStep(code -> {},
            new ISportDataListener() {
                @Override public void onSportDataChange(SportData data) {
                    AppData d = AppData.getInstance();
                    d.steps = data.getStep();
                    d.distanceKm = data.getDis();
                    d.caloriesKcal = data.getKcal();
                    requireActivity().runOnUiThread(() -> {
                        tvSteps.setText(String.valueOf(data.getStep()));
                        tvDistance.setText(String.format("%.1f", data.getDis()));
                        tvCalories.setText(String.valueOf((int) data.getKcal()));
                        readBattery();
                    });
                }
            });
    }

    private void readBattery() {
        VPOperateManager.getInstance().readBattery(code -> {},
            new IBatteryDataListener() {
                @Override public void onDataChange(BatteryData data) {
                    int pct = data.isPercent() ? data.getBatteryPercent() : data.getBatteryLevel() * 25;
                    AppData.getInstance().batteryPercent = pct;
                    requireActivity().runOnUiThread(() -> {
                        tvBatteryPct.setText(String.valueOf(pct));
                        readSleep();
                    });
                }
            });
    }

    private void readSleep() {
        VPOperateManager.getInstance().readSleepData(code -> {},
            new ISleepDataListener() {
                @Override public void onSleepDataChange(String day, SleepData data) {
                    AppData d = AppData.getInstance();
                    d.sleepTotalMinutes  = data.getAllSleepTime();
                    d.sleepDeepMinutes   = data.getDeepSleepTime();
                    d.sleepLightMinutes  = data.getLowSleepTime();
                    d.sleepWakeCount     = data.getWakeCount();
                    d.sleepQuality       = data.getSleepQulity();   // 0–100 quality score
                    requireActivity().runOnUiThread(() -> {
                        int total = data.getAllSleepTime();
                        tvSleepTotal.setText((total / 60) + "h " + (total % 60) + "m");
                        tvSleepDeep.setText(String.valueOf(data.getDeepSleepTime()));
                        tvSleepLight.setText(String.valueOf(data.getLowSleepTime()));
                        tvWakeCount.setText(String.valueOf(data.getWakeCount()));
                        int q = data.getSleepQulity();
                        tvSleepQuality.setText(String.valueOf(q));
                        applySleepQualityColor(q);
                    });
                }
                @Override public void onSleepProgress(float p) {}
                @Override public void onSleepProgressDetail(String day, int pkg) {}
                @Override public void onReadSleepComplete() {
                    requireActivity().runOnUiThread(() -> readOriginData3());
                }
            }, 1);
    }

    /**
     * IOriginData3Listener gives us HRV, respiratory rate, SpO2 from history,
     * HR from history, and BP from history — richer than IOriginDataListener.
     */
    private void readOriginData3() {
        VPOperateManager.getInstance().readOriginData(code -> {},
            new IOriginData3Listener() {
                @Override
                public void onOriginData3Change(String date, List<OriginData3> dataList) {
                    if (dataList == null || dataList.isEmpty()) return;
                    int lastHr = -1, lastSystolic = -1, lastDiastolic = -1;
                    int lastResp = -1, lastOxygen = -1;

                    for (OriginData3 d3 : dataList) {
                        // HR history
                        if (d3.getRateValue() > 0) lastHr = d3.getRateValue();
                        // BP history
                        if (d3.getHighValue() > 0) lastSystolic  = d3.getHighValue();
                        if (d3.getLowValue()  > 0) lastDiastolic = d3.getLowValue();
                        // Respiratory rate (take last non-zero from array)
                        int[] rates = d3.getResRates();
                        if (rates != null) {
                            for (int r : rates) { if (r > 0) lastResp = r; }
                        }
                        // SpO2 history
                        int[] oxygens = d3.getOxygens();
                        if (oxygens != null) {
                            for (int o : oxygens) { if (o > 0) lastOxygen = o; }
                        }
                    }

                    final int fHr = lastHr, fSys = lastSystolic, fDia = lastDiastolic;
                    final int fResp = lastResp, fOxy = lastOxygen;
                    AppData app = AppData.getInstance();
                    if (fHr    > 0) app.heartRate       = fHr;
                    if (fSys   > 0) app.bpSystolic      = fSys;
                    if (fDia   > 0) app.bpDiastolic     = fDia;
                    if (fResp  > 0) app.respirationRate = fResp;
                    if (fOxy   > 0) app.spo2Percent     = fOxy;

                    requireActivity().runOnUiThread(() -> {
                        if (fHr > 0)  tvHeartRate.setText(String.valueOf(fHr));
                        if (fSys > 0 && fDia > 0) tvBp.setText(fSys + "/" + fDia);
                        if (fResp > 0) tvRespRate.setText(String.valueOf(fResp));
                        if (fOxy > 0)  tvSpo2.setText(String.valueOf(fOxy));
                    });
                }

                @Override
                public void onOriginHRVOriginListDataChange(List<com.veepoo.protocol.model.datas.HRVOriginData> list) {
                    if (list == null || list.isEmpty()) return;
                    // Take the average HRV from the last reading
                    com.veepoo.protocol.model.datas.HRVOriginData last = list.get(list.size() - 1);
                    int hrvVal = last.getHrv();
                    if (hrvVal > 0 && hrvVal < 255) {   // 255 = invalid sentinel
                        AppData.getInstance().hrv = hrvVal;
                        requireActivity().runOnUiThread(() -> tvHrv.setText(String.valueOf(hrvVal)));
                    }
                }

                @Override public void onOringinHalfHourDataChange(OriginHalfHourData d) {}
                @Override public void onReadOriginProgressDetail(int day, String date, int all, int curr) {}
                @Override public void onReadOriginProgress(float p) {}
                @Override public void onReadOriginComplete() {
                    requireActivity().runOnUiThread(() -> {
                        btnRefresh.setEnabled(true);
                        btnRefresh.setText(getString(R.string.btn_refresh));
                    });
                }
            }, 1);
    }

    // ── Heart Rate live measurement ───────────────────────────────────────
    private void toggleHeartMeasurement() {
        if (!checkConnected()) return;
        if (!isMeasuringHeart && !noActiveMeasurement()) return;

        if (!isMeasuringHeart) {
            isMeasuringHeart = true;
            btnMeasureHeart.setText("Stop");
            showStatus(tvHeartStatus, "\u25CF Measuring\u2026 keep wrist still",
                R.color.accent_red);

            VPOperateManager.getInstance().startDetectHeart(code -> {},
                new IHeartDataListener() {
                    @Override public void onDataChange(HeartData data) {
                        int bpm = data.getRateValue();
                        if (bpm > 0) {
                            AppData.getInstance().heartRate = bpm;
                            requireActivity().runOnUiThread(() ->
                                tvHeartRate.setText(String.valueOf(bpm)));
                        }
                    }
                });
        } else {
            stopHeartMeasurement();
        }
    }

    private void stopHeartMeasurement() {
        isMeasuringHeart = false;
        btnMeasureHeart.setText("Measure");
        tvHeartStatus.setVisibility(View.GONE);
        VPOperateManager.getInstance().stopDetectHeart(code -> {});
    }

    // ── SpO2 live measurement ─────────────────────────────────────────────
    private void toggleSpo2Measurement() {
        if (!checkConnected()) return;
        if (!isMeasuringSpo2 && !noActiveMeasurement()) return;

        if (!isMeasuringSpo2) {
            isMeasuringSpo2 = true;
            btnMeasureSpo2.setText("Stop");
            showStatus(tvSpo2Status, "\u25CF Measuring\u2026 keep wrist still",
                R.color.accent_green);

            VPOperateManager.getInstance().startDetectSPO2H(code -> {},
                new ISpo2HDataListener() {
                    @Override public void onDataChange(Spo2HData data) {
                        int o2 = data.getOxygen();
                        if (o2 > 0) {
                            AppData.getInstance().spo2Percent = o2;
                            requireActivity().runOnUiThread(() -> {
                                tvSpo2.setText(String.valueOf(o2));
                                if (o2 < 90) {
                                    tvSpo2.setTextColor(getColor(R.color.accent_red));
                                    showStatus(tvSpo2Status,
                                        "\u26A0 Low \u2014 please consult a doctor",
                                        R.color.accent_red);
                                } else if (o2 < 95) {
                                    tvSpo2.setTextColor(getColor(R.color.accent_yellow));
                                    showStatus(tvSpo2Status, "\u25CF Slightly low",
                                        R.color.accent_yellow);
                                } else {
                                    tvSpo2.setTextColor(getColor(R.color.text_primary));
                                    showStatus(tvSpo2Status, "\u2713 Normal range",
                                        R.color.accent_green);
                                }
                            });
                        }
                    }
                });
        } else {
            stopSpo2Measurement();
        }
    }

    private void stopSpo2Measurement() {
        isMeasuringSpo2 = false;
        btnMeasureSpo2.setText("Measure");
        tvSpo2Status.setVisibility(View.GONE);
        tvSpo2.setTextColor(getColor(R.color.text_primary));
        VPOperateManager.getInstance().stopDetectSPO2H(code -> {});
    }

    // ── Blood Pressure live measurement ───────────────────────────────────
    private void toggleBpMeasurement() {
        if (!checkConnected()) return;
        if (!isMeasuringBp && !noActiveMeasurement()) return;

        if (!isMeasuringBp) {
            isMeasuringBp = true;
            btnMeasureBp.setText("Stop");
            showStatus(tvBpStatus, "\u25CF Measuring\u2026 sit still & relax",
                R.color.accent_blue);

            VPOperateManager.getInstance().startDetectBP(code -> {},
                new IBPDataListener() {
                    @Override public void onDataChange(BpData data) {
                        int hi = data.getHighValue(), lo = data.getLowValue();
                        if (hi > 0 && lo > 0) {
                            AppData.getInstance().bpSystolic  = hi;
                            AppData.getInstance().bpDiastolic = lo;
                            requireActivity().runOnUiThread(() -> {
                                tvBp.setText(hi + "/" + lo);
                                if (hi >= 140 || lo >= 90) {
                                    tvBp.setTextColor(getColor(R.color.accent_red));
                                    showStatus(tvBpStatus,
                                        "\u26A0 High \u2014 talk to your doctor",
                                        R.color.accent_red);
                                } else if (hi >= 130 || lo >= 80) {
                                    tvBp.setTextColor(getColor(R.color.accent_yellow));
                                    showStatus(tvBpStatus, "\u25CF Slightly elevated",
                                        R.color.accent_yellow);
                                } else {
                                    tvBp.setTextColor(getColor(R.color.text_primary));
                                    showStatus(tvBpStatus, "\u2713 Normal range",
                                        R.color.accent_green);
                                }
                            });
                        }
                    }
                });
        } else {
            stopBpMeasurement();
        }
    }

    private void stopBpMeasurement() {
        isMeasuringBp = false;
        btnMeasureBp.setText("Measure");
        tvBpStatus.setVisibility(View.GONE);
        tvBp.setTextColor(getColor(R.color.text_primary));
        VPOperateManager.getInstance().stopDetectBP(code -> {});
    }

    // ── Body Temperature measurement ──────────────────────────────────────
    private void toggleTemperatureMeasurement() {
        if (!checkConnected()) return;
        if (!isMeasuringTemp && !noActiveMeasurement()) return;

        if (!isMeasuringTemp) {
            isMeasuringTemp = true;
            btnMeasureTemp.setText("Stop");
            showStatus(tvTempStatus, "\u25CF Measuring\u2026 keep wrist still",
                R.color.accent_yellow);

            VPOperateManager.getInstance().startDetectTempture(code -> {},
                new ITemptureDataListener() {
                    @Override
                    public void onTemptureDetectDataChange(TemptureDetectData data) {
                        float temp = data.getTempture();
                        if (temp > 0) {
                            AppData.getInstance().temperature = temp;
                            requireActivity().runOnUiThread(() -> {
                                tvTemperature.setText(String.format("%.1f", temp));
                                if (temp >= 37.5f) {
                                    tvTemperature.setTextColor(getColor(R.color.accent_red));
                                    showStatus(tvTempStatus,
                                        "\u26A0 Elevated \u2014 possible fever",
                                        R.color.accent_red);
                                } else if (temp >= 37.0f) {
                                    tvTemperature.setTextColor(getColor(R.color.accent_yellow));
                                    showStatus(tvTempStatus, "\u25CF Slightly elevated",
                                        R.color.accent_yellow);
                                } else {
                                    tvTemperature.setTextColor(getColor(R.color.text_primary));
                                    showStatus(tvTempStatus, "\u2713 Normal",
                                        R.color.accent_green);
                                }
                            });
                        }
                    }
                });
        } else {
            stopTemperatureMeasurement();
        }
    }

    private void stopTemperatureMeasurement() {
        isMeasuringTemp = false;
        btnMeasureTemp.setText("Measure");
        tvTempStatus.setVisibility(View.GONE);
        tvTemperature.setTextColor(getColor(R.color.text_primary));
        VPOperateManager.getInstance().stopDetectTempture(code -> {});
    }

    // ── Wellness Check (Stress + Fatigue via MiniCheckup) ─────────────────
    private void toggleWellnessCheck() {
        if (!checkConnected()) return;
        if (!isWellnessRunning && !noActiveMeasurement()) return;

        if (!isWellnessRunning) {
            isWellnessRunning = true;
            btnWellnessCheck.setText("Stop");
            tvWellnessStatus.setText("\u25CF Checking wellness\u2026 sit still for ~30 seconds");
            tvWellnessStatus.setTextColor(getColor(R.color.accent_purple));

            VPOperateManager.getInstance().startMiniCheckup(code -> {},
                new IMiniCheckupDataListener() {
                    @Override
                    public void onMiniCheckupResultChange(MiniCheckupResultData result) {
                        AppData d = AppData.getInstance();
                        // Pull all available fields from MiniCheckup
                        if (result.getStressLevel() >= 0) d.stressLevel = result.getStressLevel();
                        if (result.getFatigueDegree() >= 0) d.fatigue = result.getFatigueDegree();
                        if (result.getHrv() > 0 && result.getHrv() < 255) d.hrv = result.getHrv();
                        if (result.getHeartRate() > 0) d.heartRate = result.getHeartRate();
                        if (result.getOxygen() > 0) d.spo2Percent = result.getOxygen();
                        if (result.getTemperature() > 0) d.temperature = result.getTemperature();
                        if (result.getSystolic() > 0) d.bpSystolic = result.getSystolic();
                        if (result.getDiastolic() > 0) d.bpDiastolic = result.getDiastolic();

                        requireActivity().runOnUiThread(() -> {
                            // Update all fields that MiniCheckup populates
                            if (result.getStressLevel() >= 0)
                                tvStress.setText(String.valueOf(result.getStressLevel()));
                            if (result.getFatigueDegree() >= 0)
                                tvFatigue.setText(String.valueOf(result.getFatigueDegree()));
                            if (result.getHrv() > 0 && result.getHrv() < 255)
                                tvHrv.setText(String.valueOf(result.getHrv()));
                            if (result.getHeartRate() > 0)
                                tvHeartRate.setText(String.valueOf(result.getHeartRate()));
                            if (result.getTemperature() > 0)
                                tvTemperature.setText(
                                    String.format("%.1f", result.getTemperature()));

                            // Stress interpretation for older adults
                            int s = result.getStressLevel();
                            if (s >= 0) {
                                String msg;
                                if (s >= 75)      msg = "\u26A0 High stress \u2014 try to rest";
                                else if (s >= 50) msg = "\u25CF Moderate stress";
                                else              msg = "\u2713 Stress is low \u2014 good";
                                tvWellnessStatus.setText(msg);
                                tvWellnessStatus.setTextColor(
                                    s >= 75 ? getColor(R.color.accent_red)
                                    : s >= 50 ? getColor(R.color.accent_yellow)
                                    : getColor(R.color.accent_green));
                            }
                        });
                    }
                });
        } else {
            stopWellnessCheck();
        }
    }

    private void stopWellnessCheck() {
        isWellnessRunning = false;
        btnWellnessCheck.setText("Check Now");
        VPOperateManager.getInstance().stopMiniCheckup(code -> {});
    }

    // ── Helper: show/hide status text with color ──────────────────────────
    private void showStatus(TextView tv, String msg, int colorRes) {
        tv.setText(msg);
        tv.setTextColor(getColor(colorRes));
        tv.setVisibility(View.VISIBLE);
    }

    // ── Cleanup ───────────────────────────────────────────────────────────
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (isMeasuringHeart)  VPOperateManager.getInstance().stopDetectHeart(code -> {});
        if (isMeasuringSpo2)   VPOperateManager.getInstance().stopDetectSPO2H(code -> {});
        if (isMeasuringBp)     VPOperateManager.getInstance().stopDetectBP(code -> {});
        if (isMeasuringTemp)   VPOperateManager.getInstance().stopDetectTempture(code -> {});
        if (isWellnessRunning) VPOperateManager.getInstance().stopMiniCheckup(code -> {});
        isMeasuringHeart = isMeasuringSpo2 = isMeasuringBp = isMeasuringTemp
            = isWellnessRunning = false;
    }
}
