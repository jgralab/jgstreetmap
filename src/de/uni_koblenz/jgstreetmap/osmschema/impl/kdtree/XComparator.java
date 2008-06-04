package de.uni_koblenz.jgstreetmap.osmschema.impl.kdtree;

import java.util.Comparator;
import de.uni_koblenz.jgstreetmap.osmschema.Node;

public class XComparator implements Comparator<Node>{

	public int compare(Node n1, Node n2) {
		if(n1.getLongitude()<n2.getLongitude())
			return -1;
		if(n1.getLongitude()==n2.getLongitude())
			return 0;
		return 1;
	}

}
