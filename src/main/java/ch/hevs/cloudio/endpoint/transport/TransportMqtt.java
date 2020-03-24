package ch.hevs.cloudio.endpoint.transport;

import ch.hevs.cloudio.endpoint.configuration.CloudioEndpointConfiguration;

public abstract class TransportMqtt extends Transport {
    protected static final String MQTT_HOST_URI_PROPERTY          = "ch.hevs.cloudio.endpoint.hostUri";
    protected static final String MQTT_CONNECTION_TIMEOUT_PROPERTY= "ch.hevs.cloudio.endpoint.connectTimeout";
    protected static final String MQTT_CONNECTION_TIMEOUT_DEFAULT = "5";
    protected static final String MQTT_CONNECT_RETRY_PROPERTY     = "ch.hevs.cloudio.endpoint.connectRetryInterval";
    protected static final String MQTT_CONNECT_RETRY_DEFAULT      = "10";
    protected static final String MQTT_KEEPALIVE_INTERVAL_PROPERTY= "ch.hevs.cloudio.endpoint.keepAliveInterval";
    protected static final String MQTT_KEEPALIVE_INTERVAL_DEFAULT = "60";
    protected static final String MQTT_MAXINFLIGHT_PROPERTY		  = "ch.hevs.cloudio.endpoint.maxInFlight";
    protected static final String MQTT_MAXINFLIGHT_DEFAULT	      = "1000";
    protected static final String MQTT_PERSISTENCE_MEMORY         = "memory";
    protected static final String MQTT_PERSISTENCE_FILE           = "file";
    protected static final String MQTT_PERSISTENCE_NONE           = "none";
    protected static final String MQTT_PERSISTENCE_PROPERTY       = "ch.hevs.cloudio.endpoint.persistence";
    protected static final String MQTT_PERSISTENCE_DEFAULT        = MQTT_PERSISTENCE_FILE;
    protected static final String ENDPOINT_IDENTITY_FILE_TYPE     = "PKCS12";
    protected static final String ENDPOINT_IDENTITY_MANAGER_TYPE  = "SunX509";
    protected static final String ENDPOINT_IDENTITY_FILE_PROPERTY = "ch.hevs.cloudio.endpoint.ssl.clientCert";
    protected static final String ENDPOINT_IDENTITY_PASS_PROPERTY = "ch.hevs.cloudio.endpoint.ssl.clientPassword";
    protected static final String ENDPOINT_IDENTITY_PASS_DEFAULT  = "";
    protected static final String CERT_AUTHORITY_FILE_TYPE        = "JKS";
    protected static final String CERT_AUTHORITY_MANAGER_TYPE     = "SunX509";
    protected static final String CERT_AUTHORITY_FILE_PROPERTY    = "ch.hevs.cloudio.endpoint.ssl.authorityCert";
    protected static final String CERT_AUTHORITY_FILE_DEFAULTNAME = "authority.jks";
    protected static final String CERT_AUTHORITY_PASS_PROPERTY    = "ch.hevs.cloudio.endpoint.ssl.authorityPassword";
    protected static final String CERT_AUTHORITY_PASS_DEFAULT     = "";
    protected static final String SSL_PROTOCOL_PROPERTY           = "ch.hevs.cloudio.endpoint.ssl.protocol";
    protected static final String SSL_PROTOCOL_DEFAULT            = "TLSv1.2";
    protected static final String MQTT_CLEAN_SESSION_PROPERTY     = "ch.hevs.cloudio.endpoint.cleanSession";
    protected static final String MQTT_CLEAN_SESSION_DEFAULT      = "false";
    protected static final String SSL_VERIFY_HOSTNAME_PROPERTY    = "ch.hevs.cloudio.endpoint.ssl.verifyHostname";
    protected static final String SSL_VERIFY_HOSTNAME_DEFAULT     = "true";

	protected TransportMqtt(String uuid, byte[] bs, CloudioEndpointConfiguration c, TransportListener l) {
		super(uuid, bs, c, l);
	}

}
