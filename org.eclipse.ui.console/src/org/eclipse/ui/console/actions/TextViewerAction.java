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
package org.eclipse.ui.console.actions;


import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.ui.texteditor.IUpdate;

public class TextViewerAction extends Action implements IUpdate {

	private int fOperationCode= -1;
	private ITextOperationTarget fOperationTarget;

	public TextViewerAction(ITextViewer viewer, int operationCode) {
		fOperationCode= operationCode;
		fOperationTarget= viewer.getTextOperationTarget();
		update();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.IUpdate#update()
	 * 
	 * Updates the enabled state of the action.
	 * Fires a property change if the enabled state changes.
	 * 
	 * @see org.eclipse.jface.action.Action#firePropertyChange(String, Object, Object)
	 */
	public void update() {

		boolean wasEnabled= isEnabled();
		boolean isEnabled= (fOperationTarget != null && fOperationTarget.canDoOperation(fOperationCode));
		setEnabled(isEnabled);

		if (wasEnabled != isEnabled) {
			firePropertyChange(ENABLED, wasEnabled ? Boolean.TRUE : Boolean.FALSE, isEnabled ? Boolean.TRUE : Boolean.FALSE);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	public void run() {
		if (fOperationCode != -1 && fOperationTarget != null) {
			fOperationTarget.doOperation(fOperationCode);
		}
	}
	
	public void configureAction(String text, String toolTipText, String description) {
		setText(text);
		setToolTipText(toolTipText);
		setDescription(description);
	}
}