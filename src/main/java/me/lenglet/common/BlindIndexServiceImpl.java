package me.lenglet.common;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
class BlindIndexServiceImpl implements BlindIndexService {

    public static final int MOD = 3;
    private final HashService hashService;

    public BlindIndexServiceImpl(
            HashService hashService
    ) {
        this.hashService = hashService;
    }

    @Override
    public Set<String> computeBlindIndex(String plaintext) {
        final var modifiedValue = plaintext.toLowerCase()
                .replace(" ", "");

        final var nbOfSearchableTerm = modifiedValue.length() / MOD;
        final var searchableTerms = IntStream.rangeClosed(1, nbOfSearchableTerm)
                .map(i -> i * 3)
                .mapToObj(i -> modifiedValue.substring(0, i))
                .collect(Collectors.toSet());
        searchableTerms.add(modifiedValue);

        return searchableTerms.stream()
                .map(this.hashService::hash)
                .collect(Collectors.toSet());
    }

    @Override
    public String computeTerm(String plaintext) {
        final var modifiedValue =  plaintext.toLowerCase().replace(" ", "");
        return this.hashService.hash(modifiedValue.substring(0, modifiedValue.length() - (modifiedValue.length() % MOD)));
    }
}
