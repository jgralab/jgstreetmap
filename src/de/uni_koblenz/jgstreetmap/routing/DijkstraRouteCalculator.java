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
import de.uni_koblenz.jgstreetmap.osmschema.routing.ContainsSegment;
import de.uni_koblenz.jgstreetmap.osmschema.routing.Route;
import de.uni_koblenz.jgstreetmap.osmschema.routing.Segment;
import de.uni_koblenz.jgstreetmap.osmschema.routing.SegmentType;

public class DijkstraRouteCalculator implements RouteCalculator {

	protected OsmGraph theGraph;
	protected OsmDijkstraMarker dijkstraMarker;
	// protected RoutingRestriction rest;
	protected Set<SegmentType> relevantTypes;

	public DijkstraRouteCalculator(OsmGraph g, RoutingRestriction rest) {
		dijkstraMarker = null;
		theGraph = g;
		relevantTypes = new TreeSet<SegmentType>();
		setRestriction(rest);
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
			relevantTypes.add(SegmentType.SERVICE);
		} else if (rest == RoutingRestriction.FOOT) {
			relevantTypes.add(SegmentType.CYCLEWAY);
			relevantTypes.add(SegmentType.UNSURFACED);
			relevantTypes.add(SegmentType.RESIDENTIAL);
			relevantTypes.add(SegmentType.TERTIARY);
			relevantTypes.add(SegmentType.SERVICE);
			relevantTypes.add(SegmentType.FOOTWAY);
		}

	}

	protected class OsmDijkstraMarker extends
			GraphMarker<OsmDijkstraMarker.Marker> {

		public OsmDijkstraMarker() {
			super(theGraph);
		}

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

		/** Initializes a vertex with a new Marker. */
		public void init(Vertex v) {
			mark(v, new Marker());
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

		/**
		 * Checks if the Vertex <code>v</code> is already done.
		 * 
		 * @return true if the Vertex was already handled.
		 */
		public boolean isDone(Node v) {
			return getMark(v).done;
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
	}

	@Override
	public Route calculateShortestRoute(Node start, Node end) {
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
		for (Node currentNode : theGraph.getNodeVertices()) {
			queue.add(currentNode);
		}

		// dijkstra algorithm main loop
		while (!queue.isEmpty()) {
			// retreive vertex with smallest distance and mark as "done"
			Node currentVertex = queue.poll();

			// System.out.println("Taken vertex " + currentVertex);

			// System.out.println(dijkstraMarker.getDistance(currentVertex));
			dijkstraMarker.done(currentVertex);
			double currentDistance = dijkstraMarker.getDistance(currentVertex);

			// System.out.println(currentDistance);

			// follow each traverseable edge
			List<Segment> relevantSegments = getRelevantEdges(currentVertex);
			// Segment currentEdge = parameters.getFirstEdge(currentVertex);
			// Segment currentEdge;
			// Iterator<Segment> edgeIterator= relevantSegments.iterator();

			for (Segment currentEdge : relevantSegments) {

				Node nextVertex = getNextVertex(currentVertex, currentEdge);

				double newDistance = currentDistance + currentEdge.getLength();
				// System.out.println(parameters.getWeight(currentEdge));
				// if the new path is shorter than the distance stored
				// at the other end, this new value is stored
				if (dijkstraMarker.getDistance(nextVertex) > newDistance) {
					dijkstraMarker.setNewDistance(nextVertex, newDistance,
							currentVertex, currentEdge);
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
				// currentEdge = parameters.getNextEdge(currentEdge);
			}
		}
		System.out.println(dijkstraMarker.getDistance(end));
		return traceback(start, end);
	}

	private Route traceback(Node start, Node target) {
		Stack<Segment> routesegments = new Stack<Segment>();
		if (dijkstraMarker.getPreviousSegment(target) == null) {
			return null;
		}
		Node currentNode = target;
		while (dijkstraMarker.getPreviousSegment(currentNode) != null) {
			routesegments.push(dijkstraMarker.getPreviousSegment(currentNode));
			currentNode = dijkstraMarker.getPreviousNode(currentNode);
		}
		Route r = theGraph.createRoute();
		ContainsSegment currentEdge;
		Segment currentSegment;
		currentNode = start;
		while (routesegments.size() > 0) {
			currentSegment = routesegments.pop();
			currentEdge = r.addSegment(currentSegment);
			if (currentSegment.getSourceList().get(0) == currentNode) {
				currentEdge.setOpposite(false);
				currentNode = currentSegment.getTargetList().get(0);
			} else {
				currentEdge.setOpposite(true);
				currentNode = currentSegment.getSourceList().get(0);
			}
		}
		return r;
	}

	private List<Segment> getRelevantEdges(Node n) {
		List<Segment> out = new LinkedList<Segment>();
		// all edges in normal direction
		for (Segment currentSegment : n.getSegmentToTargetList()) {
			if (relevantTypes.contains(currentSegment.getWayType())) {
				out.add(currentSegment);
			}
		}
		// all edges in reversed direction if not oneway
		for (Segment currentSegment : n.getSegmentToSourceList()) {
			if (!currentSegment.isOneway()
					&& relevantTypes.contains(currentSegment.getWayType())) {
				out.add(currentSegment);
			}
		}
		return out;
	}

	private Node getNextVertex(Node v, Segment e) {
		if (e.getSourceList().get(0) == v) {
			// System.out.println("n");
			return e.getTargetList().get(0);
		} else if (e.getTargetList().get(0) == v) {
			// System.out.println("r");
			return e.getSourceList().get(0);
		}
		return null;
	}

}
