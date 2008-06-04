package de.uni_koblenz.jgstreetmap.routing;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

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
import static java.lang.Math.abs;
import static java.lang.Math.cos;
import static java.lang.Math.toRadians;
import static java.lang.Math.sqrt;

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
 * - calculate the length between neighboring nodes and create the sum.
 *    and use it as attribute for length (in meters).
 * + check if the segment belongs to a oneway-Way and store this information in
 *   the segment.
 */
public class RoutingEdgesAttacher {

	private static OsmGraph theGraph;
	public static final int MINUTEMETER = 1852;

	private static class Segmentation {
		private Way way;
		private List<Node> segmentNodes;
		private Node last;

		public Segmentation(Way way) {
			this.way = way;
			segmentNodes = new LinkedList<Node>();
		}

		public int getAmount() {
			return segmentNodes.size();
		}

		private double distance(Node n1, Node n2) {
			double deltaLat = abs(n1.getLatitude() - n2.getLatitude());
			double deltaLon = abs(n1.getLongitude() - n2.getLongitude());
			double latMiddle = (n1.getLatitude() + n2.getLatitude()) / 2.0;
			double k1 = deltaLat * 60 * MINUTEMETER;
			double k2 = deltaLon * 60 * MINUTEMETER * cos(toRadians(latMiddle));
			return sqrt(k1 * k1 + k2 * k2);
		}

		public double getLength() {
			Iterator<Node> nodeIterator = segmentNodes.iterator();
			Node currentNode = null;
			Node newNode = null;
			try {
				currentNode = nodeIterator.next();
			} catch (NoSuchElementException e) {
				return 0;
			}
			double length = 0;
			while (nodeIterator.hasNext()) {
				newNode = nodeIterator.next();
				length += distance(currentNode, newNode);
				currentNode = newNode;
			}
			return length;
		}

		public void addNode(Node n) {
			segmentNodes.add(n);
			last = n;
		}

		public Way getWay() {
			return way;
		}

		public Node getStart() {
			return segmentNodes.get(0);
		}

		public Node getEnd() {
			return last;
		}
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
			System.out.print("Computing tags for the ways...");
			computeTags(theGraph.vertices(Way.class));
			System.out.println("done");
			System.out.print("Computing relevant Ways...");
			List<Way> relevantWays = computeRelevantWays(theGraph
					.vertices(Way.class));
			System.out.println("done");
			System.out.print("Dividing Ways into Segments...");
			List<Segmentation> segmentations = computeSegmentations(relevantWays);
			System.out.println("done");
			System.out
					.println("Created " + segmentations.size() + " segments.");
			System.out.print("Creating edges from Segments...");
			createSegmentEdges(segmentations);
			System.out.println("done");
			System.out.println("Storing the graph...");
			GraphIO.saveGraphToFile(targetGraphFilename, theGraph,
					new ProgressFunctionImpl());

		} catch (GraphIOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("finito");

	}

	public static boolean isOneway(Way w) {
		String oneway = AnnotatedOsmGraph.getTag(w, "oneway");
		if (oneway != null
				&& (oneway.equalsIgnoreCase("yes") || oneway
						.equalsIgnoreCase("true")))
			return true;
		else
			return false;
	}

	private static void createSegmentEdges(List<Segmentation> segmentations) {
		for (Segmentation currentSegmentation : segmentations) {
			createSegmentEdge(currentSegmentation);
		}
	}

	private static void createSegmentEdge(Segmentation s) {
		Segment newSegment = theGraph.createSegment();
		Way currentWay = s.getWay();
		newSegment.setLength(s.getLength());
		newSegment.setOneway(isOneway(currentWay));
		newSegment.setOneway(false);
		newSegment.setWayType(currentWay.getWayType());
		currentWay.addSegment(newSegment);
		newSegment.addSource(s.getStart());
		newSegment.addTarget(s.getEnd());
	}

	private static List<Segmentation> computeSegmentations(
			List<Way> relevantWays) {
		List<Segmentation> output = new LinkedList<Segmentation>();
		for (Way currentWay : relevantWays) {
			boolean open = false;
			Segmentation currentSeg = new Segmentation(currentWay);
			for (Node currentNode : currentWay.getNodeList()) {
				if (!open) {
					open = true;
					currentSeg.addNode(currentNode);
				} else {
					if (isIntersection(currentNode)) {
						open = false;
						currentSeg.addNode(currentNode);
						output.add(currentSeg);
						currentSeg = new Segmentation(currentWay);
					} else {
						currentSeg.addNode(currentNode);
					}
				}
			}
			if (currentSeg.getAmount() > 0) {
				output.add(currentSeg);
			}
		}
		return output;
	}

	private static boolean isIntersection(Node currentNode) {
		int relevantWayAmount = 0;
		for (Way currentWay : currentNode.getWayList()) {
			if (!currentWay.getWayType().equals(SegmentType.NOWAY)) {
				relevantWayAmount++;
			}
		}
		if (relevantWayAmount > 1) {
			return true;
		} else {
			return false;
		}
	}

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
			return SegmentType.NOWAY;
		} else if (highwayValue.equals("service")) {
			return SegmentType.SERVICE;
		} else {
			System.out.println("Unknown highway tag'" + highwayValue);
			return SegmentType.NOWAY;
		}
	}
}
