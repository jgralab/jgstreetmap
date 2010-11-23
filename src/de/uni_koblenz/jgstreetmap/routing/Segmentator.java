package de.uni_koblenz.jgstreetmap.routing;

import static java.lang.Math.cos;
import static java.lang.Math.sqrt;
import static java.lang.Math.toRadians;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import de.uni_koblenz.jgralab.Vertex;
import de.uni_koblenz.jgstreetmap.model.AnnotatedOsmGraph;
import de.uni_koblenz.jgstreetmap.osmschema.OsmGraph;
import de.uni_koblenz.jgstreetmap.osmschema.map.Node;
import de.uni_koblenz.jgstreetmap.osmschema.map.Way;
import de.uni_koblenz.jgstreetmap.osmschema.routing.Segment;
import de.uni_koblenz.jgstreetmap.osmschema.routing.SegmentType;

/*
 * Plan:
 * + load the graph
 * + iterate over all Ways and evaluate the tags
 * + set the Attribute wayType according to the tags
 * + if the way is relevant save it in a list of ways that have to be segmented
 * + iterate over all relevant ways and all nodes contained in the ways, identify
 *   the nodes that form junctions between relevant ways (check with SegmentType)
 *   create a list containing tuples (Way, ListOfNodesForOneSegment)
 * + Create Segment "edges" (this includes embedding them in the graph
 * + calculate the length between neighboring nodes and create the sum.
 *    and use it as attribute for length (in meters).
 * + check if the segment belongs to a oneway-Way and store this information in
 *   the segment.
 */
public class Segmentator {

	private static List<Way> computeRelevantWays(Iterable<Vertex> vertices) {
		List<Way> output = new LinkedList<Way>();
		int amount = 0;
		int added = 0;
		Way currentWay;
		for (Vertex current : vertices) {
			amount++;
			currentWay = (Way) current;
			if (!currentWay.get_wayType().equals(SegmentType.NOWAY)) {
				added++;
				output.add(currentWay);
			}
		}
		System.out.print("added " + added + " out of " + amount + "...");
		return output;
	}

	public static SegmentType computeSegmentType(String highwayValue) {
		if (highwayValue.startsWith("motorway")) {
			return SegmentType.MOTORWAY;
		} else if (highwayValue.startsWith("primary")
				|| highwayValue.startsWith("trunk")) {
			return SegmentType.PRIMARY;
		} else if (highwayValue.startsWith("secondary")
				|| highwayValue.equals("road")) {
			return SegmentType.SECONDARY;
		} else if (highwayValue.equals("tertiary")) {
			return SegmentType.TERTIARY;
		} else if (highwayValue.equals("residential")
				|| highwayValue.equals("residental")
				|| highwayValue.equals("living_street")
				|| highwayValue.equals("turning_circle")
				|| highwayValue.equals("minor")) {
			return SegmentType.RESIDENTIAL;
		} else if (highwayValue.startsWith("foot")
				|| highwayValue.equals("bridleway")
				|| highwayValue.equals("pedestrian")
				|| highwayValue.equals("path") || highwayValue.equals("steps")
				|| highwayValue.equals("stepway")
				|| highwayValue.contains("track")
				|| highwayValue.equals("trak") || highwayValue.equals("tr")
				|| highwayValue.equals("elevator")
				|| highwayValue.equals("boardwalk")) {
			return SegmentType.FOOTWAY;
		} else if (highwayValue.startsWith("cycle")) {
			return SegmentType.CYCLEWAY;
		} else if (highwayValue.equals("unsurfaced")
				|| highwayValue.equals("unsealed")) {
			return SegmentType.UNSURFACED;
		} else if (highwayValue.equals("construction")
				|| highwayValue.equals("no") || highwayValue.equals("planned")
				|| highwayValue.equals("proposed")
				|| highwayValue.equals("raceway")
				|| highwayValue.equals("buslane")
				|| highwayValue.equals("bus_stop")
				|| highwayValue.equals("bus_guideway")
				|| highwayValue.equals("unknown")) {
			return SegmentType.NOWAY;
		} else if (highwayValue.equals("unclassified")) {
			return SegmentType.TERTIARY;
		} else if (highwayValue.startsWith("service")) {
			return SegmentType.SERVICE;
		} else {
			System.out.println("Unknown highway tag'" + highwayValue + "'!");
			return SegmentType.NOWAY;
		}
	}

	private static void computeTags(Iterable<Vertex> vertices) {
		Way currentWay;
		String currentValue;
		for (Vertex current : vertices) {
			currentWay = (Way) current;
			currentValue = AnnotatedOsmGraph.getTag(currentWay, "highway");
			if (currentValue != null) {
				currentWay.set_wayType(computeSegmentType(currentValue));
			} else if (AnnotatedOsmGraph.getTag(currentWay, "cycleway") != null) {
				currentWay.set_wayType(SegmentType.CYCLEWAY);

			} else {
				currentWay.set_wayType(SegmentType.NOWAY);
			}
		}
	}

	private static void createSegment(OsmGraph theGraph, Node source,
			Node target, Way currentWay, boolean oneway) {
		Segment newSegment = theGraph.createSegment(source, target);
		newSegment.set_length(distance(source, target));
		newSegment.set_oneway(oneway);
		newSegment.set_wayType(currentWay.get_wayType());
		newSegment.set_wayId(currentWay.get_osmId());
	}

	public static double distance(Node n1, Node n2) {
		return distance(n1.get_latitude(), n1.get_longitude(), n2
				.get_latitude(), n2.get_longitude());
	}

	public static double distance(double lat, double lon, Node n) {
		return distance(lat, lon, n.get_latitude(), n.get_longitude());
	}

	public static double distance(double lat1, double lon1, double lat2,
			double lon2) {
		double deltaLat = lat1 - lat2;
		double deltaLon = lon1 - lon2;
		double k1 = deltaLat * 60 * GpsTools.MINUTEMETER;
		double k2 = deltaLon * 60 * GpsTools.MINUTEMETER
				* cos(toRadians((lat1 + lat2) / 2.0));
		return sqrt(k1 * k1 + k2 * k2);
	}

	public static boolean isIntersection(Node currentNode) {
		int relevantWayAmount = 0;
		for (Way currentWay : currentNode.get_ways()) {
			if (currentWay.get_wayType() != SegmentType.NOWAY) {
				relevantWayAmount++;
			}
		}
		return (relevantWayAmount >= 2);

	}

	private static boolean isOneway(Way w) {
		String oneway = AnnotatedOsmGraph.getTag(w, "oneway");
		return (oneway != null && w.get_wayType() != SegmentType.NOWAY && (oneway
				.equalsIgnoreCase("yes") || oneway.equalsIgnoreCase("true")));
	}

	private static int segmentate(OsmGraph theGraph, List<Way> relevantWays) {
		int out = 0;
		for (Way currentWay : relevantWays) {
			out += segmentateWay(theGraph, currentWay);
		}
		return out;
	}

	public static void segmentateGraph(OsmGraph theGraph) {
		System.out.print("Computing tags for the ways...");
		computeTags(theGraph.vertices(Way.class));
		System.out.println("done");
		System.out.print("Computing relevant Ways...");
		List<Way> relevantWays = computeRelevantWays(theGraph
				.vertices(Way.class));
		System.out.println("done");

		System.out.println("Segmentating Ways...");
		int c = segmentate(theGraph, relevantWays);
		System.out.println("done");

		System.out.println(c + " segments created.");
	}

	private static int segmentateWay(OsmGraph theGraph, Way currentWay) {
		int out = 0;
		Iterator<? extends Node> iter = currentWay.get_nodes().iterator();
		Node source, target;
		if (iter.hasNext()) {
			boolean oneway = isOneway(currentWay);
			source = iter.next();
			while (iter.hasNext()) {
				target = iter.next();
				createSegment(theGraph, source, target, currentWay, oneway);
				out++;
				source = target;
			}
		}
		return out;
	}
}
