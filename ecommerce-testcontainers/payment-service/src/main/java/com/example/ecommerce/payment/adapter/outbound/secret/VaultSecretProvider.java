package com.example.ecommerce.payment.adapter.outbound.secret;

import com.example.ecommerce.shared.port.SecretProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.vault.core.VaultKeyValueOperationsSupport.KeyValueBackend;
import org.springframework.vault.core.VaultTemplate;

import java.util.Optional;

/**
 * Reads secrets from a Vault KV v2 mount called "secret".
 * Each call hits Vault — for production you would layer a short-TTL cache.
 */
@Component
@Primary
@ConditionalOnBean(VaultTemplate.class)
public class VaultSecretProvider implements SecretProvider {

    private static final String MOUNT = "secret";

    private final VaultTemplate vault;

    public VaultSecretProvider(VaultTemplate vault) {
        this.vault = vault;
    }

    @Override
    public Optional<String> get(String key) {
        var response = vault.opsForKeyValue(MOUNT, KeyValueBackend.KV_2).get(key);
        if (response == null || response.getData() == null) {
            return Optional.empty();
        }
        Object value = response.getData().get("value");
        return value == null ? Optional.empty() : Optional.of(value.toString());
    }
}
