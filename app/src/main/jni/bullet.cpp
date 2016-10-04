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
#include "bullet.h"
#include "time.h"
#include <cstdint>
#include <stdint.h>

JNIEXPORT
jint
JNICALL
Java_org_siprop_bullet_Bullet_createNonConfigPhysicsWorld(JNIEnv* env,
                                                       jobject thiz,
                                                       jobject physicsWorld_obj) {

	btDefaultCollisionConfiguration* pCollisionConfig = new btDefaultCollisionConfiguration();
//	LOGV("Load btDefaultCollisionConfiguration.");


	btCollisionDispatcher* pCollisionDispatcher = new btCollisionDispatcher(pCollisionConfig);
//	LOGV("Load btCollisionDispatcher.");

//	LOGV("Load worldAabb.");
	btAxisSweep3* pWorldAabbCache = new btAxisSweep3(get_vec_by_JavaObj(env, physicsWorld_obj, "worldAabbMin"),
													 get_vec_by_JavaObj(env, physicsWorld_obj, "worldAabbMax"),
													 get_int_by_JavaObj(env, physicsWorld_obj, "maxProxies"));
//	LOGV("Load g_pWorldAabbCache.");

	btSequentialImpulseConstraintSolver* pSolver = new btSequentialImpulseConstraintSolver();

	btDiscreteDynamicsWorld* pDynamicsWorld = new btDiscreteDynamicsWorld(
		pCollisionDispatcher,
		pWorldAabbCache,
		pSolver,
		pCollisionConfig );

	if(!is_NULL_vec_field_JavaObj(env, physicsWorld_obj, "gravity")) {
		btVector3 gravity(get_vec_by_JavaObj(env, physicsWorld_obj, "gravity"));
		pDynamicsWorld->setGravity(gravity);
	//	LOGV("Load setGravity.");
	}
	g_DynamicsWorlds.push_back(pDynamicsWorld);

	jint addr_val = (intptr_t)pDynamicsWorld;
	set_JavaObj_int(env, physicsWorld_obj, "id", addr_val);

	return addr_val;
}

JNIEXPORT
jint
JNICALL
Java_org_siprop_bullet_Bullet_changePhysicsWorldConfiguration(JNIEnv* env,
                                                       jobject thiz,
                                                       jobject physicsWorld_obj) {

	int id = get_int_by_JavaObj(env, physicsWorld_obj, "id");
//	LOGV("Load physicsWorld_obj ID.");

	btDynamicsWorld* pDynamicsWorld = g_DynamicsWorlds.get((btDynamicsWorld*)id);	
	if(pDynamicsWorld == NULL) {
	//	LOGV("Don't Load DynamicsWorld.");
		return id;
	}
	
	jobject method_obj = get_obj_by_JavaObj(env, physicsWorld_obj, "dynamicsWorld", "Lorg/siprop/bullet/interfaces/DynamicsWorld;");
	int type = get_type_by_JavaObj(env, method_obj);
	if(type == BT_DISCRETE_DYNAMICS_WORLD) {
		btDiscreteDynamicsWorld* pDiscreteDynamicsWorld = (btDiscreteDynamicsWorld*)pDynamicsWorld;
	//	LOGV("Cast btDiscreteDynamicsWorld.");
		
		pDiscreteDynamicsWorld->setGravity(get_vec_by_JavaObj(env, physicsWorld_obj, "gravity"));
	//	LOGV("Load setGravity.");
		
	} else if(type == BT_CONTINUOUS_DYNAMICS_WORLD) {
		btContinuousDynamicsWorld* pContinuousDynamicsWorld = (btContinuousDynamicsWorld*)pDynamicsWorld;
	} else if(type == BT_SIMPLE_DYNAMICS_WORLD) {
		btSimpleDynamicsWorld* pSimpleDynamicsWorld = (btSimpleDynamicsWorld*)pDynamicsWorld;
	} else {
		return NULL;
	}
	
	return id;

}

JNIEXPORT
jint
JNICALL
Java_org_siprop_bullet_Bullet_makeDynamic(JNIEnv* env,
											jobject thiz,
											jfloat mass,
											jint physicsworldid,
											jint rigidbodyid){

	btDynamicsWorld* pDynamicsWorld = g_DynamicsWorlds.get((btDynamicsWorld*)physicsworldid);
	if(pDynamicsWorld == NULL) {
	//	LOGV("Don't Load pDynamicsWorld.");
		return 0;
	}

	btCollisionObject* obj = pDynamicsWorld->getCollisionObjectArray().get((btRigidBody*)rigidbodyid);
	if(obj == NULL) {
	//	LOGV("Don't Load btRigidBody.");
		return 0;
	}
	btRigidBody* body = btRigidBody::upcast(obj);

	pDynamicsWorld->removeRigidBody(body);

	body->setCollisionFlags(body->getCollisionFlags() & ~btCollisionObject::CF_KINEMATIC_OBJECT);
	body->setCollisionFlags(body->getCollisionFlags() & ~btCollisionObject::CF_STATIC_OBJECT);

	btVector3 inertia;
	body->getCollisionShape()->calculateLocalInertia((btScalar)mass, inertia);
	body->setMassProps(mass,inertia);

	pDynamicsWorld->addRigidBody(body);
	body->activate();
	body->forceActivationState(ACTIVE_TAG);


	return 1;
}

JNIEXPORT
jint
JNICALL
Java_org_siprop_bullet_Bullet_makeKinematic(JNIEnv* env,
										jobject thiz,
										jint physicsworldid,
										jint rigidbodyid){
	btDynamicsWorld* pDynamicsWorld = g_DynamicsWorlds.get((btDynamicsWorld*)physicsworldid);
	if(pDynamicsWorld == NULL) {
	//	LOGV("Don't Load pDynamicsWorld.");
		return 0;
	}

	btCollisionObject* obj = pDynamicsWorld->getCollisionObjectArray().get((btRigidBody*)rigidbodyid);
	if(obj == NULL) {
	//	LOGV("Don't Load btRigidBody.");
		return 0;
	}
	btRigidBody* body = btRigidBody::upcast(obj);

	pDynamicsWorld->removeRigidBody(body);

	//body->setCollisionFlags(body->getCollisionFlags() | btCollisionObject::CF_KINEMATIC_OBJECT);

	btVector3 inertia;
	body->getCollisionShape()->calculateLocalInertia((btScalar)0, inertia);
	body->setMassProps(0,inertia);

	body->setActivationState(DISABLE_DEACTIVATION);
	body->clearForces();

	pDynamicsWorld->addRigidBody(body);
	return 1;
}

JNIEXPORT
jboolean
JNICALL
Java_org_siprop_bullet_Bullet_isActive(JNIEnv* env,
										jobject thiz,
										jint physicsworldid,
										jint rigidbodyid){
	btDynamicsWorld* pDynamicsWorld = g_DynamicsWorlds.get((btDynamicsWorld*)physicsworldid);
	if(pDynamicsWorld == NULL) {
	//	LOGV("Don't Load pDynamicsWorld.");
		return 0;
	}

	btCollisionObject* obj = pDynamicsWorld->getCollisionObjectArray().get((btRigidBody*)rigidbodyid);
	if(obj == NULL) {
	//	LOGV("Don't Load btRigidBody.");
		return 0;
	}
	btRigidBody* body = btRigidBody::upcast(obj);

	return (jboolean)(body->isActive());
}

JNIEXPORT
jint
JNICALL
Java_org_siprop_bullet_Bullet_createGeometry(JNIEnv* env,
                                             jobject thiz,
                                             jobject geometry_obj,
                                             jfloatArray arr,
                                             jintArray inds) {

	btCollisionShape* groundShape;
	
	btScalar mass = get_float_by_JavaObj(env, geometry_obj, "mass");
	//btVector3 localInertia = get_vec_by_JavaObj(env, geometry_obj, "localInertia");
	//LOGV("in createGeometry.");
	
	
	jobject shape_obj = get_obj_by_JavaObj(env, geometry_obj, "shape", "Lorg/siprop/bullet/interfaces/Shape;");
//	LOGV("Load shape interface.");
	int shapeType = get_type_by_JavaObj(env, shape_obj);
//	LOGV("Load shape val.");
	switch(shapeType) {
		case CONVEX_HULL_SHAPE_PROXYTYPE:
		{
			//	LOGV("in CONVEX_HULL_PROXYTYPE.");

				jsize len = env->GetArrayLength(arr);
				jfloat *body = env->GetFloatArrayElements(arr,0);

				btScalar* b = new btScalar[len];

				for (int i=0; i<len; i++){
					b[i] = body[i];
				}

				env->ReleaseFloatArrayElements(arr,body,0);

				groundShape = new btConvexHullShape(b,len/3,3*sizeof(b[0]));
		}
			break;
		case STATIC_PLANE_PROXYTYPE:
		//	LOGV("in STATIC_PLANE_PROXYTYPE.");
			groundShape = new btStaticPlaneShape(get_vec_by_JavaObj(env, shape_obj, "planeNormal"),
												 get_float_by_JavaObj(env, shape_obj, "planeConstant"));
			
			break;
		
		case BOX_SHAPE_PROXYTYPE:
		//	LOGV("in BOX_SHAPE_PROXYTYPE.");
			groundShape = new btBoxShape(get_vec_by_JavaObj(env, shape_obj, "boxHalfExtents"));
			
			break;

		case CAPSULE_SHAPE_PROXYTYPE:
			groundShape = new btCapsuleShape(get_float_by_JavaObj(env, shape_obj, "radius"),
											 get_float_by_JavaObj(env, shape_obj, "height"));
			
			break;

		case CONE_SHAPE_PROXYTYPE:
			groundShape = new btConeShape(get_float_by_JavaObj(env, shape_obj, "radius"),
										  get_float_by_JavaObj(env, shape_obj, "height"));
			
			break;

		case CYLINDER_SHAPE_PROXYTYPE:
			groundShape = new btCylinderShape(get_vec_by_JavaObj(env, shape_obj, "halfExtents"));
			
			break;

//		case EMPTY_SHAPE_PROXYTYPE:
//			groundShape = new btEmptyShape();
//			
//			break;

		case SPHERE_SHAPE_PROXYTYPE:
		//	LOGV("in SPHERE_SHAPE_PROXYTYPE.");
			groundShape = new btSphereShape(get_float_by_JavaObj(env, shape_obj, "radius"));
			
			break;

		case TETRAHEDRAL_SHAPE_PROXYTYPE:
			groundShape = new btBU_Simplex1to4(get_point_by_JavaObj(env, shape_obj, "p0"),
											   get_point_by_JavaObj(env, shape_obj, "p1"),
											   get_point_by_JavaObj(env, shape_obj, "p2"),
											   get_point_by_JavaObj(env, shape_obj, "p3"));
			
			break;
		case TERRAIN_SHAPE_PROXYTYPE:
		{
		//	LOGV("in TERRAIN_SHAPE_PROXYTYPE.");

			jsize len = env->GetArrayLength(arr);
			jfloat *body = env->GetFloatArrayElements(arr,0);

			float b [len];

			for (int i=0; i<len; i++){
				b[i] = body[i];
			}

			env->ReleaseFloatArrayElements(arr,body,0);

			groundShape = new btHeightfieldTerrainShape(21,21,b, 100,1,true,false);
			//btVector3 ls(5,1,5);
			//groundShape->setLocalScaling(ls);
		}
		break;

		case TRIANGLE_MESH_SHAPE_PROXYTYPE:
		{
		//	LOGV("in TERRAIN_SHAPE_PROXYTYPE.");

			jsize len1 = env->GetArrayLength(arr);
			jfloat *body1 = env->GetFloatArrayElements(arr,0);

			jsize len2 = env->GetArrayLength(inds);
			jint *body2 = env->GetIntArrayElements(inds,0);

			btScalar* b1 = new btScalar[len1];
			int* b2 = new int[len2];

			for (int i=0; i<len1; i++){
				b1[i] = body1[i];
			}

			for (int i=0; i<len2; i++){
				b2[i] = body2[i];
			}

			env->ReleaseFloatArrayElements(arr,body1,0);
			env->ReleaseIntArrayElements(inds,body2,0);



			btTriangleIndexVertexArray* vertexArray = new btTriangleIndexVertexArray(((int)len2)/3,b2,3*sizeof(b2[0]),((int)len1)/3,b1,3*sizeof(b1[0]));

			groundShape = new btBvhTriangleMeshShape(vertexArray, false, true);


		}
		break;
//		case TRIANGLE_SHAPE_PROXYTYPE:
//			groundShape = new btTriangleShape(get_p2v_by_JavaObj(env, shape_obj, "p0"),
//											  get_p2v_by_JavaObj(env, shape_obj, "p1"),
//											  get_p2v_by_JavaObj(env, shape_obj, "p2"));
//			
//			break;
			
		default:
			return 0;
	}


	bool isDynamic = (mass != 0.f);
	if(isDynamic) {
		btVector3 localInertia(0,0,0);
		groundShape->calculateLocalInertia( mass, localInertia);

		jfloat inert[3];

		inert[0] = (jfloat)localInertia.x();
		inert[1] = (jfloat)localInertia.y();
		inert[2] = (jfloat)localInertia.z();

		jfloatArray j_inert = env->NewFloatArray(sizeof(inert));

		env->SetFloatArrayRegion(j_inert, 0, sizeof(inert)-1, inert);

		jclass bullet_clazz = env->GetObjectClass(geometry_obj);
				jmethodID set_inertia = env->GetMethodID(bullet_clazz, "setInertia", "([F)V");
				env->CallVoidMethod(geometry_obj, set_inertia, j_inert);
			//	LOGV("CallVoidMethod setInertia.");
	}
	else{
		btVector3 localInertia(0,0,0);
		groundShape->calculateLocalInertia(0,localInertia);
	}
	


	g_CollisionShapes.push_back(groundShape);
	
	jint addr_val = (intptr_t)groundShape;
	set_JavaObj_int(env, geometry_obj, "id", addr_val);

	return addr_val;
	
}


JNIEXPORT
jint
JNICALL
Java_org_siprop_bullet_Bullet_createAndAddRigidBody(JNIEnv* env,
                                                    jobject thiz,
                                                    jint physicsWorldId,
                                                    jobject rigidBody_obj) {
	
//	LOGV("in createAndAddRigidBody.");
	
	btDynamicsWorld* pDynamicsWorld = g_DynamicsWorlds.get((btDynamicsWorld*)physicsWorldId);	
	if(pDynamicsWorld == NULL) {
	//	LOGV("Don't Load pDynamicsWorld.");
		return 0;
	}
	
	jobject geometry_obj = get_obj_by_JavaObj(env, rigidBody_obj, "geometry", "Lorg/siprop/bullet/Geometry;");
	if(geometry_obj == NULL) {
	//	LOGV("geometry is NULL.");
		return 0;
	}
//	LOGV("Load geometry_obj.");
	
	jobject shape_obj = get_obj_by_JavaObj(env, geometry_obj, "shape", "Lorg/siprop/bullet/interfaces/Shape;");
	if(shape_obj == NULL) {
	//	LOGV("shape is NULL.");
		return 0;
	}
//	LOGV("Load shapeID.");
	int shapeID = get_id_by_JavaObj(env, shape_obj);
	btCollisionShape* colShape = g_CollisionShapes.get((btCollisionShape*)shapeID);
	if(colShape == NULL) {
	//	LOGV("shapeID is NULL.");
		return 0;
	}
	
	
	
	jobject motionState_obj = get_obj_by_JavaObj(env, rigidBody_obj, "motionState", "Lorg/siprop/bullet/MotionState;");
	if(motionState_obj == NULL) {
	//	LOGV("motionState is NULL.");
		return 0;
	}
//	LOGV("Load motionState_obj.");
	
	
	btTransform startTransform;
	startTransform.setIdentity();
	jobject worldTransform_obj = get_obj_by_JavaObj(env, motionState_obj, "worldTransform", "Lorg/siprop/bullet/Transform;");
//	LOGV("Load worldTransform_obj.");
	if(worldTransform_obj != NULL) {
		if(!is_NULL_point_field_JavaObj(env, worldTransform_obj, "originPoint")) {
		//	LOGV("Load originPoint.");
			startTransform.setOrigin(get_p2v_by_JavaObj(env, worldTransform_obj, "originPoint"));
		}
		if(!is_NULL_mat3x3_field_JavaObj(env, worldTransform_obj, "basis")) {
		//	LOGV("Load basis.");
			startTransform.setBasis(get_mat3x3_by_JavaObj(env, worldTransform_obj, "basis"));
		}
		if(!is_NULL_quat_field_JavaObj(env, worldTransform_obj, "rotation")) {
		//	LOGV("Load rotation.");
			startTransform.setRotation(get_quat_by_JavaObj(env, worldTransform_obj, "rotation"));
		}
		if(!is_NULL_point_field_JavaObj(env, worldTransform_obj, "invXform")) {
		//	LOGV("Load invXform.");
			startTransform.invXform(get_p2v_by_JavaObj(env, worldTransform_obj, "invXform"));
		}
	}
	
	btDefaultMotionState* myMotionState = new btDefaultMotionState(startTransform);
//	LOGV("Load myMotionState.");
	
	
	btVector3 localInertia;
	if(is_NULL_vec_field_JavaObj(env, geometry_obj, "localInertia")) {
		localInertia = btVector3(0.0f, 0.0f, 0.0f);
	} else {
		localInertia = get_vec_by_JavaObj(env, geometry_obj, "localInertia");
	}
//	LOGV("Load localInertia.");
	
	
	btRigidBody::btRigidBodyConstructionInfo rbInfo(get_float_by_JavaObj(env, geometry_obj, "mass"), 
													myMotionState, 
													colShape, 
													localInertia);
	btRigidBody* body = new btRigidBody(rbInfo);
//	LOGV("Load localInertia.");


	pDynamicsWorld->addRigidBody(body);
	
	jint addr_val = (intptr_t)body;
	set_JavaObj_int(env, rigidBody_obj, "id", addr_val);

	return addr_val;
}

JNIEXPORT
jint
JNICALL
Java_org_siprop_bullet_Bullet_changeTrans(JNIEnv* env,
                                         jobject thiz,
                                         jint physicsWorldId,
                                         jint rigidBodyId,
                                         jfloatArray trans) {

//	LOGV("in Java_org_siprop_bullet_Bullet_changeTrans.");

	btDynamicsWorld* pDynamicsWorld = g_DynamicsWorlds.get((btDynamicsWorld*)physicsWorldId);
	if(pDynamicsWorld == NULL) {
	//	LOGV("Don't Load pDynamicsWorld.");
		return 0;
	}

	btCollisionObject* obj = pDynamicsWorld->getCollisionObjectArray().get((btRigidBody*)rigidBodyId);
	if(obj == NULL) {
	//	LOGV("Don't Load btRigidBody.");
		return 0;
	}
	btRigidBody* rigidbody = btRigidBody::upcast(obj);

	jsize len = env->GetArrayLength(trans);
	jfloat *body = env->GetFloatArrayElements(trans,0);

	float b [len];

	for (int i=0; i<len; i++){
		b[i] = body[i];
	}

	env->ReleaseFloatArrayElements(trans,body,0);

	btTransform transform;

	transform.setFromOpenGLMatrix(b);

    rigidbody->activate(true);
	rigidbody->setWorldTransform(transform);

    return 1;
}

JNIEXPORT
jint
JNICALL
Java_org_siprop_bullet_Bullet_zeroVelocities(JNIEnv* env,
        jobject thiz,
        jint physicsWorldId,
        jint rigidBodyId){

//	LOGV("in Java_org_siprop_bullet_Bullet_changeTrans.");

	btDynamicsWorld* pDynamicsWorld = g_DynamicsWorlds.get((btDynamicsWorld*)physicsWorldId);
	if(pDynamicsWorld == NULL) {
	//	LOGV("Don't Load pDynamicsWorld.");
		return 0;
	}

	btCollisionObject* obj = pDynamicsWorld->getCollisionObjectArray().get((btRigidBody*)rigidBodyId);
	if(obj == NULL) {
	//	LOGV("Don't Load btRigidBody.");
		return 0;
	}
	btRigidBody* rigidbody = btRigidBody::upcast(obj);

	rigidbody->setLinearVelocity(btVector3(0,0,0));
	rigidbody->setAngularVelocity(btVector3(0,0,0));

	return 1;
}

JNIEXPORT
jint
JNICALL
Java_org_siprop_bullet_Bullet_setAngVel(JNIEnv* env,
        jobject thiz,
        jint physicsWorldId,
        jint rigidBodyId,
        jobject angVel){

//	LOGV("in Java_org_siprop_bullet_Bullet_changeTrans.");

	btDynamicsWorld* pDynamicsWorld = g_DynamicsWorlds.get((btDynamicsWorld*)physicsWorldId);
	if(pDynamicsWorld == NULL) {
	//	LOGV("Don't Load pDynamicsWorld.");
		return 0;
	}

	btCollisionObject* obj = pDynamicsWorld->getCollisionObjectArray().get((btRigidBody*)rigidBodyId);
	if(obj == NULL) {
	//	LOGV("Don't Load btRigidBody.");
		return 0;
	}
	btRigidBody* rigidbody = btRigidBody::upcast(obj);

	rigidbody->setAngularVelocity(get_vec_by_JavaVecObj(env, angVel));

	return 1;
}

JNIEXPORT
jint
JNICALL
Java_org_siprop_bullet_Bullet_setLinVel(JNIEnv* env,
        jobject thiz,
        jint physicsWorldId,
        jint rigidBodyId,
        jobject linVel){

//	LOGV("in Java_org_siprop_bullet_Bullet_changeTrans.");

	btDynamicsWorld* pDynamicsWorld = g_DynamicsWorlds.get((btDynamicsWorld*)physicsWorldId);
	if(pDynamicsWorld == NULL) {
	//	LOGV("Don't Load pDynamicsWorld.");
		return 0;
	}

	btCollisionObject* obj = pDynamicsWorld->getCollisionObjectArray().get((btRigidBody*)rigidBodyId);
	if(obj == NULL) {
	//	LOGV("Don't Load btRigidBody.");
		return 0;
	}
	btRigidBody* rigidbody = btRigidBody::upcast(obj);

	rigidbody->setLinearVelocity(get_vec_by_JavaVecObj(env, linVel));

	return 1;
}


JNIEXPORT
jint
JNICALL
Java_org_siprop_bullet_Bullet_applyForce(JNIEnv* env,
                                         jobject thiz,
                                         jint physicsWorldId, 
                                         jint rigidBodyId, 
                                         jobject force,
                                         jobject applyPoint) {

//	LOGV("in Java_org_siprop_bullet_Bullet_applyForce.");

	btDynamicsWorld* pDynamicsWorld = g_DynamicsWorlds.get((btDynamicsWorld*)physicsWorldId);	
	if(pDynamicsWorld == NULL) {
	//	LOGV("Don't Load pDynamicsWorld.");
		return 0;
	}

	btCollisionObject* obj = pDynamicsWorld->getCollisionObjectArray().get((btRigidBody*)rigidBodyId);
	if(obj == NULL) {
	//	LOGV("Don't Load btRigidBody.");
		return 0;
	}
	btRigidBody* body = btRigidBody::upcast(obj);
	body->applyForce(get_vec_by_JavaVecObj(env, force),
					 get_vec_by_JavaVecObj(env, applyPoint));

	return 1;
}

JNIEXPORT
jint
JNICALL
Java_org_siprop_bullet_Bullet_applyCentralForce(JNIEnv* env,
                                         jobject thiz,
                                         jint physicsWorldId,
                                         jint rigidBodyId,
                                         jobject force){
//	LOGV("in Java_org_siprop_bullet_Bullet_applyForce.");

	btDynamicsWorld* pDynamicsWorld = g_DynamicsWorlds.get((btDynamicsWorld*)physicsWorldId);
	if(pDynamicsWorld == NULL) {
	//	LOGV("Don't Load pDynamicsWorld.");
		return 0;
	}

	btCollisionObject* obj = pDynamicsWorld->getCollisionObjectArray().get((btRigidBody*)rigidBodyId);
	if(obj == NULL) {
	//	LOGV("Don't Load btRigidBody.");
		return 0;
	}
	btRigidBody* body = btRigidBody::upcast(obj);
	body->applyCentralForce(get_vec_by_JavaVecObj(env, force));

	return 1;
}

JNIEXPORT
jint
JNICALL
Java_org_siprop_bullet_Bullet_applyTorque(JNIEnv* env,
                                          jobject thiz,
                                          jint physicsWorldId, 
                                          jint rigidBodyId, 
                                          jobject torque) {

//	LOGV("in Java_org_siprop_bullet_Bullet_applyTorque.");

	btDynamicsWorld* pDynamicsWorld = g_DynamicsWorlds.get((btDynamicsWorld*)physicsWorldId);	
	if(pDynamicsWorld == NULL) {
	//	LOGV("Don't Load pDynamicsWorld.");
		return 0;
	}

	btCollisionObject* obj = pDynamicsWorld->getCollisionObjectArray().get((btRigidBody*)rigidBodyId);
	if(obj == NULL) {
	//	LOGV("Don't Load btRigidBody.");
		return 0;
	}
	btRigidBody* body = btRigidBody::upcast(obj);
	body->applyTorque(get_vec_by_JavaVecObj(env, torque));

	return 1;

}



JNIEXPORT
jint
JNICALL
Java_org_siprop_bullet_Bullet_applyCentralImpulse(JNIEnv* env,
                                                  jobject thiz,
                                                  jint physicsWorldId, 
                                                  jint rigidBodyId, 
                                                  jobject impulse) {

//	LOGV("in Java_org_siprop_bullet_Bullet_applyCentralImpulse.");

	btDynamicsWorld* pDynamicsWorld = g_DynamicsWorlds.get((btDynamicsWorld*)physicsWorldId);	
	if(pDynamicsWorld == NULL) {
	//	LOGV("Don't Load pDynamicsWorld.");
		return 0;
	}

	btCollisionObject* obj = pDynamicsWorld->getCollisionObjectArray().get((btRigidBody*)rigidBodyId);
	if(obj == NULL) {
	//	LOGV("Don't Load btRigidBody.");
		return 0;
	}
	btRigidBody* body = btRigidBody::upcast(obj);
	body->applyCentralImpulse(get_vec_by_JavaVecObj(env, impulse));

	return 1;
}


JNIEXPORT
jint
JNICALL
Java_org_siprop_bullet_Bullet_applyTorqueImpulse(JNIEnv* env,
                                                 jobject thiz,
                                                 jint physicsWorldId, 
                                                 jint rigidBodyId, 
                                                 jobject torque) {
	
//	LOGV("in Java_org_siprop_bullet_Bullet_applyTorqueImpulse.");

	btDynamicsWorld* pDynamicsWorld = g_DynamicsWorlds.get((btDynamicsWorld*)physicsWorldId);	
	if(pDynamicsWorld == NULL) {
	//	LOGV("Don't Load pDynamicsWorld.");
		return 0;
	}

	btCollisionObject* obj = pDynamicsWorld->getCollisionObjectArray().get((btRigidBody*)rigidBodyId);
	if(obj == NULL) {
	//	LOGV("Don't Load btRigidBody.");
		return 0;
	}
	btRigidBody* body = btRigidBody::upcast(obj);
	body->applyTorqueImpulse(get_vec_by_JavaVecObj(env, torque));

	return 1;	
}


JNIEXPORT
jint
JNICALL
Java_org_siprop_bullet_Bullet_applyImpulse(JNIEnv* env,
                                           jobject thiz,
                                           jint physicsWorldId, 
                                           jint rigidBodyId, 
                                           jobject impulse, 
                                           jobject applyPoint) {

//	LOGV("in Java_org_siprop_bullet_Bullet_applyImpulse.");

	btDynamicsWorld* pDynamicsWorld = g_DynamicsWorlds.get((btDynamicsWorld*)physicsWorldId);	
	if(pDynamicsWorld == NULL) {
	//	LOGV("Don't Load pDynamicsWorld.");
		return 0;
	}

	btCollisionObject* obj = pDynamicsWorld->getCollisionObjectArray().get((btRigidBody*)rigidBodyId);
	if(obj == NULL) {
	//	LOGV("Don't Load btRigidBody.");
		return 0;
	}
	btRigidBody* body = btRigidBody::upcast(obj);
	body->applyImpulse(get_vec_by_JavaVecObj(env, impulse),
					   get_vec_by_JavaVecObj(env, applyPoint));

	return 1;	
	
}



JNIEXPORT
jint
JNICALL
Java_org_siprop_bullet_Bullet_clearForces(JNIEnv* env,
                                          jobject thiz,
                                          jint physicsWorldId, 
                                          jint rigidBodyId) {

//	LOGV("in Java_org_siprop_bullet_Bullet_clearForces.");

	btDynamicsWorld* pDynamicsWorld = g_DynamicsWorlds.get((btDynamicsWorld*)physicsWorldId);	
	if(pDynamicsWorld == NULL) {
	//	LOGV("Don't Load pDynamicsWorld.");
		return 0;
	}

	btCollisionObject* obj = pDynamicsWorld->getCollisionObjectArray().get((btRigidBody*)rigidBodyId);
	if(obj == NULL) {
	//	LOGV("Don't Load btRigidBody.");
		return 0;
	}
	btRigidBody* body = btRigidBody::upcast(obj);
	body->clearForces();

	return 1;
}



JNIEXPORT
jint
JNICALL
Java_org_siprop_bullet_Bullet_setActivePhysicsWorldAll(JNIEnv* env,
                                                       jobject thiz,
                                                       jint physicsWorldId, 
                                                       jboolean isActive) {

	btCollisionObject* obj;
	btRigidBody* body;

	btDynamicsWorld* pDynamicsWorld = g_DynamicsWorlds.get((btDynamicsWorld*)physicsWorldId);	
	if(pDynamicsWorld == NULL) {
	//	LOGV("Don't Load pDynamicsWorld.");
		return 0;
	}

	signed int coll_obj = pDynamicsWorld->getNumCollisionObjects();
	for(signed int i = coll_obj - 1; i >= 0; i--){
	//	LOGV("in for loop.");
		obj = pDynamicsWorld->getCollisionObjectArray()[i];
		body = btRigidBody::upcast(obj);
		
		body->activate(isActive);
		
	}
	return 1;

}

JNIEXPORT
jint
JNICALL
Java_org_siprop_bullet_Bullet_setActive(JNIEnv* env,
                                        jobject thiz,
                                        jint physicsWorldId, 
                                        jint rigidBodyId, 
                                        jboolean isActive) {
	
//	LOGV("in Java_org_siprop_bullet_Bullet_applyImpulse.");

	btDynamicsWorld* pDynamicsWorld = g_DynamicsWorlds.get((btDynamicsWorld*)physicsWorldId);	
	if(pDynamicsWorld == NULL) {
	//	LOGV("Don't Load pDynamicsWorld.");
		return 0;
	}

	btCollisionObject* obj = pDynamicsWorld->getCollisionObjectArray().get((btRigidBody*)rigidBodyId);
	if(obj == NULL) {
	//	LOGV("Don't Load btRigidBody.");
		return 0;
	}
	btRigidBody* body = btRigidBody::upcast(obj);
	body->activate(isActive);
	
	return 1;
}



JNIEXPORT
jint
JNICALL
Java_org_siprop_bullet_Bullet_setActiveAll(JNIEnv* env,
                                          jobject thiz,
                                          jboolean isActive) {

	btDynamicsWorld* pDynamicsWorld;
	btCollisionObject* obj;
	btRigidBody* body;
	signed int coll_obj;

	for( signed int i = 0; i < g_DynamicsWorlds.size(); i++ ) {
		pDynamicsWorld = g_DynamicsWorlds[i];
		
		coll_obj = pDynamicsWorld->getNumCollisionObjects();
		for(signed int j = coll_obj - 1; j >= 0; j--){
		//	LOGV("in for loop.");
			obj = pDynamicsWorld->getCollisionObjectArray()[j];
			body = btRigidBody::upcast(obj);
			
			body->activate(isActive);
			
		}
		
	}
	return 1;

}



JNIEXPORT
jint
JNICALL
Java_org_siprop_bullet_Bullet_addConstraint(JNIEnv* env,
                                            jobject thiz,
                                            jobject constraint_obj) {


//	LOGV("in addConstraint.");

	int constraintType = get_type_by_JavaObj(env, constraint_obj);
//	LOGV("Load constraintType val.");
	switch(constraintType) {
		case HINGE_CONSTRAINT_TYPE: {
		//	LOGV("in HINGE_CONSTRAINT_TYPE.");
			
			jobject rbA_obj;
			jint rbAID;
			
			if(is_NULL_field_JavaObj(env, constraint_obj, "rbA", "Lorg/siprop/bullet/RigidBody;")) {
			//	LOGV("Don't Load rbA.");
				return 0;
			}
			rbA_obj = get_obj_by_JavaObj(env, constraint_obj, "rbA", "Lorg/siprop/bullet/RigidBody;");
			rbAID = get_int_by_JavaObj(env, rbA_obj, "id");
			int physicsWorldId = get_int_by_JavaObj(env, rbA_obj, "physicsWorldId");
			if(physicsWorldId == 0) {
			//	LOGV("Don't Load physicsWorldId.");
				return 0;
			}
			
			btDynamicsWorld* pDynamicsWorld = g_DynamicsWorlds.get((btDynamicsWorld*)physicsWorldId);	
			if(pDynamicsWorld == NULL) {
			//	LOGV("Don't Load pDynamicsWorld.");
				return 0;
			}
			
			
			
			btCollisionObject* objA = pDynamicsWorld->getCollisionObjectArray().get((btRigidBody*)rbAID);
			btRigidBody* bodyA = btRigidBody::upcast(objA);
			btVector3 pivotInA = get_pivot_by_JavaObj(env, constraint_obj, "pivotInA");
			btVector3 axisInA = get_axis_by_JavaObj(env, constraint_obj, "axisInA");
			
			btTypedConstraint*	hinge;
			
			if(is_NULL_field_JavaObj(env, constraint_obj, "rbB", "Lorg/siprop/bullet/RigidBody;")) {
				hinge = new btHingeConstraint(*bodyA,
											  pivotInA,
											  axisInA);
			} else {
				jobject rbB_obj = get_obj_by_JavaObj(env, constraint_obj, "rbB", "Lorg/siprop/bullet/RigidBody;");
				jint rbBID = get_int_by_JavaObj(env, rbB_obj, "id");
				btCollisionObject* objB = pDynamicsWorld->getCollisionObjectArray().get((btRigidBody*)rbBID);
				btRigidBody* bodyB = btRigidBody::upcast(objB);
				btVector3 pivotInB = get_pivot_by_JavaObj(env, constraint_obj, "pivotInB");
				btVector3 axisInB = get_axis_by_JavaObj(env, constraint_obj, "axisInB");
				hinge = new btHingeConstraint(*bodyA,
											  *bodyB,
											  pivotInA,
											  pivotInB,
											  axisInA,
											  axisInB);
			}
			
			
			pDynamicsWorld->addConstraint(hinge);
			
			break;
		}
		
		case POINT2POINT_CONSTRAINT_TYPE: {
		//	LOGV("in POINT2POINT_CONSTRAINT_TYPE.");
			jobject rbA_obj;
			jint rbAID;
			
			if(is_NULL_field_JavaObj(env, constraint_obj, "rbA", "Lorg/siprop/bullet/RigidBody;")) {
			//	LOGV("Don't Load rbA.");
				return 0;
			}
			rbA_obj = get_obj_by_JavaObj(env, constraint_obj, "rbA", "Lorg/siprop/bullet/RigidBody;");
			rbAID = get_int_by_JavaObj(env, rbA_obj, "id");
			int physicsWorldId = get_int_by_JavaObj(env, rbA_obj, "physicsWorldId");
			if(physicsWorldId == 0) {
			//	LOGV("Don't Load physicsWorldId.");
				return 0;
			}
			
			btDynamicsWorld* pDynamicsWorld = g_DynamicsWorlds.get((btDynamicsWorld*)physicsWorldId);	
			if(pDynamicsWorld == NULL) {
			//	LOGV("Don't Load pDynamicsWorld.");
				return 0;
			}
			
			
			
			btCollisionObject* objA = pDynamicsWorld->getCollisionObjectArray().get((btRigidBody*)rbAID);
			btRigidBody* bodyA = btRigidBody::upcast(objA);
			btVector3 pivotInA = get_pivot_by_JavaObj(env, constraint_obj, "pivotInA");
			
			btTypedConstraint*	p2p;
			
			if(is_NULL_field_JavaObj(env, constraint_obj, "rbB", "Lorg/siprop/bullet/RigidBody;")) {
				p2p = new btPoint2PointConstraint(*bodyA,
											  pivotInA);
			} else {
				jobject rbB_obj = get_obj_by_JavaObj(env, constraint_obj, "rbB", "Lorg/siprop/bullet/RigidBody;");
				jint rbBID = get_int_by_JavaObj(env, rbB_obj, "id");
				btCollisionObject* objB = pDynamicsWorld->getCollisionObjectArray().get((btRigidBody*)rbBID);
				btRigidBody* bodyB = btRigidBody::upcast(objB);
				btVector3 pivotInB = get_pivot_by_JavaObj(env, constraint_obj, "pivotInB");
				p2p = new btPoint2PointConstraint(*bodyA,
											  *bodyB,
											  pivotInA,
											  pivotInB);
			}
			
			
			pDynamicsWorld->addConstraint(p2p);
			
			break;
		}
		default:
			break;
	}
	return 1;
}









JNIEXPORT
jint
JNICALL
Java_org_siprop_bullet_Bullet_doSimulationNative(JNIEnv* env,
                                                 jobject thiz,
                                                 jint physicsWorldId,
                                                 jint count) {

	btCollisionObject* obj;
	btRigidBody* body;
	btCollisionShape* shape;
	
	btConvexHullShape* convexhull;
	btHeightfieldTerrainShape* heightfield;
	btBvhTriangleMeshShape* trianglemesh;
	btSphereShape* sphere;
	btStaticPlaneShape* plane;
	btBoxShape* box;
	btCylinderShape* cylinder;
	btConeShape* cone;
	btCapsuleShape* capsule;
	btBU_Simplex1to4* tetrahedral;

	btTransform trans;
	btMatrix3x3 rot;
	btVector3 pos;
	int shapeType;
	
	jfloat res_rot[9];
	jfloat res_pos[3];
	jfloat res_vel[3];
	jfloat res_angVel[3];
	jfloat res_opt[9];
	jfloatArray j_res_rot = env->NewFloatArray(sizeof(res_rot));
	jfloatArray j_res_pos = env->NewFloatArray(sizeof(res_pos));
	jfloatArray j_res_vel = env->NewFloatArray(sizeof(res_vel));
	jfloatArray j_res_angVel = env->NewFloatArray(sizeof(res_angVel));
	jfloatArray j_res_opt = env->NewFloatArray(sizeof(res_opt));

//	LOGV("in doSimulationNative.");

	btDynamicsWorld* pDynamicsWorld = g_DynamicsWorlds.get((btDynamicsWorld*)physicsWorldId);	
	if(pDynamicsWorld == NULL) {
	//	LOGV("Don't Load pDynamicsWorld.");
		return 0;
	}

	jlong time = (jlong)getTimeNsec();

	jclass bullet_clazz = env->GetObjectClass(thiz);
				jmethodID bullet_setTime = env->GetMethodID(bullet_clazz, "setTime", "(J)J");
				jlong exec_time = env->CallLongMethod(thiz, bullet_setTime, time);
			//	LOGV("CallVoidMethod bullet_resultSimulation_mid.");

	if (exec_time != -1) pDynamicsWorld->stepSimulation(exec_time/(float)1000000000, count);

	signed int coll_obj = pDynamicsWorld->getNumCollisionObjects();

	//__android_log_print(ANDROID_LOG_ERROR,"GARBAGE","%d",coll_obj);
	for(signed int i = coll_obj - 1; i >= 0; i--){
	//	LOGV("in getVertex for loop.");
		obj = pDynamicsWorld->getCollisionObjectArray()[i];
		body = btRigidBody::upcast(obj);
		shape = body->getCollisionShape();

		shapeType = shape->getShapeType();

		trans = body->getWorldTransform();
		btScalar glMat[16];
		trans.getOpenGLMatrix(glMat);
		
		res_rot[0] = (jfloat)glMat[0];
		res_rot[1] = (jfloat)glMat[1];
		res_rot[2] = (jfloat)glMat[2];
		res_rot[3] = (jfloat)glMat[4];
		res_rot[4] = (jfloat)glMat[5];
		res_rot[5] = (jfloat)glMat[6];
		res_rot[6] = (jfloat)glMat[8];
		res_rot[7] = (jfloat)glMat[9];
		res_rot[8] = (jfloat)glMat[10];
		
		res_pos[0] = (jfloat)glMat[12];
		res_pos[1] = (jfloat)glMat[13];
		res_pos[2] = (jfloat)glMat[14];

		btVector3 vel = body->getLinearVelocity();

		res_vel[0] = (jfloat)vel.x();
		res_vel[1] = (jfloat)vel.y();
		res_vel[2] = (jfloat)vel.z();

		btVector3 angVel = body->getAngularVelocity();

		res_angVel[0] = (jfloat)angVel.x();
		res_angVel[1] = (jfloat)angVel.y();
		res_angVel[2] = (jfloat)angVel.z();

		//char logout[40];
		//sprintf(logout, "%f", res_pos[1]);

		//__android_log_write(ANDROID_LOG_ERROR, "Tag", logout);

	//	LOGV("check shape->getShapeType().");
		switch(shapeType) {
			case STATIC_PLANE_PROXYTYPE:
			//	LOGV("in STATIC_PLANE_PROXYTYPE.");
				
				break;
			
			case BOX_SHAPE_PROXYTYPE:
			//	LOGV("in BOX_PROXYTYPE.");
				box = static_cast<btBoxShape*>(shape);
				res_opt[0] = (jfloat)box->getHalfExtentsWithoutMargin().x();
				res_opt[1] = (jfloat)box->getHalfExtentsWithoutMargin().y();
				res_opt[2] = (jfloat)box->getHalfExtentsWithoutMargin().z();
				
				break;

			case CAPSULE_SHAPE_PROXYTYPE:
			//	LOGV("in CAPSULE_SHAPE_PROXYTYPE.");
				capsule = static_cast<btCapsuleShape*>(shape);
				res_opt[0] = (jfloat)capsule->getRadius();
				res_opt[1] = (jfloat)capsule->getHalfHeight();
				
				break;

			case CONE_SHAPE_PROXYTYPE:
			//	LOGV("in CONE_SHAPE_PROXYTYPE.");
				cone = static_cast<btConeShape*>(shape);
				res_opt[0] = (jfloat)cone->getRadius();
				res_opt[1] = (jfloat)cone->getHeight();
			
				break;

			case CYLINDER_SHAPE_PROXYTYPE:
			//	LOGV("in CYLINDER_SHAPE_PROXYTYPE.");
				cylinder = static_cast<btCylinderShape*>(shape);
				res_opt[0] = (jfloat)cylinder->getHalfExtentsWithoutMargin().x();
				res_opt[1] = (jfloat)cylinder->getHalfExtentsWithoutMargin().y();
				res_opt[2] = (jfloat)cylinder->getHalfExtentsWithoutMargin().z();
				res_opt[3] = (jfloat)cylinder->getRadius();
				break;

//			case EMPTY_SHAPE_PROXYTYPE:
//				groundShape = new btEmptyShape();
//				
//				break;

			case SPHERE_SHAPE_PROXYTYPE:
			//	LOGV("in SPHERE_SHAPE_PROXYTYPE.");
				sphere = static_cast<btSphereShape*>(shape);
				
				res_opt[0] = (jfloat)sphere->getRadius();
				break;

			case TETRAHEDRAL_SHAPE_PROXYTYPE:
			//	LOGV("in TETRAHEDRAL_SHAPE_PROXYTYPE.");
				tetrahedral = static_cast<btBU_Simplex1to4*>(shape);
				break;
			case CONVEX_HULL_SHAPE_PROXYTYPE:
			//	LOGV("in CONVEX_HULL_SHAPE_PROXYTYPE.");
				convexhull = static_cast<btConvexHullShape*>(shape);
				break;
			case TERRAIN_SHAPE_PROXYTYPE:
			//	LOGV("TERRAIN_SHAPE_PROXYTYPE.");
				heightfield = static_cast<btHeightfieldTerrainShape*>(shape);
				break;
			case TRIANGLE_MESH_SHAPE_PROXYTYPE:
			//	LOGV("TRIANGLE_MESH_SHAPE_PROXYTYPE.");
				trianglemesh = static_cast<btBvhTriangleMeshShape*>(shape);
				break;
//			case TRIANGLE_SHAPE_PROXYTYPE:
//				groundShape = new btTriangleShape(get_p2v_by_JavaObj(env, shape_obj, "p0"),
//												  get_p2v_by_JavaObj(env, shape_obj, "p1"),
//												  get_p2v_by_JavaObj(env, shape_obj, "p2"));
//				
//				break;
				
			default:
				return 0;
		}
		

		env->SetFloatArrayRegion(j_res_rot, 0, sizeof(res_rot)-1, res_rot); 
		env->SetFloatArrayRegion(j_res_pos, 0, sizeof(res_pos)-1, res_pos); 
		env->SetFloatArrayRegion(j_res_vel, 0, sizeof(res_vel)-1, res_vel);
		env->SetFloatArrayRegion(j_res_angVel, 0, sizeof(res_angVel)-1, res_angVel);
		env->SetFloatArrayRegion(j_res_opt, 0, sizeof(res_opt)-1, res_opt); 

		jclass bullet_clazz = env->GetObjectClass(thiz);
		jmethodID bullet_resultSimulation_mid = env->GetMethodID(bullet_clazz, "resultSimulation", "(II[F[F[F[F[F)V");
		env->CallVoidMethod(thiz, bullet_resultSimulation_mid, (intptr_t)body, shapeType, j_res_rot, j_res_pos, j_res_vel, j_res_angVel, j_res_opt);
	//	LOGV("CallVoidMethod bullet_resultSimulation_mid.");
	}
//	LOGV("out doSimulationNative.");

    return (jint)coll_obj;
}



static int64_t getTimeNsec(){
	struct timespec now;
	clock_gettime(CLOCK_MONOTONIC, &now);
	return (int64_t) now.tv_sec*1000000000LL + now.tv_nsec;
}

JNIEXPORT
jboolean
JNICALL
Java_org_siprop_bullet_Bullet_isColliding(JNIEnv* env,
                                                 jobject thiz,
                                                 jint physicsWorldId,
                                                 jint rigidBodyId0,
                                                 jint rigidBodyId1) {

//	LOGV("in Java_org_siprop_bullet_Bullet_isColliding.");

	btDynamicsWorld* pDynamicsWorld = g_DynamicsWorlds.get((btDynamicsWorld*)physicsWorldId);
	if(pDynamicsWorld == NULL) {
	//	LOGV("Don't Load pDynamicsWorld.");
		return false;
	}

	btCollisionObject* obj0 = pDynamicsWorld->getCollisionObjectArray().get((btRigidBody*)rigidBodyId0);
	if(obj0 == NULL) {
	//	LOGV("Don't Load btRigidBody.");
		return false;
	}
	btRigidBody* body0 = btRigidBody::upcast(obj0);

	btCollisionObject* obj1 = pDynamicsWorld->getCollisionObjectArray().get((btRigidBody*)rigidBodyId1);
	if(obj1 == NULL) {
	//	LOGV("Don't Load btRigidBody.");
		return false;
	}
	btRigidBody* body1 = btRigidBody::upcast(obj1);

    btCollisionAlgorithm* algo =  pDynamicsWorld->getDispatcher()->findAlgorithm(body0,body1);
    btManifoldResult contactPointResult(body0,body1);
    algo->processCollision(body0,body1,pDynamicsWorld->getDispatchInfo(),&contactPointResult);

    btPersistentManifold* contactManifold = contactPointResult.getPersistentManifold();
    bool result;
    if (contactManifold != NULL && contactManifold->getNumContacts() > 0){
    	result = true;
    }
    else{
    	result = false;
    }

    algo->~btCollisionAlgorithm();
    pDynamicsWorld->getDispatcher()->freeCollisionAlgorithm(algo);

    return result;
}

void destroyPhysicsWorld(btDynamicsWorld* pDynamicsWorld) {

//	LOGV("in destroyPhysicsWorld.");

	
	// delete MotionStates.
	for( signed int i = pDynamicsWorld->getNumCollisionObjects() - 1; i >= 0; i-- ) {
		btCollisionObject* obj = pDynamicsWorld->getCollisionObjectArray()[i];
		btRigidBody* body = btRigidBody::upcast(obj);
		if( (body != NULL) && (body->getMotionState() != NULL) ) {
			delete body->getMotionState();
		}
		pDynamicsWorld->removeCollisionObject( obj );
		SAFE_DELETE( obj );
	}

	btCollisionConfiguration* cc = pDynamicsWorld->getCollisionConfiguration();
	SAFE_DELETE(cc);
	btBroadphaseInterface* bp = pDynamicsWorld->getBroadphase();
	SAFE_DELETE(bp);
	btOverlappingPairCache* pc = pDynamicsWorld->getPairCache();
	SAFE_DELETE(pc);
	btDispatcher* dp = pDynamicsWorld->getDispatcher();
	SAFE_DELETE(dp);
	btIDebugDraw* dd = pDynamicsWorld->getDebugDrawer();
	SAFE_DELETE(dd);
	btConstraintSolver* cs = pDynamicsWorld->getConstraintSolver();
	SAFE_DELETE(cs);
//	SAFE_DELETE(pDynamicsWorld->getDispatchInfo());

	g_DynamicsWorlds.remove(pDynamicsWorld);

	SAFE_DELETE(pDynamicsWorld);
	
}


JNIEXPORT
jboolean
JNICALL
Java_org_siprop_bullet_Bullet_destroyPhysicsWorld(JNIEnv* env,
                                                  jobject thiz,
                                                  jint physicsWorldId) {

//	LOGV("in Java_org_siprop_bullet_Bullet_destroyPhysicsWorld.");

	btDynamicsWorld* pDynamicsWorld = g_DynamicsWorlds.get((btDynamicsWorld*)physicsWorldId);	
	if(pDynamicsWorld == NULL) {
	//	LOGV("Don't Load pDynamicsWorld.");
		return JNI_TRUE;
	}
	
	destroyPhysicsWorld(pDynamicsWorld);
	
    return  JNI_TRUE;
}


JNIEXPORT
jboolean
JNICALL
Java_org_siprop_bullet_Bullet_destroyNative(JNIEnv* env,
                                            jobject thiz) {

//	LOGV("in Java_org_siprop_bullet_Bullet_destroyNative.");
	
	// delete DynamicsWorld.
	for( signed int i = 0; i < g_DynamicsWorlds.size(); i++ ) {
		btDynamicsWorld* pDynamicsWorld = g_DynamicsWorlds[i];
		destroyPhysicsWorld(pDynamicsWorld);
	}

	// delete Collisions.
	for( signed int i = 0; i < g_CollisionShapes.size(); i++ ) {
		btCollisionShape* shape = g_CollisionShapes[i];
		g_CollisionShapes[i] = 0;
		SAFE_DELETE( shape );
	}
	
	return JNI_TRUE;

}

