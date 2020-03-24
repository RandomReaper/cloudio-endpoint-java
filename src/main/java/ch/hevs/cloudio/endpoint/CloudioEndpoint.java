package ch.hevs.cloudio.endpoint;

import ch.hevs.cloudio.endpoint.configuration.CloudioEndpointConfiguration;
import ch.hevs.cloudio.endpoint.configuration.PropertiesEndpointConfiguration;
import ch.hevs.cloudio.endpoint.transport.Transport;
import ch.hevs.cloudio.endpoint.transport.TransportException;
import ch.hevs.cloudio.endpoint.transport.TransportListener;
import ch.hevs.cloudio.endpoint.transport.TransportMqttPaho;
import ch.hevs.utils.ResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.*;

/**
 * An Endpoint is the root object of any connection of a device or a gateway to cloud.io. The parameters of the
 * Endpoint can be either passed to the constructor as simple Java Properties or they can be present in a Java
 * Properties file at the following locations (The Properties files are searched in the order of listing):
 * <br><br>
 * <ul>
 *     <li>~/.config/cloud.io/{uuidOrAppName}.properties on the local file system.</li>
 *     <li>/etc/cloud.io/{uuidOrAppName}.properties on the local file system.</li>
 *     <li>cloud.io/{uuidOrAppName}.properties inside the application bundle.</li>
 * </ul>
 * <br><br>
 * <b>Specifying resource locations:</b><br>
 * When you specify a location for any configuration file or key/certificate file or any other kind of file, you need
 * to use URIs to specify the file location. The following URI schemes are supported:
 * <ul>
 *     <li>
 *         <b>classpath:</b><br>
 *         The file is located in the classpath (Either in the JAR file or inside the same OSGi Bundle).
 *     </li>
 *     <li>
 *         <b>file:</b>
 *         The file is located on the local file system. An absolute path to the file has to be given.
 *     </li>
 *     <li>
 *         <b>home:</b>
 *         The file is located in the home directory of the actual user. The given path is relative to the user's home
 *         directory.
 *     </li>
 *     <li>
 *         <b>http:</b>
 *         The resource (file) is located on an HTTP server.
 *     </li>
 * </ul>
 * <br><br>
 * The properties configure all aspects of the communication between the endpoint and the cloud. Here the list of the
 * supported properties:
 * <br><br>
 * <ul>
 *     <li>
 *         <b>ch.hevs.cloudio.endpoint.uuid</b><br>
 *         UUID of the EndPoint, must be used when an application name is passed to the CloudioEndpoint constructor.
 *         The parameter will be used to search for a {uuidOrAppName}.properties file.
 *     </li>
 *     <li>
 *         <b>ch.hevs.cloudio.endpoint.hostUri</b><br>
 *         URI of the cloud.io broker URI to connect to. An example might be "ssl://example.org:8883". Note that two
 *         schemes of connections are supported, "tcp" for non secure connections and "ssl" for secure connections.
 *         This property is <b>mandatory.</b>
 *     </li>
 *     <li>
 *         <b>ch.hevs.cloudio.endpoint.connectTimeout</b><br>
 *         Timeout in seconds to use when trying to connect to the cloud.io message broker. The default value is <b>5
 *         seconds</b>.
 *     </li>
 *     <li>
 *         <b>ch.hevs.cloudio.endpoint.connectRetryInterval</b><br>
 *         Interval in seconds to start a reconnect try after a connect failure. The default value is <b>10 seconds</b>.
 *         If the value is 0, no connection retry has will be done at all once the connection is lost or could not
 *         be established.
 *     </li>
 *     <li>
 *         <b>ch.hevs.cloudio.endpoint.keepAliveInterval</b><br>
 *         Interval at which the client exchanges messages with the server in order to check that the connection is
 *         still alive and in order to guarantee that NAT port mappings remain. Default is <b>60 seconds</b>.
 *     </li>
 *     <li>
 *         <b>ch.hevs.cloudio.endpoint.persistence</b><br>
 *         This option configures which persistence to use in order to save temporary data. Three options are possible:
 *         <br>
 *         <ul>
 *             <li>
 *                 <b>memory</b><br>
 *                 The temporary data is saved into memory. With this option, pending messages (data) can be lost
 *                 if the application is restarted. It is not advisable to use this persistence implementation in
 *                 productive environments.
 *             </li>
 *             <li>
 *                 <b>file</b><br>
 *                 The temporary data is saved onto the file system. Using this option, pending messages (data) will
 *                 not be lost during a restart of the application if not a clean session is forced using the option
 *                 ch.hevs.cloudio.endpoint.cleanSession.
 *             </li>
 *             <li>
 *                 <b>none</b><br>
 *                 No Persistence is used at all. Using this persistence, messages (data) will be almost certain be
 *                 lost, but if your application can handle such losses and you want to use as less as resources as
 *                 possible, this might be a solution.
 *             </li>
 *         </ul>
 *         This property is optional and the default is <b>file</b>.
 *     </li>
 *     <li>
 *         <b>ch.hevs.cloudio.endpoint.ssl.clientCert</b><br>
 *         Path to the client certificate file. The certificate file must be encoded in the PKCS12 key format and it
 *         needs to contain the client's certificate and the client's private key. The file has additionally to be
 *         encrypted and protected using a password. The password to use to decrypt the file is specified using the
 *         property <b>ch.hevs.cloudio.endpoint.ssl.clientPassword</b>.<br>
 *         This property is optional and if the property is not set, the endpoint searches these locations in the
 *         given order for the JKCS12 file:
 *         <ul>
 *             <li>~/.config/cloud.io/{Endpoint UUID}.p12 on the local file system.</li>
 *             <li>/etc/cloud.io/{Endpoint UUID}.p12 on the local file system.</li>
 *             <li>cloud.io/{Endpoint UUID}.p12 inside the application bundle (classpath).</li>
 *         </ul>
 *     </li>
 *     <li>
 *         <b>ch.hevs.cloudio.endpoint.ssl.clientPassword</b><br>
 *         Password to unlock the PKCS12 archive containing the endpoint's certificate and private key. The PCKS12 file
 *         is specified using the <b>ch.hevs.cloudio.endpoint.ssl.clientCert</b> property. Note that it is not mandatory
 *         to encrypt the PKCS12 files.<br>
 *         Optional, defaults to "" which means no password at all.
 *     </li>
 *     <li>
 *         <b>ch.hevs.cloudio.endpoint.ssl.authorityCert</b><br>
 *         The path to the certificate of the certification authority embedded into a JKS file.
 *     </li>
 *     <li>
 *         <b>ch.hevs.cloudio.endpoint.ssl.authorityPassword</b><br>
 *         Password to unlock the JKS archive containing the certification authority's certificate. The JKS file
 *         is specified using the <b>ch.hevs.cloudio.endpoint.ssl.authorityCert</b> property.<br>
 *         Optional, defaults to "" which means no password at all.
 *     </li>
 *     <li>
 *         <b>ch.hevs.cloudio.endpoint.ssl.protocol</b><br>
 *         Specifies the SSL protocol to use. Possible values depend on the actual SSL implementation. Most commonly
 *         supported are:
 *         <ul>
 *             <li>SSL</li>
 *             <li>SSLv2</li>
 *             <li>SSLv3</li>
 *             <li>TLS</li>
 *             <li>TLSv1</li>
 *             <li>TLSv1.1</li>
 *             <li>TLSv1.2</li>
 *         </ul>
 *         This property is optional and defaults to TLSv1.2.
 *     </li>
 *     <li>
 *         <b>ch.hevs.cloudio.endpoint.messageFormat</b><br>
 *         The message format used to communicate within cloud.io. Currently supported data formats are:
 *         <ul>
 *             <li>json: JSON Format.</li>
 *             <li>json+zip: Compressed JSON.</li>
 *         </ul>
 *         If this property is not set, the default "json" will be used.
 *         Note that it is important that all endpoints and applications in the same cloud.io installation use the very
 *         same message format, otherwise they will not be able to communicate with each other.
 *     </li>
 *     <li>
 *         <b>ch.hevs.cloudio.endpoint.cleanSession</b><br>
 *         This property can be either "true" or "false". If it is true, a clean MQTT session is established with the
 *         central broker. This means that pending messages from a previous session will be discarded. If it is "false"
 *         which is the default, pending messages from previous sessions will be send to the server upon the connection
 *         is established.
 *     </li>
 *     <li>
 *         <b>ch.hevs.cloudio.endpoint.ssl.verifyHostname</b><br>
 *         This property can be either "true" or "false". If it is "true", the hostname of the broker will be verified
 *         the very same way as it is done by HTTPS. If "false" the host name is not verified at all. Per default the
 *         hostname is verified (true).
 *     </li>
 * </ul>
 */
public class CloudioEndpoint implements CloudioEndpointService {
    private static final Logger log = LoggerFactory.getLogger(CloudioEndpoint.class);
    final InternalEndpoint internal;

    /**
     * Constructs a new Endpoint object using the given UUID or application name. As no properties
     * are given using this constructor, the properties are loaded from the file system or
     * the actual bundle itself.
     *
     * The Java Properties file at the following locations (The Properties files are searched in the order of listing)
     * are used:
     * <ul>
     *     <li>~/.config/cloud.io/{uuidOrAppName}.properties on the local file system.</li>
     *     <li>/etc/cloud.io/{uuidOrAppName}.properties on the local file system.</li>
     *     <li>~{uuidOrAppName}.properties inside the application bundle (classpath).</li>
     * </ul>
     *
     * The endpoint will try immediately to connect to the central message broker and tries automatically to maintain
     * this connection in the background.
     *
     * @param uuidOrAppName                     Unique ID of the endpoint or an application name. In the case of an
     *                                          application name, the properties must define the UUID using the 
     *                                          ch.hevs.cloudio.endpoint.uuid property.
     * @throws InvalidUuidException             If the given UUID is invalid.
     * @throws InvalidPropertyException         Either a mandatory property is missing or a property has an invalid
     *                                          value.
     * @throws CloudioEndpointInitializationException  The endpoint could not be initialized. This might be caused by invalid
     *                                          parameters, invalid certificates or any other runtime errors.
     */
    public CloudioEndpoint(String uuidOrAppName) throws InvalidUuidException, InvalidPropertyException,
        CloudioEndpointInitializationException {
        // Call internal designated constructor with empty properties reference.
        internal = new InternalEndpoint(uuidOrAppName, null, null);
    }

    /**
     * Constructs a new CloudioEndpoint object using the given UUID and properties. The endpoint will try immediately to
     * connect to the central message broker and tries automatically to maintain this connection in the background.
     *
     * @param uuidOrAppName                     Unique ID of the endpoint or an application name. In the case of an
     *                                          application name, the properties must define the UUID using the 
     *                                          ch.hevs.cloudio.endpoint.uuid property.
     * @param properties                        Properties containing the endpoint configuration parameters.
     * @throws InvalidUuidException             If the given UUID is invalid.
     * @throws InvalidPropertyException         Either a mandatory property is missing or a property has an invalid
     *                                          value.
     * @throws CloudioEndpointInitializationException  The endpoint could not be initialized. This might be caused by invalid
     *                                          parameters, invalid certificates or any other runtime errors.
     */
    public CloudioEndpoint(String uuidOrAppName, Properties properties)
            throws InvalidUuidException, InvalidPropertyException, CloudioEndpointInitializationException {
        internal = new InternalEndpoint(uuidOrAppName, new PropertiesEndpointConfiguration(properties), null);
    }

    /**
     * Constructs a new CloudioEndpoint object using the given UUID and properties. The endpoint will try immediately to
     * connect to the central message broker and tries automatically to maintain this connection in the background.
     * The given endpoint listener receives updates about the state of the endpoint.
     *
     * @param uuidOrAppName                     Unique ID of the endpoint or an application name. In the case of an
     *                                          application name, the properties must define the UUID using the 
     *                                          ch.hevs.cloudio.endpoint.uuid property.
     * @param properties                        Properties containing the endpoint configuration parameters.
     * @param listener                          Reference to the listener receiving status updates.
     * @throws InvalidUuidException             If the given UUID is invalid.
     * @throws InvalidPropertyException         Either a mandatory property is missing or a property has an invalid
     *                                          value.
     * @throws CloudioEndpointInitializationException  The endpoint could not be initialized. This might be caused by invalid
     *                                          parameters, invalid certificates or any other runtime errors.
     */
    public CloudioEndpoint(String uuidOrAppName, Properties properties, CloudioEndpointListener listener)
            throws InvalidUuidException, InvalidPropertyException, CloudioEndpointInitializationException {
        internal = new InternalEndpoint(uuidOrAppName, new PropertiesEndpointConfiguration(properties), listener);
    }

    /**
     * Designated constructor. Uses the {@link CloudioEndpointConfiguration} interface to read the endpoint configuration
     * options.
     *
     * @param uuidOrAppName                     Unique ID of the endpoint or an application name. In the case of an
     *                                          application name, the properties must define the UUID using the 
     *                                          ch.hevs.cloudio.endpoint.uuid property.
     * @param configuration                     Configuration object containing the endpoint configuration parameters.
     * @throws InvalidUuidException             If the given UUID is invalid.
     * @throws InvalidPropertyException         Either a mandatory property is missing or a property has an invalid
     *                                          value.
     * @throws CloudioEndpointInitializationException  The endpoint could not be initialized. This might be caused by invalid
     *                                          parameters, invalid certificates or any other runtime errors.
     */
    CloudioEndpoint(String uuidOrAppName, CloudioEndpointConfiguration configuration) throws InvalidUuidException, InvalidPropertyException,
        CloudioEndpointInitializationException {
        internal = new InternalEndpoint(uuidOrAppName, configuration, null);
    }

    public void close() {
        internal.close();
    }

    @Override
    public boolean isOnline() {
        return internal.transport.isConnected();
    }

    @Override
    public void addEndpointListener(CloudioEndpointListener listener) {
        if (listener != null) {
            internal.listeners.add(listener);
        }
     }

    @Override
    public void removeEndpointListener(CloudioEndpointListener listener) {
        internal.listeners.remove(listener);
    }

    @Override
    public void addNode(String nodeName, CloudioNode node) throws DuplicateNamedItemException {
        if (nodeName != null && node != null) {
            // Add node to endpoint.
            node.internal.setName(nodeName);
            node.internal.setParentNodeContainer(this.internal);
            internal.nodes.addItem(node.internal);

            // If the endpoint is online, send node add message.
            if (isOnline()) {
                try {
                    byte[] data = internal.messageFormat.serializeNode(node.internal);
                    internal.transport.publish("@nodeAdded/" + node.internal.getUuid(), data, 1, false);
                } catch (TransportException exception) {
                    log.error("Exception: " + exception.getMessage());
                    exception.printStackTrace();
                }
            }
        }
    }

    @Override
    public <T extends CloudioNode> T addNode(String nodeName, Class<T> nodeClass)
            throws InvalidCloudioNodeException, DuplicateNamedItemException {
        if (nodeName != null && nodeClass != null) {
            try {
                // Create node instance.
                T node = nodeClass.newInstance();

                // Add the node.
                addNode(nodeName, node);

                // Return reference to the new node instance.
                return node;
            } catch (InstantiationException exception) {
                throw new InvalidCloudioNodeException();
            } catch (IllegalAccessException exception) {
                throw new InvalidCloudioNodeException();
            }
        }

        return null;
    }

    @Override
    public void removeNode(CloudioNode node) {
        if (node != null && internal.nodes.contains(node.internal)) {
            // Remove parent from node.
            node.internal.setParentNodeContainer(null);

            // If the endpoint is online, send the node remove message.
            if (isOnline()) {
                try {
                    internal.transport.publish("@nodeRemoved/" + node.internal.getUuid(), null, 1, false);
                } catch (TransportException exception) {
                    log.error("Exception: " + exception.getMessage());
                    exception.printStackTrace();
                }
            }

            // Remove the node from the child nodes list.
            internal.nodes.removeItem(node.internal);
        }
    }

    @Override
    public void removeNode(String nodeName) {
        if (nodeName != null) {
            CloudioNode.InternalNode internalNode = internal.nodes.getItem(nodeName);
            if (internalNode != null) {
                // Remove parent from node.
                internalNode.setParentNodeContainer(null);

                // If the endpoint is online, send the node remove message.
                if (isOnline()) {
                    try {
                        internal.transport.publish("@nodeRemoved/" + internalNode.getUuid(), null, 1, false);
                    } catch (TransportException exception) {
                        log.error("Exception: " + exception.getMessage());
                        exception.printStackTrace();
                    }
                }
            }

            // Remove the node from the child nodes list.
            internal.nodes.removeItem(internalNode);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends CloudioNode> T getNode(String nodeName) throws ClassCastException {
        return (T)internal.nodes.getItem(nodeName).getExternalNode();
    }

    /*** Internal API *************************************************************************************************/
    class InternalEndpoint implements CloudioNodeContainer, TransportListener {
        /*** Constants ************************************************************************************************/
        private static final String UUID_PROPERTY	                = "ch.hevs.cloudio.endpoint.uuid";
        private static final String TRANSPORT_PROPERTY	            = "ch.hevs.cloudio.endpoint.transport";
        private static final String TRANSPORT_PROPERTY_MQTT_PAHO	= "mqtt_paho";
        private static final String TRANSPORT_PROPERTY_DEFAULT      = TRANSPORT_PROPERTY_MQTT_PAHO;
        private static final String MESSAGE_FORMAT                  = "ch.hevs.cloudio.endpoint.messageFormat";
        private static final String MESSAGE_FORMAT_DEFAULT          = "json";

        /**
         * Characters prohibited in the UUID.
         * 
         * This list of characters will prevent using separator or wildcard characters for most uses
         * (messaging, databases, filesystems, ...).
         */
        private static final String UUID_INVALID_CHARS              = "./#*+\\\r\n?\"\0',:;<>";

        /*** Attributes ***********************************************************************************************/
        private final String uuid;
        private final NamedItemSet<CloudioNode.InternalNode> nodes = new NamedItemSet<CloudioNode.InternalNode>();
        private final Transport transport;
        private final CloudioMessageFormat messageFormat;
        private final List<CloudioEndpointListener> listeners = new LinkedList<CloudioEndpointListener>();

        public InternalEndpoint(String uuidOrAppName, CloudioEndpointConfiguration configuration, CloudioEndpointListener listener)
                throws InvalidUuidException, InvalidPropertyException, CloudioEndpointInitializationException {

            // The ID has to be a valid string!
            if (uuidOrAppName == null) {
                throw new InvalidUuidException("uuidOrAppName can not be null!");
            }

            // Do we need to load the properties from a file?
            if (configuration == null) {
                Properties properties = new Properties();
                try {
                    InputStream propertiesInputStream = ResourceLoader.getResourceFromLocations(uuidOrAppName + ".properties",
                            this,
                            "home:" + "/.config/cloud.io/",
                            "file:/etc/cloud.io/",
                            "classpath:cloud.io/");
                    properties.load(propertiesInputStream);
                    configuration = new PropertiesEndpointConfiguration(properties);
                } catch (Exception exception) {
                    throw new InvalidPropertyException("CloudioEndpoint properties missing: No properties given as " +
                            "argument to constructor and no properties file found " +
                            "[\"home:/.config/cloud.io/" + uuidOrAppName + ".properties\", " +
                            "\"file:/etc/cloud.io/" + uuidOrAppName + ".properties\", " +
                            "\"classpath:" + uuidOrAppName + ".properties\"].");
                }
            }

            // Set the UUID.
            uuid = configuration.getProperty(UUID_PROPERTY, uuidOrAppName);

            // Verify the UUID will be valid
            for (char c : UUID_INVALID_CHARS.toCharArray()) {
            	if (uuid.contains(""+c)) {
            		throw new InvalidUuidException(String.format("uuid(value:'%s') contains the invalid char 'UTF+%04X'",  uuid, (int)c));
            	}
            }
            
            // Create message format instance.
            String messageFormatId = configuration.getProperty(MESSAGE_FORMAT, MESSAGE_FORMAT_DEFAULT);
            if ("json".equals(messageFormatId)) {
                messageFormat = new JsonMessageFormat();
            } else if ("json+zip".equals(messageFormatId)) {
                messageFormat = new JsonZipMessageFormat();
            } else {
                throw new InvalidPropertyException("Unknown message format (ch.hevs.cloudio.endpoint.messageFormat): " +
                        "\"" + messageFormatId + "\"");
            }

            String transport_id = configuration.getProperty(TRANSPORT_PROPERTY, TRANSPORT_PROPERTY_DEFAULT);
            if (transport_id == TRANSPORT_PROPERTY_MQTT_PAHO) {
            	transport = new TransportMqttPaho(uuid, messageFormat.serializeEndpoint(InternalEndpoint.this), configuration, this);
            }
            else {
            	throw new InvalidPropertyException("Unknown transport ("+TRANSPORT_PROPERTY+"): " +
                        "\"" + transport_id + "\", supported transports :\"" + TRANSPORT_PROPERTY_MQTT_PAHO + "\"");
            }

            // Add the listener if present.
            if (listener != null) {
                this.listeners.add(listener);
            }
            
        }
        
        /*** NodeContainer Implementation *****************************************************************************/
        @Override
        public void attributeHasChangedByEndpoint(CloudioAttribute.InternalAttribute attribute) {
            // Create the MQTT message using the given message format.
            byte[] data = messageFormat.serializeAttribute(attribute);

            // Try to send the message if the MQTT client is connected.
            boolean messageSend = false;
            if (transport.isConnected()) {
                try {
                    transport.update(attribute.getUuid().toString(), data, 1, false);

                    messageSend = true;
                } catch (TransportException exception) {
                    log.error("Exception :" + exception.getMessage());
                    exception.printStackTrace();
                }
            }
            
            // FIXME : Implement persitence independently of the transport layer
        }

        @Override
        public void attributeHasChangedByCloud(CloudioAttribute.InternalAttribute attribute) {
            attributeHasChangedByEndpoint(attribute);
        }

        /*** UniqueIdentifiable Implementation ************************************************************************/
        @Override
        public Uuid getUuid() {
            return new TopicUuid(this);
        }

        /*** NamedItem Implementation *********************************************************************************/
        @Override
        public String getName() {
            return uuid;
        }

        @Override
        public void setName(String name) {
            throw new CloudioModificationException("CloudioEndpoint name can not be changed!");
        }

        /*** Package private methods **********************************************************************************/
        public List<CloudioNode.InternalNode> getNodes() {
            return nodes.toList();
        }

        public void set(String topic, Stack<String> location, CloudioMessageFormat messageFormat, byte[] data)
            throws Exception {
            // The path to the location must be start with the actual UUID of the endpoint.
            if (!location.isEmpty() && uuid.equals(location.pop()) &&
                !location.isEmpty() && "nodes".equals(location.pop()) &&
                !location.isEmpty()) {

                // Get the node with the name according to the topic.
                CloudioNode.InternalNode node = nodes.getItem(location.peek());
                if (node != null) {
                    location.pop();

                    // Get the attribute reference.
                    CloudioAttribute.InternalAttribute attribute = node.findAttribute(location);
                    if (attribute != null) {
                        // Deserialize the message into the attribute.
                        messageFormat.deserializeAttribute(data, attribute);
                    } else {
                        log.error("Attribute at \"" + topic + "\" not found!");
                    }
                } else {
                    log.error("Node \"" + location.pop() + "\" not found!");
                }
            } else {
                log.error("Invalid topic: " + topic);
            }
        }

        void close() {
        	transport.disconnect();

            // Remove all nodes.
            for (CloudioNode.InternalNode node: nodes) {
                node.close();
            }
            nodes.clear();

        }

        @Override
        protected void finalize() throws Throwable {
            super.finalize();
            close();
        }

		@Override
		public void onConnect() {
			for (CloudioEndpointListener listener: listeners) {
	            listener.endpointIsOnline(CloudioEndpoint.this);
	        }
		}

		@Override
		public void onDisconnect() {
	        for (CloudioEndpointListener listener: listeners) {
	            listener.endpointIsOffline(CloudioEndpoint.this);
	        }
		}

		@Override
		public void onRx(String topic, Stack<String> location, CloudioMessageFormat messageFormat, byte[] data) throws Exception {
			 // Read the action tag from the topic.
            String action = location.peek();
            if ("@set".equals(action)) {
                location.pop();
                set(topic, location, messageFormat, data);
            } else {
                log.error("Method \"" + action + "\" not supported!");
            }
		}
    }
}
