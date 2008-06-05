package de.uni_koblenz.jgstreetmap.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.Timer;

import de.uni_koblenz.jgralab.ProgressFunction;

public class SwingProgressFunction implements ProgressFunction, ActionListener {
	private JFrame wnd;
	private JProgressBar pb;
	private String title;
	private String label;
	private long steps;
	private BoundedRangeModel brm;
	private JLabel lbl;
	private long startTime;
	private Timer timer;

	public SwingProgressFunction(String title, String label) {
		this.title = title;
		this.label = label;
	}

	@Override
	public void finished() {
		brm.setValue(brm.getMaximum());
		setTimeText();
		timer = new Timer(5000, this);
		timer.start();
	}

	@Override
	public long getInterval() {
		return brm.getMaximum() > steps ? 1 : steps / brm.getMaximum();
	}

	@Override
	public void init(long steps) {
		this.steps = steps;
		wnd = new JFrame(title);
		wnd.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		wnd.setResizable(false);
		wnd.setLayout(new BorderLayout());
		wnd.setMinimumSize(new Dimension(200, 100));

		pb = new JProgressBar();
		brm = new DefaultBoundedRangeModel();
		pb.setModel(brm);
		lbl = new JLabel("", JLabel.CENTER);

		wnd.getContentPane().add(new JLabel(label, JLabel.CENTER),
				BorderLayout.NORTH);
		wnd.getContentPane().add(pb, BorderLayout.CENTER);
		wnd.getContentPane().add(lbl, BorderLayout.SOUTH);
		wnd.getContentPane().add(new JPanel(), BorderLayout.WEST);
		wnd.getContentPane().add(new JPanel(), BorderLayout.EAST);
		startTime = System.currentTimeMillis();
		setTimeText();
		wnd.pack();
		wnd.setVisible(true);
	}

	private void setTimeText() {
		lbl
				.setText(steps + " elements, elapsed time: "
						+ ((System.currentTimeMillis() - startTime) / 100)
						/ 10.0 + "s");
	}

	@Override
	public void progress(long progress) {
		if (brm.getValue() < brm.getMaximum()) {
			brm.setValue(brm.getValue() + 1);
			setTimeText();
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		timer.stop();
		wnd.dispose();
	}
}
