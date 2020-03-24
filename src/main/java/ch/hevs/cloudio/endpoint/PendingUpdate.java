package ch.hevs.cloudio.endpoint;

import org.eclipse.paho.client.mqttv3.MqttPersistable;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;

/**
 * Persistable object which saves an actual update message in the case the MQTT client is not already connected to the
 * broker. These messages are send when the connection to the broker could be established.
 */
public class PendingUpdate implements MqttPersistable {
    /**
     * Create the persistable object from the given data.
     *
     * @param data  Data of the message.
     */
    public PendingUpdate(final byte[] data) {
        this.data = data;
    }

    @Override
    public byte[] getHeaderBytes() throws MqttPersistenceException {
        return data;
    }

    @Override
    public int getHeaderLength() throws MqttPersistenceException {
        return data.length;
    }

    @Override
    public int getHeaderOffset() throws MqttPersistenceException {
        return 0;
    }

    @Override
    public byte[] getPayloadBytes() throws MqttPersistenceException {
        return null;
    }

    @Override
    public int getPayloadLength() throws MqttPersistenceException {
        return 0;
    }

    @Override
    public int getPayloadOffset() throws MqttPersistenceException {
        return 0;
    }

    private final byte[] data;
}
