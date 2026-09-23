package dev.leonardolemos.observability.integration;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OpenMeteoClient {

    private final RestClient restClient;

    private final ObservationRegistry observationRegistry;

    private final static String OPEN_METEO_BASE_URL = "https://api.open-meteo.com/v1";

    public OpenMeteoClient(ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
        restClient = RestClient.create(OPEN_METEO_BASE_URL);
    }

    public String getWeatherForecast(String latitude, String longitude) {
        var observation = Observation.createNotStarted("integration.http.client.requests", observationRegistry)
                .lowCardinalityKeyValue("target.host", "api.open-meteo.com")
                .lowCardinalityKeyValue("http.method", "GET");

        return observation.observe(() -> {
            var uriPath = "/forecast?latitude={lat}&longitude={lon}&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m&timezone=auto";

            observation.highCardinalityKeyValue("http.url", OPEN_METEO_BASE_URL + "/forecast?latitude=" + latitude + "&longitude=" + longitude);

            try {
                var response = restClient
                        .get()
                        .uri(uriPath, latitude, longitude)
                        .retrieve()
                        .toEntity(String.class);

                var statusCode = response.getStatusCode();

                observation.lowCardinalityKeyValue("http.status_code", String.valueOf(statusCode.value()));
                observation.lowCardinalityKeyValue("outcome", statusCode.is2xxSuccessful() ? "SUCCESS" : "ERROR");

                return response.getBody();
            } catch (Exception e) {
                observation.lowCardinalityKeyValue("http.status_code", "CLIENT_ERROR");
                observation.lowCardinalityKeyValue("outcome", "EXCEPTION");

                throw e;
            }
        });
    }

}
