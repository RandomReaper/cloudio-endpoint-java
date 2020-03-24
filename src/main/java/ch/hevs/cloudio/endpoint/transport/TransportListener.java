package ch.hevs.cloudio.endpoint.transport;

import java.util.Stack;

import ch.hevs.cloudio.endpoint.CloudioMessageFormat;

public interface TransportListener {
	public void onConnect();
	public void onDisconnect();
	public void onRx(String topic, Stack<String> location, CloudioMessageFormat messageFormat, byte[] data) throws Exception;
}
