package uk.co.philharper.tadodashboard.model;

import java.util.Map;

public record RoomSchedule(String name, Day day, Map<Integer, HourlyTemperature> hourlyTemperatures) {
}
