package uk.co.philharper.tadodashboard;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import uk.co.philharper.tadodashboard.model.Block;
import uk.co.philharper.tadodashboard.model.DayReport;
import uk.co.philharper.tadodashboard.model.Timetable;
import uk.co.philharper.tadodashboard.model.UserInfo;
import uk.co.philharper.tadodashboard.model.Zone;

import java.util.List;

@FeignClient(name = "tado", url = "${tado.api.base-url}")
public interface TadoClient {

    @GetMapping(value = "me", produces = "application/json")
    UserInfo getUserInfo(@RequestHeader("Authorization") String bearerToken);

    @GetMapping(value = "homes/{homeId}/zones", produces = "application/json")
    List<Zone> getZones(@PathVariable("homeId") int homeId, @RequestHeader("Authorization") String bearerToken);

    @GetMapping(value = "homes/{homeId}/zones/{zoneId}/schedule/activeTimetable", produces = "application/json")
    Timetable getActiveTimetable(@PathVariable("homeId") int homeId, @PathVariable("zoneId") int zoneId, @RequestHeader("Authorization") String bearerToken);

    @GetMapping(value = "/homes/{homeId}/zones/{zoneId}/schedule/timetables/{timetableTypeId}/blocks", produces = "application/json")
    List<Block> getBlocks(@PathVariable("homeId") int homeId, @PathVariable("zoneId") int zoneId, @PathVariable("timetableTypeId") int timetableTypeId, @RequestHeader("Authorization") String bearerToken);

    @GetMapping(value = "/homes/{homeId}/zones/{zoneId}/dayReport", produces = "application/json")
    DayReport getDayReport(@PathVariable("homeId") int homeId, @PathVariable("zoneId") int zoneId, @RequestParam String date, @RequestHeader("Authorization") String bearerToken);
}
