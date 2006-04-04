/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.mapping;

import java.util.*;

import org.eclipse.core.resources.mapping.IModelProviderDescriptor;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.internal.ui.*;
import org.eclipse.team.internal.ui.registry.TeamContentProviderDescriptor;
import org.eclipse.team.internal.ui.registry.TeamContentProviderManager;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.mapping.ITeamContentProviderDescriptor;
import org.eclipse.team.ui.mapping.ITeamContentProviderManager;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class ModelEnablementPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private Set previosulyEnabled = new HashSet();
	
	public ModelEnablementPreferencePage() {
		setTitle(TeamUIMessages.ModelEnablementPreferencePage_0); 
		setPreferenceStore(TeamUIPlugin.getPlugin().getPreferenceStore());
	}

	private CheckboxTableViewer tableViewer;

	protected Control createContents(Composite parent) {
		
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Label l = SWTUtils.createLabel(composite, TeamUIMessages.ModelEnablementPreferencePage_1);
		l.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		tableViewer = CheckboxTableViewer.newCheckList(composite, SWT.BORDER);
		tableViewer.getTable().setLayoutData(new GridData(GridData.FILL_BOTH));
		tableViewer.setContentProvider(new IStructuredContentProvider() {
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				// noting to do
			}
			public void dispose() {
				// nothing to do
			}
			public Object[] getElements(Object element) {
				if (element instanceof ITeamContentProviderManager) {
					ITeamContentProviderManager manager = (ITeamContentProviderManager) element;
					return manager.getDescriptors();
				}
				return new Object[0];
			}
		});
		tableViewer.setLabelProvider(new LabelProvider() {
			Map images = new HashMap();
			public String getText(Object element) {
				if (element instanceof ITeamContentProviderDescriptor) {
					ITeamContentProviderDescriptor desc = (ITeamContentProviderDescriptor) element;
					return getTextFor(desc.getModelProviderId());
				}
				return super.getText(element);
			}
			private String getTextFor(String modelProviderId) {
				IModelProviderDescriptor desc = ModelProvider.getModelProviderDescriptor(modelProviderId);
				if (desc != null) {
					// Only do this for the resource model since we don;t want to 
					// load all model providers (see bug 133604)
					if (desc.getId().equals(ModelProvider.RESOURCE_MODEL_PROVIDER_ID))
						try {
							return Utils.getLabel(desc.getModelProvider());
						} catch (CoreException e) {
							TeamUIPlugin.log(e);
						}
					return desc.getLabel();
				}
				return modelProviderId;
			}
			public Image getImage(Object element) {
				if (element instanceof ITeamContentProviderDescriptor) {
					ITeamContentProviderDescriptor desc = (ITeamContentProviderDescriptor) element;
					Image image = (Image)images.get(desc);
					if (image == null) {
						ImageDescriptor idesc = desc.getImageDescriptor();
						if (idesc != null) {
							image = idesc.createImage();
							if (image != null) {
								images.put(desc, image);
							}
						}
					}
					return image;
				}
				return super.getImage(element);
			}
			public void dispose() {
				for (Iterator iter = images.values().iterator(); iter.hasNext();) {
					Image image = (Image) iter.next();
					image.dispose();
				}
				super.dispose();
			}
		});
		tableViewer.setInput(TeamUI.getTeamContentProviderManager());
		updateChecks();
		applyDialogFont(composite);
		return composite;
	}

	private void updateChecks() {
		ITeamContentProviderDescriptor[] descriptors = TeamUI.getTeamContentProviderManager().getDescriptors();
		for (int i = 0; i < descriptors.length; i++) {
			ITeamContentProviderDescriptor descriptor = descriptors[i];
			if (descriptor.isEnabled()) {
				previosulyEnabled.add(descriptor);
			}
		}
		tableViewer.setCheckedElements(previosulyEnabled.toArray());
	}
	
	public boolean performOk() {
		boolean changed = false;
		Object[] checked = tableViewer.getCheckedElements();
		Set nowEnabled = new HashSet();
		nowEnabled.addAll(Arrays.asList(checked));
		ITeamContentProviderDescriptor[] descriptors = TeamUI.getTeamContentProviderManager().getDescriptors();
		for (int i = 0; i < descriptors.length; i++) {
			ITeamContentProviderDescriptor descriptor = descriptors[i];
			boolean enable = false;
			for (int j = 0; j < checked.length; j++) {
				ITeamContentProviderDescriptor checkedDesc = (ITeamContentProviderDescriptor)checked[j];
				if (checkedDesc.getModelProviderId().equals(descriptor.getModelProviderId())) {
					enable = true;
					break;
				}
			}
			if (descriptor.isEnabled() != enable) {
				((TeamContentProviderDescriptor)descriptor).setEnabled(enable);
				changed = true;
			}
		}
		if (changed) {
			((TeamContentProviderManager)TeamUI.getTeamContentProviderManager()).enablementChanged(
					(ITeamContentProviderDescriptor[]) previosulyEnabled.toArray(new ITeamContentProviderDescriptor[previosulyEnabled.size()]),
					(ITeamContentProviderDescriptor[]) nowEnabled.toArray(new ITeamContentProviderDescriptor[nowEnabled.size()]));
			previosulyEnabled = nowEnabled;
		}
		return true;
	}
	
	protected void performDefaults() {
		tableViewer.setCheckedElements(TeamUI.getTeamContentProviderManager().getDescriptors());
	}

	public void init(IWorkbench workbench) {
		// ignore
	}

}
