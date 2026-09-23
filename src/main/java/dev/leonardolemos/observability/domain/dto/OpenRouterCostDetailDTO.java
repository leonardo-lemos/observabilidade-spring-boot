package dev.leonardolemos.observability.domain.dto;

import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;

public record OpenRouterCostDetailDTO(
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens,
        PromptTokensDetailsDTO promptTokensDetails,
        CompletionTokensDetailsDTO completionTokensDetails,
        BigDecimal cost
) {

    public static OpenRouterCostDetailDTO fromUsageNode(JsonNode usageNode) {
        return new OpenRouterCostDetailDTO(
                integerValue(usageNode, "prompt_tokens"),
                integerValue(usageNode, "completion_tokens"),
                integerValue(usageNode, "total_tokens"),
                PromptTokensDetailsDTO.fromNode(usageNode.path("prompt_tokens_details")),
                CompletionTokensDetailsDTO.fromNode(usageNode.path("completion_tokens_details")),
                decimalValue(usageNode, "cost"));
    }

    private static Integer integerValue(JsonNode node, String fieldName) {
        var value = node.path(fieldName);
        return value.isMissingNode() || value.isNull() ? null : value.asInt();
    }

    private static BigDecimal decimalValue(JsonNode node, String fieldName) {
        var value = node.path(fieldName);
        return value.isMissingNode() || value.isNull() ? null : value.asDecimal();
    }

    public record PromptTokensDetailsDTO(Integer cachedTokens) {

        private static PromptTokensDetailsDTO fromNode(JsonNode node) {
            return new PromptTokensDetailsDTO(integerValue(node, "cached_tokens"));
        }
    }

    public record CompletionTokensDetailsDTO(Integer reasoningTokens) {

        private static CompletionTokensDetailsDTO fromNode(JsonNode node) {
            return new CompletionTokensDetailsDTO(integerValue(node, "reasoning_tokens"));
        }
    }

}
