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
package org.eclipse.ui.forms;

import org.eclipse.jface.viewers.ISelection;

/**
 * Form parts can implement this interface if they want to be notified when
 * another part on the same form changes selection state.
 * 
 * @see IFormPart
 */
public interface IPartSelectionListener {
	/**
	 * Called when the provided part has changed selection state.
	 * 
	 * @param part
	 *            the selection source
	 * @param selection
	 *            the new selection
	 */
	public void selectionChanged(IFormPart part, ISelection selection);
}
