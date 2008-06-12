package de.uni_koblenz.jgstreetmap.routing;

import static java.lang.Math.cos;
import static java.lang.Math.toRadians;
import de.uni_koblenz.jgstreetmap.osmschema.Node;
import de.uni_koblenz.jgstreetmap.osmschema.routing.Segment;

public class GpsTools {
	public static class D2Vector {

		public static final D2Vector XAXIS = new D2Vector(1, 0);

		public static final D2Vector YAXIS = new D2Vector(0, 1);
		public static double getAngle(D2Vector v1, D2Vector v2) {
			double argument = scalarProduct(v1, v2)
					/ (v1.length() * v2.length());
			// System.out.println(argument);
			return Math.acos(argument);
		}

		public static double scalarProduct(D2Vector v1, D2Vector v2) {
			return v1.x * v2.x + v1.y * v2.y;
		}

		public double x, y;

		public D2Vector() {

		}

		public D2Vector(double x, double y) {
			super();
			this.x = x;
			this.y = y;
		}

		public double length() {
			return Math.sqrt(x * x + y * y);
		}

		public D2Vector normal() {
			D2Vector out = new D2Vector();
			out.x = x / length();
			out.y = x / length();
			return out;
		}
	}

	public static final double MINUTEMETER = 1852.0;

	public static D2Vector createVectorFromGPS(double lat1, double lat2,
			double lon1, double lon2) {
		D2Vector out = new D2Vector();
		out.x = (lon2 - lon1) * 60 * MINUTEMETER
				* cos(toRadians((lat1 + lat2) / 2.0));
		out.y = (lat2 - lat1) * 60 * MINUTEMETER;
		return out;
	}

	public static D2Vector createVectorFromSegment(Segment s) {
		Node start, end;
		start = (Node) s.getThis();
		end = (Node) s.getThat();
		return createVectorFromGPS(start.getLatitude(), end.getLatitude(),
				start.getLongitude(), end.getLongitude());
	}

	public static double getAngleBetweenSegments(Segment s1, Segment s2) {
		double out = 0;
		D2Vector v1 = createVectorFromSegment(s1);
		// double angle1 = D2Vector.getAngle(v1, D2Vector.XAXIS);
		D2Vector v2 = createVectorFromSegment(s2);
		// double angle2 = D2Vector.getAngle(v2, D2Vector.XAXIS);
		out = D2Vector.getAngle(v1, v2);
		// out = Math.toDegrees(out);
		// out = angle1 - angle2;
		return out;
	}

	// public static void main(String[] args) {
	// D2Vector v1 = new D2Vector();
	// v1.x = 0;
	// v1.y = 1;
	// D2Vector v2 = new D2Vector();
	// v2.x = 1;
	// v2.y = -1;
	// System.out.println(Math.toDegrees(D2Vector.getAngle(v1, v2)));
	//
	//	}
}
