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
package org.siprop.bullet.shape;

import java.util.ArrayList;

import org.siprop.bullet.interfaces.Shape;
import org.siprop.bullet.util.ShapeType;

public class TriangleMeshShape implements Shape {
	
	private static final int type = ShapeType.TRIANGLE_MESH_SHAPE_PROXYTYPE;
	private int id;
	public float [] points;
	public int [] inds;
	
	public TriangleMeshShape(float[] points, int[] inds){
		this.points = points;
		this.inds = inds;
	}
	
	public TriangleMeshShape(ArrayList<Float> points, ArrayList<Integer> inds){
		this.points = new float[points.size()];
		this.inds = new int[inds.size()];
		
		for (int i=0; i<points.size(); i++){
			this.points[i] = points.get(i);
		}
		
		for (int i=0; i<inds.size(); i++){
			this.inds[i] = inds.get(i);
		}
	}
	
	public int getType() {
		return type;
	}
	
	public int getID() {
		return id;
	}
	
	
	public void setID(int id) {
		this.id = id;
	}
}
