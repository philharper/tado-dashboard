package uk.co.philharper.tadodashboard;

import org.springframework.stereotype.Component;
import uk.co.philharper.tadodashboard.model.BoilerRuntimeEstimate;
import uk.co.philharper.tadodashboard.model.DataPoint;
import uk.co.philharper.tadodashboard.model.Day;
import uk.co.philharper.tadodashboard.model.DayReport;
import uk.co.philharper.tadodashboard.model.HourlyTemperature;
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

    public BoilerRuntimeEstimate estimate(Map<String, DayReport> dayReports, WeeklySchedule weeklySchedule, Day day) {
        Map<String, Map<Integer, Double>> targetsByRoom = buildTargetsByRoom(weeklySchedule, day);
        List<RoomIntervalSignal> roomSignals = buildRoomSignals(dayReports, targetsByRoom);

        if (roomSignals.isEmpty()) {
            return new BoilerRuntimeEstimate(0, 0, 0, 0, "Low");
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

            double aggregateEvidence = activeSignals.stream()
                    .map(RoomIntervalSignal::evidence)
                    .sorted(Comparator.reverseOrder())
                    .limit(3)
                    .mapToDouble(Double::doubleValue)
                    .sum();

            boolean intervalSupported = (heatingRooms >= 2)
                    || (strongRooms >= 1 && demandingRooms >= 2)
                    || aggregateEvidence >= 1.45;

            boolean intervalOn = intervalSupported
                    || (boilerOn && demandingRooms >= 2 && aggregateEvidence >= 0.95);

            if (intervalSupported) {
                supportingIntervals++;
            }

            boilerOn = intervalOn;

            if (intervalOn) {
                totalMinutes += minutes;
                activeIntervals++;
            }
        }

        return new BoilerRuntimeEstimate(
                totalMinutes,
                activeIntervals,
                observedIntervals,
                supportingIntervals,
                confidenceLabel(activeIntervals, supportingIntervals)
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
                if (target == null) {
                    continue;
                }

                double tempGap = target - currentTemp;
                if (tempGap <= 0.15) {
                    continue;
                }

                double slopePerHour = ((currentTemp - previousTemp) / minutes) * 60.0;
                double demandScore = clamp(tempGap / 1.8);
                double riseScore = clamp((slopePerHour - 0.12) / 0.9);
                double evidence = (demandScore * 0.7) + (riseScore * 0.3);

                signals.add(new RoomIntervalSignal(
                        previous.timestamp(),
                        current.timestamp(),
                        evidence,
                        demandScore >= 0.45,
                        demandScore >= 0.45 && riseScore >= 0.18,
                        demandScore >= 0.65 && riseScore >= 0.32
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

    private record RoomIntervalSignal(LocalDateTime start, LocalDateTime end, double evidence, boolean hasDemand, boolean likelyHeating, boolean strongHeating) {
    }
}
