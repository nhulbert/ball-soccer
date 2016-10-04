/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package game;

import android.util.Log;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

// The Client object for handling network communication

public class Client extends Thread{
    private DatagramSocket socketout;
    private DatagramSocket socketin;
	private boolean active = false;
    
    private byte[] current=null; // The input message

	private byte[] connect1; // Handshake message that contains the local input socket port

    private Timer attemptTimer; // Timer for handshake messages
    
    private int connectStage = 0; // The current stage of the handshake process
    
    public boolean connected = false;
    
    private InetAddress lastConnected; // The last connected client

	private final int localPort;

    private ArrayList<Integer> assuredCodes = new ArrayList<>(); // A circular buffer of the previous unique AssuredMessage messages sent, checked to prevent receiving the same message twice
	private int assuredCodesInd = 0; // Buffer index

	private int port = -1;

	// Variables storing network disc physics information
	private boolean[] discDataIsNew = {false,false,false,false,false,false,false,false,false,false,false,false};
	private float[][] discMatrix = new float[12][16];
	private float[][] discVelocities = new float[12][6];
	private int discSendCounter = 0;
	private int lastDiscReceivedNum = Server.DISCSENDMAX-1;

	// Variables storing local disc correction information
	private float[][] discCorrection = new float[12][];
	private boolean[] correctDiscs = {false,false,false,false,false,false,false,false,false,false,false,false};
	private int discCorCounter = 0;
	private int lastDiscCorReceivedNum = Server.DISCSENDMAX-1;

	// Variables storing level initialization information
	private ByteBuffer level = ByteBuffer.allocateDirect(World.XSIZE *World.YSIZE *4+8);
	private int levelDataCounter = 0;
	public boolean levelDownloaded = false;
	public boolean levelSent = false;

	// Score information
	private Integer score = null;
	private boolean serverScored = false;

	// Player addition information
	private boolean isNewPlayer = false;
	private int numberOfPlayers;
	private byte[] newPlayerAddress = new byte[4];
	private int newPlayerPort;

	// Player initialization information
	private boolean toInitialize = false;
	private int initializePlayerNum;

    public Client() throws SocketException{    	
        socketout = new DatagramSocket();
		socketin = new DatagramSocket();

		socketin.setSoTimeout(250);

		socketin.setBroadcast(true);

		ByteBuffer bb = ByteBuffer.allocateDirect(4);
		bb.order(ByteOrder.BIG_ENDIAN);
		localPort = socketin.getLocalPort();
		bb.asIntBuffer().put(localPort);
		connect1 = new byte[]{Server.HANDSHAKE,Server.CONNECT1,bb.get(),bb.get(),bb.get(),bb.get()};

        active = true;
    }
    
    public void run() {
		DatagramPacket in = new DatagramPacket(new byte[256], 256);
        boolean exit = false;
        while (!exit){
            try {
                socketin.receive(in);
                synchronized (this){
                    if (!active) exit = true;
                    current = in.getData();
                   	if (lastConnected != null){
						final InetAddress curAddress = in.getAddress();
                    	switch(current[0]){
                    		case Server.HANDSHAKE: // Handshake logic
		                    	if (current[1] == Server.ACK && connectStage == 1){
									if (attemptTimer != null){
										attemptTimer.cancel();
										attemptTimer.purge();
									}

									attemptTimer = new Timer();

									new Thread(){public void run(){
										try {
											send(Server.connect2,lastConnected,port);
										} catch (IOException e) {
											e.printStackTrace();
										}}}.start();

									connected = true;
			                    }
		                    	break;
							case Server.ASSURED: // Message from AssuredMessage
								if (connected){
									boolean received = false;

									ByteBuffer bb = ByteBuffer.wrap(current,1,AssuredMessage.PREFIX_LENGTH-1).order(ByteOrder.BIG_ENDIAN);

									int returnPort = bb.getInt();

									int code = bb.getInt(); // Unique message code

									boolean last = bb.get() == (byte)1; // is the last message part

									int part = bb.getInt(); // Part number

									byte type = bb.get(); // Message type

									int size = (int)bb.get()+128; // Size of the data

									if (!assuredCodes.contains(code)){
										switch(type) {
											case Server.LEVEL: // Level initialization message
												received = true;
												if (part == levelDataCounter) {
													level.put(current, AssuredMessage.PREFIX_LENGTH, size);
													levelDataCounter++;

													Log.d("Level","Level Data Packet #"+Integer.toString(levelDataCounter));

													if (last) {
														levelDownloaded = true;

														if (assuredCodes.size() == 50) {
															assuredCodes.set(assuredCodesInd++, code);
															if (assuredCodesInd == 50)
																assuredCodesInd = 0;
														} else {
															assuredCodes.add(code);
														}
													}
												}
												break;
											case Server.DISC_CORRECTION: // Correction of a local disc
												received = true;
												float[] dc = new float[22];
												bb = ByteBuffer.wrap(current,AssuredMessage.PREFIX_LENGTH,8);
												int orderNum = bb.getInt();
												if ((orderNum-lastDiscCorReceivedNum+Server.DISCSENDMAX)%Server.DISCSENDMAX < Server.DISCSENDMAX/2f) {
													int discNum = bb.getInt();
													ByteBuffer.wrap(current, AssuredMessage.PREFIX_LENGTH + 8, size - 8).asFloatBuffer().get(dc);
													setDiscCorrection(discNum, dc);

													if (assuredCodes.size() == 50) {
														assuredCodes.set(assuredCodesInd++, code);
														if (assuredCodesInd == 50)
															assuredCodesInd = 0;
													} else {
														assuredCodes.add(code);
													}

													lastDiscCorReceivedNum = orderNum;
												}
												break;
											case Server.SCORE: // Score update message
												received = true;
												bb = ByteBuffer.wrap(current,AssuredMessage.PREFIX_LENGTH,5);
												boolean ss = false;
												if (bb.get() == (byte)1) ss = true;
												setScore(ss, bb.getInt());

												if (assuredCodes.size() == 50) {
													assuredCodes.set(assuredCodesInd++, code);
													if (assuredCodesInd == 50)
														assuredCodesInd = 0;
												} else {
													assuredCodes.add(code);
												}
												break;
											case Server.NEWPLAYER: // Add new player message
												if (!isNewPlayer) {
													bb = ByteBuffer.wrap(current, AssuredMessage.PREFIX_LENGTH, 12);

													numberOfPlayers = bb.getInt();
													bb.get(newPlayerAddress,0,4);
													newPlayerPort = bb.getInt();

													isNewPlayer = true;
													received = true;
												}
												break;
											case Server.INITIALIZATION: // Re-initialize local player location message
												received = true;
												bb = ByteBuffer.wrap(current, AssuredMessage.PREFIX_LENGTH, 12);

												initializePlayerNum = bb.getInt();
												toInitialize = true;
										}
									}

									if (received) send(ByteBuffer.allocate(9).order(ByteOrder.BIG_ENDIAN).put(Server.ASSURED_ACK).putInt(code).putInt(part).array(),curAddress,returnPort); //Acknowledge the message
								}
								break;
							case Server.ASSURED_ACK: // Acknowledgement of an assured message
								if (connected){
									ByteBuffer bb = ByteBuffer.wrap(current,1,8).order(ByteOrder.BIG_ENDIAN);
									int code = bb.getInt();
									int part = bb.getInt();
									AssuredMessage instance = AssuredMessage.instances.get(code);

									if (instance != null && instance.getCurPart() == part){
										if (instance.sentLast) {
											if (instance.getType() == Server.LEVEL){
												this.levelSent = true;
											}
											instance.cancel();
											AssuredMessage.instances.remove(code);
										}
										else{
											instance.incrementCurPart();
										}
									}
								}
								break;
							case Server.DISC_MATRIX: // Physics state of a remote disc
								if (connected) {
									ByteBuffer bb = ByteBuffer.wrap(current,1,8);
									int temp = bb.getInt();

									if ((temp-lastDiscReceivedNum+Server.DISCSENDMAX)%Server.DISCSENDMAX < Server.DISCSENDMAX/2f) {
										int discNum = bb.getInt();
										bb = ByteBuffer.wrap(current, 9, 64);
										setDiscMatrix(discNum, bb);
										bb = ByteBuffer.wrap(current, 73, 24);
										setVelocities(discNum, bb);
										flagDiscData(discNum);
										lastDiscReceivedNum = temp;
									}
								}
                    	}
                    }
                }
            }
            catch (SocketException e){
            	active = false;
            	exit = true;
            }
            catch (InterruptedIOException e){
				System.out.print("Interrupted");
			}
            catch (IOException e) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, e);
            }
        }
        socketin.close();
    }

	public synchronized void sendDiscData(byte[] msg, InetAddress address, int port) throws IOException{
		byte[] completeMsg = new byte[5+msg.length];

		ByteBuffer.wrap(completeMsg).put(Server.DISC_MATRIX).putInt(discSendCounter++).put(msg,0,msg.length);

		if (discSendCounter == Server.DISCSENDMAX) discSendCounter = 0;

		send(completeMsg, address, port);
	}

	public void sendDiscCorrection(byte[] msg, InetAddress address, int port) throws IOException{
		byte[] completeMsg = new byte[4+msg.length];
		ByteBuffer.wrap(completeMsg).putInt(discCorCounter++).put(msg,0,msg.length);

		if (discCorCounter == Server.DISCSENDMAX) discCorCounter = 0;

		sendAssured(Server.DISC_CORRECTION,completeMsg,address,port);
	}

    public synchronized void send(byte[] msg, InetAddress address, int port) throws IOException {
        if (active){
			DatagramPacket out = new DatagramPacket(msg, msg.length, address, port);
            socketout.send(out);
        }
    }
    
    public void close() {

    	active = false;
    	
    	if (socketout != null){
	        socketout.close();
	        synchronized(this) {
	            active=false;
	        }
	    	if (attemptTimer != null){
	    		attemptTimer.cancel();
	    		attemptTimer.purge();
	    	}

			for (AssuredMessage i : AssuredMessage.instances.values()){
				i.cancel();
			}
    	}
    	if (socketin != null){
    		socketin.close();
    	}
    }
    
    public void connect(InetAddress address, int port){
    	lastConnected = address;
    	this.port = port;

    	connectStage = 1;
    	
    	if (attemptTimer != null){
    		attemptTimer.cancel();
    		attemptTimer.purge();
    	}
    	
    	attemptTimer = new Timer();
    	attemptTimer.schedule(new ConnectAttempt(this,address,port), 0, 500);
    }
    
    public synchronized byte[] getCurrent() {
        return current;
    }
    
    public boolean isActive() {
        return active;
    }

	public void sendAssured(byte type, byte[] data, InetAddress address, int port){
		new Timer().schedule(new AssuredMessage(type, data, this, address, port, 1000),0, 50);
	}

	public void sendAssured(byte type, byte[] data, InetAddress address, int port, int timeOut){
		new Timer().schedule(new AssuredMessage(type, data, this, address, port, timeOut),0, 50);
	}

	public byte[] getConnectMessage(){
		return connect1;
	}

	private synchronized void setDiscMatrix(int discNum, ByteBuffer bb){
		bb.asFloatBuffer().get(discMatrix[discNum]);
	}

	public synchronized float[] getDiscMatrix(int discNum){
		if (discDataIsNew[discNum]){
			return Arrays.copyOf(discMatrix[discNum],16);
		}
		return null;
	}

	private synchronized void setVelocities(int discNum, ByteBuffer bb) {
		bb.asFloatBuffer().get(discVelocities[discNum]);
	}

	public synchronized float[] getVelocities(int discNum) {
		if (discDataIsNew[discNum]) {
			discDataIsNew[discNum] = false;
			return Arrays.copyOf(discVelocities[discNum], 6);
		}
		return null;
	}

	public synchronized void flagDiscData(int discNum){
		discDataIsNew[discNum] = true;
	}

	private synchronized void setDiscCorrection(int discNum, float[] dc){
		discCorrection[discNum] = dc;
		correctDiscs[discNum] = true;
	}

	public synchronized float[] getDiscCorrection(int discNum) {
		correctDiscs[discNum] = false;
		return discCorrection[discNum];
	}

	public synchronized boolean newCorrectionAvailable(int discNum){
		return correctDiscs[discNum];
	}

	private synchronized void setScore(boolean serverScored, int s){
		score = s;
		this.serverScored = serverScored;
	}

	public synchronized boolean serverScored(){
		return serverScored;
	}

	public synchronized Integer getScore(){
		Integer res = score;
		score = null;
		return res;
	}

	public ByteBuffer getLevel(){
		return level;
	}

	public InetAddress getAddress(){
		return this.lastConnected;
	}

	public int getLocalPort(){
		return localPort;
	}

	public synchronized int getNumberOfPlayers(){
		return numberOfPlayers;
	}

	public synchronized byte[] getNewPlayerAddress(){
		return newPlayerAddress;
	}

	public synchronized int getNewPlayerPort(){
		isNewPlayer = false;
		return newPlayerPort;
	}

	public synchronized boolean isNewPlayer(){
		return isNewPlayer;
	}

	public synchronized boolean toInitialize(){
		return toInitialize;
	}

	public synchronized int getInitializePlayerNum(){
		toInitialize = false;
		return initializePlayerNum;
	}
}
