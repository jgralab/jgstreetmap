package de.uni_koblenz.jgstreetmap.importer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class MinimalSAXParser {
	private enum State {
		INIT, SKIP_TO_GT, START_ELEMENT, END_ELEMENT, ATTR_NAME, BEFORE_ATTR_VALUE, ATTR_VALUE
	};

	private BufferedReader rdr;

	private int la, in;

	void getNext() throws SAXException {
		in = la;
		try {
			la = rdr.read();
		} catch (IOException e) {
			la = -1;
		}
	}

	public void parse(String uri, ContentHandler handler) throws SAXException {
		StringBuffer elementName = new StringBuffer();
		StringBuffer attrName = new StringBuffer();
		StringBuffer attrValue = new StringBuffer();
		StringBuffer chars = new StringBuffer();
		AttributesImpl attrs = null;

		try {
			rdr = new BufferedReader(new InputStreamReader(new FileInputStream(
					uri), "UTF-8"));
		} catch (FileNotFoundException e) {
			throw new SAXException("Can't parse document", e);
		} catch (UnsupportedEncodingException e) {
			throw new SAXException("Can't parse document", e);
		}
		getNext();
		if (la < 0) {
			return;
		}
		handler.startDocument();
		State state = State.INIT;
		State oldState = state;
		while (in >= 0) {
			if (oldState != state) {
				if (chars.length() > 0) {
					int p;
					while ((p = chars.indexOf("&lt;")) >= 0) {
						chars.replace(p, p + 4, "<");
					}
					while ((p = chars.indexOf("&gt;")) >= 0) {
						chars.replace(p, p + 4, ">");
					}
					while ((p = chars.indexOf("&apos;")) >= 0) {
						chars.replace(p, p + 6, "'");
					}
					while ((p = chars.indexOf("&quot;")) >= 0) {
						chars.replace(p, p + 6, "\"");
					}
					while ((p = chars.indexOf("&amp;")) >= 0) {
						chars.replace(p, p + 5, "&");
					}
					char[] c = chars.toString().toCharArray();
					handler.characters(c, 0, c.length);
					chars.setLength(0);
				}
				oldState = state;
			}
			getNext();
			switch (state) {
			case INIT:
				if (in == '<' && (la == '?' || la == '!')) {
					state = State.SKIP_TO_GT;
				} else if (in == '<' && Character.isLetter((char) la)) {
					state = State.START_ELEMENT;
					elementName.setLength(0);
				} else if (in == '<' && la == '/') {
					state = State.END_ELEMENT;
					getNext();
					elementName.setLength(0);
				} else {
					chars.append((char) in);
				}
				break;

			case SKIP_TO_GT:
				if (in == '>') {
					state = State.INIT;
				}
				break;

			case START_ELEMENT:
				if (in == '/') {
					handler.startElement(uri, "", elementName.toString(), null);
					handler.endElement(uri, "", elementName.toString());
					state = State.SKIP_TO_GT;
				} else if (in == '>') {
					handler.startElement(uri, "", elementName.toString(), null);
					state = State.INIT;
				} else if (Character.isLetter((char) in)) {
					elementName.append((char) in);
				} else {
					attrs = new AttributesImpl();
					attrName.setLength(0);
					state = State.ATTR_NAME;
				}
				break;

			case ATTR_NAME:
				if (in == '/') {
					handler
							.startElement(uri, "", elementName.toString(),
									attrs);
					handler.endElement(uri, "", elementName.toString());
					state = State.SKIP_TO_GT;
				} else if (in == '>') {
					handler
							.startElement(uri, "", elementName.toString(),
									attrs);
					state = State.INIT;
				} else if (Character.isLetter((char) in)) {
					attrName.append((char) in);
				} else if (in == '=') {
					state = State.BEFORE_ATTR_VALUE;
				}
				break;

			case BEFORE_ATTR_VALUE:
				if (in == '"') {
					attrValue.setLength(0);
					state = State.ATTR_VALUE;
				}
				break;

			case ATTR_VALUE:
				if (in == '"') {
					attrs.addAttribute("", "", attrName.toString(), "",
							attrValue.toString());
					attrName.setLength(0);
					state = State.ATTR_NAME;
				} else {
					attrValue.append((char) in);
				}
				break;

			case END_ELEMENT:
				if (in == '>') {
					handler.endElement(uri, "", elementName.toString());
					state = State.INIT;
				} else {
					elementName.append((char) in);
				}
				break;
			}
		}
		handler.endDocument();
	}
}
