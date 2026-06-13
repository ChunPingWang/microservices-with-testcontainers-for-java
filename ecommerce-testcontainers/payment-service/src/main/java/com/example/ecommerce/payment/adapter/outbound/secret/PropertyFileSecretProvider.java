package com.example.ecommerce.payment.adapter.outbound.secret;

import com.example.ecommerce.shared.port.SecretProvider;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Default SecretProvider used when Vault is not on the classpath / disabled.
 * Reads from Spring's {@link Environment}, which transparently picks up
 * application properties, env vars, and command-line overrides.
 *
 * <p>{@link VaultSecretProvider} declares itself {@code @Primary} when
 * activated so it wins over this one in Vault-enabled deployments; here
 * we simply always register, sidestepping the brittle
 * {@code @ConditionalOnMissingBean} ordering.
 */
@Component
public class PropertyFileSecretProvider implements SecretProvider {

    private final Environment env;

    public PropertyFileSecretProvider(Environment env) {
        this.env = env;
    }

    @Override
    public Optional<String> get(String key) {
        return Optional.ofNullable(env.getProperty(key));
    }
}
