package de.uni_koblenz.jgstreetmaptest.nonjunit;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.uni_koblenz.jgralab.Edge;
import de.uni_koblenz.jgralab.GraphElement;
import de.uni_koblenz.jgralab.GraphIOException;
import de.uni_koblenz.jgralab.algolib.algorithms.AlgorithmTerminatedException;
import de.uni_koblenz.jgralab.algolib.algorithms.weighted_shortest_paths.DijkstraAlgorithm;
import de.uni_koblenz.jgralab.algolib.functions.BooleanFunction;
import de.uni_koblenz.jgralab.algolib.functions.DoubleFunction;
import de.uni_koblenz.jgralab.algolib.functions.adapters.MethodCallToBooleanFunctionAdapter;
import de.uni_koblenz.jgralab.algolib.functions.adapters.MethodCallToDoubleFunctionAdapter;
import de.uni_koblenz.jgralab.impl.ConsoleProgressFunction;
import de.uni_koblenz.jgstreetmap.osmschema.OsmGraph;
import de.uni_koblenz.jgstreetmap.osmschema.OsmSchema;
import de.uni_koblenz.jgstreetmap.osmschema.map.Node;
import de.uni_koblenz.jgstreetmap.osmschema.routing.Segment;
import de.uni_koblenz.jgstreetmap.osmschema.routing.SegmentType;
import de.uni_koblenz.jgstreetmap.routing.DijkstraRouteCalculator;
import de.uni_koblenz.jgstreetmap.routing.RoutingResult;
import de.uni_koblenz.jgstreetmap.routing.RouteCalculator.EdgeRating;

public class TryDijkstra {
	public static void main(String[] args) throws GraphIOException {
		OsmGraph graph = OsmSchema.instance().loadOsmGraphWithSavememSupport(
				"OsmGraph.tg.gz", new ConsoleProgressFunction());

		Node start = (Node) graph.getVertex(24175);
		Node target = (Node) graph.getVertex(46927);
		System.out.println("Start node set to " + start.get_osmId());

		System.out.println("Using internal Dijkstra implementation...");
		int runs = 10;
		for (int i = 0; i < runs; i++) {
			runOnInternalImplementation(graph, start, target);
		}
		System.out.println("Done.");

		System.out.println("Using algolib implementation...");
		for (int i = 0; i < runs; i++) {
			runOnAlgolibImplementation(graph, start, target);
		}

		System.out.println("Fini.");
	}

	private static void runOnInternalImplementation(OsmGraph graph, Node start,
			Node target) {
		DijkstraRouteCalculator routing1 = new DijkstraRouteCalculator(graph);

		routing1.setStart(start);
		long time = System.currentTimeMillis();
		RoutingResult result = routing1.getRoute(target, EdgeRating.LENGTH);
		long duration = System.currentTimeMillis() - time;
		List<Segment> route = result.getRoute();
		double length = 0.0;
		for (Segment current : route) {
			length += current.get_length();
			// System.out.println(current.getId());
		}
		System.out.println("Length: " + length + " Duration: " + duration
				+ " msecs.");
	}

	private static BooleanFunction<GraphElement> subgraph = new MethodCallToBooleanFunctionAdapter<GraphElement>() {

		@Override
		public boolean get(GraphElement parameter) {
			return parameter.getM1Class() == Segment.class
					|| parameter.getM1Class() == Node.class;
		}

		@Override
		public boolean isDefined(GraphElement parameter) {
			return true;
		}
	};

	private static BooleanFunction<Edge> navigable = new MethodCallToBooleanFunctionAdapter<Edge>() {

		private Set<SegmentType> relevantTypes;
		{
			relevantTypes = new HashSet<SegmentType>();
			relevantTypes.add(SegmentType.MOTORWAY);
			relevantTypes.add(SegmentType.PRIMARY);
			relevantTypes.add(SegmentType.SECONDARY);
			relevantTypes.add(SegmentType.TERTIARY);
			relevantTypes.add(SegmentType.RESIDENTIAL);
			relevantTypes.add(SegmentType.WORMHOLE);
			relevantTypes.add(SegmentType.SERVICE);
			relevantTypes.add(SegmentType.UNSURFACED);
		}

		@Override
		public boolean get(Edge parameter) {
			Segment currentSegment = (Segment) parameter;
			return relevantTypes.contains(currentSegment.get_wayType())
					&& (currentSegment.isNormal() || !currentSegment
							.is_oneway());
		}

		@Override
		public boolean isDefined(Edge parameter) {
			return true;
		}
	};

	private static DoubleFunction<Edge> edgeWeight = new MethodCallToDoubleFunctionAdapter<Edge>() {

		@Override
		public double get(Edge parameter) {
			return ((Segment) parameter).get_length();
		}

		@Override
		public boolean isDefined(Edge parameter) {
			return true;
		}

	};

	private static void runOnAlgolibImplementation(OsmGraph graph, Node start,
			Node target) {

		DijkstraAlgorithm dijkstra = new DijkstraAlgorithm(graph, subgraph,
				navigable, edgeWeight).undirected();
		long time = System.currentTimeMillis();
		try {
			dijkstra.execute(start);
		} catch (AlgorithmTerminatedException e) {
			e.printStackTrace();
		}
		long duration = System.currentTimeMillis() - time;
//		Stack<Integer> ids = new Stack<Integer>();
//		Function<Vertex, Edge> parent = dijkstra.getParent();
//		Vertex current = target;
//		
//		while(parent.get(current) != null){
//			Edge currentParent = parent.get(current);
//			assert current == currentParent.getThat();
//			ids.push(currentParent.getId());
//			current = currentParent.getThis();
//		}
//		while(!ids.isEmpty()){
//			System.out.println(ids.pop());
//		}
		System.out.println("Length: "
				+ dijkstra.getWeightedDistance().get(target) + " Duration: "
				+ duration + " msecs.");

	}
}
