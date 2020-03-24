package ch.hevs.cloudio.endpoint.transport;

import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.hevs.cloudio.endpoint.CloudioMessageFormat;
import ch.hevs.cloudio.endpoint.configuration.CloudioEndpointConfiguration;

public abstract class Transport {
	protected final Logger log;
	protected String uuid;
	protected TransportListener listener;
	protected CloudioEndpointConfiguration configuration;
	protected byte[] endpoint;

	protected Transport(String _uuid, byte[] _ep, CloudioEndpointConfiguration _configuration,
			TransportListener _listener) {
		log = LoggerFactory.getLogger(this.getClass());
		endpoint = _ep;
		uuid = _uuid;
		configuration = _configuration;
		listener = _listener;
	}

	public abstract void connect();

	public abstract void disconnect();

	public abstract boolean isConnected();

	public abstract void publish(String string, byte[] data, int i, boolean b) throws TransportException;

	public void update(String string, byte[] data, int i, boolean b) throws TransportException {
		publish("@update/" + string, data, 1, false);
	}

	public void setOnline() {
		listener.onConnect();
	}

	public void setOffline() {
		listener.onDisconnect();
	}

	public void setRx(String topic, Stack<String> location, CloudioMessageFormat messageFormat, byte[] data)
			throws Exception {
		listener.onRx(topic, location, messageFormat, data);
	}
}
