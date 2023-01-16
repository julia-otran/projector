package dev.juhouse.projector.enums;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

public enum IntervalChoice {
    ONE_WEEK("7 Dias", 7), TWO_WEEKS("15 Dias", 15), ONE_MONTH("30 Dias", 30), THREE_MONTHS("3 Meses", 90), SIX_MONTHS("6 Meses", 180);

    final int daysInterval;
    final String intervalName;

    IntervalChoice(String intervalName, int daysInterval) {
        this.daysInterval = daysInterval;
        this.intervalName = intervalName;
    }

    public LocalDate getIntervalBegin() {
        LocalDate now = LocalDate.now(ZoneOffset.UTC.normalized());
        now = now.minus(daysInterval, ChronoUnit.DAYS);
        return now;
    }

    public String toString() {
        return intervalName;
    }

    public boolean isSelected() {
        return this == IntervalChoice.ONE_MONTH;
    }
}
