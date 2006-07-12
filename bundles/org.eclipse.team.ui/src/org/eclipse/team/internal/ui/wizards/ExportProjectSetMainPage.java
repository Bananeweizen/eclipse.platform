/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.wizards;

import java.util.*;
import java.util.List;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.internal.ui.*;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.IWorkingSetSelectionDialog;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.part.PageBook;

public class ExportProjectSetMainPage extends TeamWizardPage {

	
	PageBook book;
	ProjectPage projectPage;
	WorkingSetPage workingSetPage;
	
	IExportProjectSetPage selectedPage;
	
	Button exportWorkingSets;
	
	ArrayList passedInSelectedProjects = new ArrayList();
	
	class ProjectContentProvider implements ITreePathContentProvider{

		public Object[] getChildren(TreePath parentPath) {
			Object obj = parentPath.getLastSegment();
			if (obj instanceof IWorkingSet){
				return ((IWorkingSet)obj).getElements();
			}
			return null;
		}

		public TreePath[] getParents(Object element) {
			if (element instanceof IProject){
				ArrayList treePaths = new ArrayList();
				IWorkingSet[] workingSets = TeamUIPlugin.getPlugin().getWorkbench().getWorkingSetManager().getWorkingSets();
				for (int i = 0; i < workingSets.length; i++) {
					IAdaptable[] elements = workingSets[i].getElements();
					for (int j = 0; j < elements.length; j++) {
						if (elements[j].equals(element)){
							treePaths.add(workingSets[i]);
							break;
						}
					}
				}
				return (TreePath[]) treePaths.toArray(new TreePath[treePaths.size()]);
			}
			return null;
		}

		public boolean hasChildren(TreePath path) {
			Object obj = path.getLastSegment();
			if (obj instanceof IWorkingSet)
				return true;
			
			return false;
		}

		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof IWorkspaceRoot) {
				IWorkspaceRoot root = (IWorkspaceRoot) inputElement;
				List projectList = new ArrayList();
				IProject[] workspaceProjects = root.getProjects();
				for (int i = 0; i < workspaceProjects.length; i++) {
					if (RepositoryProvider.getProvider(workspaceProjects[i]) != null) {
						projectList.add(workspaceProjects[i]);
					}
				}
				return projectList.toArray(new IProject[projectList.size()]);
			} else if (inputElement instanceof IWorkingSetManager){
				IWorkingSetManager manager = (IWorkingSetManager) inputElement;
				IWorkingSet[] allSets = manager.getAllWorkingSets();
				ArrayList resourceSets = new ArrayList();
				for (int i = 0; i < allSets.length; i++) {
					String id = allSets[i].getId();
					if (id != null && id.equals(WorkingSetsDialog.resourceWorkingSetId)) {
						resourceSets.add(allSets[i]);
					}
				}
				
				return resourceSets.toArray(new IWorkingSet[resourceSets.size()]);
			} else if (inputElement instanceof IAdaptable){
				Object tempProject = ((IAdaptable) inputElement).getAdapter(IProject.class);
				if (tempProject != null){
					return new IProject[]{((IProject)tempProject)};
				}
			}
			else if (inputElement instanceof IAdaptable[]){
				IAdaptable[] tempAdaptable = (IAdaptable[]) inputElement;
				List projectList = new ArrayList();
				for (int i = 0; i < tempAdaptable.length; i++) {
					if (tempAdaptable[i].getAdapter(IProject.class) != null){
						projectList.add(tempAdaptable[i]);
					}
				}
				return projectList.toArray(new IProject[projectList.size()]);
			} else if (inputElement instanceof HashSet){
				HashSet tempList = (HashSet) inputElement;
				return (IProject[]) tempList.toArray(new IProject[tempList.size()]);
			}
			
			return null;
		}

		public void dispose() {
			
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			
		}
	
	};
	
	public ExportProjectSetMainPage(String pageName, String title, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
		setDescription(TeamUIMessages.ExportProjectSetMainPage_description);
	}

	/*
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		Composite c = SWTUtils.createHVFillComposite(parent, 0);
		
		//Add the export working set section
		exportWorkingSets(c);
		
		book = new PageBook(c, SWT.NONE);
		book.setLayoutData(SWTUtils.createHVFillGridData());
		// set F1 help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(book, IHelpContextIds.EXPORT_PROJECT_SET_PAGE);
		
		projectPage = new ProjectPage();
		projectPage.createControl(book);
		
		workingSetPage = new WorkingSetPage();
		workingSetPage.createControl(book);

		setControl(c);
		book.showPage(projectPage.getControl());
		
		selectedPage = projectPage;
		//pass in any selected
		projectPage.getSelectedProjects().addAll(passedInSelectedProjects);
		projectPage.getReferenceCountProjects().addAll(passedInSelectedProjects);
		
		Dialog.applyDialogFont(parent);
	}

	


	
	private void exportWorkingSets(Composite composite) {
		exportWorkingSets = new Button(composite, SWT.CHECK | SWT.LEFT);
		exportWorkingSets.setText(TeamUIMessages.ExportProjectSetMainPage_ExportWorkingSets);
	
		exportWorkingSets.setSelection(false);
		exportWorkingSets.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (((Button) e.widget).getSelection()){
					book.showPage(workingSetPage.getControl());
					selectedPage = workingSetPage;
					workingSetPage.refresh();
				}
				else{
					book.showPage(projectPage.getControl());
					selectedPage = projectPage;
				}
			}
		});
	}
	
	public IWorkingSet[] getSelectedWorkingSets(){
		return (IWorkingSet[]) selectedPage.getWorkingSet().toArray(new IWorkingSet[selectedPage.getWorkingSet().size()]);
	}
	
	public IProject[] getSelectedProjects() {
		return (IProject[]) selectedPage.getSelectedProjects().toArray(new IProject[selectedPage.getSelectedProjects().size()]);
	}

	public void setSelectedProjects(IProject[] selectedProjects) {
		passedInSelectedProjects.addAll(Arrays.asList(selectedProjects));
	}
	
	private interface IExportProjectSetPage{
		HashSet getSelectedProjects();
		ArrayList getReferenceCountProjects();
		ArrayList getWorkingSet();
	}
	
	private class ProjectPage extends Page implements IExportProjectSetPage {
		private Composite projectComposite;

		private CheckboxTableViewer tableViewer;
		private Table table;
		
		HashSet selectedProjects = new HashSet();
		ArrayList referenceCountProjects = new ArrayList();
		ArrayList selectedWorkingSet = new ArrayList();

		public void createControl(Composite parent) {
			//projectComposite = createComposite(parent, 1);
			
			projectComposite = new Composite(parent, SWT.NULL);
			
			// GridLayout
			GridLayout layout = new GridLayout();
			layout.numColumns = 1;
			projectComposite.setLayout(layout);
		
			// GridData
			GridData data = new GridData();
			data.verticalAlignment = GridData.FILL;
			data.horizontalAlignment = GridData.FILL;
			data.grabExcessHorizontalSpace=true;
			data.grabExcessVerticalSpace=true;
			projectComposite.setLayoutData(data);
			
			initializeDialogUnits(projectComposite);
			
			//Adds the project table
			addProjectSection(projectComposite);
			initializeProjects();
			updateEnablement();
		}

		public Control getControl() {
			return projectComposite;
		}

		public void setFocus() {
			projectComposite.setFocus();
		}
		
		private void addProjectSection(Composite composite) {
			
			createLabel(composite, TeamUIMessages.ExportProjectSetMainPage_Select_the_projects_to_include_in_the_project_set__2);

			table = new Table(composite, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
			tableViewer = new CheckboxTableViewer(table);
			table.setLayout(new TableLayout());
			GridData data = new GridData(GridData.FILL_BOTH);
			data.heightHint = 300;
			table.setLayoutData(data);
			tableViewer.setContentProvider(new ProjectContentProvider());
			tableViewer.setLabelProvider(new WorkbenchLabelProvider());
			tableViewer.addCheckStateListener(new ICheckStateListener() {
				public void checkStateChanged(CheckStateChangedEvent event) {
					Object temp = event.getElement();
					if (temp instanceof IProject){
						IProject project = (IProject) event.getElement();
						if (event.getChecked()) {
							selectedProjects.add(project);
							referenceCountProjects.add(project);
						} else {
							selectedProjects.remove(project);
							referenceCountProjects.remove(project);
						}
					} else if (temp instanceof IWorkingSet){
						IWorkingSet workingSet = (IWorkingSet) temp;
						if (event.getChecked()){
							IAdaptable[] elements = workingSet.getElements();
							for (int i = 0; i < elements.length; i++) {
								selectedProjects.add(elements[i]);
							}
						} else {
							IAdaptable[] elements = workingSet.getElements();
							for (int i = 0; i < elements.length; i++) {
								selectedProjects.remove(elements[i]);
							}
						}
					}
					updateEnablement();
				}
			});

			Composite buttonComposite = new Composite(composite, SWT.NONE);
			GridLayout layout = new GridLayout();
			layout.numColumns = 2;
			layout.marginWidth = 0;
			buttonComposite.setLayout(layout);
			data = new GridData(SWT.FILL, SWT.FILL, true, false);
			buttonComposite.setLayoutData(data);

			Button selectAll = new Button(buttonComposite, SWT.PUSH);
			data = new GridData();
			data.verticalAlignment = GridData.BEGINNING;
			data.horizontalAlignment = GridData.END;
			int widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
			data.widthHint = Math.max(widthHint, selectAll.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
			selectAll.setLayoutData(data);
			selectAll.setText(TeamUIMessages.ExportProjectSetMainPage_SelectAll);
			selectAll.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					tableViewer.setAllChecked(true);
					selectedProjects.removeAll(selectedProjects);
					Object[] checked = tableViewer.getCheckedElements();
					for (int i = 0; i < checked.length; i++) {
						selectedProjects.add(checked[i]);
					}
					updateEnablement();
				}
			});

			Button deselectAll = new Button(buttonComposite, SWT.PUSH);
			data = new GridData();
			data.verticalAlignment = GridData.BEGINNING;
			data.horizontalAlignment = GridData.END;
			widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
			data.widthHint = Math.max(widthHint, deselectAll.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
			deselectAll.setLayoutData(data);
			deselectAll.setText(TeamUIMessages.ExportProjectSetMainPage_DeselectAll);
			deselectAll.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					tableViewer.setAllChecked(false);
					selectedProjects.removeAll(selectedProjects);
					updateEnablement();
				}
			});
		}
		
		private void initializeProjects() {
			tableViewer.setInput(ResourcesPlugin.getWorkspace().getRoot());
		
			// Check any necessary projects
			if (selectedProjects != null) {
				tableViewer.setCheckedElements(selectedProjects.toArray(new IProject[selectedProjects.size()]));
			}
		}
		
		private void updateEnablement() {
			boolean complete;
			
			complete = (selectedProjects.size() != 0);
			
			if (complete) {
				setMessage(null);
			}
			setPageComplete(complete);
		}

		public ArrayList getReferenceCountProjects() {
			return referenceCountProjects;
		}

		public HashSet getSelectedProjects() {
			return selectedProjects;
		}

		public ArrayList getWorkingSet() {
			return selectedWorkingSet;
		}

	}

	private class WorkingSetPage extends Page implements IExportProjectSetPage {

		private Composite projectComposite;
		private Table wsTable;
		private CheckboxTableViewer wsTableViewer;
		
		private Table table;
		private TableViewer tableViewer;
		

		HashSet selectedProjects = new HashSet();
		ArrayList referenceCountProjects = new ArrayList();
		ArrayList selectedWorkingSet = new ArrayList();
		
		public void createControl(Composite parent) {
		   
			projectComposite = SWTUtils.createHVFillComposite(parent, 0, 2);			
			initializeDialogUnits(projectComposite);

			Label label = createLabel (projectComposite, TeamUIMessages.ExportProjectSetMainPage_SelectButton);
			GridData grid = (GridData) label.getLayoutData();
			grid.horizontalSpan = 2;
			label.setData(grid);
			
			//Adds the working set table
			addWorkingSetSection(projectComposite);
			
			addProjectSection(projectComposite);
			
			addButtons(projectComposite);
			updateEnablement();
		}
		
		private void addProjectSection(Composite composite) {
				
				table = new Table(composite, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
				tableViewer = new TableViewer(table);
				table.setLayout(new TableLayout());
				GridData data = new GridData(GridData.FILL_BOTH);
				data.heightHint = 300;
				table.setLayoutData(data);
				tableViewer.setContentProvider(new ProjectContentProvider());
				tableViewer.setLabelProvider(new WorkbenchLabelProvider());
			}
		
		private void addWorkingSetSection(Composite projectComposite) {

			wsTable = new Table(projectComposite, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
			wsTableViewer = new CheckboxTableViewer(wsTable);
			wsTable.setLayout(new TableLayout());
			GridData data = new GridData(GridData.FILL_BOTH);
			data.heightHint = 300;
			wsTable.setLayoutData(data);
			wsTableViewer.setContentProvider(new ProjectContentProvider());
			wsTableViewer.setLabelProvider(new WorkbenchLabelProvider());
			wsTableViewer.addCheckStateListener(new ICheckStateListener() {
				public void checkStateChanged(CheckStateChangedEvent event) {
					Object temp = event.getElement();
					if (temp instanceof IWorkingSet){
						IWorkingSet workingSet = (IWorkingSet) temp;
						if (event.getChecked()){
							workingSetAdded(workingSet);
							//Add the selected project to the table viewer
							tableViewer.setInput(selectedProjects);
						} else {
							workingSetRemoved(workingSet);
							//Add the selected project to the table viewer
							tableViewer.setInput(selectedProjects);
						}
					}
					updateEnablement();
				}
			});

			wsTableViewer.setInput(TeamUIPlugin.getPlugin().getWorkbench().getWorkingSetManager());
		}

		private void addButtons(Composite projectComposite){
		
			Composite buttonComposite = new Composite(projectComposite, SWT.NONE);
			GridLayout layout = new GridLayout();
			layout.numColumns = 3;
			layout.marginWidth = 0;
			buttonComposite.setLayout(layout);
			GridData data = new GridData(SWT.FILL, SWT.FILL, false, false);
			buttonComposite.setLayoutData(data);
			 
			Button selectAll = new Button(buttonComposite, SWT.PUSH);
			data = new GridData();
			data.verticalAlignment = GridData.FILL;
			data.horizontalAlignment = GridData.FILL;
			int widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
			data.widthHint = Math.max(widthHint, selectAll.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
			selectAll.setLayoutData(data);
			selectAll.setText(TeamUIMessages.ExportProjectSetMainPage_SelectAll);
			selectAll.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					wsTableViewer.setAllChecked(true);
					
					selectedProjects.removeAll(selectedProjects);
					selectedWorkingSet.removeAll(selectedWorkingSet);
					Object[] checked = wsTableViewer.getCheckedElements();
					for (int i = 0; i < checked.length; i++) {
						selectedWorkingSet.add(checked[i]);
						if (checked[i] instanceof IWorkingSet){
							IWorkingSet ws = (IWorkingSet) checked[i];
							IAdaptable[] elements = ws.getElements();
							selectedProjects.addAll(Arrays.asList(elements));
							referenceCountProjects.addAll(Arrays.asList(elements));
						}
						tableViewer.setInput(selectedProjects);
					}
					updateEnablement();
				}
			});

			Button deselectAll = new Button(buttonComposite, SWT.PUSH);
			data = new GridData();
			data.verticalAlignment = GridData.FILL;
			data.horizontalAlignment = GridData.FILL;
			widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
			data.widthHint = Math.max(widthHint, deselectAll.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
			deselectAll.setLayoutData(data);
			deselectAll.setText(TeamUIMessages.ExportProjectSetMainPage_DeselectAll);
			deselectAll.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					wsTableViewer.setAllChecked(false);
					selectedWorkingSet.removeAll(selectedWorkingSet);
					selectedProjects.removeAll(selectedProjects);
					referenceCountProjects.removeAll(selectedProjects);
					tableViewer.setInput(selectedProjects);
					updateEnablement();
				}
			});
			
			Button newWorkingSet = new Button(buttonComposite, SWT.PUSH);
			data = new GridData();
			data.verticalAlignment = GridData.FILL;
			data.horizontalAlignment = GridData.FILL;
			widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
			data.widthHint = Math.max(widthHint, deselectAll.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
			newWorkingSet.setLayoutData(data);
			newWorkingSet.setText(TeamUIMessages.ExportProjectSetMainPage_EditButton);
			newWorkingSet.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					final IWorkingSetManager workingSetManager = TeamUIPlugin.getPlugin().getWorkbench().getWorkingSetManager();
					IWorkingSetSelectionDialog wsWizard = workingSetManager.createWorkingSetSelectionDialog(getShell(), false);
					if (wsWizard != null) {
						IPropertyChangeListener propListener = null;
						try {
							//add event listener
							propListener = new  IPropertyChangeListener() {
								public void propertyChange(PropertyChangeEvent event) {
									
								}};
								
							workingSetManager.addPropertyChangeListener(propListener);
							wsWizard.open();
							//recalculate working sets
							selectedWorkingSet.removeAll(selectedWorkingSet);
							referenceCountProjects.removeAll(selectedProjects);
							selectedProjects.removeAll(selectedProjects);
							wsTableViewer.setInput(workingSetManager);
							Object[] checked = wsTableViewer.getCheckedElements();
							for (int i = 0; i < checked.length; i++) {
								selectedWorkingSet.add(checked[i]);
								if (checked[i] instanceof IWorkingSet) {
									IWorkingSet ws = (IWorkingSet) checked[i];
									IAdaptable[] elements = ws.getElements();
									selectedProjects.addAll(Arrays.asList(elements));
									referenceCountProjects.addAll(Arrays.asList(elements));
								}

							}
						
							wsTableViewer.setInput(workingSetManager);
							tableViewer.setInput(selectedProjects);
						} finally {
							if (propListener != null)
								workingSetManager.removePropertyChangeListener(propListener);
						}
					}
				}
			});
		}
		
		public Control getControl() {
			return projectComposite;
		}

		public void setFocus() {
			projectComposite.setFocus();
		}

		public void refresh(){
			wsTableViewer.setInput(TeamUIPlugin.getPlugin().getWorkbench().getWorkingSetManager());
		}
		
		private void updateEnablement() {
			boolean complete;
			
			complete = ((selectedProjects.size() != 0) && (selectedWorkingSet.size() != 0));
			
			if (complete) {
				setMessage(null);
			}
			setPageComplete(complete);
		}

		public ArrayList getReferenceCountProjects() {
			return referenceCountProjects;
		}

		public HashSet getSelectedProjects() {
			return selectedProjects;
		}

		public ArrayList getWorkingSet() {
		return selectedWorkingSet;
		}

		private void workingSetAdded(IWorkingSet workingSet) {
			IAdaptable[] elements = workingSet.getElements();
			selectedWorkingSet.add(workingSet);
			for (int i = 0; i < elements.length; i++) {
				selectedProjects.add(elements[i]);
				referenceCountProjects.add(elements[i]);
			}
		}

		private void workingSetRemoved(IWorkingSet workingSet) {
			IAdaptable[] elements = workingSet.getElements();
			selectedWorkingSet.remove(workingSet);
			selectedProjects.removeAll(Arrays.asList(elements));
			for (int i = 0; i < elements.length; i++) {
				referenceCountProjects.remove(elements[i]);
			}
			selectedProjects.addAll(referenceCountProjects);
		}
	}
}
