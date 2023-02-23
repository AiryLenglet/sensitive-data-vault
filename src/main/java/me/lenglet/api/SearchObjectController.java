package me.lenglet.api;

import com.mongodb.client.MongoClient;
import me.lenglet.api.dto.SearchObjectRequestDto;
import me.lenglet.common.BlindIndexService;
import org.bson.Document;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@RestController
public class SearchObjectController {

    private final MongoClient mongoClient;
    private final BlindIndexService blindIndexService;

    public SearchObjectController(
            MongoClient mongoClient,
            BlindIndexService blindIndexService
    ) {
        this.mongoClient = mongoClient;
        this.blindIndexService = blindIndexService;
    }

    @PostMapping("/vaults/{vaultName}/search")
    public Stream<String> execute(
            @PathVariable("vaultName") String vaultName,
            @RequestBody SearchObjectRequestDto request
    ) {
        final var spliterator = this.mongoClient.getDatabase("admin")
                .getCollection(vaultName)
                .find(new Document("secrets.index", this.blindIndexService.computeTerm(request.term())))
                .projection(new Document("objectId", 1))
                .map(d -> d.getString("objectId"))
                .spliterator();

        return StreamSupport.stream(spliterator, false);
    }
}
