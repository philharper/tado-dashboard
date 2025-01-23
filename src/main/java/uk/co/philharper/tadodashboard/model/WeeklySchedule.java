package uk.co.philharper.tadodashboard.model;

import java.util.LinkedHashMap;

public record WeeklySchedule(LinkedHashMap<Day, HeatingSchedule> heatingSchedule) {
}
