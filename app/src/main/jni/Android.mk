# /*
# Bullet Continuous Collision Detection and Physics Library for Android NDK
# Copyright (c) 2006-2009 Noritsuna Imamura  http://www.siprop.org/
# 
# This software is provided 'as-is', without any express or implied warranty.
# In no event will the authors be held liable for any damages arising from the use of this software.
# Permission is granted to anyone to use this software for any purpose,
# including commercial applications, and to alter it and redistribute it freely,
# subject to the following restrictions:
# 
# 1. The origin of this software must not be misrepresented; you must not claim that you wrote the original software. If you use this software in a product, an acknowledgment in the product documentation would be appreciated but is not required.
# 2. Altered source versions must be plainly marked as such, and must not be misrepresented as being the original software.
# 3. This notice may not be removed or altered from any source distribution.
# */
LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := bullet
LOCAL_C_INCLUDES := \
        $(LOCAL_PATH)/ \
        $(LOCAL_PATH)/BulletCollision/BroadphaseCollision \
        $(LOCAL_PATH)/BulletCollision/CollisionDispatch \
        $(LOCAL_PATH)/BulletCollision/CollisionShapes \
        $(LOCAL_PATH)/BulletCollision/NarrowPhaseCollision \
        $(LOCAL_PATH)/BulletDynamics/ConstraintSolver \
        $(LOCAL_PATH)/BulletDynamics/Dynamics \
        $(LOCAL_PATH)/BulletDynamics/Vehicle \
        $(LOCAL_PATH)/LinearMath

LOCAL_CFLAGS := $(LOCAL_C_INCLUDES:%=-I%)
LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -ldl -lm -llog

LOCAL_SRC_FILES := \
        BulletCollision/BroadphaseCollision/btAxisSweep3.cpp \
        BulletCollision/BroadphaseCollision/btBroadphaseProxy.cpp \
        BulletCollision/BroadphaseCollision/btCollisionAlgorithm.cpp \
        BulletCollision/BroadphaseCollision/btDispatcher.cpp \
        BulletCollision/BroadphaseCollision/btMultiSapBroadphase.cpp \
        BulletCollision/BroadphaseCollision/btOverlappingPairCache.cpp \
        BulletCollision/BroadphaseCollision/btSimpleBroadphase.cpp \
        BulletCollision/CollisionDispatch/btCollisionDispatcher.cpp \
        BulletCollision/CollisionDispatch/btCollisionObject.cpp \
        BulletCollision/CollisionDispatch/btCollisionWorld.cpp \
        BulletCollision/CollisionDispatch/btCompoundCollisionAlgorithm.cpp \
        BulletCollision/CollisionDispatch/btConvexConcaveCollisionAlgorithm.cpp \
        BulletCollision/CollisionDispatch/btConvexConvexAlgorithm.cpp \
        BulletCollision/CollisionDispatch/btConvexPlaneCollisionAlgorithm.cpp \
        BulletCollision/CollisionDispatch/btDefaultCollisionConfiguration.cpp \
        BulletCollision/CollisionDispatch/btEmptyCollisionAlgorithm.cpp \
        BulletCollision/CollisionDispatch/btManifoldResult.cpp \
        BulletCollision/CollisionDispatch/btSimulationIslandManager.cpp \
        BulletCollision/CollisionDispatch/btSphereBoxCollisionAlgorithm.cpp \
        BulletCollision/CollisionDispatch/btSphereSphereCollisionAlgorithm.cpp \
        BulletCollision/CollisionDispatch/btSphereTriangleCollisionAlgorithm.cpp \
        BulletCollision/CollisionDispatch/btUnionFind.cpp \
        BulletCollision/CollisionDispatch/SphereTriangleDetector.cpp \
        BulletCollision/CollisionShapes/btBoxShape.cpp \
        BulletCollision/CollisionShapes/btBvhTriangleMeshShape.cpp \
        BulletCollision/CollisionShapes/btCapsuleShape.cpp \
        BulletCollision/CollisionShapes/btCollisionShape.cpp \
        BulletCollision/CollisionShapes/btCompoundShape.cpp \
        BulletCollision/CollisionShapes/btConcaveShape.cpp \
        BulletCollision/CollisionShapes/btConeShape.cpp \
        BulletCollision/CollisionShapes/btConvexHullShape.cpp \
        BulletCollision/CollisionShapes/btConvexInternalShape.cpp \
        BulletCollision/CollisionShapes/btConvexShape.cpp \
        BulletCollision/CollisionShapes/btConvexTriangleMeshShape.cpp \
        BulletCollision/CollisionShapes/btCylinderShape.cpp \
        BulletCollision/CollisionShapes/btEmptyShape.cpp \
        BulletCollision/CollisionShapes/btHeightfieldTerrainShape.cpp \
        BulletCollision/CollisionShapes/btMinkowskiSumShape.cpp \
        BulletCollision/CollisionShapes/btMultiSphereShape.cpp \
        BulletCollision/CollisionShapes/btOptimizedBvh.cpp \
        BulletCollision/CollisionShapes/btPolyhedralConvexShape.cpp \
        BulletCollision/CollisionShapes/btSphereShape.cpp \
        BulletCollision/CollisionShapes/btStaticPlaneShape.cpp \
        BulletCollision/CollisionShapes/btStridingMeshInterface.cpp \
        BulletCollision/CollisionShapes/btTetrahedronShape.cpp \
        BulletCollision/CollisionShapes/btTriangleBuffer.cpp \
        BulletCollision/CollisionShapes/btTriangleCallback.cpp \
        BulletCollision/CollisionShapes/btTriangleIndexVertexArray.cpp \
        BulletCollision/CollisionShapes/btTriangleMesh.cpp \
        BulletCollision/CollisionShapes/btTriangleMeshShape.cpp \
        BulletCollision/CollisionShapes/btUniformScalingShape.cpp \
        BulletCollision/NarrowPhaseCollision/btContinuousConvexCollision.cpp \
        BulletCollision/NarrowPhaseCollision/btConvexCast.cpp \
        BulletCollision/NarrowPhaseCollision/btGjkConvexCast.cpp \
        BulletCollision/NarrowPhaseCollision/btGjkEpa.cpp \
        BulletCollision/NarrowPhaseCollision/btGjkEpaPenetrationDepthSolver.cpp \
        BulletCollision/NarrowPhaseCollision/btGjkPairDetector.cpp \
        BulletCollision/NarrowPhaseCollision/btMinkowskiPenetrationDepthSolver.cpp \
        BulletCollision/NarrowPhaseCollision/btPersistentManifold.cpp \
        BulletCollision/NarrowPhaseCollision/btRaycastCallback.cpp \
        BulletCollision/NarrowPhaseCollision/btSubSimplexConvexCast.cpp \
        BulletCollision/NarrowPhaseCollision/btVoronoiSimplexSolver.cpp \
        BulletDynamics/ConstraintSolver/btConeTwistConstraint.cpp \
        BulletDynamics/ConstraintSolver/btContactConstraint.cpp \
        BulletDynamics/ConstraintSolver/btGeneric6DofConstraint.cpp \
        BulletDynamics/ConstraintSolver/btHingeConstraint.cpp \
        BulletDynamics/ConstraintSolver/btPoint2PointConstraint.cpp \
        BulletDynamics/ConstraintSolver/btSequentialImpulseConstraintSolver.cpp \
        BulletDynamics/ConstraintSolver/btSolve2LinearConstraint.cpp \
        BulletDynamics/ConstraintSolver/btTypedConstraint.cpp \
        BulletDynamics/Dynamics/btContinuousDynamicsWorld.cpp \
        BulletDynamics/Dynamics/btDiscreteDynamicsWorld.cpp \
        BulletDynamics/Dynamics/btRigidBody.cpp \
        BulletDynamics/Dynamics/btSimpleDynamicsWorld.cpp \
        BulletDynamics/Dynamics/Bullet-C-API.cpp \
        BulletDynamics/Vehicle/btRaycastVehicle.cpp \
        BulletDynamics/Vehicle/btWheelInfo.cpp \
        LinearMath/btAlignedAllocator.cpp \
        LinearMath/btGeometryUtil.cpp \
        LinearMath/btQuickprof.cpp \
        bullet.cpp

include $(BUILD_SHARED_LIBRARY)
