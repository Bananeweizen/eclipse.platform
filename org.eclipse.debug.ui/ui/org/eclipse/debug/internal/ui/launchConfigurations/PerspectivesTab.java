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
package org.eclipse.debug.internal.ui.launchConfigurations;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationListener;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.PixelConverter;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveRegistry;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.help.WorkbenchHelp;

/**
 * PerspectivesTab
 */
public class PerspectivesTab extends AbstractLaunchConfigurationTab implements ILaunchConfigurationListener {
	
	/**
	 * The launch config type this tab pertains to
	 */
	private ILaunchConfigurationType fType = null;
	
	/**
	 * Array containing modes this config type supports
	 */
	private String[] fModeIds = null;
			
	/**
	 * Array of all perspective labels for combo box (including 'None')
	 */
	private String[] fPerspectiveLabels = null;
	
	/**
	 * Map of perspective labels to ids
	 */
	private Map fPerspectiveIds = null;

	/**
	 * Combo boxes corresponding to modes
	 */
	private Combo[] fCombos = null;
	
	/**
	 * Flag indicating the UI is updating from the config, and should not
	 * update the config in response to the change.
	 */
	private boolean fInitializing = false;
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#dispose()
	 */
	public void dispose() {
		super.dispose();
		DebugPlugin.getDefault().getLaunchManager().removeLaunchConfigurationListener(this);		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.ILaunchConfigurationListener#launchConfigurationAdded(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void launchConfigurationAdded(ILaunchConfiguration configuration) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.ILaunchConfigurationListener#launchConfigurationChanged(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void launchConfigurationChanged(ILaunchConfiguration configuration) {
		if (!configuration.isWorkingCopy()) {
			if (configuration.getName().startsWith(getLaunchConfigurationType().getIdentifier())) {
				for (int i = 0; i < fModeIds.length; i++) {
					String mode = fModeIds[i];
					try {
						String persp = configuration.getAttribute(mode, (String)null);
						if (persp == null) {
							// default
							persp = IDebugUIConstants.PERSPECTIVE_DEFAULT;
						}
						DebugUITools.setLaunchPerspective(getLaunchConfigurationType(), mode, persp);
					} catch (CoreException e) {
						DebugUIPlugin.log(e);
					}
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.ILaunchConfigurationListener#launchConfigurationRemoved(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void launchConfigurationRemoved(ILaunchConfiguration configuration) {
	}

	/**
	 * Constructs a new tab
	 * 
	 * @param type
	 */
	public PerspectivesTab(ILaunchConfigurationType type) {
		super();
		fType = type;
		DebugPlugin.getDefault().getLaunchManager().addLaunchConfigurationListener(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getImage()
	 */
	public Image getImage() {
		return DebugPluginImages.getImage(IInternalDebugUIConstants.IMG_OBJS_PERSPECTIVE_TAB);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		setControl(composite);
		WorkbenchHelp.setHelp(getControl(), IDebugHelpContextIds.LAUNCH_CONFIGURATION_DIALOG_PERSPECTIVE_TAB);
		GridLayout layout = new GridLayout(2, false);
		composite.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(gd);
		composite.setFont(parent.getFont());
		
		Label label = new Label(composite, SWT.LEFT + SWT.WRAP);
		label.setFont(parent.getFont());
		label.setText(MessageFormat.format(LaunchConfigurationsMessages.getString("PerspectivesTab.0"), new String[]{getLaunchConfigurationType().getName()})); //$NON-NLS-1$
		gd = new GridData();
		PixelConverter converter = new PixelConverter(label);
		gd.widthHint = converter.convertWidthInCharsToPixels(80);
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);
		
		// init modes
		String[] modes = DebugPlugin.getDefault().getLaunchManager().getLaunchModes();
		ArrayList supported = new ArrayList();
		for (int i = 0; i < modes.length; i++) {
			String string = modes[i];
			if (getLaunchConfigurationType().supportsMode(string)) {
				supported.add(string);
			}
		}
		fModeIds = (String[])supported.toArray(new String[supported.size()]);
		
		// init perspective labels
		final IPerspectiveRegistry registry = PlatformUI.getWorkbench().getPerspectiveRegistry();
		IPerspectiveDescriptor[] descriptors = registry.getPerspectives();
		fPerspectiveLabels = new String[descriptors.length + 1];
		fPerspectiveLabels[0] = LaunchConfigurationsMessages.getString("PerspectivesTab.1"); //$NON-NLS-1$
		fPerspectiveIds = new HashMap(descriptors.length);
		for (int i = 0; i < descriptors.length; i++) {
			IPerspectiveDescriptor descriptor = descriptors[i];
			fPerspectiveLabels[i + 1] = descriptor.getLabel();
			fPerspectiveIds.put(descriptor.getLabel(), descriptor.getId());
		}
		
		// spacer
		createVerticalSpacer(composite, 2);
		
		fCombos = new Combo[fModeIds.length];
		for (int i = 0; i < fModeIds.length; i++) {
			label = new Label(composite, SWT.NONE);
			label.setFont(composite.getFont());
			gd = new GridData(GridData.BEGINNING);
			gd.horizontalSpan= 1;
			label.setLayoutData(gd);
			String text = DebugPlugin.getDefault().getLaunchManager().getLaunchModeLabel(fModeIds[i]);
			label.setText(MessageFormat.format(LaunchConfigurationsMessages.getString("PerspectivesTab.2"), new String[]{text})); //$NON-NLS-1$
			
			Combo combo = new Combo(composite, SWT.READ_ONLY);
			combo.setFont(composite.getFont());
			combo.setItems(fPerspectiveLabels);
			combo.setData(fModeIds[i]);
			gd = new GridData(GridData.BEGINNING);
			combo.setLayoutData(gd);
			fCombos[i] = combo;
			combo.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					updateLaunchConfigurationDialog();
				}
			});
		}
		
		createVerticalSpacer(composite, 2);
		
		Button restoreDefaults = createPushButton(composite, LaunchConfigurationsMessages.getString("PerspectivesTab.3"), null); //$NON-NLS-1$
		restoreDefaults.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				for (int i = 0; i < fCombos.length; i++) {
					String mode = (String)fCombos[i].getData();
					String def = DebugUIPlugin.getDefault().getPerspectiveManager().getDefaultLaunchPerspective(getLaunchConfigurationType(), mode);
					if (def == null) {
						fCombos[i].setText(LaunchConfigurationsMessages.getString("PerspectivesTab.1")); //$NON-NLS-1$
					} else {
						IPerspectiveDescriptor descriptor = registry.findPerspectiveWithId(def);
						fCombos[i].setText(descriptor.getLabel());
					}
				}
				updateLaunchConfigurationDialog();
			}
		});
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		for (int i = 0; i < fModeIds.length; i++) {
			String mode = fModeIds[i];
			// null indicates default
			configuration.setAttribute(mode, (String)null);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration configuration) {
		// each perspective is stored with its mode identifier
		fInitializing = true;
		IPerspectiveRegistry registry = PlatformUI.getWorkbench().getPerspectiveRegistry();
		for (int i = 0; i < fModeIds.length; i++) {
			String mode = fModeIds[i];
			String persp;
			try {
				persp = configuration.getAttribute(mode, (String)null);
				if (persp == null) {
					// null indicates default
					persp = DebugUITools.getLaunchPerspective(getLaunchConfigurationType(), mode);
				}
				if (IDebugUIConstants.PERSPECTIVE_NONE.equals(persp)) {
					persp = null;
				}
				if (persp == null) {
					// select none
					fCombos[i].setText(LaunchConfigurationsMessages.getString("PerspectivesTab.1")); //$NON-NLS-1$
				} else {
					IPerspectiveDescriptor descriptor = registry.findPerspectiveWithId(persp);
					fCombos[i].setText(descriptor.getLabel());
				}
			} catch (CoreException e) {
				DebugUIPlugin.log(e);
			}
		}
		fInitializing = false;

	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		for (int i = 0; i < fCombos.length; i++) {
			updateConfigFromCombo(fCombos[i], configuration);
		}
	}
	
	protected void updateConfigFromCombo(Combo combo, ILaunchConfigurationWorkingCopy workingCopy) {
		if (!fInitializing) {
			String mode = (String)combo.getData();
			String persp = combo.getText();
			if (persp.equals(LaunchConfigurationsMessages.getString("PerspectivesTab.1"))) { //$NON-NLS-1$
				persp = IDebugUIConstants.PERSPECTIVE_NONE;
			} else {
				persp = (String)fPerspectiveIds.get(persp);
			}
			// if the same as default, use null which indicates default
			String def = DebugUIPlugin.getDefault().getPerspectiveManager().getDefaultLaunchPerspective(getLaunchConfigurationType(), mode);
			if (def == null) {
				def = IDebugUIConstants.PERSPECTIVE_NONE;
			}
			if (persp.equals(def)) {
				persp = null;
			}
			workingCopy.setAttribute(mode, persp);
		}		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
	 */
	public String getName() {
		return LaunchConfigurationsMessages.getString("PerspectivesTab.7"); //$NON-NLS-1$
	}
	
	/**
	 * Returns the launch configuration type this tab was opened on.
	 * 
	 * @return launch config type
	 */
	protected ILaunchConfigurationType getLaunchConfigurationType() {
		return fType;
	}

}
