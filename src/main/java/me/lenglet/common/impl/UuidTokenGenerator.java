package me.lenglet.common.impl;

import me.lenglet.common.TokenGenerator;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
class UuidTokenGenerator implements TokenGenerator {
    @Override
    public String generate() {
        return UUID.randomUUID().toString();
    }
}
