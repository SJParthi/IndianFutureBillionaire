package com.indianfuturebillionaire.kitebot.config;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Set;

/****************************************************************
 * Example set of holidays.
 ****************************************************************/
@Component
public class MarketHolidaysConfig {

    private final Set<LocalDate> holidays2025 = Set.of(
            LocalDate.of(2025,1,1),
            LocalDate.of(2025,1,26),
            // etc...
            LocalDate.of(2025,12,25)
    );

    public boolean isHoliday(LocalDate date) {
        return holidays2025.contains(date);
    }
}
