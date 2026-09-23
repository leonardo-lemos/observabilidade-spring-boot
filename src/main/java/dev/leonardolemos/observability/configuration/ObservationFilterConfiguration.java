package dev.leonardolemos.observability.configuration;

import io.micrometer.observation.ObservationPredicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.observation.ServerRequestObservationContext;

@Configuration
public class ObservationFilterConfiguration {

    @Bean
    public ObservationPredicate noActuactorObservation() {
        return (name, context) -> {
            if (context instanceof ServerRequestObservationContext serverContext) {
                String uri = serverContext.getCarrier().getRequestURI();
                // Retorna false para ignorar completamente qualquer requisição que comece com /actuator
                return uri == null || !uri.startsWith("/actuator");
            }
            return true;
        };
    }

}
