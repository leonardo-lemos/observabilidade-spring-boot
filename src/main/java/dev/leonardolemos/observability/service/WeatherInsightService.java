package dev.leonardolemos.observability.service;

import dev.leonardolemos.observability.domain.response.WeatherInsightResponse;
import dev.leonardolemos.observability.integration.OpenMeteoClient;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class WeatherInsightService {

    private final ChatClient chatClient;

    private final OpenMeteoClient openMeteoClient;

    private final BeanOutputConverter<WeatherInsightResponse> weatherInsightResponseConverter;

    private final ObservationRegistry observationRegistry;

    private static final Map<String, Object> OPENROUTER_USAGE_EXTRA_BODY = Map.of("usage", Map.of("include", true));

    private static final String SYSTEM_PROMPT = """
            Você é um motor analítico meteorológico integrado a um sistema corporativo.
            Sua função é interpretar dados meteorológicos brutos e transformá-los em diagnósticos
            práticos, acionáveis e confiáveis para o dia a dia.
            
            Diretrizes de interpretação:
            - Score de Atividades ao Ar Livre (1 a 10): penalize severamente chuva (>0mm), vento forte (>35km/h)
              e temperaturas extremas (<10°C ou >34°C).
            - Alertas: foque em impactos reais (alagamentos, tempo seco, UV elevado, trânsito).
            - Nunca invente eventos severos se as métricas estiverem estáveis.
            - Recomendações de vestuário: baseie-se em temperatura, umidade e vento. Evite sugestões genéricas.
            - Gere respostas sempre em português do Brasil, mesmo que os dados de entrada estejam em outro idioma.
            """;

    private static final String USER_PROMPT_TEMPLATE = """
                                Localidade: {localizacao}
                                Dados brutos da API:
                                {dadosClima}
                                """;

    @Observed(name = "service.weather.insight", contextualName = "Weather Insight Service", lowCardinalityKeyValues = {
            "service", "weather-insight",
            "operation", "getWeatherInsight",
            "integration", "open-meteo",
            "integration", "openrouter"
    })
    public WeatherInsightResponse getWeatherInsight(String latitude, String longitude) {
        var weatherData = openMeteoClient.getWeatherForecast(latitude, longitude);

        var responseEntity = chatClient.prompt()
                .system(WeatherInsightService.SYSTEM_PROMPT)
                .user(promptUserSpec -> promptUserSpec
                        .text(USER_PROMPT_TEMPLATE)
                        .param("localizacao", String.format("Latitude: %s, Longitude: %s", latitude, longitude))
                        .param("dadosClima", weatherData))
                .options(OpenAiChatOptions.builder().extraBody(OPENROUTER_USAGE_EXTRA_BODY))
                .call()
                .responseEntity(weatherInsightResponseConverter);

        if (observationRegistry.getCurrentObservation() != null) {
            observationRegistry.getCurrentObservation().lowCardinalityKeyValue("weather.insight", "success");
        }

        return Objects.requireNonNull(responseEntity.entity());
    }

}
