package uk.co.philharper.tadodashboard;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.co.philharper.tadodashboard.model.Block;
import uk.co.philharper.tadodashboard.model.Day;
import uk.co.philharper.tadodashboard.model.DayType;
import uk.co.philharper.tadodashboard.model.HeatingSchedule;
import uk.co.philharper.tadodashboard.model.HourlyTemperature;
import uk.co.philharper.tadodashboard.model.Room;
import uk.co.philharper.tadodashboard.model.RoomSchedule;
import uk.co.philharper.tadodashboard.model.TimetableType;
import uk.co.philharper.tadodashboard.model.WeeklySchedule;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class HeatingScheduleMapper {

    private static final Map<String, String> GRADIENT_MAP = Map.of(
            "0", "#D3D3D3",
            "15.0", "#0bbd72",
            "16.0", "#60ad3c",
            "17.0", "#8a9800",
            "18.0", "#aa7f00",
            "19.0", "#c36013",
            "19.5", "#c36013",
            "20.0", "#d03939",
            "OFF", "#D3D3D3",
            "ON", "#d03939"
    );

    public static WeeklySchedule mapRoomsToHeatingSchedule(Map<String, Room> room) {
        List<RoomSchedule> roomSchedules = new ArrayList<>();
        var weeklySchedule = new WeeklySchedule(new LinkedHashMap<>());

        for (Map.Entry<String, Room> roomEntry : room.entrySet()) {
            var roomName = roomEntry.getKey();
            var timetableType = roomEntry.getValue().timetableType();
            var blocks = roomEntry.getValue().blocks();

            if (timetableType.equals(TimetableType.ONE_DAY)) {
                mapWeeklySchedule(blocks, roomSchedules, roomName);
            } else if (timetableType.equals(TimetableType.SEVEN_DAY)) {
                mapDailySchedule(blocks, roomSchedules, roomName);
            }
        }

        roomSchedules.sort(Comparator.comparing(rs -> rs.name().equals("Hot Water")));

        for (RoomSchedule roomSchedule : roomSchedules) {
            if (!weeklySchedule.heatingSchedule().containsKey(roomSchedule.day())) {
                weeklySchedule.heatingSchedule().put(roomSchedule.day(), new HeatingSchedule(new ArrayList<>()));
            }
            weeklySchedule.heatingSchedule().get(roomSchedule.day()).roomSchedules().add(roomSchedule);
        }

        return weeklySchedule;
    }

    private static void mapDailySchedule(List<Block> blocks, List<RoomSchedule> roomSchedules, String roomName) {
        Map<Day, Map<Integer, HourlyTemperature>> dailyHourlyTemperaturesMap = new HashMap<>();
        for (Day day : Day.values()) {
            dailyHourlyTemperaturesMap.put(day, new HashMap<>());
        }

        for (Block block : blocks) {
            LocalTime startTime = LocalTime.parse(block.start());
            LocalTime endTime = LocalTime.parse(block.end());
            Double temperature = getTemperature(block);
            String power = block.setting().power();

            int endHour = endTime.equals(LocalTime.MIDNIGHT) ? 24 : endTime.getHour();
            DayType dayType = block.dayType();

            for (int hour = startTime.getHour(); hour < endHour; hour++) {
                String value = temperature != null ? temperature.toString() : power;
                String colour = GRADIENT_MAP.get(value);
                Day day = Day.valueOf(dayType.name());
                dailyHourlyTemperaturesMap.get(day).put(hour % 24, new HourlyTemperature(value, colour));
            }
        }

        for (Day day : Day.values()) {
            roomSchedules.add(new RoomSchedule(roomName, day, dailyHourlyTemperaturesMap.get(day)));
        }
    }

    private static void mapWeeklySchedule(List<Block> blocks, List<RoomSchedule> roomSchedules, String roomName) {
        Map<Integer, HourlyTemperature> hourlyTemperatures = new HashMap<>();

        for (Block block : blocks) {

            LocalTime startTime = LocalTime.parse(block.start());
            LocalTime endTime = LocalTime.parse(block.end());
            Double temperature = getTemperature(block);
            String power = block.setting().power();

            int endHour = endTime.equals(LocalTime.MIDNIGHT) ? 24 : endTime.getHour();

            for (int hour = startTime.getHour(); hour < endHour; hour++) {
                String value = temperature != null ? temperature.toString() : power;
                String colour = GRADIENT_MAP.get(value);
                hourlyTemperatures.put(hour % 24, new HourlyTemperature(value, colour));
            }
        }

        for (Day day : Day.values()) {
            roomSchedules.add(new RoomSchedule(roomName, day, hourlyTemperatures));
        }
    }

    private static Double getTemperature(Block block) {
        return block.setting().temperature() != null
                ? block.setting().temperature().celsius()
                : null;
    }
}
