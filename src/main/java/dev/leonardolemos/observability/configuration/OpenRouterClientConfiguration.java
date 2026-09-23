package dev.leonardolemos.observability.configuration;

import dev.leonardolemos.observability.domain.response.WeatherInsightResponse;
import dev.leonardolemos.observability.integration.OpenRouterUsageCaptureInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.openai.http.okhttp.OpenAiHttpClientBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class OpenRouterClientConfiguration {

    private final ChatModel chatModel;

    @Bean
    public ChatClient openRouterChatClient() {
        return ChatClient.create(chatModel);
    }

    @Bean
    public BeanOutputConverter<WeatherInsightResponse> weatherInsightResponseConverter() {
        return new BeanOutputConverter<>(WeatherInsightResponse.class);
    }

    @Bean
    public static OpenAiHttpClientBuilderCustomizer openRouterUsageCaptureHttpClientCustomizer(
            OpenRouterUsageCaptureInterceptor interceptor) {
        return builder -> builder.interceptor(interceptor);
    }
}
