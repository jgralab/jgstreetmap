package de.uni_koblenz.jgstreetmaptest.nonjunit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Stack;
import java.util.TreeSet;

import de.uni_koblenz.jgralab.graphmarker.ArrayVertexMarker;
import de.uni_koblenz.jgralab.graphmarker.BitSetVertexMarker;
import de.uni_koblenz.jgralab.graphmarker.DoubleVertexMarker;
import de.uni_koblenz.jgstreetmap.osmschema.OsmGraph;
import de.uni_koblenz.jgstreetmap.osmschema.map.Node;
import de.uni_koblenz.jgstreetmap.osmschema.routing.Segment;
import de.uni_koblenz.jgstreetmap.osmschema.routing.SegmentType;
import de.uni_koblenz.jgstreetmap.routing.RouteCalculator;
import de.uni_koblenz.jgstreetmap.routing.RoutingResult;

public class ModifiedDijkstraRouteCalculator2 extends RouteCalculator {

	protected BitSetVertexMarker done;
	protected DoubleVertexMarker distance;
	protected ArrayVertexMarker<Segment> parentSegment;

	PriorityQueue<Node> queue;

	protected boolean routesCalculated;

	protected boolean startChanged;

	public ModifiedDijkstraRouteCalculator2(OsmGraph g) {
		done = null;
		distance = null;
		parentSegment = null;
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
		if (done != null) {
			done.clear();
		} else {
			done = new BitSetVertexMarker(graph);
		}

		if (distance != null) {
			distance.clear();
		} else {
			distance = new DoubleVertexMarker(graph);
		}

		if (parentSegment != null) {
			parentSegment.clear();
		} else {
			parentSegment = new ArrayVertexMarker<Segment>(graph);
		}

		if (queue == null) {
			queue = new PriorityQueue<Node>(128, new Comparator<Node>() {
				public int compare(Node a, Node b) {
					return Double.compare(distance.getMark(a),
							distance.getMark(b));
				}
			});
		} else {
			queue.clear();
		}

		distance.mark(start, 0);
		queue.offer(start);
		// dijkstra algorithm main loop
		while (!queue.isEmpty()) {
			// retrieve vertex with smallest distance and mark as "done"
			Node currentVertex = queue.poll();

			done.mark(currentVertex);

			// follow each traverseable edge
			for (Segment currentSegment : currentVertex.getSegmentIncidences()) {
				if (relevantTypes.contains(currentSegment.get_wayType())
						&& (currentSegment.isNormal() || !currentSegment
								.is_oneway())) {

					Node nextVertex = (Node) currentSegment.getThat();

					double newDistance = distance.getMark(currentVertex)
							+ rate(currentSegment, rating,
									parentSegment.get(currentVertex));
					// if the new path is shorter than the distance stored
					// at the other end, this new value is stored
					if (!distance.isMarked(nextVertex)) {
						distance.mark(nextVertex, newDistance);
						parentSegment.mark(nextVertex, currentSegment);
						queue.offer(nextVertex); // position in the queue
					} else if (distance.getMark(nextVertex) > newDistance) {
						distance.mark(nextVertex, newDistance);
						parentSegment.mark(nextVertex, currentSegment);
						if (!done.isMarked(nextVertex)) {
							// TODO This call is O(n) and makes the algorithm
							// damn slow!
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

		if (parentSegment.getMark(target) == null) {
			return new RoutingResult(null, System.currentTimeMillis()
					- startTime);
		}
		Stack<Segment> routesegments = new Stack<Segment>();
		Node currentNode = target;
		while ((parentSegment.getMark(currentNode) != null)) {
			routesegments.push(parentSegment.getMark(currentNode));
			currentNode = (Node) parentSegment.getMark(currentNode).getThis();
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
