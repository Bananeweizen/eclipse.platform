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
package org.eclipse.team.internal.ui.wizards;

import java.util.*;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.views.navigator.ResourceSorter;

/**
 * Page that allows the user to select a set of resources that are managed
 * by a synchronize participant.
 * 
 * Remembers last participant
 * 
 * @since 3.0
 */
public class GlobalRefreshParticipantSelectionPage extends WizardPage implements IDoubleClickListener, ISelectionChangedListener {

	private TableViewer fViewer;
	private ISynchronizeParticipantDescriptor selectedParticipantDescriptor;
	private IWizard wizard;
	private ISynchronizeParticipant participant;
	private List createdImages;

	class MyContentProvider extends BaseWorkbenchContentProvider {
		public Object[] getChildren(Object element) {
			if(element instanceof ISynchronizeManager) {
				List participants = new ArrayList();
				ISynchronizeManager manager = (ISynchronizeManager)element;
				ISynchronizeParticipant[] desciptors = manager.getSynchronizeParticipants();
				for (int i = 0; i < desciptors.length; i++) {
					ISynchronizeParticipant descriptor = desciptors[i];
					if(descriptor.doesSupportSynchronize()) {
						participants.add(descriptor);
					}
				}
				return (ISynchronizeParticipant[]) participants.toArray(new ISynchronizeParticipant[participants.size()]);
			}
			return super.getChildren(element);
		}
	}
	
	class MyLabelProvider extends LabelProvider {
		public String getText(Object element) {
			if(element instanceof ISynchronizeParticipant) {
				ISynchronizeParticipant descriptor = (ISynchronizeParticipant)element;
				return descriptor.getName();
			}
			return null;
		}	
		
		public Image getImage(Object element) {
			if(element instanceof ISynchronizeParticipant) {
				ISynchronizeParticipant descriptor = (ISynchronizeParticipant)element;
				ImageDescriptor d = descriptor.getImageDescriptor();
				if(createdImages == null) {
					createdImages = new ArrayList(3);
				}
				Image image = d.createImage();
				createdImages.add(image);
				return image;
			}
			return null;
		}
	}
		
	public GlobalRefreshParticipantSelectionPage() {
		super("Synchronize");
		setDescription("Select the participant to synchronize");
		setTitle("Select a Synchronize Participant");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#dispose()
	 */
	public void dispose() {
		if (createdImages != null) {
			for (Iterator it = createdImages.iterator(); it.hasNext();) {
				Image image = (Image) it.next();
				image.dispose();
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent2) {
		Composite top = new Composite(parent2, SWT.NULL);
		top.setLayout(new GridLayout());
		setControl(top);
		
		Label l = new Label(top, SWT.NULL);
		l.setText("Available synchronize participants:");
		fViewer = new TableViewer(top, SWT.BORDER);
		GridData data = new GridData(GridData.FILL_BOTH);
		fViewer.getControl().setLayoutData(data);
		fViewer.setContentProvider(new MyContentProvider());
		fViewer.addDoubleClickListener(this);
		fViewer.setLabelProvider(new MyLabelProvider());
		fViewer.setSorter(new ResourceSorter(ResourceSorter.NAME));
		fViewer.setInput(TeamUI.getSynchronizeManager());
		fViewer.addSelectionChangedListener(this);
		fViewer.getTable().setFocus();
		 Dialog.applyDialogFont(parent2);
	}
	
	public void doubleClick(DoubleClickEvent event) {
		selectionChanged(
			new SelectionChangedEvent(
				event.getViewer(),
				event.getViewer().getSelection()));
		getContainer().showPage(getNextPage());
	}
	
	public void selectionChanged(SelectionChangedEvent event) {
		// Initialize the wizard so we can tell whether to enable the
		// Next button
		ISelection selection = event.getSelection();
		if (selection == null || !(selection instanceof IStructuredSelection)) {
			wizard = null;
			setPageComplete(false);
			return;
		}
		IStructuredSelection ss = (IStructuredSelection) selection;
		if (ss.size() != 1) {
			wizard = null;
			setPageComplete(false);
			return;
		}
		participant = (ISynchronizeParticipant)ss.getFirstElement();
		wizard = participant.createSynchronizeWizard();
		wizard.addPages();		
		// Ask the container to update button enablement
		setPageComplete(true);
	}
	
	public IWizard getSelectedWizard() {
		return this.wizard;
	}
	
	public ISynchronizeParticipant getSelectedParticipant() {
		return this.participant;
	}
	
	/**
	 * The <code>WizardSelectionPage</code> implementation of 
	 * this <code>IWizardPage</code> method returns the first page 
	 * of the currently selected wizard if there is one.
	 * 
	 * @see WizardPage#getNextPage
	 */
	public IWizardPage getNextPage() {
		if (wizard == null) return null;
		return wizard.getStartingPage();
	}

	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			fViewer.getTable().setFocus();
		}
	}
}
