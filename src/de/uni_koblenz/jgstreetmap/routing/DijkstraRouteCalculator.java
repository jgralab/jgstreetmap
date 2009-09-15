package de.uni_koblenz.jgstreetmap.routing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Stack;
import java.util.TreeSet;

import de.uni_koblenz.jgralab.GraphMarker;
import de.uni_koblenz.jgstreetmap.osmschema.OsmGraph;
import de.uni_koblenz.jgstreetmap.osmschema.map.Node;
import de.uni_koblenz.jgstreetmap.osmschema.routing.Segment;
import de.uni_koblenz.jgstreetmap.osmschema.routing.SegmentType;

public class DijkstraRouteCalculator extends RouteCalculator {

	private class DijkstraMarker {

		/** indicates that Dijkstra is done with the vertex */
		boolean done;

		/** * records the distance to the start vertex */
		double distance;

		/** stores the predecessor in the path from the start vertex */
		Segment parentSegment;

		DijkstraMarker(double d, Segment s) {
			distance = d;
			parentSegment = s;
		}
	}

	protected GraphMarker<DijkstraMarker> dijkstraMarker;
	PriorityQueue<Node> queue;

	// protected RoutingRestriction rest;

	protected boolean routesCalculated;

	protected boolean startChanged;

	public DijkstraRouteCalculator(OsmGraph g) {
		dijkstraMarker = null;
		graph = g;
		relevantTypes = new TreeSet<SegmentType>();
		setRestriction(RoutingRestriction.CAR);
		startChanged = false;
		routesCalculated = false;
		speeds = new Speed();
	}

	public void calculateShortestRoutes(EdgeRating rating) {
		if (start == null) {
			throw new IllegalStateException(
					"setStart() must be called before invoking this method!");
		}
		if (dijkstraMarker != null) {
			dijkstraMarker.clear();
		} else {
			dijkstraMarker = new GraphMarker<DijkstraMarker>(graph);
		}

		if (queue == null) {
			queue = new PriorityQueue<Node>(128, new Comparator<Node>() {
				public int compare(Node a, Node b) {
					return Double.compare(dijkstraMarker.getMark(a).distance,
							dijkstraMarker.getMark(b).distance);
				}
			});
		} else {
			queue.clear();
		}

		dijkstraMarker.mark(start, new DijkstraMarker(0, null));
		queue.offer(start);
		// dijkstra algorithm main loop
		while (!queue.isEmpty()) {
			// retrieve vertex with smallest distance and mark as "done"
			Node currentVertex = queue.poll();

			DijkstraMarker m = dijkstraMarker.getMark(currentVertex);
			m.done = true;

			// follow each traverseable edge
			for (Segment currentSegment : currentVertex.getSegmentIncidences()) {
				if (relevantTypes.contains(currentSegment.getWayType())
						&& (currentSegment.isNormal() || !currentSegment
								.isOneway())) {

					Node nextVertex = (Node) currentSegment.getThat();

					double newDistance = m.distance
							+ rate(currentSegment, rating, m.parentSegment);
					// if the new path is shorter than the distance stored
					// at the other end, this new value is stored
					DijkstraMarker n = dijkstraMarker.getMark(nextVertex);
					if (n == null) {
						dijkstraMarker.mark(nextVertex, new DijkstraMarker(
								newDistance, currentSegment));
						queue.offer(nextVertex); // position in the queue
					} else if (n.distance > newDistance) {
						n.distance = newDistance;
						n.parentSegment = currentSegment;
						if (!n.done) {
							queue.remove(nextVertex);
							queue.offer(nextVertex);
						}
					}
				}
			}
		}
		startChanged = false;
		routesCalculated = true;
	}

	@Override
	public RoutingResult getRoute(Node target, EdgeRating rating) {
		long startTime = System.currentTimeMillis();

		if (!routesCalculated || startChanged) {
			calculateShortestRoutes(rating);
		}

		DijkstraMarker m = dijkstraMarker.getMark(target);
		if ((m == null) || (m.parentSegment == null)) {
			return new RoutingResult(null, System.currentTimeMillis()
					- startTime);
		}
		Stack<Segment> routesegments = new Stack<Segment>();
		while ((m != null) && (m.parentSegment != null)) {
			routesegments.push(m.parentSegment);
			m = dijkstraMarker.getMark(m.parentSegment.getThis());
		}

		List<Segment> out = new ArrayList<Segment>(routesegments.size());
		while (!routesegments.empty()) {
			out.add(routesegments.pop());
		}
		return new RoutingResult(out, System.currentTimeMillis() - startTime);
	}

	@Override
	public void setStart(Node start) {
		this.start = start;
		startChanged = true;
	}
}
