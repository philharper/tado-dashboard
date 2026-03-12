package uk.co.philharper.tadodashboard.model;

import java.time.LocalDateTime;

public record BooleanInterval(LocalDateTime from, LocalDateTime to, Boolean value) {
}
