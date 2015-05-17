package org.cloudfoundry.community.servicebroker.datalifecycle.exception;

public class DataProviderSanitizationFailedException extends Exception {

	private static final long serialVersionUID = 1L;

	public DataProviderSanitizationFailedException(String msg) {
		super(msg);
	}
}
