package io.jettra.server.example.client.mp;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.spi.RestClientBuilderResolver;

/**
 * Proveedor SPI de RestClientBuilderResolver para el ecosistema Jettra.
 * Resuelve instancias de JettraRestClientBuilder compatibles con Eclipse MicroProfile Rest Client 3.0.
 */
public class JettraRestClientBuilderResolver extends RestClientBuilderResolver {

    @Override
    public RestClientBuilder newBuilder() {
        return new JettraRestClientBuilder();
    }
}

