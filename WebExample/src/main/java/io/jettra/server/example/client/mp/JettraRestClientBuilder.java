package io.jettra.server.example.client.mp;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.RestClientDefinitionException;
import org.eclipse.microprofile.rest.client.ext.QueryParamStyle;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import jakarta.ws.rs.core.Configuration;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.security.KeyStore;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Implementación de RestClientBuilder bajo la especificación Eclipse MicroProfile Rest Client.
 * Construye proxies dinámicos optimizados con Java 25 HttpClient y soporte para Project Loom.
 */
public class JettraRestClientBuilder implements RestClientBuilder {

    private URI baseUri;
    private long connectTimeoutMillis = 5000;
    private long readTimeoutMillis = 5000;
    private ExecutorService executorService;
    private SSLContext sslContext;
    private final Map<String, Object> properties = new HashMap<>();

    @Override
    public RestClientBuilder baseUrl(URL url) {
        try {
            this.baseUri = url != null ? url.toURI() : null;
        } catch (Exception e) {
            throw new IllegalArgumentException("URL inválida para RestClientBuilder: " + url, e);
        }
        return this;
    }

    @Override
    public RestClientBuilder baseUri(URI uri) {
        this.baseUri = uri;
        return this;
    }

    @Override
    public RestClientBuilder connectTimeout(long timeout, TimeUnit unit) {
        this.connectTimeoutMillis = unit.toMillis(timeout);
        return this;
    }

    @Override
    public RestClientBuilder readTimeout(long timeout, TimeUnit unit) {
        this.readTimeoutMillis = unit.toMillis(timeout);
        return this;
    }

    @Override
    public RestClientBuilder executorService(ExecutorService executor) {
        this.executorService = executor;
        return this;
    }

    @Override
    public RestClientBuilder sslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
        return this;
    }

    @Override
    public RestClientBuilder trustStore(KeyStore trustStore) {
        return this;
    }

    @Override
    public RestClientBuilder keyStore(KeyStore keyStore, String keystorePassword) {
        return this;
    }

    @Override
    public RestClientBuilder hostnameVerifier(HostnameVerifier verifier) {
        return this;
    }

    @Override
    public RestClientBuilder followRedirects(boolean follow) {
        return this;
    }

    @Override
    public RestClientBuilder proxyAddress(String proxyHost, int proxyPort) {
        return this;
    }

    @Override
    public RestClientBuilder queryParamStyle(QueryParamStyle style) {
        return this;
    }

    @Override
    public Configuration getConfiguration() {
        return null;
    }

    @Override
    public RestClientBuilder property(String name, Object value) {
        if (name != null) properties.put(name, value);
        return this;
    }

    @Override
    public RestClientBuilder register(Class<?> componentClass) {
        return this;
    }

    @Override
    public RestClientBuilder register(Class<?> componentClass, int priority) {
        return this;
    }

    @Override
    public RestClientBuilder register(Class<?> componentClass, Class<?>... contracts) {
        return this;
    }

    @Override
    public RestClientBuilder register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
        return this;
    }

    @Override
    public RestClientBuilder register(Object component) {
        return this;
    }

    @Override
    public RestClientBuilder register(Object component, int priority) {
        return this;
    }

    @Override
    public RestClientBuilder register(Object component, Class<?>... contracts) {
        return this;
    }

    @Override
    public RestClientBuilder register(Object component, Map<Class<?>, Integer> contracts) {
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T build(Class<T> aClass) throws IllegalStateException, RestClientDefinitionException {
        if (aClass == null || !aClass.isInterface()) {
            throw new RestClientDefinitionException("La clase del cliente debe ser una interfaz: " + aClass);
        }

        String effectiveUri = resolveEffectiveUri(aClass);
        if (effectiveUri == null || effectiveUri.isBlank()) {
            effectiveUri = "http://localhost:8081";
        }

        HttpClient.Builder httpBuilder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMillis));

        if (executorService != null) {
            httpBuilder.executor(executorService);
        }
        if (sslContext != null) {
            httpBuilder.sslContext(sslContext);
        }

        HttpClient client = httpBuilder.build();
        JettraRestClientProxy handler = new JettraRestClientProxy(client, effectiveUri, readTimeoutMillis);

        return (T) Proxy.newProxyInstance(
                aClass.getClassLoader(),
                new Class<?>[]{aClass},
                handler
        );
    }

    private String resolveEffectiveUri(Class<?> clientInterface) {
        if (this.baseUri != null) {
            return this.baseUri.toString();
        }

        RegisterRestClient ann = clientInterface.getAnnotation(RegisterRestClient.class);
        String configKey = ann != null ? ann.configKey() : "";

        // 1. Resolver por MicroProfile Config estándar: <fqcn>/mp-rest/url o <configKey>/mp-rest/url
        String[] configCandidates = {
                clientInterface.getName() + "/mp-rest/url",
                clientInterface.getName() + "/mp-rest/uri",
                configKey.isEmpty() ? null : configKey + "/mp-rest/url",
                configKey.isEmpty() ? null : configKey + "/mp-rest/uri",
                "backend.api.url"
        };

        for (String key : configCandidates) {
            if (key == null) continue;
            try {
                var val = ConfigProvider.getConfig().getOptionalValue(key, String.class);
                if (val.isPresent() && !val.get().isBlank()) {
                    return val.get().trim();
                }
            } catch (Exception ignored) {}

            String sysProp = System.getProperty(key);
            if (sysProp != null && !sysProp.isBlank()) {
                return sysProp.trim();
            }
        }

        // 2. Anotación baseUri explícita en @RegisterRestClient
        if (ann != null && !ann.baseUri().isBlank()) {
            return ann.baseUri().trim();
        }

        return "http://localhost:8081";
    }
}

