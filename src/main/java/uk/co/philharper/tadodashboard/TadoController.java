package uk.co.philharper.tadodashboard;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import uk.co.philharper.tadodashboard.model.Room;

import java.util.HashMap;
import java.util.Map;

@Controller
@Slf4j
public class TadoController {

    @Autowired
    private TadoService tadoService;

    @GetMapping("/schedules")
    public String getSchedules(Model model) {
        Map<String, Room> roomBlocks = new HashMap<>();

        tadoService.authenticate();

        var userInfo = tadoService.getUserInfo();

        var homeId = userInfo.homes().get(0).id();

        tadoService.getZones(homeId).forEach(zone -> {
            var zoneId = zone.id();
            var timetable = tadoService.getActiveTimetable(homeId, zoneId);
            var blocks = tadoService.getBlocks(homeId, zoneId, timetable.id());
            roomBlocks.put(zone.name(), new Room(timetable.type(), blocks));

            if (zone.name().equals("Living Room")) {
                log.info("DAY REPORT");
                log.info(tadoService.getDayReport(homeId, zoneId));
            }
        });

        var weeklySchedule = HeatingScheduleMapper.mapRoomsToHeatingSchedule(roomBlocks);

        model.addAttribute("userInfo", userInfo);
        model.addAttribute("weeklySchedule", weeklySchedule);

        return "index2";
    }
}

