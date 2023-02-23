package me.lenglet.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SecretDto(
        @JsonProperty("$value") String value,
        @JsonProperty("$type") String type,
        @JsonProperty("$immutable") Boolean immutable
) {
}
