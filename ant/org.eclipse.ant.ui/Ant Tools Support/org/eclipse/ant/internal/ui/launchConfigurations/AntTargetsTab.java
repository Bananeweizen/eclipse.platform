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
package org.eclipse.ant.internal.ui.launchConfigurations;


import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.eclipse.ant.core.TargetInfo;
import org.eclipse.ant.internal.ui.model.AntUIImages;
import org.eclipse.ant.internal.ui.model.AntUIPlugin;
import org.eclipse.ant.internal.ui.model.AntUtil;
import org.eclipse.ant.internal.ui.model.IAntUIConstants;
import org.eclipse.ant.internal.ui.model.IAntUIHelpContextIds;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.externaltools.internal.launchConfigurations.ExternalToolsUtil;
import org.eclipse.ui.externaltools.internal.model.IExternalToolConstants;
import org.eclipse.ui.help.WorkbenchHelp;

public class AntTargetsTab extends AbstractLaunchConfigurationTab {
	
	private TargetInfo fDefaultTarget = null;
	private TargetInfo[] fAllTargets= null;
	private List fOrderedTargets = null;
	
	private String fLocation= null;
	
	private CheckboxTableViewer fTableViewer = null;
	private Label fSelectionCountLabel = null;
	private Text fTargetOrderText = null;
	private Button fOrderButton = null;
	private Button fFilterInternalTargets;
	private Button fSortButton;
	
	private ILaunchConfiguration fLaunchConfiguration;
	private AntTargetContentProvider fTargetContentProvider;
	private int fSortColumn= 0;
	
	/**
	 * Sort constants.
	 */
	public static int SORT_NONE= 0;
	public static int SORT_NAME= 1;
	public static int SORT_NAME_REVERSE= -1;
	public static int SORT_DESCRIPTION= 2;
	public static int SORT_DESCRIPTION_REVERSE= -2;
	
	private class AntTargetsSorter extends ViewerSorter {
		/**
		 * @see org.eclipse.jface.viewers.ViewerSorter#compare(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
		 */
		public int compare(Viewer viewer, Object e1, Object e2) {
			if (!(e1 instanceof TargetInfo && e2 instanceof TargetInfo)) {
				return super.compare(viewer, e1, e2);
			}
			if (fSortColumn == SORT_NONE) {
				return 0;
			}
			String string1, string2;
			int result= 0;
			if (fSortColumn == SORT_NAME || fSortColumn == SORT_NAME_REVERSE) {
				string1= ((TargetInfo) e1).getName();
				string2= ((TargetInfo) e2).getName();
			} else {
				string1= ((TargetInfo) e1).getDescription();
				string2= ((TargetInfo) e2).getDescription();
			}
			if (string1 != null && string2 != null) {
				result= getCollator().compare(string1, string2);
			} else if (string1 == null) {
				result= 1;
			} else if (string2 == null) {
				result= -1;
			}
			if (fSortColumn < 0) { // reverse sort
				if (result == 0) {
					result= -1;
				} else {
					result= -result;
				}
			}
			return result;
		}
	}
	
	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Font font = parent.getFont();
		
		Composite comp = new Composite(parent, SWT.NONE);
		setControl(comp);
		WorkbenchHelp.setHelp(getControl(), IAntUIHelpContextIds.ANT_TARGETS_TAB);
		GridLayout topLayout = new GridLayout();
		comp.setLayout(topLayout);		
		GridData gd = new GridData(GridData.FILL_BOTH);
		comp.setLayoutData(gd);
		comp.setFont(font);
		
		createTargetsTable(comp);
		createSelectionCount(comp);
		createSortTargets(comp);
		createFilterInternalTargets(comp);
		createVerticalSpacer(comp, 1);
		createTargetOrder(comp);
	}
	
	/**
	 * Creates the selection count widget
	 * @param parent the parent composite
	 */
	private void createSelectionCount(Composite parent) {
		GridData gd;
		fSelectionCountLabel = new Label(parent, SWT.NONE);
		fSelectionCountLabel.setFont(parent.getFont());
		fSelectionCountLabel.setText(AntLaunchConfigurationMessages.getString("AntTargetsTab.0_out_of_0_selected_2")); //$NON-NLS-1$
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fSelectionCountLabel.setLayoutData(gd);
	}

	/**
	 * Creates the widgets that display the target order
	 * @param parent the parent composite
	 */
	private void createTargetOrder(Composite parent) {
		Font font= parent.getFont();
		
		Label label = new Label(parent, SWT.NONE);
		label.setText(AntLaunchConfigurationMessages.getString("AntTargetsTab.Target_execution_order__3")); //$NON-NLS-1$
		label.setFont(font);
		
		Composite orderComposite = new Composite(parent, SWT.NONE);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		orderComposite.setLayoutData(gd);
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		orderComposite.setLayout(layout);
		orderComposite.setFont(font);
				
		fTargetOrderText = new Text(orderComposite, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL | SWT.READ_ONLY);
		fTargetOrderText.setFont(font);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = 40;
		gd.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		fTargetOrderText.setLayoutData(gd);

		fOrderButton = createPushButton(orderComposite, AntLaunchConfigurationMessages.getString("AntTargetsTab.&Order..._4"), null); //$NON-NLS-1$
		gd = (GridData)fOrderButton.getLayoutData();
		gd.verticalAlignment = GridData.BEGINNING;
		fOrderButton.setFont(font);
		fOrderButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleOrderPressed();
			}
		});
	}

	private void createFilterInternalTargets(Composite parent) {
		fFilterInternalTargets= new Button(parent, SWT.CHECK);
		fFilterInternalTargets.setText(AntLaunchConfigurationMessages.getString("AntTargetsTab.12")); //$NON-NLS-1$
		fFilterInternalTargets.setFont(parent.getFont());
		fFilterInternalTargets.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleFilterTargetsSelected();
			}
		});
	}
	
	private void createSortTargets(Composite parent) {
		fSortButton= new Button(parent, SWT.CHECK);
		fSortButton.setText(AntLaunchConfigurationMessages.getString("AntTargetsTab.14")); //$NON-NLS-1$
		fSortButton.setFont(parent.getFont());
		fSortButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleSortTargetsSelected();
			}
		});
	}
	
	private void handleFilterTargetsSelected() {
		boolean filter= fFilterInternalTargets.getSelection();
		fTargetContentProvider.setFilterInternalTargets(filter);
		if (filter) {
			ListIterator iter= fOrderedTargets.listIterator();
			while (iter.hasNext()) {
				TargetInfo target= (TargetInfo) iter.next();
				if (fTargetContentProvider.isInternal(target)) {
					iter.remove();
				}
			}
		}
		fTableViewer.refresh();
		// Must refresh before updating selection count because the selection
		// count's "hidden" reporting needs the content provider to be queried
		// first to count how many targets are hidden.
		updateSelectionCount();
		updateLaunchConfigurationDialog();
	}
	
	/**
	 * The button to sort targets has been toggled.
	 * Set the tab's sorting as appropriate.
	 */
	private void handleSortTargetsSelected() {
		setSort(fSortButton.getSelection() ? SORT_NAME : SORT_NONE);
	}
	
	/**
	 * Sets the sorting of targets in this tab. See the sort constants defined
	 * above.
	 * 
	 * @param column the column which should be sorted on
	 */
	private void setSort(int column) {
		fSortColumn= column;
		fTableViewer.refresh();
		updateLaunchConfigurationDialog();
	}

	private void handleOrderPressed() {
		TargetOrderDialog dialog = new TargetOrderDialog(getShell(), fOrderedTargets.toArray());
		int ok = dialog.open();
		if (ok == Window.OK) {
			fOrderedTargets.clear();
			Object[] targets = dialog.getTargets();
			for (int i = 0; i < targets.length; i++) {
				fOrderedTargets.add(targets[i]);
				updateSelectionCount();
				updateLaunchConfigurationDialog();
			}
		}
	}
	
	private void createTargetsTable(Composite parent) {
		Font font= parent.getFont();
		Label label = new Label(parent, SWT.NONE);
		label.setFont(font);
		label.setText(AntLaunchConfigurationMessages.getString("AntTargetsTab.Check_targets_to_e&xecute__1")); //$NON-NLS-1$
				
		Table table= new Table(parent, SWT.CHECK | SWT.BORDER | SWT.FULL_SELECTION | SWT.RESIZE);
		
		GridData data= new GridData(GridData.FILL_BOTH);
		data.heightHint = 200;
		data.widthHint = 250;
		table.setLayoutData(data);
		table.setFont(font);
				
		table.setHeaderVisible(true);
		table.setLinesVisible(true);		

		TableLayout tableLayout= new TableLayout();
		ColumnWeightData weightData = new ColumnWeightData(30, true);
		tableLayout.addColumnData(weightData);
		weightData = new ColumnWeightData(70, true);
		tableLayout.addColumnData(weightData);		
		table.setLayout(tableLayout);

		TableColumn column1= new TableColumn(table, SWT.NULL);
		column1.setText(AntLaunchConfigurationMessages.getString("AntTargetsTab.Name_5")); //$NON-NLS-1$
			
		TableColumn column2= new TableColumn(table, SWT.NULL);
		column2.setText(AntLaunchConfigurationMessages.getString("AntTargetsTab.Description_6")); //$NON-NLS-1$
		
		fTableViewer = new CheckboxTableViewer(table);
		fTableViewer.setLabelProvider(new TargetTableLabelProvider());
		fTargetContentProvider= new AntTargetContentProvider();
		fTableViewer.setContentProvider(fTargetContentProvider);
		fTableViewer.setSorter(new AntTargetsSorter());
		
		fTableViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				ISelection selection= event.getSelection();
				if (selection instanceof IStructuredSelection) {
					IStructuredSelection ss= (IStructuredSelection)selection;
					Object element= ss.getFirstElement();
					boolean checked= !fTableViewer.getChecked(element);
					fTableViewer.setChecked(element, checked);
					updateOrderedTargets(element, checked);
				}
			}
		});
		
		fTableViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				updateOrderedTargets(event.getElement(), event.getChecked());
			}
		});
		
		TableColumn[] columns= fTableViewer.getTable().getColumns();
		for (int i = 0; i < columns.length; i++) {
			final int index= i;
			columns[index].addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					if (fSortButton.getSelection()) {
						// index 0 => sort_name (1)
						// index 1 => sort_description (2)
						int column= index + 1;
						if (column == fSortColumn) {
							column= -column; // invert the sort when the same column is selected twice in a row
						}
						setSort(column);
					}
				}
			});
		}
	}
	
	/**
	 * Updates the ordered targets list in response to an element being checked
	 * or unchecked. When the element is checked, it's added to the list. When
	 * unchecked, it's removed.
	 * 
	 * @param element the element in question
	 * @param checked whether the element has been checked or unchecked
	 */
	private void updateOrderedTargets(Object element , boolean checked) {
		if (checked) {
			 fOrderedTargets.add(element);
		} else {
			fOrderedTargets.remove(element);
		}	 
		updateSelectionCount();
		updateLaunchConfigurationDialog();	
	}
	
	/**
	 * Updates the selection count widget to display how many targets are
	 * selected (example, "1 out of 6 selected").
	 */
	private void updateSelectionCount() {
		Object[] checked = fTableViewer.getCheckedElements();
		String numSelected = Integer.toString(checked.length);
		int length= fTargetContentProvider.getNumTargets();
		String total = Integer.toString(length);
		int numHidden= fTargetContentProvider.getNumFiltered();
		if (numHidden > 0) {
			fSelectionCountLabel.setText(MessageFormat.format(AntLaunchConfigurationMessages.getString("AntTargetsTab.13"), new String[]{numSelected, total, String.valueOf(numHidden)})); //$NON-NLS-1$
		} else {
			fSelectionCountLabel.setText(MessageFormat.format(AntLaunchConfigurationMessages.getString("AntTargetsTab.{0}_out_of_{1}_selected_7"), new String[]{numSelected, total})); //$NON-NLS-1$
		}
		
		fOrderButton.setEnabled(checked.length > 1);
		
		StringBuffer buffer = new StringBuffer();
		Iterator iter = fOrderedTargets.iterator();
		while (iter.hasNext()) {
			buffer.append(((TargetInfo)iter.next()).getName());
			buffer.append(", "); //$NON-NLS-1$
		}
		if (buffer.length() > 2) {
			// remove trailing comma
			buffer.setLength(buffer.length() - 2);
		}
		fTargetOrderText.setText(buffer.toString());
	}
	
	private TargetInfo[] getTargets() {
		if (fAllTargets == null || isDirty()) {
			fAllTargets= null;
			setDirty(false);
			setErrorMessage(null);
			
			String location = null;
			try {
				location = DebugPlugin.getDefault().getStringVariableManager().performStringSubstitution(fLocation);
			} catch (CoreException e1) {
				setErrorMessage(e1.getMessage());
			}
			final String expandedLocation = location;
			if (expandedLocation != null) {
				final CoreException[] exceptions= new CoreException[1];
				try {
					final String[] arguments = ExternalToolsUtil.getArguments(fLaunchConfiguration);
					IRunnableWithProgress operation= new IRunnableWithProgress() {
						/* (non-Javadoc)
						 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
						 */
						public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
							try {
								fAllTargets = AntUtil.getTargets(expandedLocation, arguments, fLaunchConfiguration);
							} catch (CoreException ce) {
								exceptions[0]= ce;
							}
						}
					};
					
					getLaunchConfigurationDialog().run(false, false, operation);
				} catch (CoreException ce) {
					exceptions[0]= ce;
				} catch (InvocationTargetException e) {
				} catch (InterruptedException e) {
				}
				
				if (exceptions[0] != null) {
					IStatus exceptionStatus= exceptions[0].getStatus();
					IStatus[] children= exceptionStatus.getChildren();
					StringBuffer message= new StringBuffer(exceptions[0].getMessage());
					for (int i = 0; i < children.length; i++) {
						message.append(' ');
						IStatus childStatus = children[i];
						message.append(childStatus.getMessage());
					}
					setErrorMessage(message.toString());
					fAllTargets= null;
					return fAllTargets;
				}
				for (int i=0; i < fAllTargets.length; i++) {
					if (fAllTargets[i].isDefault()) {
						fDefaultTarget = fAllTargets[i];
						break;
					}
				}
			}
		}
		return fAllTargets;
	}
	
	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration configuration) {
		fLaunchConfiguration= configuration;
		setErrorMessage(null);
		setMessage(null);
		boolean hideInternal= false;
		try {
			hideInternal = fLaunchConfiguration.getAttribute(IAntLaunchConfigurationConstants.ATTR_HIDE_INTERNAL_TARGETS, false);
		} catch (CoreException e) {
			AntUIPlugin.log(e);
		}
		fFilterInternalTargets.setSelection(hideInternal);
		fTargetContentProvider.setFilterInternalTargets(hideInternal);
		int sort= SORT_NONE;
		try {
			sort = fLaunchConfiguration.getAttribute(IAntLaunchConfigurationConstants.ATTR_SORT_TARGETS, sort);
		} catch (CoreException e) {
			AntUIPlugin.log(e);
		}
		fSortButton.setSelection(sort != SORT_NONE);
		setSort(sort);
		String configTargets= null;
		String newLocation= null;
		fOrderedTargets = new ArrayList();
		try {
			configTargets= configuration.getAttribute(IAntLaunchConfigurationConstants.ATTR_ANT_TARGETS, (String)null);
			newLocation= configuration.getAttribute(IExternalToolConstants.ATTR_LOCATION, (String)null);
		} catch (CoreException ce) {
			AntUIPlugin.log(AntLaunchConfigurationMessages.getString("AntTargetsTab.Error_reading_configuration_12"), ce); //$NON-NLS-1$
		}
		
		if (newLocation == null) {
			fAllTargets= null;
			fLocation= newLocation;
			setExecuteInput(new TargetInfo[0]);
			fTableViewer.setInput(new TargetInfo[0]);
			return; 
		}
		
		if (!newLocation.equals(fLocation)) {
			fAllTargets= null;
			fLocation= newLocation;
		}
		
		TargetInfo[] allInfos= getTargets();
		if (allInfos == null) {
			setExecuteInput(new TargetInfo[0]);
			fTableViewer.setInput(new TargetInfo[0]);
			return; 
		}
		
		String[] targetNames= AntUtil.parseRunTargets(configTargets);
		if (targetNames.length == 0) {
			fOrderedTargets.add(fDefaultTarget);
			fTableViewer.setAllChecked(false);
			setExecuteInput(allInfos);
			if (fDefaultTarget != null) {
				fTableViewer.setChecked(fDefaultTarget, true);
			}
			updateSelectionCount();
			return;
		}
		
		setExecuteInput(allInfos);
		fTableViewer.setAllChecked(false);
		for (int i = 0; i < targetNames.length; i++) {
			for (int j = 0; j < fAllTargets.length; j++) {
				if (targetNames[i].equals(fAllTargets[j].getName())) {
					fOrderedTargets.add(fAllTargets[j]);
					fTableViewer.setChecked(fAllTargets[j], true);
				}
			}
		}
		updateSelectionCount();
	}
	
	/**
	 * Sets the execute table's input to the given input.
	 */
	private void setExecuteInput(Object input) {
		fTableViewer.setInput(input);
		updateSelectionCount();
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		//	attribute added in 3.0, so null must be used instead of false for backwards compatibility
		if (fFilterInternalTargets.getSelection()) {
			configuration.setAttribute(IAntLaunchConfigurationConstants.ATTR_HIDE_INTERNAL_TARGETS, true);
		} else {
			configuration.setAttribute(IAntLaunchConfigurationConstants.ATTR_HIDE_INTERNAL_TARGETS, (String)null);
		}
		configuration.setAttribute(IAntLaunchConfigurationConstants.ATTR_SORT_TARGETS, fSortColumn);
		if (fOrderedTargets.size() == 1) {
			TargetInfo item = (TargetInfo)fOrderedTargets.get(0);
			if (item.isDefault()) {
				configuration.setAttribute(IAntLaunchConfigurationConstants.ATTR_ANT_TARGETS, (String)null);
				return;
			}
		} else if (fOrderedTargets.size() == 0) {
			configuration.setAttribute(IAntLaunchConfigurationConstants.ATTR_ANT_TARGETS, (String)null);
			return;
		}
		
		StringBuffer buff= new StringBuffer();
		Iterator iter = fOrderedTargets.iterator();
		String targets = null;
		while (iter.hasNext()) {
			TargetInfo item = (TargetInfo)iter.next();
			buff.append(item.getName());
			buff.append(',');
		}
		if (buff.length() > 0) {
			targets= buff.toString();
		}  

		configuration.setAttribute(IAntLaunchConfigurationConstants.ATTR_ANT_TARGETS, targets);
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
	 */
	public String getName() {
		return AntLaunchConfigurationMessages.getString("AntTargetsTab.Tar&gets_14"); //$NON-NLS-1$
	}
		
	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getImage()
	 */
	public Image getImage() {
		return AntUIImages.getImage(IAntUIConstants.IMG_TAB_ANT_TARGETS);
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#isValid(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public boolean isValid(ILaunchConfiguration launchConfig) {
		if (fAllTargets == null && getErrorMessage() != null) {
			//error in parsing;
			return false;
		}
		setErrorMessage(null);
		setMessage(null);
		
		if (fAllTargets != null && fTableViewer.getCheckedElements().length == 0) {
			setErrorMessage(AntLaunchConfigurationMessages.getString("AntTargetsTab.No_targets")); //$NON-NLS-1$
			return false;
		}
		return super.isValid(launchConfig);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#setDirty(boolean)
	 */
	protected void setDirty(boolean dirty) {
		//provide package visibility
		super.setDirty(dirty);
	}
}
