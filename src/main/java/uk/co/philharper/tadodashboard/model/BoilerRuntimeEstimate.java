package uk.co.philharper.tadodashboard.model;

public record BoilerRuntimeEstimate(int totalMinutes, int activeIntervals, int observedIntervals, int supportingIntervals, String confidenceLabel) {
}
