package com.example.ecommerce.payment.adapter.secret;

import com.example.ecommerce.payment.adapter.outbound.secret.VaultSecretProvider;
import com.example.ecommerce.test.podman.PodmanCompatibility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultKeyValueOperationsSupport.KeyValueBackend;
import org.springframework.vault.core.VaultTemplate;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.vault.VaultContainer;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class VaultSecretProviderIT {

    static final String ROOT_TOKEN = "test-root-token";

    @SuppressWarnings("resource")
    static VaultContainer<?> vault;
    static VaultTemplate template;
    static VaultSecretProvider provider;

    @BeforeAll
    static void startup() {
        PodmanCompatibility.apply();
        vault = new VaultContainer<>(DockerImageName.parse("hashicorp/vault:1.16"))
                .withVaultToken(ROOT_TOKEN);
        vault.start();

        var endpoint = VaultEndpoint.create(vault.getHost(), vault.getMappedPort(8200));
        endpoint.setScheme("http");
        template = new VaultTemplate(endpoint, new TokenAuthentication(ROOT_TOKEN));

        // Seed a secret at secret/payment.api-key
        template.opsForKeyValue("secret", KeyValueBackend.KV_2)
                .put("payment.api-key", Map.of("value", "vault-stored-api-key"));

        provider = new VaultSecretProvider(template);
    }

    @AfterAll
    static void shutdown() {
        if (vault != null) vault.stop();
    }

    @Test
    void resolves_seeded_secret() {
        assertThat(provider.get("payment.api-key"))
                .contains("vault-stored-api-key");
    }

    @Test
    void missing_secret_returns_empty() {
        assertThat(provider.get("not-a-real-key")).isEmpty();
    }

    @Test
    void require_throws_for_missing() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> provider.require("nope"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("nope");
    }
}
