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
import org.eclipse.swt.graphics.Image;
import java.util.Hashtable;

/**
 * @version 	1.0
 * @author
 */
public interface IImageSegment extends IParagraphSegment, IObjectReference {
	public static final int TOP = 1;
	public static final int MIDDLE = 2;
	public static final int BOTTOM = 3;
	
	public int getVerticalAlignment();

	Image getImage(Hashtable objectTable);
}
