package us.guihouse.projector.enums;

import lombok.Getter;

import java.sql.Date;
import java.util.Calendar;

public enum Weekday {
    ALL(-1, "Todos"),
    MONDAY(Calendar.MONDAY, "Segunda"),
    TUESDAY(Calendar.TUESDAY, "Terça"),
    WEDNESDAY(Calendar.WEDNESDAY, "Quarta"),
    THURSDAY(Calendar.THURSDAY, "Quinta"),
    FRIDAY(Calendar.FRIDAY, "Sexta"),
    SATURDAY(Calendar.SATURDAY, "Sábado"),
    SUNDAY(Calendar.SUNDAY, "Domingo");

    @Getter
    final int weekdayNumber;
    final String name;

    Weekday(int weekdayNumber, String name) {
        this.weekdayNumber = weekdayNumber;
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public boolean isWeekday(Date date) {
        if (this == ALL) {
            return true;
        }

        Calendar c = Calendar.getInstance();
        c.setTime(date);

        return c.get(Calendar.DAY_OF_WEEK) == weekdayNumber;
    }
}
