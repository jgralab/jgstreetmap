package de.uni_koblenz.jgstreetmap.routing;

import de.uni_koblenz.jgstreetmap.osmschema.Node;
import de.uni_koblenz.jgstreetmap.osmschema.routing.Route;

public interface RouteCalculator {
	
	public Route calculateShortestRoute(Node start, Node end);
	
}
