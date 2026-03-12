package uk.co.philharper.tadodashboard.model;

import java.time.LocalDateTime;

public record CallForHeatInterval(LocalDateTime from, LocalDateTime to, String value) {
}
