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
package org.eclipse.update.internal.ui.search;

import org.eclipse.swt.widgets.*;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;
import java.util.Map;

public interface ISearchCategory {
/**
 * Returns the unique identifier of this search category.
 */
	public String getId();
/**
 * Accepts the identifier assigned to this category during
 * the registry reading.
 */
	public void setId(String id);
/**
 * Returns an array of queries that need to be executed
 * when search of this category is initiated.
 */
	public ISearchQuery [] getQueries();
/**
 * Creates a control that contains widgets users can use to
 * customize category settings.
 */
	public void createControl(Composite parent, FormWidgetFactory factory);
/**
 * Returns the control that hosts widgets users can use to 
 * customize category settings.
 */	
	public Control getControl();
/**
 * Returns a textual representation of the current search. This
 * text will be used in the UI 
 */
	public String getCurrentSearch();
/**
 * Load the category settings from the provided object.
 */	
	public void load(Map settings, boolean editable);
/**
 * Save the category settings into the provided object.
 */
	public void store(Map settings);
}