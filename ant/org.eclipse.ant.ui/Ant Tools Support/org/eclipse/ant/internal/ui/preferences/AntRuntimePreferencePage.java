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
package org.eclipse.ant.internal.ui.preferences;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.ant.core.AntCorePreferences;
import org.eclipse.ant.core.Property;
import org.eclipse.ant.core.Task;
import org.eclipse.ant.core.Type;
import org.eclipse.ant.internal.ui.model.AntUIPlugin;
import org.eclipse.ant.internal.ui.model.IAntUIHelpContextIds;
import org.eclipse.ant.internal.ui.model.IAntUIPreferenceConstants;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.help.WorkbenchHelp;

/**
 * Ant preference page to set the classpath, tasks, and types and properties.
 */
public class AntRuntimePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	
	private AntClasspathPage classpathPage;
	private AntTasksPage tasksPage;
	private AntTypesPage typesPage;
	private AntPropertiesPage propertiesPage;
	
	/**
	 * Creates the preference page
	 */
	public AntRuntimePreferencePage() {
		setDescription(AntPreferencesMessages.getString("AntPreferencePage.description")); //$NON-NLS-1$
		setPreferenceStore(AntUIPlugin.getDefault().getPreferenceStore());
	}
	
	/* (non-Javadoc)
	 * Method declared on IWorkbenchPreferencePage.
	 */
	public void init(IWorkbench workbench) {
	}
	
	/* (non-Javadoc)
	 * Method declared on PreferencePage.
	 */
	protected Control createContents(Composite parent) {
		WorkbenchHelp.setHelp(getControl(), IAntUIHelpContextIds.ANT_RUNTIME_PREFERENCE_PAGE);
		initializeDialogUnits(parent);

		TabFolder folder = new TabFolder(parent, SWT.NONE);
		folder.setLayout(new TabFolderLayout());	
		folder.setLayoutData(new GridData(GridData.FILL_BOTH));
		folder.setFont(parent.getFont());

		classpathPage = new AntClasspathPage(this);
		classpathPage.createTabItem(folder);
		
		tasksPage = new AntTasksPage(this);
		tasksPage.createTabItem(folder);
		
		typesPage = new AntTypesPage(this);
		typesPage.createTabItem(folder);

		propertiesPage= new AntPropertiesPage(this);
		propertiesPage.createTabItem(folder);
		
		tasksPage.initialize();
		typesPage.initialize();
		classpathPage.initialize();
		propertiesPage.initialize();

		return folder;
	}
	
	/* (non-Javadoc)
	 * Method declared on PreferencePage.
	 */
	protected void performDefaults() {
		super.performDefaults();
		
		AntCorePreferences prefs = AntCorePlugin.getPlugin().getPreferences();
		tasksPage.setInput(prefs.getDefaultTasks());
		typesPage.setInput(prefs.getDefaultTypes());
		classpathPage.performDefaults();
		propertiesPage.performDefaults();
		
		getPreferenceStore().setToDefault(IAntUIPreferenceConstants.ANT_VM_INFORMATION);
	}
	
	/* (non-Javadoc)
	 * Method declared on PreferencePage.
	 */
	public boolean performOk() {
		AntCorePreferences prefs = AntCorePlugin.getPlugin().getPreferences();
		
		prefs.setAntHomeClasspathEntries(classpathPage.getAntHomeEntries());
		
		
		prefs.setAdditionalClasspathEntries(classpathPage.getAdditionalEntries());
		
		String antHome= classpathPage.getAntHome();
		prefs.setAntHome(antHome);
		
		List contents = tasksPage.getContents(false);
		if (contents != null) {
			Task[] tasks = (Task[]) contents.toArray(new Task[contents.size()]);
			prefs.setCustomTasks(tasks);
		}
		
		contents = typesPage.getContents(false);
		if (contents != null) {
			Type[] types = (Type[]) contents.toArray(new Type[contents.size()]);
			prefs.setCustomTypes(types);
		}
		
		contents = propertiesPage.getProperties();
		if (contents != null) {
			Property[] properties = (Property[]) contents.toArray(new Property[contents.size()]);
			prefs.setCustomProperties(properties);
		}
		
		String[] files = propertiesPage.getPropertyFiles();
		prefs.setCustomPropertyFiles(files);
		
		prefs.updatePluginPreferences();
	
		return super.performOk();
	}
	
	/**
	 * @see org.eclipse.jface.dialogs.DialogPage#setButtonLayoutData(org.eclipse.swt.widgets.Button)
	 */
	protected GridData setButtonLayoutData(Button button) {
		return super.setButtonLayoutData(button);
	}
	
	protected List getLibraryURLs() {
		List urls= new ArrayList();
		urls.addAll(Arrays.asList(classpathPage.getAntHomeEntries()));
		urls.addAll(Arrays.asList(classpathPage.getAdditionalEntries()));
		return urls;
	}
}
