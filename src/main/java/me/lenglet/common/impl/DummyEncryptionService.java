package me.lenglet.common.impl;

import me.lenglet.common.EncryptionService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
class DummyEncryptionService implements EncryptionService {

    @Override
    public String encrypt(String plaintext) {
        return Base64.getEncoder().encodeToString(plaintext.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String decrypt(String ciphertext) {
        return new String(Base64.getDecoder().decode(ciphertext), StandardCharsets.UTF_8);
    }
}
