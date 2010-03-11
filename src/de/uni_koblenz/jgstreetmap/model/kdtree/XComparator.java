package de.uni_koblenz.jgstreetmap.model.kdtree;

import java.io.Serializable;
import java.util.Comparator;

import de.uni_koblenz.jgstreetmap.osmschema.map.Node;

public class XComparator implements Comparator<Node>, Serializable {

	private static final long serialVersionUID = 7667080577936537147L;

	public int compare(Node n1, Node n2) {
		if (n1.get_longitude() < n2.get_longitude()) {
			return -1;
		}
		if (n1.get_longitude() == n2.get_longitude()) {
			return 0;
		}
		return 1;
	}

}
