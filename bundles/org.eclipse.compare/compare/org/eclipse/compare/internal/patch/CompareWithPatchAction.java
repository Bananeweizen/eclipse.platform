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
package org.eclipse.compare.internal.patch;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;

import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IEditorPart;

import org.eclipse.compare.internal.*;


public class CompareWithPatchAction implements IActionDelegate {

	static class PatchWizardDialog extends WizardDialog {
	
		PatchWizardDialog(Shell parent, IWizard wizard) {
			super(parent, wizard);
			
			setShellStyle(getShellStyle() | SWT.RESIZE);
			setMinimumPageSize(700, 500);
		}
	}
	
	private ISelection fSelection;
	

	public void selectionChanged(IAction action, ISelection selection) {
		fSelection= selection;
		IResource[] resources= PatchWizard.getResource(fSelection);
		action.setEnabled(resources != null && resources.length == 1);
	}
		
	public void run(IAction action) {
		PatchWizard wizard= new PatchWizard(fSelection);
		
		if (areAllEditorsSaved()) {
			PatchWizardDialog dialog= new PatchWizardDialog(CompareUIPlugin.getShell(), wizard);
			dialog.open();
		}
	}

	private boolean areAllEditorsSaved(){
		if (CompareUIPlugin.getDirtyEditors().length == 0)
			return true;
		if (! saveAllDirtyEditors())
			return false;
		Shell shell= CompareUIPlugin.getShell();
		try {
			// Save isn't cancelable.
			IWorkspace workspace= ResourcesPlugin.getWorkspace();
			IWorkspaceDescription description= workspace.getDescription();
			boolean autoBuild= description.isAutoBuilding();
			description.setAutoBuilding(false);
			workspace.setDescription(description);
			try {
				new ProgressMonitorDialog(shell).run(false, false, createRunnable());
			} finally {
				description.setAutoBuilding(autoBuild);
				workspace.setDescription(description);
			}
			return true;
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, shell, PatchMessages.getString("PatchAction.ExceptionTitle"), PatchMessages.getString("Exception"));  //$NON-NLS-1$ //$NON-NLS-2$
			return false;
		} catch (CoreException e) {
			ExceptionHandler.handle(e, shell, PatchMessages.getString("PatchAction.ExceptionTitle"), PatchMessages.getString("Exception"));  //$NON-NLS-1$ //$NON-NLS-2$
			return false;			
		} catch (InterruptedException e) {
			Assert.isTrue(false); // Can't happen. Operation isn't cancelable.
			return false;
		}
	}

	private IRunnableWithProgress createRunnable() {
		return new IRunnableWithProgress() {
			public void run(IProgressMonitor pm) {
				IEditorPart[] editorsToSave= CompareUIPlugin.getDirtyEditors();
				pm.beginTask(PatchMessages.getString("PatchAction.SavingDirtyEditorsTask"), editorsToSave.length); //$NON-NLS-1$
				for (int i= 0; i < editorsToSave.length; i++) {
					editorsToSave[i].doSave(new SubProgressMonitor(pm, 1));
					pm.worked(1);
				}
				pm.done();
			}
		};
	}

	private boolean saveAllDirtyEditors() {
		if (ComparePreferencePage.getSaveAllEditors()) //must save everything
			return true;
		ListDialog dialog= new ListDialog(CompareUIPlugin.getShell()) {
			protected Control createDialogArea(Composite parent) {
				Composite result= (Composite) super.createDialogArea(parent);
				final Button check= new Button(result, SWT.CHECK);
				check.setText(PatchMessages.getString("PatchAction.AlwaysSaveQuestion")); //$NON-NLS-1$
				check.setSelection(ComparePreferencePage.getSaveAllEditors());
				check.addSelectionListener(
					new SelectionAdapter() {
						public void widgetSelected(SelectionEvent e) {
							ComparePreferencePage.setSaveAllEditors(check.getSelection());
						}
					}
				);
				applyDialogFont(result);
				return result;
			}
		};
		dialog.setTitle(PatchMessages.getString("PatchAction.SaveAllQuestion")); //$NON-NLS-1$
		dialog.setAddCancelButton(true);
		dialog.setLabelProvider(createDialogLabelProvider());
		dialog.setMessage(PatchMessages.getString("PatchAction.SaveAllDescription")); //$NON-NLS-1$
		dialog.setContentProvider(new ListContentProvider());
		dialog.setInput(Arrays.asList(CompareUIPlugin.getDirtyEditors()));
		return dialog.open() == Dialog.OK;
	}

	private ILabelProvider createDialogLabelProvider() {
		return new LabelProvider() {
			public Image getImage(Object element) {
				return ((IEditorPart) element).getTitleImage();
			}
			public String getText(Object element) {
				return ((IEditorPart) element).getTitle();
			}
		};
	}
}
