/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.update.ui.forms.internal.engine;

import org.eclipse.swt.graphics.*;
import org.eclipse.jface.resource.JFaceResources;
import java.util.Hashtable;
import org.eclipse.swt.SWT;
import java.text.BreakIterator;
import java.util.Vector;

/**
 * @version 	1.0
 * @author
 */
public class TextSegment extends ParagraphSegment implements ITextSegment {
	private Color color;
	private String fontId;
	private String text;
	protected boolean underline;
	private boolean wrapAllowed = true;
	private Vector areaRectangles = new Vector();

	public TextSegment(String text, String fontId) {
		this.text = cleanup(text);
		this.fontId = fontId;
	}

	private String cleanup(String text) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == '\n' || c == '\r' || c=='\f') {
				if (i > 0)
					buf.append(' ');
			} else
				buf.append(c);
		}
		return buf.toString();
	}

	public void setWordWrapAllowed(boolean value) {
		wrapAllowed = value;
	}

	public boolean isWordWrapAllowed() {
		return wrapAllowed;
	}

	public Color getColor() {
		return color;
	}

	public Font getFont() {
		if (fontId == null)
			return JFaceResources.getDefaultFont();
		else
			return JFaceResources.getFontRegistry().get(fontId);
	}

	public String getText() {
		return text;
	}

	void setText(String text) {
		this.text = cleanup(text);
	}

	void setColor(Color color) {
		this.color = color;
	}

	void setFontId(String fontId) {
		this.fontId = fontId;
	}

	public boolean contains(int x, int y) {
		for (int i = 0; i < areaRectangles.size(); i++) {
			Rectangle ar = (Rectangle) areaRectangles.get(i);
			if (ar.contains(x, y))
				return true;
		}
		return false;
	}

	public void advanceLocator(
		GC gc,
		int wHint,
		Locator locator,
		Hashtable objectTable) {
		Font oldFont = null;
		if (fontId != null) {
			oldFont = gc.getFont();
			gc.setFont(getFont());
		}
		FontMetrics fm = gc.getFontMetrics();
		int lineHeight = fm.getHeight();

		if (wHint == SWT.DEFAULT || !wrapAllowed) {
			Point extent = gc.textExtent(text);

			if (locator.x + extent.x > wHint) {
				// new line
				locator.x = 0;
				locator.y += locator.rowHeight;
				locator.rowHeight = 0;
			}
			locator.x += extent.x;
			locator.width = extent.x;
			locator.height = extent.y;
			locator.rowHeight = Math.max(locator.rowHeight, extent.y);
			return;
		}

		BreakIterator wb = BreakIterator.getLineInstance();
		wb.setText(text);

		int saved = 0;
		int last = 0;

		int width = 0;

		Point lastExtent = null;

		for (int loc = wb.first(); loc != BreakIterator.DONE; loc = wb.next()) {
			String word = text.substring(saved, loc);
			Point extent = gc.textExtent(word);

			if (locator.x + extent.x > wHint) {
				// overflow
				String savedWord = text.substring(saved, last);
				if (lastExtent==null)
				   lastExtent = gc.textExtent(savedWord);
				int lineWidth = locator.x + lastExtent.x;

				saved = last;
				locator.rowHeight = Math.max(locator.rowHeight, lastExtent.y);
				locator.x = 0;
				locator.y += locator.rowHeight;
				locator.rowHeight = 0;
				width = Math.max(width, lineWidth);
			}
			last = loc;
			lastExtent = extent;
		}
		String lastString = text.substring(saved, last);
		Point extent = gc.textExtent(lastString);
		locator.x += extent.x;
		locator.width = width;
		locator.height = lineHeight;
		locator.rowHeight = Math.max(locator.rowHeight, extent.y);
		if (oldFont != null) {
			gc.setFont(oldFont);
		}
	}

	public void paint(
		GC gc,
		int width,
		Locator locator,
		Hashtable objectTable,
		boolean selected) {
		Font oldFont = null;
		Color oldColor = null;

		areaRectangles.clear();

		if (fontId != null) {
			oldFont = gc.getFont();
			gc.setFont(getFont());
		}
		if (color != null) {
			oldColor = gc.getForeground();
			gc.setForeground(color);
		}
		FontMetrics fm = gc.getFontMetrics();
		int lineHeight = fm.getHeight();
		int descent = fm.getDescent();

		if (!wrapAllowed) {
			Point extent = gc.textExtent(text);

			if (locator.x + extent.x > width) {
				// new line
				locator.x = locator.marginWidth;
				locator.y += locator.rowHeight;
				locator.rowHeight = 0;
			}
			gc.drawString(text, locator.x, locator.y);
			if (underline) {
				int lineY = locator.y + lineHeight - descent + 1;
				gc.drawLine(locator.x, lineY, locator.x + extent.x, lineY);
			}
			Rectangle br =
				new Rectangle(locator.x - 1, locator.y, extent.x + 2, lineHeight - descent + 3);
			areaRectangles.add(br);
			if (selected) {
				if (color != null)
					gc.setForeground(oldColor);
				gc.drawFocus(br.x, br.y, br.width, br.height);
			}

			locator.x += extent.x;
			locator.width = extent.x;
			locator.height = lineHeight;
			locator.rowHeight = Math.max(locator.rowHeight, extent.y);
			if (oldFont != null) {
				gc.setFont(oldFont);
			}
			if (oldColor != null) {
				gc.setForeground(oldColor);
			}
			return;
		}

		BreakIterator wb = BreakIterator.getLineInstance();
		wb.setText(text);

		int saved = 0;
		int last = 0;

		for (int loc = wb.first(); loc != BreakIterator.DONE; loc = wb.next()) {
			if (loc == 0)
				continue;
			String word = text.substring(saved, loc);
			Point extent = gc.textExtent(word);

			if (locator.x + extent.x > width) {
				// overflow
				String prevLine = text.substring(saved, last);
				gc.drawString(prevLine, locator.x, locator.y, true);
				Point prevExtent = gc.textExtent(prevLine);

				if (underline) {
					int lineY = locator.y + lineHeight - descent + 1;
					gc.drawLine(locator.x, lineY, locator.x + prevExtent.x, lineY);
				}
				Rectangle br =
					new Rectangle(
						locator.x - 1,
						locator.y,
						prevExtent.x + 2,
						lineHeight - descent + 3);
				if (selected) {
					if (color != null)
						gc.setForeground(oldColor);
					gc.drawFocus(br.x, br.y, br.width, br.height);
					if (color != null)
						gc.setForeground(color);
				}
				areaRectangles.add(br);
				locator.rowHeight = Math.max(locator.rowHeight, prevExtent.y);
				locator.x = locator.marginWidth;
				locator.y += locator.rowHeight;
				locator.rowHeight = 0;
				saved = last;
			}
			last = loc;
		}
		// paint the last line
		String lastLine = text.substring(saved, last);
		gc.drawString(lastLine, locator.x, locator.y, true);
		Point lastExtent = gc.textExtent(lastLine);
		Rectangle br =
			new Rectangle(
				locator.x - 1,
				locator.y,
				lastExtent.x + 2,
				lineHeight - descent + 3);
		areaRectangles.add(br);
		if (underline) {
			int lineY = locator.y + lineHeight - descent + 1;
			gc.drawLine(locator.x, lineY, locator.x + lastExtent.x, lineY);
		}
		if (selected) {
			if (color != null)
				gc.setForeground(oldColor);
			gc.drawFocus(br.x, br.y, br.width, br.height);
		}
		locator.x += lastExtent.x;
		locator.rowHeight = Math.max(locator.rowHeight, lastExtent.y);
		if (oldFont != null) {
			gc.setFont(oldFont);
		}
		if (oldColor != null) {
			gc.setForeground(oldColor);
		}
	}
}