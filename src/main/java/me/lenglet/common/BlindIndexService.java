package me.lenglet.common;

import java.util.Set;

public interface BlindIndexService {

    Set<String> computeBlindIndex(String plaintext);

    String computeTerm(String plaintext);
}
