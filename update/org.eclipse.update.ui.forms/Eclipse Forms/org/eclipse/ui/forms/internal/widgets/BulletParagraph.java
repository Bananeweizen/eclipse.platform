/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.forms.internal.widgets;

import java.util.Hashtable;

import org.eclipse.swt.graphics.*;

public class BulletParagraph extends Paragraph {
	public static final int CIRCLE = 1;
	public static final int TEXT = 2;
	public static final int IMAGE = 3;
	private int style = CIRCLE;
	private String text;
	private int CIRCLE_DIAM = 5;
	private int SPACING = 10;
	private int indent = -1;
	/**
	 * Constructor for BulletParagraph.
	 * @param addVerticalSpace
	 */
	public BulletParagraph(boolean addVerticalSpace) {
		super(addVerticalSpace);
	}

	public int getIndent() {
		if (indent != -1)
			return indent;
		switch (style) {
			case CIRCLE :
				return CIRCLE_DIAM + SPACING;
		}
		return 20;
	}

	/*
	 * @see IBulletParagraph#getBulletStyle()
	 */
	public int getBulletStyle() {
		return style;
	}

	public void setBulletStyle(int style) {
		this.style = style;
	}

	public void setBulletText(String text) {
		this.text = text;
	}

	public void setIndent(int indent) {
		this.indent = indent;
	}

	/*
	 * @see IBulletParagraph#getBulletText()
	 */
	public String getBulletText() {
		return text;
	}

	public void paint(
		GC gc,
		int width,
		Locator loc,
		int lineHeight,
		Hashtable objectTable,
		HyperlinkSegment selectedLink) {
		paintBullet(gc, loc, lineHeight, objectTable);
		super.paint(gc, width, loc, lineHeight, objectTable, selectedLink);
	}

	public void paintBullet(
		GC gc,
		Locator loc,
		int lineHeight,
		Hashtable objectTable) {
		int x = loc.x - getIndent();
		if (style == CIRCLE) {
			int y = loc.y + lineHeight / 2 - CIRCLE_DIAM / 2;
			Color bg = gc.getBackground();
			Color fg = gc.getForeground();
			gc.setBackground(fg);
			gc.fillRectangle(x, y + 1, 5, 3);
			gc.fillRectangle(x + 1, y, 3, 5);
			gc.setBackground(bg);
		} else if (style == TEXT && text != null) {
			gc.drawText(text, x, loc.y);
		} else if (style == IMAGE && text != null) {
			Image image = (Image) objectTable.get(text);
			if (image != null) {
				int y = loc.y + lineHeight / 2 - image.getBounds().height / 2;
				gc.drawImage(image, x, y);
			}
		}
	}
}