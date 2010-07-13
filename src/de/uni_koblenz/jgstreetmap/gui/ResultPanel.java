package de.uni_koblenz.jgstreetmap.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

public class ResultPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private JTextPane textPane;
	private SimpleAttributeSet defaultAttributeSet;

	public ResultPanel() {
		setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		textPane = new JTextPane() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean getScrollableTracksViewportWidth() {
				return false;
			}
		};
		setLayout(new BorderLayout());
		JScrollPane scp = new JScrollPane(textPane);
		scp.setBorder(BorderFactory.createTitledBorder("Results"));
		scp.setMinimumSize(new Dimension(200, 150));
		scp.setPreferredSize(new Dimension(200, 150));
		scp
				.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		// textPane.setEditable(false);
		add(scp, BorderLayout.CENTER);
		add(new JLabel(" "), BorderLayout.SOUTH);

		defaultAttributeSet = new SimpleAttributeSet();
		StyleConstants.setFontFamily(defaultAttributeSet, "Monospaced");
		StyleConstants.setFontSize(defaultAttributeSet, 12);
		StyleConstants.setForeground(defaultAttributeSet, Color.BLACK);

	}

	public Document getTextDocument() {
		return textPane.getDocument();
	}

	public void println() {
		print("\n");
	}

	public void println(String str) {
		print(str + "\n");
	}

	public void print(String str) {
		try {
			getTextDocument().insertString(getTextDocument().getLength(), str,
					null);
			textPane.setCaretPosition(getTextDocument().getLength());
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void printLabel(String str, Color backgroundColor) {
		JLabel l = new JLabel(str);
		l.setForeground(Color.BLACK);
		l.setOpaque(true);
		l.setBackground(backgroundColor);
		textPane.insertComponent(l);
		println();
	}

	public void clear() {
		try {
			getTextDocument().remove(0, getTextDocument().getLength());
			textPane.setCaretPosition(0);
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
