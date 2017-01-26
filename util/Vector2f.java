package RUfoo.util;

import battlecode.common.MapLocation;

public final class Vector2f {

	public float dx, dy;

	public Vector2f(float _dx, float _dy) {
		dx = _dx;
		dy = _dy;
	}

	public Vector2f(MapLocation vec) {
		dx = vec.x;
		dy = vec.y;
	}
	
	public Vector2f(MapLocation start, MapLocation end) {
		dx = end.x - start.x;
		dy = end.y - start.y;
	}

	public Vector2f add(Vector2f p2) {
        return new Vector2f(dx + p2.dx, dy + p2.dy);
    }
	
	public Vector2f sub(Vector2f p2) {
         return new Vector2f(dx - p2.dx, dy - p2.dy);
     }
	
	public float dot(Vector2f other) {
		return dx * other.dx + dy * other.dy;
	}

	public Vector2f divide(float scale) {
		return new Vector2f(dx / scale, dy / scale);
	}

	public Vector2f multiply(float scale) {
		return new Vector2f(dx * scale, dy * scale);
	}

	public float magnitude() {
		return (float) Math.sqrt(dx * dx + dy * dy);
	}

	public float cross(Vector2f other) {
		return dx * other.dy - other.dx * dy;
	}
	
	public MapLocation projectOn(Vector2f other) {
		// ProjVonU = (u / len(u) ) * (v . u)/ len(u)
		Vector2f projectOtherOnThis = (other.divide(other.magnitude())).multiply(this.dot(other) / other.magnitude());
		return new MapLocation(projectOtherOnThis.dx, projectOtherOnThis.dy);
	}

	public MapLocation toMap() {
		return new MapLocation(dx, dy);
	}
	
}
