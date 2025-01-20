package com.indianfuturebillionaire.kitebot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix="aggregator")
public class AggregatorProperties {
    private String defaultTimeframe;
    private boolean enablePartialBars;
    private double partialBarThresholdPercent;
    private int quietZoneThresholdMinutes;
    private List<String> multiTimeframes;
    private boolean subCycle;
    private int barMergeThresholdVolume;
    private boolean barLockExtreme;
    private int barSoftCloseSeconds;
    private int barFinalizeGraceSeconds;

    public String getDefaultTimeframe() { return defaultTimeframe; }
    public void setDefaultTimeframe(String dt) { this.defaultTimeframe = dt; }

    public boolean isEnablePartialBars() { return enablePartialBars; }
    public void setEnablePartialBars(boolean e) { this.enablePartialBars = e; }

    public double getPartialBarThresholdPercent() { return partialBarThresholdPercent; }
    public void setPartialBarThresholdPercent(double p) { this.partialBarThresholdPercent = p; }

    public int getQuietZoneThresholdMinutes() { return quietZoneThresholdMinutes; }
    public void setQuietZoneThresholdMinutes(int q) { this.quietZoneThresholdMinutes = q; }

    public List<String> getMultiTimeframes() { return multiTimeframes; }
    public void setMultiTimeframes(List<String> m) { this.multiTimeframes = m; }

    public boolean isSubCycle() { return subCycle; }
    public void setSubCycle(boolean s) { this.subCycle = s; }

    public int getBarMergeThresholdVolume() { return barMergeThresholdVolume; }
    public void setBarMergeThresholdVolume(int v) { this.barMergeThresholdVolume = v; }

    public boolean isBarLockExtreme() { return barLockExtreme; }
    public void setBarLockExtreme(boolean b) { this.barLockExtreme = b; }

    public int getBarSoftCloseSeconds() { return barSoftCloseSeconds; }
    public void setBarSoftCloseSeconds(int b) { this.barSoftCloseSeconds = b; }

    public int getBarFinalizeGraceSeconds() { return barFinalizeGraceSeconds; }
    public void setBarFinalizeGraceSeconds(int b) { this.barFinalizeGraceSeconds = b; }
}
