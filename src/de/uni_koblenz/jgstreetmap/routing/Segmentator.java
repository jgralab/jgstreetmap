package de.uni_koblenz.jgstreetmap.routing;

import static java.lang.Math.abs;
import static java.lang.Math.cos;
import static java.lang.Math.sqrt;
import static java.lang.Math.toRadians;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import de.uni_koblenz.jgralab.GraphIO;
import de.uni_koblenz.jgralab.GraphIOException;
import de.uni_koblenz.jgralab.Vertex;
import de.uni_koblenz.jgralab.impl.ProgressFunctionImpl;
import de.uni_koblenz.jgstreetmap.model.AnnotatedOsmGraph;
import de.uni_koblenz.jgstreetmap.osmschema.Node;
import de.uni_koblenz.jgstreetmap.osmschema.OsmGraph;
import de.uni_koblenz.jgstreetmap.osmschema.OsmSchema;
import de.uni_koblenz.jgstreetmap.osmschema.Way;
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

	private static OsmGraph theGraph;
	public static final int MINUTEMETER = 1852;

	private static List<Way> computeRelevantWays(Iterable<Vertex> vertices) {
		List<Way> output = new LinkedList<Way>();
		int amount = 0;
		int added = 0;
		Way currentWay;
		for (Vertex current : vertices) {
			amount++;
			currentWay = (Way) current;
			if (!currentWay.getWayType().equals(SegmentType.NOWAY)) {
				added++;
				output.add(currentWay);
			}
		}
		System.out.print("added " + added + " out of " + amount + "...");
		return output;
	}

	public static SegmentType computeSegmentType(String highwayValue) {
		if (highwayValue.equals("motorway")
				|| highwayValue.equals("motorway_link")
				|| highwayValue.equals("motorway_trunk")) {
			return SegmentType.MOTORWAY;
		} else if (highwayValue.equals("primary")
				|| highwayValue.equals("primary_link")
				|| highwayValue.equals("trunk")
				|| highwayValue.equals("trunk_link")) {
			return SegmentType.PRIMARY;
		} else if (highwayValue.equals("secondary")
				|| highwayValue.equals("secondary_link")) {
			return SegmentType.SECONDARY;
		} else if (highwayValue.equals("tertiary")) {
			return SegmentType.TERTIARY;
		} else if (highwayValue.equals("residential")
				|| highwayValue.equals("living_street")
				|| highwayValue.equals("minor")) {
			return SegmentType.RESIDENTIAL;
		} else if (highwayValue.equals("footway")
				|| highwayValue.equals("cycleway")
				|| highwayValue.equals("pedestrian")
				|| highwayValue.equals("steps") || highwayValue.equals("track")
				|| highwayValue.equals("foot")) {
			return SegmentType.FOOTWAY;
		} else if (highwayValue.equals("unsurfaced")) {
			return SegmentType.UNSURFACED;
		} else if (highwayValue.equals("construction")) {
			return SegmentType.NOWAY;
		} else if (highwayValue.equals("unclassified")) {
			return SegmentType.TERTIARY;
		} else if (highwayValue.equals("service")) {
			return SegmentType.SERVICE;
		} else {
			System.out.println("Unknown highway tag'" + highwayValue);
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
				currentWay.setWayType(computeSegmentType(currentValue));
			} else {
				currentWay.setWayType(SegmentType.NOWAY);
			}
		}
	}

	private static void createSegment(OsmGraph theGraph, Node source,
			Node target, Way currentWay, boolean oneway) {
		Segment newSegment = theGraph.createSegment(source, target);
		newSegment.setLength(distance(source, target));
		newSegment.setOneway(oneway);
		newSegment.setWayType(currentWay.getWayType());
		newSegment.setWayId(currentWay.getOsmId());
	}

	public static double distance(Node n1, Node n2) {
		return distance(n1.getLatitude(), n2.getLatitude(),n1.getLongitude(), n2.getLongitude());
		// double deltaLat = abs(n1.getLatitude() - n2.getLatitude());
		// double deltaLon = abs(n1.getLongitude() - n2.getLongitude());
		// double latMiddle = (n1.getLatitude() + n2.getLatitude()) / 2.0;
		// double k1 = deltaLat * 60 * MINUTEMETER;
		// double k2 = deltaLon * 60 * MINUTEMETER * cos(toRadians(latMiddle));
		//		return sqrt(k1 * k1 + k2 * k2);
	}

	public static double distance(double lat1, double lat2, double lon1,
			double lon2) {
		double deltaLat = abs(lat1 - lat2);
		double deltaLon = abs(lon1 - lon2);
		double latMiddle = (lat1 + lat2) / 2.0;
		double k1 = deltaLat * 60 * MINUTEMETER;
		double k2 = deltaLon * 60 * MINUTEMETER * cos(toRadians(latMiddle));
		return sqrt(k1 * k1 + k2 * k2);
	}

	public static boolean isIntersection(Node currentNode) {
		int relevantWayAmount = 0;
		for (Way currentWay : currentNode.getWayList()) {
			if (currentWay.getWayType() != SegmentType.NOWAY) {
				relevantWayAmount++;
			}
		}
		return (relevantWayAmount >= 2);

	}

	private static boolean isOneway(Way w) {
		String oneway = AnnotatedOsmGraph.getTag(w, "oneway");
		return (oneway != null && w.getWayType() != SegmentType.NOWAY && (oneway
				.equalsIgnoreCase("yes") || oneway.equalsIgnoreCase("true")));
	}

	public static void main(String[] args) {
		String sourceGraphFilename = (args.length > 0) ? args[0]
				: "OsmGraph.tg";
		String targetGraphFilename = (args.length > 1) ? args[1]
				: "OsmGraphRouting.tg";
		theGraph = null;
		try {
			System.out.println("Loading graph...");
			theGraph = OsmSchema.instance().loadOsmGraph(sourceGraphFilename,
					new ProgressFunctionImpl());
			segmentateGraph(theGraph);

			System.out.println("Storing the graph...");
			GraphIO.saveGraphToFile(targetGraphFilename, theGraph,
					new ProgressFunctionImpl());

		} catch (GraphIOException e) {
			e.printStackTrace();
		}

		System.out.println("finito");

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
		Iterator<? extends Node> iter = currentWay.getNodeList().iterator();
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
