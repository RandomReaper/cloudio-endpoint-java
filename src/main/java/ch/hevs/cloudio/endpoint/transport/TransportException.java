package ch.hevs.cloudio.endpoint.transport;

public class TransportException extends Exception {

	private static final long serialVersionUID = -669071140907738299L;

	public TransportException() {
		super();
	}

	public TransportException(Throwable cause) {
		super(cause);
	}

	public TransportException(String message) {
		super(message);
	}

	public TransportException(String message, Throwable cause) {
		super(message, cause);
	}
}
