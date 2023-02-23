package me.lenglet;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.client.MongoClient;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.Updates;
import me.lenglet.common.EncryptionService;
import me.lenglet.common.HashService;
import me.lenglet.common.TokenGenerator;
import org.bson.Document;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static me.lenglet.common.Bsons.objectId;

@RestController
public class PatchDataController {

    private final EncryptionService encryptionService;
    private final TokenGenerator tokenGenerator;
    private final MongoClient mongoClient;
    private final HashService hashService;

    public PatchDataController(
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

    @PatchMapping("/vaults/{vaultName}/{objectId}")
    public String execute(
            @PathVariable("vaultName") String vaultName,
            @PathVariable("objectId") String objectId,
            @RequestBody Set<PatchOperationRequestDto> operations
    ) throws JsonProcessingException {

        final var objectMapper = new ObjectMapper();
        final var result = objectMapper.createObjectNode();

        final var writeOperations = operations.stream()
                .map(op -> this.map(objectId, op, result))
                .toList();

        final var bulkWriteResult = this.mongoClient.getDatabase("admin")
                .getCollection(vaultName)
                .bulkWrite(writeOperations);

        return objectMapper.writeValueAsString(result);
    }

    private UpdateOneModel<Document> map(String objectId, PatchOperationRequestDto operation, ObjectNode result) {
        return switch (operation.op()) {
            case "replace" -> {
                final var token = this.tokenGenerator.generate();
                result.put(operation.value().getKey(), token);
                yield this.createReplaceSecretWriteOperation(
                        objectId,
                        operation.value().getKey(),
                        operation.value().getValue().value(),
                        token
                );
            }
            case "add" -> {
                final var token = this.tokenGenerator.generate();
                result.put(operation.value().getKey(), token);
                yield this.createAddSecretWriteOperation(
                        objectId,
                        operation.value().getKey(),
                        operation.value().getValue().value(),
                        token
                );
            }
            case "remove" -> createRemoveSecretWriteOperation(
                    objectId,
                    operation.field()
            );
            default -> throw new IllegalArgumentException("Unknown operation value " + operation.op());
        };
    }

    private static UpdateOneModel<Document> createRemoveSecretWriteOperation(String objectId, String secretKeyToRemove) {
        return new UpdateOneModel<>(
                objectId(objectId),
                new Document("$pull", new Document(
                        "secrets",
                        new Document().append("key", secretKeyToRemove))
                )
        );
    }

    private UpdateOneModel<Document> createAddSecretWriteOperation(
            String objectId,
            String secretKey,
            String plaintextValue,
            String token
    ) {
        final var cypherValue = this.encryptionService.encrypt(plaintextValue);
        final var blindIndex = List.of(this.hashService.hash(plaintextValue));

        return new UpdateOneModel<>(
                objectId(objectId),
                Updates.push(
                        "secrets",
                        new Document()
                                .append("key", secretKey)
                                .append("value", cypherValue)
                                .append("token", token)
                                .append("type", "string")
                                .append("index", blindIndex))
        );
    }

    private UpdateOneModel<Document> createReplaceSecretWriteOperation(
            String objectId,
            String secretKey,
            String plaintextValue,
            String token
    ) {
        final var cypherValue = this.encryptionService.encrypt(plaintextValue);
        final var blindIndex = List.of(this.hashService.hash(plaintextValue));
        return new UpdateOneModel<>(
                objectId(objectId)
                        .append("secrets.key", secretKey),
                new Document("$set", new Document()
                        .append("secrets.$.value", cypherValue)
                        .append("secrets.$.token", token)
                        .append("secrets.$.index", blindIndex))
        );
    }

    public record PatchOperationRequestDto(
            String op,
            Map.Entry<String, SecretDto> value,
            String field
    ) {
    }

    public record SecretDto(
            @JsonProperty("$value") String value,
            @JsonProperty("$type") String type,
            @JsonProperty("$immutable") Boolean immutable
    ) {
    }
}
