package org.eclipse.team.tests.ccvs.ui;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.*;
import org.eclipse.ui.part.*;
import org.eclipse.ui.part.*;
import org.eclipse.swt.SWT;

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