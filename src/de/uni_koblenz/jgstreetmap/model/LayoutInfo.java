package de.uni_koblenz.jgstreetmap.model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import de.uni_koblenz.jgstreetmap.osmschema.OsmPrimitive;
import de.uni_koblenz.jgstreetmap.osmschema.Tag;
import de.uni_koblenz.jgstreetmap.osmschema.Way;

public class LayoutInfo {

	private static List<LayoutInfo> infoList = new ArrayList<LayoutInfo>();

	public int zOrder;
	public Color fgColor;
	public Color bgColor;
	public double width;
	public double minWidth;
	public double maxWidth;
	public boolean visible;
	public boolean area;
	public int zoomVisibilityLimit;

	private static final LayoutInfo ERROR = new LayoutInfo();
	private static final LayoutInfo INVISIBLE = new LayoutInfo();
	private static final LayoutInfo DEFAULT = new LayoutInfo();

	private static final LayoutInfo RAILWAY = new LayoutInfo();

	private static final LayoutInfo MOTORWAY = new LayoutInfo();
	private static final LayoutInfo PRIMARY = new LayoutInfo();
	private static final LayoutInfo SECONDARY = new LayoutInfo();
	private static final LayoutInfo TERTIARY = new LayoutInfo();
	private static final LayoutInfo RESIDENTIAL = TERTIARY;
	private static final LayoutInfo UNCLASSIFIED = TERTIARY;
	private static final LayoutInfo SERVICE = TERTIARY;
	private static final LayoutInfo FOOTWAY = new LayoutInfo();
	private static final LayoutInfo CYCLEWAY = FOOTWAY;
	private static final LayoutInfo UNSURFACED = new LayoutInfo();
	private static final LayoutInfo CONSTRUCTION = new LayoutInfo();

	private static final LayoutInfo LAND = new LayoutInfo();
	private static final LayoutInfo FOREST = new LayoutInfo();
	private static final LayoutInfo WATER_WAY = new LayoutInfo();
	private static final LayoutInfo WATER_AREA = new LayoutInfo();

	static {
		DEFAULT.zOrder = 50;
		DEFAULT.fgColor = Color.MAGENTA;
		DEFAULT.minWidth = 1.0;
		DEFAULT.maxWidth = 10.0;
		DEFAULT.visible = true;

		RAILWAY.zOrder = 90;
		RAILWAY.fgColor = Color.DARK_GRAY;
		RAILWAY.minWidth = 1.0;
		RAILWAY.maxWidth = 5.0;
		RAILWAY.visible = true;

		MOTORWAY.zOrder = 200;
		MOTORWAY.fgColor =  new Color(40, 118, 227);
		MOTORWAY.bgColor = MOTORWAY.fgColor.darker();
		MOTORWAY.minWidth = 1.5;
		MOTORWAY.maxWidth = 20.0;
		MOTORWAY.visible = true;

		PRIMARY.zOrder = 180;
		PRIMARY.fgColor = new Color(255, 102, 102);
		PRIMARY.bgColor = PRIMARY.fgColor.darker();
		PRIMARY.minWidth = 1.0;
		PRIMARY.maxWidth = 14.0;
		PRIMARY.visible = true;

		SECONDARY.zOrder = 160;
		SECONDARY.fgColor = new Color(244, 181, 110);
		SECONDARY.bgColor = SECONDARY.fgColor.darker();
		SECONDARY.minWidth = 0.75;
		SECONDARY.maxWidth = 12.0;
		SECONDARY.visible = true;
		SECONDARY.zoomVisibilityLimit = 15;

		TERTIARY.zOrder = 150;
		TERTIARY.fgColor = Color.GRAY;
		TERTIARY.bgColor = TERTIARY.fgColor.darker();
		TERTIARY.minWidth = 0.1;
		TERTIARY.maxWidth = 10.0;
		TERTIARY.visible = true;
		TERTIARY.zoomVisibilityLimit = 20;

		FOOTWAY.zOrder = 140;
		FOOTWAY.fgColor = Color.LIGHT_GRAY;
		FOOTWAY.bgColor = TERTIARY.bgColor;
		FOOTWAY.minWidth = 0.1;
		FOOTWAY.maxWidth = 5.0;
		FOOTWAY.visible = true;
		FOOTWAY.zoomVisibilityLimit = 25;

		UNSURFACED.zOrder = 140;
		UNSURFACED.fgColor = new Color(207, 146, 79);
		UNSURFACED.minWidth = 0.1;
		UNSURFACED.maxWidth = 3.0;
		UNSURFACED.visible = true;
		UNSURFACED.zoomVisibilityLimit = 25;

		CONSTRUCTION.zOrder = 140;
		CONSTRUCTION.fgColor = Color.LIGHT_GRAY;
		CONSTRUCTION.minWidth = 0.1;
		CONSTRUCTION.maxWidth = 3.0;
		CONSTRUCTION.visible = true;
		CONSTRUCTION.zoomVisibilityLimit = 25;

		LAND.zOrder = 10;
		LAND.visible = true;
		LAND.area = true;
		LAND.fgColor = new Color(255, 255, 224);

		FOREST.zOrder = 20;
		FOREST.visible = true;
		FOREST.area = true;
		FOREST.fgColor = new Color(113, 184, 91);

		WATER_WAY.zOrder = 30;
		WATER_WAY.fgColor = new Color(113, 187, 232);
		WATER_WAY.minWidth = 1.5;
		WATER_WAY.maxWidth = 30.0;
		WATER_WAY.visible = true;

		WATER_AREA.zOrder = 30;
		WATER_AREA.fgColor = WATER_WAY.fgColor;
		WATER_AREA.visible = true;
		WATER_AREA.area = true;

		ERROR.zOrder = 300;
		ERROR.visible = true;
		ERROR.minWidth = 1.0;
		ERROR.maxWidth = 1.0;
		ERROR.fgColor = Color.RED;
		ERROR.zoomVisibilityLimit = 20;
	}

	private LayoutInfo() {
		infoList.add(this);
	}

	public static void updateLayoutInfo(int zoomFactor, double scaleLat) {
		for (LayoutInfo l : infoList) {
			if (!l.area) {
				l.width = Math.max(l.minWidth, l.maxWidth * scaleLat / 1852.0);
				l.visible = l.width >= 0.5
						&& zoomFactor >= l.zoomVisibilityLimit;
				// l.width = l.minWidth + (l.maxWidth - l.minWidth) *
				// zoomFactor;
			}
		}
	}

	private static String getTag(OsmPrimitive o, String key) {
		List<Tag> tagList = o.getTags();
		if (tagList == null) {
			return null;
		}
		for (Tag t : tagList) {
			if (t.key.equals(key)) {
				return t.value;
			}
		}
		return null;
	}

	public static LayoutInfo computeInfo(Way way) {
		String t = getTag(way, "highway");
		if (t != null) {
			if (t.equals("motorway") || t.equals("motorway_link")
					|| t.equals("motorway_trunk")) {
				return MOTORWAY;
			} else if (t.equals("primary") || t.equals("primary_link")
					|| t.equals("trunk") || t.equals("trunk_link")) {
				return PRIMARY;
			} else if (t.equals("secondary") || t.equals("secondary_link")) {
				return SECONDARY;
			} else if (t.equals("tertiary")) {
				return TERTIARY;
			} else if (t.equals("residential") || t.equals("living_street")
					|| t.equals("minor")) {
				return RESIDENTIAL;
			} else if (t.equals("footway") || t.equals("cycleway")
					|| t.equals("pedestrian") || t.equals("steps")
					|| t.equals("track") || t.equals("foot")) {
				return FOOTWAY;
			} else if (t.equals("unsurfaced")) {
				return UNSURFACED;
			} else if (t.equals("construction")) {
				return CONSTRUCTION;
			} else if (t.equals("unclassified")) {
				return UNCLASSIFIED;
			} else if (t.equals("service")) {
				return SERVICE;
			} else {
				System.out.println("Unknown highway tag'" + t + "' in way "
						+ way);
				return DEFAULT;
			}
		}

		t = getTag(way, "waterway");
		if (t != null) {
			return way.isClosed() ? WATER_AREA : WATER_WAY;
		}

		t = getTag(way, "cycleway");
		if (t != null) {
			return CYCLEWAY;
		}

		t = getTag(way, "railway");
		if (t != null) {
			if (t.equals("rail")) {
				return RAILWAY;
			}
		}

		t = getTag(way, "landuse");
		if (t != null) {
			if (t.equals("forest")) {
				return FOREST;
			} else {
				return INVISIBLE;
			}
		}

		t = getTag(way, "amenity");
		if (t != null) {
			return INVISIBLE;
		}

		t = getTag(way, "place");
		if (t != null) {
			return INVISIBLE;
		}

		t = getTag(way, "natural");
		if (t != null) {
			if (t.equals("water")) {
				return way.isClosed() ? WATER_AREA : WATER_WAY;
			} else if (t.equals("wood")) {
				return FOREST;
			} else if (t.equals("land")) {
				return LAND;
			} else {
				return DEFAULT;
			}
		}

		System.out.println("No layout information for " + way);
		System.out.println(way.getTags());
		System.out.println();
		return ERROR;
	}
}
