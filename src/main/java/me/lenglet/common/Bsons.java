package me.lenglet.common;

import org.bson.Document;

public class Bsons {

    public static Document objectId(String objectId) {
        return new Document("objectId", objectId);
    }
}
