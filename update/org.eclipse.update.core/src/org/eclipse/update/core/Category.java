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
package org.eclipse.update.core;

import org.eclipse.update.core.model.CategoryModel;

/**
 * Convenience implementation of feature category definition.
 * <p>
 * This class may be instantiated or subclassed by clients.
 * </p> 
 * @see org.eclipse.update.core.ICategory
 * @see org.eclipse.update.core.model.CategoryModel
 * @since 2.0
 */
public class Category extends CategoryModel implements ICategory {

	/**
	 * Default Constructor
	 */
	public Category() {
	}

	/**
	 * Constructor
	 */
	public Category(String name, String label) {
		setName(name);
		setLabel(label);
	}

	/**
	 * Retrieve the detailed category description
	 * @see ICategory#getDescription()
	 */
	public IURLEntry getDescription() {
		return (IURLEntry) getDescriptionModel();
	}
}
