package de.uni_koblenz.jgstreetmap.routing;

import de.uni_koblenz.jgralab.GraphIOException;
import de.uni_koblenz.jgralab.impl.ProgressFunctionImpl;
import de.uni_koblenz.jgstreetmap.osmschema.Node;
import de.uni_koblenz.jgstreetmap.osmschema.OsmGraph;
import de.uni_koblenz.jgstreetmap.osmschema.OsmSchema;
import de.uni_koblenz.jgstreetmap.osmschema.routing.Segment;
import de.uni_koblenz.jgstreetmap.routing.DijkstraRouteCalculator.EdgeRating;
import de.uni_koblenz.jgstreetmap.routing.DijkstraRouteCalculator.RoutingRestriction;

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
		DijkstraRouteCalculator d = new DijkstraRouteCalculator(theGraph);
		Node start = (Node) theGraph.getVertex(23);
		Node target = (Node) theGraph.getVertex(50);
		d.setStart(start);
		d.setRestriction(RoutingRestriction.CAR);
		d.calculateShortestRoutes(EdgeRating.LENGTH);
		for(Segment currentSegment : d.getRoute(target)){
			System.out.println(currentSegment);
		}
		// double distance = d.dijkstra(start, target);
		// System.out.println("Distance: " + distance);
	}

}
