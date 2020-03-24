package ch.hevs.cloudio.endpoint;

/**
 * The CloudioMessageFormat interface declares the methods that are used by the {@link CloudioEndpoint} implementation in order to
 * encode and decode attribute changes into MQTT messages.
 */
public interface CloudioMessageFormat {
    /**
     * A CloudioMessageFormat implementation should return the encoded payload of the serialization of the given endpoint.
     *
     * @param endpoint  Endpoint to serialize.
     * @return          Raw data representation of the endpoint.
     */
    byte[] serializeEndpoint(CloudioEndpoint.InternalEndpoint endpoint);

    /**
     * A CloudioMessageFormat implementation should return the encoded payload of the serialization of the given node.
     *
     * @param node  Node to serialize.
     * @return      Raw data representation of the node.
     */
    byte[] serializeNode(CloudioNode.InternalNode node);

    /**
     * A CloudioMessageFormat implementation should return the encoded payload of the serialization of the given attribute.
     *
     * @param attribute Attribute to serialize.
     * @return          Raw data representation of the attribute.
     */
    byte[] serializeAttribute(CloudioAttribute.InternalAttribute attribute);

    /**
     * A CloudioMessageFormat implementation should parse the data payload and update the given attribute according to the
     * data.
     *
     * @param data                              Data received in the MQTT message.
     * @param attribute                         Attribute to update using the raw message data.
     * @throws Exception                        If the attribute can not be updated from the cloud for any reason.
     */
    void deserializeAttribute(byte[] data, CloudioAttribute.InternalAttribute attribute)
        throws Exception;
}
