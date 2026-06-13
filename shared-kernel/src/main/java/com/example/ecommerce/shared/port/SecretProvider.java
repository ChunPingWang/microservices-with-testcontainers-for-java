package com.example.ecommerce.shared.port;

import java.util.Optional;

/** Outbound port for fetching secrets. Real: Vault. Test: properties file. */
public interface SecretProvider {

    Optional<String> get(String key);

    default String require(String key) {
        return get(key).orElseThrow(
                () -> new IllegalStateException("required secret missing: " + key));
    }
}
