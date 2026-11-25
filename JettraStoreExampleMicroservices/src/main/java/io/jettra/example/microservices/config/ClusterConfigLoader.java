package io.jettra.example.microservices.config;

import io.jettra.driver.JettraClient;
import io.jettra.driver.config.JettraClientConfig;
import io.jettra.store.core.JettraConfigValidator;

import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

/**
 * Carga la configuración del clúster JettraStore desde {@code cluster.config}
 * y construye la conexión de {@link JettraClient} (JettraStoreDriver) hacia {@code example_factura_db}.
 */
public final class ClusterConfigLoader {

    public static final String DEFAULT_DATABASE = "example_factura_db";

    private ClusterConfigLoader() {
    }

    public static Properties loadClusterProperties() {
        Path clusterFile = JettraConfigValidator.locateClusterConfigFile();
        return JettraConfigValidator.loadProperties(clusterFile, "/cluster.config", "/jettra.config");
    }

    public static String resolveDatabaseName(Properties props) {
        String sysDb = System.getProperty("jettra.database");
        if (sysDb != null && !sysDb.isBlank()) {
            return sysDb.trim();
        }
        String cfgDb = props != null ? props.getProperty("jettra.database") : null;
        return (cfgDb != null && !cfgDb.isBlank()) ? cfgDb.trim() : DEFAULT_DATABASE;
    }

    public static JettraClientConfig buildClientConfig() {
        Properties props = loadClusterProperties();
        String db = resolveDatabaseName(props);
        String user = System.getProperty("jettra.user", props.getProperty("jettra.user", "admin"));
        String pass = System.getProperty("jettra.password", props.getProperty("jettra.password", "admin-jettra"));
        String role = System.getProperty("jettra.role", props.getProperty("jettra.role", "admin"));

        JettraClientConfig.Builder builder = JettraClientConfig.builder()
                .credentials(user, pass)
                .role(role)
                .defaultDatabase(db);

        String hostOverride = System.getProperty("jettra.host");
        Integer portOverride = Integer.getInteger("jettra.port");
        if (hostOverride != null && !hostOverride.isBlank() && portOverride != null && portOverride > 0) {
            builder.addClusterNode(hostOverride.trim(), portOverride);
            return builder.build();
        }

        List<JettraConfigValidator.ClusterNodeInfo> nodes = JettraConfigValidator.parseClusterNodes(props);
        if (nodes.isEmpty()) {
            builder.addClusterNode("127.0.0.1", 50001);
        } else {
            nodes.sort((a, b) -> Boolean.compare("PRIMARY".equalsIgnoreCase(b.role()), "PRIMARY".equalsIgnoreCase(a.role())));
            for (var n : nodes) {
                if (n.ip() != null && !n.ip().isBlank() && n.grpcPort() > 0) {
                    builder.addClusterNode(n.ip().trim(), n.grpcPort());
                }
            }
        }
        return builder.build();
    }

    public static JettraClient connect() {
        JettraClientConfig config = buildClientConfig();
        JettraClient client = JettraClient.connect(config);
        client.createDatabase(config.getDefaultDatabase());
        return client;
    }
}
