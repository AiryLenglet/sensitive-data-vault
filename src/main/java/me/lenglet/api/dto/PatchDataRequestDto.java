package me.lenglet.api.dto;

import java.util.Map;

public record PatchDataRequestDto(
        String op,
        Map.Entry<String, SecretDto> value,
        String field
) {
}
