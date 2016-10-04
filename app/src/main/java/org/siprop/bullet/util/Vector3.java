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
package org.siprop.bullet.util;

public class Vector3 {
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Float.floatToIntBits(x);
		result = prime * result + Float.floatToIntBits(y);
		result = prime * result + Float.floatToIntBits(z);
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
		Vector3 other = (Vector3) obj;
		if (Float.floatToIntBits(x) != Float.floatToIntBits(other.x))
			return false;
		if (Float.floatToIntBits(y) != Float.floatToIntBits(other.y))
			return false;
		if (Float.floatToIntBits(z) != Float.floatToIntBits(other.z))
			return false;
		return true;
	}
	public float x;
	public float y;
	public float z;
	
	public Vector3() {
		
	}
	public Vector3(Vector3 v){
		x = v.x;
		y = v.y;
		z = v.z;
	}
	public Vector3(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	public Vector3(float[] mat) {
		if(mat.length >= 3) {
			this.x = mat[0];
			this.y = mat[1];
			this.z = mat[2];
		}
	}
	
	public void sub(Vector3 a, Vector3 b){
		x = a.x-b.x;
		y = a.y-b.y;
		z = a.z-b.z;
	}
	
	public void add(Vector3 a, Vector3 b){
		x = a.x+b.x;
		y = a.y+b.y;
		z = a.z+b.z;
	}
	
	public void normalize(){
		float len = (float)Math.sqrt(x*x+y*y+z*z);
	
		x /= len;
		y /= len;
		z /= len;
	}
	
	public void negate(){
		x = -x;
		y = -y;
		z = -z;
	}
	
	public void cross(Vector3 a, Vector3 b) {
		x = a.y*b.z-a.z*b.y;
		y = a.z*b.x-a.x*b.z;
		z = a.x*b.y-a.y*b.x;
	}
	public float lengthSquared() {
		return x*x+y*y+z*z;
	}

	public float length(){
		return (float)Math.sqrt(lengthSquared());
	}

	public float dot(Vector3 a){
		return x*a.x+y*a.y+z*a.z;
	}
	
	public void scale(float f){
		x = f*x;
		y = f*y;
		z = f*z;
	}
}
