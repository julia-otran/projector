package us.guihouse.projector.enums;

import lombok.Getter;

public enum Weekday {
    ALL(7, "Todos"), MONDAY(1, "Segunda"), TUESDAY(2, "Terça"), WEDNESDAY(3, "Quarta"), THURSDAY(4, "Quinta"), FRIDAY(5, "Sexta"), SATURDAY(6, "Sábado"), SUNDAY(0, "Domingo");

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
}
