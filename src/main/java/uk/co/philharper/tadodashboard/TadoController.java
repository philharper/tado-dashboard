package uk.co.philharper.tadodashboard;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import uk.co.philharper.tadodashboard.model.DayReport;
import uk.co.philharper.tadodashboard.model.Room;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Controller
@Slf4j
public class TadoController {

    @Autowired
    private TadoService tadoService;

    @Autowired
    private HttpSession session;

    @GetMapping("/schedule")
    public String getSchedules(Model model) {
        if (session.getAttribute("token") == null || ((LocalDateTime) session.getAttribute("expiry")).isBefore(LocalDateTime.now())) {
            return "redirect:/login";
        }

        Map<String, Room> roomBlocks = new HashMap<>();

        var userInfo = tadoService.getUserInfo();

        var homeId = userInfo.homes().get(0).id();

        tadoService.getZones(homeId).forEach(zone -> {
            var zoneId = zone.id();
            var timetable = tadoService.getActiveTimetable(homeId, zoneId);
            var blocks = tadoService.getBlocks(homeId, zoneId, timetable.id());
            roomBlocks.put(zone.name(), new Room(timetable.type(), blocks));
        });

        var weeklySchedule = HeatingScheduleMapper.mapRoomsToHeatingSchedule(roomBlocks);

        model.addAttribute("userInfo", userInfo);
        model.addAttribute("weeklySchedule", weeklySchedule);

        return "schedule";
    }

    @GetMapping("/chart")
    public String getChart(@RequestParam(value = "date", required = false) String date, Model model) {
        if (session.getAttribute("token") == null || ((LocalDateTime) session.getAttribute("expiry")).isBefore(LocalDateTime.now())) {
            return "redirect:/login";
        }

        if (date == null || date.isEmpty()) {
            date = LocalDateTime.now().toLocalDate().toString();
        }

        LocalDateTime selectedDate = LocalDateTime.parse(date + "T00:00:00");

        Map<String, DayReport> dayReports = new HashMap<>();

        var userInfo = tadoService.getUserInfo();
        var homeId = userInfo.homes().get(0).id();

        tadoService.getZones(homeId).forEach(zone -> {
            var zoneId = zone.id();

            if (!zone.name().equals("Hot Water")) {
                dayReports.put(zone.name(), tadoService.getDayReport(homeId, zoneId, selectedDate.toLocalDate().toString()));
            }
        });

        model.addAttribute("userInfo", userInfo);
        model.addAttribute("dayReports", dayReports);
        model.addAttribute("selectedDate", selectedDate.toLocalDate().toString());

        return "chart";
    }

}

