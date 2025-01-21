package uk.co.philharper.tadodashboard;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "tadoAuthClient", url = "${tado.api.auth-url}")
public interface TadoAuthClient {

    @PostMapping(consumes = "application/x-www-form-urlencoded", produces = "application/json")
    Map<String, Object> authenticate(@RequestBody MultiValueMap<String, String> body);
}
