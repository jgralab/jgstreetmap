package de.uni_koblenz.jgstreetmap.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.List;

import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import de.uni_koblenz.jgralab.BooleanGraphMarker;
import de.uni_koblenz.jgstreetmap.model.AnnotatedOsmGraph;
import de.uni_koblenz.jgstreetmap.model.LayoutInfo;
import de.uni_koblenz.jgstreetmap.osmschema.HasNode;
import de.uni_koblenz.jgstreetmap.osmschema.Node;
import de.uni_koblenz.jgstreetmap.osmschema.OsmPrimitive;
import de.uni_koblenz.jgstreetmap.osmschema.Way;
import de.uni_koblenz.jgstreetmap.osmschema.routing.Segment;
import de.uni_koblenz.jgstreetmap.osmschema.routing.SegmentType;

public class MapPanel extends JPanel {
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
	private static final Color END_COLOR = Color.RED;
	private static final Color WAY_COLOR = Color.RED;
	private static final Color EDGE_COLOR = Color.BLUE; // new Color(0, 0, 255,
	// 128);
	private static final Color NODE_FILLER = Color.WHITE;

	private static final double MINUTE = 1.0 / 60.0;

	private AnnotatedOsmGraph graph;

	private double lonW, lonE; // E-W bounds of display area
	private double latS, latN; // N-S bounds of display area
	private double latC, lonC; // center of display area
	private double scaleLat; // pixel/minute in N-S direction
	private double scaleLon; // pixel/minute in E-W direction

	private Point mousePos; // uses to save click positions

	private RenderingHints antialiasOn; // anti-aliased painting hints
	private RenderingHints antialiasOff; // simple painting hints

	BooleanGraphMarker visibleElements; // marks elements to be painted

	private BoundedRangeModel zoomLevel;

	private boolean showStreetsOnly = false;
	private boolean showGraph = false;
	private boolean showWaysInGraph = true;
	private boolean showMap = true;

	public MapPanel(AnnotatedOsmGraph graph) {
		this.graph = graph;
		visibleElements = new BooleanGraphMarker(graph);

		antialiasOff = new RenderingHints(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_OFF);

		antialiasOn = new RenderingHints(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		antialiasOn.put(RenderingHints.KEY_RENDERING,
				RenderingHints.VALUE_RENDER_QUALITY);

		setMinimumSize(new Dimension(200, 150));
		setPreferredSize(new Dimension(1000, 750));

		zoomLevel = new DefaultBoundedRangeModel(ZOOM_INIT, 0, ZOOM_MIN,
				ZOOM_MAX);
		zoomLevel.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				zoomChanged();
			}
		});

		zoomChanged();
		setDefaultPosition();

		mousePos = new Point();

		addMouseListener(new MouseAdapter() {
			// Stores mouse position on click.
			@Override
			public void mousePressed(MouseEvent e) {
				super.mousePressed(e);
				mousePos.x = e.getX();
				mousePos.y = e.getY();
				// System.out.println(formatLatitude(getLat(mousePos.y)) + " "
				// + formatLongitude(getLon(mousePos.x)));
			}

			// Computes translation of the mouse coordinates and recenters the
			// map to the new position
			public void mouseReleased(MouseEvent e) {
				super.mouseReleased(e);
				double deltaLat = getLat(e.getY()) - getLat(mousePos.y);
				double deltaLon = getLon(e.getX()) - getLon(mousePos.x);
				setCenter(latC - deltaLat, lonC - deltaLon);
			}
		});
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
	 * Computes the longitude value for a given <code>x</code> position on
	 * this MapPanel.
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
		// super.paint(g);

		// compute latitude boundaries w.r.t. the center position and the height
		// of this panel
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
		LayoutInfo.setStreetsOnly(showStreetsOnly);

		computeVisibleElements();
		g.setClip(FRAMEWIDTH + 1, FRAMEWIDTH + 1, getWidth() - 2 * FRAMEWIDTH
				- 1, getHeight() - 2 * FRAMEWIDTH - 1);
		if (showMap) {
			paintMap(g2);
		}
		if (showGraph) {
			paintGraph(g2);
		}

		// draw center cross
		g2.setRenderingHints(antialiasOff);
		g2.setStroke(new BasicStroke(1.0f, BasicStroke.JOIN_BEVEL,
				BasicStroke.CAP_BUTT));
		int x = getPx(lonC);
		int y = getPy(latC);
		g2.setColor(Color.RED);
		g2.drawLine(x, y - 20, x, y + 20);
		g2.drawLine(x - 20, y, x + 20, y);

	}

	/**
	 * Paints the map in the coordinate range latS..latN/lonW..lonE.
	 * 
	 * @param g
	 *            graphics context for paint operations
	 */
	private void paintMap(Graphics2D g) {
		long start = System.currentTimeMillis();

		g.setRenderingHints(antialiasOn);

		// draw areas
		for (List<Way> lst : graph.getOrderedWayVertices().values()) {
			for (Way way : lst) {
				LayoutInfo l = graph.getLayoutInfo(way);
				if (l.enabled && l.visible && l.area
						&& visibleElements.isMarked(way)) {
					Polygon poly = new Polygon();
					for (Node n : way.getNodeList()) {
						poly.addPoint(getPx(n.getLongitude()), getPy(n
								.getLatitude()));
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
				if (l.enabled && l.visible && l.bgColor != null && !l.area
						&& visibleElements.isMarked(way)
						&& way.getWayType() != SegmentType.NOWAY) {
					Polygon poly = new Polygon();
					for (Node n : way.getNodeList()) {
						poly.addPoint(getPx(n.getLongitude()), getPy(n
								.getLatitude()));
					}
					g.setStroke(new BasicStroke((float) l.width,
							BasicStroke.JOIN_ROUND, BasicStroke.CAP_ROUND));
					g.setColor(l.bgColor);
					g.drawPolyline(poly.xpoints, poly.ypoints, poly.npoints);
				}
			}

			// draw foreground of ways
			for (Way way : lst) {
				LayoutInfo l = graph.getLayoutInfo(way);
				if (l.enabled && l.visible && !l.area
						&& visibleElements.isMarked(way)
						&& way.getWayType() != SegmentType.NOWAY) {
					Polygon poly = new Polygon();
					for (Node n : way.getNodeList()) {
						poly.addPoint(getPx(n.getLongitude()), getPy(n
								.getLatitude()));
					}
					g.setStroke(new BasicStroke((float) l.width * 0.75f,
							BasicStroke.JOIN_ROUND, BasicStroke.CAP_ROUND));
					g.setColor(l.fgColor);
					g.drawPolyline(poly.xpoints, poly.ypoints, poly.npoints);
				}
			}
		}

		long stop = System.currentTimeMillis();
		System.out.println("time to paint map: " + (stop - start) + "ms");
	}

	private void computeVisibleElements() {
		long start = System.currentTimeMillis();
		// determine which ways are (at least partly) visible
		// this doesn't work for large zooms, because only visible nodes are
		// determined, also doesn't work correctly for areas, but for
		// demonstration purposes, the result is ok ;-)

		// TODO: perform intersection test not for nodes (end-points) but for
		// TODO: the whole line segment and/or the whole area
		visibleElements.clear();
		for (Node n : graph.getNodeVertices()) {
			if (n.getLatitude() >= latS && n.getLatitude() <= latN
					&& n.getLongitude() >= lonW && n.getLongitude() <= lonE) {
				visibleElements.mark(n);
				HasNode e = n.getFirstHasNode();
				while (e != null) {
					OsmPrimitive o = (OsmPrimitive) e.getThat();
					visibleElements.mark(o);
					e = e.getNextHasNode();
				}
			}
		}
		long stop = System.currentTimeMillis();
		System.out.println("time to compute visible elements: "
				+ (stop - start) + "ms");
	}

	// TODO: display street edges correctly
	// In Graph display, "street" edges are not visualized correctly. Those
	// edges only connect end points of ways, not the intersections as it should
	// be. This will be fixed as soon as the importer is ready to create
	// "street" links.
	private void paintGraph(Graphics2D g) {
		if (zoomLevel.getValue() < 10) {
			return;
		}
		long start = System.currentTimeMillis();
		g.setRenderingHints(antialiasOn);

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
				if (l == null || !l.enabled || !l.visible) {
					continue;
				}

				HasNode e = way.getFirstHasNode();
				if (e == null) {
					continue;
				}

				// determine begin and end position of the current way
				Node n = (Node) e.getOmega();
				alpha.x = getPx(n.getLongitude());
				alpha.y = getPy(n.getLatitude());
				while (e != null) {
					n = (Node) e.getOmega();
					e = e.getNextHasNode();
				}
				omega.x = getPx(n.getLongitude());
				omega.y = getPy(n.getLatitude());

				// determine compute the location of the node representing
				// the
				// way by computing a point on the perpendicular in the
				// middle of the segment alpha-omega. the distance of this
				// point
				// to the segment is limited to a length of 50 to 200 metres
				// w.r.t. the scale of the display.
				AffineTransform t = g.getTransform();
				double dx = omega.x - alpha.x;
				double dy = omega.y - alpha.y;
				double len = Math.sqrt(dx * dx + dy * dy) / 2.0;
				double theta = Math.atan2(dy, dx);
				g.translate(alpha.x, alpha.y);
				g.rotate(theta);
				g.translate(len, 0);
				g.rotate(Math.PI / 2.0);
				len = Math.max(50.0 * scaleLat / 1852, Math.min(
						200.0 * scaleLat / 1852, len));
				Point2D.Double dst = new Point2D.Double(len, 0);
				g.getTransform().transform(dst, dst);
				waypoint.x = (int) dst.x;
				waypoint.y = (int) dst.y;
				g.setTransform(t);

				// draw graph edges
				e = way.getFirstHasNode();
				while (e != null) {
					n = (Node) e.getOmega();
					int y = getPy(n.getLatitude());
					int x = getPx(n.getLongitude());

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
					e = e.getNextHasNode();
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
			}
		} else {
			// show segments, not ways
			for (Segment s : graph.getSegmentVertices()) {
				Node source = (Node) s.getFirstHasSource().getOmega();
				if (!visibleElements.isMarked(source)) {
					continue;
				}
				Node target = (Node) s.getFirstHasTarget().getOmega();
				if (!visibleElements.isMarked(target)) {
					continue;
				}
				alpha.x = getPx(source.getLongitude());
				alpha.y = getPy(source.getLatitude());
				omega.x = getPx(target.getLongitude());
				omega.y = getPy(target.getLatitude());

				// paint the edge connecting source and target of a segment
				drawEdge(g, alpha, omega, diameter, edgeStroke, true); // s.isOneway());

				// draw alpha and omega nodes
				g.setStroke(outlineStroke);
				g.setColor(END_COLOR);
				g.drawLine(alpha.x, alpha.y, alpha.x, alpha.y);
				g.drawLine(omega.x, omega.y, omega.x, omega.y);
				if (zoomLevel.getValue() >= 20) {
					g.setColor(NODE_FILLER);
					g.setStroke(fillerStroke);
					g.drawLine(alpha.x, alpha.y, alpha.x, alpha.y);
					g.drawLine(omega.x, omega.y, omega.x, omega.y);
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
		g.setRenderingHints(antialiasOff);

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

		Graphics2D g2 = (Graphics2D) g;
		s = formatLatitude(latN);
		AffineTransform t = g2.getTransform();
		g2.rotate(-Math.PI / 2, g.getFont().getSize(), FRAMEWIDTH
				+ g.getFontMetrics().stringWidth(s));
		g2.drawString(s, g.getFont().getSize(), FRAMEWIDTH
				+ g.getFontMetrics().stringWidth(s));

		s = formatLatitude(latS);
		g2.setTransform(t);
		g2
				.rotate(-Math.PI / 2, g.getFont().getSize(), getHeight()
						- FRAMEWIDTH);
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
	private String formatLongitude(double lon) {
		assert lon >= -180.0 && lon <= 180.0;
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
	private String formatLatitude(double lat) {
		assert lat >= -90.0 && lat <= 90.0;
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
	private String formatPos(double degrees, char p, char n) {
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

	/**
	 * Computes the new latitude scale (pixel per angle minute) as follows:
	 * scaleLat = 10 ^ (zoom/10) and repaints the map if needed.
	 */
	private void zoomChanged() {
		int zoom = zoomLevel.getValue();
		scaleLat = Math.pow(10.0, zoom / 10.0);

		// adjust visibility and line widths of layout information
		LayoutInfo.updateLayoutInfo(zoom, scaleLat);

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
			repaint();
		}
	}

	public boolean isShowingGraph() {
		return showGraph;
	}

	public void setShowGraph(boolean b) {
		if (b != showGraph) {
			showGraph = b;
			repaint();
		}
	}

	public boolean isShowingWaysInGraph() {
		return showWaysInGraph;
	}

	public void setShowWaysInGraph(boolean b) {
		if (b != showWaysInGraph) {
			showWaysInGraph = b;
			if (showGraph) {
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
			repaint();
		}
	}
}