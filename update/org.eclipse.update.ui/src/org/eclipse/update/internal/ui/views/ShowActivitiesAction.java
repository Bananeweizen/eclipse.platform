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
package org.eclipse.update.internal.ui.views;

import org.eclipse.jface.action.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.internal.ui.wizards.*;

public class ShowActivitiesAction extends Action {
	Shell shell;
	
	public ShowActivitiesAction(Shell parentShell, String text) {
		super(text);
		this.shell = parentShell;
	}
	
	public void run() {
		ShowActivitiesDialog dialog = new ShowActivitiesDialog(shell);
		dialog.create();
		dialog.getShell().setSize(500,500);
		dialog.open();
	}
}
