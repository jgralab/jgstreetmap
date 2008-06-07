package de.uni_koblenz.jgstreetmap.routing;

import de.uni_koblenz.jgralab.GraphIOException;
import de.uni_koblenz.jgralab.impl.ProgressFunctionImpl;
import de.uni_koblenz.jgstreetmap.osmschema.Node;
import de.uni_koblenz.jgstreetmap.osmschema.OsmGraph;
import de.uni_koblenz.jgstreetmap.osmschema.OsmSchema;
import de.uni_koblenz.jgstreetmap.osmschema.routing.Route;
import de.uni_koblenz.jgstreetmap.osmschema.routing.Segment;

public class TryRouting {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// read graph
		String sourceGraphFilename = "OsmGraphRouting.tg";
		OsmGraph theGraph = null;
		try {
			System.out.println("Loading graph...");
			theGraph = OsmSchema.instance().loadOsmGraph(sourceGraphFilename,
					new ProgressFunctionImpl());
		} catch (GraphIOException e) {
			throw new RuntimeException("Graph could not be loaded.");
		}
		// OsmParameters params = new OsmParameters(theGraph);
		RouteCalculator d = new DijkstraRouteCalculator(theGraph, RoutingRestriction.CAR);
		Node start = (Node) theGraph.getVertex(23);
		Node target = (Node) theGraph.getVertex(50);
		Route r = d.calculateShortestRoute(start, target);
		for(Segment currentSegment : r.getSegmentList()){
			System.out.println(currentSegment);
		}
		// double distance = d.dijkstra(start, target);
		// System.out.println("Distance: " + distance);
	}

}
