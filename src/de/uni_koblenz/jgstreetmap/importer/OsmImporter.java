package de.uni_koblenz.jgstreetmap.importer;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.pcollections.ArrayPMap;
import org.pcollections.PMap;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import de.uni_koblenz.ist.utilities.option_handler.OptionHandler;
import de.uni_koblenz.jgralab.GraphIO;
import de.uni_koblenz.jgralab.GraphIOException;
import de.uni_koblenz.jgralab.impl.ConsoleProgressFunction;
import de.uni_koblenz.jgstreetmap.model.AnnotatedOsmGraph;
import de.uni_koblenz.jgstreetmap.model.kdtree.KDTreeBuilder;
import de.uni_koblenz.jgstreetmap.osmschema.OsmGraph;
import de.uni_koblenz.jgstreetmap.osmschema.OsmSchema;
import de.uni_koblenz.jgstreetmap.osmschema.map.HasMember;
import de.uni_koblenz.jgstreetmap.osmschema.map.Node;
import de.uni_koblenz.jgstreetmap.osmschema.map.OsmPrimitive;
import de.uni_koblenz.jgstreetmap.osmschema.map.Relation;
import de.uni_koblenz.jgstreetmap.osmschema.map.Way;
import de.uni_koblenz.jgstreetmap.routing.Segmentator;

public class OsmImporter extends DefaultHandler {
	private enum State {
		INIT, OSM, NODE, WAY, RELATION
	};

	static {
		OsmSchema
				.instance()
				.getGraphFactory()
				.setGraphImplementationClass(OsmGraph.class,
						AnnotatedOsmGraph.class);
	}

	private static final int DEFAULT_SET_MEMBERS = 512;

	private State state;
	private long startTime;
	// private Map<Long, OsmPrimitive> osmIdMap;
	private Map<Long, Node> nodeMap;
	private Map<Long, Way> wayMap;
	private Map<Long, Relation> relationMap;
	private AnnotatedOsmGraph graph;
	private SimpleDateFormat dateFormat;
	private OsmPrimitive currentPrimitive;
	private PMap<String, String> currentTagMap;
	private int nodeCount;
	private MinimalSAXParser parser;
	private HashSet<String> usedTags;

	private String inFile;
	private String outFile;
	private int levels, members;
	private boolean buildKD;
	private boolean createSegments;

	public OsmImporter(String inFile, String outFile) {
		this.inFile = inFile;
		this.outFile = outFile;
		levels = -1;
		members = DEFAULT_SET_MEMBERS;
		buildKD = true;
		createSegments = true;

		dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		usedTags = new HashSet<String>(10);
		// Only these tags which are needed for visualizing the map and routing
		// are saved in the graph.
		usedTags.add("highway");
		usedTags.add("waterway");
		usedTags.add("cycleway");
		usedTags.add("railway");
		usedTags.add("landuse");
		usedTags.add("amenity");
		usedTags.add("place");
		usedTags.add("natural");
		usedTags.add("oneway");
		usedTags.add("name");
	}

	private static CommandLine processCommandLineOptions(String[] args) {
		String toolString = "java " + OsmImporter.class.getName();
		String versionString = "1.1";
		OptionHandler oh = new OptionHandler(toolString, versionString);

		Option output = new Option("o", "output", true,
				"(required): output TG file");
		output.setRequired(true);
		output.setArgName("file");
		oh.addOption(output);

		Option input = new Option("i", "input", true,
				"(required): input OSM xml file");
		input.setRequired(true);
		input.setArgName("file");
		oh.addOption(input);

		Option levels = new Option("l", "levels", true,
				"(optional): number of levels in the KD tree (cannot be set with option m)");
		levels.setRequired(false);
		levels.setArgName("number");
		oh.addOption(levels);

		Option members = new Option(
				"m",
				"members",
				true,
				"(optional): maximum number of members in KD tree leafs (cannot be set with option l)");
		members.setRequired(false);
		members.setArgName("number");
		oh.addOption(members);

		Option disableKD = new Option(
				"N",
				"no-kd",
				false,
				"(optional): disables the creation of a KD-Tree entirely (cannot be set with options l or m)");
		disableKD.setRequired(false);
		oh.addOption(disableKD);

		OptionGroup kdLevel = new OptionGroup();
		kdLevel.addOption(members);
		kdLevel.addOption(levels);
		kdLevel.addOption(disableKD);
		oh.addOptionGroup(kdLevel);

		Option disableSegments = new Option("S", "no-segments", false,
				"(optional): disables the creation of segments");
		disableSegments.setRequired(false);
		oh.addOption(disableSegments);
		return oh.parse(args);
	}

	public static void main(String[] args) {
		CommandLine cl = processCommandLineOptions(args);
		String inFile = cl.getOptionValue("i");
		String outFile = cl.getOptionValue("o");

		OsmImporter importer = new OsmImporter(inFile, outFile);

		if (cl.hasOption("l")) {
			int levels = Integer.parseInt(cl.getOptionValue("l"));
			importer.setLevels(levels);
		}
		if (cl.hasOption("m")) {
			int members = Integer.parseInt(cl.getOptionValue("m"));
			importer.setMembers(members);
		}
		if (cl.hasOption("N")) {
			importer.setBuildKD(false);
			System.out.println("Disabling KD tree creation...");
		}
		if (cl.hasOption("S")) {
			importer.setCreateSegments(false);
			System.out.println("Disabling the creation of segments...");
		}

		importer.importOsm();

		System.out.println("OSM to TGraph");
		System.out.println("=============");
		// OsmSchema.instance().getGraphFactory().setGraphImplementationClass(
		// OsmGraph.class, AnnotatedOsmGraph.class);
		// new OsmImporter(filename, out).importOsm();
	}

	public void importOsm() {
		parser = new MinimalSAXParser();
		try {
			state = State.INIT;
			currentPrimitive = null;
			currentTagMap = null;
			parser.parse(inFile, this);
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void setLevels(int levels) {
		if (levels > 0) {
			members = -1;
			this.levels = levels;
		} else {
			throw new RuntimeException("\"levels\" may not be negative.");
		}
	}

	public void setMembers(int members) {
		if (members > 0) {
			levels = -1;
			this.members = members;
		} else {
			throw new RuntimeException("\"members\" may not be negative.");
		}
	}

	@Override
	public void startDocument() throws SAXException {
		System.out.println("Converting...");
		startTime = System.currentTimeMillis();
		graph = (AnnotatedOsmGraph) OsmSchema.instance().createOsmGraph();
		nodeMap = new HashMap<Long, Node>();
		wayMap = new HashMap<Long, Way>();
		relationMap = new HashMap<Long, Relation>();
		// graph.createKDTree();
		nodeCount = 0;
	}

	@Override
	public void endDocument() throws SAXException {
		postProcess();
		graph.defragment();
		try {
			GraphIO.saveGraphToFile(outFile, graph,
					new ConsoleProgressFunction());
		} catch (GraphIOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		long stopTime = System.currentTimeMillis();
		System.out.println("Total conversion time: " + (stopTime - startTime)
				/ 1000.0 + "s");
		System.out.println("Fini.");
	}

	private void postProcess() {
		if (createSegments) {
			Segmentator.segmentateGraph(graph);
		}
		if (buildKD) {
			int n = nodeCount;
			if (levels < 0) {
				levels = 1;
				assert members > 0;
				while (n > members) {
					n /= 2;
					++levels;
				}
			}

			System.out.println("Building KDTree with " + levels + " levels...");
			KDTreeBuilder.buildTree(graph, levels);
		}
	}

	private void addToMap(long id, Node value) {
		nodeMap.put(id, value);
	}

	private void addToMap(long id, Way value) {
		wayMap.put(id, value);
	}

	private void addToMap(long id, Relation value) {
		relationMap.put(id, value);
	}

	private Node getNode(long id) {
		return nodeMap.get(id);
	}

	private Way getWay(long id) {
		return wayMap.get(id);
	}

	private Relation getRelation(long id) {
		return relationMap.get(id);
	}

	@Override
	public void startElement(String uri, String localName, String name,
			Attributes atts) throws SAXException {
		// System.out.println(state + ": " + name);
		// for (int i = 0; i < atts.getLength(); ++i) {
		// System.out.println("\t" + atts.getQName(i) + "='"
		// + atts.getValue(i) + "'");
		// }
		if ((state == State.OSM) && name.equals("node")) {
			state = State.NODE;
			++nodeCount;
			Node n = graph.createNode();
			long id = Long.parseLong(atts.getValue("id"));
			n.set_osmId(id);
			n.set_latitude(Double.parseDouble(atts.getValue("lat")));
			n.set_longitude(Double.parseDouble(atts.getValue("lon")));
			n.set_timestamp(parseTimestamp(atts.getValue("timestamp")));
			n.set_user(atts.getValue("user"));
			currentPrimitive = n;
			// osmIdMap.put(id, n);
			addToMap(id, n);

		} else if ((state == State.OSM) && name.equals("way")) {
			state = State.WAY;
			Way w = graph.createWay();
			long id = Long.parseLong(atts.getValue("id"));
			w.set_osmId(id);
			w.set_timestamp(parseTimestamp(atts.getValue("timestamp")));
			w.set_user(atts.getValue("user"));
			currentPrimitive = w;
			// osmIdMap.put(id, w);
			addToMap(id, w);

		} else if ((state == State.OSM) && name.equals("relation")) {
			state = State.RELATION;
			Relation r = graph.createRelation();
			long id = Long.parseLong(atts.getValue("id"));
			r.set_osmId(id);
			r.set_timestamp(parseTimestamp(atts.getValue("timestamp")));
			currentPrimitive = r;
			// osmIdMap.put(id, r);
			addToMap(id, r);

		} else if ((state == State.WAY) && name.equals("nd")) {
			long ref = Long.parseLong(atts.getValue("ref"));
			try {
				// Node n = (Node) osmIdMap.get(ref);
				Node n = getNode(ref);
				// if (!osmIdMap.containsKey(ref)) {
				// System.err.println("node " + ref + " not found!");
				// }
				if (n != null) {
					graph.createHasNode((Way) currentPrimitive, n);
				}
			} catch (ClassCastException e) {
				System.err.println("Line number: " + parser.getLine());
				e.printStackTrace();
			}

		} else if ((state != State.INIT) && (state != State.OSM)
				&& name.equals("tag")) {
			if (currentTagMap == null) {
				currentTagMap = ArrayPMap.empty();
			}
			String key = atts.getValue("k");
			if (!usedTags.contains(key)) {
				return;
			}
			String v = atts.getValue("v");
			v = v.replaceAll("&apos;", "'");
			v = v.replaceAll("&gt;", ">");
			v = v.replaceAll("&lt;", "<");
			v = v.replaceAll("&quot;", "\"");
			v = v.replaceAll("&amp;", "&");
			currentTagMap = currentTagMap.plus(key, v);

		} else if ((state == State.RELATION) && name.equals("member")) {
			String type = atts.getValue("type").trim();
			String role = atts.getValue("role");
			long ref = Long.parseLong(atts.getValue("ref"));
			OsmPrimitive p = type.equals("node") ? getNode(ref) : type
					.equals("way") ? getWay(ref)
					: type.equals("relation") ? getRelation(ref) : null;

			// if (!osmIdMap.containsKey(ref)) {
			// System.err.println(type + " " + ref + " not found!");
			// }
			if (p != null) {
				HasMember m = graph.createHasMember(
						(Relation) currentPrimitive, p);
				m.set_memberType(type);
				m.set_memberRole(role);
			} else {
				System.err.println("Warning: Could not create relation from "
						+ currentPrimitive + " to element of type " + type
						+ ". Either unknown type or unknown id (" + ref + ").");
			}

		} else if ((state == State.INIT) && name.equals("osm")) {
			state = State.OSM;
			currentPrimitive = null;
			currentTagMap = null;

		} else if (name.equals("bounds")) {
			System.err.println("Warning: ignoring tag \"bounds\"");
		} else {
			throw new RuntimeException("Yee! Found element '" + name
					+ "' in state " + state + " which I can't handle :-(");
		}
	}

	private long parseTimestamp(String timestamp) {
		if ((timestamp == null) || (timestamp.length() != 25)) {
			return -1;
		}
		ParsePosition p = new ParsePosition(0);
		timestamp = timestamp.substring(0, timestamp.length() - 3)
				+ timestamp.substring(timestamp.length() - 2);
		Date d = dateFormat.parse(timestamp, p);
		if (p.getErrorIndex() >= 0) {
			System.err.println("Error at position " + p.getErrorIndex()
					+ " in '" + timestamp + "'");
			return -1;
		}
		return d.getTime();
	}

	@Override
	public void endElement(String uri, String localName, String name)
			throws SAXException {
		if (name.equals("node") || name.equals("way")
				|| name.equals("relation")) {
			currentPrimitive.set_tags(currentTagMap);
			if (state == State.WAY) {
				Way w = (Way) currentPrimitive;
				Node first = null;
				Node last = null;
				int count = 0;
				for (Node n : w.get_nodes()) {
					++count;
					if (first == null) {
						first = n;
					}
					last = n;
				}
				w.set_closed((count >= 2) && (first == last));
			}
			currentPrimitive = null;
			currentTagMap = null;
			state = State.OSM;
		}
	}

	public void setBuildKD(boolean buildKD) {
		this.buildKD = buildKD;
	}

	public void setCreateSegments(boolean createSegments) {
		this.createSegments = createSegments;
	}
}
