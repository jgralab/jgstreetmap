/**
 * 
 */
package de.uni_koblenz.jgstreetmap.routing;

import java.util.List;

import de.uni_koblenz.jgstreetmap.osmschema.routing.Segment;

/**
 * @author horn
 * 
 */
public class RoutingResult {
	private List<Segment> route;
	private long routeCalculationTime;

	public RoutingResult(List<Segment> route, long calculationTime) {
		this.route = route;
		routeCalculationTime = calculationTime;
	}

	public List<Segment> getRoute() {
		return route;
	}

	public long getRouteCalculationTime() {
		return routeCalculationTime;
	}
}
