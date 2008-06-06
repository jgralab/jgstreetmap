package de.uni_koblenz.jgstreetmap.model;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;

import de.uni_koblenz.jgralab.GraphMarker;
import de.uni_koblenz.jgralab.Vertex;
import de.uni_koblenz.jgstreetmap.model.kdtree.KDTreeBuilder;
import de.uni_koblenz.jgstreetmap.osmschema.OsmPrimitive;
import de.uni_koblenz.jgstreetmap.osmschema.Tag;
import de.uni_koblenz.jgstreetmap.osmschema.Way;
import de.uni_koblenz.jgstreetmap.osmschema.impl.OsmGraphImpl;
import de.uni_koblenz.jgstreetmap.osmschema.kdtree.KDTree;
import de.uni_koblenz.jgstreetmap.osmschema.kdtree.Key;
import de.uni_koblenz.jgstreetmap.osmschema.kdtree.NodeSet;
import de.uni_koblenz.jgstreetmap.osmschema.kdtree.XKey;
import de.uni_koblenz.jgstreetmap.osmschema.kdtree.YKey;

public class AnnotatedOsmGraph extends OsmGraphImpl {
	private Map<Long, OsmPrimitive> osmIdMap;
	private GraphMarker<LayoutInfo> layoutInfo;
	private SortedMap<Integer, List<Way>> orderedWays;

	public AnnotatedOsmGraph(String id, int vmax, int emax) {
		super(id, vmax, emax);
		osmIdMap = new HashMap<Long, OsmPrimitive>();
		layoutInfo = new GraphMarker<LayoutInfo>(this);
		orderedWays = new TreeMap<Integer, List<Way>>();
	}

	@Override
	public void loadingCompleted() {
		for (OsmPrimitive o : getOsmPrimitiveVertices()) {
			osmIdMap.put(o.getOsmId(), o);
		}
		for (Way way : getWayVertices()) {
			LayoutInfo l = LayoutInfo.computeInfo(way);
			layoutInfo.mark(way, l);
			List<Way> lst = orderedWays.get(l.zOrder);
			if (lst == null) {
				lst = new LinkedList<Way>();
				orderedWays.put(l.zOrder, lst);
			}
			lst.add(way);
		}
		
		KDTreeBuilder.buildTree(this, 10);
		
		// Mengengerüst berechnen
		Map<String, Integer> m = new HashMap<String, Integer>();
		for (Vertex v = getFirstVertex(); v != null; v = v.getNextVertex()) {
			String n = "V " + v.getM1Class().getSimpleName();
			if (m.containsKey(n)) {
				m.put(n, m.get(n) + 1);
			} else {
				m.put(n, 1);
			}
		}
		for (de.uni_koblenz.jgralab.Edge e = getFirstEdgeInGraph(); e != null; e = e
				.getNextEdgeInGraph()) {
			String n = "E " + e.getM1Class().getSimpleName();
			if (m.containsKey(n)) {
				m.put(n, m.get(n) + 1);
			} else {
				m.put(n, 1);
			}
		}
		for (String n : new TreeSet<String>(m.keySet())) {
			System.err.println(m.get(n) + "\t" + n);
		}
	}

	public SortedMap<Integer, List<Way>> getOrderedWayVertices() {
		return orderedWays;
	}

	public LayoutInfo getLayoutInfo(OsmPrimitive o) {
		return layoutInfo.getMark(o);
	}

	public static String getTag(OsmPrimitive o, String key) {
		List<Tag> tagList = o.getTags();
		if (tagList == null) {
			return null;
		}
		for (Tag t : tagList) {
			if (t.key.equals(key)) {
				return t.value;
			}
		}
		return null;
	}

	public boolean hasKDTree() {
		return getFirstKDTree() != null;
	}

	public void deleteKDTree() {
		if (!hasKDTree()) {
			return;
		}
		KDTree tree = getKDTree();
		Stack<Key> s = new Stack<Key>();
		s.push((Key) tree.getFirstHasRoot().getThat());
		while (!s.empty()) {
			Key current = s.pop();
			NodeSet ns = (NodeSet) current.getFirstHasSet().getThat();
			if (ns != null) {
				ns.delete();
			} else {
				if (current instanceof XKey) {
					for (Key k : ((XKey) current).getChildList()) {
						s.push(k);
					}
				} else {
					for (Key k : ((YKey) current).getChildList()) {
						s.push(k);
					}
				}
				current.delete();
			}
		}
	}

	public KDTree getKDTree() {
		KDTree tree = getFirstKDTree();
		if (tree == null) {
			tree = createKDTree();
		}
		return tree;
	}

}
