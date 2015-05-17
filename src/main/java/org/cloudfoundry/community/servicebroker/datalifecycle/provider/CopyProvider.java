package org.cloudfoundry.community.servicebroker.datalifecycle.provider;

import java.util.Map;

import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;

public interface CopyProvider {

	/**
	 * Create a copy of the source instance. The implementer should inject that
	 * through an environment variable or other means. Upon the copy will be
	 * launched and accessible to clients.
	 * 
	 * @param instanceId
	 *            to create a copy of
	 * @return the id of the new copy.
	 * @throws ServiceBrokerException
	 *             on error
	 */
	String createCopy(String instanceId)
			throws ServiceBrokerException;

	/**
	 * Remove a copy from the iaas. The expectation is that all artifacts
	 * associated with the copy (snapshots, ami's etc) are cleaned up
	 * 
	 * @param instance
	 *            to delete
	 * @throws ServiceBrokerException
	 *             on error
	 */
	void deleteCopy(final String instance)
			throws ServiceBrokerException;

	/**
	 * Return the creds hash associated with service brokers. Should contain a
	 * URI, username, password or whatever makes sense for your service. Will be
	 * injected into your app.
	 */
	Map<String, Object> getCreds(final String instance)
			throws ServiceBrokerException;
}
