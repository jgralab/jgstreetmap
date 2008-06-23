package de.uni_koblenz.jgstreetmap.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import de.uni_koblenz.jgstreetmap.gui.MapPanel.RoutingAlgorithms;
import de.uni_koblenz.jgstreetmap.model.AnnotatedOsmGraph;

public class MapFrame extends JFrame {
	private static final long serialVersionUID = 1L;

	private MapPanel mapPanel;
	private ResultPanel resultPanel;
	private JButton zoomInButton;
	private JButton zoomOutButton;
	private JSlider zoomSlider;
	private JButton zahnButton;
	private JButton startButton;
	private JButton printButton;

	private JCheckBox showMapButton;

	private JCheckBox showNatureButton;

	private JCheckBox showGraphButton;

	private JCheckBox showWaysInGraphButton;

	private JCheckBox showLengthButton;

	private JCheckBox showWayIdsButton;

	private JCheckBox showNodeIdsButton;

	private JCheckBox showRoutesButton;

	private JComboBox algorithmComboBox;

	private RoutingAlgorithms[] routingAlgorithms = {
			RoutingAlgorithms.Dijkstra, RoutingAlgorithms.AStar };

	// private OsmGraph graph;

	public MapFrame(AnnotatedOsmGraph graph) {
		super("jgStreetMap");
		// this.graph = graph;
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new BorderLayout());
		resultPanel = new ResultPanel();
		mapPanel = new MapPanel(graph, resultPanel);

		getContentPane().add(mapPanel, BorderLayout.CENTER);
		getContentPane().add(resultPanel, BorderLayout.EAST);

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
		getContentPane().add(buttonPanel, BorderLayout.WEST);

		JPanel detailPanel = new JPanel();
		getContentPane().add(detailPanel, BorderLayout.SOUTH);

		detailPanel.add(new JLabel("Visible details:"));

		showMapButton = new JCheckBox("Map", mapPanel.isShowingMap());
		detailPanel.add(showMapButton);
		showMapButton.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				mapPanel.setShowMap(showMapButton.isSelected());
			}

		});

		showNatureButton = new JCheckBox("Streets only", mapPanel
				.isShowingStreetsOnly());
		detailPanel.add(showNatureButton);
		showNatureButton.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				mapPanel.setShowStreetsOnly(showNatureButton.isSelected());
			}
		});

		showGraphButton = new JCheckBox("Graph", mapPanel.isShowingGraph());
		detailPanel.add(showGraphButton);
		showGraphButton.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				mapPanel.setShowGraph(showGraphButton.isSelected());
			}
		});

		showWaysInGraphButton = new JCheckBox("OSM Ways", mapPanel
				.isShowingWaysInGraph());
		detailPanel.add(showWaysInGraphButton);
		showWaysInGraphButton.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				mapPanel.setShowWaysInGraph(showWaysInGraphButton.isSelected());
			}
		});

		showWayIdsButton = new JCheckBox("Way names", mapPanel
				.isShowingWayIds());
		detailPanel.add(showWayIdsButton);
		showWayIdsButton.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				mapPanel.setShowWayIds(showWayIdsButton.isSelected());
			}
		});

		showNodeIdsButton = new JCheckBox("Node IDs", mapPanel
				.isShowingNodeIds());
		detailPanel.add(showNodeIdsButton);
		showNodeIdsButton.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				mapPanel.setShowNodeIds(showNodeIdsButton.isSelected());
			}
		});

		showLengthButton = new JCheckBox("Length", mapPanel.isShowingLength());
		detailPanel.add(showLengthButton);
		showLengthButton.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				mapPanel.setShowLength(showLengthButton.isSelected());
			}
		});

		showRoutesButton = new JCheckBox("Routes", mapPanel.isShowingRoutes());
		detailPanel.add(showRoutesButton);
		showRoutesButton.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				mapPanel.setShowRoutes(showRoutesButton.isSelected());
			}
		});

		algorithmComboBox = new JComboBox(routingAlgorithms);
		detailPanel.add(algorithmComboBox);
		algorithmComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mapPanel
						.setRoutingAlgorithm((RoutingAlgorithms) algorithmComboBox
								.getSelectedItem());
			}
		});

		buttonPanel.add(new Box.Filler(new Dimension(5, 0),
				new Dimension(5, 50), new Dimension(5, Short.MAX_VALUE)));

		JLabel lbl = new JLabel("Zoom");
		lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
		buttonPanel.add(lbl);

		zoomInButton = new JButton("+");
		zoomInButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		zoomInButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mapPanel.getZoomLevelModel().setValue(
						Math.min(mapPanel.getZoomLevelModel().getValue() + 5,
								MapPanel.ZOOM_MAX));
			}
		});
		buttonPanel.add(zoomInButton);

		zoomSlider = new JSlider(JSlider.VERTICAL);
		zoomSlider.setModel(mapPanel.getZoomLevelModel());
		zoomSlider.setAlignmentX(Component.CENTER_ALIGNMENT);
		buttonPanel.add(zoomSlider);

		zoomOutButton = new JButton("-");
		zoomOutButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		zoomOutButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mapPanel.getZoomLevelModel().setValue(
						Math.max(mapPanel.getZoomLevelModel().getValue() - 5,
								MapPanel.ZOOM_MIN));
			}
		});
		buttonPanel.add(zoomOutButton);
		buttonPanel.add(new Box.Filler(new Dimension(5, 0),
				new Dimension(5, 50), new Dimension(5, Short.MAX_VALUE)));

		startButton = new JButton("Set start");
		startButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		startButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mapPanel.setMouseStartNode();
			}
		});
		buttonPanel.add(startButton);

		printButton = new JButton("Print");
		printButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		printButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				PrinterJob pj = PrinterJob.getPrinterJob();
				pj.setPrintable(mapPanel);
				if (pj.printDialog()) {
					try {
						pj.print();
					} catch (PrinterException exc) {
						exc.printStackTrace();
					}
				}
			}
		});
		buttonPanel.add(printButton);

		zahnButton = new JButton("\"Zahn\"");
		zahnButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		zahnButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mapPanel.setDefaultPosition();
			}
		});

		buttonPanel.add(zahnButton);
		pack();
		setVisible(true);
	}
}
