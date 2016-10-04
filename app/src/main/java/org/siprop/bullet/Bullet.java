/*
Bullet Continuous Collision Detection and Physics Library for Android NDK
Copyright (c) 2006-2009 Noritsuna Imamura  http://www.siprop.org/

This software is provided 'as-is', without any express or implied warranty.
In no event will the authors be held liable for any damages arising from the use of this software.
Permission is granted to anyone to use this software for any purpose,
including commercial applications, and to alter it and redistribute it freely,
subject to the following restrictions:

1. The origin of this software must not be misrepresented; you must not claim that you wrote the original software. If you use this software in a product, an acknowledgment in the product documentation would be appreciated but is not required.
2. Altered source versions must be plainly marked as such, and must not be misrepresented as being the original software.
3. This notice may not be removed or altered from any source distribution.
*/
package org.siprop.bullet;

import java.util.HashMap;
import java.util.Map;

import org.siprop.bullet.interfaces.Constraint;
import org.siprop.bullet.interfaces.DynamicsWorld;
import org.siprop.bullet.interfaces.ResultSimulationCallback;
import org.siprop.bullet.interfaces.Shape;
import org.siprop.bullet.interfaces.Solver;
import org.siprop.bullet.util.Point3;
import org.siprop.bullet.util.ShapeType;
import org.siprop.bullet.util.Vector3;

import android.util.Log;


public class Bullet {
	private long prevTime = -1;
	private static long timeDiff = -1;

	private Map<Integer, PhysicsWorld> physicsWorlds = new HashMap<Integer, PhysicsWorld>();
	private Map<Integer, Geometry> geometries = new HashMap<Integer, Geometry>();
	private Map<Integer, RigidBody> rigidBodies = new HashMap<Integer, RigidBody>();
	
	// PhysicsWorld
	private PhysicsWorld defaultPhysicsWorld;
	
	public void setDefaultPhysicsWorld(PhysicsWorld defaultPhysicsWorld) {
		this.defaultPhysicsWorld = defaultPhysicsWorld;
	}
	public PhysicsWorld getDefaultPhysicsWorld() {
		return defaultPhysicsWorld;
	}
	
	public Map<Integer, PhysicsWorld> getPhysicsWorlds() {
		return physicsWorlds;
	}
	public PhysicsWorld getPhysicsWorld(int id) {
		return physicsWorlds.get(id);
	}
	public PhysicsWorld createPhysicsWorld(Vector3 worldAabbMin,
										  Vector3 worldAabbMax, 
										  int maxProxies, 
										  Vector3 gravity) {
		
		PhysicsWorld phyWorld = new PhysicsWorld();
		phyWorld.worldAabbMin = worldAabbMin;
		phyWorld.worldAabbMax = worldAabbMax;
		phyWorld.maxProxies = maxProxies;
		phyWorld.gravity = gravity;

		phyWorld.id = createNonConfigPhysicsWorld(phyWorld);
		
		defaultPhysicsWorld = phyWorld;
		physicsWorlds.put(phyWorld.id, phyWorld);
		return phyWorld;
	}
	
	public PhysicsWorld createPhysicsWorld(CollisionConfiguration cllisionConfiguration,
								   CollisionDispatcher collisionDispatcher,
								   Solver solver,
								   DynamicsWorld dynamicsWorld,
								   Vector3 worldAabbMin,
								   Vector3 worldAabbMax, 
								   int maxProxies, 
								   Vector3 gravity) {
		
		PhysicsWorld phyWorld = new PhysicsWorld();
		phyWorld.collisionConfiguration = cllisionConfiguration;
		phyWorld.collisionDispatcher = collisionDispatcher;
		phyWorld.solver = solver;
		phyWorld.dynamicsWorld = dynamicsWorld;
		phyWorld.worldAabbMin = worldAabbMin;
		phyWorld.worldAabbMax = worldAabbMax;
		phyWorld.maxProxies = maxProxies;
		phyWorld.gravity = gravity;
		
		phyWorld.id = createPhysicsWorld(phyWorld);
		
		defaultPhysicsWorld = phyWorld;
		physicsWorlds.put(phyWorld.id, phyWorld);
		return phyWorld;
	}
	
	public native int createNonConfigPhysicsWorld(PhysicsWorld physicsWorld);
	public native int createPhysicsWorld(PhysicsWorld physicsWorld);
	
	public native int changePhysicsWorldConfiguration(PhysicsWorld physicsWorld);
	
	

	// Geometry
	public Map<Integer, Geometry> getGeometries() {
		return geometries;
	}
	public Geometry getGeometry(int id) {
		return geometries.get(id);
	}
	public Geometry createGeometry(Shape collisionShape,
								   float mass) {
		
		Geometry geometry = new Geometry();
		geometry.shape = collisionShape;
		geometry.mass = mass;
		geometry.localInertia = new Vector3(0,0,0);
		
		if (collisionShape.getType() == ShapeType.CONVEX_HULL_SHAPE_PROXYTYPE){
			geometry.id = createGeometry(geometry,((org.siprop.bullet.shape.ConvexHullShape)collisionShape).verts,null);
		}
		else if (collisionShape.getType() == ShapeType.TERRAIN_SHAPE_PROXYTYPE){
			geometry.id = createGeometry(geometry,((org.siprop.bullet.shape.HeightfieldTerrainShape)collisionShape).heights,null);	
		}
		else if (collisionShape.getType() == ShapeType.TRIANGLE_MESH_SHAPE_PROXYTYPE){
			geometry.id = createGeometry(geometry,((org.siprop.bullet.shape.TriangleMeshShape)collisionShape).points,((org.siprop.bullet.shape.TriangleMeshShape)collisionShape).inds);	
		}
		else{
			geometry.id = createGeometry(geometry,null,null);
		}
		
		geometry.shape.setID(geometry.id);
		geometries.put(geometry.id, geometry);
		return geometry;
	}
	
	public native int createGeometry(Geometry geometry,float[] arr, int[] inds);

	public void makeDynamic(RigidBody body, float mass) {
		if(body.id != 0 && body.physicsWorldId != 0) {
			makeDynamic(mass, body.physicsWorldId, body.id);
		}
	}
	
	private native int makeDynamic(float mass, int physicsworldid, int rigidbodyid);
	
	public void makeKinematic(RigidBody body) {
		if(body.id != 0 && body.physicsWorldId != 0) {
			makeKinematic(body.physicsWorldId, body.id);
		}
	}
	
	private native int makeKinematic(int physicsworldid, int rigidbodyid);
	
	// RigidBody
	public RigidBody createAndAddRigidBody(Geometry geometry,
										   MotionState motionState) {
		return createAndAddRigidBody(defaultPhysicsWorld, geometry, motionState);
	}
	public RigidBody createAndAddRigidBody(PhysicsWorld physicsWorld, 
			Geometry geometry,
			MotionState motionState) {
		
		RigidBody rigidBody = new RigidBody();
		rigidBody.geometry = geometry;
		rigidBody.motionState = motionState;
		rigidBody.physicsWorldId = physicsWorld.id;
		rigidBody.id = createAndAddRigidBody(physicsWorld.id, rigidBody);
		
		rigidBodies.put(rigidBody.id , rigidBody);
		
		return rigidBody;
	}
	public RigidBody createAndAddRigidBody(RigidBody rigidBody) {
		rigidBody.physicsWorldId = defaultPhysicsWorld.id;
		rigidBody.id = createAndAddRigidBody(rigidBody.physicsWorldId, rigidBody);
		rigidBodies.put(rigidBody.id , rigidBody);
		return rigidBody;
	}
	public native int createAndAddRigidBody(int physicsWorldId, RigidBody rigidBody);

	public void removeRigidBody(RigidBody body) {
		if(body.physicsWorldId >= 0) {
			removeRigidBody(body.physicsWorldId, body);
			body.physicsWorldId = 0;
			rigidBodies.remove(body.id);
		}
	}
	public native void removeRigidBody(int worldID, RigidBody body);	


	public boolean changeTrans(RigidBody body, float[] trans) {
		if(body.id != 0 && body.physicsWorldId != 0) {
			int temp = changeTrans(body.physicsWorldId, body.id, trans);
			if (temp != 0) return true;
		}
		return false;
	}
	public native int changeTrans(int physicsWorldId, int rigidBodyId, float[] trans);
	
	public void zeroVelocities(RigidBody body) {
		zeroVelocities(body.physicsWorldId ,body.id);
	}
	
	private native void zeroVelocities(int physicsWorldId, int rigidBodyId);
	
	public void setAngVel(RigidBody body, Vector3 angVel){
		setAngVel(body.physicsWorldId, body.id, angVel);
	}
	
	private native void setAngVel(int physicsWorldId, int rigidBodyId, Vector3 angVel);
	
	public void setLinVel(RigidBody body, Vector3 linVel){
		setLinVel(body.physicsWorldId, body.id, linVel);
	}
	
	private native void setLinVel(int physicsWorldId, int rigidBodyId, Vector3 linVel);
	
	// applyForce
	public void applyForce(RigidBody body, Vector3 force, Vector3 applyPoint) {
		if(body.id != 0 && body.physicsWorldId != 0) {
			applyForce(body.physicsWorldId, body.id, force, applyPoint);
		}
	}
	public native int applyForce(int physicsWorldId, int rigidBodyId, Vector3 force, Vector3 applyPoint);

	public void applyCentralForce(RigidBody body, Vector3 force) {
		if(body.id != 0 && body.physicsWorldId != 0) {
			applyCentralForce(body.physicsWorldId, body.id, force);
		}
	}
	public native int applyCentralForce(int physicsWorldId, int rigidBodyId, Vector3 force);
	
	public void applyTorque(RigidBody body, Vector3 torque) {
		if(body.id != 0 && body.physicsWorldId != 0) {
			applyTorque(body.physicsWorldId, body.id, torque);
		}
	}
	public native int applyTorque(int physicsWorldId, int rigidBodyId, Vector3 torque);
	
	
	public void applyCentralImpulse(RigidBody body, Vector3 impulse) {
		if(body.id != 0 && body.physicsWorldId != 0) {
			applyCentralImpulse(body.physicsWorldId, body.id, impulse);
		}
	}
	public native int applyCentralImpulse(int physicsWorldId, int rigidBodyId, Vector3 impulse);

	
  	public void applyTorqueImpulse(RigidBody body, Vector3 torque) {
		if(body.id != 0 && body.physicsWorldId != 0) {
			applyTorqueImpulse(body.physicsWorldId, body.id, torque);
		}
	}
	public native int applyTorqueImpulse(int physicsWorldId, int rigidBodyId, Vector3 torque);
	
	public void applyImpulse(RigidBody body, Vector3 impulse, Vector3 applyPoint)  {
		if(body.id != 0 && body.physicsWorldId != 0) {
			applyImpulse(body.physicsWorldId, body.id, impulse, applyPoint);
		}
	}
	public native int applyImpulse(int physicsWorldId, int rigidBodyId, Vector3 impulse, Vector3 applyPoint);

	
	public void clearForces(RigidBody body) {
		if(body.id != 0 && body.physicsWorldId != 0) {
			clearForces(body.physicsWorldId, body.id);
		}
	}
	public native int clearForces(int physicsWorldId, int rigidBodyId);
	

	public void setActive(PhysicsWorld physicsWorld, boolean isActive) {
		setActivePhysicsWorldAll(physicsWorld.id, isActive);
	}
	public void setActive(RigidBody body, boolean isActive) {
		setActive(body.physicsWorldId, body.id, isActive);
	}
	public native int setActive(int physicsWorldId, int rigidBodyId, boolean isActive);
	public native int setActivePhysicsWorldAll(int physicsWorldId, boolean isActive);
	public native int setActiveAll(boolean isActive);

	public boolean isActive(RigidBody r){
		return isActive(r.physicsWorldId,r.id);
	}
	
	public native boolean isActive(int physicsworldid, int rigidbodyid);
	
	// addConstraint
	public native int addConstraint(Constraint constraint);
		
	
	public Map<Integer, RigidBody> doSimulation(float execTime, int count) {
		return doSimulation(defaultPhysicsWorld, count);
	}
	public Map<Integer, RigidBody> doSimulation(PhysicsWorld physicsWorld, int count) {
		doSimulationNative(physicsWorld.id, count);
		return rigidBodies;
	}
	public Map<Integer, RigidBody> doSimulationWithCallback(ResultSimulationCallback resultCallback, float execTime, int count) {
		return doSimulationWithCallback(resultCallback, defaultPhysicsWorld, execTime, count);
	}
	public Map<Integer, RigidBody> doSimulationWithCallback(ResultSimulationCallback resultCallback, PhysicsWorld physicsWorld, float execTime, int count) {
		doSimulationNative(physicsWorld.id, count);
		if(resultCallback != null) {
			resultCallback.resultSimulation(rigidBodies);
		}
		return rigidBodies;
	}
	private native int doSimulationNative(int worldId, int count); 

	public boolean isColliding(RigidBody a, RigidBody b){
		return isColliding(a.physicsWorldId, a.id, b.id);
	}
	
	private native boolean isColliding(int physicsWorldId, int rigidBodyId0, int rigidBodyId1);
	
	private void resultSimulation(int rigidBodyID, int shapeType, float[] rot, float[] pos, float[] vel, float[] angVel, float[] shapeOption) {
		RigidBody body = rigidBodies.get(rigidBodyID);
		if(body == null) {
			Log.d("resultSimulation", "body is null.");
			return;
		}
		if(rot.length < 9) {
			Log.d("resultSimulation", "rot is " + rot.length);
			return;
		}
		if(pos.length < 3) {
			Log.d("resultSimulation", "pos is " + pos.length);
			return;
		}
		body.motionState.resultSimulation.basis.xx = rot[0];
		body.motionState.resultSimulation.basis.xy = rot[1];
		body.motionState.resultSimulation.basis.xz = rot[2];
		body.motionState.resultSimulation.basis.yx = rot[3];
		body.motionState.resultSimulation.basis.yy = rot[4];
		body.motionState.resultSimulation.basis.yz = rot[5];
		body.motionState.resultSimulation.basis.zx = rot[6];
		body.motionState.resultSimulation.basis.zy = rot[7];
		body.motionState.resultSimulation.basis.zz = rot[8];
		
		body.motionState.resultSimulation.originPoint.x = pos[0];
		body.motionState.resultSimulation.originPoint.y = pos[1];
		body.motionState.resultSimulation.originPoint.z = pos[2];
		
		body.motionState.resultSimulation.vel.x = vel[0];
		body.motionState.resultSimulation.vel.y = vel[1];
		body.motionState.resultSimulation.vel.z = vel[2];
		
		body.motionState.resultSimulation.angVel.x = angVel[0];
		body.motionState.resultSimulation.angVel.y = angVel[1];
		body.motionState.resultSimulation.angVel.z = angVel[2];
		
		body.motionState.resultSimulation.option_param[0] = shapeOption[0];
		body.motionState.resultSimulation.option_param[1] = shapeOption[1];
		body.motionState.resultSimulation.option_param[2] = shapeOption[2];
		body.motionState.resultSimulation.option_param[3] = shapeOption[3];
		body.motionState.resultSimulation.option_param[4] = shapeOption[4];
		body.motionState.resultSimulation.option_param[5] = shapeOption[5];
		body.motionState.resultSimulation.option_param[6] = shapeOption[6];
		body.motionState.resultSimulation.option_param[7] = shapeOption[7];
		body.motionState.resultSimulation.option_param[8] = shapeOption[8];
//		if(shapeType == ShapeType.BOX_SHAPE_PROXYTYPE) {
//		} else if(shapeType == ShapeType.STATIC_PLANE_PROXYTYPE) {
//		} else if(shapeType == ShapeType.SPHERE_SHAPE_PROXYTYPE) {
//		} else if(shapeType == ShapeType.CAPSULE_SHAPE_PROXYTYPE) {
//		} else if(shapeType == ShapeType.CONE_SHAPE_PROXYTYPE) {
//		} else if(shapeType == ShapeType.CYLINDER_SHAPE_PROXYTYPE) {
//		} else if(shapeType == ShapeType.TETRAHEDRAL_SHAPE_PROXYTYPE) {
//		} else if(shapeType == ShapeType.EMPTY_SHAPE_PROXYTYPE) {
//		} else if(shapeType == ShapeType.TRIANGLE_SHAPE_PROXYTYPE) {
//		}
	}
	
	
	public void destory() {
		defaultPhysicsWorld = null;
		destroyNative();
	}
	public native void destroyPhysicsWorld(PhysicsWorld physicsWorld);
	private native void destroyNative();
	
	public long setTime(long newTime){
		long p = prevTime;
		prevTime = newTime;
		timeDiff = newTime-p;
		return timeDiff;
	}

	public static long getTimeDiff(){
		return timeDiff;
	}

    static {
        System.loadLibrary("bullet");
    }
}
