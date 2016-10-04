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
#include "btBulletCollisionCommon.h"
#include "btBulletDynamicsCommon.h"
#include <stdlib.h>
#include <string.h>
#include <jni.h>
#include <android/log.h>

#define LOGV(...) __android_log_print(ANDROID_LOG_SILENT, LOG_TAG, __VA_ARGS__)
//ANDROID_LOG_UNKNOWN, ANDROID_LOG_DEFAULT, ANDROID_LOG_VERBOSE, ANDROID_LOG_DEBUG, ANDROID_LOG_INFO, ANDROID_LOG_WARN, ANDROID_LOG_ERROR, ANDROID_LOG_FATAL, ANDROID_LOG_SILENT
//LOGV(ANDROID_LOG_DEBUG, "JNI", "");

#define ANDROID_LOG_VERBOSE ANDROID_LOG_DEBUG
#define LOG_TAG "JNI"
#define INVALID_ARGUMENT -18456

#define		SAFE_DELETE(p)			{ if(p){ delete (p); (p)=NULL; } }
#define		SAFE_DELETE_ARRAY(p)	{ if(p){ delete [](p); (p)=NULL; } }




// Bullet Objects
btAlignedObjectArray<btCollisionShape*>	g_CollisionShapes;
btAlignedObjectArray<btDynamicsWorld*>	g_DynamicsWorlds;



#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT
jint
JNICALL
Java_org_siprop_bullet_Bullet_createNonConfigPhysicsWorld(JNIEnv* env,
                                                        jobject thiz,
                                                        jobject physicsWorld );

JNIEXPORT
jint
JNICALL
Java_org_siprop_bullet_Bullet_changePhysicsWorldConfiguration(JNIEnv* env,
                                                        jobject thiz,
                                                        jobject physicsWorld );


JNIEXPORT
jint
JNICALL
Java_org_siprop_bullet_Bullet_createGeometry(JNIEnv* env,
                                                        jobject thiz,
                                                        jobject geometry,
                                                        jfloatArray arr,
                                                        jintArray inds);

JNIEXPORT
jint
JNICALL
Java_org_siprop_bullet_Bullet_makeDynamic(JNIEnv* env,
										jobject thiz,
										jfloat mass,
										jint physicsworldid,
										jint rigidbodyid);

JNIEXPORT
jint
JNICALL
Java_org_siprop_bullet_Bullet_makeKinematic(JNIEnv* env,
										jobject thiz,
										jint physicsworldid,
										jint rigidbodyid);

JNIEXPORT
jboolean
JNICALL
Java_org_siprop_bullet_Bullet_isActive(JNIEnv* env,
										jobject thiz,
										jint physicsworldid,
										jint rigidbodyid);

JNIEXPORT
jint
JNICALL
Java_org_siprop_bullet_Bullet_createAndAddRigidBody(JNIEnv* env,
                                                    jobject thiz,
                                                    jint physicsWorldId,
                                                    jobject rigidBody_obj);



JNIEXPORT
jint
JNICALL
Java_org_siprop_bullet_Bullet_changeTrans(JNIEnv* env,
                                         jobject thiz,
                                         jint physicsWorldId,
                                         jint rigidBodyId,
                                         jfloatArray trans);
JNIEXPORT
jint
JNICALL
Java_org_siprop_bullet_Bullet_zeroVelocities(JNIEnv* env,
        jobject thiz,
        jint physicsWorldId,
        jint rigidBodyId);

JNIEXPORT
jint
JNICALL
Java_org_siprop_bullet_Bullet_setAngVel(JNIEnv* env,
        jobject thiz,
        jint physicsWorldId,
        jint rigidBodyId,
        jobject angVel);

JNIEXPORT
jint
JNICALL
Java_org_siprop_bullet_Bullet_setLinVel(JNIEnv* env,
        jobject thiz,
        jint physicsWorldId,
        jint rigidBodyId,
        jobject linVel);

JNIEXPORT
jint
JNICALL
Java_org_siprop_bullet_Bullet_applyForce(JNIEnv* env,
                                         jobject thiz,
                                         jint physicsWorldId, 
                                         jint rigidBodyId, 
                                         jobject force,
                                         jobject applyPoint);

JNIEXPORT
jint
JNICALL
Java_org_siprop_bullet_Bullet_applyCentralForce(JNIEnv* env,
                                         jobject thiz,
                                         jint physicsWorldId,
                                         jint rigidBodyId,
                                         jobject force);

JNIEXPORT
jint
JNICALL
Java_org_siprop_bullet_Bullet_applyTorque(JNIEnv* env,
                                          jobject thiz,
                                          jint physicsWorldId, 
                                          jint rigidBodyId, 
                                          jobject torque);



JNIEXPORT
jint
JNICALL
Java_org_siprop_bullet_Bullet_applyCentralImpulse(JNIEnv* env,
                                                  jobject thiz,
                                                  jint physicsWorldId, 
                                                  jint rigidBodyId, 
                                                  jobject impulse);


JNIEXPORT
jint
JNICALL
Java_org_siprop_bullet_Bullet_applyTorqueImpulse(JNIEnv* env,
                                                 jobject thiz,
                                                 jint physicsWorldId, 
                                                 jint rigidBodyId, 
                                                 jobject torque);


JNIEXPORT
jint
JNICALL
Java_org_siprop_bullet_Bullet_applyImpulse(JNIEnv* env,
                                           jobject thiz,
                                           jint physicsWorldId, 
                                           jint rigidBodyId, 
                                           jobject impulse, 
                                           jobject applyPoint);



JNIEXPORT
jint
JNICALL
Java_org_siprop_bullet_Bullet_clearForces(JNIEnv* env,
                                          jobject thiz,
                                          jint physicsWorldId, 
                                          jint rigidBodyId);




JNIEXPORT
jint
JNICALL
Java_org_siprop_bullet_Bullet_setActive(JNIEnv* env,
                                        jobject thiz,
                                        jint physicsWorldId, 
                                        jint rigidBodyId, 
                                        jboolean isActive);

JNIEXPORT
jint
JNICALL
Java_org_siprop_bullet_Bullet_setActivePhysicsWorldAll(JNIEnv* env,
                                        jobject thiz,
                                        jint physicsWorldId, 
                                        jboolean isActive);

JNIEXPORT
jint
JNICALL
Java_org_siprop_bullet_Bullet_setActiveAll(JNIEnv* env,
                                          jobject thiz,
                                          jboolean isActive);


JNIEXPORT
jint
JNICALL
Java_org_siprop_bullet_Bullet_addConstraint(JNIEnv* env,
                                            jobject thiz,
                                            jobject constraint_obj);


JNIEXPORT
jint
JNICALL
Java_org_siprop_bullet_Bullet_doSimulationNative(JNIEnv* env,
                                                 jobject thiz,
                                                 jint physicsWorldId,
                                                 jint count);

static int64_t getTimeNsec();

JNIEXPORT
jboolean
JNICALL
Java_org_siprop_bullet_Bullet_isColliding(JNIEnv* env,
                                                 jobject thiz,
                                                 jint physicsWorldId,
                                                 jint rigidBodyId0,
                                                 jint rigidBodyId1);

JNIEXPORT
jboolean
JNICALL
Java_org_siprop_bullet_Bullet_destroyPhysicsWorld(JNIEnv* env,
                                                  jobject thiz,
                                                  jint physicsWorldId);

JNIEXPORT
jboolean
JNICALL
Java_org_siprop_bullet_Bullet_destroyNative(JNIEnv* env,
                                            jobject thiz);

#ifdef __cplusplus
}
#endif



bool is_NULL_field_JavaObj(JNIEnv* env, jobject java_obj, const char* field_name, const char* field_type) {


//	LOGV("in is_NULL_field_JavaObj!");
	jclass clazz = env->GetObjectClass(java_obj);


	// get field
	jfieldID fid = env->GetFieldID(clazz, field_name, field_type);

	jobject obj = env->GetObjectField(java_obj, fid);
	if(obj == NULL) {
	//	LOGV("Object is NULL!");
		return true;
	}
	return false;
}

bool is_NULL_vec_field_JavaObj(JNIEnv* env, jobject java_obj, const char* field_name) {
	return is_NULL_field_JavaObj(env, java_obj, field_name, "Lorg/siprop/bullet/util/Vector3;");
}

bool is_NULL_point_field_JavaObj(JNIEnv* env, jobject java_obj, const char* field_name) {
	return is_NULL_field_JavaObj(env, java_obj, field_name, "Lorg/siprop/bullet/util/Point3;");
}

bool is_NULL_axis_field_JavaObj(JNIEnv* env, jobject java_obj, const char* field_name) {
	return is_NULL_field_JavaObj(env, java_obj, field_name, "Lorg/siprop/bullet/util/Axis;");
}

bool is_NULL_pivot_field_JavaObj(JNIEnv* env, jobject java_obj, const char* field_name) {
	return is_NULL_field_JavaObj(env, java_obj, field_name, "Lorg/siprop/bullet/util/Pivot3;");
}

bool is_NULL_quat_field_JavaObj(JNIEnv* env, jobject java_obj, const char* field_name) {
	return is_NULL_field_JavaObj(env, java_obj, field_name, "Lorg/siprop/bullet/util/Quaternion;");
}

bool is_NULL_mat3x3_field_JavaObj(JNIEnv* env, jobject java_obj, const char* field_name) {
	return is_NULL_field_JavaObj(env, java_obj, field_name, "Lorg/siprop/bullet/util/Matrix3x3;");
}
bool is_NULL_mat3x1_field_JavaObj(JNIEnv* env, jobject java_obj, const char* field_name) {
	return is_NULL_field_JavaObj(env, java_obj, field_name, "Lorg/siprop/bullet/util/Matrix3x1;");
}




void set_JavaObj_int(JNIEnv* env, jobject java_obj, const char* field_name, jint val) {

//	LOGV("in set_JavaObj_int!");

	jclass clazz = env->GetObjectClass(java_obj);
	jfieldID fid = env->GetFieldID(clazz, field_name, "I");

	env->SetIntField(java_obj, fid, val);

}

btVector3 get_vec_by_JavaVecObj(JNIEnv* env, jobject vector_obj) {

//	LOGV("in get_vec_by_JavaVecObj!");

	jclass vector_clazz = env->GetObjectClass(vector_obj);
	jfieldID vector_x_fid = env->GetFieldID(vector_clazz, "x", "F");
	jfloat vector_x_obj = env->GetFloatField(vector_obj, vector_x_fid);
	jfieldID vector_y_fid = env->GetFieldID(vector_clazz, "y", "F");
	jfloat vector_y_obj = env->GetFloatField(vector_obj, vector_y_fid);
	jfieldID vector_z_fid = env->GetFieldID(vector_clazz, "z", "F");
	jfloat vector_z_obj = env->GetFloatField(vector_obj, vector_z_fid);

	return btVector3(vector_x_obj, vector_y_obj, vector_z_obj);

}


int get_id_by_JavaObj(JNIEnv* env, jobject java_obj) {

//	LOGV("in get_id_by_JavaObj!");

	jclass method_clazz = env->GetObjectClass(java_obj);
	jmethodID get_type_mid = env->GetMethodID(method_clazz, "getID", "()I");
	return env->CallIntMethod(java_obj, get_type_mid);

}

int get_type_by_JavaObj(JNIEnv* env, jobject java_obj) {

//	LOGV("in get_type_by_JavaObj!");

	jclass method_clazz = env->GetObjectClass(java_obj);
	jmethodID get_type_mid = env->GetMethodID(method_clazz, "getType", "()I");
	return env->CallIntMethod(java_obj, get_type_mid);

}


int get_int_by_JavaObj(JNIEnv* env, jobject java_obj, const char* field_name) {
	
//	LOGV("in get_int_by_JavaObj!");

	jclass clazz = env->GetObjectClass(java_obj);
	jfieldID int_fid = env->GetFieldID(clazz, field_name, "I");
	return env->GetIntField(java_obj, int_fid);

}


float get_float_by_JavaObj(JNIEnv* env, jobject java_obj, const char* field_name) {
	
//	LOGV("in get_float_by_JavaObj!");

	jclass clazz = env->GetObjectClass(java_obj);
	jfieldID float_fid = env->GetFieldID(clazz, field_name, "F");
	return env->GetFloatField(java_obj, float_fid);

}


jobject get_obj_by_JavaObj(JNIEnv* env, jobject java_obj, const char* field_name, const char* obj_type) {
	
//	LOGV("in get_obj_by_JavaObj!");

	jclass clazz = env->GetObjectClass(java_obj);
	jfieldID obj_fid = env->GetFieldID(clazz, field_name, obj_type);
	return env->GetObjectField(java_obj, obj_fid);

}


btVector3 get_vec_by_JavaObj(JNIEnv* env, jobject java_obj, const char* field_name) {

//	LOGV("in get_vec_by_JavaObj!");

	jclass clazz = env->GetObjectClass(java_obj);
	jfieldID vector_fid = env->GetFieldID(clazz, field_name, "Lorg/siprop/bullet/util/Vector3;");
	jobject vector_obj = env->GetObjectField(java_obj, vector_fid);
	
	
	jclass vector_clazz = env->GetObjectClass(vector_obj);
	jfieldID vector_x_fid = env->GetFieldID(vector_clazz, "x", "F");
	jfloat vector_x_obj = env->GetFloatField(vector_obj, vector_x_fid);
	jfieldID vector_y_fid = env->GetFieldID(vector_clazz, "y", "F");
	jfloat vector_y_obj = env->GetFloatField(vector_obj, vector_y_fid);
	jfieldID vector_z_fid = env->GetFieldID(vector_clazz, "z", "F");
	jfloat vector_z_obj = env->GetFloatField(vector_obj, vector_z_fid);

	return btVector3(vector_x_obj, vector_y_obj, vector_z_obj);

}

btPoint3 get_point_by_JavaObj(JNIEnv* env, jobject java_obj, const char* field_name) {

//	LOGV("in get_point_by_JavaObj!");
	
	jclass clazz = env->GetObjectClass(java_obj);
	jfieldID point_fid = env->GetFieldID(clazz, field_name, "Lorg/siprop/bullet/util/Point3;");
	jobject point_obj = env->GetObjectField(java_obj, point_fid);

	jclass point_clazz = env->GetObjectClass(point_obj);
	jfieldID point_x_fid = env->GetFieldID(point_clazz, "x", "F");
	jfloat point_x_obj = env->GetFloatField(point_obj, point_x_fid);
	jfieldID point_y_fid = env->GetFieldID(point_clazz, "y", "F");
	jfloat point_y_obj = env->GetFloatField(point_obj, point_y_fid);
	jfieldID point_z_fid = env->GetFieldID(point_clazz, "z", "F");
	jfloat point_z_obj = env->GetFloatField(point_obj, point_z_fid);

	return btPoint3(point_x_obj, point_y_obj, point_z_obj);

}


btVector3 get_p2v_by_JavaObj(JNIEnv* env, jobject java_obj, const char* field_name) {


//	LOGV("in get_p2v_by_JavaObj!");
	
	jclass clazz = env->GetObjectClass(java_obj);
	jfieldID point_fid = env->GetFieldID(clazz, field_name, "Lorg/siprop/bullet/util/Point3;");
	jobject point_obj = env->GetObjectField(java_obj, point_fid);


	jclass point_clazz = env->GetObjectClass(point_obj);
	jfieldID point_x_fid = env->GetFieldID(point_clazz, "x", "F");
	jfloat point_x_obj = env->GetFloatField(point_obj, point_x_fid);
	jfieldID point_y_fid = env->GetFieldID(point_clazz, "y", "F");
	jfloat point_y_obj = env->GetFloatField(point_obj, point_y_fid);
	jfieldID point_z_fid = env->GetFieldID(point_clazz, "z", "F");
	jfloat point_z_obj = env->GetFloatField(point_obj, point_z_fid);

	return btVector3(point_x_obj, point_y_obj, point_z_obj);

}


btVector3 get_pivot_by_JavaObj(JNIEnv* env, jobject java_obj, const char* field_name) {


//	LOGV("in get_pivot_by_JavaObj!");

	jclass clazz = env->GetObjectClass(java_obj);
	jfieldID pivot_fid = env->GetFieldID(clazz, field_name, "Lorg/siprop/bullet/util/Pivot3;");
	jobject pivot_obj = env->GetObjectField(java_obj, pivot_fid);


	jclass pivot_clazz = env->GetObjectClass(pivot_obj);
	jfieldID pivot_x_fid = env->GetFieldID(pivot_clazz, "x", "F");
	jfloat pivot_x_obj = env->GetFloatField(pivot_obj, pivot_x_fid);
	jfieldID pivot_y_fid = env->GetFieldID(pivot_clazz, "y", "F");
	jfloat pivot_y_obj = env->GetFloatField(pivot_obj, pivot_y_fid);
	jfieldID pivot_z_fid = env->GetFieldID(pivot_clazz, "z", "F");
	jfloat pivot_z_obj = env->GetFloatField(pivot_obj, pivot_z_fid);

	return btVector3(pivot_x_obj, pivot_y_obj, pivot_z_obj);

}



btVector3 get_axis_by_JavaObj(JNIEnv* env, jobject java_obj, const char* field_name) {


//	LOGV("in get_axis_by_JavaObj!");

	jclass clazz = env->GetObjectClass(java_obj);
	jfieldID axis_fid = env->GetFieldID(clazz, field_name, "Lorg/siprop/bullet/util/Axis3;");
	jobject axis_obj = env->GetObjectField(java_obj, axis_fid);


	jclass axis_clazz = env->GetObjectClass(axis_obj);
	jfieldID axis_x_fid = env->GetFieldID(axis_clazz, "x", "F");
	jfloat axis_x_obj = env->GetFloatField(axis_obj, axis_x_fid);
	jfieldID axis_y_fid = env->GetFieldID(axis_clazz, "y", "F");
	jfloat axis_y_obj = env->GetFloatField(axis_obj, axis_y_fid);
	jfieldID axis_z_fid = env->GetFieldID(axis_clazz, "z", "F");
	jfloat axis_z_obj = env->GetFloatField(axis_obj, axis_z_fid);

	return btVector3(axis_x_obj, axis_y_obj, axis_z_obj);

}



btQuaternion get_quat_by_JavaObj(JNIEnv* env, jobject java_obj, const char* field_name) {


//	LOGV("in get_quat_by_JavaObj!");

	jclass clazz = env->GetObjectClass(java_obj);
	jfieldID quaternion_fid = env->GetFieldID(clazz, field_name, "Lorg/siprop/bullet/util/Quaternion;");
	jobject quaternion_obj = env->GetObjectField(java_obj, quaternion_fid);


	jclass quaternion_clazz = env->GetObjectClass(quaternion_obj);
	jfieldID quaternion_x_fid = env->GetFieldID(quaternion_clazz, "x", "F");
	jfloat quaternion_x_obj = env->GetFloatField(quaternion_obj, quaternion_x_fid);
	jfieldID quaternion_y_fid = env->GetFieldID(quaternion_clazz, "y", "F");
	jfloat quaternion_y_obj = env->GetFloatField(quaternion_obj, quaternion_y_fid);
	jfieldID quaternion_z_fid = env->GetFieldID(quaternion_clazz, "z", "F");
	jfloat quaternion_z_obj = env->GetFloatField(quaternion_obj, quaternion_z_fid);
	jfieldID quaternion_w_fid = env->GetFieldID(quaternion_clazz, "w", "F");
	jfloat quaternion_w_obj = env->GetFloatField(quaternion_obj, quaternion_w_fid);

	return btQuaternion(quaternion_x_obj, quaternion_y_obj, quaternion_z_obj, quaternion_w_obj);

}



btVector3 get_mat3x1_by_JavaObj(JNIEnv* env, jobject java_obj, const char* field_name) {


//	LOGV("in get_mat3x1_by_JavaObj!");

	jclass clazz = env->GetObjectClass(java_obj);
	jfieldID matrix3x1_fid = env->GetFieldID(clazz, field_name, "Lorg/siprop/bullet/util/Matrix3x1;");
	jobject matrix3x1_obj = env->GetObjectField(java_obj, matrix3x1_fid);



	jclass matrix3x1_clazz = env->GetObjectClass(matrix3x1_obj);
	jfieldID matrix3x1_x_fid = env->GetFieldID(matrix3x1_clazz, "x", "F");
	jfloat matrix3x1_x_obj = env->GetFloatField(matrix3x1_obj, matrix3x1_x_fid);
	jfieldID matrix3x1_y_fid = env->GetFieldID(matrix3x1_clazz, "y", "F");
	jfloat matrix3x1_y_obj = env->GetFloatField(matrix3x1_obj, matrix3x1_y_fid);
	jfieldID matrix3x1_z_fid = env->GetFieldID(matrix3x1_clazz, "z", "F");
	jfloat matrix3x1_z_obj = env->GetFloatField(matrix3x1_obj, matrix3x1_z_fid);

	return btVector3(matrix3x1_x_obj, matrix3x1_y_obj, matrix3x1_z_obj);

}





btMatrix3x3 get_mat3x3_by_JavaObj(JNIEnv* env, jobject java_obj, const char* field_name) {


//	LOGV("in get_mat3x3_by_JavaObj!");

	jclass clazz = env->GetObjectClass(java_obj);
	jfieldID matrix3x3_fid = env->GetFieldID(clazz, field_name, "Lorg/siprop/bullet/util/Matrix3x3;");
	jobject matrix3x3_obj = env->GetObjectField(java_obj, matrix3x3_fid);


	jclass matrix3x3_clazz = env->GetObjectClass(matrix3x3_obj);
	jfieldID matrix3x3_xx_fid = env->GetFieldID(matrix3x3_clazz, "xx", "F");
	jfloat matrix3x3_xx_obj = env->GetFloatField(matrix3x3_obj, matrix3x3_xx_fid);
	jfieldID matrix3x3_xy_fid = env->GetFieldID(matrix3x3_clazz, "xy", "F");
	jfloat matrix3x3_xy_obj = env->GetFloatField(matrix3x3_obj, matrix3x3_xy_fid);
	jfieldID matrix3x3_xz_fid = env->GetFieldID(matrix3x3_clazz, "xz", "F");
	jfloat matrix3x3_xz_obj = env->GetFloatField(matrix3x3_obj, matrix3x3_xz_fid);


	jfieldID matrix3x3_yx_fid = env->GetFieldID(matrix3x3_clazz, "yx", "F");
	jfloat matrix3x3_yx_obj = env->GetFloatField(matrix3x3_obj, matrix3x3_yx_fid);
	jfieldID matrix3x3_yy_fid = env->GetFieldID(matrix3x3_clazz, "yy", "F");
	jfloat matrix3x3_yy_obj = env->GetFloatField(matrix3x3_obj, matrix3x3_yy_fid);
	jfieldID matrix3x3_yz_fid = env->GetFieldID(matrix3x3_clazz, "yz", "F");
	jfloat matrix3x3_yz_obj = env->GetFloatField(matrix3x3_obj, matrix3x3_yz_fid);


	jfieldID matrix3x3_zx_fid = env->GetFieldID(matrix3x3_clazz, "zx", "F");
	jfloat matrix3x3_zx_obj = env->GetFloatField(matrix3x3_obj, matrix3x3_zx_fid);
	jfieldID matrix3x3_zy_fid = env->GetFieldID(matrix3x3_clazz, "zy", "F");
	jfloat matrix3x3_zy_obj = env->GetFloatField(matrix3x3_obj, matrix3x3_zy_fid);
	jfieldID matrix3x3_zz_fid = env->GetFieldID(matrix3x3_clazz, "zz", "F");
	jfloat matrix3x3_zz_obj = env->GetFloatField(matrix3x3_obj, matrix3x3_zz_fid);


	return btMatrix3x3(matrix3x3_xx_obj, matrix3x3_xy_obj, matrix3x3_xz_obj,
					   matrix3x3_yx_obj, matrix3x3_yy_obj, matrix3x3_yz_obj,
					   matrix3x3_zx_obj, matrix3x3_zy_obj, matrix3x3_zz_obj);

}

