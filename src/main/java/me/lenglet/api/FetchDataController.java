package me.lenglet.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import me.lenglet.common.EncryptionService;
import org.bson.Document;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

import static me.lenglet.common.Bsons.objectId;

@RestController
public class FetchDataController {

    private final MongoClient mongoClient;
    private final EncryptionService encryptionService;

    public FetchDataController(
            MongoClient mongoClient,
            EncryptionService encryptionService
    ) {
        this.mongoClient = mongoClient;
        this.encryptionService = encryptionService;
    }

    @PostMapping("/vaults/{vaultName}/{objectId}")
    public String execute(
            @PathVariable("vaultName") String vaultName,
            @PathVariable("objectId") String objectId
    ) throws JsonProcessingException {

        final var doc = this.mongoClient.getDatabase("admin")
                .getCollection(vaultName)
                .find(objectId(objectId))
                .first();

        return toJson(doc);
    }

    private String toJson(Document document) throws JsonProcessingException {
        final var mapper = new ObjectMapper();
        final var result = mapper.createObjectNode();

        result.put("$objectId", document.getString("objectId"));
        for (Document secret : document.getList("secrets", Document.class, Collections.emptyList())) {
            result.put(secret.getString("key"), this.encryptionService.decrypt(secret.getString("value")));
        }

        return mapper.writeValueAsString(result);
    }
}
