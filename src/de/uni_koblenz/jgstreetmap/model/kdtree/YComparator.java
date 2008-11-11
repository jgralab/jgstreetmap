package de.uni_koblenz.jgstreetmap.model.kdtree;

import java.util.Comparator;

import de.uni_koblenz.jgstreetmap.osmschema.map.Node;

public class YComparator implements Comparator<Node> {

	public int compare(Node n1, Node n2) {
		if (n1.getLatitude() < n2.getLatitude()) {
			return -1;
		}
		if (n1.getLatitude() == n2.getLatitude()) {
			return 0;
		}
		return 1;
	}

}
