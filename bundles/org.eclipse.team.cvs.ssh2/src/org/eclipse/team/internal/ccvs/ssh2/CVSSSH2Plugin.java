/* -*-mode:java; c-basic-offset:2; -*- */
/*******************************************************************************
 * Copyright (c) 2003, Atsuhiko Yamanaka, JCraft,Inc. and others. All rights
 * reserved. This program and the accompanying materials are made available
 * under the terms of the Common Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: Atsuhiko Yamanaka, JCraft,Inc. - initial API and
 * implementation.
 ******************************************************************************/
package org.eclipse.team.internal.ccvs.ssh2;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class CVSSSH2Plugin extends AbstractUIPlugin {

	public static String ID = "org.eclipse.team.cvs.ssh2"; //$NON-NLS-1$
	private static CVSSSH2Plugin plugin;

	public CVSSSH2Plugin(IPluginDescriptor d) {
		super(d);
		plugin = this;
	}

	public static CVSSSH2Plugin getPlugin() {
		return plugin;
	}

	public void shutdown() throws org.eclipse.core.runtime.CoreException {
		JSchSession.shutdown();
		super.shutdown();
	}

	public static CVSSSH2Plugin getDefault() {
		return plugin;
	}

	public static IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}

	protected void initializeDefaultPreferences(IPreferenceStore store) {
		CVSSSH2PreferencePage.initDefaults(store);
	}
	
	public void startup() throws CoreException {
		Policy.localize("org.eclipse.team.ccvs.ssh2.messages"); //$NON-NLS-1$
		super.startup();		
	}
}