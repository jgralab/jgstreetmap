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
}
