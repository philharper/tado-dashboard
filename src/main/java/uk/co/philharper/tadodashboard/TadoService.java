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

    @Value("${tado.api.client-secret}")
    private String clientSecret;

    @Autowired
    private TadoClient tadoClient;

    @Autowired
    private TadoAuthClient tadoAuthClient;

    @Autowired
    private HttpSession session;

    public void authenticate(String username, String password) {
        var token = getAuthValues(username, password);
        session.setAttribute("token", "Bearer " + token.accessToken());
        session.setAttribute("expiry", LocalDateTime.now().plusSeconds(token.expiresIn()));
    }

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

    private String getToken() {
        return (String) session.getAttribute("token");
    }

    private AuthorisationResponse getAuthValues(String username, String password) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("username", username);
        body.add("password", password);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);

        return tadoAuthClient.authenticate(body);
    }
}