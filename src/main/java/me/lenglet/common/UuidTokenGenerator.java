package me.lenglet.common;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
class UuidTokenGenerator implements TokenGenerator {
    @Override
    public String generate() {
        return UUID.randomUUID().toString();
    }
}
