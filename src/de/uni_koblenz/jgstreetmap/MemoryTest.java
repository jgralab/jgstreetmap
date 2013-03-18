package de.uni_koblenz.jgstreetmap;

import de.uni_koblenz.jgralab.exception.GraphIOException;
import de.uni_koblenz.jgralab.impl.ConsoleProgressFunction;
import de.uni_koblenz.jgstreetmap.osmschema.OsmGraph;
import de.uni_koblenz.jgstreetmap.osmschema.OsmSchema;

public class MemoryTest {

	public static void main(String[] args) {
		try {
			String graphFile = (args.length > 0) ? args[0] : "OsmGraph.tg";
			printMemory("After start");

			// OsmGraphFactory f = new OsmGraphFactoryImpl();
			// f.setGraphImplementationClass(OsmGraph.GC,
			// AnnotatedOsmGraph.class);
			// printMemory("After setting GraphFactory");

			long start = System.currentTimeMillis();
			// OsmGraph graph = OsmSchema.instance().loadOsmGraph(graphFile, f,
			// new ConsoleProgressFunction());
			OsmGraph graph = OsmSchema.instance().loadOsmGraph(graphFile,
					new ConsoleProgressFunction());
			long stop = System.currentTimeMillis();
			System.out.println("Time to load graph: " + (stop - start) / 1000.0
					+ "s");
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
