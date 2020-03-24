package ch.hevs.cloudio.endpoint.transport;

import java.security.KeyStore;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Stack;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistable;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

import ch.hevs.cloudio.endpoint.CloudioEndpointInitializationException;
import ch.hevs.cloudio.endpoint.CloudioMessageFormat;
import ch.hevs.cloudio.endpoint.CloudioMessageFormatFactory;
import ch.hevs.cloudio.endpoint.InvalidPropertyException;
import ch.hevs.cloudio.endpoint.PendingUpdate;
import ch.hevs.cloudio.endpoint.configuration.CloudioEndpointConfiguration;
import ch.hevs.utils.ResourceLoader;

public class TransportMqttPaho extends TransportMqtt implements MqttCallback, Runnable {
	private final MqttAsyncClient mqtt;
	private final MqttConnectOptions options;
	private final MqttClientPersistence persistence;
	private int retryInterval;
	private boolean isConnected = false;

	/***
	 * Private methods
	 ******************************************************************************************/
	private SSLSocketFactory createSocketFactory(String endpointUuid, CloudioEndpointConfiguration properties)
			throws Exception {
		// Endpoint identity (Key & Certificate) in single PKCS #12 archive file named
		// with the actual Endpoint ID.
		KeyStore endpointKeyCertStore = KeyStore.getInstance(ENDPOINT_IDENTITY_FILE_TYPE);

		// If the key file is present in settings, use it to load the identity file.
		if (properties.containsKey(ENDPOINT_IDENTITY_FILE_PROPERTY)) {
			endpointKeyCertStore.load(
					ResourceLoader.getResource(properties.getProperty(ENDPOINT_IDENTITY_FILE_PROPERTY), this),
					properties.getProperty(ENDPOINT_IDENTITY_PASS_PROPERTY, ENDPOINT_IDENTITY_PASS_DEFAULT)
							.toCharArray());

			// If the key file is not given, try to load from default locations.
		} else {
			endpointKeyCertStore.load(
					ResourceLoader.getResourceFromLocations(endpointUuid + ".p12", this, "home:" + "/.config/cloud.io/",
							"file:/etc/cloud.io/", "classpath:cloud.io/"),
					properties.getProperty(ENDPOINT_IDENTITY_PASS_PROPERTY, ENDPOINT_IDENTITY_PASS_DEFAULT)
							.toCharArray());
		}

		KeyManagerFactory endpointKeyCertManagerFactory = KeyManagerFactory.getInstance(ENDPOINT_IDENTITY_MANAGER_TYPE);
		endpointKeyCertManagerFactory.init(endpointKeyCertStore, "".toCharArray());

		// Authority certificate in JKS format.
		KeyStore authorityKeyStore = KeyStore.getInstance(CERT_AUTHORITY_FILE_TYPE);

		if (properties.containsKey(CERT_AUTHORITY_FILE_PROPERTY)) {
			authorityKeyStore.load(
					ResourceLoader.getResource(properties.getProperty(CERT_AUTHORITY_FILE_PROPERTY), this),
					properties.getProperty(CERT_AUTHORITY_PASS_PROPERTY, CERT_AUTHORITY_PASS_DEFAULT).toCharArray());
		} else {
			authorityKeyStore.load(
					ResourceLoader.getResourceFromLocations(CERT_AUTHORITY_FILE_DEFAULTNAME, this,
							"home:" + "/.config/cloud.io/", "file:/etc/cloud.io/", "classpath:cloud.io/"),
					properties.getProperty(CERT_AUTHORITY_PASS_PROPERTY, CERT_AUTHORITY_PASS_DEFAULT).toCharArray());
		}

		TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(CERT_AUTHORITY_MANAGER_TYPE);
		trustManagerFactory.init(authorityKeyStore);

		// Create SSL Context.
		SSLContext sslContext = SSLContext
				.getInstance(properties.getProperty(SSL_PROTOCOL_PROPERTY, SSL_PROTOCOL_DEFAULT));
		sslContext.init(endpointKeyCertManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

		return sslContext.getSocketFactory();
	}

	public TransportMqttPaho(String uuid, byte[] bs, CloudioEndpointConfiguration c, TransportListener l)
			throws CloudioEndpointInitializationException, InvalidPropertyException {
		super(uuid, bs, c, l);

		// Create a SSL based MQTT option object.
		options = new MqttConnectOptions();
		try {
			options.setSocketFactory(createSocketFactory(uuid, configuration));
		} catch (Exception exception) {
			throw new CloudioEndpointInitializationException(exception);
		}

		// Get retry interval.
		try {
			retryInterval = Integer
					.parseInt(configuration.getProperty(MQTT_CONNECT_RETRY_PROPERTY, MQTT_CONNECT_RETRY_DEFAULT));
			if (retryInterval <= 0) {
				throw new InvalidPropertyException("Invalid connect retry interval "
						+ "(ch.hevs.cloudio.endpoint.connectRetryInterval), " + "must be a greater than 0");
			}
		} catch (NumberFormatException exception) {
			throw new InvalidPropertyException("Invalid connect retry interval "
					+ "(ch.hevs.cloudio.endpoint.connectRetryInterval), " + "must be a valid integer number");
		}

		// Do we start a clean session?
		String cleanSession = configuration.getProperty(MQTT_CLEAN_SESSION_PROPERTY, MQTT_CLEAN_SESSION_DEFAULT)
				.toLowerCase();
		if ("true".equals(cleanSession)) {
			options.setCleanSession(true);
		} else if ("false".equals(cleanSession)) {
			options.setCleanSession(false);
		} else {
			throw new InvalidPropertyException("Clean session parameter (ch.hevs.cloudio.endpoint.cleanSession), "
					+ "must either be \"true\" or \"false\"");
		}

		// Get the connection timeout property.
		try {
			options.setConnectionTimeout(Integer.parseInt(
					configuration.getProperty(MQTT_CONNECTION_TIMEOUT_PROPERTY, MQTT_CONNECTION_TIMEOUT_DEFAULT)));
		} catch (NumberFormatException e) {
			throw new InvalidPropertyException("Invalid connect timeout "
					+ "(ch.hevs.cloudio.endpoint.connectTimeout), " + "must be a valid integer number");
		}

		// Get the keep alive interval property.
		try {
			options.setKeepAliveInterval(Integer.parseInt(
					configuration.getProperty(MQTT_KEEPALIVE_INTERVAL_PROPERTY, MQTT_KEEPALIVE_INTERVAL_DEFAULT)));
		} catch (NumberFormatException exception) {
			throw new InvalidPropertyException("Invalid keep alive interval "
					+ "(ch.hevs.cloudio.endpoint.keepAliveInterval), " + "must be a valid integer number");
		}

		// Get the maxInFlight property.
		try {
			options.setMaxInflight(
					Integer.parseInt(configuration.getProperty(MQTT_MAXINFLIGHT_PROPERTY, MQTT_MAXINFLIGHT_DEFAULT)));
		} catch (NumberFormatException exception) {
			throw new InvalidPropertyException("Invalid max in flight messages"
					+ "(ch.hevs.cloudio.endpoint.maxInFlight), " + "must be a valid integer number");
		}

		// Create persistence object.
		String persistenceProvider = configuration.getProperty(MQTT_PERSISTENCE_PROPERTY, MQTT_PERSISTENCE_DEFAULT);
		if (persistenceProvider.equals(MQTT_PERSISTENCE_MEMORY)) {
			persistence = new MemoryPersistence();
		} else if (persistenceProvider.equals(MQTT_PERSISTENCE_FILE)) {
			persistence = new MqttDefaultFilePersistence();
		} else if (persistenceProvider.equals(MQTT_PERSISTENCE_NONE)) {
			persistence = null;
		} else {
			throw new InvalidPropertyException("Unknown persistence implementation "
					+ "(ch.hevs.cloudio.endpoint.persistence): " + "\"" + persistenceProvider + "\"");
		}

		// Last will is a message with the UUID of the endpoint and no payload.
		options.setWill("@offline/" + uuid, new byte[0], 1, false);

		// Create the MQTT client.
		try {
			String host = configuration.getProperty(MQTT_HOST_URI_PROPERTY);
			if (host == null) {
				throw new InvalidPropertyException("Missing mandatory property \"" + MQTT_HOST_URI_PROPERTY + "\"");
			}
			mqtt = new MqttAsyncClient(configuration.getProperty(MQTT_HOST_URI_PROPERTY), uuid, persistence);
		} catch (MqttException exception) {
			throw new CloudioEndpointInitializationException(exception);
		}

		// Enable or disable HTTPS hostname verification.
		options.setHttpsHostnameVerificationEnabled(
				"true".equals(configuration.getProperty(SSL_VERIFY_HOSTNAME_PROPERTY, SSL_VERIFY_HOSTNAME_DEFAULT)));
	}

	@Override
	public void update(String string, byte[] data, int i, boolean b) throws TransportException {
		boolean messageSend = false;
		if (isConnected()) {
			try {
				publish("@update/" + string, data, 1, false);

				messageSend = true;
			} catch (TransportException exception) {
				log.error("Exception :" + exception.getMessage());
				exception.printStackTrace();
			}
		}
		// If the message could not be send for any reason, add the message to the
		// pending updates persistence if
		// available.
		if (!messageSend && persistence != null) {
			try {
				persistence.put(
						"PendingUpdate-" + string.replace("/", ";") + "-" + Calendar.getInstance().getTimeInMillis(),
						new PendingUpdate(data));
			} catch (MqttPersistenceException exception) {
				log.error("Exception :" + exception.getMessage());
				exception.printStackTrace();
			}
		}
	}

	@Override
	public void connect() {
		// Start the connection process in a detached thread.
		new Thread(this).start();
	}

	@Override
	public void disconnect() {
		// Disconnect.
		retryInterval = 0;
		if (mqtt.isConnected()) {
			try {
				mqtt.disconnect();
			} catch (MqttException exception) {
				exception.printStackTrace();
			}
		}

		// Close MQTT.
		try {
			mqtt.close();
		} catch (MqttException exception) {
			exception.printStackTrace();
		}

		// Close persistence.
		try {
			persistence.close();
		} catch (MqttPersistenceException exception) {
			exception.printStackTrace();
		}
	}

	@Override
	public boolean isConnected() {
		return isConnected;
	}

	@Override
	public void publish(String string, byte[] data, int i, boolean b) throws TransportException {
		try {
			mqtt.publish(string, data, i, b);
		} catch (MqttException e) {
			throw new TransportException(e);
		}
	}

	@Override
	public void connectionLost(Throwable throwable) {
		isConnected = false;
		setOffline();

		// Start thread that tries to reestablish the connection if needed.
		if (retryInterval != 0) {
			new Thread(this).start();
		}
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
		// Does not matter.
	}

	@Override
	public void run() {
		mqtt.setCallback(null);

		// As long as we are not connected, try to establish a connection to the broker.
		while (!mqtt.isConnected()) {
			try {
				// Try to connect to the broker.
				IMqttToken token = mqtt.connect(options, null, new IMqttActionListener() {

					@Override
					public void onSuccess(IMqttToken iMqttToken) {
						try {
							// Send birth message.
							mqtt.publish("@online/" + uuid, endpoint, 1, true);

							// Subscribe to all set commands.
							mqtt.subscribe("@set/" + uuid + "/#", 1);

							isConnected = true;

							// Send all saved updates on update topic.
							if (persistence != null) {
								new Thread(new Runnable() {
									@Override
									public void run() {
										try {
											@SuppressWarnings("unchecked")
											Enumeration<String> keyEnum = persistence.keys();
											while (mqtt.isConnected() && keyEnum.hasMoreElements()) {
												String key = keyEnum.nextElement();

												// Is it a pending update?
												if (key.startsWith("PendingUpdate-")) {

													// Get the pending update persistent object from store.
													MqttPersistable pendingUpdate = persistence.get(key);
													String uuid = key.substring(14, key.lastIndexOf("-")).replace(";",
															"/");

													// Try to send the update to the broker and remove it from the
													// storage.
													try {
														mqtt.publish("@update/" + uuid, pendingUpdate.getHeaderBytes(),
																1, false);
														persistence.remove(key);
													} catch (MqttException exception) {
														log.error("Exception: " + exception.getMessage());
														exception.printStackTrace();
													}

													try {
														Thread.sleep(100);
													} catch (InterruptedException exception) {
														log.error("Exception: " + exception.getMessage());
														exception.printStackTrace();
													}
												}
											}
										} catch (MqttPersistenceException exception) {
											log.error("Exception: " + exception.getMessage());
											exception.printStackTrace();
										}

									}
								}).start();
							}
						} catch (MqttException exception) {
							log.error("Exception: " + exception.getMessage());
							exception.printStackTrace();
						}
					}

					@Override
					public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
						log.error("Exception: " + throwable.getMessage());
						throwable.printStackTrace();
					}
				});

				// Wait for connect or error...
				token.waitForCompletion();

			} catch (MqttException exception) {
				log.error("Exception during connect:", exception);
			}

			// If the connection could not be established, sleep a moment before the next
			// try.
			if (!mqtt.isConnected()) {
				// If we should not retry, give up.
				if (retryInterval == 0)
					return;

				// Wait the retry interval.
				try {
					Thread.sleep(1000 * retryInterval);
				} catch (InterruptedException exception) {
					log.error("Exception: " + exception.getMessage());
					exception.printStackTrace();
				}

				// Again, if we should not retry, give up.
				if (retryInterval == 0)
					return;
			}
		}

		// If we arrive here, we are online, so we can inform listeners about that and
		// stop the connecting thread.
		mqtt.setCallback(this);
		setOnline();
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		try {
			byte[] data = message.getPayload();

			// First determine the message format (first byte identifies the message
			// format).
			CloudioMessageFormat messageFormat = CloudioMessageFormatFactory.massageFormat(message.getPayload()[0]);
			if (messageFormat == null) {
				log.error("Message-format " + (int) message.getPayload()[0] + " not supported!");
				return;
			}

			// Create attribute location path stack.
			Stack<String> location = new Stack<String>();
			String[] topics = topic.split("/");
			for (int i = topics.length - 1; i >= 0; --i) {
				location.push(topics[i]);
			}

			setRx(topic, location, messageFormat, data);

			// Read the action tag from the topic.
			String action = location.peek();
			if ("@set".equals(action)) {
				location.pop();
			} else {
				log.error("Method \"" + action + "\" not supported!");
			}
		} catch (Exception exception) {
			log.error("Exception: " + exception.getMessage());
			exception.printStackTrace();
		}
	}

}
