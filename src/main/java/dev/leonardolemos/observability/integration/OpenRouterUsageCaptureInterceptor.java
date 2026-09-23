package dev.leonardolemos.observability.integration;

import dev.leonardolemos.observability.domain.dto.OpenRouterCostDetailDTO;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
@Slf4j
public class OpenRouterUsageCaptureInterceptor implements Interceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenRouterUsageCaptureInterceptor.class);

    private static final long MAX_RESPONSE_BODY_BYTES = 1024 * 1024;

    private static final String COST_METRIC_NAME = "gen_ai.usage.cost";

    private static final String COST_OBSERVATION_KEY = "gen_ai.usage.cost";

    private static final String USAGE_OBSERVATION_KEY = "gen_ai.openrouter.usage";

    private static final String OPENROUTER_PROVIDER = "openrouter";

    private static final String UNKNOWN_MODEL = "unknown";

    private final MeterRegistry meterRegistry;

    private final ObservationRegistry observationRegistry;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenRouterUsageCaptureInterceptor(MeterRegistry meterRegistry, ObservationRegistry observationRegistry) {
        this.meterRegistry = meterRegistry;
        this.observationRegistry = observationRegistry;
    }

    @Override
    @NotNull
    public Response intercept(Chain chain) throws IOException {
        log.info("Intercepting OpenRouter response to capture usage metadata.");

        var response = chain.proceed(chain.request());
        var responseBody = response.body();

        if (responseBody == null || !isJsonResponse(response)) {
            return response;
        }

        var contentType = responseBody.contentType();
        var responseBodyText = responseBody.string();

        captureUsage(responseBodyText);

        return response.newBuilder()
                .body(ResponseBody.create(contentType, responseBodyText))
                .build();
    }

    private boolean isJsonResponse(Response response) {
        var responseBody = response.body();
        if (responseBody == null || responseBody.contentLength() > MAX_RESPONSE_BODY_BYTES) {
            return false;
        }

        var contentType = responseBody.contentType();
        return contentType == null || contentType.subtype().contains("json");
    }

    private void captureUsage(String responseBodyText) {
        try {
            var rootNode = objectMapper.readTree(responseBodyText);
            var usageNode = rootNode.path("usage");

            if (!usageNode.isObject()) {
                return;
            }

            var usage = OpenRouterCostDetailDTO.fromUsageNode(usageNode);
            if (usage.cost() == null) {
                return;
            }
            var serializedUsage = objectMapper.writeValueAsString(usage);
            var model = rootNode.path("model").isMissingNode()
                    ? UNKNOWN_MODEL
                    : rootNode.path("model").asString(UNKNOWN_MODEL);

            if (observationRegistry.getCurrentObservation() != null) {
                observationRegistry.getCurrentObservation()
                        .highCardinalityKeyValue(USAGE_OBSERVATION_KEY, serializedUsage)
                        .highCardinalityKeyValue(COST_OBSERVATION_KEY, usage.cost().toPlainString());
            }

            log.info("Captured OpenRouter usage metadata: {}", serializedUsage);

            Counter.builder(COST_METRIC_NAME)
                    .description("Total cost reported by OpenRouter chat completion metadata.")
                    .baseUnit("usd")
                    .tag("provider", OPENROUTER_PROVIDER)
                    .tag("model", model)
                    .register(meterRegistry)
                    .increment(usage.cost().doubleValue());

            DistributionSummary.builder("gen_ai.usage.cost.per.request")
                    .baseUnit("usd")
                    .tag("provider", "openrouter")
                    .tag("model", model)
                    .register(meterRegistry)
                    .record(usage.cost().doubleValue());
        }
        catch (JacksonException ex) {
            LOGGER.debug("Unable to deserialize OpenRouter usage metadata from response body.", ex);
        }
    }
}
