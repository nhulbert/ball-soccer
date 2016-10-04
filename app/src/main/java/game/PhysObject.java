/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package game;

import org.siprop.bullet.Bullet;
import org.siprop.bullet.Geometry;
import org.siprop.bullet.MotionState;
import org.siprop.bullet.PhysicsWorld;
import org.siprop.bullet.RigidBody;
import org.siprop.bullet.interfaces.Shape;
import org.siprop.bullet.util.Vector3;

// The physics object in the Java portion of the JNI wrapper

public class PhysObject {
    public Shape shape;
    public MotionState motionState;
    public float mass;
    public Vector3 inertia;
    public RigidBody rigidBody;
    public PhysicsWorld ddw;
    public static Bullet bullet;
    
    public PhysObject(PhysicsWorld ddw, Shape cs, MotionState ms, float mass){
        this.ddw = ddw;
        shape = cs;
        motionState = ms;
        this.mass = mass;
        
        
    	Geometry floarGeom = bullet.createGeometry(cs,
					mass);
    	
        rigidBody = bullet.createAndAddRigidBody(ddw,floarGeom,ms);
    }
    
    public PhysObject(PhysicsWorld ddw, Shape cs, MotionState ms, float mass, float[] trans){
        this(ddw,cs,ms,mass);
        
        if (trans != null){
        	bullet.changeTrans(rigidBody, trans);
        }
    }
}