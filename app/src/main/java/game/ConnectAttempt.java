package game;

import java.io.IOException;
import java.net.InetAddress;
import java.util.TimerTask;

// Task that attempts to connect to a specified Server socket

public class ConnectAttempt extends TimerTask{
	Client client;
	
	InetAddress address;

	int port;

	public ConnectAttempt(Client client, InetAddress address, int port){
		this.client = client;
		this.address = address;
		this.port = port;
	}
	
	public void run() {
		try {
			client.send(client.getConnectMessage(), address, port);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
