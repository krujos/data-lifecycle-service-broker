package io.pivotal.cdm.utils;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.apache.log4j.Logger;

public class HostUtils {
	private Logger logger = Logger.getLogger(HostUtils.class);

	public boolean waitForBoot(Map<String, Object> creds) {
		try {
			URI uri = new URI(((String) creds.get("uri")).replace("jdbc:", ""));
			int port = uri.getPort();
			String host = uri.getHost();
			logger.info("Waiting for " + host + " to boot.");
			for (int i = 8; i > 0; --i) {
				try {
					try {
						Thread.sleep(20000);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					logger.info("Attempting to connect to " + host
							+ " on port " + port);
					Socket socket = new Socket();
					SocketAddress addr = new InetSocketAddress(
							Inet4Address.getByName(host), port);
					socket.connect(addr, 10000);
					boolean connected = socket.isConnected();
					socket.close();
					if (connected) {
						logger.info(host + "is responding on " + port);
						return true;
					}
				} catch (IOException e) {
					logger.error(host + " is not responding on " + port + " "
							+ i + " retires remaning.");
				}
			}
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
}
