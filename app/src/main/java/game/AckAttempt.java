package game;

import java.io.IOException;
import java.net.InetAddress;
import java.util.TimerTask;

public class AckAttempt extends TimerTask{
	Server server;
	
	InetAddress address;

	int port;

	public AckAttempt(Server server, InetAddress address, int port){
		this.server = server;
		this.address = address;
		this.port = port;
	}
	
	public void run() {
		try {
			server.send(Server.ack,address,port);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
