package game;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.Matrix;

import org.siprop.bullet.Bullet;
import org.siprop.bullet.util.Vector3;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class Game implements Renderer {
   private int canvas_width;  // width of the screen
   private int canvas_height; // height of the screen

   private Surface surface;

	private Context context;
	
	public Game(Context context, Surface surface) {
		this.context = context;
		this.surface = surface;
	}

	private float[] mViewMatrix = new float[16];
	private float[] mInverseView = new float[16]; // Inverse view matrix

	private float[] mProjectionMatrix = new float[16];
	private float[] mInverseProjection = new float[16]; // Inverse projection matrix

	World world; // Static physics and OpenGL objects
	
	ArrayList<Entity> entities; // Complete entity list

	int[] entCountDowns = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}; // Holds cooldown counters to prevent receiving remote entities' physics data after a local correction
	static final int COOLDOWN = 60; // Aforementioned cooldown time, in game frames

	float angleY = (float)Math.PI/6; // Camera elevation angle
	
	boolean[] isDown = {false, false}; // is finger down?
	
	float ratio; // view ratio
	float left; // left screen coord
	float right; // etc.
	float bottom;
	float top;
	float near;
	float far;
	
	private final float[] mLightPosInWorldSpace = {81,60,81,1};

	// Camera position
	float camposx;
	float camposy;
	float camposz;

	boolean isServer = false;
	boolean selected = false; // is Client/Server choice selected?

	Entity clientButton;
	Entity serverButton;

	boolean clientButtonDownInBounds = false;
	boolean serverButtonDownInBounds = false;

	Client client;
	Server server;

	// Keeps track of program state
	boolean resolved = false; // Service found on the network by client
	boolean connected = false; // Server and client are connected
	boolean ready = false; // Game started

	ServiceListener serviceListener; // Service listener for client to connect to server

	int localScore = 0; // Our score
	int networkScore = 0; // Their score

	float[] prevPos = new float[2];

	int scoreCooldown = 0; // Cooldown so ball-goal collision not processed multiple times

	int life = 0; // Game uptime in frames, up to the # of frames that Bullet will accept changes of physics data, allows new Client initialization

	int sendCounter = 0; // Keeps track of how many frames since last network send, sending every frame is unnecessary

	/**
	 * The Surface is created/init()
	 */
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.1f, 0f, 0.9f, 0.0f);
        GLES20.glClearDepthf(1.0f);
        //GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glEnable(GL10.GL_DEPTH_TEST);
        GLES20.glDepthFunc(GL10.GL_LEQUAL);
        
		Draw.checkGlError("Initialization");

		//Matrix.translateM(mViewMatrix,0,-1.6f, -2.0f, -10f); // translate left and into the screen

		Matrix.setLookAtM(mViewMatrix, 0, -3f, -4f, -5f, 7f, 0f, 0f, 0f, 1f, 0f);
		Matrix.invertM(mInverseView, 0, mViewMatrix, 0);
		mInverseView[12] = 0f;
		mInverseView[13] = 0f;
		mInverseView[14] = 0f;

		Draw.initialize(context, new float[]{0, 0, 15, 1});

		addButtons();
	}

	public void initializeWorld(ByteBuffer level){
		Bullet bullet = new Bullet();
		PhysObject.bullet = bullet;
		world = new World(bullet,level);

		entities = new ArrayList<>();

		addBalls();
		addBackStops();
		for (Entity e : entities){
			e.update();
		}

		Draw.setLightPos(mLightPosInWorldSpace);
	}

	public void onDrawFrame(GL10 gl) {
		if (ready) {
			handleWorldInput();

			Entity e = entities.get(0);
			Entity ball = entities.get(2);

			Vector3 dir;

			dir = new Vector3(e.curPosX-ball.curPosX, 0, e.curPosZ-ball.curPosZ);

			dir.normalize();

			camposx = (float) (10 * Math.cos(angleY) * dir.x) + e.curPosX;
			camposy = (float) (10 * Math.sin(angleY)) + e.curPosY;
			camposz = (float) (10 * Math.cos(angleY) * dir.z) + e.curPosZ;

			Draw.setCamPos(camposx, camposy, camposz);

			Matrix.setLookAtM(mViewMatrix, 0, camposx, camposy, camposz, ball.curPosX, ball.curPosY, ball.curPosZ, 0, 1f, 0);

			//if (entCountDowns[2] != 0) Log.e("Ballpos", Float.toString(ball.mat[12] - ball.prevMat[12])+", "+Float.toString(ball.mat[13] - ball.prevMat[13])+", "+Float.toString(ball.mat[14] - ball.prevMat[14]));

			Matrix.transposeM(mInverseView, 0, mViewMatrix, 0);

			mInverseView[3] = 0f;
			mInverseView[7] = 0f;
			mInverseView[11] = 0f;

			GLES20.glClear(GL10.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

 			render();

			physUpdate();
		}
		else{
			handleButtonInput();

			GLES20.glClear(GL10.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
			if (clientButtonDownInBounds) clientButton.draw(new float[]{1,0,0,0,
					0,1,0,0,
					0,0,1,0,
					0,0,0,1},mProjectionMatrix,true);
			else clientButton.draw(new float[]{1,0,0,0,
					0,1,0,0,
					0,0,1,0,
					0,0,0,1},mProjectionMatrix);
			if (serverButtonDownInBounds) serverButton.draw(new float[]{1,0,0,0,
					0,1,0,0,
					0,0,1,0,
					0,0,0,1},mProjectionMatrix,true);
			else serverButton.draw(new float[]{1,0,0,0,
					0,1,0,0,
					0,0,1,0,
					0,0,0,1},mProjectionMatrix);

			if (selected){
				if (!connected) {
					if (!isServer && !resolved && serviceListener.resolved) {
						resolved = true;															//state #1
						//try {
							client.connect(/*InetAddress.getByName("10.0.2.6")*/ serviceListener.host, /*27015*/ serviceListener.port);
							initializeWorld(null); //Change addBalls entity back to use serviceListener.host, and server port
						//} catch (UnknownHostException e) {
						//	e.printStackTrace();
						//}
					} else {
						if (((isServer && server.connected) || (!isServer && client.connected))) {
							connected = true;													   //state #2
							if (isServer){
								Entity e = entities.get(1);
								e.setAddressPort(new AddressPort(server.getLastConnected(),server.getLastPort()));

								/*int length = World.XSIZE*World.YSIZE;
								ByteBuffer bb = ByteBuffer.wrap(new byte[length*4]);
								FloatBuffer fb = FloatBuffer.wrap(world.groundPoints);
								for (int i=0; i<length; i++){
									fb.get();
									bb.putFloat(fb.get());
									fb.get();
								}

								server.sendAssured(Server.LEVEL, bb.array());*/
							}

							ready = true;
						}
					}
				}
				/*else{
					if ((isServer && server.levelSent) || (!isServer && client.levelDownloaded)){
																				  //state #3
						if (isServer){
							angleX = 0;
						}
						else{
							angleX = (float)Math.PI;
							initializeWorld(client.getLevel());
						}
						ready = true;
					}
				}*/
			}
		}
	}
	
	private void physUpdate() {
		Entity p0 = entities.get(0); // Local disc
		Entity p1 = entities.get(1); // First Network disc
		Entity ball = entities.get(2); // Ball

		// Adds new players for the Server or Client
		if (isServer){
			if (server.isNewPlayer()){
				int pn = server.getNumberOfPlayers();

				int wb = pn;
				if (wb == 1) wb = 2;

				int texnum = 13;
				if (wb%2 == 0) texnum = 14;

				float[] trans;

				if (wb%2 == 0){
					trans = new float[]{1,0,0,0,
							0,1,0,0,
							0,0,1,0,
							10f,30f,81f+10*wb,1};
				}
				else{
					trans = new float[]{1,0,0,0,
							0,1,0,0,
							0,0,1,0,
							151f,30f,81f+10*wb,1};
				}

				AddressPort addressPort = null;
				try {
					addressPort = new AddressPort(InetAddress.getByAddress(server.getNewPlayerAddress()),server.getNewPlayerPort());
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}

				Entity e = new Entity(p0.verts, trans, 1f, p0.faces, p0.normals, p0.uv, texnum, false, null, world, true, false, pn, /*InetAddress.getByName("10.0.2.6")*/ addressPort);
				entities.add(e);
			}
		}
		else{
			if (client.isNewPlayer() && life == 5){
				int pn = client.getNumberOfPlayers();

				int wb = pn;
				if (wb == 1) wb = 2;

				int texnum = 13;


				float[] trans;

				if (wb%2 == 0){
					texnum = 14;
					trans = new float[]{1,0,0,0,
							0,1,0,0,
							0,0,1,0,
							10f,30f,81f+10*wb,1};
				}
				else{
					trans = new float[]{1,0,0,0,
							0,1,0,0,
							0,0,1,0,
							151f,30f,81f+10*wb,1};
				}

				AddressPort addressPort = null;
				try {
					addressPort = new AddressPort(InetAddress.getByAddress(client.getNewPlayerAddress()),client.getNewPlayerPort());
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}

				Entity e = new Entity(p0.verts, trans, 1f, p0.faces, p0.normals, p0.uv, texnum, false, null, world, true, false, pn, /*InetAddress.getByName("10.0.2.6")*/ addressPort);
				entities.add(e);
			}

			if (client.toInitialize() && life == 5){
				int pn = client.getInitializePlayerNum();

				int texnum = 13;

				float[] trans;

				if (pn%2 == 0){
					texnum = 14;
					trans = new float[]{1,0,0,0,
							0,1,0,0,
							0,0,1,0,
							11f,30f,81f+10*pn,1};
				}
				else{
					trans = new float[]{1,0,0,0,
							0,1,0,0,
							0,0,1,0,
							151f,30f,81f+10*pn,1};
				}

				p0.setMat(trans);
				p0.setID(pn);
				p0.setTexnum(texnum);

				Entity.ids.clear();
				for (Entity e : entities){
					int id = e.getUniqueID();
					if (id != -1) Entity.ids.put(id,e);
				}
			}
		}

		//Increments entCountDowns if the counter is active
		for (int i=0; i<entCountDowns.length; i++){
			if (entCountDowns[i] != 0){
				entCountDowns[i]++;

				if (entCountDowns[i] == COOLDOWN) entCountDowns[i]=0;
			}
		}

		//Updates local and remote discs on server and client
		float[] mat;
		if (isServer){
			for (int i=0; i<12; i++) {
				Entity e = Entity.ids.get(i);
				if (e != null) {
					if (e.isLocal()) {
						if (server.newCorrectionAvailable(i)) {               //Applies any pending network collision correction before forces are added
							float[] dc = server.getDiscCorrection(i);

							float[] dcMat = Arrays.copyOfRange(dc, 0, 16);

							//if (dcMat[0] != Float.NaN) {
								e.setMat(dcMat);
							//}

							e.zeroVelocities();
							e.setLinVel(new Vector3(dc[16], dc[17], dc[18]));
							e.setAngVel(new Vector3(dc[19], dc[20], dc[21]));
							e.update();
						}
					} else{
						if (entCountDowns[i] == 0){
							mat = server.getDiscMatrix(i);
							if (mat != null) {
								e.setMat(mat);

								float[] v = server.getVelocities(i);
								e.zeroVelocities();
								e.setLinVel(new Vector3(v[0], v[1], v[2]));
								e.setAngVel(new Vector3(v[3], v[4], v[5]));
							}
						}
						else{
							server.getDiscMatrix(i);
							server.getVelocities(i);

							/*if (mat != null && entCountDowns[i] >= COOLDOWN-AVGTIME){
								float t = (entCountDowns[i]-(COOLDOWN-AVGTIME))/(float)AVGTIME;

								mat[12] = t*mat[12]+(1-t)*e.curPosX;
								mat[13] = t*mat[13]+(1-t)*e.curPosY;
								mat[14] = t*mat[14]+(1-t)*e.curPosZ;

								e.setMat(mat);

								e.zeroVelocities();
								e.setLinVel(new Vector3(v[0], v[1], v[2]));
								e.setAngVel(new Vector3(v[3], v[4], v[5]));
							}*/
						}
					}
				}
			}
		}
		else{
			for (int i=0; i<12; i++) {
				Entity e = Entity.ids.get(i);
				if (e != null) {
					if (e.isLocal()) {
						if (client.newCorrectionAvailable(i)) {               //Applies any pending network collision correction before forces are added
							float[] dc = client.getDiscCorrection(i);

							float[] dcMat = Arrays.copyOfRange(dc, 0, 16);

							//if (dcMat[0] != Float.NaN) {
								e.setMat(dcMat);
							//}
							e.zeroVelocities();
							e.setLinVel(new Vector3(dc[16], dc[17], dc[18]));
							e.setAngVel(new Vector3(dc[19], dc[20], dc[21]));
							e.update();
						}
					} else{
						if (entCountDowns[i] == 0){
							mat = client.getDiscMatrix(i);
							if (mat != null) {
								e.setMat(mat);

								float[] v = client.getVelocities(i);
								e.zeroVelocities();
								e.setLinVel(new Vector3(v[0], v[1], v[2]));
								e.setAngVel(new Vector3(v[3], v[4], v[5]));
							}
						}
						else{
							client.getDiscMatrix(i);
							client.getVelocities(i);

							/*if (mat != null && entCountDowns[i] >= COOLDOWN-AVGTIME){
								float t = (entCountDowns[i]-(COOLDOWN-AVGTIME))/(float)AVGTIME;

								mat[12] = t*mat[12]+(1-t)*e.curPosX;
								mat[13] = t*mat[13]+(1-t)*e.curPosY;
								mat[14] = t*mat[14]+(1-t)*e.curPosZ;

								e.setMat(mat);

								e.zeroVelocities();
								e.setLinVel(new Vector3(v[0], v[1], v[2]));
								e.setAngVel(new Vector3(v[3], v[4], v[5]));
							}*/
						}
					}
				}
			}
		}

		// Fixes local discs if they falls off the map somehow
		if (p0.curPosY < -10){
			p0.setPosition(new Vector3(p0.curPosX,30,p0.curPosZ));
			p0.update();
		}

		if(isServer && ball.curPosY < -10){
			ball.setPosition(new Vector3(ball.curPosX,30,ball.curPosZ));
			ball.update();
		}

		// Handles input
		if (isDown[0]) {
			float[] throwVec = {((float) surface.curpos.get(0).x - (prevPos[0])) / canvas_width, 0, ((float) surface.curpos.get(0).y - (prevPos[1]))/canvas_width, 1};

			prevPos[0] = (float) surface.curpos.get(0).x;
			prevPos[1] = (float) surface.curpos.get(0).y;

			float[] throwVecWorld = new float[4];

			Matrix.multiplyMV(throwVecWorld, 0, mInverseView, 0, throwVec, 0);

			Vector3 normed = new Vector3(throwVecWorld[0], 0, throwVecWorld[2]);

			if (normed.lengthSquared() != 0){
				normed.normalize();

				normed.x *= 1f;
				normed.z *= 1f;

				p0.applyCentralImpulse(normed);
			}
		}

		// Updates Bullet physics world
		world.update(System.currentTimeMillis());
		for (Entity e : entities){
			e.update();
		}

		// Fixes local discs if Bullet weirds out
		if (Float.isNaN(p0.curPosX+p0.curVelX) || Float.isInfinite(p0.curPosX+p0.curVelX)){
			p0.stepBack();
		}
		if (isServer && (Float.isNaN(ball.curPosX+ball.curVelX) || Float.isInfinite(ball.curPosX+ball.curVelX))){
			ball.stepBack();
		}

		// Increments the cooldown after scoring in the event that it's active
		if (scoreCooldown != 0){
			scoreCooldown++;
			if (scoreCooldown == 30){
				scoreCooldown = 0;
			}
		}

		// Sends collision correction to remote discs a few frames after the local collision
		for (int i=0; i<entCountDowns.length; i++){
			Entity nonlocal = Entity.ids.get(i);
			if (entCountDowns[i] == 5 && !nonlocal.isLocal()){
				byte[] data = new byte[92];
				ByteBuffer bb = ByteBuffer.wrap(data);
				FloatBuffer fb = bb.asFloatBuffer();

				//float[] matCopy = Arrays.copyOf(nonlocal.mat, 16);
				//if (entCountDowns[nonlocal.getUniqueID()] != 0) {
				//	matCopy[0] = Float.NaN;
				//}

				fb.put(0f); //the respective buffer positions are independent, advances fb's position by putting temporary data
				bb.putInt(nonlocal.uniqueID);
				fb.put(nonlocal.mat);
				fb.put(nonlocal.curVelX);
				fb.put(nonlocal.curVelY);
				fb.put(nonlocal.curVelZ);
				fb.put(nonlocal.curAngVelX);
				fb.put(nonlocal.curAngVelY);
				fb.put(nonlocal.curAngVelZ);

				if (isServer) {
					AddressPort ap = nonlocal.getAddressPort();
					try {
						server.sendDiscCorrection(data, ap.address, ap.port);
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else {
					AddressPort ap = nonlocal.getAddressPort();
					try {
						client.sendDiscCorrection(data, ap.address, ap.port);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

		// Goes through collisions detected by Bullet and adds extra momentum to the ball
		for (int h=0; h<entities.size()-1; h++){
			Entity a = entities.get(h);
			if (a.getUniqueID() != -1) for (int i=h+1; i<entities.size(); i++) {
				Entity b = entities.get(i);
				if (b.getUniqueID() != -1 && Entity.isColliding(b, a)) { //Technically should be "has collided", the contact manifold keeps track of collisions from the last world update
					float scale = 20f;

					Vector3 dir = new Vector3(b.curPosX - a.curPosX, b.curPosY - a.curPosY, b.curPosZ - a.curPosZ);
					dir.normalize();
					dir.scale(scale);

					Vector3 aVel = new Vector3(a.curVelX - dir.x, a.curVelY - dir.y, a.curVelZ - dir.z);

					dir.scale(a.mass / b.mass);

					Vector3 bVel = new Vector3(b.curVelX + dir.x, b.curVelY + dir.y, b.curVelZ + dir.z); //Mass portion of the momentum, only applied to e


					if ((a.isLocal() || b.isLocal()) && (a.getUniqueID() == 2 || b.getUniqueID() == 2) && (entCountDowns[2] == 0 || entCountDowns[2] > 15)) {
						if (b.getUniqueID() != 2 && !(isServer && b.getUniqueID() != 0 && a.getUniqueID() == 2)) {
							//a.zeroVelocities();
							a.setLinVel(aVel);
						}
						if (a.getUniqueID() != 2 && !(isServer && a.getUniqueID() != 0 && b.getUniqueID() == 2)) {
							//b.zeroVelocities();
							b.setLinVel(bVel);
						}

						entCountDowns[2] = 1;
					}
				}
			}
		}

		// For server to detect goals, respond to them accordingly, and notify clients
		boolean cs = world.clientScored(ball);
		boolean ss = world.serverScored(ball);

		if (isServer && scoreCooldown == 0 && (cs || ss)){
			byte[] data = new byte[5];
			ByteBuffer bb = ByteBuffer.wrap(data);
			if (cs){
				bb.put((byte)0);
				bb.putInt(++networkScore);
			}
			else {
				bb.put((byte)1);
				bb.putInt(++localScore);
			}
			scoreCooldown++;

			ArrayList<AddressPort> adds = server.getConnectedAddressPorts();

			for (AddressPort add : adds){
				server.sendAssured(Server.SCORE, data, add.address, add.port, 10000);
			}

			p0.setPosition(new Vector3(151,30,81));
			p0.zeroVelocities();
			p1.setPosition(new Vector3(10,30,81));
			p1.zeroVelocities();
			ball.setPosition(new Vector3(81,30,81));
			ball.zeroVelocities();

			for (int i=3; i<entities.size(); i+=2){
				Entity e = Entity.ids.get(i);
				e.setPosition(new Vector3(151f,30f,81f+10*i));
				e.zeroVelocities();
			}
			for (int i=4; i<entities.size(); i+=2){
				Entity e = Entity.ids.get(i);
				e.setPosition(new Vector3(11f,30f,81f+10*i));
				e.zeroVelocities();
			}
		}

		// Sends the telemetry for all local discs to all other players
		if (sendCounter == 1) {
			for (Entity e : entities) {
				if (e.getUniqueID() != -1 && e.isLocal()) {
					final byte[] data = new byte[92];
					ByteBuffer bb = ByteBuffer.wrap(data, 0, 92);
					FloatBuffer fb = bb.asFloatBuffer();
					fb.put(0f);
					bb.putInt(e.uniqueID);
					fb.put(e.mat);
					fb.put(e.curVelX);
					fb.put(e.curVelY);
					fb.put(e.curVelZ);
					fb.put(e.curAngVelX);
					fb.put(e.curAngVelY);
					fb.put(e.curAngVelZ);

					final ArrayList<AddressPort> adds;
					if (isServer) {
						adds = server.getConnectedAddressPorts();
						new Thread() {
							public void run() {
								try {
									for (AddressPort add : adds) {
										server.sendDiscData(data, add.address, add.port);
									}
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}.start();
					} else {
						adds = Entity.addressPorts;
						new Thread() {
							public void run() {
								try {
									for (AddressPort add : adds) {
										client.sendDiscData(data, add.address, add.port);
									}
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}.start();
					}
				}
			}
		}
		sendCounter++;
		if (sendCounter == 2) sendCounter = 0;

		// For client to respond to a server-sent score message
		if (!isServer){
			Integer score = client.getScore();
			if (score != null){
				int id = p0.getUniqueID();
				if (id == 1) id++;
				if (client.serverScored()){
					if (id%2 == 0) networkScore = score;
					else localScore = score;
				}
				else{
					if (id%2 == 0) localScore = score;
					else networkScore = score;
				}

				int xpos = 10;
				int zpos = 81;

				if (id%2 == 1){
					if (id > 2){
						xpos = 151;
						zpos = 81 + 10 * id;
					}
					else xpos = 151;
				}
				else {
					if (id > 2){
						xpos = 11;
						zpos = 81 + 10 * id;
					}
				}

				p0.setPosition(new Vector3(xpos,30,zpos));
				p0.zeroVelocities();
				ball.setPosition(new Vector3(81,30,81));
				ball.zeroVelocities();
			}
		}

		if (life < 5) life++;
	}

	// Renders the graphical world
	private void render() {
        world.draw(mViewMatrix, mProjectionMatrix);

		for (Entity e : entities){
			e.draw(mViewMatrix, mProjectionMatrix);
		}

		drawScores();
	}
	
	// resets the view on changed surface
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		canvas_width = width;
		canvas_height = height;
		
		GLES20.glViewport(0, 0, width, height); 	//Reset The Current Viewport

		ratio = (float) width / height;
		left = -ratio;
		right = ratio;
		bottom = -1.0f;
		top = 1.0f;
		near = 1.0f;
		far = 1000.0f;

		Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near, far);					//Reset The Modelview Matrix
		Matrix.invertM(mInverseProjection, 0, mProjectionMatrix, 0);

		System.out.print("yo");
	}

	// To initialize the disc entities
	private void addBalls(){
		ArrayList<Vector3> tempVerts = new ArrayList<>();
		ArrayList<ArrayList<Integer>> tempFaces = new ArrayList<>();
		ArrayList<Vector3> tempNormals = new ArrayList<>();
		ArrayList<Float> tempUV = new ArrayList<>();
		float s = 1f; //scale
		final float sqr2 = (float)Math.sqrt(2);

		tempVerts.add(new Vector3(-s,0,-s/sqr2));
		tempVerts.add(new Vector3(s,0,-s/sqr2));
		tempVerts.add(new Vector3(0,s,s/sqr2));
		tempVerts.add(new Vector3(0,-s,s/sqr2));

		tempFaces.add(new ArrayList<>(Arrays.asList(0,1,2)));
		tempFaces.add(new ArrayList<>(Arrays.asList(1,0,3)));
		tempFaces.add(new ArrayList<>(Arrays.asList(0,2,3)));
		tempFaces.add(new ArrayList<>(Arrays.asList(1,3,2)));

		for (int h=0; h<3; h++){
			HashMap<VectDouble,VectwBool> map = new HashMap<>();

			for (ArrayList<Integer> face : tempFaces){
				for (int i=0; i<3; i++){
					Vector3 a = tempVerts.get(face.get(i));
					Vector3 b = tempVerts.get(face.get((i+1)%3));

					VectDouble vd = new VectDouble(a,b);
					if (!map.containsKey(vd)) map.put(vd, new VectwBool(vd.mid(s)));
				}
			}

			ArrayList<ArrayList<Integer>> newFaces = new ArrayList<>();

			for (ArrayList<Integer> face : tempFaces){
				int f0 = face.get(0);
				int f1 = face.get(1);
				int f2 = face.get(2);

				Vector3 v0 = tempVerts.get(f0);
				Vector3 v1 = tempVerts.get(f1);
				Vector3 v2 = tempVerts.get(f2);

				VectwBool v01 = map.get(new VectDouble(v0,v1));
				VectwBool v02 = map.get(new VectDouble(v0,v2));
				VectwBool v12 = map.get(new VectDouble(v1,v2));

				int f01;
				int f02;
				int f12;

				if (v01.added) f01 = tempVerts.indexOf(v01.v);
				else{
					tempVerts.add(v01.v);
					f01 = tempVerts.size()-1;
					v01.added = true;
				}
				if (v02.added) f02 = tempVerts.indexOf(v02.v);
				else{
					tempVerts.add(v02.v);
					f02 = tempVerts.size()-1;
					v02.added = true;
				}
				if (v12.added) f12 = tempVerts.indexOf(v12.v);
				else{
					tempVerts.add(v12.v);
					f12 = tempVerts.size()-1;
					v12.added = true;
				}

				newFaces.add(new ArrayList<>(Arrays.asList(f0,f01,f02)));
				newFaces.add(new ArrayList<>(Arrays.asList(f1,f12,f01)));
				newFaces.add(new ArrayList<>(Arrays.asList(f2,f02,f12)));
				newFaces.add(new ArrayList<>(Arrays.asList(f01,f12,f02)));
			}

			tempFaces = newFaces;
		}

		for (Vector3 v : tempVerts){
			Vector3 n = new Vector3(v.x,v.y,v.z);

			n.normalize();

			tempNormals.add(n);

			tempUV.add((v.x+v.y+v.z)%1f); // Complete garbage: texture should be a uniform color until this is fixed
			tempUV.add((v.x+v.y+v.z+0.3f)%1f);
		}

		float[] trans = {1,0,0,0,
				0,1,0,0,
				0,0,1,0,
				10f,30f,81f,1};

		float[] trans2 = new float[]{1,0,0,0,
					0,1,0,0,
					0,0,1,0,
					151f,30f,81f,1};

		if (isServer){
			Entity e0 = new Entity(tempVerts, trans2, 1f, tempFaces, tempNormals, tempUV, 13, false, null, world, true, true, 0, null);
			Entity e1 = new Entity(tempVerts, trans, 1f, tempFaces, tempNormals, tempUV, 14, false, null, world, true, false, 1, null);

			entities.add(e0);
			entities.add(e1);
		}
		else{
			Entity e0 = new Entity(tempVerts, trans, 1f, tempFaces, tempNormals, tempUV, 14, false, null, world, true, true, 1, null);
			Entity e1;
			//try {
				e1 = new Entity(tempVerts, trans2, 1f, tempFaces, tempNormals, tempUV, 13, false, null, world, true, false, 0, /*new AddressPort(InetAddress.getByName("10.0.2.6"),27015)*/ new AddressPort(serviceListener.host, serviceListener.port));
			//} catch (UnknownHostException e) {
			//	e.printStackTrace();
			//}

			entities.add(e0);
			entities.add(e1);
		}

		s = 3;

		tempVerts = new ArrayList<>();
		tempFaces = new ArrayList<>();
		tempNormals = new ArrayList<>();
		tempUV = new ArrayList<>();

		tempVerts.add(new Vector3(-s,0,-s/sqr2));
		tempVerts.add(new Vector3(s,0,-s/sqr2));
		tempVerts.add(new Vector3(0,s,s/sqr2));
		tempVerts.add(new Vector3(0,-s,s/sqr2));

		tempFaces.add(new ArrayList<>(Arrays.asList(0,1,2)));
		tempFaces.add(new ArrayList<>(Arrays.asList(1,0,3)));
		tempFaces.add(new ArrayList<>(Arrays.asList(0,2,3)));
		tempFaces.add(new ArrayList<>(Arrays.asList(1,3,2)));

		for (int h=0; h<3; h++){
			HashMap<VectDouble,VectwBool> map = new HashMap<>();

			for (ArrayList<Integer> face : tempFaces){
				for (int i=0; i<3; i++){
					Vector3 a = tempVerts.get(face.get(i));
					Vector3 b = tempVerts.get(face.get((i+1)%3));

					VectDouble vd = new VectDouble(a,b);

					if (!map.containsKey(vd)) map.put(vd, new VectwBool(vd.mid(s)));
				}
			}

			ArrayList<ArrayList<Integer>> newFaces = new ArrayList<>();

			for (ArrayList<Integer> face : tempFaces){
				int f0 = face.get(0);
				int f1 = face.get(1);
				int f2 = face.get(2);

				Vector3 v0 = tempVerts.get(f0);
				Vector3 v1 = tempVerts.get(f1);
				Vector3 v2 = tempVerts.get(f2);

				VectwBool v01 = map.get(new VectDouble(v0,v1));
				VectwBool v02 = map.get(new VectDouble(v0,v2));
				VectwBool v12 = map.get(new VectDouble(v1,v2));

				int f01;
				int f02;
				int f12;

				if (v01.added) f01 = tempVerts.indexOf(v01.v);
				else{
					tempVerts.add(v01.v);
					f01 = tempVerts.size()-1;
					v01.added = true;
				}
				if (v02.added) f02 = tempVerts.indexOf(v02.v);
				else{
					tempVerts.add(v02.v);
					f02 = tempVerts.size()-1;
					v02.added = true;
				}
				if (v12.added) f12 = tempVerts.indexOf(v12.v);
				else{
					tempVerts.add(v12.v);
					f12 = tempVerts.size()-1;
					v12.added = true;
				}

				newFaces.add(new ArrayList<>(Arrays.asList(f0,f01,f02)));
				newFaces.add(new ArrayList<>(Arrays.asList(f1,f12,f01)));
				newFaces.add(new ArrayList<>(Arrays.asList(f2,f02,f12)));
				newFaces.add(new ArrayList<>(Arrays.asList(f01,f12,f02)));
			}

			tempFaces = newFaces;


		}

		for (Vector3 v : tempVerts){
			Vector3 n = new Vector3(v.x,v.y,v.z);

			n.normalize();

			tempNormals.add(n);

			tempUV.add((v.x+v.y+v.z)%1f); // Complete garbage: texture should be a uniform color until this is fixed
			tempUV.add((v.x+v.y+v.z+0.3f)%1f);
		}

		trans = new float[]{1,0,0,0,
				0,1,0,0,
				0,0,1,0,
				81f,30f,81f,1};

		boolean local = false;

		AddressPort add = null;

		if (isServer){
			local = true;
		}else{
			add = entities.get(1).getAddressPort();
		}

		entities.add(new Entity(tempVerts, trans, 0.5f, tempFaces, tempNormals, tempUV, 0, false, null, world, true, local, 2, add)); // The ball
		//entities.get(0).makeKinematic();
		//entities.get(1).makeKinematic();
	}

	// Adds the colored backstops for each team's goal
	private void addBackStops(){
		float miny = 0;
		float maxy = World.scale*World.EDGE_WIDTH;
		float minz = World.scale*(World.EDGE_WIDTH+2*(World.YNUM-1)/5f+0.5f);
		float maxz = World.scale*(World.EDGE_WIDTH+3*(World.YNUM-1)/5f+0.5f);
		float xval = 0;

		ArrayList<Vector3> tempVerts = new ArrayList<>(Arrays.asList(
				new Vector3(xval, miny, minz),
				new Vector3(xval, miny, maxz),
				new Vector3(xval, maxy, maxz),
				new Vector3(xval, maxy, minz)));
		ArrayList<ArrayList<Integer>> tempFaces = new ArrayList<>();
		{
			tempFaces.add(new ArrayList<>(Arrays.asList(0, 1, 2)));
			tempFaces.add(new ArrayList<>(Arrays.asList(0, 2, 3)));
		}
		ArrayList<Vector3> tempNormals = new ArrayList<>(Arrays.asList(new Vector3(0, 0, 1),
				new Vector3(0, 0, 1),
				new Vector3(0, 0, 1),
				new Vector3(0, 0, 1)));
		ArrayList<Float> tempUV = new ArrayList<>(Arrays.asList(0f, 1f,
				0f, 0f,
				1f, 0f,
				1f, 1f));
		float[] trans = {1, 0, 0, 0,
				0, 1, 0, 0,
				0, 0, 1, 0,
				0, 0, 0, 1};
		entities.add(new Entity(tempVerts, trans, 0f, tempFaces, tempNormals, tempUV, 14, false, null, null, false, false, -1, null));

		miny = 0;
		maxy = World.scale*World.EDGE_WIDTH;
		minz = World.scale*(World.EDGE_WIDTH+2*(World.YNUM-1)/5f-0.5f);
		maxz = World.scale*(World.EDGE_WIDTH+3*(World.YNUM-1)/5f-0.5f);
		xval = World.scale*(World.XSIZE-1);

		tempVerts = new ArrayList<>(Arrays.asList(
				new Vector3(xval, miny, minz),
				new Vector3(xval, miny, maxz),
				new Vector3(xval, maxy, maxz),
				new Vector3(xval, maxy, minz)));

		entities.add(new Entity(tempVerts, trans, 0f, tempFaces, tempNormals, tempUV, 13, false, null, null, false, false, -1, null));
	}

	// For the generation of a sphere approximating triangle mesh with arbitrary resolution
	private static class VectwBool{
		Vector3 v;
		boolean added;
		public VectwBool(Vector3 v){
			this.v = v;
			added = false;
		}
	}

	// An ordered pair of Vector3s
	private static class VectDouble{
		private static final float SQRT6D2 = 1.224744871391589049098642037352945695982973740f;

		public VectDouble(Vector3 a, Vector3 b) {
			this.a = a;
			this.b = b;
		}

		public Vector3 mid(float s){
			Vector3 ab = new Vector3();
			ab.add(a,b);
			ab.normalize();
			ab.scale(s*SQRT6D2);
			return ab;
		}

		@Override
		public int hashCode() {
			int hash = 7;
			hash = 37 * hash + Objects.hashCode(this.a)+Objects.hashCode(this.b);
			return hash;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final VectDouble other = (VectDouble) obj;
			if (!Objects.equals(this.a, other.a) && !Objects.equals(this.a, other.b)) {
				return false;
			}
			if (!Objects.equals(this.b, other.b) && !Objects.equals(this.b, other.a)){
				return false;
			}
			return true;
		}

		Vector3 a;
		Vector3 b;

		boolean added = false;
	}
	
    public void handleWorldInput(){
		for (int i=0; i<2; i++){
			if (surface.processDown.get(i)){
				isDown[i] = true;

				surface.processDown.set(i,false);

				if (i == 0){
					prevPos[0] = (float)surface.downpos.get(i).x;
					prevPos[1] = (float)surface.downpos.get(i).y;
				}
			}

			if (surface.processUp.get(i)){
				isDown[i] = false;
				surface.processUp.set(i, false);
			}
		}
	}

	public void handleButtonInput(){
		for (int i=0; i<2; i++) {
			if (surface.processDown.get(i)) {
				isDown[i] = true;

				surface.processDown.set(i, false);

				if (i==0 && !selected){
					Point2D dp = surface.downpos.get(0);

					float[] screenPos = {(float)(dp.x*2/canvas_width-1),(float)(dp.y*2/canvas_height-1),1,1};

					float[] invProj = new float[4];

					Matrix.multiplyMV(invProj, 0, this.mInverseProjection, 0, screenPos, 0);

					float x = invProj[0]*10;
					float y = invProj[1]*10;

					if(y<2.5 && y>-2.5){
						if (x<-1 && x>-6){
							serverButtonDownInBounds = true;
						}
						else if(x<6 && x>1){

							clientButtonDownInBounds = true;
						}
					}
				}
			}

			if (surface.processUp.get(i)) {
				isDown[i] = false;

				surface.processUp.set(i, false);

				if (i==0){
					Point2D dp = surface.uppos.get(0);

					float[] screenPos = {(float)(dp.x*2/canvas_width-1),(float)(dp.y*2/canvas_height-1),1,1};

					float[] invProj = new float[4];

					Matrix.multiplyMV(invProj, 0, this.mInverseProjection, 0, screenPos, 0);

					float x = invProj[0]*10;
					float y = invProj[1]*10;

					if(y<2.5 && y>-2.5){
						if (x<-1 && x>-6 && serverButtonDownInBounds){
							isServer = true;
							initializeWorld(null);
							selected = true;
							try {
								server = new Server(context);
							} catch (SocketException e) {
								e.printStackTrace();
							}
							server.start();
						}
						else if(x<6 && x>1 && clientButtonDownInBounds){
							selected = true;
							try {
								client = new Client();
							} catch (SocketException e) {
								e.printStackTrace();
							}
							client.start();
							serviceListener = new ServiceListener(context);
							/*try {
								client.connect(InetAddress.getByName("10.0.0.3"));
							} catch (UnknownHostException e) {
								e.printStackTrace();
							}*/
							System.out.print("hellop");
						}
					}

					serverButtonDownInBounds = false;
					clientButtonDownInBounds = false;
				}
			}
		}
	}

	// Add the client/server selection buttons
	public void addButtons() {
		float minx = 1;
		float maxx = 6;
		float miny = -2.5f;
		float maxy = 2.5f;

		ArrayList<Vector3> tempVerts = new ArrayList<>(Arrays.asList(new Vector3(minx, miny, -10),
				new Vector3(minx, maxy, -10),
				new Vector3(maxx, maxy, -10),
				new Vector3(maxx, miny, -10)));
		ArrayList<ArrayList<Integer>> tempFaces = new ArrayList<>();
		{
			tempFaces.add(new ArrayList<>(Arrays.asList(0, 1, 2)));
			tempFaces.add(new ArrayList<>(Arrays.asList(0, 2, 3)));
		}
		ArrayList<Vector3> tempNormals = new ArrayList<>(Arrays.asList(new Vector3(0, 0, 1),
				new Vector3(0, 0, 1),
				new Vector3(0, 0, 1),
				new Vector3(0, 0, 1)));
		ArrayList<Float> tempUV = new ArrayList<>(Arrays.asList(0f, 1f,
				0f, 0f,
				1f, 0f,
				1f, 1f));
		float[] trans = {1, 0, 0, 0,
				0, 1, 0, 0,
				0, 0, 1, 0,
				0, 0, 0, 1};
		clientButton = new Entity(tempVerts, trans, 0, tempFaces, tempNormals, tempUV, 15, null, null);

		minx = -6;
		maxx = -1;
		miny = -2.5f;
		maxy = 2.5f;

		tempVerts = new ArrayList<>(Arrays.asList(new Vector3(minx, miny, -10),
				new Vector3(minx, maxy, -10),
				new Vector3(maxx, maxy, -10),
				new Vector3(maxx, miny, -10)));

		serverButton = new Entity(tempVerts, trans, 0, tempFaces, tempNormals, tempUV, 17, null, null);
	}

	// Draws the player scores on top of the screen
	private void drawScores(){
		float[] ident = {1, 0, 0, 0,
				0, 1, 0, 0,
				0, 0, 1, 0,
				0, 0, 0, 1};

		float[] view = {1, 0, 0, 0,
				0, 1, 0, 0,
				0, 0, 1, 0,
				-19.5f, 12.5f, -6, 1};

		String str = Integer.toString(localScore);

		for (int i = 0; i < str.length(); i++) {
			String cur = str.substring(i,i+1);
			Draw.drawFullbright(serverButton.vertBufInd, serverButton.faceBufInd, serverButton.faces.size() * 3, view, mProjectionMatrix, ident,Integer.parseInt(cur)+3);
			view[12] += 2;
		}

		view[13] = 8.5f;
		view[12] = -19.5f;
		str = Integer.toString(networkScore);

		for (int i = 0; i < str.length(); i++) {
			String cur = str.substring(i,i+1);
			Draw.drawFullbright(serverButton.vertBufInd, serverButton.faceBufInd, serverButton.faces.size() * 3, view, mProjectionMatrix, ident,Integer.parseInt(cur)+3);
			view[12] += 2;
		}
	}

	// Closes all open networking threads
	public void shutDownNetworking(){
		if (server != null) server.close();
		if (client != null) client.close();
		if (serviceListener != null) serviceListener.tearDown();
	}
}