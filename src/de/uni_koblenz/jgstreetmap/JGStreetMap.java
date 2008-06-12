package de.uni_koblenz.jgstreetmap;

import de.uni_koblenz.jgralab.GraphIOException;
import de.uni_koblenz.jgstreetmap.gui.MapFrame;
import de.uni_koblenz.jgstreetmap.gui.SwingProgressFunction;
import de.uni_koblenz.jgstreetmap.model.AnnotatedOsmGraph;
import de.uni_koblenz.jgstreetmap.osmschema.OsmGraph;
import de.uni_koblenz.jgstreetmap.osmschema.OsmSchema;

public class JGStreetMap {
	public static void main(String[] args) {
		try {
			String graphFile = (args.length > 0) ? args[0] : "OsmGraph.tg";
			OsmSchema.instance().getGraphFactory().setGraphImplementationClass(
					OsmGraph.class, AnnotatedOsmGraph.class);
			OsmGraph graph = OsmSchema.instance().loadOsmGraph(graphFile,
					new SwingProgressFunction("jgStreetMap", "Loading Map..."));
			new MapFrame((AnnotatedOsmGraph) graph);
		} catch (GraphIOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
	}
}
