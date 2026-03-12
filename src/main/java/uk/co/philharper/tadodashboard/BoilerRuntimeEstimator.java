package uk.co.philharper.tadodashboard;

import org.springframework.stereotype.Component;
import uk.co.philharper.tadodashboard.model.BoilerRuntimeEstimate;
import uk.co.philharper.tadodashboard.model.BoilerRuntimeWindow;
import uk.co.philharper.tadodashboard.model.BooleanInterval;
import uk.co.philharper.tadodashboard.model.CallForHeatInterval;
import uk.co.philharper.tadodashboard.model.DataPoint;
import uk.co.philharper.tadodashboard.model.Day;
import uk.co.philharper.tadodashboard.model.DayReport;
import uk.co.philharper.tadodashboard.model.HourlyTemperature;
import uk.co.philharper.tadodashboard.model.SettingInterval;
import uk.co.philharper.tadodashboard.model.RoomSchedule;
import uk.co.philharper.tadodashboard.model.WeeklySchedule;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Component
public class BoilerRuntimeEstimator {

    private static final int MAX_INTERVAL_MINUTES = 45;
    private static final int MIN_INTERVAL_MINUTES = 3;
    private static final double BASE_RISE_PER_HOUR = 0.12;
    private static final double BOOST_RISE_PER_HOUR = 0.45;
    public BoilerRuntimeEstimate estimate(Map<String, DayReport> dayReports, WeeklySchedule weeklySchedule, Day day) {
        Map<String, Map<Integer, Double>> targetsByRoom = buildTargetsByRoom(weeklySchedule, day);
        List<RoomIntervalSignal> roomSignals = buildRoomSignals(dayReports, targetsByRoom);

        if (roomSignals.isEmpty()) {
            return new BoilerRuntimeEstimate(0, 0, 0, 0, "Low", List.of());
        }

        TreeSet<LocalDateTime> boundaries = new TreeSet<>();
        roomSignals.forEach(signal -> {
            boundaries.add(signal.start());
            boundaries.add(signal.end());
        });

        List<LocalDateTime> boundaryList = new ArrayList<>(boundaries);
        boolean boilerOn = false;
        int totalMinutes = 0;
        int activeIntervals = 0;
        int observedIntervals = 0;
        int supportingIntervals = 0;
        List<BoilerRuntimeWindow> windows = new ArrayList<>();
        LocalDateTime currentWindowStart = null;

        for (int index = 0; index < boundaryList.size() - 1; index++) {
            LocalDateTime start = boundaryList.get(index);
            LocalDateTime end = boundaryList.get(index + 1);
            int minutes = (int) Duration.between(start, end).toMinutes();

            if (minutes < MIN_INTERVAL_MINUTES || minutes > MAX_INTERVAL_MINUTES) {
                continue;
            }

            List<RoomIntervalSignal> activeSignals = roomSignals.stream()
                    .filter(signal -> !signal.start().isAfter(start) && signal.end().isAfter(start))
                    .toList();

            if (activeSignals.isEmpty()) {
                continue;
            }

            observedIntervals++;

            long demandingRooms = activeSignals.stream().filter(RoomIntervalSignal::hasDemand).count();
            long heatingRooms = activeSignals.stream().filter(RoomIntervalSignal::likelyHeating).count();
            long strongRooms = activeSignals.stream().filter(RoomIntervalSignal::strongHeating).count();
            long boostedRooms = activeSignals.stream().filter(RoomIntervalSignal::boostedHeating).count();
            long callingRooms = activeSignals.stream().filter(RoomIntervalSignal::callingForHeat).count();
            boolean heatRequestObserved = activeSignals.stream().anyMatch(RoomIntervalSignal::heatRequestKnown);

            double aggregateEvidence = activeSignals.stream()
                    .map(RoomIntervalSignal::evidence)
                    .sorted(Comparator.reverseOrder())
                    .limit(3)
                    .mapToDouble(Double::doubleValue)
                    .sum();

            boolean intervalSupported = (heatingRooms >= 2)
                    || (strongRooms >= 1 && demandingRooms >= 2)
                    || (boostedRooms >= 2)
                    || (boostedRooms >= 1 && heatingRooms >= 1)
                    || aggregateEvidence >= 1.45;

            if (heatRequestObserved && callingRooms == 0) {
                intervalSupported = false;
            }

            boolean intervalOn = intervalSupported
                    || (boilerOn && (demandingRooms >= 2 || boostedRooms >= 1) && aggregateEvidence >= 0.95);

            if (heatRequestObserved && callingRooms == 0) {
                intervalOn = false;
            }

            if (intervalSupported) {
                supportingIntervals++;
            }

            boilerOn = intervalOn;

            if (intervalOn) {
                if (currentWindowStart == null) {
                    currentWindowStart = start;
                }
                totalMinutes += minutes;
                activeIntervals++;
            } else if (currentWindowStart != null) {
                windows.add(new BoilerRuntimeWindow(currentWindowStart, start));
                currentWindowStart = null;
            }
        }

        if (currentWindowStart != null && boundaryList.size() > 1) {
            windows.add(new BoilerRuntimeWindow(currentWindowStart, boundaryList.get(boundaryList.size() - 1)));
        }

        return new BoilerRuntimeEstimate(
                totalMinutes,
                activeIntervals,
                observedIntervals,
                supportingIntervals,
                confidenceLabel(activeIntervals, supportingIntervals),
                windows
        );
    }

    private Map<String, Map<Integer, Double>> buildTargetsByRoom(WeeklySchedule weeklySchedule, Day day) {
        if (weeklySchedule == null || weeklySchedule.heatingSchedule() == null || !weeklySchedule.heatingSchedule().containsKey(day)) {
            return Map.of();
        }

        return weeklySchedule.heatingSchedule().get(day).roomSchedules().stream()
                .collect(Collectors.toMap(
                        RoomSchedule::name,
                        roomSchedule -> roomSchedule.hourlyTemperatures().entrySet().stream()
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        entry -> parseNumericTarget(entry.getValue())
                                ))
                ));
    }

    private List<RoomIntervalSignal> buildRoomSignals(Map<String, DayReport> dayReports, Map<String, Map<Integer, Double>> targetsByRoom) {
        List<RoomIntervalSignal> signals = new ArrayList<>();

        dayReports.forEach((roomName, dayReport) -> {
            if (dayReport == null || dayReport.measuredData() == null || dayReport.measuredData().insideTemperature() == null || dayReport.measuredData().insideTemperature().dataPoints() == null) {
                return;
            }

            List<DataPoint> points = dayReport.measuredData().insideTemperature().dataPoints();
            List<CallForHeatInterval> callForHeatIntervals = dayReport.callForHeat() == null
                    ? List.of()
                    : dayReport.callForHeat().dataIntervals();
            List<SettingInterval> settingIntervals = dayReport.settings() == null
                    ? List.of()
                    : dayReport.settings().dataIntervals();
            List<BooleanInterval> sunnyIntervals = dayReport.weather() == null || dayReport.weather().sunny() == null
                    ? List.of()
                    : dayReport.weather().sunny().dataIntervals();
            Map<Integer, Double> hourlyTargets = targetsByRoom.getOrDefault(roomName, Map.of());

            for (int index = 1; index < points.size(); index++) {
                DataPoint previous = points.get(index - 1);
                DataPoint current = points.get(index);
                int minutes = (int) Duration.between(previous.timestamp(), current.timestamp()).toMinutes();

                if (minutes < MIN_INTERVAL_MINUTES || minutes > MAX_INTERVAL_MINUTES) {
                    continue;
                }

                double currentTemp = current.value().celsius();
                double previousTemp = previous.value().celsius();
                Double target = hourlyTargets.get(current.timestamp().getHour());
                double slopePerHour = ((currentTemp - previousTemp) / minutes) * 60.0;
                double tempGap = target == null ? 0.0 : target - currentTemp;
                double demandScore = tempGap <= 0.0 ? 0.0 : clamp(tempGap / 1.8);
                double riseScore = clamp((slopePerHour - BASE_RISE_PER_HOUR) / 0.9);
                boolean boostedHeating = slopePerHour >= BOOST_RISE_PER_HOUR && (currentTemp - previousTemp) >= 0.08;
                HeatRequestState heatRequestState = resolveHeatRequestState(callForHeatIntervals, previous.timestamp(), current.timestamp());
                boolean manualBoost = resolveManualBoost(settingIntervals, previous.timestamp(), current.timestamp());
                boolean sunny = resolveSunnyState(sunnyIntervals, previous.timestamp(), current.timestamp());

                if (demandScore == 0.0 && !boostedHeating && !manualBoost) {
                    continue;
                }

                double evidence = Math.max(
                        (demandScore * 0.7) + (riseScore * 0.3),
                        (boostedHeating || manualBoost) ? 0.78 + (riseScore * 0.22) : 0.0
                );

                if (sunny && !manualBoost && !heatRequestState.callingForHeat()) {
                    evidence *= 0.55;
                }

                signals.add(new RoomIntervalSignal(
                        previous.timestamp(),
                        current.timestamp(),
                        evidence,
                        demandScore >= 0.35 || boostedHeating || manualBoost,
                        (demandScore >= 0.35 && riseScore >= 0.12) || boostedHeating || manualBoost,
                        (demandScore >= 0.6 && riseScore >= 0.26) || slopePerHour >= 0.7 || manualBoost,
                        boostedHeating || manualBoost,
                        heatRequestState.known(),
                        heatRequestState.callingForHeat()
                ));
            }
        });

        return signals;
    }

    private Double parseNumericTarget(HourlyTemperature hourlyTemperature) {
        try {
            return Double.parseDouble(hourlyTemperature.temperature());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private HeatRequestState resolveHeatRequestState(List<CallForHeatInterval> callForHeatIntervals, LocalDateTime start, LocalDateTime end) {
        if (callForHeatIntervals == null || callForHeatIntervals.isEmpty()) {
            return new HeatRequestState(false, false);
        }

        boolean intervalKnown = callForHeatIntervals.stream()
                .anyMatch(interval -> overlaps(interval, start, end));

        if (!intervalKnown) {
            return new HeatRequestState(false, false);
        }

        boolean callingForHeat = callForHeatIntervals.stream()
                .filter(interval -> overlaps(interval, start, end))
                .anyMatch(interval -> interval.value() != null && !interval.value().equalsIgnoreCase("NONE"));

        return new HeatRequestState(true, callingForHeat);
    }

    private boolean overlaps(CallForHeatInterval interval, LocalDateTime start, LocalDateTime end) {
        if (interval == null || interval.from() == null || interval.to() == null) {
            return false;
        }

        return interval.from().isBefore(end) && interval.to().isAfter(start);
    }

    private boolean resolveManualBoost(List<SettingInterval> settingIntervals, LocalDateTime start, LocalDateTime end) {
        if (settingIntervals == null || settingIntervals.isEmpty()) {
            return false;
        }

        return settingIntervals.stream()
                .filter(interval -> overlaps(interval, start, end))
                .map(SettingInterval::value)
                .anyMatch(value -> value != null && Boolean.TRUE.equals(value.isBoost()));
    }

    private boolean resolveSunnyState(List<BooleanInterval> sunnyIntervals, LocalDateTime start, LocalDateTime end) {
        if (sunnyIntervals == null || sunnyIntervals.isEmpty()) {
            return false;
        }

        return sunnyIntervals.stream()
                .filter(interval -> overlaps(interval, start, end))
                .anyMatch(interval -> Boolean.TRUE.equals(interval.value()));
    }

    private boolean overlaps(SettingInterval interval, LocalDateTime start, LocalDateTime end) {
        if (interval == null || interval.from() == null || interval.to() == null) {
            return false;
        }

        return interval.from().isBefore(end) && interval.to().isAfter(start);
    }

    private boolean overlaps(BooleanInterval interval, LocalDateTime start, LocalDateTime end) {
        if (interval == null || interval.from() == null || interval.to() == null) {
            return false;
        }

        return interval.from().isBefore(end) && interval.to().isAfter(start);
    }

    private String confidenceLabel(int activeIntervals, int supportingIntervals) {
        if (activeIntervals == 0 || supportingIntervals == 0) {
            return "Low";
        }

        double ratio = (double) supportingIntervals / Math.max(activeIntervals, supportingIntervals);

        if (ratio >= 0.8) {
            return "High";
        }

        if (ratio >= 0.55) {
            return "Medium";
        }

        return "Low";
    }

    private record RoomIntervalSignal(
            LocalDateTime start,
            LocalDateTime end,
            double evidence,
            boolean hasDemand,
            boolean likelyHeating,
            boolean strongHeating,
            boolean boostedHeating,
            boolean heatRequestKnown,
            boolean callingForHeat
    ) {
    }

    private record HeatRequestState(boolean known, boolean callingForHeat) {
    }
}
