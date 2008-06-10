package de.uni_koblenz.jgstreetmap.routing;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import de.uni_koblenz.jgralab.GraphMarker;
import de.uni_koblenz.jgralab.Vertex;
import de.uni_koblenz.jgstreetmap.osmschema.Node;
import de.uni_koblenz.jgstreetmap.osmschema.OsmGraph;
import de.uni_koblenz.jgstreetmap.osmschema.routing.Segment;
import de.uni_koblenz.jgstreetmap.osmschema.routing.SegmentType;

public class DijkstraRouteCalculator {

	public enum Direction {
		NORMAL, REVERSED;
	}

	public enum EdgeRating {
		LENGTH, TIME;
	}

	protected class OsmDijkstraMarker extends
			GraphMarker<OsmDijkstraMarker.Marker> {

		/** Marker class for temporary attributes of Dijkstra algorithm */
		private class Marker {

			/** indicates that Dijkstra is done with the vertex */
			boolean done;

			/** * records the distance to the start vertex */
			double distance = Double.MAX_VALUE;;

			/** stores the predecessor in the path from the start vertex */
			Node prev;
			Segment sourceSegment;

		}

		public OsmDijkstraMarker() {
			super(theGraph);
		}

		/** Marks the Vertex <code>v</code> as "done". */
		public void done(Node v) {
			getMark(v).done = true;
		}

		/**
		 * Gets the current distance of Vertex <code>v</code> to the start
		 * vertex.
		 * 
		 * @return the current distance
		 */
		public double getDistance(Node v) {
			// System.out.println(v);
			return getMark(v).distance;
		}

		public Node getPreviousNode(Node v) {
			return getMark(v).prev;
		}

		public Segment getPreviousSegment(Node v) {
			return getMark(v).sourceSegment;
		}

		/** Initializes a vertex with a new Marker. */
		public void init(Vertex v) {
			mark(v, new Marker());
		}

		/**
		 * Checks if the Vertex <code>v</code> is already done.
		 * 
		 * @return true if the Vertex was already handled.
		 */
		public boolean isDone(Node v) {
			return getMark(v).done;
		}

		/**
		 * Sets the distance at Vertex <code>v</code> to
		 * <code>newDistance</code> and stores the possibly new predecessor<code>previousVertex</code>
		 * in the path from the start vertex.
		 */
		public void setNewDistance(Node v, double newDistance,
				Node previousVertex, Segment sourceSegment) {
			Marker m = getMark(v);
			m.distance = newDistance;
			m.prev = previousVertex;
			m.sourceSegment = sourceSegment;
		}
	}

	public enum RoutingRestriction {
		CAR, BIKE, FOOT
	}

	// public class SegmentDirectionTuple {
	// public Segment segment;
	// public Direction direction;
	// }

	public class Speed {
		public double cycle = 15;
		public double motorway = 110;
		public double countryroad = 60;
		public double residential = 40;
		public double footway = 5;
		public double unsurfaced = 20;
		public double service = 10;
	}

	protected OsmGraph theGraph;
	protected OsmDijkstraMarker dijkstraMarker;

	// protected RoutingRestriction rest;
	protected Set<SegmentType> relevantTypes;

	protected Node start;

	protected boolean routesCalculated;

	protected boolean startChanged;

	protected Speed speeds;

	public DijkstraRouteCalculator(OsmGraph g) {
		dijkstraMarker = null;
		theGraph = g;
		relevantTypes = new TreeSet<SegmentType>();
		setRestriction(RoutingRestriction.CAR);
		startChanged = false;
		routesCalculated = false;
		speeds = new Speed();
	}

	public double calculateCompleteWeight(List<Segment> list, EdgeRating r) {
		double out = 0;
		for (Segment currentSegment : list) {
			switch (r) {
			case LENGTH:
				out += currentSegment.getLength();
				break;
			case TIME:
				out += currentSegment.getLength()
						* computeFactor(currentSegment);
			}
		}
		return out;
	}

	public void calculateShortestRoutes(EdgeRating r) {
		// long timestamp1 = System.currentTimeMillis();
		if (start == null) {
			throw new IllegalStateException(
					"start must be set before invoking this method!");
		}
		dijkstraMarker = new OsmDijkstraMarker();
		for (Node currentNode : theGraph.getNodeVertices()) {
			dijkstraMarker.init(currentNode);
		}

		dijkstraMarker.setNewDistance(start, 0, null, null);

		PriorityQueue<Node> queue = new PriorityQueue<Node>(start.getGraph()
				.getVCount(), new Comparator<Node>() {
			public int compare(Node a, Node b) {
				return Double.compare(dijkstraMarker.getDistance(a),
						dijkstraMarker.getDistance(b));
			}
		});

		// add all vertices into priority queue
		// for (Node currentNode : theGraph.getNodeVertices()) {
		// queue.add(currentNode);
		// }

		queue.add(start);

		// dijkstra algorithm main loop
		while (!queue.isEmpty()) {
			// retreive vertex with smallest distance and mark as "done"
			Node currentVertex = queue.poll();

			// System.out.println("Taken vertex " + currentVertex);

			// System.out.println(dijkstraMarker.getDistance(currentVertex));
			dijkstraMarker.done(currentVertex);
			double currentDistance = dijkstraMarker.getDistance(currentVertex);

			// follow each traverseable edge
			for (Segment currentSegment : currentVertex.getSegmentIncidences()) {
				if (relevantTypes.contains(currentSegment.getWayType())
						&& (currentSegment.isNormal() || !currentSegment
								.isOneway())) {

					Node nextVertex = (Node) currentSegment.getThat();

					double newDistance = currentDistance
							+ rate(currentSegment, r);
					// System.out.println(parameters.getWeight(currentEdge));
					// if the new path is shorter than the distance stored
					// at the other end, this new value is stored
					if (dijkstraMarker.getDistance(nextVertex) > newDistance) {
						dijkstraMarker.setNewDistance(nextVertex, newDistance,
								currentVertex, currentSegment);
						// if the otherEnd was not handled, re-queue it to
						// get the order of the priority queue right
						if (!dijkstraMarker.isDone(nextVertex)) {
							// System.out.println("reenqueued vertex " +
							// nextVertex);
							queue.remove(nextVertex); // put the vertex at the
							// right
							queue.add(nextVertex); // position in the queue
						}
					}
				}
			}
		}
		// long timestamp2 = System.currentTimeMillis();
		// System.out.println("Dijkstra calculation completed in "
		// + (timestamp2 - timestamp1) / 1000.0 + " seconds.");
		// System.out.println(dijkstraMarker.getDistance(end));
		// return traceback(start, end);
		startChanged = false;
		routesCalculated = true;
	}

	private double computeFactor(Segment s) {
		switch (s.getWayType()) {
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
	
	public List<Segment> getRoute(Node target) {
		if (!routesCalculated) {
			throw new IllegalStateException(
					"Routes must be calculatet before invoking this method.");
		}
		if (startChanged) {
			throw new IllegalStateException(
					"start must not be changed after calculating the routes.");
		}
		Stack<Segment> routesegments = new Stack<Segment>();
		if (dijkstraMarker.getPreviousSegment(target) == null) {
			return null;
		}
		Node currentNode = target;
		while (dijkstraMarker.getPreviousSegment(currentNode) != null) {
			routesegments.push(dijkstraMarker.getPreviousSegment(currentNode));
			currentNode = dijkstraMarker.getPreviousNode(currentNode);
		}

		List<Segment> out = new LinkedList<Segment>();

		// Segment currentSegment;
		currentNode = start;
		Segment currentSegment;
		while (routesegments.size() > 0) {
			currentSegment = routesegments.pop();
			currentNode = (Node) currentSegment.getThat();
			out.add(currentSegment);
		}
		return out;
	}

	public Node getStart() {
		return start;
	}

	protected double rate(Segment s, EdgeRating r) {
		switch (r) {
		case LENGTH:
			return s.getLength();
		case TIME:
			return s.getLength() * computeFactor(s);
		default:
			return Double.MAX_VALUE;
		}
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

	public void setStart(Node start) {
		this.start = start;
		this.startChanged = true;
		// calculateShortestRoute();
	}

}
