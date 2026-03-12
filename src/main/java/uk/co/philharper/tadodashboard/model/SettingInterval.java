package uk.co.philharper.tadodashboard.model;

import java.time.LocalDateTime;

public record SettingInterval(LocalDateTime from, LocalDateTime to, IntervalSetting value) {
}
