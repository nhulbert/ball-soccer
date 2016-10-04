/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package game;

import android.content.Context;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Neil
 */
public class Server extends Thread {
    private Context context;
    DatagramSocket socketin;
    DatagramSocket socketout;
    
    private boolean active = false;
    
    private byte[] current=null;
    
    private Timer attemptTimer;  // Used to send scheduled packets

    public static final byte CONNECT1 = 12;
	public static final byte ACK = 13;
    public static final byte CONNECT2 = 14;
    public static final byte HANDSHAKE = 15;
    public static final byte ASSURED = 18;
    public static final byte ASSURED_ACK = 19;
    public static final byte DISC_MATRIX = 20;

    public static final byte LEVEL = 11; // Assured message types
    public static final byte DISC_CORRECTION = 12;
    public static final byte SCORE = 13;
    public static final byte NEWPLAYER = 14;
    public static final byte INITIALIZATION = 15;

    public static final byte[] connect2 = {HANDSHAKE,Server.CONNECT2};
    public static final byte[] ack = {HANDSHAKE,Server.ACK};

    public boolean connected = false;

    ArrayList<Integer> assuredCodes = new ArrayList<>();
    private int assuredCodesInd=0;

    private Broadcaster broadcaster;

    private ByteBuffer level = ByteBuffer.allocateDirect(World.XSIZE *World.YSIZE *4+8);

    private int levelDataCounter = 0;
    public boolean levelDownloaded = false;

    public boolean levelSent = false;

    private boolean[] discDataIsNew = {false,false,false,false,false,false,false,false,false,false,false,false};
    private float[][] discMatrix = new float[12][16];
    private float[][] discVelocities = new float[12][6];
    private int discSendCounter = 0;
    public static final int DISCSENDMAX = 200000;
    private int lastDiscReceivedNum = DISCSENDMAX-1;

    private float[][] discCorrection = new float[12][];
    private boolean[] correctDiscs = {false,false,false,false,false,false,false,false,false,false,false,false};
    private int discCorCounter = 0;
    private int lastDiscCorReceivedNum = Server.DISCSENDMAX-1;

    private ArrayList<AddressPort> connectedAddressPorts = new ArrayList<>();

    private HashMap<AddressPort, Integer> connectStages = new HashMap<>();

    private InetAddress lastConnected = null;
    private int lastConnectedPort =-1;

    private final int localPort;

    private int numberOfPlayers = 2; //Starts at the number of players present after the initial connection
    private boolean isNewPlayer = false;
    private byte[] newPlayerAddress = new byte[4];
    private int newPlayerPort;

    public Server(Context context) throws SocketException{
        this("Server", context);
    }
    
    public Server(String name, Context context) throws SocketException{
        super(name);

        this.context = context;

        socketout = new DatagramSocket();
        socketin = new DatagramSocket(/*27015*/);
        
        socketin.setSoTimeout(250);

        socketout.setBroadcast(true);

        localPort = socketin.getLocalPort();

        active = true;
    }
    
    @Override
    public void run() {
        boolean exit=false;

        broadcast();

        while(!exit){
            try {
                DatagramPacket in = new DatagramPacket(new byte[256], 256);
                //if (mate == null){
                    socketin.receive(in);

                //}
                /*else
                {
                    do {
                        socket.receive(packet);
                    } while (packet.getAddress() != mate);
                }*/
                synchronized (this) {
                    if (!active) exit=true;
                    current = in.getData();
                    final InetAddress curAddress = in.getAddress();

                    switch(current[0]){
                    	case Server.HANDSHAKE:
                            boolean contains = false;
                            for (AddressPort a : connectStages.keySet()){
                                if (Arrays.equals(a.address.getAddress(),curAddress.getAddress())){
                                    contains = true;
                                    break;
                                }
                            }
	                    	if (!contains && current[1] == CONNECT1){
                                ByteBuffer bb = ByteBuffer.wrap(current);
                                bb.order(ByteOrder.BIG_ENDIAN);
                                bb.getChar();
                                lastConnected = curAddress;
                                lastConnectedPort = bb.getInt();

                                ack(curAddress, lastConnectedPort);

                                System.out.print("hello");
	                    	}
	                    	else{
                                Integer stage = connectStages.get(new AddressPort(curAddress,lastConnectedPort));
                                if (stage != null && stage.equals(1) && current[1] == CONNECT2){
                                    if (attemptTimer != null) {
                                        attemptTimer.cancel();
                                        attemptTimer.purge();
                                    }

                                    attemptTimer = new Timer();

                                    if (!connectedAddressPorts.isEmpty()) {
                                        final int curPort = lastConnectedPort;

                                        final byte[] data = new byte[12];
                                        ByteBuffer bb = ByteBuffer.wrap(data);
                                        bb.putInt(++numberOfPlayers);
                                        bb.put(curAddress.getAddress(), 0, 4);
                                        bb.putInt(curPort);

                                        sendAssured(Server.INITIALIZATION, data, curAddress, curPort, 10000);  //Sends client's own info to it, address/port being sent is not used

                                        for (int i = 0; i < connectedAddressPorts.size(); i++) {
                                            AddressPort a = connectedAddressPorts.get(i);
                                            int ind = i+2;
                                            if (ind == 2) ind = 1; // Skips over the ball unique ID which is always 2
                                            byte[] data2 = new byte[12];
                                            bb = ByteBuffer.wrap(data2);
                                            bb.putInt(ind);
                                            bb.put(a.address.getAddress(), 0, 4);
                                            bb.putInt(a.port);

                                            sendAssured(Server.NEWPLAYER, data, a.address, a.port, 10000);     //Sends newly connected client info to clients already connected
                                            sendAssured(Server.NEWPLAYER, data2, curAddress, curPort, 10000);  //Sends info of clients already connected to newly connected client
                                        }

                                        newPlayerAddress = curAddress.getAddress();
                                        newPlayerPort = lastConnectedPort;
                                        isNewPlayer = true;
                                    }

                                    connectedAddressPorts.add(new AddressPort(curAddress, lastConnectedPort));
                                    connected = true;
                                }
	                    	}
	                    	break;
                        case Server.ASSURED:
                            if (connected){
                                boolean received = false;

                                ByteBuffer bb = ByteBuffer.wrap(current,1,AssuredMessage.PREFIX_LENGTH-1).order(ByteOrder.BIG_ENDIAN);

                                int returnPort = bb.getInt();

                                int code = bb.getInt();

                                boolean last = bb.get() == (byte)1;

                                int part = bb.getInt();

                                byte type = bb.get();

                                int size = (int)bb.get()+128;

                                if (!assuredCodes.contains(code)){
                                    switch(type) {
                                        case Server.LEVEL:
                                            received = true;
                                            if (part == levelDataCounter) {
                                                level.put(current, AssuredMessage.PREFIX_LENGTH, size);
                                                levelDataCounter++;

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
                                        case Server.DISC_CORRECTION:
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
                                    }
                                }

                                if (received) send(ByteBuffer.allocate(9).order(ByteOrder.BIG_ENDIAN).put(Server.ASSURED_ACK).putInt(code).putInt(part).array(), curAddress, returnPort); //Acknowledge the message
                            }
                            break;
                        case Server.ASSURED_ACK:
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
                        case Server.DISC_MATRIX:
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
            }catch (SocketException e){
            	active = false;
            	exit = true;
            }
            catch (InterruptedIOException ignored){
            	
            }
            catch (IOException e){
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, e);
            }
        }
        socketin.close();
    }

    public synchronized void sendDiscData(byte[] msg, InetAddress address, int port) throws IOException{
        byte[] completeMsg = new byte[5+msg.length];

        ByteBuffer.wrap(completeMsg).put(Server.DISC_MATRIX).putInt(discSendCounter++).put(msg,0,msg.length);

        if (discSendCounter == DISCSENDMAX) discSendCounter = 0;

        send(completeMsg, address, port);
    }

    public void sendDiscCorrection(byte[] msg, InetAddress address, int port) throws IOException{
        byte[] completeMsg = new byte[4+msg.length];
        ByteBuffer.wrap(completeMsg).putInt(discCorCounter++).put(msg,0,msg.length);

        if (discCorCounter == Server.DISCSENDMAX) discCorCounter = 0;

        sendAssured(Server.DISC_CORRECTION,completeMsg,address,port);
    }

    public synchronized void send(byte[] msg, InetAddress address, int port) throws IOException {
        boolean act;
        synchronized (this) {act = active;}
        if (act){
            socketout.send(new DatagramPacket(msg, msg.length, address, port));
        }
    }

    public synchronized byte[] getCurrent() {
        return current;
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

        broadcaster.tearDown();
	}
    
    public boolean isActive() {
        return active;
    }
    
    private void ack(InetAddress a,int p){
    	synchronized(this){
            connectStages.put(new AddressPort(a,p), 1);
        }
    	
    	if (attemptTimer != null){
    		attemptTimer.cancel();
    		attemptTimer.purge();
    	}
    	
    	attemptTimer = new Timer();
    	
    	attemptTimer.schedule(new AckAttempt(this,a,p),0,500);
    }

    private void broadcast(){
        broadcaster = new Broadcaster(context,socketin.getLocalPort());
    }

    public void sendAssured(byte type, byte[] data, InetAddress address, int port){
        new Timer().schedule(new AssuredMessage(type, data, this, address, port, 1000),0, 50);
    }

    public void sendAssured(byte type, byte[] data, InetAddress address, int port, int timeOut){
        new Timer().schedule(new AssuredMessage(type, data, this, address, port, timeOut),0, 50);
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

    public ByteBuffer getLevel(){
        return level;
    }

    public synchronized ArrayList<AddressPort> getConnectedAddressPorts(){
        return connectedAddressPorts;
    }

    public synchronized InetAddress getLastConnected(){
        return lastConnected;
    }

    public synchronized int getLastPort(){
        return lastConnectedPort;
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
}
