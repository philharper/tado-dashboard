package uk.co.philharper.tadodashboard.model;

import java.util.List;

public record BoilerRuntimeEstimate(
        int totalMinutes,
        int activeIntervals,
        int observedIntervals,
        int supportingIntervals,
        String confidenceLabel,
        List<BoilerRuntimeWindow> windows
) {
}
