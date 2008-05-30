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

public class MapPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	public static final int ZOOM_MAX = 45;
	public static final int ZOOM_INIT = 30;
	public static final int ZOOM_MIN = 0;

	private static final int FRAMEWIDTH = 16;

	private static final Color FRAME_BG_COLOR = Color.WHITE;;
	private static final Color FRAME_FG_COLOR = Color.BLACK;
	private static final Color FRAME_FONT_COLOR = Color.BLUE;
	private static final Color FRAME_SHADE_COLOR = Color.getHSBColor(0.59167f,
			0.04f, 0.98f);

	private static final Color MAP_BG_COLOR = new Color(255, 255, 224);

	private static final double MINUTE = 1.0 / 60.0;

	private AnnotatedOsmGraph graph;

	private double lonW, lonE; // E-W bounds of display area
	private double latS, latN; // N-S bounds of display area
	private double latC, lonC; // center of display area
	private double scaleLat; // px / minute in N-S direction
	private double scaleLon; // px / minute in E-W direction

	private Point mousePos; // uses to save click positions

	private RenderingHints antialiasOn; // antialiased painting hints
	private RenderingHints antialiasOff; // simple painting hints

	BooleanGraphMarker visibleElements; // marks elements to be painted

	private BoundedRangeModel zoomLevel;

	// private int zoom; // 10 * log_10 of scaleLat

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
				System.out.println(formatLatitude(getLat(mousePos.y)) + " "
						+ formatLongitude(getLon(mousePos.x)));
			}

			// Computes translation of the mouse coordinates and recenters the
			// map to the new position
			public void mouseReleased(MouseEvent e) {
				super.mouseReleased(e);
				double deltaLat = getLat(e.getY()) - getLat(mousePos.y);
				double deltaLon = getLon(e.getX()) - getLon(mousePos.x);
				latC -= deltaLat;
				lonC -= deltaLon;
				repaint();
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
	 * Computes the x postion on this MapPanel for a given longitude
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
	 * Computes the y postion on this MapPanel for a given latitude
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
	 * Computes the latitude value for a given <code>y</code> postion on this
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
	 * Computes the longitude value for a given <code>x</code> postion on this
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
		super.paint(g);

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

		// paint a frame with coordinates and grid
		paintFrame(g2);

		// paint the map
		paintMap(g2);
	}

	/**
	 * Paints the map in the coordinate range latS..latN/lonW..lonE.
	 * 
	 * @param g
	 *            graphics context for paint operations
	 */
	private void paintMap(Graphics2D g) {
		long start = System.currentTimeMillis();

		g.setClip(FRAMEWIDTH + 1, FRAMEWIDTH + 1, getWidth() - 2 * FRAMEWIDTH
				- 1, getHeight() - 2 * FRAMEWIDTH - 1);
		g.setRenderingHints(antialiasOn);

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
				HasNode e = n.getFirstHasNode();
				while (e != null) {
					OsmPrimitive o = (OsmPrimitive) e.getThat();
					visibleElements.mark(o);
					e = e.getNextHasNode();
				}
			}
		}

		long stopCompute = System.currentTimeMillis();

		// draw areas
		for (List<Way> lst : graph.getOrderedWayVertices().values()) {
			for (Way way : lst) {

				LayoutInfo l = graph.getLayoutInfo(way);
				if (l.visible && l.area && visibleElements.isMarked(way)) {
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
				if (l.visible && l.bgColor != null && !l.area
						&& visibleElements.isMarked(way)) {
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
				if (l.visible && !l.area && visibleElements.isMarked(way)) {
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

		// draw center cross
		g.setRenderingHints(antialiasOff);
		g.setStroke(new BasicStroke(1.0f, BasicStroke.JOIN_BEVEL,
				BasicStroke.CAP_BUTT));
		int x = getPx(lonC);
		int y = getPy(latC);
		g.setColor(Color.RED);
		g.drawLine(x, y - 20, x, y + 20);
		g.drawLine(x - 20, y, x + 20, y);
		long stop = System.currentTimeMillis();
		System.out.println("time to compute:" + (stopCompute - start)
				+ "ms, time to paint: " + (stop - stopCompute) + "ms");
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
	public void setCenter(double lat, double lon) {
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
}