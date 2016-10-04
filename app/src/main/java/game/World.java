package game;

import android.opengl.GLES20;

import org.siprop.bullet.Bullet;
import org.siprop.bullet.MotionState;
import org.siprop.bullet.PhysicsWorld;
import org.siprop.bullet.RigidBody;
import org.siprop.bullet.interfaces.Shape;
import org.siprop.bullet.shape.ConvexHullShape;
import org.siprop.bullet.shape.StaticPlaneShape;
import org.siprop.bullet.shape.TriangleMeshShape;
import org.siprop.bullet.util.Vector3;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

// The Bullet physics world class, also used for storing the static physics objects comprising the 'world'

public class World{
	PhysicsWorld physicsWorld;
    Bullet bullet;
    
    Long time = null;
    
    PhysObject ground;
	PhysObject left;
	PhysObject right;
	PhysObject back;
	PhysObject front;
	PhysObject top;
    
    float[] groundPoints;
    int[] groundInds;

	final static int XSIZE = 81; // number of mesh points along the x axis
	final static int YSIZE = 81; // and the y axis

	final static int EDGE_WIDTH = 4; // width of the sloped edges of the field

	final static int XNUM = XSIZE-2*EDGE_WIDTH; // number of mesh points along the x axis excluding the edges
	final static int YNUM = YSIZE-2*EDGE_WIDTH; // and along the y axis

	final static float scale = 2f; // the distance between adjacent mesh points

    Map<Integer,RigidBody> rigidbodies; // Bullet rigidbody collection

	// holds the OpenGL buffer indices
    int vertBufInd=-1;
    int faceBufInd=-1;
    
    int vertBufIndSkybox=-1;
    int faceBufIndSkybox=-1;
    
    int indexCount=-1;
    int indexCountSkybox=-1;

	public World(Bullet bullet, ByteBuffer level){
		this.bullet = bullet;
    	physicsWorld = bullet.createPhysicsWorld(new Vector3( -10000.0f, -10000.0f, -10000.0f),
        		new Vector3( 10000.0f,  10000.0f,  10000.0f),
        		1024, new Vector3(0, -50f, 0));
    	
    	groundPoints = new float[3*(XNUM*YNUM + 2*EDGE_WIDTH*(XNUM+YSIZE))];
    	
    	//double variation = 5;

		//Random r1 = new Random(System.currentTimeMillis());

		// Creates ground without edges
		if (level == null) {
			for (int i = 0; i < YNUM; i++) {
				for (int h = 0; h < XNUM; h++) {
					/*float heightAvg = 0;
					int count = 0;
					if (i != 0) {
						heightAvg += groundPoints[3 * (((i - 1) * XSIZE) + h) + 1];
						count++;
					}
					if (h != 0) {
						heightAvg += groundPoints[3 * (i * XSIZE + h - 1) + 1];
						count++;
					}
					if (h != 0 && i != 0) {
						heightAvg += groundPoints[3 * ((i - 1) * XSIZE + h - 1) + 1] / Math.sqrt(2);
						count++;
					}

					if (count != 0) heightAvg /= count;*/

					groundPoints[3 * (i * XNUM + h)] = scale * (EDGE_WIDTH+h);
					groundPoints[3 * (i * XNUM + h) + 1] = 0;
					groundPoints[3 * (i * XNUM + h) + 2] = scale * (EDGE_WIDTH+i);
				}

				/*float min = Float.MAX_VALUE;

				for (Float f : groundPoints){
					if (f<min) min = f;
				}
				for (int h=0; h<groundPoints.size(); h++){
					groundPoints.set(h, groundPoints.get(h)-min);
				}*/
			}
		}
		/*else{
			level.position(0);
			for (int i = 0; i < YSIZE; i++) {
				for (int h = 0; h < XSIZE; h++) {
					float heightAvg = 0;
					int count = 0;
					if (i != 0) {
						heightAvg += groundPoints[3 * (((i - 1) * XSIZE) + h) + 1];
						count++;
					}
					if (h != 0) {
						heightAvg += groundPoints[3 * (i * XSIZE + h - 1) + 1];
						count++;
					}
					if (h != 0 && i != 0) {
						heightAvg += groundPoints[3 * ((i - 1) * XSIZE + h - 1) + 1] / Math.sqrt(2);
						count++;
					}

					if (count != 0) heightAvg /= count;

					groundPoints[3 * (i * XSIZE + h)] = scale * i;
					groundPoints[3 * (i * XSIZE + h) + 1] = level.getFloat();
					groundPoints[3 * (i * XSIZE + h) + 2] = scale * h;
				}

				float min = Float.MAX_VALUE;

				for (Float f : groundPoints){
					if (f<min) min = f;
				}
				for (int h=0; h<groundPoints.size(); h++){
					groundPoints.set(h, groundPoints.get(h)-min);
				}
			}
		}*/

		// Creates the sloped edges of the ground, with t being the parameter for the iteration
		int numSquares = (XNUM-1)*(YNUM-1);

		groundInds = new int[6*(numSquares+2*EDGE_WIDTH*(XNUM+YSIZE))-3*(EDGE_WIDTH*4)];

		for (int i=0; i<numSquares; i++){
			int xval=i%(XNUM-1);
			int yval=i/(XNUM-1);

			groundInds[6*i] = yval* XNUM +xval;
			groundInds[6*i+1] = (yval+1)* XNUM +xval+1;
			groundInds[6*i+2] = yval* XNUM +xval+1;

			groundInds[6*i+3] = yval* XNUM +xval;
			groundInds[6*i+4] = (yval+1)* XNUM +xval;
			groundInds[6*i+5] = (yval+1)* XNUM +xval+1;
		}

		final int t1 = XNUM-1;
		final int t2 = t1+EDGE_WIDTH;
		final int t3 = t2+(YNUM-1);
		final int t4 = t3+EDGE_WIDTH;
		final int t5 = t4+(XNUM-1);
		final int t6 = t5+EDGE_WIDTH;
		final int t7 = t6+(YNUM-1);
		final int t8 = t7+EDGE_WIDTH;

		int ind = 3*(XNUM)*(YNUM);
		int ind2 = 6*numSquares;

		ArrayList<Point2D> curve = new ArrayList<>();
		Point2D p = new Point2D(0d,-1d);

		double td = Math.PI/2/EDGE_WIDTH;
		double c = Math.cos(td);
		double s = Math.sin(td);

		for (int i=0; i<EDGE_WIDTH; i++){
			double temp = c*p.x-s*p.y;
			p.y = s*p.x+c*p.y;
			p.x = temp;
			curve.add(new Point2D(p.x, p.y));
		}

		int t=0;
		for (; t<t1; t++){
			for (int j=0; j<curve.size(); j++) {
				Point2D h = curve.get(j);
				groundPoints[ind++] = scale*(EDGE_WIDTH+t);
				groundPoints[ind++] = scale*(float)(EDGE_WIDTH*(h.y+1));
				groundPoints[ind++] = scale*(EDGE_WIDTH-(float)(EDGE_WIDTH*h.x));

				if (j == 0) groundInds[ind2++] = t;
				else groundInds[ind2++] = ind/3-2;

				groundInds[ind2++] = ind/3-1;
				groundInds[ind2++] = ind/3-1+EDGE_WIDTH;

				groundInds[ind2++] = groundInds[(ind2) - 4];
				groundInds[ind2++] = ind/3-1+EDGE_WIDTH;
				if (j == 0) groundInds[ind2++] = t+1;
				else groundInds[ind2++] = ind/3-2+EDGE_WIDTH;
			}
		}

		for (; t<t2; t++){
			int tm = t-t1;
			Point2D cu = curve.get(EDGE_WIDTH-1-tm);
			for (int j=0; j<curve.size(); j++) {
				Point2D h = curve.get(j);
				groundPoints[ind++] = scale*(XSIZE-1-EDGE_WIDTH-(float)(EDGE_WIDTH*cu.y*h.x));
				groundPoints[ind++] = scale*(float)(EDGE_WIDTH*(h.y+1));
				groundPoints[ind++] = scale*(EDGE_WIDTH-(float)(EDGE_WIDTH*cu.x*h.x));

				if (j == 0) groundInds[ind2++] = XNUM-1;
				else groundInds[ind2++] = ind/3-2;
				groundInds[ind2++] = ind/3-1;
				groundInds[ind2++] = ind/3-1+EDGE_WIDTH;

				if (j != 0) {
					groundInds[ind2++] = groundInds[(ind2) - 4];
					groundInds[ind2++] = ind / 3 - 1 + EDGE_WIDTH;
					groundInds[ind2++] = ind / 3 - 2 + EDGE_WIDTH;
				}
			}
		}
		for (; t<t3; t++){
			int tm = t-t2;
			for (int j=0; j<curve.size(); j++) {
				Point2D h = curve.get(j);

				if (tm < 3*(YNUM-1)/5 && tm > 2*(YNUM-1)/5) {
					h = new Point2D(j,-1d);
				}

				groundPoints[ind++] = scale*(XSIZE-1-EDGE_WIDTH+(float)(EDGE_WIDTH*h.x));
				groundPoints[ind++] = scale*(float)(EDGE_WIDTH*(h.y+1));
				groundPoints[ind++] = scale*(EDGE_WIDTH+tm);

				if (j == 0) groundInds[ind2++] = XNUM-1+XNUM*tm;
				else groundInds[ind2++] = ind/3-2;
				groundInds[ind2++] = ind/3-1;
				groundInds[ind2++] = ind/3-1+EDGE_WIDTH;

				groundInds[ind2++] = groundInds[(ind2) - 4];
				groundInds[ind2++] = ind/3-1+EDGE_WIDTH;
				if (j == 0) groundInds[ind2++] = XNUM-1+XNUM*(tm+1);
				else groundInds[ind2++] = ind/3-2+EDGE_WIDTH;
			}
		}
		for (; t<t4; t++){
			int tm = t-t3;
			Point2D cu = curve.get(EDGE_WIDTH-1-tm);
			Point2D h;
			for (int j=0; j<curve.size(); j++) {
				h = curve.get(j);

				groundPoints[ind++] = scale*(XSIZE-1-EDGE_WIDTH+(float)(EDGE_WIDTH*cu.x*h.x));
				groundPoints[ind++] = scale*(float)(EDGE_WIDTH*(h.y+1));
				groundPoints[ind++] = scale*(YSIZE-1-EDGE_WIDTH-(float)(EDGE_WIDTH*cu.y*h.x));

					if (j == 0) groundInds[ind2++] = XNUM*YNUM-1;
					else groundInds[ind2++] = ind/3-2;
					groundInds[ind2++] = ind/3-1;
					groundInds[ind2++] = ind/3-1+EDGE_WIDTH;

					if (j != 0) {
						groundInds[ind2++] = groundInds[(ind2) - 4];
						groundInds[ind2++] = ind/ 3 - 1 + EDGE_WIDTH;
						groundInds[ind2++] = ind/ 3 - 2 + EDGE_WIDTH;
					}
			}
		}
		for (; t<t5; t++){
			int tm = t-t4;
			for (int j=0; j<curve.size(); j++) {
				Point2D h = curve.get(j);
				groundPoints[ind++] = scale*(XSIZE-1-EDGE_WIDTH-tm);
				groundPoints[ind++] = scale*(float)(EDGE_WIDTH*(h.y+1));
				groundPoints[ind++] = scale*(YSIZE-1-EDGE_WIDTH+(float)(EDGE_WIDTH*h.x));

				if (j == 0) groundInds[ind2++] = XNUM*YNUM-1-tm;
				else groundInds[ind2++] = ind/3-2;
				groundInds[ind2++] = ind/3-1;
				groundInds[ind2++] = ind/3-1+EDGE_WIDTH;

				groundInds[ind2++] = groundInds[(ind2) - 4];
				groundInds[ind2++] = ind/3-1+EDGE_WIDTH;
				if (j == 0) groundInds[ind2++] = XNUM*YNUM-1-tm-1;
				else groundInds[ind2++] = ind/3-2+EDGE_WIDTH;
			}
		}
		for (; t<t6; t++){
			int tm = t-t5;
			Point2D cu = curve.get(EDGE_WIDTH-1-tm);
			for (int j=0; j<curve.size(); j++) {
				Point2D h = curve.get(j);
				groundPoints[ind++] = scale*(EDGE_WIDTH+(float)(EDGE_WIDTH*cu.y*h.x));
				groundPoints[ind++] = scale*(float)(EDGE_WIDTH*(h.y+1));
				groundPoints[ind++] = scale*(YSIZE-1-EDGE_WIDTH+(float)(EDGE_WIDTH*cu.x*h.x));

				if (j == 0) groundInds[ind2++] = XNUM*(YNUM-1);
				else groundInds[ind2++] = ind/3-2;
				groundInds[ind2++] = ind/3-1;
				groundInds[ind2++] = ind/3-1+EDGE_WIDTH;

				if (j != 0) {
					groundInds[ind2++] = groundInds[(ind2) - 4];
					groundInds[ind2++] = ind/ 3 - 1 + EDGE_WIDTH;
					groundInds[ind2++] = ind/ 3 - 2 + EDGE_WIDTH;
				}
			}
		}
		for (; t<t7; t++){
			int tm = t-t6;
			for (int j=0; j<curve.size(); j++) {
				Point2D h = curve.get(j);

				if (tm < 3*(YNUM-1)/5 && tm > 2*(YNUM-1)/5) {
					h = new Point2D(j,-1d);
				}

				groundPoints[ind++] = scale*(EDGE_WIDTH-(float)(EDGE_WIDTH*h.x));
				groundPoints[ind++] = scale*(float)(EDGE_WIDTH*(h.y+1));
				groundPoints[ind++] = scale*(YSIZE-1-EDGE_WIDTH-tm);

				if (j == 0) groundInds[ind2++] = XNUM*(YNUM-1)-tm*XNUM;
				else groundInds[ind2++] = ind/3-2;
				groundInds[ind2++] = ind/3-1;
				groundInds[ind2++] = ind/3-1+EDGE_WIDTH;

				groundInds[ind2++] = groundInds[(ind2) - 4];
				groundInds[ind2++] = ind/3-1+EDGE_WIDTH;
				if (j == 0) groundInds[ind2++] = XNUM*(YNUM-1)-(tm+1)*XNUM;
				else groundInds[ind2++] = ind/3-2+EDGE_WIDTH;
			}
		}
		for (; t<t8; t++){
			int tm = t-t7;
			Point2D cu = curve.get(EDGE_WIDTH-1-tm);
			for (int j=0; j<curve.size(); j++) {
				Point2D h = curve.get(j);
				groundPoints[ind++] = scale*(EDGE_WIDTH-(float)(EDGE_WIDTH*cu.x*h.x));
				groundPoints[ind++] = scale*(float)(EDGE_WIDTH*(h.y+1));
				groundPoints[ind++] = scale*(EDGE_WIDTH+(float)(EDGE_WIDTH*cu.y*h.x));

				if (j == 0) groundInds[ind2++] = 0;
				else groundInds[ind2++] = ind/3-2;
				groundInds[ind2++] = ind/3-1;
				groundInds[ind2++] = ((ind/3-1+EDGE_WIDTH)-XNUM*YNUM)%(t8*EDGE_WIDTH)+XNUM*YNUM;

				if (j != 0) {
					groundInds[ind2++] = groundInds[(ind2) - 4];
					groundInds[ind2++] = ((ind/ 3 - 1 + EDGE_WIDTH)-XNUM*YNUM)%(t8*EDGE_WIDTH)+XNUM*YNUM;
					groundInds[ind2++] = ((ind/ 3 - 2 + EDGE_WIDTH)-XNUM*YNUM)%(t8*EDGE_WIDTH)+XNUM*YNUM;
				}
			}
		}

    	createBuffers();
    	
        StaticPlaneShape l = new StaticPlaneShape(new Vector3(1.0f, 0.0f, 0.0f), 0f);
    	StaticPlaneShape r = new StaticPlaneShape(new Vector3(-1.0f, 0.0f, 0.0f),-160f);
    	StaticPlaneShape f = new StaticPlaneShape(new Vector3(0.0f, 0.0f, 1.0f),0f);
    	StaticPlaneShape b = new StaticPlaneShape(new Vector3(0.0f, 0.0f, -1.0f),-160f);
		StaticPlaneShape o = new StaticPlaneShape(new Vector3(0.0f,-1.0f,0.0f),-60);

		left = new PhysObject(physicsWorld, l, new MotionState(),0);
		right = new PhysObject(physicsWorld, r, new MotionState(),0);
		front = new PhysObject(physicsWorld, f, new MotionState(),0);
		back = new PhysObject(physicsWorld, b, new MotionState(),0);
		top = new PhysObject(physicsWorld, o, new MotionState(),0);

    	Shape groundShape = new TriangleMeshShape(groundPoints,groundInds);
    	
    	MotionState groundState = new MotionState();
    	
        ground = new PhysObject(physicsWorld, groundShape, groundState, 0);
        
        //ground.rigidBody.setRestitution(0.1f);
        //ground.rigidBody.setFriction(0.98f);

        createSkybox();
    }

	// Creates the skybox
	private void createSkybox() {
    	ArrayList<Float> verts = new ArrayList<>(Arrays.asList(
    			-1f,-1f,-1f,
    			1f,-1f,-1f,
    			1f,1f,-1f,
    			-1f,1f,-1f,
    			-1f,-1f,1f,
    			1f,-1f,1f,
    			1f,1f,1f,
    			-1f,1f,1f));
    	ArrayList<Integer> inds = new ArrayList<>(Arrays.asList(
    			0,1,2,
    			2,3,0,
    			
    			4,5,6,
    			6,7,4,
    			
    			0,3,7,
    			7,4,0,
    			
    			1,5,2,
    			2,5,6,
    			
    			3,2,6,
    			6,7,3,
    			
    			1,0,4,
    			4,5,1
    			));
    	
    	ArrayList<Integer> res = generateBuffers(verts,null,inds,null);

    	vertBufIndSkybox = res.get(0);
    	faceBufIndSkybox = res.get(1);
    	indexCountSkybox = res.get(2);
	}
    
	public void update(Long curtime){
		if (time != null){
            rigidbodies = bullet.doSimulation(physicsWorld,10);
        }
        
        time = curtime;
    }

	// The collision checking methods
    public boolean isGroundColliding(Entity a){
    	return PhysObject.bullet.isColliding(a.getPhysObj().rigidBody, ground.rigidBody);
    }

	public boolean serverScored(Entity a){
		return (PhysObject.bullet.isColliding(a.getPhysObj().rigidBody, left.rigidBody) && a.curPosY < scale*EDGE_WIDTH+4f && a.curPosZ < 2*(EDGE_WIDTH+3*(YNUM-1)/5) && a.curPosZ > 2*(EDGE_WIDTH+2*(YNUM-1)/5));
	}

	public boolean clientScored(Entity a){
		return (PhysObject.bullet.isColliding(a.getPhysObj().rigidBody, right.rigidBody) && a.curPosY < scale*EDGE_WIDTH+4f && a.curPosZ < 2*(EDGE_WIDTH+3*(YNUM-1)/5) && a.curPosZ > 2*(EDGE_WIDTH+2*(YNUM-1)/5));
	}

    public void draw(float[] mViewMatrix, float[] mProjectionMatrix){
    	float m[] = {1,0,0,0,
    				 0,1,0,0,
    				 0,0,1,0,
    				 0,0f,0f,1};

    	Draw.draw(vertBufInd, faceBufInd, indexCount, mViewMatrix, mProjectionMatrix, m, 1);

		//Draw.draw(hole.vertBufInd, hole.faceBufInd, hole.faces.size()*3, mViewMatrix, mProjectionMatrix, m ,0);
		//Draw.draw(hole2.vertBufInd, hole2.faceBufInd, hole2.faces.size()*3, mViewMatrix, mProjectionMatrix, m ,0);
    	Draw.drawSkybox(vertBufIndSkybox, faceBufIndSkybox, indexCountSkybox, mViewMatrix, mProjectionMatrix, 2);
    }

	// Adds an object to the physics world by calculating the object's convex hull and using it to approximate the object's shape
    public PhysObject addConvexHullObj(ArrayList<Vector3> points, float[] trans, float mass){
        float[] verts = new float[3*points.size()];
        
    	for (int i=0; i<points.size(); i++){
        	verts[3*i]=points.get(i).x;
        	verts[3*i+1]=points.get(i).y;
        	verts[3*i+2]=points.get(i).z;
        }
    	
    	Shape shape = new ConvexHullShape(verts);
    	MotionState motionState = new MotionState();
    	
        return new PhysObject(physicsWorld, shape, motionState, mass, trans);
    }

	// Adds an object to the physics world by using precisely the same geometric mesh used for rendering
	public PhysObject addTriangleMeshObj(ArrayList<Vector3> points, ArrayList<ArrayList<Integer>> inds, float[] trans, float mass){
		float[] verts = new float[3*points.size()];
		int[] indices = new int[3*inds.size()];

		for (int i=0; i<points.size(); i++){
			verts[3*i]=points.get(i).x;
			verts[3*i+1]=points.get(i).y;
			verts[3*i+2]=points.get(i).z;
		}

		for (int i=0; i<inds.size(); i++){
			indices[3*i] = inds.get(i).get(0);
			indices[3*i+1] = inds.get(i).get(1);
			indices[3*i+2] = inds.get(i).get(2);
		}

		Shape shape = new TriangleMeshShape(verts,indices);
		MotionState motionState = new MotionState();

		return new PhysObject(physicsWorld, shape, motionState, mass, trans);
	}

	// Creates the OpenGL buffers for the world's static objects
    private void createBuffers(){
		final int vertBuf[] = new int[1];
    	final int faceBuf[] = new int[1];
    	
		ByteBuffer byteBuf = ByteBuffer.allocateDirect((XNUM*YNUM + 2*EDGE_WIDTH*(XNUM+YSIZE)) * 8 * 4);
		byteBuf.order(ByteOrder.nativeOrder());
		FloatBuffer vertexBuffer = byteBuf.asFloatBuffer();
		for (int i=0; i<3*XNUM*YNUM; i+=3){
			int xval = (i/3)% XNUM;
			int yval = (i/3)/ XNUM;
			Vector3 v = new Vector3(groundPoints[i],groundPoints[i+1],groundPoints[i+2]);
			
			Vector3 n = new Vector3(0,0,0);
			
			if (xval > 0){
				Vector3 xm = new Vector3(groundPoints[i-3],groundPoints[i-2],groundPoints[i-1]);
				xm.sub(xm, v);
				
				if (yval > 0){
					Vector3 ym = new Vector3(groundPoints[i-3*(XNUM)],groundPoints[i-3*(XNUM)+1],groundPoints[i-3*(XNUM)+2]);
					ym.sub(ym, v);
					
					Vector3 cross = new Vector3();
					cross.cross(xm, ym);
					n.add(n, cross);
				}
				
				if (yval < YNUM -1){
					Vector3 ym = new Vector3(groundPoints[i+3*(XNUM)],groundPoints[i+3*(XNUM)+1],groundPoints[i+3*(XNUM)+2]);
					ym.sub(v, ym);
					
					Vector3 cross = new Vector3();
					cross.cross(xm, ym);
					n.add(n, cross);
				}
			}
			if (xval < XNUM -1){
				Vector3 xm = new Vector3(groundPoints[i+3],groundPoints[i+4],groundPoints[i+5]);
				xm.sub(v, xm);
				
				if (yval > 0){
					Vector3 ym = new Vector3(groundPoints[i-3*(XNUM)],groundPoints[i-3*(XNUM)+1],groundPoints[i-3*(XNUM)+2]);
					ym.sub(ym, v);
					
					Vector3 cross = new Vector3();
					cross.cross(xm, ym);
					n.add(n, cross);
				}
				
				if (yval < YNUM -1){
					Vector3 ym = new Vector3(groundPoints[i+3*(XNUM)],groundPoints[i+3*(XNUM)+1],groundPoints[i+3*(XNUM)+2]);
					ym.sub(v,ym);
					
					Vector3 cross = new Vector3();
					cross.cross(xm, ym);
					n.add(n, cross);
				}
			}
			
			n.normalize();
			
			vertexBuffer.put(v.x);
			vertexBuffer.put(v.y);
			vertexBuffer.put(v.z);
			
			vertexBuffer.put(n.x);
			vertexBuffer.put(n.y);
			vertexBuffer.put(n.z);
			
			vertexBuffer.put((float)(xval%2));
			vertexBuffer.put((float)(yval%2));
		}

		final int t1 = XNUM-1;
		final int t2 = t1+EDGE_WIDTH;
		final int t3 = t2+(YNUM-1);
		final int t4 = t3+EDGE_WIDTH;
		final int t5 = t4+(XNUM-1);
		final int t6 = t5+EDGE_WIDTH;
		final int t7 = t6+(YNUM-1);
		final int t8 = t7+EDGE_WIDTH;

		for (int i = 3*XNUM*YNUM; i< 3*(XNUM*YNUM+2*EDGE_WIDTH*(XNUM+YSIZE)); i+=3){
			int xval = (i/3)% EDGE_WIDTH;
			int yval = (i/3)/EDGE_WIDTH;

			Vector3 p = new Vector3();

			if (xval < t1){
				p = new Vector3(EDGE_WIDTH+xval,EDGE_WIDTH,EDGE_WIDTH);
			}
			else if(xval < t2){
				p = new Vector3(XSIZE-1-EDGE_WIDTH,EDGE_WIDTH,EDGE_WIDTH);
			}
			else if (xval < t3){
				p = new Vector3(XSIZE-1-EDGE_WIDTH,EDGE_WIDTH,EDGE_WIDTH+(xval-t2));
			}
			else if (xval < t4){
				p = new Vector3(XSIZE-1-EDGE_WIDTH,EDGE_WIDTH,YSIZE-1-EDGE_WIDTH);
			}
			else if (xval < t5){
				p = new Vector3(XSIZE-1-(xval-t4),EDGE_WIDTH,YSIZE-1-EDGE_WIDTH);
			}
			else if (xval < t6){
				p = new Vector3(EDGE_WIDTH,EDGE_WIDTH,YSIZE-1-EDGE_WIDTH);
			}
			else if (xval < t7){
				p = new Vector3(EDGE_WIDTH,EDGE_WIDTH,YSIZE-1-EDGE_WIDTH-(xval-t6));
			}
			else if (xval < t8){
				p = new Vector3(EDGE_WIDTH,EDGE_WIDTH,EDGE_WIDTH);
			}

			p.scale(scale);

			Vector3 v = new Vector3(groundPoints[i],groundPoints[i+1],groundPoints[i+2]);

			Vector3 n = new Vector3();

			n.sub(p,v);
			n.normalize();

			vertexBuffer.put(v.x);
			vertexBuffer.put(v.y);
			vertexBuffer.put(v.z);

			vertexBuffer.put(n.x);
			vertexBuffer.put(n.y);
			vertexBuffer.put(n.z);

			vertexBuffer.put((float)(xval%2));
			vertexBuffer.put((float)(yval%2));
		}

		vertexBuffer.position(0);
		
		GLES20.glGenBuffers(1, vertBuf, 0);
		
		vertBufInd = vertBuf[0];
		
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertBufInd);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, groundPoints.length/3*8*4, vertexBuffer, GLES20.GL_STATIC_DRAW);
		
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
		
		int numTriangles = groundInds.length/3;
		indexCount = numTriangles*3;
		
		ByteBuffer indexBuffer = ByteBuffer.allocateDirect(indexCount*2).order(ByteOrder.nativeOrder());
		
		for (int i : groundInds){
			indexBuffer.putShort((short)i);
		}
		indexBuffer.position(0);	
		
		GLES20.glGenBuffers(1, faceBuf, 0);
		
		faceBufInd = faceBuf[0];
		
		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, faceBufInd);
		GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexCount*2, indexBuffer, GLES20.GL_STATIC_DRAW);
		
		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

	// Generates the OpenGL buffers given an object's OpenGL relevant info
    private ArrayList<Integer> generateBuffers(ArrayList<Float> verts, ArrayList<Float> norms, ArrayList<Integer> inds, ArrayList<Float> uv){
    	final int vertBuf[] = new int[1];
    	final int faceBuf[] = new int[1];
    	
    	int size = 8;
    	if (norms == null) size = 5;
    	if (uv == null) size = 3;
    	
    	ArrayList<Integer> out = new ArrayList<>();
    	
		ByteBuffer byteBuf = ByteBuffer.allocateDirect((verts.size()/3) * size * 4);
		byteBuf.order(ByteOrder.nativeOrder());
		FloatBuffer vertexBuffer = byteBuf.asFloatBuffer();
		
		for (int i=0; i<verts.size(); i+=3){
			vertexBuffer.put(verts.get(i));
			vertexBuffer.put(verts.get(i+1));
			vertexBuffer.put(verts.get(i+2));
			
			if (norms != null){
				vertexBuffer.put(norms.get(i));
				vertexBuffer.put(norms.get(i+1));
				vertexBuffer.put(norms.get(i+2));
			}
			
			if (uv != null){
				vertexBuffer.put(uv.get(2*(i/3)));
				vertexBuffer.put(uv.get(2*(i/3)+1));
			}
		}
		vertexBuffer.position(0);
		
		GLES20.glGenBuffers(1, vertBuf, 0);
		
		out.add(vertBuf[0]);
		
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertBuf[0]);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, verts.size()/3*size*4, vertexBuffer, GLES20.GL_STATIC_DRAW);
		
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
		
		int ic = inds.size();
		
		ByteBuffer indexBuffer = ByteBuffer.allocateDirect(ic*2).order(ByteOrder.nativeOrder());
		
		for (int i : inds){
			indexBuffer.putShort((short)i);
		}
		indexBuffer.position(0);	
		
		GLES20.glGenBuffers(1, faceBuf, 0);
		
		out.add(faceBuf[0]);
		
		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, faceBuf[0]);
		GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, ic*2, indexBuffer, GLES20.GL_STATIC_DRAW);
		
		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
		
		out.add(ic);
		
		return out;
    }
}