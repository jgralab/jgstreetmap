package de.uni_koblenz.jgstreetmap.model.kdtree;

import java.util.LinkedList;

import de.uni_koblenz.jgstreetmap.model.AnnotatedOsmGraph;
import de.uni_koblenz.jgstreetmap.osmschema.Node;
import de.uni_koblenz.jgstreetmap.osmschema.kdtree.Key;
import de.uni_koblenz.jgstreetmap.osmschema.kdtree.NodeSet;
import de.uni_koblenz.jgstreetmap.osmschema.kdtree.XKey;
import de.uni_koblenz.jgstreetmap.osmschema.kdtree.YKey;

public class KDTreeQueries {

	public static LinkedList<Node> rangeQuery(AnnotatedOsmGraph g,
			double topLeftLong, double topLeftLat, double bottomRightLong,
			double bottomRightLat, Key key) {
		if (key.getClass() == XKey.class) {
			return rangeQuery(g, topLeftLong, topLeftLat, bottomRightLong,
					bottomRightLat, (XKey) key);
		} else if (key.getClass() == YKey.class) {
			return rangeQuery(g, topLeftLong, topLeftLat, bottomRightLong,
					bottomRightLat, (YKey) key);
		}
		return null;
	}

	public static LinkedList<Node> rangeQuery(AnnotatedOsmGraph g,
			double topLeftLong, double topLeftLat, double bottomRightLong,
			double bottomRightLat, YKey key) {
		LinkedList<Node> includedNodes = new LinkedList<Node>();
		double keyVal = key.getKeyValue();
		if (key.getChildList().size() == 0) { // is true whenever key has no
			// XKey children,
			// therefore the only child of key is a NodeSet
			KDTreeQueries.rangeQuery(g, topLeftLong, topLeftLat,
					bottomRightLong, bottomRightLat, key.getSetList().get(0));
		} else {
			if (keyVal < bottomRightLat) {
				// only the right subtree has to be examined further
				includedNodes.addAll(KDTreeQueries.rangeQuery(g, topLeftLong,
						topLeftLat, bottomRightLong, bottomRightLat, key
								.getChildList().get(1)));
			}
			if (keyVal >= topLeftLat) {
				// only the left subtree has to be examined further
				includedNodes.addAll(KDTreeQueries.rangeQuery(g, topLeftLong,
						topLeftLat, bottomRightLong, bottomRightLat, key
								.getChildList().get(0)));
			}
			if ((bottomRightLat < keyVal) && (keyVal < topLeftLat)) {
				// the right and the left subtree have to be examined
				includedNodes.addAll(KDTreeQueries.rangeQuery(g, topLeftLong,
						topLeftLat, bottomRightLong, bottomRightLat, key
								.getChildList().get(0)));
				includedNodes.addAll(KDTreeQueries.rangeQuery(g, topLeftLong,
						topLeftLat, bottomRightLong, bottomRightLat, key
								.getChildList().get(1)));
			}
		}

		return includedNodes;
	}

	public static LinkedList<Node> rangeQuery(AnnotatedOsmGraph g,
			double topLeftLong, double topLeftLat, double bottomRightLong,
			double bottomRightLat, XKey key) {
		LinkedList<Node> includedNodes = new LinkedList<Node>();
		double keyVal = key.getKeyValue();

		if (key.getChildList().size() == 0) { // is true, when key has a
			// NodeSet as a child
			KDTreeQueries.rangeQuery(g, topLeftLong, topLeftLat,
					bottomRightLong, bottomRightLat, key.getSetList().get(0));
		} else {
			if (keyVal < topLeftLong) {
				// examine only right subtree
				includedNodes.addAll(rangeQuery(g, topLeftLong, topLeftLat,
						bottomRightLong, bottomRightLat, key.getChildList()
								.get(1)));
			}
			if (keyVal >= bottomRightLong) {
				// examine only left subtree
				includedNodes.addAll(rangeQuery(g, topLeftLong, topLeftLat,
						bottomRightLong, bottomRightLat, key.getChildList()
								.get(0)));
			}
			if ((topLeftLong < keyVal) && (keyVal < bottomRightLong)) {
				// examine oth subtrees
				includedNodes.addAll(rangeQuery(g, topLeftLong, topLeftLat,
						bottomRightLong, bottomRightLat, key.getChildList()
								.get(0)));
				includedNodes.addAll(rangeQuery(g, topLeftLong, topLeftLat,
						bottomRightLong, bottomRightLat, key.getChildList()
								.get(1)));
			}
		}

		return includedNodes;
	}

	/**
	 * examines for every Node of the given NodeSet set if the Node is contained
	 * by the rectangle defined by its vertex at the upper left corner and its
	 * vertex at the lower right corner
	 * 
	 * @param g
	 * @param topLeftLong
	 *            longitude of the vertex at the upper left corner of the
	 *            rectangle
	 * @param topLeftLat
	 *            latitude of the vertex at the upper left corner of the
	 *            rectangle
	 * @param bottomRightLong
	 *            longitude of the vertex at the lower right corner of the
	 *            rectangle
	 * @param bottomRightLat
	 *            latitude of the vertex at the lower right corner of the
	 *            rectangle
	 * @param set
	 * @return a List of all Nodes which are included in the rectangle
	 */
	public static LinkedList<Node> rangeQuery(AnnotatedOsmGraph g,
			double topLeftLong, double topLeftLat, double bottomRightLong,
			double bottomRightLat, NodeSet set) {
		LinkedList<Node> nodes = new LinkedList<Node>();
		nodes.addAll(set.getElementList());
		LinkedList<Node> includedNodes = new LinkedList<Node>();

		for (int i = 0; i < nodes.size(); i++) {
			if ((nodes.get(i).getLatitude() <= topLeftLat)
					&& (nodes.get(i).getLatitude() >= bottomRightLat))
				if ((nodes.get(i).getLongitude() <= bottomRightLong)
						&& (nodes.get(i).getLongitude() >= topLeftLong))
					includedNodes.add(nodes.get(i));
		}

		return includedNodes;
	}

}
