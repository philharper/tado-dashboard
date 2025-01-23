package uk.co.philharper.tadodashboard.model;

import java.util.List;

public record Room(TimetableType timetableType, List<Block> blocks) {
}
