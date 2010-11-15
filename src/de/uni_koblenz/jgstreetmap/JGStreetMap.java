package de.uni_koblenz.jgstreetmap;

import de.uni_koblenz.jgralab.GraphIOException;
import de.uni_koblenz.jgralab.impl.SwingProgressFunction;
import de.uni_koblenz.jgstreetmap.gui.MapFrame;
import de.uni_koblenz.jgstreetmap.model.AnnotatedOsmGraph;
import de.uni_koblenz.jgstreetmap.osmschema.OsmGraph;
import de.uni_koblenz.jgstreetmap.osmschema.OsmSchema;

public class JGStreetMap {
	static {
		OsmSchema
				.instance()
				.getGraphFactory()
				.setGraphSavememImplementationClass(OsmGraph.class,
						AnnotatedOsmGraph.class);
	}

	public static void main(String[] args) {
		try {
			String graphFile = (args.length > 0) ? args[0] : "OsmGraph.tg.gz";

			OsmGraph graph = OsmSchema.instance()
					.loadOsmGraphWithSavememSupport(
							graphFile,
							new SwingProgressFunction("jgStreetMap",
									"Loading Map..."));
			new MapFrame((AnnotatedOsmGraph) graph);
		} catch (GraphIOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private static long max;
	private static long free;
	private static long total;
	private static long used;

	@SuppressWarnings("unused")
	private static void printMemory(String headline) {
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
