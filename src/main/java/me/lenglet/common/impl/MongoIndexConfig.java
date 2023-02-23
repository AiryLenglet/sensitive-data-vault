package me.lenglet.common.impl;

import com.mongodb.client.MongoClient;
import com.mongodb.client.model.IndexOptions;
import org.bson.Document;

public class MongoIndexConfig {

    public void config(MongoClient mongoClient, String vaultName) {
        mongoClient.getDatabase("admin")
                .getCollection(vaultName)
                .createIndex(
                        new Document()
                                .append("objectId", 1),
                        new IndexOptions()
                                .unique(true)
                                .name("ux_objectId_" + vaultName));
        mongoClient.getDatabase("admin")
                .getCollection(vaultName)
                .createIndex(
                        new Document()
                                .append("secrets.index", 1),
                        new IndexOptions()
                                .name("ix_index_" + vaultName));
    }
}
