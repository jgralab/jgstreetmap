package de.uni_koblenz.jgstreetmap;

import de.uni_koblenz.jgralab.GraphIOException;
import de.uni_koblenz.jgralab.impl.ProgressFunctionImpl;
import de.uni_koblenz.jgstreetmap.model.AnnotatedOsmGraph;
import de.uni_koblenz.jgstreetmap.osmschema.OsmGraph;
import de.uni_koblenz.jgstreetmap.osmschema.OsmSchema;

public class MemoryTest {

	public static void main(String[] args) {
		try {
			String graphFile = (args.length > 0) ? args[0] : "OsmGraph.tg";
			printMemory("After start");
			OsmSchema.instance().getGraphFactory().setGraphImplementationClass(
					OsmGraph.class, AnnotatedOsmGraph.class);
			printMemory("After setting GraphFactory");
			OsmGraph graph = OsmSchema.instance().loadOsmGraph(graphFile,
					new ProgressFunctionImpl());
			printMemory("After loading Graph");
			System.err.println("V: " + graph.getVCount());
			System.err.println("E: " + graph.getECount());
			System.err.println((double) used
					/ (graph.getVCount() + graph.getECount()));
		} catch (GraphIOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private static long max;
	private static long free;
	private static long total;
	private static long used;

	private static void printMemory(String headline) {
		System.gc();
		long newMax = Runtime.getRuntime().maxMemory();
		long newFree = Runtime.getRuntime().freeMemory();
		long newTotal = Runtime.getRuntime().totalMemory();
		long newUsed = newTotal - newFree;

		System.err.println("===== " + headline + " =====");
		System.err.println("Max.  Memory: " + newMax + " (" + (newMax - max)
				+ ")");
		System.err.println("Free  Memory: " + newFree + " (" + (newFree - free)
				+ ")");
		System.err.println("Total Memory: " + newTotal + " ("
				+ (newTotal - total) + ")");
		System.err.println("Used  Memory: " + newUsed + " (" + (newUsed - used)
				+ ")");
		max = newMax;
		free = newFree;
		total = newTotal;
		used = newUsed;
	}
}