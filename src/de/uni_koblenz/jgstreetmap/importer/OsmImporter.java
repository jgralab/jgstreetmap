package de.uni_koblenz.jgstreetmap.importer;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import de.uni_koblenz.jgralab.GraphIO;
import de.uni_koblenz.jgralab.GraphIOException;
import de.uni_koblenz.jgralab.impl.ProgressFunctionImpl;
import de.uni_koblenz.jgstreetmap.model.AnnotatedOsmGraph;
import de.uni_koblenz.jgstreetmap.model.kdtree.KDTreeBuilder;
import de.uni_koblenz.jgstreetmap.osmschema.HasMember;
import de.uni_koblenz.jgstreetmap.osmschema.Node;
import de.uni_koblenz.jgstreetmap.osmschema.OsmGraph;
import de.uni_koblenz.jgstreetmap.osmschema.OsmPrimitive;
import de.uni_koblenz.jgstreetmap.osmschema.OsmSchema;
import de.uni_koblenz.jgstreetmap.osmschema.Relation;
import de.uni_koblenz.jgstreetmap.osmschema.Tag;
import de.uni_koblenz.jgstreetmap.osmschema.Way;
import de.uni_koblenz.jgstreetmap.routing.Segmentator;

public class OsmImporter extends DefaultHandler {
	private enum State {
		INIT, OSM, NODE, WAY, RELATION
	};

	private State state;
	private long startTime;
	private Map<Long, OsmPrimitive> osmIdMap;
	private AnnotatedOsmGraph graph;
	private SimpleDateFormat dateFormat;
	private OsmPrimitive currentPrimitive;
	private List<Tag> currentTagList;
	private int nodeCount;
	private static final int MAX_SET_MEMBERS = 512;

	public OsmImporter() {
		dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
	}

	public static void main(String[] args) {
		String filename = (args.length > 0) ? args[0] : "map.osm.xml";
		System.out.println("OSM to TGraph");
		System.out.println("=============");
		// new OsmImporter().importOsm("rheinland-pfalz.osm.xml");
		OsmSchema.instance().getGraphFactory().setGraphImplementationClass(
				OsmGraph.class, AnnotatedOsmGraph.class);
		new OsmImporter().importOsm(filename);
	}

	public void importOsm(String fileName) {
		MinimalSAXParser parser = new MinimalSAXParser();
		try {
			state = State.INIT;
			currentPrimitive = null;
			currentTagList = null;
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
		graph = (AnnotatedOsmGraph) OsmSchema.instance().createOsmGraph(16000,
				16000);
		osmIdMap = new HashMap<Long, OsmPrimitive>();
		graph.createKDTree();
		nodeCount = 0;
	}

	@Override
	public void endDocument() throws SAXException {
		Segmentator.segmentateGraph(graph);
		int n = nodeCount;
		int levels = 1;
		while (n > MAX_SET_MEMBERS) {
			n /= 2;
			++levels;
		}
		System.out.println("Building KDTree with " + levels + " levels...");
		KDTreeBuilder.buildTree(graph, levels);
		try {
			GraphIO.saveGraphToFile("OsmGraph.tg", graph,
					new ProgressFunctionImpl());
		} catch (GraphIOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		long stopTime = System.currentTimeMillis();
		System.out.println("Total conversion time: " + (stopTime - startTime)
				/ 1000.0 + "s");
		System.out.println("Fini.");
	}

	@Override
	public void startElement(String uri, String localName, String name,
			Attributes atts) throws SAXException {
		// System.out.println(state + ": " + name);
		// for (int i = 0; i < atts.getLength(); ++i) {
		// System.out.println("\t" + atts.getQName(i) + "='"
		// + atts.getValue(i) + "'");
		// }
		if (state == State.OSM && name.equals("node")) {
			state = State.NODE;
			++nodeCount;
			Node n = graph.createNode();
			long id = Long.parseLong(atts.getValue("id"));
			n.setOsmId(id);
			n.setLatitude(Double.parseDouble(atts.getValue("lat")));
			n.setLongitude(Double.parseDouble(atts.getValue("lon")));
			n.setTimestamp(parseTimestamp(atts.getValue("timestamp")));
			n.setUser(atts.getValue("user"));
			currentPrimitive = n;
			osmIdMap.put(id, n);

		} else if (state == State.OSM && name.equals("way")) {
			state = State.WAY;
			Way w = graph.createWay();
			long id = Long.parseLong(atts.getValue("id"));
			w.setOsmId(id);
			w.setTimestamp(parseTimestamp(atts.getValue("timestamp")));
			w.setUser(atts.getValue("user"));
			currentPrimitive = w;
			osmIdMap.put(id, w);

		} else if (state == State.OSM && name.equals("relation")) {
			state = State.RELATION;
			Relation r = graph.createRelation();
			long id = Long.parseLong(atts.getValue("id"));
			r.setOsmId(id);
			r.setTimestamp(parseTimestamp(atts.getValue("timestamp")));
			currentPrimitive = r;
			osmIdMap.put(id, r);

		} else if (state == State.WAY && name.equals("nd")) {
			long ref = Long.parseLong(atts.getValue("ref"));
			try {
				Node n = (Node) osmIdMap.get(ref);
				// if (!osmIdMap.containsKey(ref)) {
				// System.err.println("node " + ref + " not found!");
				// }
				if (n != null) {
					graph.createHasNode((Way) currentPrimitive, n);
				}
			} catch (ClassCastException e) {
				e.printStackTrace();
			}

		} else if (state != State.INIT && state != State.OSM
				&& name.equals("tag")) {
			if (currentTagList == null) {
				currentTagList = new ArrayList<Tag>();
			}
			String v = atts.getValue("v");
			v = v.replaceAll("&apos;", "'");
			v = v.replaceAll("&gt;", ">");
			v = v.replaceAll("&lt;", "<");
			v = v.replaceAll("&quot;", "\"");
			v = v.replaceAll("&amp;", "&");
			currentTagList.add(new Tag(atts.getValue("k"), v));

		} else if (state == State.RELATION && name.equals("member")) {
			String type = atts.getValue("type");
			long ref = Long.parseLong(atts.getValue("ref"));
			OsmPrimitive p = osmIdMap.get(ref);
			// if (!osmIdMap.containsKey(ref)) {
			// System.err.println(type + " " + ref + " not found!");
			// }
			if (p != null) {
				HasMember m = graph.createHasMember(
						(Relation) currentPrimitive, p);
				m.setMemberType(type);
				m.setMemberRole(atts.getValue("role"));
			}

		} else if (state == State.INIT && name.equals("osm")) {
			state = State.OSM;
			currentPrimitive = null;
			currentTagList = null;

		} else {
			throw new RuntimeException("Yee! Found element '" + name
					+ "' in state " + state + " which I can't handle :-(");
		}
	}

	private long parseTimestamp(String timestamp) {
		if (timestamp == null || timestamp.length() != 25) {
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
			currentPrimitive.setTags(currentTagList);
			if (state == State.WAY) {
				Way w = (Way) currentPrimitive;
				List<? extends Node> nl = w.getNodeList();
				w.setClosed(nl.size() >= 2
						&& nl.get(0) == nl.get(nl.size() - 1));
			}
			currentPrimitive = null;
			currentTagList = null;
			state = State.OSM;
		}
	}
}
