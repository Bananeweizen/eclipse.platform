package org.eclipse.debug.internal.ui;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.ITextOperationTarget;

public class ConsoleViewerAction extends Action {

	private int fOperationCode= -1;
	private ITextOperationTarget fOperationTarget;

	public ConsoleViewerAction(ConsoleViewer viewer, int operationCode) {
		fOperationCode= operationCode;
		fOperationTarget= viewer.getTextOperationTarget();
		update();
	}

	/**
	 * Updates the enabled state of the action.
	 * Fires a property change if the enabled state changes.
	 * 
	 * @see Action#firePropertyChange(String, Object, Object)
	 */
	public void update() {

		boolean wasEnabled= isEnabled();
		boolean isEnabled= (fOperationTarget != null && fOperationTarget.canDoOperation(fOperationCode));
		setEnabled(isEnabled);

		if (wasEnabled != isEnabled) {
			firePropertyChange(ENABLED, wasEnabled ? Boolean.TRUE : Boolean.FALSE, isEnabled ? Boolean.TRUE : Boolean.FALSE);
		}
	}
	
	/**
	 * @see Action#run()
	 */
	public void run() {
		if (fOperationCode != -1 && fOperationTarget != null) {
			fOperationTarget.doOperation(fOperationCode);
		}
	}
	
	protected void configureAction(String text, String toolTipText, String description) {
		setText(text);
		setToolTipText(toolTipText);
		setDescription(description);
	}
}

