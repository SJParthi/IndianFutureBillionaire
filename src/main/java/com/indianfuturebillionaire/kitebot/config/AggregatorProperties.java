package com.indianfuturebillionaire.kitebot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * HPC meltdown synergy config => aggregator.* from application.yml.
 * Covers ring buffer size, partial bar thresholds, sub-cycle expansions, etc.
 */
@Component
@ConfigurationProperties(prefix="aggregator")
public class AggregatorProperties {

    // HPC meltdown synergy => ring buffer size, e.g. 65536
    private int ringBufferSize;

    // Default timeframe => e.g. "1m"
    private String defaultTimeframe;

    // If partial bars are enabled => aggregator can finalize bars mid-cycle for big price moves
    private boolean enablePartialBars;

    // E.g. 0.02 => if price moves 2% from bar open => meltdown aggregator finalizes partial bar
    private double partialBarThresholdPercent;

    // If we see no ticks for quietZoneThresholdMinutes => aggregator finalizes bar
    private int quietZoneThresholdMinutes;

    // HPC meltdown synergy => multiple timeframes => e.g. ["1m","5m","15m"]
    private List<String> multiTimeframes;

    // If subCycle => aggregator can forcibly finalize bar mid-cycle (like a half-split)
    private boolean subCycle;

    // HPC meltdown synergy => if volume is below some threshold => aggregator might merge bars
    private int barMergeThresholdVolume;

    // HPC meltdown synergy => can lock bar extremes if barLockExtreme is true
    private boolean barLockExtreme;

    // HPC meltdown synergy => aggregator can "soft close" bar after barSoftCloseSeconds,
    // then wait for final ticks
    private int barSoftCloseSeconds;

    // HPC meltdown synergy => aggregator can finalize bar after barFinalizeGraceSeconds even if meltdown
    private int barFinalizeGraceSeconds;

    // Getters & Setters
    public int getRingBufferSize() { return ringBufferSize; }
    public void setRingBufferSize(int ringBufferSize) { this.ringBufferSize = ringBufferSize; }

    public String getDefaultTimeframe() { return defaultTimeframe; }
    public void setDefaultTimeframe(String dt) { this.defaultTimeframe = dt; }

    public boolean isEnablePartialBars() { return enablePartialBars; }
    public void setEnablePartialBars(boolean e) { this.enablePartialBars = e; }

    public double getPartialBarThresholdPercent() { return partialBarThresholdPercent; }
    public void setPartialBarThresholdPercent(double p) { this.partialBarThresholdPercent = p; }

    public int getQuietZoneThresholdMinutes() { return quietZoneThresholdMinutes; }
    public void setQuietZoneThresholdMinutes(int q) { this.quietZoneThresholdMinutes = q; }

    public List<String> getMultiTimeframes() { return multiTimeframes; }
    public void setMultiTimeframes(List<String> multiTimeframes) { this.multiTimeframes = multiTimeframes; }

    public boolean isSubCycle() { return subCycle; }
    public void setSubCycle(boolean subCycle) { this.subCycle = subCycle; }

    public int getBarMergeThresholdVolume() { return barMergeThresholdVolume; }
    public void setBarMergeThresholdVolume(int b) { this.barMergeThresholdVolume = b; }

    public boolean isBarLockExtreme() { return barLockExtreme; }
    public void setBarLockExtreme(boolean barLockExtreme) { this.barLockExtreme = barLockExtreme; }

    public int getBarSoftCloseSeconds() { return barSoftCloseSeconds; }
    public void setBarSoftCloseSeconds(int b) { this.barSoftCloseSeconds = b; }

    public int getBarFinalizeGraceSeconds() { return barFinalizeGraceSeconds; }
    public void setBarFinalizeGraceSeconds(int b) { this.barFinalizeGraceSeconds = b; }
}
