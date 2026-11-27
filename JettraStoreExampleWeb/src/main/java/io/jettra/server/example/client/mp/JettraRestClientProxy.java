package io.jettra.server.example.client.mp;

import io.jettra.json.JettraJson;
import io.jettra.server.example.client.JettraJwtAuthManager;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Proxy dinámico para interfaces Eclipse MicroProfile Rest Client 3.0.
 * Procesa anotaciones de ruta, parámetros, serialización/deserialización JSON
 * e inyección automática del token Jettra JWT Bearer.
 */
public class JettraRestClientProxy implements InvocationHandler {

    private final HttpClient httpClient;
    private final String baseUri;
    private final long timeoutMillis;
    private final JettraJson json = new JettraJson();

    public JettraRestClientProxy(HttpClient httpClient, String baseUri, long timeoutMillis) {
        this.httpClient = httpClient != null ? httpClient : HttpClient.newHttpClient();
        this.baseUri = (baseUri != null && baseUri.endsWith("/")) ? baseUri.substring(0, baseUri.length() - 1) : (baseUri != null ? baseUri : "http://localhost:8081");
        this.timeoutMillis = timeoutMillis > 0 ? timeoutMillis : 5000;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(this, args);
        }

        // 1. Construir la ruta completa del endpoint
        String fullPath = baseUri;
        if (method.getDeclaringClass().isAnnotationPresent(Path.class)) {
            String classPath = method.getDeclaringClass().getAnnotation(Path.class).value();
            if (!classPath.startsWith("/")) classPath = "/" + classPath;
            fullPath += classPath;
        }
        if (method.isAnnotationPresent(Path.class)) {
            String methodPath = method.getAnnotation(Path.class).value();
            if (!methodPath.startsWith("/")) methodPath = "/" + methodPath;
            fullPath += methodPath;
        }

        // 2. Determinar método HTTP
        String httpMethod = "GET";
        if (method.isAnnotationPresent(POST.class)) {
            httpMethod = "POST";
        } else if (method.isAnnotationPresent(PUT.class)) {
            httpMethod = "PUT";
        } else if (method.isAnnotationPresent(DELETE.class)) {
            httpMethod = "DELETE";
        }

        // 3. Extraer y procesar parámetros
        Parameter[] parameters = method.getParameters();
        Map<String, String> queryParams = new HashMap<>();
        Map<String, String> headerParams = new HashMap<>();
        Object bodyEntity = null;
        boolean authHeaderExplicit = false;

        if (args != null) {
            for (int i = 0; i < parameters.length; i++) {
                Parameter param = parameters[i];
                Object val = args[i];

                if (param.isAnnotationPresent(PathParam.class)) {
                    String name = param.getAnnotation(PathParam.class).value();
                    String encoded = val != null ? URLEncoder.encode(val.toString(), StandardCharsets.UTF_8) : "";
                    fullPath = fullPath.replace("{" + name + "}", encoded);
                } else if (param.isAnnotationPresent(QueryParam.class)) {
                    if (val != null) {
                        queryParams.put(param.getAnnotation(QueryParam.class).value(), val.toString());
                    }
                } else if (param.isAnnotationPresent(HeaderParam.class)) {
                    String hName = param.getAnnotation(HeaderParam.class).value();
                    if (val != null) {
                        headerParams.put(hName, val.toString());
                        if (hName.equalsIgnoreCase("Authorization")) {
                            authHeaderExplicit = true;
                        }
                    }
                } else {
                    bodyEntity = val;
                }
            }
        }

        // 4. Inyección automática de autenticación Jettra JWT Bearer si no se proveyó
        if (!authHeaderExplicit || !headerParams.containsKey("Authorization")) {
            String jwtToken = JettraJwtAuthManager.getInstance().getBearerToken();
            if (jwtToken != null && !jwtToken.isBlank()) {
                headerParams.put("Authorization", jwtToken);
            }
        }

        // 5. Query parameters
        if (!queryParams.isEmpty()) {
            StringBuilder sb = new StringBuilder("?");
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                if (sb.length() > 1) sb.append("&");
                sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                  .append("=")
                  .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            }
            fullPath += sb.toString();
        }

        // 6. Preparar petición HTTP
        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(fullPath))
                .timeout(Duration.ofMillis(timeoutMillis));

        headerParams.putIfAbsent("Accept", "application/json");

        HttpRequest.BodyPublisher bodyPublisher;
        if (bodyEntity != null) {
            String jsonPayload = json.toJson(bodyEntity);
            bodyPublisher = HttpRequest.BodyPublishers.ofString(jsonPayload);
            headerParams.putIfAbsent("Content-Type", "application/json");
        } else {
            bodyPublisher = HttpRequest.BodyPublishers.noBody();
        }

        for (Map.Entry<String, String> h : headerParams.entrySet()) {
            reqBuilder.header(h.getKey(), h.getValue());
        }
        reqBuilder.method(httpMethod, bodyPublisher);

        // 7. Ejecución con HttpClient de Java 25
        HttpResponse<String> response;
        try {
            response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new WebApplicationException("Error de comunicación HTTP con endpoint " + fullPath + ": " + e.getMessage(), e, 503);
        }

        int status = response.statusCode();
        if (status >= 400) {
            Response.Status resStatus = Response.Status.fromStatusCode(status);
            throw new WebApplicationException("HTTP " + status + " desde " + fullPath + ": " + response.body(), status);
        }

        Class<?> returnType = method.getReturnType();
        if (returnType == void.class || returnType == Void.class) {
            return null;
        }

        String body = response.body();
        if (body == null || body.isBlank()) {
            return null;
        }

        Type genericType = method.getGenericReturnType();
        try {
            return json.fromJson(body, genericType);
        } catch (Exception e) {
            return json.fromJson(body, returnType);
        }
    }
}

