/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.update.ui.forms.internal.engine;

/**
 * @version 	1.0
 * @author
 */
public class Locator {
	public int x, y;
	public int width, height;
	public int rowHeight;
	public int marginWidth;
	public int marginHeight;
	
	public void newLine() {
		x = marginWidth;
		y += rowHeight;
		rowHeight = 0;
	}
}
