package de.uni_koblenz.jgstreetmap.routing;

import java.util.List;
import java.util.Set;

import de.uni_koblenz.jgstreetmap.osmschema.OsmGraph;
import de.uni_koblenz.jgstreetmap.osmschema.map.Node;
import de.uni_koblenz.jgstreetmap.osmschema.routing.Segment;
import de.uni_koblenz.jgstreetmap.osmschema.routing.SegmentType;

public abstract class RouteCalculator {
	public class Speed {
		public double cycle = 15;
		public double motorway = 100;
		public double countryroad = 60;
		public double residential = 35;
		public double footway = 5;
		public double unsurfaced = 20;
		public double service = 10;
	}

	public enum Direction {
		NORMAL, REVERSED;
	}

	public enum EdgeRating {
		LENGTH, TIME, CONVENIENCE;
	}

	public enum RoutingRestriction {
		CAR, BIKE, FOOT
	}

	protected Set<SegmentType> relevantTypes;
	protected Node start;
	protected Speed speeds;
	protected OsmGraph graph;
	protected static final double INCONVENIENCEVALUE = 100;

	public abstract RoutingResult getRoute(Node target, EdgeRating r);

	protected double computeFactor(Segment s) {
		switch (s.get_wayType()) {
		case CYCLEWAY:
			return 3.6 / speeds.cycle;
		case MOTORWAY:
			return 3.6 / speeds.motorway;
		case PRIMARY:
			return 3.6 / speeds.countryroad;
		case SECONDARY:
			return 3.6 / speeds.countryroad;
		case TERTIARY:
			return 3.6 / speeds.residential;
		case RESIDENTIAL:
			return 3.6 / speeds.residential;
		case FOOTWAY:
			return 3.6 / speeds.footway;
		case UNSURFACED:
			return 3.6 / speeds.unsurfaced;
		case SERVICE:
			return 3.6 / speeds.service;
		case WORMHOLE:
			return 0;
		default:
			return Double.MAX_VALUE;
		}
	}

	public void setStart(Node start) {
		this.start = start;
	}

	public Node getStart() {
		return start;
	}

	public void setRestriction(RoutingRestriction rest) {
		if (rest == RoutingRestriction.CAR) {
			relevantTypes.add(SegmentType.MOTORWAY);
			relevantTypes.add(SegmentType.PRIMARY);
			relevantTypes.add(SegmentType.SECONDARY);
			relevantTypes.add(SegmentType.TERTIARY);
			relevantTypes.add(SegmentType.RESIDENTIAL);
			relevantTypes.add(SegmentType.WORMHOLE);
			relevantTypes.add(SegmentType.SERVICE);
			relevantTypes.add(SegmentType.UNSURFACED);
		} else if (rest == RoutingRestriction.BIKE) {
			relevantTypes.add(SegmentType.CYCLEWAY);
			relevantTypes.add(SegmentType.UNSURFACED);
			relevantTypes.add(SegmentType.RESIDENTIAL);
			relevantTypes.add(SegmentType.TERTIARY);
			relevantTypes.add(SegmentType.SECONDARY);
			relevantTypes.add(SegmentType.WORMHOLE);
			relevantTypes.add(SegmentType.SERVICE);
		} else if (rest == RoutingRestriction.FOOT) {
			relevantTypes.add(SegmentType.CYCLEWAY);
			relevantTypes.add(SegmentType.UNSURFACED);
			relevantTypes.add(SegmentType.RESIDENTIAL);
			relevantTypes.add(SegmentType.TERTIARY);
			relevantTypes.add(SegmentType.SERVICE);
			relevantTypes.add(SegmentType.WORMHOLE);
			relevantTypes.add(SegmentType.FOOTWAY);
		}
	}

	public double calculateCompleteWeight(List<Segment> list, EdgeRating r) {
		double out = 0;
		Segment last = null;
		for (Segment currentSegment : list) {
			// TODO uncomment here
			// switch (r) {
			// case LENGTH:
			// out += currentSegment.getLength();
			// break;
			// case TIME:
			// out += currentSegment.getLength()
			// * computeFactor(currentSegment);
			// break;
			// case CONVENIENCE:
			// if (last != null)
			// out += Math.toDegrees(GpsTools.getAngleBetweenSegments(
			// last, currentSegment));
			// }
			out += rate(currentSegment, r, last);
			last = currentSegment;
		}
		return out;
	}

	protected double rate(Segment s, EdgeRating r, Segment previous) {
		switch (r) {
		case LENGTH:
			return s.get_length();
		case TIME:
			return s.get_length() * computeFactor(s);
		case CONVENIENCE:
			// TODO comment here
			// if (previous != null) {
			// int angle = GpsTools.getAngleBetweenSegments(
			// previous, s);
			// if(GpsTools.isNearer(previous, s)){
			// angle = 180 - angle;
			// }
			// System.out.println(angle);
			// return Math.abs(angle);
			// } else {
			// return 0;
			// }
			if (previous != null) {
				return (previous.get_wayId() == s.get_wayId()) ? s.get_length()
						: s.get_length() + INCONVENIENCEVALUE;
			}
			return s.get_length();
		default:
			return Double.MAX_VALUE;
		}
	}
}
