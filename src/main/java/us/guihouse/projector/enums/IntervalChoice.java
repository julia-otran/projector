package us.guihouse.projector.enums;

import us.guihouse.projector.forms.controllers.StatisticsController;

import java.util.Calendar;

public enum IntervalChoice {
    ONE_WEEK("7 Dias", 7), TWO_WEEKS("15 Dias", 15), ONE_MONTH("30 Dias", 30), THREE_MONTHS("3 Meses", 90), SIX_MONTHS("6 Meses", 180);

    final int daysInterval;
    final String intervalName;

    IntervalChoice(String intervalName, int daysInterval) {
        this.daysInterval = daysInterval;
        this.intervalName = intervalName;
    }

    public Calendar getIntervalBegin() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -1 * daysInterval);
        return calendar;
    }

    public Calendar getIntervalEnd() {
        return Calendar.getInstance();
    }

    public String toString() {
        return intervalName;
    }

    public boolean isSelected() {
        return this == IntervalChoice.ONE_MONTH;
    }
}
