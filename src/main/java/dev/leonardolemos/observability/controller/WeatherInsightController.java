package dev.leonardolemos.observability.controller;

import dev.leonardolemos.observability.domain.request.LocationRequest;
import dev.leonardolemos.observability.domain.response.WeatherInsightResponse;
import dev.leonardolemos.observability.service.WeatherInsightService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/weather")
public class WeatherInsightController {

    private final WeatherInsightService weatherInsightService;

    @GetMapping
    ResponseEntity<WeatherInsightResponse> getWeatherInsight(LocationRequest request) {
        return ResponseEntity.ok(weatherInsightService.getWeatherInsight(request.latitude(), request.longitude()));
    }

}
