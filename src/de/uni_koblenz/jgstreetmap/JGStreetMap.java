package de.uni_koblenz.jgstreetmap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import de.uni_koblenz.ist.utilities.option_handler.OptionHandler;
import de.uni_koblenz.jgralab.GraphIOException;
import de.uni_koblenz.jgralab.impl.SwingProgressFunction;
import de.uni_koblenz.jgstreetmap.gui.MapFrame;
import de.uni_koblenz.jgstreetmap.model.AnnotatedOsmGraph;
import de.uni_koblenz.jgstreetmap.osmschema.OsmGraph;
import de.uni_koblenz.jgstreetmap.osmschema.OsmGraphFactory;
import de.uni_koblenz.jgstreetmap.osmschema.OsmSchema;
import de.uni_koblenz.jgstreetmap.osmschema.impl.std.OsmGraphFactoryImpl;

public class JGStreetMap {
	private static CommandLine processCommandLineOptions(String[] args) {
		String toolString = "java " + JGStreetMap.class.getName();
		String versionString = "1.1";
		OptionHandler oh = new OptionHandler(toolString, versionString);

		Option input = new Option("i", "input", true,
				"(required): input OSM graph file");
		input.setRequired(true);
		input.setArgName("file");
		oh.addOption(input);

		Option enableAntiAliasing = new Option("a", "antialiasing", false,
				"(optional): if set, anti aliasing is enabled when rendering the map.");
		enableAntiAliasing.setRequired(false);
		oh.addOption(enableAntiAliasing);
		return oh.parse(args);
	}

	public static void main(String[] args) {
		try {
			CommandLine cl = processCommandLineOptions(args);
			String graphFile = cl.getOptionValue("i");
			boolean withAntiAliazing = cl.hasOption("a");

			// try {
			// for (LookAndFeelInfo info : UIManager
			// .getInstalledLookAndFeels()) {
			// if ("Nimbus".equals(info.getName())) {
			// UIManager.setLookAndFeel(info.getClassName());
			// break;
			// }
			// }
			// } catch (Exception e) {
			// // If Nimbus is not available, you can set the GUI to another
			// // look
			// // and feel.
			// }

			OsmGraphFactory f = new OsmGraphFactoryImpl();
			f.setGraphImplementationClass(OsmGraph.ATTRIBUTED_ELEMENT_CLASS,
					AnnotatedOsmGraph.class);
			OsmGraph graph = OsmSchema.instance().loadOsmGraph(graphFile, f,
					new SwingProgressFunction("jgStreetMap", "Loading Map..."));
			new MapFrame((AnnotatedOsmGraph) graph, withAntiAliazing);
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
