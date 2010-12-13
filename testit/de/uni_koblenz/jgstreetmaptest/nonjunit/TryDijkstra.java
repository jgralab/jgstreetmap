package de.uni_koblenz.jgstreetmaptest.nonjunit;

import de.uni_koblenz.jgralab.GraphIOException;
import de.uni_koblenz.jgralab.impl.ConsoleProgressFunction;
import de.uni_koblenz.jgstreetmap.osmschema.OsmGraph;
import de.uni_koblenz.jgstreetmap.osmschema.OsmSchema;
import de.uni_koblenz.jgstreetmap.osmschema.map.Node;
import de.uni_koblenz.jgstreetmap.routing.DijkstraRouteCalculator;
import de.uni_koblenz.jgstreetmap.routing.RouteCalculator.EdgeRating;

public class TryDijkstra {
	public static void main(String[] args) throws GraphIOException {
		OsmGraph graph = OsmSchema.instance().loadOsmGraphWithSavememSupport(
				"OsmGraph.tg.gz", new ConsoleProgressFunction());

		System.out.println("Using internal Dijkstra implementation...");
		for (int i = 0; i < 20; i++) {
			runOnInternalImplementation(graph);
		}
		System.out.println("Done.");

		System.out.println("Skipping algolib implementation.");

		System.out.println("Fini.");
	}

	private static void runOnInternalImplementation(OsmGraph graph) {
		DijkstraRouteCalculator routing1 = new DijkstraRouteCalculator(graph);
		routing1.setStart((Node) graph.getVertex(24175));
		long time = System.currentTimeMillis();
		routing1.calculateShortestRoutes(EdgeRating.LENGTH);
		long duration = System.currentTimeMillis() - time;
		System.out.println("Duration: " + duration + " msecs.");
	}
}
