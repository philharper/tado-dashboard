package uk.co.philharper.tadodashboard;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import uk.co.philharper.tadodashboard.model.DeviceAuthorisationResponse;
import uk.co.philharper.tadodashboard.model.TokenResponse;

@FeignClient(name = "tadoOAuthClient", url = "${tado.api.oauth-url}")
public interface TadoOAuthClient {

    @PostMapping("/oauth2/device_authorize")
    DeviceAuthorisationResponse authorizeDevice(@RequestParam("client_id") String clientId, @RequestParam("scope") String scope);

    @PostMapping("/oauth2/token")
    ResponseEntity<TokenResponse> getToken(@RequestParam("client_id") String clientId, @RequestParam("device_code") String deviceCode, @RequestParam("grant_type") String grantType);
}
