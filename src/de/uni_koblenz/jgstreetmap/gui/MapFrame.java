package de.uni_koblenz.jgstreetmap.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import de.uni_koblenz.jgstreetmap.model.AnnotatedOsmGraph;

public class MapFrame extends JFrame {
	private static final long serialVersionUID = 1L;

	private MapPanel mapPanel;
	private JButton zoomInButton;
	private JButton zoomOutButton;
	private JSlider zoomSlider;
	private JButton zahnButton;

	// private OsmGraph graph;

	public MapFrame(AnnotatedOsmGraph graph) {
		super("jgStreetMap");
		// this.graph = graph;
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new BorderLayout());
		mapPanel = new MapPanel(graph);

		getContentPane().add(mapPanel, BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
		getContentPane().add(buttonPanel, BorderLayout.WEST);

		mapPanel.getZoomLevelModel().addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				System.out
						.println("Zoom=" + mapPanel.getZoomLevelModel().getValue());
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
				mapPanel.getZoomLevelModel().setValue(Math.min(mapPanel.getZoomLevelModel().getValue() + 5,
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
				mapPanel.getZoomLevelModel().setValue(Math.max(mapPanel.getZoomLevelModel().getValue() - 5,
						MapPanel.ZOOM_MIN));
			}
		});
		buttonPanel.add(zoomOutButton);
		buttonPanel.add(new Box.Filler(new Dimension(5, 0),
				new Dimension(5, 50), new Dimension(5, Short.MAX_VALUE)));

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
