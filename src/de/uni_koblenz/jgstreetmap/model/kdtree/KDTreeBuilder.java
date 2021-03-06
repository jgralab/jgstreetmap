package de.uni_koblenz.jgstreetmap.model.kdtree;

import java.util.Collections;
import java.util.LinkedList;

import de.uni_koblenz.jgstreetmap.model.AnnotatedOsmGraph;
import de.uni_koblenz.jgstreetmap.osmschema.kdtree.KDTree;
import de.uni_koblenz.jgstreetmap.osmschema.kdtree.NodeSet;
import de.uni_koblenz.jgstreetmap.osmschema.kdtree.XKey;
import de.uni_koblenz.jgstreetmap.osmschema.kdtree.YKey;
import de.uni_koblenz.jgstreetmap.osmschema.map.Node;

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
		KDTree kdTree = g.createKDTree();
		kdTree.set_levels(maxLevels);
		kdTree.add_root(constructkdTreeY(g, nodeList, 0));
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
		if ((depth == g.getKDTree().get_levels()) || (nodeList.size() <= 1)) {
			NodeSet set = g.createNodeSet();
			yk.add_nodeset(set);
			for (Node n : nodeList) {
				set.add_elements(n);
			}
		} else {
			double keyValue = nodeList.get(nodeList.size() / 2 - 1)
					.get_latitude();
			yk.set_keyValue(keyValue);
			// List of nodes is split into two lists, the nodes contained in
			// leftList
			// will be part of the left subtree, the nodes left in nodeList will
			// be
			// part of the right subtree

			LinkedList<Node> leftList = new LinkedList<Node>();
			Node n = nodeList.peek();
			while (n.get_latitude() <= keyValue) {
				leftList.add(nodeList.poll());
				n = nodeList.peek();
			}
			// construct next level, which consists of XKeys
			yk.add_children(constructkdTreeX(g, leftList, depth + 1));
			yk.add_children(constructkdTreeX(g, nodeList, depth + 1));
		}
		return yk;
	}

	public static XKey constructkdTreeX(AnnotatedOsmGraph g,
			LinkedList<Node> nodeList, int depth) {
		Collections.sort(nodeList, new XComparator());
		XKey xk = g.createXKey();

		if ((depth == g.getKDTree().get_levels()) || (nodeList.size() <= 1)) {
			NodeSet set = g.createNodeSet();
			xk.add_nodeset(set);
			for (Node n : nodeList) {
				set.add_elements(n);
			}
		} else {
			double keyValue = nodeList.get(nodeList.size() / 2 - 1)
					.get_longitude();

			xk.set_keyValue(keyValue);
			LinkedList<Node> leftList = new LinkedList<Node>();
			Node n = nodeList.peek();
			while (n.get_longitude() <= keyValue) {
				leftList.add(nodeList.poll());
				n = nodeList.peek();
			}
			xk.add_children(constructkdTreeY(g, leftList, depth + 1));
			xk.add_children(constructkdTreeY(g, nodeList, depth + 1));
		}
		return xk;
	}
}
