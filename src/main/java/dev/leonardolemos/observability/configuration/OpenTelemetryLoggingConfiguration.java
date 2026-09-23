package dev.leonardolemos.observability.configuration;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;

/**
 * Liga o appender do Logback (configurado em logback-spring.xml) à instância de
 * OpenTelemetry gerenciada pelo Spring Boot, permitindo que os logs da aplicação
 * sejam exportados via OTLP (ex.: para o Loki).
 */
@Configuration
public class OpenTelemetryLoggingConfiguration implements InitializingBean {

    private final OpenTelemetry openTelemetry;

    public OpenTelemetryLoggingConfiguration(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
    }

    @Override
    public void afterPropertiesSet() {
        OpenTelemetryAppender.install(this.openTelemetry);
    }

}
