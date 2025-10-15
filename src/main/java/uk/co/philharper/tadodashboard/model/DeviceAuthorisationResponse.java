package uk.co.philharper.tadodashboard.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DeviceAuthorisationResponse(
        @JsonProperty("device_code") String deviceCode,
        @JsonProperty("expires_in") int expiresIn,
        @JsonProperty("interval") int interval,
        @JsonProperty("user_code") String userCode,
        @JsonProperty("verification_uri") String verificationUri,
        @JsonProperty("verification_uri_complete") String verificationUriComplete
) {}

