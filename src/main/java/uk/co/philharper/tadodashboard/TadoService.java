package uk.co.philharper.tadodashboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import uk.co.philharper.tadodashboard.model.Block;
import uk.co.philharper.tadodashboard.model.Timetable;
import uk.co.philharper.tadodashboard.model.UserInfo;
import uk.co.philharper.tadodashboard.model.Zone;

import java.util.List;
import java.util.Map;

@Service
public class TadoService {

    @Value("${tado.api.auth-url}")
    private String authUrl;

    @Value("${tado.api.base-url}")
    private String baseUrl;

    @Value("${tado.api.username}")
    private String username;

    @Value("${tado.api.password}")
    private String password;

    @Value("${tado.api.client-id}")
    private String clientId;

    @Value("${tado.api.client-secret}")
    private String clientSecret;

    private String accessToken;

    @Autowired
    TadoClient tadoClient;

    @Autowired
    TadoAuthClient tadoAuthClient;

    public void authenticate() {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("scope", "home.user");
        body.add("username", username);
        body.add("password", password);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);

        Map<String, Object> responseBody = tadoAuthClient.authenticate(body);
        this.accessToken = "Bearer " + responseBody.get("access_token");
    }

    public UserInfo getUserInfo() {
        return tadoClient.getUserInfo(accessToken);
    }

    public List<Zone> getZones(int homeId) {
        return tadoClient.getZones(homeId, accessToken);
    }

    public Timetable getActiveTimetable(int homeId, int zoneId) {
        return tadoClient.getActiveTimetable(homeId, zoneId, accessToken);
    }

    public List<Block> getBlocks(int homeId, int zoneId, int timetableTypeId) {
        return tadoClient.getBlocks(homeId, zoneId, timetableTypeId, accessToken);
    }

    public String getDayReport(int homeId, int zoneId) {
        return tadoClient.getDayReport(homeId, zoneId, accessToken);
    }
}

