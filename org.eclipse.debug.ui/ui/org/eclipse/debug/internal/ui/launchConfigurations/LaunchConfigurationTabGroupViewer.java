package org.eclipse.debug.internal.ui.launchConfigurations;

/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp.  All rights reserved.
This file is made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html
**********************************************************************/

import java.text.MessageFormat;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.SWTUtil;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.ILaunchConfigurationTabGroup;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

/**
 * A viewer that displays tabs for a launch configuration, with apply and revert
 * buttons.
 */
public class LaunchConfigurationTabGroupViewer extends Viewer {
	
	/**
	 * Containing launch dialog
	 */
	private ILaunchConfigurationDialog fDialog;
	
	/**
	 * The this viewer's input
	 */
	private Object fInput;
	
	/**
	 * The launch configuration (original) being edited
	 */
	private ILaunchConfiguration fOriginal;
	
	/**
	 * The working copy of the original
	 */
	private ILaunchConfigurationWorkingCopy fWorkingCopy;
	
	/**
	 * This view's control, which contains a composite area of controls
	 */
	private Composite fViewerControl;
	
	/**
	 * Name text widget
	 */
	private Text fNameWidget;
	
	/**
	 * Composite containing the launch config tab widgets
	 */
	private Composite fTabComposite;
	
	/**
	 * Tab folder
	 */
	private TabFolder fTabFolder;
	
	/**
	 * The current tab group being displayed
	 */
	private ILaunchConfigurationTabGroup fTabGroup;

	/**
	 * The type of config tabs are currently displayed
	 * for
	 */
	private ILaunchConfigurationType fTabType;	
	
	/**
	 * Index of the active tab
	 */
	private int fCurrentTabIndex = -1;
	
	/**
	 * Apply & Rever buttons
	 */
	private Button fApplyButton;
	private Button fRevertButton;
	
	/**
	 * Whether tabs are currently being disposed or initialized
	 */
	private boolean fDisposingTabs = false;
	private boolean fInitializingTabs = false;

	/**
	 * Constructs a viewer in the given composite, contained by the given
	 * launch configuration dialog.
	 * 
	 * @param parent composite containing this viewer
	 * @param dialog containing launch configuration dialog
	 */
	public LaunchConfigurationTabGroupViewer(Composite parent, ILaunchConfigurationDialog dialog) {
		super();
		fDialog = dialog;
		createControl(parent);
	}
	
	/**
	 * Cleanup
	 */
	public void dispose() {
		disposeTabGroup();
	}

	/**
	 * Dispose the active tab group, if any.
	 */
	protected void disposeTabGroup() {
		if (getTabGroup() != null) {
			getTabGroup().dispose();
			setTabGroup(null);
			setTabType(null);
		}
	}	
	
	/**
	 * Creates this viewer's control This area displays the name of the launch
	 * configuration currently being edited, as well as a tab folder of tabs
	 * that are applicable to the launch configuration.
	 *
	 * @return the composite used for launch configuration editing
	 */
	private void createControl(Composite parent) {
		fViewerControl = new Composite(parent, SWT.NONE);
		GridLayout outerCompLayout = new GridLayout();
		outerCompLayout.numColumns = 2;
		outerCompLayout.marginHeight = 0;
		outerCompLayout.marginWidth = 5;
		fViewerControl.setLayout(outerCompLayout);
		GridData gd = new GridData(GridData.FILL_BOTH);
		fViewerControl.setLayoutData(gd);

		Label nameLabel = new Label(fViewerControl, SWT.HORIZONTAL | SWT.LEFT);
		nameLabel.setText(LaunchConfigurationsMessages.getString("LaunchConfigurationDialog.&Name__16")); //$NON-NLS-1$
		gd = new GridData(GridData.BEGINNING);
		nameLabel.setLayoutData(gd);

		Text nameText = new Text(fViewerControl, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		nameText.setLayoutData(gd);
		setNameWidget(nameText);

		getNameWidget().addModifyListener(
			new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					handleNameModified();
				}
			}
		);

		Label spacer = new Label(fViewerControl, SWT.NONE);
		gd = new GridData();
		gd.horizontalSpan = 2;
		spacer.setLayoutData(gd);

		fTabComposite = new Composite(fViewerControl, SWT.NONE);
		GridLayout outerTabCompositeLayout = new GridLayout();
		outerTabCompositeLayout.marginHeight = 0;
		outerTabCompositeLayout.marginWidth = 0;
		fTabComposite.setLayout(outerTabCompositeLayout);
		gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 2;
		fTabComposite.setLayoutData(gd);

		TabFolder tabFolder = new TabFolder(fTabComposite, SWT.NONE);
		setTabFolder(tabFolder);
		gd = new GridData(GridData.FILL_BOTH);
		tabFolder.setLayoutData(gd);
		getTabFolder().addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				if (!isInitializingTabs()) {
					handleTabSelected();
				}
			}
		});

		Composite buttonComp = new Composite(fViewerControl, SWT.NONE);
		GridLayout buttonCompLayout = new GridLayout();
		buttonCompLayout.numColumns = 2;
		buttonComp.setLayout(buttonCompLayout);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
		gd.horizontalSpan = 2;
		buttonComp.setLayoutData(gd);

		setApplyButton(new Button(buttonComp, SWT.PUSH));
		getApplyButton().setText(LaunchConfigurationsMessages.getString("LaunchConfigurationDialog.&Apply_17")); //$NON-NLS-1$
		gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
		getApplyButton().setLayoutData(gd);
		SWTUtil.setButtonDimensionHint(getApplyButton());
		getApplyButton().addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				handleApplyPressed();
			}
		});

		setRevertButton(new Button(buttonComp, SWT.PUSH));
		getRevertButton().setText(LaunchConfigurationsMessages.getString("LaunchConfigurationDialog.Revert_2"));   //$NON-NLS-1$
		gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
		getRevertButton().setLayoutData(gd);
		SWTUtil.setButtonDimensionHint(getRevertButton());
		getRevertButton().addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				handleRevertPressed();
			}
		});

	}
	
	/**
	 * Sets the apply button
	 */
	private void setApplyButton(Button button) {
		fApplyButton = button;
	}
	
	/**
	 * Returns the apply button
	 */
	protected Button getApplyButton() {
		return fApplyButton;
	}	
	
	/**
	 * Sets the revert button
	 */
	private void setRevertButton(Button button) {
		fRevertButton = button;
	}	
	
	/**
	 * Returns the revert button
	 */
	protected Button getRevertButton() {
		return fRevertButton;
	}	
	
	/**
	 * Sets the tab folder
	 */
	private void setTabFolder(TabFolder tabFolder) {
		fTabFolder = tabFolder;
	}
	
	/**
	 * Sets the tab folder
	 */
	private TabFolder getTabFolder() {
		return fTabFolder;
	}
		
	/**
	 * Returns the name widget
	 */
	private Text getNameWidget() {
		return fNameWidget;
	}
	
	/**
	 * Sets the name widget
	 */
	private void setNameWidget(Text nameText) {
		fNameWidget = nameText;
	}
	
	/**
	 * Sets the current name
	 */
	public void setName(String name) {
		if (getWorkingCopy() != null) {
			if (name == null) {
				name = ""; //$NON-NLS-1$
			}
			getNameWidget().setText(name.trim());
			refreshStatus();
		}
	}	

	/**
	 * @see org.eclipse.jface.viewers.Viewer#getControl()
	 */
	public Control getControl() {
		return fViewerControl;
	}
	
	/**
	 * Returns the shell this viewer is contained in.
	 */
	protected Shell getShell() {
		return getControl().getShell();
	}

	/**
	 * @see org.eclipse.jface.viewers.IInputProvider#getInput()
	 */
	public Object getInput() {
		return fInput;
	}

	/**
	 * @see org.eclipse.jface.viewers.ISelectionProvider#getSelection()
	 */
	public ISelection getSelection() {
		if (getActiveTab() == null) {
			return new StructuredSelection();
		} else {
			return new StructuredSelection(getActiveTab());
		}
	}

	/**
	 * @see org.eclipse.jface.viewers.Viewer#refresh()
	 */
	public void refresh() {
		ILaunchConfigurationTab[] tabs = getTabs();
		if (!isInitializingTabs() && tabs != null) {
			// update the working copy from the active tab
			getActiveTab().performApply(getWorkingCopy());
			updateButtons();
			// update error ticks
			TabFolder folder = getTabFolder();
			for (int i = 0; i < tabs.length; i++) {
				ILaunchConfigurationTab tab = tabs[i];
				boolean error = tab.getErrorMessage() != null;
				TabItem item = folder.getItem(i);
				setTabIcon(item, error, tab);
			}		
		}
	}

	private void updateButtons() {
		boolean dirty = isDirty();
		getApplyButton().setEnabled(dirty && canSave());
		getRevertButton().setEnabled(dirty);
	}
	
	/**
	 * Set the specified tab item's icon to an error icon if <code>error</code> is true,
	 * or a transparent icon of the same size otherwise.
	 */
	private void setTabIcon(TabItem tabItem, boolean error, ILaunchConfigurationTab tab) {
		Image image = null;
		if (error) {
			image = LaunchConfigurationManager.getDefault().getErrorTabImage(tab);
		} else {
			image = tab.getImage();
		}
		tabItem.setImage(image);
	}	

	/**
	 * @see org.eclipse.jface.viewers.Viewer#setInput(java.lang.Object)
	 */
	public void setInput(Object input) {
		if (input == null) {
			if (fInput == null) {
				return;
			} else {
				inputChanged(input);
			}
		} else {
			if (!input.equals(fInput)) {
				inputChanged(input);
			}
		}
	}
	
	/**
	 * The input has changed to the given object, possibly <code>null</code>.
	 * 
	 * @param input the new input, possibly <code>null</code>
	 */
	protected void inputChanged(Object input) {
		fInput = input;
		if (input instanceof ILaunchConfiguration) {
			ILaunchConfiguration configuration = (ILaunchConfiguration)input;
			setOriginal(configuration);
			try {
				setWorkingCopy(configuration.getWorkingCopy());
			} catch (CoreException e) {
				errorDialog(e);
			}
			displayTabs();
		} else {
			setOriginal(null);
			setWorkingCopy(null);
			getControl().setVisible(false);
			disposeExistingTabs();
		}
	}
	
	/**
	 * Displays tabs for the current working copy
	 */
	protected void displayTabs() {
		// Turn on initializing flag to ignore message updates
		setInitializingTabs(true);

		ILaunchConfigurationType type = null;
		try {
			type = getWorkingCopy().getType();
			showTabsFor(type);
		} catch (CoreException e) {
			errorDialog(e);
			setInitializingTabs(false);
			return;
		}

		// Update the name field before to avoid verify error
		getNameWidget().setText(getWorkingCopy().getName());

		// Retrieve the current tab group.  If there is none, clean up and leave
		ILaunchConfigurationTabGroup tabGroup = getTabGroup();
		if (tabGroup == null) {
			IStatus status = new Status(IStatus.ERROR, DebugUIPlugin.getUniqueIdentifier(), 0, MessageFormat.format("No tabs defined for launch configuration type {0}", new String[]{type.getName()}), null);
			CoreException e = new CoreException(status);
			errorDialog(e);
			setInitializingTabs(false);
			return;
		}

		// Update the tabs with the new working copy
		tabGroup.initializeFrom(getWorkingCopy());

		// Update the name field after in case client changed it
		getNameWidget().setText(getWorkingCopy().getName());
		
		fCurrentTabIndex = getTabFolder().getSelectionIndex();

		// Turn off initializing flag to update message
		setInitializingTabs(false);
		
		if (!getControl().isVisible()) {
			getControl().setVisible(true);
		}

		refreshStatus();		
	}
	
	/**
	 * Populate the tabs in the configuration edit area to be appropriate to the current
	 * launch configuration type.
	 */
	private void showTabsFor(ILaunchConfigurationType configType) {

		// Don't do any work if the current tabs are for the current config type
		if (getTabType() != null && getTabType().equals(configType)) {
			return;
		}

		// Avoid flicker
		getControl().setRedraw(false);

		// Dispose the current tabs
		disposeExistingTabs();

		// Build the new tabs
		ILaunchConfigurationTabGroup group = null;
		try {
			group = createGroup(configType);
			setTabGroup(group);
		} catch (CoreException ce) {
			DebugUIPlugin.errorDialog(getShell(), LaunchConfigurationsMessages.getString("LaunchConfigurationDialog.Error_19"), LaunchConfigurationsMessages.getString("LaunchConfigurationDialog.Exception_occurred_creating_launch_configuration_tabs_27"),ce); //$NON-NLS-1$ //$NON-NLS-2$
			return;
		}

		// Create the Control for each tab
		ILaunchConfigurationTab[] tabs = group.getTabs();
		for (int i = 0; i < tabs.length; i++) {
			TabItem tab = new TabItem(getTabFolder(), SWT.NONE);
			String name = tabs[i].getName();
			if (name == null) {
				name = LaunchConfigurationsMessages.getString("LaunchConfigurationDialog.unspecified_28"); //$NON-NLS-1$
			}
			tab.setText(name);
			Image image = tabs[i].getImage();
			tab.setImage(image);
			tabs[i].createControl(tab.getParent());
			Control control = tabs[i].getControl();
			if (control != null) {
				tab.setControl(control);
			}
		}
		
		setTabGroup(group);
		setTabType(configType);
		getControl().setRedraw(true);
	}	
	
	/**
	 * Returns tab group for the given type of launch configuration.
	 * Tabs are initialized to be contained in this dialog.
	 *
	 * @exception CoreException if unable to instantiate a tab group
	 */
	protected ILaunchConfigurationTabGroup createGroup(final ILaunchConfigurationType configType) throws CoreException {
		// Use a final Object array to store the tab group and any exception that
		// results from the Runnable
		final Object[] finalArray = new Object[2];
		Runnable runnable = new Runnable() {
			public void run() {
				ILaunchConfigurationTabGroup tabGroup = null;
				try {
					tabGroup = LaunchConfigurationPresentationManager.getDefault().getTabGroup(configType);
					finalArray[0] = tabGroup;
				} catch (CoreException ce) {
					finalArray[1] = ce;
					return;
				}
				tabGroup.createTabs(getLaunchConfigurationDialog(), getLaunchConfigurationDialog().getMode());
				ILaunchConfigurationTab[] tabs = tabGroup.getTabs();
				for (int i = 0; i < tabs.length; i++) {
					tabs[i].setLaunchConfigurationDialog(getLaunchConfigurationDialog());
				}
			}
		};

		// Creating the tabs can result in plugin loading, so we show the busy cursor
		BusyIndicator.showWhile(getControl().getDisplay(), runnable);

		// Re-throw any CoreException if there was one
		if (finalArray[1] != null) {
			throw (CoreException)finalArray[1];
		}

		// Otherwise return the tab group
		return (ILaunchConfigurationTabGroup)finalArray[0];
	}	

	/**
	 * @see org.eclipse.jface.viewers.Viewer#setSelection(org.eclipse.jface.viewers.ISelection, boolean)
	 */
	public void setSelection(ISelection selection, boolean reveal) {
		if (getWorkingCopy() != null) {
			if (selection instanceof IStructuredSelection) {
				IStructuredSelection structuredSelection = (IStructuredSelection)selection;
				Object object = structuredSelection.getFirstElement();
				if (object instanceof ILaunchConfigurationTab) {
					ILaunchConfigurationTab[] tabs = getTabs();
					for (int i = 0; i < tabs.length; i++) {
						ILaunchConfigurationTab tab = tabs[i];
						if (tab.equals(object)) {
							fCurrentTabIndex = i;
							getTabFolder().setSelection(i);
						}
						return;
					}
				}
			}
		}
			
	}

	/**
	 * Returns the tabs currently being displayed, or
	 * <code>null</code> if none.
	 *
	 * @return currently displayed tabs, or <code>null</code>
	 */
	public ILaunchConfigurationTab[] getTabs() {
		if (getTabGroup() != null) {
			return getTabGroup().getTabs();
		}
		return null;
	}

	/**
	 * Returns the currently active <code>ILaunchConfigurationTab</code>
	 * being displayed, or <code>null</code> if there is none.
	 *
	 * @return currently active <code>ILaunchConfigurationTab</code>, or <code>null</code>.
	 */
	public ILaunchConfigurationTab getActiveTab() {
		TabFolder folder = getTabFolder();
		ILaunchConfigurationTab[] tabs = getTabs();
		if (folder != null && tabs != null) {
			int pageIndex = folder.getSelectionIndex();
			if (pageIndex >= 0) {
				return tabs[pageIndex];
			}
		}
		return null;
	}
	
	/**
	 * Returns whether the launch configuration being edited is dirty (i.e.
	 * needs saving)
	 * 
	 * @return whether the launch configuration being edited needs saving
	 */
	public boolean isDirty() {
		ILaunchConfigurationWorkingCopy workingCopy = getWorkingCopy();
		if (workingCopy == null) {
			return false;
		}

		// Working copy hasn't been saved
		if (workingCopy.getOriginal() == null) {
			return true;
		}

		ILaunchConfiguration original = getOriginal();
		return !original.contentsEqual(workingCopy);
	}
	
	/**
	 * Update apply & revert buttons, as well as buttons and message on the
	 * launch config dialog.
	 */
	protected void refreshStatus() {
		getLaunchConfigurationDialog().updateButtons();
		getLaunchConfigurationDialog().updateMessage();
	}	
	
	/**
	 * Returns the containing launch dialog
	 */
	protected ILaunchConfigurationDialog getLaunchConfigurationDialog() {
		return fDialog;
	}
	
	/**
	 * Sets the launch configuration being displayed/edited, possilby
	 * <code>null</code>.
	 */
	private void setOriginal(ILaunchConfiguration configuration) {
		fOriginal = configuration;
	}
	
	/**
	 * Returns the original launch configuration being edited, possibly
	 * <code>null</code>.
	 * 
	 * @return ILaunchConfiguration
	 */
	protected ILaunchConfiguration getOriginal() {
		return fOriginal;
	}
	
	/**
	 * Sets the working copy used to edit the original.
	 */
	private void setWorkingCopy(ILaunchConfigurationWorkingCopy workingCopy) {
		fWorkingCopy = workingCopy;
	}
	
	/**
	 * Returns the working copy used to edit the original, possibly
	 * <code>null</code>.
 	 */
	protected ILaunchConfigurationWorkingCopy getWorkingCopy() {
		return fWorkingCopy;
	}
	
	/**
	 * Return whether the current configuration can be saved.
	 * <p>
	 * Note this is NOT the same thing as the config simply being valid. It
	 * is possible to save a config that does not validate. This method
	 * determines whether the config can be saved without causing a serious
	 * error. For example, a shared config that has no specified location would
	 * cause this method to return <code>false</code>.
	 * </p>
	 */
	public boolean canSave() {
		// First make sure that name doesn't prevent saving the config
		try {
			verifyName();
		} catch (CoreException ce) {
			return false;
		}

		// Next, make sure none of the tabs object to saving the config
		ILaunchConfigurationTab[] tabs = getTabs();
		if (tabs == null) {
			return false;
		}
		for (int i = 0; i < tabs.length; i++) {
			if (!tabs[i].canSave()) {
				return false;
			}
		}
		return true;
	}	
	
	/**
	 * @see ILaunchConfigurationDialog#canLaunch()
	 */
	public boolean canLaunch() {
		if (getWorkingCopy() == null) {
			return false;
		}
		try {
			verifyName();
		} catch (CoreException e) {
			return false;
		}

		ILaunchConfigurationTab[] tabs = getTabs();
		if (tabs == null) {
			return false;
		}
		for (int i = 0; i < tabs.length; i++) {
			if (!tabs[i].isValid(getWorkingCopy())) {
				return false;
			}
		}
		return true;
	}	
	
	/**
	 * Returns the current error message or <code>null</code> if none.
	 */
	public String getErrorMesssage() {
		if (getWorkingCopy() == null) {
			return null;
		}
		try {
			verifyName();
		} catch (CoreException ce) {
			return ce.getStatus().getMessage();
		}
	
		String message = null;
		ILaunchConfigurationTab activeTab = getActiveTab();
		if (activeTab == null) {
			return null;
		} else {
			activeTab.isValid(getWorkingCopy());
			message = activeTab.getErrorMessage();
		}
		if (message != null) {
			return message;
		}
		
		ILaunchConfigurationTab[] allTabs = getTabs();
		for (int i = 0; i < allTabs.length; i++) {
			ILaunchConfigurationTab tab = allTabs[i];
			if (tab == activeTab) {
				continue;
			}
			tab.isValid(getWorkingCopy());
			message = tab.getErrorMessage();
			if (message != null) {
				message = '[' + removeAmpersandsFrom(tab.getName()) + "]: " + message; //$NON-NLS-1$
				return message;
			}
		}
		return null;
	}
	
	/**
	 * Return a copy of the specified string without ampersands.
	 */
	private String removeAmpersandsFrom(String string) {
		String newString = new String(string);
		int index = newString.indexOf('&');
		while (index != -1) {
			newString = string.substring(0, index) + newString.substring(index + 1, newString.length());
			index = newString.indexOf('&');
		}
		return newString;
	}	
	
	/**
	 * Returns the current message or <code>null</code> if none.
	 */
	public String getMesssage() {
		ILaunchConfigurationTab tab = getActiveTab();
		if (tab == null) {
			return null;
		} else {
			return tab.getMessage();
		}
	}	
		
	/**
	 * Verify that the launch configuration name is valid.
	 */
	protected void verifyName() throws CoreException {
		String currentName = getNameWidget().getText().trim();

		// If there is no name, complain
		if (currentName.length() < 1) {
			throw new CoreException(new Status(IStatus.ERROR,
												 DebugUIPlugin.getUniqueIdentifier(),
												 0,
												 LaunchConfigurationsMessages.getString("LaunchConfigurationDialog.Name_required_for_launch_configuration_11"), //$NON-NLS-1$
												 null));
		}

		// See if name contains any 'illegal' characters
		IStatus status = ResourcesPlugin.getWorkspace().validateName(currentName, IResource.FILE);
		if (status.getCode() != IStatus.OK) {
			throw new CoreException(new Status(IStatus.ERROR,
												 DebugUIPlugin.getDefault().getDescriptor().getUniqueIdentifier(),
												 0,
												 status.getMessage(),
												 null));
		}

		// Otherwise, if there's already a config with the same name, complain
		if (!getOriginal().getName().equals(currentName)) {
			if (getLaunchManager().isExistingLaunchConfigurationName(currentName)) {
				throw new CoreException(new Status(IStatus.ERROR,
													 DebugUIPlugin.getDefault().getDescriptor().getUniqueIdentifier(),
													 0,
													 LaunchConfigurationsMessages.getString("LaunchConfigurationDialog.Launch_configuration_already_exists_with_this_name_12"), //$NON-NLS-1$
													 null));
			}
		}
	}
	
	private void setDisposingTabs(boolean disposing) {
		fDisposingTabs = disposing;
	}

	private boolean isDisposingTabs() {
		return fDisposingTabs;
	}
	
	private void setInitializingTabs(boolean initializing) {
		fInitializingTabs = initializing;
	}

	private boolean isInitializingTabs() {
		return fInitializingTabs;
	}		
	
	private void disposeExistingTabs() {
		setDisposingTabs(true);
		TabItem[] oldTabs = getTabFolder().getItems();
		for (int i = 0; i < oldTabs.length; i++) {
			oldTabs[i].dispose();
		}
		disposeTabGroup();
		setDisposingTabs(false);
	}	
	
	private ILaunchManager getLaunchManager() {
		return DebugPlugin.getDefault().getLaunchManager();
	}
	
	/**
	 * Returns the type that tabs are currently displayed
	 * for, or <code>null</code> if none.
	 *
	 * @return launch configuration type or <code>null</code>
	 */
	private ILaunchConfigurationType getTabType() {
		return fTabType;
	}

	/**
	 * Sets the type that tabs are currently displayed
	 * for, or <code>null</code> if none.
	 *
	 * @param tabType launch configuration type
	 */
	private void setTabType(ILaunchConfigurationType tabType) {
		fTabType = tabType;
	}	
	
	/**
	 * Sets the current tab group being displayed
	 *
	 * @param group the current tab group being displayed
	 */
	private void setTabGroup(ILaunchConfigurationTabGroup group) {
		fTabGroup = group;
	}

	/**
	 * Returns the current tab group
	 *
	 * @return the current tab group, or <code>null</code> if none
	 */
	public ILaunchConfigurationTabGroup getTabGroup() {
		return fTabGroup;
	}
	
	/**
	 * Notification that a tab has been selected
	 *
	 * Disallow tab changing when the current tab is invalid.
	 * Update the config from the tab being left, and refresh
	 * the tab being entered.
	 */
	protected void handleTabSelected() {
		if (isDisposingTabs()) {
			return;
		}
		ILaunchConfigurationTab[] tabs = getTabs();
		if (fCurrentTabIndex == getTabFolder().getSelectionIndex() || tabs == null || tabs.length == 0 || fCurrentTabIndex > (tabs.length - 1)) {
			return;
		}
		if (fCurrentTabIndex != -1) {
			ILaunchConfigurationTab tab = tabs[fCurrentTabIndex];
			ILaunchConfigurationWorkingCopy wc = getWorkingCopy();
			if (wc != null) {
				setInitializingTabs(true);
				// apply changes when leaving a tab
				tab.performApply(wc);
				// re-initialize a tab when entering it
				getActiveTab().initializeFrom(wc);
				setInitializingTabs(false);
			}
		}
		fCurrentTabIndex = getTabFolder().getSelectionIndex();
		SelectionChangedEvent event = new SelectionChangedEvent(this, getSelection());
		fireSelectionChanged(event);
	}
	
	/**
	 * Notification the name field has been modified
	 */
	protected void handleNameModified() {
		getWorkingCopy().rename(getNameWidget().getText().trim());
		refreshStatus();
	}		
	
	/**
	 * Notification that the 'Apply' button has been pressed
	 */
	protected void handleApplyPressed() {
		try {
			// trim name
			Text widget = getNameWidget();
			String name = widget.getText();
			String trimmed = name.trim();

			// update launch config
			setInitializingTabs(true);
			if (!name.equals(trimmed)) {
				widget.setText(trimmed);
			}
			getWorkingCopy().rename(trimmed);
			getTabGroup().performApply(getWorkingCopy());
			setInitializingTabs(false);
			//
			
			if (isDirty()) {
				getWorkingCopy().doSave();
			}
			updateButtons();
		} catch (CoreException e) {
			DebugUIPlugin.errorDialog(getShell(), LaunchConfigurationsMessages.getString("LaunchConfigurationDialog.Launch_Configuration_Error_46"), LaunchConfigurationsMessages.getString("LaunchConfigurationDialog.Exception_occurred_while_saving_launch_configuration_47"), e); //$NON-NLS-1$ //$NON-NLS-2$
			return;
		}
	}

	/**
	 * Notification that the 'Revert' button has been pressed
	 */
	protected void handleRevertPressed() {
		inputChanged(getOriginal());
	}	
	
	/**
	 * Show an error dialog on the given exception.
	 *
	 * @param exception
	 */
	protected void errorDialog(CoreException exception) {
		ErrorDialog.openError(getShell(), null, null, exception.getStatus());
	}	
}
