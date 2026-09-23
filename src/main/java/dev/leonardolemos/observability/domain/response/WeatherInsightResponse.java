package dev.leonardolemos.observability.domain.response;

import java.util.List;

public record WeatherInsightResponse(
        String conditionSummary,
        int outdoorActivityScore, // 1 to 10
        String recommendedTimeWindow,
        List<String> practicalAlerts,
        List<String> clothingRecommendations
) {
}
