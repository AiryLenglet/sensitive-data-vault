package me.lenglet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.client.MongoClient;
import me.lenglet.common.EncryptionService;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
            @PathVariable("objectId") String objectId,
            @RequestBody String json
    ) throws JsonProcessingException {
        final var node = new ObjectMapper().readTree(json);

        final var projection = this.from(node);

        final var doc = this.mongoClient.getDatabase("admin")
                .getCollection(vaultName)
                .find(new Document().append("_id", new ObjectId(objectId)))
                .projection(projection)
                .first();

        return toJson(doc);
    }

    private String toJson(Document document) throws JsonProcessingException {
        final var mapper = new ObjectMapper();
        final var result = mapper.createObjectNode();

        mapDoc(document, result);

        return mapper.writeValueAsString(result);
    }

    private void mapDoc(Document document, ObjectNode result) {
        for (final var entry : document.entrySet()) {

            if (entry.getValue() instanceof String s) {
                if ("$value".equals(entry.getKey())) {
                    result.put(entry.getKey(), this.encryptionService.decrypt(s));
                }
            } else if (entry.getValue() instanceof Document d) {
                final var obj = result.putObject(entry.getKey());
                mapDoc(d, obj);
            } else if ("_id".equals(entry.getKey())) {
                result.put("$id", entry.getValue().toString());
            }
        }
    }

    private Bson from(JsonNode node) {

        return from(node, new Document());
    }

    private Bson from(JsonNode node, Document rootProjection) {
        if (node.isObject()) {

            this.stream(node.fields())
                    .forEach(e -> {
                        if (hasTokenLeaf(e.getValue())) {
                            rootProjection.append(e.getKey(), 1);
                        } else {
                            final var doc = new Document();
                            rootProjection.append(e.getKey(), doc);
                            from(e.getValue(), doc);
                        }
                    });
        }
        return rootProjection;
    }

    private boolean hasTokenLeaf(JsonNode node) {
        if (!node.isObject()) {
            return false;
        }
        return this.stream(node.fieldNames()).anyMatch("$token"::equals);
    }

    private <T> Stream<T> stream(Iterator<T> iterator) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
                false
        );
    }
}
