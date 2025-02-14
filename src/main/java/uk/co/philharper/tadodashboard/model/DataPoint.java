package uk.co.philharper.tadodashboard.model;

import java.time.LocalDateTime;

public record DataPoint(LocalDateTime timestamp, Value value) {
}
