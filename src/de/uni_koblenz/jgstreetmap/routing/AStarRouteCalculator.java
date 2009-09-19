/**
 *
 */
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

/**
 * @author Tassilo Horn
 * 
 */
public class AStarRouteCalculator extends RouteCalculator {
	GraphMarker<AStarMark> marker;
	PriorityQueue<Node> queue;

	private class AStarMark {
		/** indicates that AStar is done with the vertex */
		boolean done;

		/** records the distance to the start vertex + the heuristic value */
		double fvalue;

		/** records the distance to the start vertex */
		double distance;

		/** stores the predecessor in the path from the start vertex */
		Segment parentSegment;

		AStarMark(double f, double d, Segment s) {
			fvalue = f;
			distance = d;
			parentSegment = s;
		}
	}

	public AStarRouteCalculator(OsmGraph graph) {
		this.graph = graph;
		marker = null;
		relevantTypes = new TreeSet<SegmentType>();
		setRestriction(RoutingRestriction.CAR);
		speeds = new Speed();
	}

	@Override
	public RoutingResult getRoute(Node target, EdgeRating r) {
		long startTime = System.currentTimeMillis();

		if (marker != null) {
			marker.clear();
		} else {
			marker = new GraphMarker<AStarMark>(graph);
		}

		if (queue == null) {
			queue = new PriorityQueue<Node>(128, new Comparator<Node>() {
				@Override
				public int compare(Node a, Node b) {
					return Double.compare(marker.getMark(a).fvalue, marker
							.getMark(b).fvalue);
				}
			});
		} else {
			queue.clear();
		}

		marker.mark(start, new AStarMark(calculateHeuristic(start, target, r),
				0, null));
		queue.offer(start);

		while (!queue.isEmpty()) {
			Node currentVertex = queue.poll();
			AStarMark mark = marker.getMark(currentVertex);
			mark.done = true;

			for (Segment currentSegment : currentVertex.getSegmentIncidences()) {
				if (relevantTypes.contains(currentSegment.get_wayType())
						&& (currentSegment.isNormal() || !currentSegment
								.is_oneway())) {
					Node nextVertex = (Node) currentSegment.getThat();
					double newDist = mark.distance
							+ rate(currentSegment, r, mark.parentSegment);
					double heuristic = calculateHeuristic(nextVertex, target, r);
					double newFValue = newDist + heuristic;
					AStarMark nextMark = marker.getMark(nextVertex);
					if (nextMark == null) {
						marker.mark(nextVertex, new AStarMark(newFValue,
								newDist, currentSegment));
						queue.offer(nextVertex);
					} else if (nextMark.distance > newDist) {
						nextMark.fvalue = newFValue;
						nextMark.distance = newDist;
						nextMark.parentSegment = currentSegment;
						if (!nextMark.done) {
							queue.remove(nextVertex);
							queue.offer(nextVertex);
						}
					}

					if (nextVertex == target) {
						System.out.println("Target found!");
						Stack<Segment> routesegments = new Stack<Segment>();
						AStarMark targetMark = marker.getMark(target);
						while (targetMark != null
								&& targetMark.parentSegment != null) {
							routesegments.push(targetMark.parentSegment);
							targetMark = marker
									.getMark(targetMark.parentSegment.getThis());
						}

						List<Segment> out = new ArrayList<Segment>(
								routesegments.size());
						while (!routesegments.empty()) {
							out.add(routesegments.pop());
						}
						// printRoute(out, r);
						return new RoutingResult(out, System
								.currentTimeMillis()
								- startTime);
					}
				} else {
					// System.out.println("Segment " + currentSegment
					// + " is not relevant.");
				}
			}
		}
		// System.out.println("Returning null route!");
		return new RoutingResult(null, System.currentTimeMillis() - startTime);
	}

	private double calculateHeuristic(Node start, Node target, EdgeRating r) {
		if (start == target) {
			return 0;
		}

		double sLat = start.get_latitude() * Math.PI / 180;
		double sLong = start.get_longitude() * Math.PI / 180;
		double tLat = target.get_latitude() * Math.PI / 180;
		double tLong = target.get_longitude() * Math.PI / 180;
		double earthRadius = 6378.3 * 1000;

		double dist = Math.acos(Math.sin(sLat) * Math.sin(tLat)
				+ Math.cos(sLat) * Math.cos(tLat) * Math.cos(sLong - tLong))
				* earthRadius;

		// System.out.println("dist between " + start + " and target " + target
		// + " = " + dist + " meters.");

		switch (r) {
		case LENGTH:
			return dist;
		case TIME:
			return dist * 3.6 / speeds.motorway;
		case CONVENIENCE:
			return 0;
		default:
			return 0;
		}
	}
}
