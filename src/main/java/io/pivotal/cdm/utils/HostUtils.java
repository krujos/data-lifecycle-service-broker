package io.pivotal.cdm.utils;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import org.apache.log4j.Logger;
import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;

public class HostUtils {
	private Logger logger = Logger.getLogger(HostUtils.class);

	public boolean waitForBoot(String ip, int port)
			throws ServiceBrokerException {

		logger.info("Waiting for " + ip + " to boot.");
		for (int i = 8; i > 0; --i) {
			try {
				try {
					Thread.sleep(20000);
				} catch (InterruptedException e1) {
					throw new ServiceBrokerException(
							"Internal Error! Something went wrong sleeping the threads!");
				}
				logger.info("Attempting to connect to " + ip + " on port "
						+ port);
				Socket socket = new Socket();
				SocketAddress addr = new InetSocketAddress(
						Inet4Address.getByName(ip), port);
				socket.connect(addr, 10000);
				boolean connected = socket.isConnected();
				socket.close();
				if (connected) {
					logger.info(ip + "is responding on " + port);
					return true;
				}
			} catch (IOException e) {
				logger.error(ip + " is not responding on " + port + " " + i
						+ " retires remaning.");
			}
		}

		return false;
	}
}
