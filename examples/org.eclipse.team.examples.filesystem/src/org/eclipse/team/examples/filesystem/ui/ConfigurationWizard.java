/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.examples.filesystem.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.examples.filesystem.FileSystemPlugin;
import org.eclipse.team.examples.filesystem.FileSystemProvider;
import org.eclipse.team.examples.filesystem.Policy;
import org.eclipse.team.ui.IConfigurationWizard;
import org.eclipse.ui.IWorkbench;

/**
 * The file system configuration wizard used when associating a project
 * the the file system provider. It is registered as a Team configuration wizard
 * in the plugin.xml and is invoked when a user chooses to create a File System
 * Repository Provider. One invoked, this wizard makes use of the <code>FileSystemMainPage</code>
 * in order to obtain a target location on disk.
 */
public class ConfigurationWizard extends Wizard implements IConfigurationWizard {
	
	IProject project;
	
	FileSystemMainPage mainPage;
	
	public ConfigurationWizard() {
		// retrieve the remembered dialog settings
		IDialogSettings workbenchSettings = FileSystemPlugin.getPlugin().getDialogSettings();
		IDialogSettings section = workbenchSettings.getSection("ProviderExamplesWizard"); //$NON-NLS-1$
		if (section == null) {
			section = workbenchSettings.addNewSection("ProviderExamplesWizard"); //$NON-NLS-1$
		}
		setDialogSettings(section);
	}

	/**
	 * Remember the project so we can map it on finish
	 * 
	 * @see org.eclipse.team.ui.IConfigurationWizard#init(IWorkbench, IProject)
	 */
	public void init(IWorkbench workbench, IProject project) {
		this.project = project;
	}
	
	public void addPages() {
		mainPage = new FileSystemMainPage(
			"FileSystemMainPage", //$NON-NLS-1$
			Policy.bind("ConfigurationWizard.name"),  //$NON-NLS-1$
			Policy.bind("ConfigurationWizard.description"),  //$NON-NLS-1$
			null);
		addPage(mainPage);
	}
	
	/*
	 * Using the information entered in the main page set the provider for
	 * the given project.
	 */
	public boolean performFinish() {
		mainPage.finish(null);
		try {
			// Map the provider and set the location
			RepositoryProvider.map(project, FileSystemPlugin.PROVIDER_ID);
			FileSystemProvider provider = (FileSystemProvider) RepositoryProvider.getProvider(project);
			provider.setTargetLocation(mainPage.getLocation());
		} catch (TeamException e) {
			ErrorDialog.openError(
				getShell(),
				Policy.bind("ConfigurationWizard.errorMapping"), //$NON-NLS-1$
				Policy.bind("ConfigurationWizard.error"), //$NON-NLS-1$
				e.getStatus());
			return false;
		}
		return true;
	}

}
