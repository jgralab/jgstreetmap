package de.uni_koblenz.jgstreetmap.model.kdtree;

import java.util.Collections;
import java.util.LinkedList;

import de.uni_koblenz.jgstreetmap.model.AnnotatedOsmGraph;
import de.uni_koblenz.jgstreetmap.osmschema.Node;
import de.uni_koblenz.jgstreetmap.osmschema.kdtree.KDTree;
import de.uni_koblenz.jgstreetmap.osmschema.kdtree.NodeSet;
import de.uni_koblenz.jgstreetmap.osmschema.kdtree.XKey;
import de.uni_koblenz.jgstreetmap.osmschema.kdtree.YKey;

public class KDTreeBuilder {
	public static void buildTree(AnnotatedOsmGraph g, int maxLevels) {
		// construct kdtree out of given graph
		// get all Vertices of the Graph
		LinkedList<Node> nodeList = new LinkedList<Node>();
		for (Node n : g.getNodeVertices()) {
			nodeList.add(n);
		}

		if (g.hasKDTree()) {
			g.deleteKDTree();
		}
		KDTree kdTree = g.getKDTree();
		kdTree.setLevels(maxLevels);
		kdTree.addRoot(constructkdTreeY(g, nodeList, 1));
	}

	public static YKey constructkdTreeY(AnnotatedOsmGraph g,
			LinkedList<Node> nodeList, int depth) {
		Collections.sort(nodeList, new YComparator());
		YKey yk = g.createYKey();

		// if the depth of the tree is equal to the number of levels which are
		// allowed
		// exist or the list of nodes which would be splitted contains only one
		// node,
		// NodeSet is assigned to YKey as the child of YKey
		if ((depth == g.getKDTree().getLevels()) || (nodeList.size() <= 1)) {
			NodeSet set = g.createNodeSet();
			yk.addSet(set);
			for (Node n : nodeList) {
				set.addElement(n);
			}
		} else {
			yk.setKeyValue(nodeList.get(nodeList.size() / 2 - 1).getLatitude());
			// List of nodes is split into two lists, the nodes contained in
			// leftList
			// will be part of the left subtree, the nodes left in nodeList will
			// be
			// part of the right subtree
			LinkedList<Node> leftList = new LinkedList<Node>();
			for (int i = 0; i < nodeList.size() / 2; i++) {
				leftList.add(nodeList.poll());
			}

			// construct next level, which consists of XKeys
			yk.addChild(constructkdTreeX(g, leftList, depth + 1));
			yk.addChild(constructkdTreeX(g, nodeList, depth + 1));
		}
		return yk;
	}

	public static XKey constructkdTreeX(AnnotatedOsmGraph g,
			LinkedList<Node> nodeList, int depth) {
		Collections.sort(nodeList, new XComparator());
		XKey xk = g.createXKey();

		if ((depth == g.getKDTree().getLevels()) || (nodeList.size() <= 1)) {
			NodeSet set = g.createNodeSet();
			xk.addSet(set);
			for (Node n : nodeList) {
				set.addElement(n);
			}
		} else {
			xk
					.setKeyValue(nodeList.get(nodeList.size() / 2 - 1)
							.getLongitude());
			LinkedList<Node> leftList = new LinkedList<Node>();
			for (int i = 0; i < nodeList.size() / 2; i++) {
				leftList.add(nodeList.poll());
			}

			xk.addChild(constructkdTreeY(g, leftList, depth + 1));
			xk.addChild(constructkdTreeY(g, nodeList, depth + 1));
		}
		return xk;
	}

	public LinkedList<Node> rangeQuery(AnnotatedOsmGraph g, double topLeftLong,
			double topLeftLat, double bottomRightLong, double bottomRightLat,
			YKey key) {
		LinkedList<Node> includedNodes = new LinkedList<Node>();
		double keyVal = key.getKeyValue();
		if (key.getChildList().size() == 0) { // is true whenever key has no
			// XKey children,
			// therefore the only child of key is a NodeSet
			rangeQuery(g, topLeftLong, topLeftLat, bottomRightLong,
					bottomRightLat, key.getSetList().get(0));
		} else {
			if (keyVal < bottomRightLat) {
				// only the right subtree has to be examined further
				includedNodes.addAll(rangeQuery(g, topLeftLong, topLeftLat,
						bottomRightLong, bottomRightLat, key.getChildList()
								.get(1)));
			}
			if (keyVal >= topLeftLat) {
				// only the left subtree has to be examined further
				includedNodes.addAll(rangeQuery(g, topLeftLong, topLeftLat,
						bottomRightLong, bottomRightLat, key.getChildList()
								.get(0)));
			}
			if ((bottomRightLat < keyVal) && (keyVal < topLeftLat)) {
				// the right and the left subtree have to be examined
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

	public LinkedList<Node> rangeQuery(AnnotatedOsmGraph g, double topLeftLong,
			double topLeftLat, double bottomRightLong, double bottomRightLat,
			XKey key) {
		LinkedList<Node> includedNodes = new LinkedList<Node>();
		double keyVal = key.getKeyValue();

		if (key.getChildList().size() == 0) { // is true, when key has a
			// NodeSet as a child
			rangeQuery(g, topLeftLong, topLeftLat, bottomRightLong,
					bottomRightLat, key.getSetList().get(0));
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
	public LinkedList<Node> rangeQuery(AnnotatedOsmGraph g, double topLeftLong,
			double topLeftLat, double bottomRightLong, double bottomRightLat,
			NodeSet set) {
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
