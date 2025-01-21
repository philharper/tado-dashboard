package uk.co.philharper.tadodashboard;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.co.philharper.tadodashboard.model.Block;
import uk.co.philharper.tadodashboard.model.HeatingSchedule;
import uk.co.philharper.tadodashboard.model.HourlyTemperature;
import uk.co.philharper.tadodashboard.model.Room;
import uk.co.philharper.tadodashboard.model.RoomSchedule;
import uk.co.philharper.tadodashboard.model.WeeklySchedule;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class HeatingScheduleMapper {

    static List<String> days = List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");

    static Map<String, String> gradientMap = Map.of(
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
        log.info("Mapping rooms to heating schedule");
        List<RoomSchedule> roomSchedules = new ArrayList<>();
        var weeklySchedule = new WeeklySchedule(new LinkedHashMap<>());

        for (Map.Entry<String, Room> roomEntry : room.entrySet()) {
            String roomName = roomEntry.getKey();
            List<Block> blocks = roomEntry.getValue().blocks();

            Map<Integer, HourlyTemperature> hourlyTemperatures = new HashMap<>();

            for (Block block : blocks) {

                LocalTime startTime = LocalTime.parse(block.start());
                LocalTime endTime = LocalTime.parse(block.end());
                Double temperature = getTemperature(block);
                String power = block.setting().power();

                int endHour = endTime.equals(LocalTime.MIDNIGHT) ? 24 : endTime.getHour();

                for (int hour = startTime.getHour(); hour < endHour; hour++) {
                    String value = temperature != null ? temperature.toString() : power;
                    String colour = gradientMap.get(value);
                    hourlyTemperatures.put(hour % 24, new HourlyTemperature(value, colour));
                }
            }

            roomSchedules.add(new RoomSchedule(roomName, hourlyTemperatures));
        }

        roomSchedules.sort(Comparator.comparing(rs -> rs.name().equals("Hot Water")));

        for (String day : days) {
            weeklySchedule.heatingSchedule().put(day, new HeatingSchedule(roomSchedules));
        }

        log.info("Finished mapping rooms to heating schedule");

        return weeklySchedule;
    }

    private static Double getTemperature(Block block) {
        return block.setting().temperature() != null
                ? block.setting().temperature().celsius()
                : null;
    }
}
