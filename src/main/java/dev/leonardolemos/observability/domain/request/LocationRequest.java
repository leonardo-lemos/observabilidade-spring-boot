package dev.leonardolemos.observability.domain.request;

public record LocationRequest(
        String latitude,
        String longitude
) {
}
