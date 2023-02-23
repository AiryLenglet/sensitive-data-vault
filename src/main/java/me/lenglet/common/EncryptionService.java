package me.lenglet.common;

public interface EncryptionService {
    String encrypt(String plaintext);

    String decrypt(String ciphertext);
}
