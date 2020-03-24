package ch.hevs.cloudio.endpoint;

import java.io.IOException;

/**
 * This exception is thrown whenever the initialisation of an endpoint caused an exception. The message contains the
 * actual cause of the exception or the exception thrown internally that prevented the endpoint from a proper
 * initialization.
 */
public class CloudioEndpointInitializationException extends IOException {
    public CloudioEndpointInitializationException(final String message) {
        super(message);
    }

    public CloudioEndpointInitializationException(final Exception cause) {
        super(cause);
    }
}
