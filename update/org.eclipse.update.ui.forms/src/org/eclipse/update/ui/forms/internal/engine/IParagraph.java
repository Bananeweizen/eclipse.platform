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
package org.eclipse.update.ui.forms.internal.engine;


import org.eclipse.swt.graphics.GC;
import java.util.Hashtable;

/**
 * @version 	1.0
 * @author
 */
public interface IParagraph {
	public int getIndent();
	public IParagraphSegment [] getSegments();
	public void addSegment(IParagraphSegment segment);
	public boolean getAddVerticalSpace();
	public ITextSegment findSegmentAt(int x, int y);
	public void paint(GC gc, int width, Locator loc, int lineHeight, Hashtable objectTable, IHyperlinkSegment selectedLink);
	public String getAccessibleText();
}
