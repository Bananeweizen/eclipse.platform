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
package org.eclipse.update.ui.forms.internal.richtext;

import java.util.Hashtable;

import org.eclipse.swt.graphics.*;
import org.eclipse.update.ui.forms.internal.HyperlinkSettings;
import org.eclipse.update.ui.forms.richtext.*;

/**
 * @version 	1.0
 * @author
 */
public class HyperlinkSegment
	extends TextSegment
	implements IRichTextHyperlink {
	private String actionId;
	private String arg;
	private HyperlinkSettings settings;
	
	public HyperlinkSegment(String text, HyperlinkSettings settings, String fontId) {
		super(text, fontId);
		this.settings = settings;
		underline = settings.getHyperlinkUnderlineMode()==HyperlinkSettings.UNDERLINE_ALWAYS;
	}
	
	/*
	 * @see IHyperlinkSegment#getListener(Hashtable)
	 */
	public RichTextHyperlinkAction getAction(Hashtable objectTable) {
		if (actionId==null) return null;
		Object obj = objectTable.get(actionId);
		if (obj==null) return null;
		if (obj instanceof RichTextHyperlinkAction) return (RichTextHyperlinkAction)obj;
		return null;
	}
	
	/*
	 * @see IObjectReference#getObjectId()
	 */
	public String getObjectId() {
		return actionId;
	}
	
	void setActionId(String id) {
		this.actionId = id;
	}
	public void paint(GC gc, int width, Locator locator, Hashtable objectTable, boolean selected) {
		setColor(settings.getForeground());
		super.paint(gc, width, locator, objectTable, selected);
	}
	
	public void repaint(GC gc, boolean hover) {
		FontMetrics fm = gc.getFontMetrics();
		int lineHeight = fm.getHeight();
		int descent = fm.getDescent();
		boolean rolloverMode = settings.getHyperlinkUnderlineMode()==HyperlinkSettings.UNDERLINE_ROLLOVER;
		for (int i=0; i<areaRectangles.size(); i++) {
			AreaRectangle areaRectangle = (AreaRectangle)areaRectangles.get(i);
			Rectangle rect = areaRectangle.rect;
			String text = areaRectangle.getText();
			Point extent = gc.textExtent(text);
			int textX = rect.x + 1;
			gc.drawString(text, textX, rect.y, true);
			if (underline || hover || rolloverMode) {
				int lineY = rect.y + lineHeight - descent + 1;
				Color saved=null;
				if (rolloverMode && !hover) {
					saved = gc.getForeground();
					gc.setForeground(gc.getBackground());
				}
				gc.drawLine(textX, lineY, textX+extent.x, lineY);
				if (saved!=null)
					gc.setForeground(saved);
			}
		}
	}
	/**
	 * @return
	 */
	public String getArg() {
		return arg;
	}

	/**
	 * @param string
	 */
	public void setArg(String string) {
		arg = string;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.update.ui.forms.richtext.IRichTextHyperlink#getHref()
	 */
	public String getHref() {
		return getObjectId();
	}

}
