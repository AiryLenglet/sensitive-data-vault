package me.lenglet.api.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class PersistDataRequestDto {
    @JsonProperty("$objectId")
    private String objectId;
    @JsonAnySetter
    private Map<String, SecretDto> secrets;

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public Map<String, SecretDto> getSecrets() {
        return secrets;
    }

    public void setSecrets(Map<String, SecretDto> secrets) {
        this.secrets = secrets;
    }
}
