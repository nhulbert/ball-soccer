/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package game;

import android.opengl.GLES20;

import org.siprop.bullet.Transform;
import org.siprop.bullet.util.Vector3;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;


/**
 *
 * @author Neil
 */
public class Entity {
    protected ArrayList<Vector3> verts;
    protected ArrayList<ArrayList<Integer>> faces;
    protected ArrayList<Vector3> normals;
    protected ArrayList<Float> uv;
    
    protected PhysObject physObj;

	protected float mass;

    public float[] mat;
    public float[] prevMat;

    FloatBuffer vertexBuffer;
	ByteBuffer indexBuffer;
    
	int texnum;

	boolean fullBright;

	float curPosX=0;
	float curPosY=0;
	float curPosZ=0;

    
	static final int POSITION_DATA_SIZE = 3;	

	static final int NORMAL_DATA_SIZE = 3;

	static final int TEXTURE_COORDINATE_DATA_SIZE = 2;	
    
	static final int STRIDE = POSITION_DATA_SIZE + NORMAL_DATA_SIZE + TEXTURE_COORDINATE_DATA_SIZE;
	
	static HashMap<Integer,Integer> bufInds = new HashMap<>();
	
	int vertBufInd;
	int faceBufInd;

	public float curVelX;
	public float prevVelX;
	public float curVelY;
	public float prevVelY;
	public float curVelZ;
	public float prevVelZ;

	public float curAngVelX;
	public float prevAngVelX;
	public float curAngVelY;
	public float prevAngVelY;
	public float curAngVelZ;
	public float prevAngVelZ;

	private boolean isLocal = true;

	public int uniqueID = -1;

	public static HashMap<Integer, Entity> ids = new HashMap<>();
	public static ArrayList<AddressPort> addressPorts = new ArrayList<>();

	private AddressPort addressPort;

    public Entity(ArrayList<Vector3> verts, float[] trans, float mass, ArrayList<ArrayList<Integer>> faces, ArrayList<Vector3> normals, ArrayList<Float> uv, int texnum, Integer bufInd, World world){
    	this.verts = verts;
        this.faces = faces;
        this.normals = normals;
        this.uv = uv;
        this.texnum = texnum;        

		fullBright = true;

		this.mass = mass;

        if (bufInd == null || !bufInds.keySet().contains(bufInd)){
        	createBuffers();
        	bufInds.put(vertBufInd,faceBufInd);
        }
        else{
        	this.vertBufInd = bufInd;
            this.faceBufInd = bufInds.get(bufInd);
        }
        
        if (world != null) physObj = world.addConvexHullObj(verts,trans,mass);
    }

	public Entity(ArrayList<Vector3> verts, float[] trans, float mass, ArrayList<ArrayList<Integer>> faces, ArrayList<Vector3> normals, ArrayList<Float> uv, int texnum, Integer bufInd, World world, boolean convex){
		this.verts = verts;
		this.faces = faces;
		this.normals = normals;
		this.uv = uv;
		this.texnum = texnum;

		fullBright = true;

		this.mass = mass;

		if (bufInd == null || !bufInds.keySet().contains(bufInd)){
			createBuffers();
			bufInds.put(vertBufInd,faceBufInd);
		}
		else{
			this.vertBufInd = bufInd;
			this.faceBufInd = bufInds.get(bufInd);
		}

		if (world != null){
			if (convex) physObj = world.addConvexHullObj(verts,trans,mass);
			else physObj = world.addTriangleMeshObj(verts,faces,trans,mass);
		}
	}

	public Entity(ArrayList<Vector3> verts, float[] trans, float mass, ArrayList<ArrayList<Integer>> faces, ArrayList<Vector3> normals, ArrayList<Float> uv, int texnum, boolean fullBright, Integer bufInd, World world, boolean convex, boolean local, int uniqueID, AddressPort addressPort){
		this.verts = verts;
		this.faces = faces;
		this.normals = normals;
		this.uv = uv;
		this.texnum = texnum;
		this.fullBright = fullBright;

		this.mass = mass;

		this.isLocal = local;

		this.uniqueID = uniqueID;

		if (bufInd == null || !bufInds.keySet().contains(bufInd)){
			createBuffers();
			bufInds.put(vertBufInd,faceBufInd);
		}
		else{
			this.vertBufInd = bufInd;
			this.faceBufInd = bufInds.get(bufInd);
		}

		if (world != null){
			if (convex) physObj = world.addConvexHullObj(verts,trans,mass);
			else physObj = world.addTriangleMeshObj(verts,faces,trans,mass);
		}

		ids.put(uniqueID,this);
		if (addressPort != null && !addressPorts.contains(addressPort)) addressPorts.add(addressPort);
		this.addressPort = addressPort;
	}

	public void removeFromPhysicsWorld(){
		PhysObject.bullet.removeRigidBody(physObj.rigidBody);
	}

    public void draw(float[] mViewMatrix, float[] mProjectionMatrix){        
        draw(mViewMatrix, mProjectionMatrix, false, true);
    }
	public void draw(float[] mViewMatrix, float[] mProjectionMatrix, boolean selected){
        draw(mViewMatrix, mProjectionMatrix, selected, true);
    }

    public void draw(float[] mViewMatrix, float[] mProjectionMatrix, boolean selected, boolean reflections){
		if (physObj != null){
        	if (reflections) Draw.drawReflection(vertBufInd, faceBufInd, faces.size()*3, mViewMatrix, mProjectionMatrix, mat, texnum, 2);
        	else Draw.draw(vertBufInd, faceBufInd, faces.size()*3, mViewMatrix, mProjectionMatrix, mat, texnum);
		}
		else{
			float[] mat = {1,0,0,0,
					0,1,0,0,
					0,0,1,0,
					0,0,0,1};
			if (fullBright) {
				if (selected)
					Draw.drawFullbright(vertBufInd, faceBufInd, faces.size() * 3, mViewMatrix, mProjectionMatrix, mat, texnum + 1);
				else
					Draw.drawFullbright(vertBufInd, faceBufInd, faces.size() * 3, mViewMatrix, mProjectionMatrix, mat, texnum);
			}
			else{
				if (reflections) Draw.drawReflection(vertBufInd, faceBufInd, faces.size()*3, mViewMatrix, mProjectionMatrix, mat, texnum, 2);
				else Draw.draw(vertBufInd, faceBufInd, faces.size()*3, mViewMatrix, mProjectionMatrix, mat, texnum);
			}
		}
    }
    
    private void createBuffers(){
		final int vertBuf[] = new int[1];
    	final int faceBuf[] = new int[1];

		ByteBuffer byteBuf = ByteBuffer.allocateDirect(verts.size() * 8 * 4);
		byteBuf.order(ByteOrder.nativeOrder());
		vertexBuffer = byteBuf.asFloatBuffer();
		for (int i=0; i<verts.size(); i++){
			Vector3 v = verts.get(i);
			Vector3 n = normals.get(i);
			
			vertexBuffer.put(v.x);
			vertexBuffer.put(v.y);
			vertexBuffer.put(v.z);
			
			vertexBuffer.put(n.x);
			vertexBuffer.put(n.y);
			vertexBuffer.put(n.z);
			
			vertexBuffer.put(uv.get(i*2));
			vertexBuffer.put(uv.get(i*2+1));
		}
		vertexBuffer.position(0);
		
		GLES20.glGenBuffers(1, vertBuf, 0);
		
		vertBufInd = vertBuf[0];
		
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertBufInd);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, verts.size()*STRIDE*4, vertexBuffer, GLES20.GL_STATIC_DRAW);
		
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
		
		indexBuffer = ByteBuffer.allocateDirect(faces.size() * 3 * 2).order(ByteOrder.nativeOrder());
		for (ArrayList<Integer> f : faces){
			for (Integer i : f){
				indexBuffer.putShort((short)((int)i));
			}
		}
		indexBuffer.position(0);	
		
		GLES20.glGenBuffers(1, faceBuf, 0);
		
		faceBufInd = faceBuf[0];
		
		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, faceBufInd);
		GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, faces.size()*3*2, indexBuffer, GLES20.GL_STATIC_DRAW);
		
		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

	public void makeKinematic() {
		PhysObject.bullet.makeKinematic(physObj.rigidBody);
	}
	
	public void makeDynamic(float mass){
		PhysObject.bullet.makeDynamic(physObj.rigidBody, mass);
	}
	
	public void applyCentralImpulse(Vector3 v){
		PhysObject.bullet.setActive(physObj.rigidBody, true);
		PhysObject.bullet.applyCentralImpulse(physObj.rigidBody, v);
	}
	public void applyTorqueImpulse(Vector3 v){
		PhysObject.bullet.applyTorqueImpulse(physObj.rigidBody, v);
	}
	
	public void setPosition(Vector3 v) {
		float[] m = {1f,0,0,0,
					 0,1f,0,0,
					 0,0,1f,0,
					 v.x,v.y,v.z,1f};
		
		
		PhysObject.bullet.makeKinematic(physObj.rigidBody);
		PhysObject.bullet.changeTrans(physObj.rigidBody, m);
		PhysObject.bullet.makeDynamic(physObj.rigidBody, mass);
	}

	public void setMat(float[] m){
		PhysObject.bullet.makeKinematic(physObj.rigidBody);
		PhysObject.bullet.changeTrans(physObj.rigidBody, m);
		PhysObject.bullet.makeDynamic(physObj.rigidBody, mass);
	}

	public void update() {
		if (physObj != null){
			Transform t = physObj.rigidBody.motionState.resultSimulation;

			prevMat = mat;

			prevVelX = curVelX;
			prevVelY = curVelY;
			prevVelZ = curVelZ;

			prevAngVelX = curAngVelX;
			prevAngVelY = curAngVelY;
			prevAngVelZ = curAngVelZ;

			curPosX = t.originPoint.x;
			curPosY = t.originPoint.y;
			curPosZ = t.originPoint.z;

			curVelX = t.vel.x;
			curVelY = t.vel.y;
			curVelZ = t.vel.z;

			curAngVelX = t.angVel.x;
			curAngVelY = t.angVel.y;
			curAngVelZ = t.angVel.z;

			mat = new float[]{t.basis.xx, t.basis.xy, t.basis.xz, 0,
					t.basis.yx, t.basis.yy, t.basis.yz, 0,
					t.basis.zx, t.basis.zy, t.basis.zz, 0,
					t.originPoint.x, t.originPoint.y, t.originPoint.z, 1};
		}
	}

	public void stepBack(){
		mat = Arrays.copyOf(prevMat,16);

		curVelX = prevVelX;
		curVelY = prevVelY;
		curVelZ = prevVelZ;

		curAngVelX = prevAngVelX;
		curAngVelY = prevAngVelY;
		curAngVelZ = prevAngVelZ;

		changeTrans(mat);
		this.setLinVel(new Vector3(curVelX,curVelY,curVelZ));
		this.setAngVel(new Vector3(curAngVelX,curAngVelY,curAngVelZ));
	}

	public boolean changeTrans(float[] m){
		return PhysObject.bullet.changeTrans(physObj.rigidBody, m);
	}
	
	public void setAngVel(Vector3 v){
		PhysObject.bullet.setAngVel(physObj.rigidBody, v);
	}
	
	public void setLinVel(Vector3 v){
		PhysObject.bullet.setLinVel(physObj.rigidBody, v);
	}
	
	public boolean isActive(){
		return PhysObject.bullet.isActive(physObj.rigidBody);
	}

	public void zeroVelocities() {
		PhysObject.bullet.zeroVelocities(physObj.rigidBody);
	}
	
	public static boolean isColliding(Entity a, Entity b){
		return PhysObject.bullet.isColliding(a.physObj.rigidBody, b.physObj.rigidBody);
	}
	
	public PhysObject getPhysObj(){
		return physObj;
	}

	public boolean isLocal(){
		return isLocal;
	}

	public AddressPort getAddressPort(){
		return addressPort;
	}

	public void setAddressPort(AddressPort a){
		addressPort = a;
		if (a != null) addressPorts.add(a);
	}

	public int getUniqueID(){
		return uniqueID;
	}

	public void setID(int id) {
		this.uniqueID = id;
	}

	public void setTexnum(int texnum){
		this.texnum = texnum;
	}

	public float[] getMat(){
		return mat;
	}

	/*public ArrayList<Entity> splitAlongPlane(Vector3 point, Vector3 normal){
		ArrayList<Entity> out = new ArrayList<>();
		
		HashSet<Integer> m = new HashSet<>();
		HashSet<Integer> e = new HashSet<>();
		HashSet<Integer> l = new HashSet<>();
		
		float ref = point.dot(normal);
		
		for (int i=0; i<verts.size(); i++){
			Vector3 p = verts.get(i);
			
			int res = Float.compare(p.dot(normal), ref);
			switch(res){
				case 1:
					m.add(i);
					break;
				case -1:
					l.add(i);
					break;
				case 0:
					e.add(i);
			}
		}
		
		for (ArrayList<Integer> i : faces){
			int mc=0;
			int ec=0;
			int lc=0;
			
			int mi=-1;
			int ei =-1;
			int li = -1;
			
			for (Integer j : i){
				if (m.contains(j)){
					mc++;
					mi = j;
				}
				else if (e.contains(j)) {
					ec++;
					ei = j;
				}
				else if (l.contains(j)){
					lc++;
					li = j;
				}
			}
			
			
			
			if (mc == 2){				
				if (lc == 1){
					int ind = i.indexOf(li);
					Vector3 o = verts.get(li);
					
					Vector3 a = verts.get(i.get((ind+1)%3));
					Vector3 b = verts.get(i.get((ind+2)%3));
					
					Vector3 v1 = new Vector3();
					Vector3 v2 = new Vector3();
					
					v1.sub(a, o);
					v2.sub(b, o);
					
					Vector3 t = new Vector3();
					t.sub(point, o);
					
					float x1 = t.dot(normal)/v1.dot(normal);
					float x2 = t.dot(normal)/v2.dot(normal);
					
					Vector3 int1 = new Vector3(v1);
					Vector3 int2 = new Vector3(v2);
					
					int1.scale(x1);
					int2.scale(x2);
					
					int1.add(int1, o);
					int2.add(int2, o);
				}
			}
			else if (lc == 2){
				if (mc == 1){
					
				}
			}
			else if (mc == 1 && lc == 1 && ec == 1){
				
			}
		}
		
		return out;
	}*/
	
	private class Edge{
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((a == null) ? 0 : a.hashCode());
			result = prime * result + ((b == null) ? 0 : b.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Edge other = (Edge) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (a == null) {
				if (other.a != null)
					return false;
			} else if (!a.equals(other.a))
				return false;
			if (b == null) {
				if (other.b != null)
					return false;
			} else if (!b.equals(other.b))
				return false;
			return true;
		}

		Vector3 a;
		Vector3 b;
		
		public Edge(Vector3 a, Vector3 b){
			this.a = a;
			this.b = b;
		}

		private Entity getOuterType() {
			return Entity.this;
		}
	}
}
