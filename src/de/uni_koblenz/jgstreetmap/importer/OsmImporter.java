package de.uni_koblenz.jgstreetmap.importer;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

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
		OsmSchema.instance().getGraphFactory()
				.setGraphSavememImplementationClass(OsmGraph.class,
						AnnotatedOsmGraph.class);
	}

	private State state;
	private long startTime;
	// private Map<Long, OsmPrimitive> osmIdMap;
	private Map<Long, Node> nodeMap;
	private Map<Long, Way> wayMap;
	private Map<Long, Relation> relationMap;
	private AnnotatedOsmGraph graph;
	private SimpleDateFormat dateFormat;
	private OsmPrimitive currentPrimitive;
	private Map<String, String> currentTagMap;
	private int nodeCount;
	private static final int MAX_SET_MEMBERS = 512;
	private MinimalSAXParser parser;
	private HashSet<String> usedTags;
	private String outFile;

	public OsmImporter(String outTgFile) {
		outFile = outTgFile;
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

	public static void main(String[] args) {
		if (args.length != 2) {
			System.err.println("Usage: OsmImporter <map.osm.xml> <out.tg>");
			System.exit(1);
		}

		String filename = args[0];
		String out = args[1];
		System.out.println("OSM to TGraph");
		System.out.println("=============");
		OsmSchema.instance().getGraphFactory().setGraphImplementationClass(
				OsmGraph.class, AnnotatedOsmGraph.class);
		new OsmImporter(out).importOsm(filename);
	}

	public void importOsm(String fileName) {
		parser = new MinimalSAXParser();
		try {
			state = State.INIT;
			currentPrimitive = null;
			currentTagMap = null;
			parser.parse(fileName, this);
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void startDocument() throws SAXException {
		System.out.println("Converting...");
		startTime = System.currentTimeMillis();
		graph = (AnnotatedOsmGraph) OsmSchema.instance()
				.createOsmGraphWithSavememSupport();
		// osmIdMap = new HashMap<Long, OsmPrimitive>();
		nodeMap = new HashMap<Long, Node>();
		wayMap = new HashMap<Long, Way>();
		relationMap = new HashMap<Long, Relation>();
		graph.createKDTree();
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
		Segmentator.segmentateGraph(graph);
		int n = nodeCount;
		int levels = 1;
		while (n > MAX_SET_MEMBERS) {
			n /= 2;
			++levels;
		}
		System.out.println("Building KDTree with " + levels + " levels...");
		KDTreeBuilder.buildTree(graph, levels);
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
				currentTagMap = new HashMap<String, String>();
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
			currentTagMap.put(key, v);

		} else if ((state == State.RELATION) && name.equals("member")) {
			String type = atts.getValue("type");
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
}
