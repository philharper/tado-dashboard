package uk.co.philharper.tadodashboard.model;

import java.util.LinkedHashMap;

public record WeeklySchedule(LinkedHashMap<String, HeatingSchedule> heatingSchedule) {
}
