package ch.hevs.cloudio.endpoint.configuration;

import ch.hevs.cloudio.endpoint.CloudioEndpoint;

/**
 * Interface a configuration source for a cloud.io endpoint has to implement. An object implementing this interface can
 * be passed to the constructor of cloud.io's {@link CloudioEndpoint} instance.
 */
public interface CloudioEndpointConfiguration {
    /**
     * Returns the configuration item for the given key or null if the property actually does not exists.
     *
     * @param key   Key of the configuration item.
     * @return      The string value of the property or null if no such property exists.
     */
    public String getProperty(String key);

    /**
     * Returns the configuration item for the given key or returns the given default value if the property actually
     * does not exists.
     *
     * @param key           Key of the configuration item.
     * @param defaultValue  Default value to use in the case the configuration item does not exists.
     * @return              The string value of the property or the default value if no such property exists.
     */
    public String getProperty(String key, String defaultValue);

    /**
     * Returns true if a configuration item with the given key exists, false otherwise.
     *
     * @param key   Key of the configuration item.
     * @return      True if the configuration item exists, false otherwise.
     */
    public boolean containsKey(String key);
}
