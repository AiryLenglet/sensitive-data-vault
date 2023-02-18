package me.lenglet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.client.MongoClient;
import me.lenglet.common.EncryptionService;
import me.lenglet.common.TokenGenerator;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@RestController
public class PatchDataController {


    private final EncryptionService encryptionService;
    private final TokenGenerator tokenGenerator;
    private final MongoClient mongoClient;

    public PatchDataController(
            EncryptionService encryptionService,
            TokenGenerator tokenGenerator,
            MongoClient mongoClient
    ) {
        this.encryptionService = encryptionService;
        this.tokenGenerator = tokenGenerator;
        this.mongoClient = mongoClient;
    }

    @PostMapping("/vaults/{vaultName}/{objectId}")
    public String execute(
            @PathVariable("vaultName") String vaultName,
            @PathVariable("objectId") String objectId,
            @RequestBody String json
    ) throws JsonProcessingException {
        final var node = new ObjectMapper().readTree(json);

        final var update = this.toDoc(node);

        mongoClient.getDatabase("admin")
                .getCollection(vaultName)
                .updateOne(
                        new Document().append("_id", new ObjectId(objectId)),
                        update
                );

        return """
                {
                }
                """;
    }

    private String toJson(Document document) throws JsonProcessingException {
        final var mapper = new ObjectMapper();
        final var result = mapper.createObjectNode();

        mapDoc(document, result);

        return mapper.writeValueAsString(result);
    }

    private static void mapDoc(Document document, ObjectNode result) {
        for (final var entry : document.entrySet()) {

            if (entry.getValue() instanceof String s) {
                result.put(entry.getKey(), s);
            } else if (entry.getValue() instanceof Document d) {
                final var obj = result.putObject(entry.getKey());
                mapDoc(d, obj);
            } else if ("_id".equals(entry.getKey())) {
                result.put("$id", entry.getValue().toString());
            }
        }
    }

    Document toDoc(JsonNode jsonNode) {
        return this.toDoc(jsonNode, new Document());
    }

    Document toDoc(JsonNode jsonNode, Document root) {
        for (JsonNode node : jsonNode) {
            
        }
        return root;
    }

    private <T> Stream<T> stream(Iterator<T> iterator) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
                false
        );
    }

    private String extractStringValue(JsonNode jsonNode, JsonNode valueNode) {
        return switch (valueNode.getNodeType()) {
            case BOOLEAN -> this.convertBoolean(valueNode);
            case NUMBER -> this.convertNumber(valueNode);
            case STRING -> valueNode.textValue();
            default -> throw new IllegalArgumentException(valueNode.getNodeType() + " node type not handled");
        };
    }

    private String convertBoolean(JsonNode jsonNode) {
        return jsonNode.booleanValue() ? "true" : "false";
    }

    String convertNumber(JsonNode jsonNode) {
        //TODO: update
        return jsonNode.numberValue().toString();
    }

}
