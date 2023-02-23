package me.lenglet;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import me.lenglet.common.EncryptionService;
import me.lenglet.common.HashService;
import me.lenglet.common.TokenGenerator;
import org.bson.Document;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static me.lenglet.common.Bsons.objectId;

@RestController
public class PersistDataController {


    private final EncryptionService encryptionService;
    private final TokenGenerator tokenGenerator;
    private final MongoClient mongoClient;
    private final HashService hashService;

    public PersistDataController(
            EncryptionService encryptionService,
            TokenGenerator tokenGenerator,
            MongoClient mongoClient,
            HashService hashService
    ) {
        this.encryptionService = encryptionService;
        this.tokenGenerator = tokenGenerator;
        this.mongoClient = mongoClient;
        this.hashService = hashService;
    }

    @PostMapping("/vaults/{vaultName}")
    public String execute(
            @PathVariable("vaultName") String vaultName,
            @RequestBody PersistDataRequestDto request
    ) throws JsonProcessingException {

        final var doc = this.toDoc(request);
        mongoClient.getDatabase("admin")
                .getCollection(vaultName)
                .insertOne(doc);

        return toJson(doc);
    }

    public static class PersistDataRequestDto {
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

    public record SecretDto(
            @JsonProperty("$value") String value,
            @JsonProperty("$type") String type,
            @JsonProperty("$immutable") Boolean immutable
    ) {
    }

    private String toJson(Document document) throws JsonProcessingException {
        final var mapper = new ObjectMapper();
        final var result = mapper.createObjectNode();

        result.put("$objectId", document.getString("objectId"));
        for (Document secret : document.getList("secrets", Document.class, Collections.emptyList())) {
            result.put(secret.getString("key"), secret.getString("token"));
        }

        return mapper.writeValueAsString(result);
    }

    Document toDoc(PersistDataRequestDto jsonNode) {
        final var doc = objectId(jsonNode.getObjectId());

        final var secrets = jsonNode.getSecrets().entrySet().stream()
                .filter(e -> !e.getKey().startsWith("$"))
                .map(e -> createSecretAttributeDoc(e.getKey(), e.getValue()))
                .toList();

        doc.append("secrets", secrets);
        return doc;
    }

    private Document createSecretAttributeDoc(String field, SecretDto jsonNode) {
        final var valueNode = jsonNode.value();
        final var plaintextValue = valueNode;
        final var cypherValue = this.encryptionService.encrypt(plaintextValue);
        final var blindIndex = List.of(this.hashService.hash(plaintextValue));

        return new Document()
                .append("key", field)
                .append("value", cypherValue)
                .append("token", this.tokenGenerator.generate())
                .append("type", "string")
                .append("index", blindIndex);
    }
}
