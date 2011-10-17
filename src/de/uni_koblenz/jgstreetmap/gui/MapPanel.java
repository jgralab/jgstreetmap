package de.uni_koblenz.jgstreetmap.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import de.uni_koblenz.jgralab.graphmarker.BooleanGraphMarker;
import de.uni_koblenz.jgstreetmap.model.AnnotatedOsmGraph;
import de.uni_koblenz.jgstreetmap.model.AnnotatedOsmGraph.Neighbour;
import de.uni_koblenz.jgstreetmap.model.LayoutInfo;
import de.uni_koblenz.jgstreetmap.model.kdtree.KDTreeQueries;
import de.uni_koblenz.jgstreetmap.osmschema.map.HasNode;
import de.uni_koblenz.jgstreetmap.osmschema.map.Node;
import de.uni_koblenz.jgstreetmap.osmschema.map.Way;
import de.uni_koblenz.jgstreetmap.osmschema.routing.Segment;
import de.uni_koblenz.jgstreetmap.osmschema.routing.SegmentType;
import de.uni_koblenz.jgstreetmap.routing.AStarRouteCalculator;
import de.uni_koblenz.jgstreetmap.routing.DijkstraRouteCalculator;
import de.uni_koblenz.jgstreetmap.routing.RouteCalculator;
import de.uni_koblenz.jgstreetmap.routing.RouteCalculator.EdgeRating;
import de.uni_koblenz.jgstreetmap.routing.RouteCalculator.RoutingRestriction;
import de.uni_koblenz.jgstreetmap.routing.RoutingResult;

public class MapPanel extends JPanel implements Printable {
	private static final long serialVersionUID = 1L;

	public static final int ZOOM_MAX = 40;
	public static final int ZOOM_INIT = 30;
	public static final int ZOOM_MIN = 0;

	private static final int FRAMEWIDTH = 16;

	private static final Color FRAME_BG_COLOR = Color.WHITE;;
	private static final Color FRAME_FG_COLOR = Color.BLACK;
	private static final Color FRAME_FONT_COLOR = Color.BLUE;
	private static final Color FRAME_SHADE_COLOR = Color.getHSBColor(0.59167f,
			0.04f, 0.98f);
	private static final Color MAP_BG_COLOR = new Color(255, 255, 224);
	private static final Color NODE_COLOR = Color.GREEN.darker();
	// private static final Color END_COLOR = Color.RED;
	private static final Color WAY_COLOR = Color.RED;
	private static final Color EDGE_COLOR = new Color(0, 0, 255, 128);
	private static final Color LABEL_COLOR = Color.MAGENTA.darker();
	private static final Color NODE_FILLER = Color.WHITE;

	private static final double MINUTE = 1.0 / 60.0;

	private AnnotatedOsmGraph graph;

	private double lonW, lonE; // E-W bounds of display area
	private double latS, latN; // N-S bounds of display area
	private double latC, lonC; // center of display area
	private double scaleLat; // pixel/minute in N-S direction
	private double scaleLon; // pixel/minute in E-W direction

	private Point mousePos; // used to save click positions
	private boolean mouseDragged; // true if mouse was dragged
	private boolean mouseSetStartNode;

	// simple painting hints (can be disabled with the disable-aa system
	// property)
	private RenderingHints antialias;

	BooleanGraphMarker visibleElements; // marks elements to be painted

	private BoundedRangeModel zoomLevel;

	private boolean showStreetsOnly = false;
	private boolean showGraph = false;
	private boolean showWaysInGraph = true;
	private boolean showMap = true;
	private boolean showLength = false;
	private boolean showWayIds = true;
	private boolean showNodeIds = false;
	private boolean showTowns = true;

	private transient RouteCalculator fastestRouteCalculator;
	private transient RouteCalculator shortestRouteCalculator;
	private transient RouteCalculator mostConvenientRouteCalculator;
	private boolean showRoutes = true;
	private RoutingResult fastestRoute = null;
	private RoutingResult shortestRoute = null;
	private RoutingResult mostConvenientRoute = null;
	private Node startNode = null;

	private ResultPanel resultPanel;

	private Map<String, Node> townMap = new HashMap<String, Node>(10000);

	public enum RoutingAlgorithms {
		Dijkstra, AStar;
	}

	// public MapPanel(AnnotatedOsmGraph graph, ResultPanel respnl) {
	// this(graph, respnl, System.getProperty("disable-aa") == null);
	// }

	public MapPanel(AnnotatedOsmGraph graph, ResultPanel respnl,
			boolean withAntialiasing) {
		this.graph = graph;
		this.resultPanel = respnl;

		initializeTownMap();

		setRoutingAlgorithm(RoutingAlgorithms.Dijkstra);

		visibleElements = new BooleanGraphMarker(graph);

		startNode = null;
		// (Node) graph.getOsmPrimitiveById(30432771);

		if (withAntialiasing) {
			System.out.println("Antialiasing enabled!");
			antialias = new RenderingHints(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
			antialias.put(RenderingHints.KEY_RENDERING,
					RenderingHints.VALUE_RENDER_QUALITY);
		} else {
			System.out.println("Antialiasing disabled!");
			antialias = new RenderingHints(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_OFF);
		}

		setMinimumSize(new Dimension(200, 150));
		setPreferredSize(new Dimension(1000, 750));

		zoomLevel = new DefaultBoundedRangeModel(ZOOM_INIT, 0, ZOOM_MIN,
				ZOOM_MAX);
		zoomLevel.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if (isVisible()) {
					repaint();
				}
			}
		});

		setDefaultPosition();

		mousePos = new Point();

		addMouseMotionListener(new MouseMotionListener() {

			@Override
			public void mouseDragged(MouseEvent e) {
				setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				mouseDragged = true;
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				// TODO Auto-generated method stub

			}

		});
		addMouseListener(new MouseAdapter() {
			// Stores mouse position on click.
			@Override
			public void mousePressed(MouseEvent e) {
				super.mousePressed(e);
				mouseDragged = false;
				mousePos.x = e.getX();
				mousePos.y = e.getY();

				// System.out.println(formatLatitude(lat) + " "
				// + formatLongitude(lon));
			}

			// Computes translation of the mouse coordinates and recenters the
			// map to the new position
			@Override
			public void mouseReleased(MouseEvent e) {
				super.mouseReleased(e);
				double lat = getLat(mousePos.y);
				double lon = getLon(mousePos.x);
				if (mouseDragged) {
					double deltaLat = getLat(e.getY()) - lat;
					double deltaLon = getLon(e.getX()) - lon;
					setCursor(Cursor
							.getPredefinedCursor(mouseSetStartNode ? Cursor.CROSSHAIR_CURSOR
									: Cursor.DEFAULT_CURSOR));
					setCenter(latC - deltaLat, lonC - deltaLon);
				} else {
					// long start = System.currentTimeMillis();
					List<Neighbour> neighbours = null;
					// for (int n = 0; n < 1000; ++n) {
					neighbours = KDTreeQueries.neighboursKD(
							MapPanel.this.graph, lat, lon, 100.0);
					// }
					// long stop = System.currentTimeMillis();
					// System.out.println("Time:" + (stop - start));
					resultPanel.clear();
					if ((neighbours != null) && (neighbours.size() >= 1)) {
						Node dest = neighbours.get(0).getNode();
						if (mouseSetStartNode) {
							setStartNode(dest);
							printNode("Start", startNode);
							setCursor(Cursor
									.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
							mouseSetStartNode = false;
						} else {
							printNode("Start", startNode);
							printNode("Destination", dest);
							if (startNode != null) {
								System.out.println("Selected destination node "
										+ dest);
								fastestRoute = fastestRouteCalculator.getRoute(
										dest, EdgeRating.TIME);
								shortestRoute = shortestRouteCalculator
										.getRoute(dest, EdgeRating.LENGTH);
								if (mostConvenientRouteCalculator != null) {
									mostConvenientRoute = mostConvenientRouteCalculator
											.getRoute(dest,
													EdgeRating.CONVENIENCE);
								}
								if (showRoutes) {
									repaint();
								}
								printRoute(fastestRoute,
										fastestRouteCalculator, EdgeRating.TIME);
								printRoute(shortestRoute,
										shortestRouteCalculator,
										EdgeRating.LENGTH);
								if (mostConvenientRoute != null) {
									printRoute(mostConvenientRoute,
											mostConvenientRouteCalculator,
											EdgeRating.CONVENIENCE);
								}
							}
						}
					} else {
						resultPanel.println("No node found :-(");
					}
				}
			}

		});
	}

	private void initializeTownMap() {
		for (Node n : graph.getNodeVertices()) {
			Map<String, String> tags = n.get_tags();
			if ((tags == null) || !tags.containsKey("place")
					|| !tags.containsKey("name")) {
				continue;
			}
			townMap.put(tags.get("name").toLowerCase(), n);
		}
		System.out.println("Initialized town map with " + townMap.size()
				+ " towns.");
	}

	private void setStartNode(Node n) {
		if (n != null) {
			System.out.println("Selected start node " + n);
			startNode = n;
			resultPanel.clear();
			fastestRouteCalculator.setStart(startNode);
			shortestRouteCalculator.setStart(startNode);
			if (mostConvenientRouteCalculator != null) {
				mostConvenientRouteCalculator.setStart(startNode);
			}
		}
	}

	private void printNode(String label, Node n) {
		if (n == null) {
			return;
		}
		resultPanel.printLabel(label + ":", Color.white);
		boolean foundName = false;
		for (Way w : n.get_ways()) {
			String name = AnnotatedOsmGraph.getTag(w, "name");
			if (name != null) {
				name = name.trim();
			}
			if ((name == null) || (name.length() == 0)) {
				name = AnnotatedOsmGraph.getTag(w, "ref");
				if (name != null) {
					name = name.trim();
				}
			}
			if ((name != null) && (name.length() > 0)) {
				resultPanel.print("  " + name);
				foundName = true;
			}
		}
		if (!foundName) {
			resultPanel.print("  Node " + n.get_osmId());
		}
		resultPanel.println();
	}

	private void printRoute(RoutingResult result, RouteCalculator calculator,
			EdgeRating rating) {
		List<Segment> route = result.getRoute();

		resultPanel.println();
		switch (rating) {
		case TIME:
			resultPanel.printLabel("Fastest Route:",
					LayoutInfo.FASTEST_ROUTE.bgColor);
			break;
		case LENGTH:
			resultPanel.printLabel("Shortest Route:",
					LayoutInfo.SHORTEST_ROUTE.bgColor);
			break;
		case CONVENIENCE:
			resultPanel.printLabel("Most convenient Route:",
					LayoutInfo.MOSTCONVENIENT_ROUTE.bgColor);
			break;
		}

		if ((route == null) || (route.size() < 1)) {
			resultPanel.println("No route found :-(");
			return;
		}

		double length = calculator.calculateCompleteWeight(route,
				EdgeRating.LENGTH) / 1000.0;
		double time = calculator
				.calculateCompleteWeight(route, EdgeRating.TIME) / 3600;
		// double degree = calculator.calculateCompleteWeight(route,
		// EdgeRating.CONVENIENCE);
		resultPanel.println("  Calculation time: "
				+ result.getRouteCalculationTime() / 1000d + " seconds");

		resultPanel.println("  Length: " + Math.round(length * 100.0) / 100.0
				+ "km");

		long hrs = (long) Math.floor(time);
		long min = (long) Math.floor((time - hrs) * 60.0);
		long sec = Math.round((time - (hrs + min / 60d)) * 60 * 60);
		resultPanel.println("  Time  : " + hrs + " h " + min + " min " + sec
				+ " sec");
		resultPanel.println();

		// resultPanel.println(" Degree : " + degree);
		Way lastWay = null;
		String lastName = null;
		for (Segment s : route) {
			Way way = getWay(s);
			if ((way != null) && (way != lastWay)) {
				String name = AnnotatedOsmGraph.getTag(way, "name");
				if (name != null) {
					name = name.trim();
				}
				if (name == null) {
					name = AnnotatedOsmGraph.getTag(way, "ref");
					if (name != null) {
						name = name.trim();
					}
				}
				if ((name != null) && !name.equals(lastName)) {
					resultPanel.println("  " + name);
					lastName = name;
				}
				lastWay = way;
			}

		}
	}

	private Way getWay(Segment s) {
		for (Way w : ((Node) s.getAlpha()).get_ways()) {
			for (Way v : ((Node) s.getOmega()).get_ways()) {
				if (v == w) {
					return w;
				}
			}
		}
		return null;
	}

	/**
	 * Sets the center of the map to the default position University Campus,
	 * "Steiler Zahn".
	 */
	public void setDefaultPosition() {
		setCenter(50.36258, 7.5587);
	}

	/**
	 * Computes the x position on this MapPanel for a given longitude
	 * <code>lon</code>.
	 * 
	 * @param lon
	 *            a longitude value
	 * @return the x position
	 */
	private int getPx(double lon) {
		return FRAMEWIDTH
				+ (int) Math.round(((getWidth() - FRAMEWIDTH * 2)
						* (lon - lonW) / (lonE - lonW)));
	}

	/**
	 * Computes the y position on this MapPanel for a given latitude
	 * <code>lat</code>.
	 * 
	 * @param lat
	 *            a latitude value
	 * @return the y position
	 */
	private int getPy(double lat) {
		return getHeight()
				- FRAMEWIDTH
				- (int) Math.round(((getHeight() - FRAMEWIDTH * 2)
						* (lat - latS) / (latN - latS)));
	}

	/**
	 * Computes the latitude value for a given <code>y</code> position on this
	 * MapPanel.
	 * 
	 * @param y
	 *            an y position
	 * @return the latitude value
	 */
	private double getLat(int y) {
		return latN - ((double) (y - FRAMEWIDTH))
				/ (getHeight() - FRAMEWIDTH * 2) * (latN - latS);
	}

	/**
	 * Computes the longitude value for a given <code>x</code> position on this
	 * MapPanel.
	 * 
	 * @param x
	 *            an x position
	 * @return the longitude value
	 */
	private double getLon(int x) {
		return lonW + ((double) (x - FRAMEWIDTH))
				/ (getWidth() - FRAMEWIDTH * 2) * (lonE - lonW);
	}

	/**
	 * Paints a more or less beautiful map.
	 */
	@Override
	public void paint(Graphics g) {
		// compute latitude boundaries w.r.t. the center position and the height
		// of this panel
		scaleLat = Math.pow(10.0, zoomLevel.getValue() / 10.0);
		double mapHeight = getHeight() - 2 * FRAMEWIDTH;
		double deltaLat = MINUTE * mapHeight / scaleLat / 2.0;
		latS = latC - deltaLat;
		latN = latC + deltaLat;

		// compute longittude boundaries w.r.t. the center position and the
		// width of this panel. additionally, the longitude scale has to be
		// stretched the more north the display area is located.
		scaleLon = scaleLat * Math.cos(Math.toRadians(latC));

		double mapWidth = getWidth() - 2 * FRAMEWIDTH;
		double deltaLon = MINUTE * mapWidth / scaleLon / 2.0;
		lonW = lonC - deltaLon;
		lonE = lonC + deltaLon;

		Graphics2D g2 = (Graphics2D) g;
		paintFrame(g2);

		LayoutInfo.updateLayoutInfo(g2, zoomLevel.getValue(), scaleLat,
				showStreetsOnly);

		computeVisibleElements();
		g.setClip(FRAMEWIDTH + 1, FRAMEWIDTH + 1, getWidth() - 2 * FRAMEWIDTH
				- 1, getHeight() - 2 * FRAMEWIDTH - 1);
		if (showMap) {
			paintMap(g2);
		}
		if (showGraph) {
			paintGraph(g2);
		}
		if (showRoutes) {
			paintRoute(g2, fastestRoute, LayoutInfo.FASTEST_ROUTE);
			paintRoute(g2, shortestRoute, LayoutInfo.SHORTEST_ROUTE);
			paintRoute(g2, mostConvenientRoute, LayoutInfo.MOSTCONVENIENT_ROUTE);
		}

		// draw center cross
		g2.setRenderingHints(antialias);
		g2.setStroke(new BasicStroke(1.0f, BasicStroke.JOIN_BEVEL,
				BasicStroke.CAP_BUTT));
		int x = getPx(lonC);
		int y = getPy(latC);
		g2.setColor(Color.RED);
		g2.drawLine(x, y - 20, x, y + 20);
		g2.drawLine(x - 20, y, x + 20, y);

	}

	private void paintRoute(Graphics2D g, RoutingResult result, LayoutInfo l) {
		if ((result == null) || (result.getRoute() == null)
				|| (result.getRoute().size() < 1)) {
			return;
		}
		Polygon poly = new Polygon();
		Segment lastSeg = null;
		for (Segment s : result.getRoute()) {
			Node n = (Node) s.getThis();
			poly.addPoint(getPx(n.get_longitude()), getPy(n.get_latitude()));
			lastSeg = s;
		}
		if (lastSeg != null) {
			Node n = (Node) lastSeg.getThat();
			poly.addPoint(getPx(n.get_longitude()), getPy(n.get_latitude()));
		}
		g.setStroke(l.bgStroke);
		g.setColor(l.bgColor);
		g.drawPolyline(poly.xpoints, poly.ypoints, poly.npoints);
	}

	/**
	 * Paints the map in the coordinate range latS..latN/lonW..lonE.
	 * 
	 * @param g
	 *            graphics context for paint operations
	 */
	private void paintMap(Graphics2D g) {
		// long start = System.currentTimeMillis();

		g.setRenderingHints(antialias);

		// draw areas
		for (List<Way> lst : graph.getOrderedWayVertices().values()) {
			for (Way way : lst) {
				LayoutInfo l = graph.getLayoutInfo(way);
				if (l.enabled && l.visible && l.area
						&& visibleElements.isMarked(way)) {
					Polygon poly = new Polygon();
					for (Node n : way.get_nodes()) {
						poly.addPoint(getPx(n.get_longitude()),
								getPy(n.get_latitude()));
					}
					g.setColor(l.fgColor);
					g.fillPolygon(poly.xpoints, poly.ypoints, poly.npoints);
				}
			}
		}

		// draw background of ways (a slightly wider line as border)
		for (List<Way> lst : graph.getOrderedWayVertices().values()) {
			for (Way way : lst) {
				LayoutInfo l = graph.getLayoutInfo(way);
				if (l.enabled
						&& l.visible
						&& (l.bgColor != null)
						&& !l.area
						&& (l.bgColor != null)
						&& visibleElements.isMarked(way)
						&& (!showStreetsOnly || (way.get_wayType() != SegmentType.NOWAY))) {
					Polygon poly = new Polygon();
					for (Node n : way.get_nodes()) {
						poly.addPoint(getPx(n.get_longitude()),
								getPy(n.get_latitude()));
					}
					g.setStroke(l.bgStroke);
					g.setColor(l.bgColor);
					g.drawPolyline(poly.xpoints, poly.ypoints, poly.npoints);
				}
			}

			// draw foreground of ways
			for (Way way : lst) {
				LayoutInfo l = graph.getLayoutInfo(way);
				if (l.enabled
						&& l.visible
						&& !l.area
						&& visibleElements.isMarked(way)
						&& (l.fgColor != null)
						&& (!showStreetsOnly || (way.get_wayType() != SegmentType.NOWAY))) {
					Polygon poly = new Polygon();
					for (Node n : way.get_nodes()) {
						poly.addPoint(getPx(n.get_longitude()),
								getPy(n.get_latitude()));
					}
					g.setStroke(l.fgStroke);
					g.setColor(l.fgColor);
					g.drawPolyline(poly.xpoints, poly.ypoints, poly.npoints);
				}
			}
		}

		// draw town/village names
		if (showTowns) {
			for (Node n : townMap.values()) {
				g.setColor(Color.black);
				g.drawString(n.get_tags().get("name"),
						getPx(n.get_longitude()), getPy(n.get_latitude()));
			}
		}

		// long stop = System.currentTimeMillis();
		// System.out.println("time to paint map: " + (stop - start) + "ms");
	}

	private boolean intersects(Node a, Node b) {
		// test if the rectangle defined by the segment a-b
		// has a non-empty intersection with the display area
		double maxLat = Math.max(a.get_latitude(), b.get_latitude());
		if (maxLat < latS) {
			return false;
		}
		double minLat = Math.min(a.get_latitude(), b.get_latitude());
		if (minLat > latN) {
			return false;
		}
		double maxLon = Math.max(a.get_longitude(), b.get_longitude());
		if (maxLon < lonW) {
			return false;
		}
		double minLon = Math.min(a.get_longitude(), b.get_longitude());
		if (minLon > lonE) {
			return false;
		}
		return true;
	}

	public void setRoutingAlgorithm(RoutingAlgorithms alg) {
		switch (alg) {
		case Dijkstra:
			fastestRouteCalculator = new DijkstraRouteCalculator(graph);
			shortestRouteCalculator = new DijkstraRouteCalculator(graph);
			mostConvenientRouteCalculator = new DijkstraRouteCalculator(graph);
			System.out.println("Choosen Dijkstra routing algorithm.");
			break;
		case AStar:
			fastestRouteCalculator = new AStarRouteCalculator(graph);
			shortestRouteCalculator = new AStarRouteCalculator(graph);
			mostConvenientRouteCalculator = new AStarRouteCalculator(graph);
			System.out.println("Choosen A* routing algorithm.");
			break;
		default:
			System.err.println("Unknown RoutingAlgorithm selected! " + alg);
		}
		fastestRouteCalculator.setRestriction(RoutingRestriction.CAR);
		shortestRouteCalculator.setRestriction(RoutingRestriction.CAR);
		if (mostConvenientRouteCalculator != null) {
			mostConvenientRouteCalculator
					.setRestriction(RoutingRestriction.CAR);
		}

		System.out.println("Setting start node to " + startNode);
		setStartNode(startNode);
	}

	private void computeVisibleElements() {
		// long start = System.currentTimeMillis();
		visibleElements.clear();
		if (showWaysInGraph || showMap) {
			WAY: for (Way w : graph.getWayVertices()) {
				HasNode e = w.getFirstHasNodeIncidence();
				if (e == null) {
					continue;
				}
				Node b = (Node) e.getThat();
				Node a;
				e = e.getNextHasNodeIncidence();
				while (e != null) {
					a = b;
					b = (Node) e.getThat();
					e = e.getNextHasNodeIncidence();
					if (intersects(a, b)) {
						visibleElements.mark(w);
						continue WAY;
					}
				}
			}
		}

		if (showGraph && !showWaysInGraph) {
			for (Segment s : graph.getSegmentEdges()) {
				Node a = (Node) s.getAlpha();
				Node b = (Node) s.getOmega();
				if (intersects(a, b)) {
					visibleElements.mark(s);
				}
			}
		}

		// long stop = System.currentTimeMillis();
		// System.out.println("time to compute visible elements: "
		// + (stop - start) + "ms");
	}

	private void paintGraph(Graphics2D g) {
		if (zoomLevel.getValue() < 10) {
			return;
		}
		long start = System.currentTimeMillis();
		g.setRenderingHints(antialias);

		double diameter = Math.max(1.0, 10.0 * scaleLat / 1852.0);
		double width = 0.25 * diameter;

		BasicStroke outlineStroke = new BasicStroke((float) diameter,
				BasicStroke.JOIN_ROUND, BasicStroke.CAP_ROUND);
		BasicStroke fillerStroke = new BasicStroke((float) diameter * 0.75f,
				BasicStroke.JOIN_ROUND, BasicStroke.CAP_ROUND);
		BasicStroke edgeStroke = new BasicStroke((float) width,
				BasicStroke.JOIN_ROUND, BasicStroke.CAP_ROUND);

		Point alpha = new Point();
		Point omega = new Point();
		Point waypoint = new Point();
		Point node = new Point();

		if (showWaysInGraph) {
			// show ways, not segments
			for (Way way : graph.getWayVertices()) {
				if (!visibleElements.isMarked(way)) {
					continue;
				}

				LayoutInfo l = graph.getLayoutInfo(way);
				if ((l == null) || !l.enabled || !l.visible) {
					continue;
				}

				HasNode e = way.getFirstHasNodeIncidence();
				if (e == null) {
					continue;
				}

				// determine begin and end position of the current way
				Node n = (Node) e.getOmega();
				alpha.x = getPx(n.get_longitude());
				alpha.y = getPy(n.get_latitude());
				while (e != null) {
					n = (Node) e.getOmega();
					e = e.getNextHasNodeIncidence();
				}
				omega.x = getPx(n.get_longitude());
				omega.y = getPy(n.get_latitude());

				// determine compute the location of the node representing
				// the way by computing a point on the perpendicular in the
				// middle of the segment alpha-omega. the distance of this
				// point to the segment is limited to a length of 50 to 200
				// metres w.r.t. the scale of the display.
				AffineTransform t = g.getTransform();
				double dx = omega.x - alpha.x;
				double dy = omega.y - alpha.y;
				double len = Math.sqrt(dx * dx + dy * dy) / 2.0;
				double theta = Math.atan2(dy, dx);
				g.translate(alpha.x, alpha.y);
				g.rotate(theta);
				g.translate(len, 0);
				g.rotate(Math.PI / 2.0);
				len = Math.max(50.0 * scaleLat / 1852,
						Math.min(200.0 * scaleLat / 1852, len));
				Point2D.Double dst = new Point2D.Double(len, 0);
				g.getTransform().transform(dst, dst);
				waypoint.x = (int) dst.x;
				waypoint.y = (int) dst.y;
				g.setTransform(t);

				// draw graph edges
				e = way.getFirstHasNodeIncidence();
				while (e != null) {
					n = (Node) e.getOmega();
					int y = getPy(n.get_latitude());
					int x = getPx(n.get_longitude());

					// Edges from way position to member nodes
					node.x = x;
					node.y = y;
					drawEdge(g, waypoint, node, diameter, edgeStroke, true);

					// intermediate nodes
					g.setColor(NODE_COLOR);
					g.setStroke(outlineStroke);
					g.drawLine(x, y, x, y);
					if (zoomLevel.getValue() >= 20) {
						g.setColor(NODE_FILLER);
						g.setStroke(fillerStroke);
						g.drawLine(x, y, x, y);
					}
					e = e.getNextHasNodeIncidence();
				}

				// draw the waypoint
				g.setStroke(outlineStroke);
				g.setColor(WAY_COLOR);
				g.drawLine(waypoint.x, waypoint.y, waypoint.x, waypoint.y);
				if (zoomLevel.getValue() >= 20) {
					g.setColor(NODE_FILLER);
					g.setStroke(fillerStroke);
					g.drawLine(waypoint.x, waypoint.y, waypoint.x, waypoint.y);
				}
				if (showWayIds && (zoomLevel.getValue() >= 30)) {
					g.setColor(LABEL_COLOR);
					String name = AnnotatedOsmGraph.getTag(way, "name");
					if ((name == null) || (name.length() == 0)) {
						name = Long.toString(way.get_osmId());
					}
					g.drawString(
							name,
							waypoint.x + 12 * outlineStroke.getLineWidth() / 20,
							waypoint.y + getFont().getSize() / 2);
				}
			}
		} else {
			// show segments, not ways
			for (Segment s : graph.getSegmentEdges()) {
				if (!visibleElements.isMarked(s)) {
					continue;
				}
				Node source = (Node) s.getAlpha();
				Node target = (Node) s.getOmega();

				alpha.x = getPx(source.get_longitude());
				alpha.y = getPy(source.get_latitude());
				omega.x = getPx(target.get_longitude());
				omega.y = getPy(target.get_latitude());

				// paint the edge connecting source and target of a segment
				drawEdge(g, alpha, omega, diameter, edgeStroke, s.is_oneway());

				// draw alpha and omega nodes
				g.setStroke(outlineStroke);
				g.setColor(NODE_COLOR);
				g.drawLine(alpha.x, alpha.y, alpha.x, alpha.y);
				g.drawLine(omega.x, omega.y, omega.x, omega.y);
				if (showNodeIds && (zoomLevel.getValue() >= 35)) {
					g.setColor(LABEL_COLOR);
					g.drawString(Long.toString(source.get_osmId()), alpha.x
							+ 12 * outlineStroke.getLineWidth() / 20, alpha.y
							+ getFont().getSize() / 2);
					g.drawString(Long.toString(target.get_osmId()), omega.x
							+ 12 * outlineStroke.getLineWidth() / 20, omega.y
							+ getFont().getSize() / 2);
				}
				if (zoomLevel.getValue() >= 20) {
					g.setColor(NODE_FILLER);
					g.setStroke(fillerStroke);
					g.drawLine(alpha.x, alpha.y, alpha.x, alpha.y);
					g.drawLine(omega.x, omega.y, omega.x, omega.y);
				}

				if (showLength && (zoomLevel.getValue() >= 30)) {
					// draw length
					AffineTransform t = g.getTransform();
					int dy = omega.y - alpha.y;
					int dx = omega.x - alpha.x;
					double theta = Math.atan2(dy, dx);
					g.translate(alpha.x, alpha.y);
					g.rotate(theta);
					g.setColor(LABEL_COLOR);
					long len = Math.round(Math.sqrt(dy * dy + dx * dx));
					long d = Math.round(s.get_length());
					String lbl = d + "m";
					// String lbl =
					// Double.toString(Math.round(theta*100.0)/100.0);
					int textWidth = g.getFontMetrics().stringWidth(lbl);
					if (Math.abs(theta) >= Math.PI / 2) {
						g.rotate(Math.PI);
						g.translate(-(len + textWidth) / 2.0, -getFont()
								.getSize());
					} else {
						g.translate((len - textWidth) / 2.0, -getFont()
								.getSize());
					}
					g.drawString(lbl, 0, 0);
					g.setTransform(t);
				}
			}
		}
		long stop = System.currentTimeMillis();
		System.out.println("time to paint graph:" + (stop - start) + "ms");
	}

	private void drawEdge(Graphics2D g, Point alpha, Point omega,
			double diameter, BasicStroke edgeStroke, boolean directed) {
		// draw edge
		g.setColor(EDGE_COLOR);
		g.setStroke(edgeStroke);
		g.drawLine(alpha.x, alpha.y, omega.x, omega.y);

		if (!directed) {
			return;
		}

		// draw arrow head
		AffineTransform t = g.getTransform();
		double theta = Math.atan2(alpha.y - omega.y, alpha.x - omega.x);
		int len = (int) (diameter);
		g.translate(omega.x, omega.y);
		g.rotate(theta);
		g.translate(diameter / 2, 0);
		g.rotate(-Math.PI / 3.0);
		g.drawLine(0, 0, 0, len);
		g.rotate(-Math.PI / 3.0);
		g.drawLine(0, 0, 0, len);
		g.setTransform(t);
	}

	/**
	 * Paints a frame of width FRAMEWIDTH around the border of this MapPanel.
	 * 
	 * @param g
	 *            graphics context for paint operations
	 */
	private void paintFrame(Graphics2D g) {
		g.setRenderingHints(antialias);
		g.setClip(0, 0, getWidth(), getHeight());
		// draw background of frame
		g.setColor(FRAME_BG_COLOR);
		g.fillRect(0, 0, FRAMEWIDTH, getHeight());
		g.fillRect(0, 0, getWidth(), FRAMEWIDTH);
		g.fillRect(getWidth() - FRAMEWIDTH, 0, getWidth() - FRAMEWIDTH,
				getHeight() - FRAMEWIDTH);
		g.fillRect(0, getHeight() - FRAMEWIDTH, getWidth(), getHeight());

		// draw ticks in N-S direction (one per minute) if height is more than
		// 20px
		if (scaleLat >= 20.0) {
			double s = Math.round(latS / MINUTE / 2.0) * 2.0 * MINUTE;
			int n = 0;
			double s0 = s;
			while (s0 < latN) {
				double s1 = s0 + MINUTE;
				int y0 = getPy(s0);
				int y1 = getPy(s1);
				g.setColor(FRAME_SHADE_COLOR);
				g.fillRect(0, y1, FRAMEWIDTH, y0 - y1);
				g.fillRect(getWidth() - FRAMEWIDTH, y1, FRAMEWIDTH, y0 - y1);
				g.setColor(FRAME_FG_COLOR);
				g.drawLine(0, y1, FRAMEWIDTH - 1, y1);
				g.drawLine(0, y0, FRAMEWIDTH - 1, y0);
				g.drawLine(getWidth() - FRAMEWIDTH, y1, getWidth(), y1);
				g.drawLine(getWidth() - FRAMEWIDTH, y0, getWidth(), y0);
				n += 2;
				s0 = s + n * MINUTE;
			}
		}

		// draw ticks in E-W direction (one per minute) if width is more than
		// 20px
		if (scaleLon >= 20.0) {
			double s = Math.round(lonW / MINUTE / 2.0) * 2.0 * MINUTE;
			double n = 0;
			double s0 = s;
			while (s0 < lonE) {
				double s1 = s0 + MINUTE;
				int x0 = getPx(s0);
				int x1 = getPx(s1);
				g.setColor(FRAME_SHADE_COLOR);
				g.fillRect(x0, 0, x1 - x0, FRAMEWIDTH);
				g.fillRect(x0, getHeight() - FRAMEWIDTH, x1 - x0, FRAMEWIDTH);
				g.setColor(FRAME_FG_COLOR);
				g.drawLine(x1, 0, x1, FRAMEWIDTH - 1);
				g.drawLine(x0, 0, x0, FRAMEWIDTH - 1);
				g.drawLine(x1, getHeight() - FRAMEWIDTH, x1, getHeight());
				g.drawLine(x0, getHeight() - FRAMEWIDTH, x0, getHeight());
				n += 2;
				s0 = s + n * MINUTE;
			}
		}

		// fill corners with background color
		g.setColor(getBackground());
		g.fillRect(0, 0, FRAMEWIDTH, FRAMEWIDTH);
		g.fillRect(0, getHeight() - FRAMEWIDTH, FRAMEWIDTH, FRAMEWIDTH);
		g.fillRect(getWidth() - FRAMEWIDTH, 0, FRAMEWIDTH, FRAMEWIDTH);
		g.fillRect(getWidth() - FRAMEWIDTH, getHeight() - FRAMEWIDTH,
				FRAMEWIDTH, FRAMEWIDTH);

		// draw a rectangle on inner sides of the frame
		g.setColor(FRAME_FG_COLOR);
		g.drawRect(FRAMEWIDTH, FRAMEWIDTH, getWidth() - 2 * FRAMEWIDTH,
				getHeight() - 2 * FRAMEWIDTH);

		// draw coordinate bounds as numbers
		g.setFont(Font.getFont(Font.SANS_SERIF));
		g.setColor(FRAME_FONT_COLOR);

		String s = formatLongitude(lonW);
		g.drawString(s, FRAMEWIDTH, g.getFont().getSize());

		s = formatLongitude(lonE);
		g.drawString(s, getWidth() - FRAMEWIDTH
				- g.getFontMetrics().stringWidth(s), g.getFont().getSize());

		Graphics2D g2 = g;
		s = formatLatitude(latN);
		AffineTransform t = g2.getTransform();
		g2.rotate(-Math.PI / 2, g.getFont().getSize(), FRAMEWIDTH
				+ g.getFontMetrics().stringWidth(s));
		g2.drawString(s, g.getFont().getSize(), FRAMEWIDTH
				+ g.getFontMetrics().stringWidth(s));

		s = formatLatitude(latS);
		g2.setTransform(t);
		g2.rotate(-Math.PI / 2, g.getFont().getSize(), getHeight() - FRAMEWIDTH);
		g2.drawString(s, g.getFont().getSize(), getHeight() - FRAMEWIDTH);
		g2.setTransform(t);

		// clear map background
		g.setColor(MAP_BG_COLOR);
		g.fillRect(FRAMEWIDTH + 1, FRAMEWIDTH + 1, getWidth() - 2 * FRAMEWIDTH
				- 1, getHeight() - 2 * FRAMEWIDTH - 1);

	}

	/**
	 * Formats a longitude value <code>lon</code>.
	 * 
	 * @param lon
	 *            a longitude value, -180.0 &lt;= lon &lt;= 180.0
	 * @return a nice string representation
	 * @see #formatPos
	 */
	public static String formatLongitude(double lon) {
		assert (lon >= -180.0) && (lon <= 180.0);
		return formatPos(lon, 'E', 'W');
	}

	/**
	 * Formats a latitude value <code>lan</code>.
	 * 
	 * @param lat
	 *            a latitude value, -90.0 &lt;= lon &lt;= 90.0
	 * @return a nice string representation
	 * @see #formatPos
	 */
	public static String formatLatitude(double lat) {
		assert (lat >= -90.0) && (lat <= 90.0);
		return formatPos(lat, 'N', 'S');
	}

	/**
	 * Formats a degrees value into a String d&#176;mm.ff'H, where H is p for
	 * positive values, and H is n for negative values. Example:
	 * 
	 * formatPos(54.5, 'N', 'S') --> 54&#176;30.00'N
	 * 
	 * formatPos(-10.5, 'E', 'W') --> 10&#176;30.00'W
	 * 
	 * @param degrees
	 *            value in degrees and decimal fractions
	 * @param p
	 *            hemisphere character for positive values
	 * @param n
	 *            hemisphere character for positive values
	 * @return a String formatted like d&#176;mm.ff'H
	 */
	public static String formatPos(double degrees, char p, char n) {
		if (degrees < 0) {
			degrees = -degrees;
			int d = (int) Math.floor(degrees);
			double frac = degrees - d;
			int m = (int) Math.round(100.0 * frac / MINUTE);
			int s = m % 100;
			m /= 100;
			return d + (m < 10 ? "\u00b00" : "\u00b0") + m + "."
					+ (s < 10 ? "0" : "") + s + "'" + n;
		} else {
			int d = (int) Math.floor(degrees);
			double frac = degrees - d;
			int m = (int) Math.round(100.0 * frac / MINUTE);
			int s = m % 100;
			m /= 100;
			return d + (m < 10 ? "\u00b00" : "\u00b0") + m + "."
					+ (s < 10 ? "0" : "") + s + "'" + p;
		}
	}

	/**
	 * Sets the center of the map display to the specified position
	 * <code>lat</code>, <code>lon</code>.
	 * 
	 * @param lat
	 *            latitude of the new center
	 * @param lon
	 *            longitude of the new center
	 */
	private void setCenter(double lat, double lon) {
		latC = lat;
		lonC = lon;
		if (isVisible()) {
			repaint();
		}
	}

	public BoundedRangeModel getZoomLevelModel() {
		return zoomLevel;
	}

	public boolean isShowingStreetsOnly() {
		return showStreetsOnly;
	}

	public void setShowStreetsOnly(boolean b) {
		if (b != showStreetsOnly) {
			showStreetsOnly = b;
			if (isVisible()) {
				repaint();
			}
		}
	}

	public boolean isShowingGraph() {
		return showGraph;
	}

	public void setShowGraph(boolean b) {
		if (b != showGraph) {
			showGraph = b;
			if (isVisible()) {
				repaint();
			}
		}
	}

	public boolean isShowingWaysInGraph() {
		return showWaysInGraph;
	}

	public void setShowWaysInGraph(boolean b) {
		if (b != showWaysInGraph) {
			showWaysInGraph = b;
			if (isVisible()) {
				repaint();
			}
		}
	}

	public boolean isShowingMap() {
		return showMap;
	}

	public void setShowMap(boolean b) {
		if (b != showMap) {
			showMap = b;
			if (isVisible()) {
				repaint();
			}
		}
	}

	public boolean isShowingLength() {
		return showLength;
	}

	public void setShowLength(boolean b) {
		if (b != showLength) {
			showLength = b;
			if (isVisible() && showGraph && !showWaysInGraph) {
				repaint();
			}
		}
	}

	public void setShowTowns(boolean b) {
		if (b != showTowns) {
			showTowns = b;
			if (isVisible()) {
				repaint();
			}
		}
	}

	public boolean isShowingWayIds() {
		return showWayIds;
	}

	public void setShowWayIds(boolean b) {
		if (b != showWayIds) {
			showWayIds = b;
			if (isVisible() && showGraph && showWaysInGraph) {
				repaint();
			}
		}
	}

	public void setShowNodeIds(boolean b) {
		if (b != showNodeIds) {
			showNodeIds = b;
			if (isVisible() && showGraph && !showWaysInGraph) {
				repaint();
			}
		}
	}

	public boolean isShowingNodeIds() {
		return showNodeIds;
	}

	public void setShowRoutes(boolean b) {
		if (b != showRoutes) {
			showRoutes = b;
			if (isVisible()
					&& ((fastestRoute != null) || (shortestRoute != null) || (mostConvenientRoute != null))) {
				repaint();
			}
		}
	}

	public boolean isShowingRoutes() {
		return showRoutes;
	}

	public void setMouseStartNode() {
		fastestRoute = null;
		shortestRoute = null;
		mostConvenientRoute = null;

		if (showRoutes) {
			repaint();
		}
		mouseSetStartNode = true;
		setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
	}

	@Override
	public int print(Graphics graphics, PageFormat pf, int page)
			throws PrinterException {
		if (page > 0) { /* We have only one page, and 'page' is zero-based */
			return NO_SUCH_PAGE;
		}
		Graphics2D g2 = (Graphics2D) graphics;
		g2.translate(pf.getImageableX(), pf.getImageableY());
		double scale = Math.min(pf.getImageableWidth() / getWidth(),
				pf.getImageableHeight() / getHeight());
		g2.scale(scale, scale);
		paint(g2);
		return PAGE_EXISTS;
	}

	public void centerTown(String town) {
		Node n = townMap.get(town);
		if (n == null) {
			return;
		}
		setCenter(n.get_latitude(), n.get_longitude());
	}

	public boolean isShowingTowns() {
		return showTowns;
	}
}