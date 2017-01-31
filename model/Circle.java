package RUfoo.model;

import battlecode.common.MapLocation;

public class Circle {
	
	private MapLocation center;
	private float radius;
	
	public Circle(MapLocation center, float radius) {
		this.center = center;
		this.radius = radius;
	}
	
	public MapLocation[] intersections(Circle c) {
		Vector2f v0 = new Vector2f(center);
		Vector2f v1 = new Vector2f(c.center);
		
        float d, a, h, radiusSq;
        radiusSq = radius * radius;
        d = center.distanceTo(c.center);
        a = (radiusSq - c.radius * c.radius + d * d) / (2 * d);
        h = (float)Math.sqrt(radiusSq - a * a);
        
        MapLocation p2 = v1.sub(v0).multiply(a/d).add(v0).toMap();
        float x3, y3, x4, y4;
        x3 = p2.x + h * (v1.dy - v0.dy) / d;
        y3 = p2.y - h * (v1.dx - v0.dx) / d;
        x4 = p2.x - h * (v1.dy - v0.dy) / d;
        y4 = p2.y + h * (v1.dx - v0.dx) / d;

        return new MapLocation[] {new MapLocation(x3, y3), new MapLocation(x4, y4)};
    }
	
}
