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

    private static final Map<String, String> GRADIENT_MAP;

    static {
        GRADIENT_MAP = new HashMap<>();

        // Blue range (5.0 - 9.5)
        GRADIENT_MAP.put("5.0", "#6699FF");   // Blue
        GRADIENT_MAP.put("5.5", "#66A3FF");   // Lighter Blue
        GRADIENT_MAP.put("6.0", "#66B3FF");   // Lighter Blue
        GRADIENT_MAP.put("6.5", "#66CCFF");   // Light Blue
        GRADIENT_MAP.put("7.0", "#66D9FF");   // Very Light Blue
        GRADIENT_MAP.put("7.5", "#66E5FF");   // Pale Blue
        GRADIENT_MAP.put("8.0", "#66F2FF");   // Very Pale Blue
        GRADIENT_MAP.put("8.5", "#66FFFF");   // Near White Blue
        GRADIENT_MAP.put("9.0", "#66CCFF");   // Light Blue
        GRADIENT_MAP.put("9.5", "#66B3FF");   // Lighter Blue

        // Green range (10.0 - 19.5)
        GRADIENT_MAP.put("10.0", "#33CC66");  // Green (Starts green)
        GRADIENT_MAP.put("10.5", "#40D25F");  // Lighter Green
        GRADIENT_MAP.put("11.0", "#4DD75B");  // Lighter Green
        GRADIENT_MAP.put("11.5", "#5AD850");  // Soft Green
        GRADIENT_MAP.put("12.0", "#60AD3C");  // Medium Green
        GRADIENT_MAP.put("12.5", "#6BBF3E");  // Light Green
        GRADIENT_MAP.put("13.0", "#77C640");  // Light Green
        GRADIENT_MAP.put("13.5", "#8AD52B");  // Greenish Yellow
        GRADIENT_MAP.put("14.0", "#8A9800");  // Yellow-Green
        GRADIENT_MAP.put("14.5", "#A1B800");  // Yellow-Green
        GRADIENT_MAP.put("15.0", "#B0D400");  // Yellow-Green
        GRADIENT_MAP.put("15.5", "#B8D800");  // Light Yellow
        GRADIENT_MAP.put("16.0", "#8A9800");  // Yellow-Green
        GRADIENT_MAP.put("16.5", "#A1B800");  // Yellow-Green
        GRADIENT_MAP.put("17.0", "#B0D400");  // Yellow-Green
        GRADIENT_MAP.put("17.5", "#B8D800");  // Light Yellow
        GRADIENT_MAP.put("18.0", "#90B800");  // Yellow-Green
        GRADIENT_MAP.put("18.5", "#A0C800");  // Yellow-Green
        GRADIENT_MAP.put("19.0", "#A9D000");  // Yellow-Green
        GRADIENT_MAP.put("19.5", "#B2D800");  // Light Green-Yellow

        // Red range (20.0 - 25.0)
        GRADIENT_MAP.put("20.0", "#D03939");  // Red (Start of Red)
        GRADIENT_MAP.put("20.5", "#D03232");  // Deeper Red
        GRADIENT_MAP.put("21.0", "#D02C2C");  // Red
        GRADIENT_MAP.put("21.5", "#D02727");  // Darker Red
        GRADIENT_MAP.put("22.0", "#D02323");  // Intense Red
        GRADIENT_MAP.put("22.5", "#D01F1F");  // Dark Red
        GRADIENT_MAP.put("23.0", "#D01B1B");  // Darker Red
        GRADIENT_MAP.put("23.5", "#D01818");  // Very Dark Red
        GRADIENT_MAP.put("24.0", "#D01414");  // Deep Red
        GRADIENT_MAP.put("24.5", "#D01010");  // Deepest Red
        GRADIENT_MAP.put("25.0", "#D00000");  // Strong Red

        // Special states
        GRADIENT_MAP.put("ON", "#D03939");    // Red (ON state)
        GRADIENT_MAP.put("OFF", "#D3D3D3");   // Grey (OFF state)
    }


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
            } else if (timetableType.equals(TimetableType.THREE_DAY)) {
                mapThreeDaySchedule(blocks, roomSchedules, roomName);
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

        private static void mapThreeDaySchedule(List<Block> blocks, List<RoomSchedule> roomSchedules, String roomName) {
        Map<Day, Map<Integer, HourlyTemperature>> threeDayHourlyTemperaturesMap = new HashMap<>();
        for (Day day : Day.values()) {
            threeDayHourlyTemperaturesMap.put(day, new HashMap<>());
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
                
                switch (dayType) {
                    case MONDAY_TO_FRIDAY:
                        for (Day day : List.of(Day.MONDAY, Day.TUESDAY, Day.WEDNESDAY, Day.THURSDAY, Day.FRIDAY)) {
                            threeDayHourlyTemperaturesMap.get(day).put(hour % 24, new HourlyTemperature(value, colour));
                        }
                        break;
                    case SATURDAY:
                        threeDayHourlyTemperaturesMap.get(Day.SATURDAY).put(hour % 24, new HourlyTemperature(value, colour));
                        break;
                    case SUNDAY:
                        threeDayHourlyTemperaturesMap.get(Day.SUNDAY).put(hour % 24, new HourlyTemperature(value, colour));
                        break;
                }
            }
        }

        for (Day day : Day.values()) {
            roomSchedules.add(new RoomSchedule(roomName, day, threeDayHourlyTemperaturesMap.get(day)));
        }
    }


    private static Double getTemperature(Block block) {
        return block.setting().temperature() != null
                ? block.setting().temperature().celsius()
                : null;
    }
}
