package uk.co.philharper.tadodashboard.model;

import java.util.List;

public record Room(String timetableType, List<Block> blocks) {
}
