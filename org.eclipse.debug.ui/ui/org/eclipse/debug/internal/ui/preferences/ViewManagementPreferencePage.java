/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.preferences;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.debug.internal.ui.SWTUtil;
import org.eclipse.debug.internal.ui.views.launch.LaunchView;
import org.eclipse.debug.internal.ui.views.launch.LaunchViewContextListener;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveRegistry;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

/**
 * Preference page for configuring the debugger's automatic
 * view management.
 */
public class ViewManagementPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private CheckboxTableViewer fPerspectiveViewer;
	private Button fTrackViewsButton;
	private Button fResetViewsButton;
	
	public ViewManagementPreferencePage() {
		super();
		setTitle(DebugPreferencesMessages.getString("ViewManagementPreferencePage.1")); //$NON-NLS-1$
		setDescription(DebugPreferencesMessages.getString("ViewManagementPreferencePage.0")); //$NON-NLS-1$
		setPreferenceStore(DebugUITools.getPreferenceStore());
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createContents(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		createPerspectiveViewer(composite);
		
		createViewTrackingOptions(composite);
		
		Dialog.applyDialogFont(composite);
		
		return composite;
	}

	/**
	 * @param composite
	 */
	private void createViewTrackingOptions(Composite composite) {
		Group group= new Group(composite, SWT.NONE);
		group.setLayout(new GridLayout());
		group.setLayoutData(new GridData(GridData.FILL_BOTH));
	
		fTrackViewsButton= new Button(group, SWT.CHECK);
		fTrackViewsButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fTrackViewsButton.setText(DebugPreferencesMessages.getString("ViewManagementPreferencePage.3")); //$NON-NLS-1$
		fTrackViewsButton.setSelection(DebugUITools.getPreferenceStore().getBoolean(IDebugUIConstants.PREF_TRACK_VIEWS));
		
		Label label= new Label(group, SWT.WRAP);
		label.setText(DebugPreferencesMessages.getString("ViewManagementPreferencePage.4")); //$NON-NLS-1$
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		//fResetViewsButton= new Button(group, SWT.PUSH);
		//fResetViewsButton.setLayoutData(new GridData());
		//fResetViewsButton.setText("Reset");
		fResetViewsButton= SWTUtil.createPushButton(group, DebugPreferencesMessages.getString("ViewManagementPreferencePage.5"), null); //$NON-NLS-1$
		((GridData) fResetViewsButton.getLayoutData()).horizontalAlignment= GridData.BEGINNING;
		fResetViewsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getPreferenceStore().setValue(LaunchViewContextListener.PREF_VIEWS_TO_NOT_OPEN, ""); //$NON-NLS-1$
				getPreferenceStore().setValue(LaunchViewContextListener.PREF_OPENED_VIEWS, ""); //$NON-NLS-1$
				fResetViewsButton.setEnabled(false);
			}
		});
		// Enable reset if either persisted view collection is not empty.
		boolean enableReset= !"".equals(getPreferenceStore().getString(LaunchViewContextListener.PREF_VIEWS_TO_NOT_OPEN)) || //$NON-NLS-1$
			!"".equals(getPreferenceStore().getString(LaunchViewContextListener.PREF_OPENED_VIEWS)); //$NON-NLS-1$
		fResetViewsButton.setEnabled(enableReset);
	}

	/**
	 * @param parent
	 */
	private void createPerspectiveViewer(Composite parent) {
		Label label= new Label(parent, SWT.WRAP);
		label.setText(DebugPreferencesMessages.getString("ViewManagementPreferencePage.2")); //$NON-NLS-1$
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Table table= new Table(parent, SWT.CHECK | SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		table.setLayout(new GridLayout());
		table.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		fPerspectiveViewer= new CheckboxTableViewer(table);
		PerspectiveProvider provider= new PerspectiveProvider();
		fPerspectiveViewer.setContentProvider(provider);
		fPerspectiveViewer.setLabelProvider(provider);
		fPerspectiveViewer.setInput(this);
		
		checkPerspectives(getPreferenceStore().getString(IDebugUIConstants.PREF_MANAGE_VIEW_PERSPECTIVES));
	}
	
	private void checkPerspectives(String perspectiveList) {
		fPerspectiveViewer.setAllChecked(false);
		IPerspectiveRegistry registry= PlatformUI.getWorkbench().getPerspectiveRegistry();
		Iterator perspectiveIds= LaunchView.parseList(perspectiveList).iterator();
		while (perspectiveIds.hasNext()) {
			IPerspectiveDescriptor descriptor = registry.findPerspectiveWithId((String) perspectiveIds.next());
			fPerspectiveViewer.setChecked(descriptor, true);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferencePage#performOk()
	 */
	public boolean performOk() {
		StringBuffer buffer= new StringBuffer();
		Object[] descriptors = fPerspectiveViewer.getCheckedElements();
		for (int i = 0; i < descriptors.length; i++) {
			buffer.append(((IPerspectiveDescriptor) descriptors[i]).getId()).append(',');
		}
		
		getPreferenceStore().setValue(IDebugUIConstants.PREF_MANAGE_VIEW_PERSPECTIVES, buffer.toString());
		getPreferenceStore().setValue(IDebugUIConstants.PREF_TRACK_VIEWS, fTrackViewsButton.getSelection());
		return super.performOk();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		checkPerspectives(getPreferenceStore().getDefaultString(IDebugUIConstants.PREF_MANAGE_VIEW_PERSPECTIVES));
		fTrackViewsButton.setSelection(getPreferenceStore().getDefaultBoolean(IDebugUIConstants.PREF_TRACK_VIEWS));
		super.performDefaults();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}

	private class PerspectiveProvider implements IStructuredContentProvider, ILabelProvider {

		private List fImages= new ArrayList();
		
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
		 */
		public Object[] getElements(Object inputElement) {
			return PlatformUI.getWorkbench().getPerspectiveRegistry().getPerspectives();
		}
		
		public void dispose() {
			Iterator images= fImages.iterator();
			while (images.hasNext()) {
				((Image) images.next()).dispose();
			}
		}
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
		 */
		public Image getImage(Object element) {
			Image image= null;
			if (element instanceof IPerspectiveDescriptor) {
				image= ((IPerspectiveDescriptor) element).getImageDescriptor().createImage();
				fImages.add(image);
			}
			return image;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
		 */
		public String getText(Object element) {
			String text= null;
			if (element instanceof IPerspectiveDescriptor) {
				IPerspectiveDescriptor descriptor = (IPerspectiveDescriptor) element;
				text= descriptor.getLabel();
			}
			return text;
		}
		public void addListener(ILabelProviderListener listener) {
		}
		public boolean isLabelProperty(Object element, String property) {
			return false;
		}
		public void removeListener(ILabelProviderListener listener) {
		}
		
	}
}
