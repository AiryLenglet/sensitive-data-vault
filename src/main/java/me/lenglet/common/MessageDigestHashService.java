package me.lenglet.common;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
class MessageDigestHashService implements HashService {

    private final MessageDigest messageDigest;

    MessageDigestHashService(
            MessageDigest messageDigest
    ) {
        this.messageDigest = messageDigest;
    }

    @Override
    public String hash(String value) {
        final var digest = this.messageDigest.digest(value.getBytes(StandardCharsets.UTF_8));
        return new String(digest, StandardCharsets.UTF_8);
    }
}
