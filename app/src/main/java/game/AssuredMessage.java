package game;

import android.os.SystemClock;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.TimerTask;

public class AssuredMessage extends TimerTask{
    private Client client=null;
    private Server server=null;

    private InetAddress address;

    private int port;

    private int localPort;

    private static int code=0;

    private Integer localCode;

    private byte type;

    private byte[] data;

    private int timeOut;

    private long firstSend=Long.MIN_VALUE;

    public static HashMap<Integer, AssuredMessage> instances = new HashMap<>();

    private int numParts;

    private int curPart = 0;

    public boolean sentLast = false;

    public static final int PREFIX_LENGTH = 1+4+4+1+4+1+1; //16

    public AssuredMessage(byte type, byte[] data, Client client, InetAddress address, int port, int timeOut){
        this.type = type;
        this.data = Arrays.copyOf(data,data.length);
        this.client = client;
        this.address = address;
        this.port = port;
        this.timeOut = timeOut;
        localPort = client.getLocalPort();
        localCode = code++;
        if (code == Integer.MAX_VALUE) code = 0; //Uses 1 less than all possible non-negative Integers

        numParts = data.length/(256-PREFIX_LENGTH);
        if (data.length%(256-PREFIX_LENGTH) != 0) numParts++;

        instances.put(localCode,this);
    }

    public AssuredMessage(byte type, byte[] data, Server server, InetAddress address, int port, int timeOut){
        this.type = type;
        this.data = Arrays.copyOf(data,data.length);
        this.server = server;
        this.address = address;
        this.port = port;
        this.timeOut = timeOut;
        localPort = server.getLocalPort();
        localCode = code++;
        if (code == Integer.MAX_VALUE) code = 0;

        numParts = data.length/(256-PREFIX_LENGTH);
        if (data.length%(256-PREFIX_LENGTH) != 0) numParts++;

        instances.put(localCode,this);
    }

    public void run() {
        if (firstSend == Long.MIN_VALUE){
            firstSend = SystemClock.elapsedRealtime();
        }
        else{
            int lateness = (int)(SystemClock.elapsedRealtime()-firstSend);

            if (lateness > timeOut){ // ~20 attemps should be more than enough
                this.cancel();
                instances.remove(localCode);
                return;
            }
        }


        int dataSize = (256-PREFIX_LENGTH)-128;
        if (curPart == numParts-1){
            dataSize = (byte)((data.length%(256-PREFIX_LENGTH)-128)); // the receiver of the message will add back the 128 to get the actual size
            sentLast = true;
        }

        byte last = (byte)0;
        if (sentLast) last = (byte)1;
        byte[] bytes = new byte[256];
        ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).put(Server.ASSURED).putInt(localPort).putInt(localCode).put(last).putInt(curPart).put(type).put((byte)dataSize).put(data,curPart*(256-PREFIX_LENGTH),dataSize+128).array();

        try {
            if (client != null){
                client.send(bytes, address,port);
            }
            else if (server != null){
                server.send(bytes, address,port);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized int getCurPart(){
        return curPart;
    }

    public synchronized  void incrementCurPart(){
        curPart++;
        firstSend = Long.MIN_VALUE;
    }

    public byte getType(){
        return type;
    }
}
