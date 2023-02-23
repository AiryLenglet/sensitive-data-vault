package me.lenglet.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import me.lenglet.api.dto.PersistDataRequestDto;
import me.lenglet.api.dto.SecretDto;
import me.lenglet.common.BlindIndexService;
import me.lenglet.common.EncryptionService;
import me.lenglet.common.TokenGenerator;
import org.bson.Document;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

import static me.lenglet.common.Bsons.objectId;

@RestController
public class PersistDataController {


    private final EncryptionService encryptionService;
    private final TokenGenerator tokenGenerator;
    private final MongoClient mongoClient;
    private final BlindIndexService blindIndexService;

    public PersistDataController(
            EncryptionService encryptionService,
            TokenGenerator tokenGenerator,
            MongoClient mongoClient,
            BlindIndexService blindIndexService
    ) {
        this.encryptionService = encryptionService;
        this.tokenGenerator = tokenGenerator;
        this.mongoClient = mongoClient;
        this.blindIndexService = blindIndexService;
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

        return new Document()
                .append("key", field)
                .append("value", cypherValue)
                .append("token", this.tokenGenerator.generate())
                .append("type", "string")
                .append("index", this.blindIndexService.computeBlindIndex(plaintextValue));
    }
}
