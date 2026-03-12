package uk.co.philharper.tadodashboard;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import uk.co.philharper.tadodashboard.model.*;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class TadoService {

    @Value("${tado.api.client-id}")
    private String clientId;

    @Autowired
    private TadoClient tadoClient;

    @Autowired
    private TadoOAuthClient tadoOAuthClient;

    @Autowired
    private HttpSession session;

    public UserInfo getUserInfo() {
        return tadoClient.getUserInfo(getToken());
    }

    public List<Zone> getZones(int homeId) {
        return tadoClient.getZones(homeId, getToken());
    }

    public Timetable getActiveTimetable(int homeId, int zoneId) {
        return tadoClient.getActiveTimetable(homeId, zoneId, getToken());
    }

    public List<Block> getBlocks(int homeId, int zoneId, int timetableTypeId) {
        return tadoClient.getBlocks(homeId, zoneId, timetableTypeId, getToken());
    }

    public DayReport getDayReport(int homeId, int zoneId, String date) {
        return tadoClient.getDayReport(homeId, zoneId, date, getToken());
    }

    private String getToken() {
        return (String) session.getAttribute("token");
    }

    public DeviceAuthorisationResponse authoriseDevice() {
        var deviceAuthReponse = tadoOAuthClient.authorizeDevice(clientId, "offline_access");
        session.setAttribute("deviceCode", deviceAuthReponse.deviceCode());
        return deviceAuthReponse;
    }

    public TokenResponse getTokenResponse() {
        try {
            var token = tadoOAuthClient.getToken(clientId, (String) session.getAttribute("deviceCode"), "urn:ietf:params:oauth:grant-type:device_code");
            if (token.getStatusCode().is2xxSuccessful()) {
                session.setAttribute("token", "Bearer " + token.getBody().accessToken());
                session.setAttribute("expiry", LocalDateTime.now().plusSeconds(token.getBody().expiresIn()));
            }
            return token.getBody();
        } catch (Exception e) {
            return null;
        }

    }
}