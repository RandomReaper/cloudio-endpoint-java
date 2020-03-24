package ch.hevs.cloudio.endpoint.configuration;

import java.util.Properties;

/**
 * Implements the {@link CloudioEndpointConfiguration} interface using the Java properties {@link Properties} as configuration
 * source.
 */
public class PropertiesEndpointConfiguration implements CloudioEndpointConfiguration {
    private final Properties properties;

    /**
     * Creates a new object implementing the {@link CloudioEndpointConfiguration} interface by using the given Java properties
     * object.
     *
     * @param properties    Java properties object to search the configuration values from.
     */
    public PropertiesEndpointConfiguration(Properties properties) {
        this.properties = properties;
    }

    @Override
    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    @Override
    public boolean containsKey(String key) {
        return properties.containsKey(key);
    }
}
