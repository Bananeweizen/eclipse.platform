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
package org.eclipse.team.tests.ccvs.ui;


import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

/**
 * This perspective is used for testing api.  It defines an initial
 * layout with no parts, just an editor area.
 * 
 * Note: originally borrowed from org.eclipse.jdt.junit.eclipse.util
 */
public class EmptyPerspective implements IPerspectiveFactory {
	
	/**
	 * The perspective id.
	 */
	public static final String PERSP_ID = "org.eclipse.team.tests.ccvs.ui.EmptyPerspective";
	
	/**
	 * Constructs a new Default layout engine.
	 */
	public EmptyPerspective() {
		super();
	}
	
	/**
	 * Defines the initial layout for a perspective.  
	 *
	 * Implementors of this method may add additional views to a
	 * perspective.  The perspective already contains an editor folder
	 * with <code>ID = ILayoutFactory.ID_EDITORS</code>.  Add additional views
	 * to the perspective in reference to the editor folder.
	 *
	 * This method is only called when a new perspective is created.  If
	 * an old perspective is restored from a persistence file then
	 * this method is not called.
	 *
	 * @param factory the factory used to add views to the perspective
	 */
	public void createInitialLayout(IPageLayout layout) {
		//layout.addView( MockViewPart.ID, IPageLayout.BOTTOM, 0.5f, layout.getEditorArea() );
	}
}
