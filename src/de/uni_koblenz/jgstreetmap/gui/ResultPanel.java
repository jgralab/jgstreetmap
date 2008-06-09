package de.uni_koblenz.jgstreetmap.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

public class ResultPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private JTextPane textPane;
	private SimpleAttributeSet defaultAttributeSet;

	public ResultPanel() {
		textPane = new JTextPane();
		setLayout(new BorderLayout());
		JScrollPane scp = new JScrollPane(textPane);
		scp.setMinimumSize(new Dimension(200, 150));
		scp.setPreferredSize(new Dimension(200, 150));
		textPane.setEditable(false);
		add(scp, BorderLayout.CENTER);

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
					defaultAttributeSet);
			textPane.setCaretPosition(getTextDocument().getLength());
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
