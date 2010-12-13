package de.uni_koblenz.jgstreetmap.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
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

	private JCheckBox showTownsButton;

	private JComboBox algorithmComboBox;

	private JTextField searchTextField;

	private RoutingAlgorithms[] routingAlgorithms = {
			RoutingAlgorithms.Dijkstra, RoutingAlgorithms.AStar };

	// private OsmGraph graph;

	public MapFrame(AnnotatedOsmGraph graph, boolean withAntiAliasing) {
		super("jgStreetMap");
		// this.graph = graph;
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		getContentPane().setLayout(new BorderLayout());
		resultPanel = new ResultPanel();
		mapPanel = new MapPanel(graph, resultPanel, withAntiAliasing);

		getContentPane().add(mapPanel, BorderLayout.CENTER);
		getContentPane().add(resultPanel, BorderLayout.EAST);

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
		buttonPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
		getContentPane().add(buttonPanel, BorderLayout.WEST);

		// ---------------------
		// VISIBLE DETAILS PANEL
		// ---------------------
		JPanel pnl = new JPanel();
		pnl.setAlignmentX(0.5f);
		pnl.setLayout(new BoxLayout(pnl, BoxLayout.Y_AXIS));
		pnl.setBorder(BorderFactory.createTitledBorder("Visible details"));
		buttonPanel.add(pnl);

		showMapButton = new JCheckBox("Map", mapPanel.isShowingMap());
		pnl.add(showMapButton);
		showMapButton.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				mapPanel.setShowMap(showMapButton.isSelected());
			}

		});

		showNatureButton = new JCheckBox("Streets only", mapPanel
				.isShowingStreetsOnly());
		pnl.add(showNatureButton);
		showNatureButton.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				mapPanel.setShowStreetsOnly(showNatureButton.isSelected());
			}
		});

		showGraphButton = new JCheckBox("Graph", mapPanel.isShowingGraph());
		pnl.add(showGraphButton);
		showGraphButton.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				mapPanel.setShowGraph(showGraphButton.isSelected());
			}
		});

		showWaysInGraphButton = new JCheckBox("OSM Ways", mapPanel
				.isShowingWaysInGraph());
		pnl.add(showWaysInGraphButton);
		showWaysInGraphButton.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				mapPanel.setShowWaysInGraph(showWaysInGraphButton.isSelected());
			}
		});

		showWayIdsButton = new JCheckBox("Way names", mapPanel
				.isShowingWayIds());
		pnl.add(showWayIdsButton);
		showWayIdsButton.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				mapPanel.setShowWayIds(showWayIdsButton.isSelected());
			}
		});

		showNodeIdsButton = new JCheckBox("Node IDs", mapPanel
				.isShowingNodeIds());
		pnl.add(showNodeIdsButton);
		showNodeIdsButton.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				mapPanel.setShowNodeIds(showNodeIdsButton.isSelected());
			}
		});

		showLengthButton = new JCheckBox("Length", mapPanel.isShowingLength());
		pnl.add(showLengthButton);
		showLengthButton.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				mapPanel.setShowLength(showLengthButton.isSelected());
			}
		});

		showRoutesButton = new JCheckBox("Routes", mapPanel.isShowingRoutes());
		pnl.add(showRoutesButton);
		showRoutesButton.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				mapPanel.setShowRoutes(showRoutesButton.isSelected());
			}
		});

		showTownsButton = new JCheckBox("Towns", mapPanel.isShowingTowns());
		pnl.add(showTownsButton);
		showTownsButton.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				mapPanel.setShowTowns(showTownsButton.isSelected());
			}
		});

		Dimension d8x8 = new Dimension(8, 8);
		buttonPanel.add(new Box.Filler(d8x8, d8x8, d8x8));

		// ---------------------
		// ROUTING PANEL
		// ---------------------
		pnl = new JPanel();
		pnl.setLayout(new BoxLayout(pnl, BoxLayout.Y_AXIS));
		pnl.setBorder(BorderFactory.createTitledBorder("Routing"));
		buttonPanel.add(pnl);

		algorithmComboBox = new JComboBox(routingAlgorithms);
		pnl.add(algorithmComboBox);
		algorithmComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mapPanel
						.setRoutingAlgorithm((RoutingAlgorithms) algorithmComboBox
								.getSelectedItem());
			}
		});

		startButton = new JButton("Set start");
		startButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		startButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mapPanel.setMouseStartNode();
			}
		});
		pnl.add(startButton);

		buttonPanel.add(new Box.Filler(d8x8, d8x8, d8x8));

		// ---------------------
		// SEARCH/ZOOM PANEL
		// ---------------------
		pnl = new JPanel();
		pnl.setLayout(new BoxLayout(pnl, BoxLayout.Y_AXIS));
		pnl.setBorder(BorderFactory.createTitledBorder("Map"));
		buttonPanel.add(pnl);

		JLabel lbl = new JLabel("Locate town");
		lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
		pnl.add(lbl);

		searchTextField = new JTextField();
		searchTextField.setAlignmentX(Component.CENTER_ALIGNMENT);
		searchTextField
				.setToolTipText("Enter a name of a town and hit return.");
		pnl.add(searchTextField);
		searchTextField.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				mapPanel.centerTown(searchTextField.getText().toLowerCase());
			}
		});

		lbl = new JLabel("Zoom");
		lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
		pnl.add(lbl);

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
		pnl.add(zoomInButton);

		zoomSlider = new JSlider(SwingConstants.VERTICAL);
		zoomSlider.setModel(mapPanel.getZoomLevelModel());
		zoomSlider.setAlignmentX(Component.CENTER_ALIGNMENT);
		pnl.add(zoomSlider);

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
		pnl.add(zoomOutButton);

		zahnButton = new JButton("\"Zahn\"");
		zahnButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		zahnButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mapPanel.setDefaultPosition();
			}
		});
		pnl.add(zahnButton);

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
		pnl.add(printButton);

		pack();
		setVisible(true);
		validate();
	}
}
