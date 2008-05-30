package de.uni_koblenz.jgstreetmap.model;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import de.uni_koblenz.jgralab.GraphMarker;
import de.uni_koblenz.jgstreetmap.osmschema.OsmPrimitive;
import de.uni_koblenz.jgstreetmap.osmschema.Way;
import de.uni_koblenz.jgstreetmap.osmschema.impl.OsmGraphImpl;

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
		System.out.println("AnnotatedOsmGraph.loadingComplete()");
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
	}

	public SortedMap<Integer, List<Way>> getOrderedWayVertices() {
		return orderedWays;
	}

	public LayoutInfo getLayoutInfo(OsmPrimitive o) {
		return layoutInfo.getMark(o);
	}

}
