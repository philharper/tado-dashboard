package uk.co.philharper.tadodashboard.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthorisationResponse(@JsonProperty("access_token") String accessToken, @JsonProperty("expires_in") int expiresIn) {
}
