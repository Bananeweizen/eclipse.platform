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
import java.util.Iterator;
import java.util.List;

import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.ant.core.AntCorePreferences;
import org.eclipse.ant.core.Task;
import org.eclipse.ant.internal.ui.model.IAntUIHelpContextIds;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

/**
 * Sub-page that allows the user to enter custom tasks
 * to be used when running Ant build files.
 */
public class AntTasksPage extends AntPage {
	private static final int ADD_TASK_BUTTON = IDialogConstants.CLIENT_ID + 1;
	private static final int EDIT_TASK_BUTTON = IDialogConstants.CLIENT_ID + 2;
	private static final int REMOVE_BUTTON = IDialogConstants.CLIENT_ID + 3;

	private final AntObjectLabelProvider labelProvider = new AntObjectLabelProvider();
	/**
	 * Creates an instance.
	 */
	public AntTasksPage(AntRuntimePreferencePage preferencePage) {
		super(preferencePage);
	}
	
	/* (non-Javadoc)
	 * Method declared on AntPage.
	 */
	protected void addButtonsToButtonGroup(Composite parent) {
		createPushButton(parent, AntPreferencesMessages.getString("AntTasksPage.1"), ADD_TASK_BUTTON); //$NON-NLS-1$
		editButton = createPushButton(parent, AntPreferencesMessages.getString("AntTasksPage.2"), EDIT_TASK_BUTTON); //$NON-NLS-1$
		removeButton = createPushButton(parent, AntPreferencesMessages.getString("AntTasksPage.3"), REMOVE_BUTTON); //$NON-NLS-1$
	}
	
	/**
	 * Allows the user to enter a custom task.
	 */
	private void addTask() {
		String title = AntPreferencesMessages.getString("AntTasksPage.addTaskDialogTitle"); //$NON-NLS-1$
		AddCustomDialog dialog = getCustomDialog(title, IAntUIHelpContextIds.ADD_TASK_DIALOG);
		if (dialog.open() == Window.CANCEL) {
			return;
		}

		Task task = new Task();
		task.setTaskName(dialog.getName());
		task.setClassName(dialog.getClassName());
		task.setLibrary(dialog.getLibrary());
		addContent(task);
	}
	
	private AddCustomDialog getCustomDialog(String title, String helpContext) {
		Iterator tasks= getContents(true).iterator();
		List names= new ArrayList();
		while (tasks.hasNext()) {
			Task task = (Task) tasks.next();
			names.add(task.getTaskName());	
		}
		AddCustomDialog dialog = new AddCustomDialog(getShell(), getPreferencePage().getLibraryURLs(), names, helpContext);
		dialog.setTitle(title);
		dialog.setAlreadyExistsErrorMsg(AntPreferencesMessages.getString("AntTasksPage.8")); //$NON-NLS-1$
		dialog.setNoNameErrorMsg(AntPreferencesMessages.getString("AntTasksPage.9")); //$NON-NLS-1$
		return dialog;
	}

	/* (non-Javadoc)
	 * Method declared on AntPage.
	 */
	protected void buttonPressed(int buttonId) {
		switch (buttonId) {
			case ADD_TASK_BUTTON :
				addTask();
				break;
			case EDIT_TASK_BUTTON :
				edit(getSelection());
				break;
			case REMOVE_BUTTON :
				remove();
				break;
		}
	}
	
	/**
	 * Creates the tab item that contains this sub-page.
	 */
	protected TabItem createTabItem(TabFolder folder) {
		TabItem item = new TabItem(folder, SWT.NONE);
		item.setText(AntPreferencesMessages.getString("AntTasksPage.title")); //$NON-NLS-1$
		item.setImage(labelProvider.getTaskImage());
		item.setData(this);
		Composite top = new Composite(folder, SWT.NONE);
		top.setFont(folder.getFont());
		item.setControl(createContents(top));
		
		connectToFolder(item, folder);
		
		return item;
	}

	/**
	 * Allows the user to edit a custom Ant task.
	 */	
	protected void edit(IStructuredSelection selection) {
		Task task= (Task)selection.getFirstElement();
		String title = AntPreferencesMessages.getString("AntTasksPage.editTaskDialogTitle"); //$NON-NLS-1$
		AddCustomDialog dialog = getCustomDialog(title, IAntUIHelpContextIds.EDIT_TASK_DIALOG);
		dialog.setClassName(task.getClassName());
		dialog.setName(task.getTaskName());
		dialog.setLibrary(task.getLibrary());
		if (dialog.open() == Window.CANCEL) {
			return;
		}

		task.setTaskName(dialog.getName());
		task.setClassName(dialog.getClassName());
		task.setLibrary(dialog.getLibrary());
		updateContent(task);
	}
	
	/* (non-Javadoc)
	 * Method declared on AntPage.
	 */
	protected ITableLabelProvider getLabelProvider() {
		return labelProvider;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.preferences.AntPage#initialize()
	 */
	protected void initialize() {
		AntCorePreferences prefs = AntCorePlugin.getPlugin().getPreferences();
		setInput(prefs.getTasks());
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.preferences.AntPage#getHelpContextId()
	 */
	protected String getHelpContextId() {
		return IAntUIHelpContextIds.ANT_TASKS_PAGE;
	}
}
