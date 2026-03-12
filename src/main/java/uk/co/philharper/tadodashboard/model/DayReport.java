package uk.co.philharper.tadodashboard.model;

public record DayReport(
        MeasuredData measuredData,
        CallForHeat callForHeat,
        Settings settings,
        Weather weather
) {
}
