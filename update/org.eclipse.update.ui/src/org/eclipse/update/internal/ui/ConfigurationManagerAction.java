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
package org.eclipse.update.internal.ui;

import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.update.internal.ui.UpdateUI;

/**
 * Insert the type's description here.
 * @see IWorkbenchWindowActionDelegate
 */
public class ConfigurationManagerAction implements IWorkbenchWindowActionDelegate {

	IWorkbenchWindow window;
	ConfigurationManagerWindow openedWindow;
	/**
	 * The constructor.
	 */
	public ConfigurationManagerAction() {
	}

	/**
	 * Insert the method's description here.
	 */
	public void run(IAction action) {
		BusyIndicator
			.showWhile(window.getShell().getDisplay(), new Runnable() {
			public void run() {
				openConfigurationManager();
			}
		});
	}

	private void openConfigurationManager() {
		if (openedWindow!=null) {
			restoreConfigurationWindow();
			return;
		}
		ConfigurationManagerWindow cwindow = new ConfigurationManagerWindow(window.getShell());
		cwindow.create();
		cwindow.getShell().setText(UpdateUI.getString("ConfigurationManagerAction.title")); //$NON-NLS-1$
		cwindow.getShell().setSize(800, 600);
		cwindow.getShell().addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				openedWindow = null;
			}
		});
		cwindow.updateActionBars();
		openedWindow = cwindow;
		cwindow.open();
	}
	
	private void restoreConfigurationWindow() {
		Shell shell = openedWindow.getShell();
		if (shell.getMinimized())
			shell.setMinimized(false);
		shell.open();
	}

	/**
	 * Insert the method's description here.
	 */
	public void selectionChanged(IAction action, ISelection selection) {
	}

	/**
	 * Insert the method's description here.
	 * @see IWorkbenchWindowActionDelegate#dispose
	 */
	public void dispose() {
	}

	/**
	 * Insert the method's description here.
	 * @see IWorkbenchWindowActionDelegate#init
	 */
	public void init(IWorkbenchWindow window) {
		this.window = window;
	}
}