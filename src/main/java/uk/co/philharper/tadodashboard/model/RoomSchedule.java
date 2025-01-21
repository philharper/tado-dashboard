package uk.co.philharper.tadodashboard.model;

import java.util.Map;

public record RoomSchedule(String name, Map<Integer, HourlyTemperature> hourlyTemperatures) {
}
