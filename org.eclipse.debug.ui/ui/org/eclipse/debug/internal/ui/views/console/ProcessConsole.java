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
package org.eclipse.debug.internal.ui.views.console;

import java.text.MessageFormat;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.console.AbstractConsole;
import org.eclipse.debug.internal.ui.console.IConsoleView;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.part.IPageBookViewPage;

/**
 * A console for a system process
 * <p>
 * Clients may instantiate this class. This class is not intended for
 * sub-classing.
 * </p>
 * @since 3.0
 */
public class ProcessConsole extends AbstractConsole implements IDebugEventSetListener {
	
	private IProcess fProcess = null;

	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.ui.console.IConsole#createPage(org.eclipse.debug.internal.ui.console.IConsoleView)
	 */
	public IPageBookViewPage createPage(IConsoleView view) {
		return new ProcessConsolePage(view, this);
	}

	/**
	 * Computes and returns the image descriptor for this console.
	 * 
	 * @return an image descriptor for this console or <code>null</code>
	 */
	protected ImageDescriptor computeImageDescriptor() {
		ILaunchConfiguration configuration = getProcess().getLaunch().getLaunchConfiguration();
		if (configuration != null) {
			ILaunchConfigurationType type;
			try {
				type = configuration.getType();
				return DebugPluginImages.getImageDescriptor(type.getIdentifier());
			} catch (CoreException e) {
				DebugUIPlugin.log(e);
			}
		}
		return null;
	}

	/**
	 * Computes and returns the current name of this console.
	 * 
	 * @return a name for this console
	 */
	protected String computeName() {	
		ILaunchConfiguration configuration = getProcess().getLaunch().getLaunchConfiguration(); 
		if (configuration != null) {
			if (getProcess().isTerminated()) {
				return MessageFormat.format(ConsoleMessages.getString("ProcessConsole.0"), new String[]{configuration.getName()}); //$NON-NLS-1$
			} else {
				return configuration.getName();
			}
		}
		if (getProcess().isTerminated()) {
			return MessageFormat.format(ConsoleMessages.getString("ProcessConsole.0"), new String[]{getProcess().getLabel()}); //$NON-NLS-1$
		} else {
			return getProcess().getLabel();
		}
	}

	/**
	 * Proxy to a console document
	 */
	public ProcessConsole(IProcess process) {
		super("", null); //$NON-NLS-1$
		fProcess = process;
		setName(computeName());
		setImageDescriptor(computeImageDescriptor());
	}
			
	/**
	 * Returns the process associated with this console.
	 * 
	 * @return the process associated with this console
	 */
	public IProcess getProcess() {
		return fProcess;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.ui.console.AbstractConsole#dispose()
	 */
	protected void dispose() {
		super.dispose();
		DebugPlugin.getDefault().removeDebugEventListener(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.ui.console.AbstractConsole#init()
	 */
	protected void init() {
		super.init();
		DebugPlugin.getDefault().addDebugEventListener(this);
	}
	
	/**
	 * Notify listeners when name changes.
	 * 
	 * @see org.eclipse.debug.core.IDebugEventSetListener#handleDebugEvents(org.eclipse.debug.core.DebugEvent[])
	 */
	public void handleDebugEvents(DebugEvent[] events) {
		for (int i = 0; i < events.length; i++) {
			DebugEvent event = events[i];
			if (event.getSource().equals(getProcess())) {
				Runnable r = new Runnable() {
					public void run() {
						setName(computeName());
					}
				};	
				DebugUIPlugin.getStandardDisplay().asyncExec(r);
			}
		}
	}
}
