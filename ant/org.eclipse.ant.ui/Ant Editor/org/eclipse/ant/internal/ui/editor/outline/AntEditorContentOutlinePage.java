/*******************************************************************************
 * Copyright (c) 2002, 2004 GEBIT Gesellschaft fuer EDV-Beratung
 * und Informatik-Technologien mbH, 
 * Berlin, Duesseldorf, Frankfurt (Germany) and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     GEBIT Gesellschaft fuer EDV-Beratung und Informatik-Technologien mbH - initial API and implementation
 * 	   IBM Corporation - bug fixes
 *     John-Mason P. Shackelford (john-mason.shackelford@pearson.com) - bug 49380, bug 34548, bug 53547
 *******************************************************************************/

package org.eclipse.ant.internal.ui.editor.outline;

import java.util.List;

import org.apache.tools.ant.Target;
import org.eclipse.ant.internal.ui.editor.AntEditor;
import org.eclipse.ant.internal.ui.editor.model.AntElementNode;
import org.eclipse.ant.internal.ui.editor.model.AntImportNode;
import org.eclipse.ant.internal.ui.editor.model.AntProjectNode;
import org.eclipse.ant.internal.ui.editor.model.AntPropertyNode;
import org.eclipse.ant.internal.ui.editor.model.AntTargetNode;
import org.eclipse.ant.internal.ui.editor.model.AntTaskNode;
import org.eclipse.ant.internal.ui.model.AntUIPlugin;
import org.eclipse.ant.internal.ui.model.IAntUIConstants;
import org.eclipse.ant.internal.ui.model.IAntUIPreferenceConstants;
import org.eclipse.ant.internal.ui.views.actions.AntOpenWithMenu;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.util.ListenerList;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

/**
 * Content outline page for the Ant Editor.
 */
public class AntEditorContentOutlinePage extends ContentOutlinePage implements IShowInSource, IAdaptable {
	
	private static final int EXPAND_TO_LEVEL= 2;

	private Menu menu;
	private AntOpenWithMenu openWithMenu;
	private AntRunTargetAction runTargetImmediately; 
	private AntRunTargetAction runAnt;
	
	private IDocumentModelListener fListener;
	private AntModel fModel;
	private XMLCore fCore;
	private ListenerList fPostSelectionChangedListeners= new ListenerList();
	private boolean fIsModelEmpty= true;
	private boolean fFilterInternalTargets;
    private InternalTargetFilter fInternalTargetFilter= null;
    private boolean fFilterImportedElements;
    private ImportedElementsFilter fImportedElementsFilter= null;   
	private boolean fFilterProperties;
	private PropertiesFilter fPropertiesFilter= null;
	private boolean fFilterTopLevel;
	private TopLevelFilter fTopLevelFilter= null;
	private boolean fSort;

	private ViewerSorter fSorter;
	
	private static final Object[] EMPTY_ARRAY= new Object[0];
	
	private AntEditor fEditor;
	
	/**
	 * A viewer filter that removes internal targets
	 */
	private class InternalTargetFilter extends ViewerFilter {
		
		/**
		 * Returns whether the given target is an internal target. Internal
		 * targets are targets which have no description. The default target
		 * is never considered internal.
		 */
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (element instanceof AntTargetNode) {
				Target target= ((AntTargetNode)element).getTarget();
				return target.getDescription() != null || ((AntTargetNode)element).isDefaultTarget();
			} 
			return true;
		}
	}
	
	/**
	 * A viewer filter that removes imported elements except an imported default target
	 */
	private class ImportedElementsFilter extends ViewerFilter {
		
		/**
		 * Returns whether the given {@link AntElementNode} is imported from
		 * another file.
		 */
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (element instanceof AntElementNode) {
			    AntElementNode node = (AntElementNode) element; 			    
				if (node.getImportNode() !=  null || node.isExternal()) {
					if (node instanceof AntTargetNode && ((AntTargetNode)node).isDefaultTarget()) {
						return true;
					}
					return false;
				}
			} 
			return true;
		}
	}
	
	/**
	 * A viewer filter that removes properties
	 */
	private class PropertiesFilter extends ViewerFilter {
		
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (element instanceof AntPropertyNode) {
				return false;
			} 
			return true;
		}
	}
	
	/**
	 * A viewer filter that removes top level tasks and types
	 */
	private class TopLevelFilter extends ViewerFilter {
		
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (element instanceof AntTaskNode) {
				if (parentElement instanceof AntProjectNode) {
					return false;
				}
			} 
			return true;
		}
	}
	
	private class AntOutlineSorter extends ViewerSorter {
		/**
		 * @see org.eclipse.jface.viewers.ViewerSorter#compare(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
		 */
		public int compare(Viewer viewer, Object e1, Object e2) {
			if (!(e1 instanceof AntElementNode && e2 instanceof AntElementNode)) {
				return super.compare(viewer, e1, e2);
			}
			String name1= ((AntElementNode) e1).getLabel();
			String name2= ((AntElementNode) e2).getLabel();
			return getCollator().compare(name1, name2);
		}
	}

	/**
	 * The content provider for the objects shown in the outline view.
	 */
	private class ContentProvider implements ITreeContentProvider {

		/**
		 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
		 */
		public void dispose() {
		}
        
		/**
		 * do nothing
		 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(Viewer, Object, Object)
		 */
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(Object)
		 */
		public Object[] getChildren(Object parentNode) {
			AntElementNode tempParentElement = (AntElementNode)parentNode;
			if (tempParentElement.hasChildren()) {
				List children= tempParentElement.getChildNodes();
				return children.toArray();
			} 
			return EMPTY_ARRAY;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(Object)
		 */
		public Object getParent(Object aNode) {
			AntElementNode tempElement = (AntElementNode)aNode;
			return tempElement.getParentNode();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(Object)
		 */
		public boolean hasChildren(Object aNode) {
			return ((AntElementNode)aNode).hasChildren();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(Object)
		 */
		public Object[] getElements(Object anInputElement) {
			return ((AntModel) anInputElement).getRootElements();
		}

	}
    
	/**
	 * The label provider for the objects shown in the outline view.
	 */
	private class LabelProvider extends org.eclipse.jface.viewers.LabelProvider implements IColorProvider {

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(Object)
		 */
		public Image getImage(Object anElement) {
			AntElementNode node = (AntElementNode)anElement;
			return node.getImage();
		}
        
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ILabelProvider#getText(Object)
		 */
		public String getText(Object node) {
			AntElementNode element= (AntElementNode) node;
			return element.getLabel();
		}

		public Color getForeground(Object node) {
			if (node instanceof AntTargetNode && ((AntTargetNode)node).isDefaultTarget() ) {
				return Display.getDefault().getSystemColor(SWT.COLOR_BLUE);
			}
			
			return null;
		}

		public Color getBackground(Object element) {
			return null;
		}
	}
	
	/**
	 * Sets whether internal targets should be filtered out of the outline.
	 * 
	 * @param filter whether or not internal targets should be filtered out
	 */
	protected void setFilterInternalTargets(boolean filter) {
		fFilterInternalTargets= filter;
		setFilter(filter, getInternalTargetsFilter(), IAntUIPreferenceConstants.ANTEDITOR_FILTER_INTERNAL_TARGETS);   
	}
	
	/**
	 * Sets whether imported elements should be filtered out of the outline.
	 * 
	 * @param filter whether or not imported elements should be filtered out
	 */
    protected void setFilterImportedElements(boolean filter) {
		fFilterImportedElements= filter;
		setFilter(filter, getImportedElementsFilter(), IAntUIPreferenceConstants.ANTEDITOR_FILTER_IMPORTED_ELEMENTS);        
    }

	private void setFilter(boolean filter, ViewerFilter viewerFilter, String name) {
		if (filter) {
			getTreeViewer().addFilter(viewerFilter);
		} else {
			getTreeViewer().removeFilter(viewerFilter);
		}
		AntUIPlugin.getDefault().getPreferenceStore().setValue(name, filter);
		getTreeViewer().refresh();
	}

	/**
	 * Sets whether properties should be filtered out of the outline.
	 * 
	 * @param filter whether or not properties should be filtered out
	 */
	protected void setFilterProperties(boolean filter) {
		fFilterProperties= filter;
		setFilter(filter, getPropertiesFilter(), IAntUIPreferenceConstants.ANTEDITOR_FILTER_PROPERTIES);     
	}
	
	/**
	 * Sets whether internal targets should be filtered out of the outline.
	 * 
	 * @param filter whether or not internal targets should be filtered out
	 */
	protected void setFilterTopLevel(boolean filter) {
		fFilterTopLevel= filter;
		setFilter(filter, getTopLevelFilter(), IAntUIPreferenceConstants.ANTEDITOR_FILTER_TOP_LEVEL);     
	}
	
	private ViewerFilter getInternalTargetsFilter() {
		if (fInternalTargetFilter == null) {
			fInternalTargetFilter= new InternalTargetFilter();
		}
		return fInternalTargetFilter;
	}
	
    private ViewerFilter getImportedElementsFilter() {
		if (fImportedElementsFilter == null) {
		    fImportedElementsFilter= new ImportedElementsFilter();
		}
		return fImportedElementsFilter;
    }
	
	private ViewerFilter getPropertiesFilter() {
		if (fPropertiesFilter == null) {
			fPropertiesFilter= new PropertiesFilter();
		}
		return fPropertiesFilter;
	}
	
	private ViewerFilter getTopLevelFilter() {
		if (fTopLevelFilter == null) {
			fTopLevelFilter= new TopLevelFilter();
		}
		return fTopLevelFilter;
	}

	/**
	 * Returns whether internal targets are currently being filtered out of
	 * the outline.
	 * 
	 * @return whether or not internal targets are being filtered out
	 */
	protected boolean filterInternalTargets() {
		return fFilterInternalTargets;
	}
	
	/**
	 * Returns whether imported elements are currently being filtered out of
	 * the outline.
	 * 
	 * @return whether or not imported elements are being filtered out
	 */
	protected boolean filterImportedElements() {
		return fFilterImportedElements;
	}

	/**
	 * Returns whether properties are currently being filtered out of
	 * the outline.
	 * 
	 * @return whether or not properties are being filtered out
	 */
	protected boolean filterProperties() {
		return fFilterProperties;
	}
	
	/**
	 * Returns whether top level tasks/types are currently being filtered out of
	 * the outline.
	 * 
	 * @return whether or not top level tasks/types are being filtered out
	 */
	protected boolean filterTopLevel() {
		return fFilterTopLevel;
	}
	
	/**
	 * Sets whether elements should be sorted in the outline.
	 *  
	 * @param sort whether or not elements should be sorted
	 */
	protected void setSort(boolean sort) {
		fSort= sort;
		if (sort) {
			if (fSorter == null) {
				fSorter= new AntOutlineSorter();
			}
			getTreeViewer().setSorter(fSorter);
		} else {
			getTreeViewer().setSorter(null);
		}
		AntUIPlugin.getDefault().getPreferenceStore().setValue(IAntUIPreferenceConstants.ANTEDITOR_SORT, sort);
	}
	
	/**
	 * Returns whether elements are currently being sorted.
	 * 
	 * @return whether elements are currently being sorted
	 */
	protected boolean isSort() {
		return fSort;
	}
	
	/**
	 * Creates a new AntEditorContentOutlinePage.
	 */
	public AntEditorContentOutlinePage(XMLCore core, AntEditor editor) {
		super();
		fCore= core;
		fFilterInternalTargets= AntUIPlugin.getDefault().getPreferenceStore().getBoolean(IAntUIPreferenceConstants.ANTEDITOR_FILTER_INTERNAL_TARGETS);
		fFilterImportedElements= AntUIPlugin.getDefault().getPreferenceStore().getBoolean(IAntUIPreferenceConstants.ANTEDITOR_FILTER_IMPORTED_ELEMENTS);
		fFilterProperties= AntUIPlugin.getDefault().getPreferenceStore().getBoolean(IAntUIPreferenceConstants.ANTEDITOR_FILTER_PROPERTIES);
		fFilterTopLevel= AntUIPlugin.getDefault().getPreferenceStore().getBoolean(IAntUIPreferenceConstants.ANTEDITOR_FILTER_TOP_LEVEL);
		fSort= AntUIPlugin.getDefault().getPreferenceStore().getBoolean(IAntUIPreferenceConstants.ANTEDITOR_SORT);
		fEditor= editor;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.IPage#dispose()
	 */
	public void dispose() {
		if (menu != null) {
			menu.dispose();
		}
		if (openWithMenu != null) {
			openWithMenu.dispose();
		}
		if (fListener != null) {
			fCore.removeDocumentModelListener(fListener);
			fListener= null;
		}
	}
	
	/**  
	 * Creates the control (outline view) for this page
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
    
		TreeViewer viewer = getTreeViewer();
        
		/*
		 * We might want to implement our own content provider.
		 * This content provider should be able to work on a dom like tree
		 * structure that resembles the file contents.
		 */
		viewer.setContentProvider(new ContentProvider());
		setSort(fSort);

		/*
		 * We probably also need our own label provider.
		 */ 
		viewer.setLabelProvider(new LabelProvider());
		if (fModel != null) {
			setViewerInput(fModel);
		}
		
		MenuManager manager= new MenuManager("#PopUp"); //$NON-NLS-1$
		manager.setRemoveAllWhenShown(true);
		manager.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager menuManager) {
				contextMenuAboutToShow(menuManager);
			}
		});
		menu= manager.createContextMenu(viewer.getTree());
		viewer.getTree().setMenu(menu);

		IPageSite site= getSite();
		site.registerContextMenu(IAntUIConstants.PLUGIN_ID + ".antEditorOutline", manager, viewer); //$NON-NLS-1$
		
		IToolBarManager tbm= site.getActionBars().getToolBarManager();
		tbm.add(new ToggleSortAntOutlineAction(this));
		tbm.add(new FilterInternalTargetsAction(this));
		tbm.add(new FilterPropertiesAction(this));
		tbm.add(new FilterImportedElementsAction(this));
		tbm.add(new FilterTopLevelAction(this));
		
		IMenuManager viewMenu= site.getActionBars().getMenuManager();
		viewMenu.add(new ToggleLinkWithEditorAction(fEditor));
		
		openWithMenu= new AntOpenWithMenu(this.getSite().getPage());
		
		viewer.addPostSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				firePostSelectionChanged(event.getSelection());
			}
		});
		
		runTargetImmediately = new AntRunTargetAction(this, AntRunTargetAction.MODE_IMMEDIATE_EXECUTE);
		runAnt = new AntRunTargetAction(this, AntRunTargetAction.MODE_DISPLAY_DIALOG);
		setFilterInternalTargets(fFilterInternalTargets);
		setFilterImportedElements(fFilterImportedElements);
		setFilterProperties(fFilterProperties);
		setFilterTopLevel(fFilterTopLevel);
	}
	
	private void setViewerInput(Object newInput) {
		TreeViewer tree= getTreeViewer();
		Object oldInput= tree.getInput();
		
		boolean isAntModel= (newInput instanceof AntModel);
		boolean wasAntModel= (oldInput instanceof AntModel);
		
		if (isAntModel && !wasAntModel) {
			if (fListener == null) {
				fListener= createAntModelChangeListener();
			}
			fCore.addDocumentModelListener(fListener);
		} else if (!isAntModel && wasAntModel && fListener != null) {
			fCore.removeDocumentModelListener(fListener);
			fListener= null;
		}

		
		tree.setInput(newInput);
		
		if (isAntModel) {
			updateTreeExpansion();
		}
	}
	
	public void setPageInput(AntModel xmlModel) {
		fModel= xmlModel;
		if (getTreeViewer() != null) {
			setViewerInput(fModel);
		}
	}
		
	private IDocumentModelListener createAntModelChangeListener() {
		return new IDocumentModelListener() {
			public void documentModelChanged(final DocumentModelChangeEvent event) {
				if (event.getModel() == fModel && !getControl().isDisposed()) {
					getControl().getDisplay().asyncExec(new Runnable() {
						public void run() {
							Control ctrl= getControl();
							if (ctrl != null && !ctrl.isDisposed()) {
								getTreeViewer().refresh();
								updateTreeExpansion();
							}
						}
					});
				}
			}
		};
	}
	
	public void addPostSelectionChangedListener(ISelectionChangedListener listener) {
		fPostSelectionChangedListeners.add(listener);
	}
	
	public void removePostSelectionChangedListener(ISelectionChangedListener listener) {
		fPostSelectionChangedListeners.remove(listener);
	}
	
	private void updateTreeExpansion() {
		boolean wasModelEmpty= fIsModelEmpty;
		fIsModelEmpty= fModel == null || fModel.getRootElements() == null || fModel.getRootElements().length == 0;
		if (wasModelEmpty && !fIsModelEmpty) {
			getTreeViewer().expandToLevel(EXPAND_TO_LEVEL);
		}
	}
	
	private void firePostSelectionChanged(ISelection selection) {
		// create an event
		SelectionChangedEvent event= new SelectionChangedEvent(this, selection);
 
		// fire the event
		Object[] listeners= fPostSelectionChangedListeners.getListeners();
		for (int i= 0; i < listeners.length; ++i) {
			((ISelectionChangedListener) listeners[i]).selectionChanged(event);
		}
	}
	
	private void contextMenuAboutToShow(IMenuManager menuManager) {	
		if (shouldAddOpenWithMenu()) {
			addOpenWithMenu(menuManager);
		}
		if (shouldAddRunTargetMenu()) {
			addRunTargetMenu(menuManager);
		}
		menuManager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}
	
	private void addOpenWithMenu(IMenuManager menuManager) {
		AntElementNode element= getSelectedNode();
		IFile file = element.getIFile();
		if (file != null) {
			menuManager.add(new Separator("group.open")); //$NON-NLS-1$
			IMenuManager submenu= new MenuManager(AntOutlineMessages.getString("AntEditorContentOutlinePage.Open_With_1"));  //$NON-NLS-1$
			openWithMenu.setFile(file);
			if (element.getImportNode() != null) {
				int[] lineAndColumn= element.getExternalInfo();
				openWithMenu.setExternalInfo(lineAndColumn[0], lineAndColumn[1]);
			}
			submenu.add(openWithMenu);
			menuManager.appendToGroup("group.open", submenu); //$NON-NLS-1$
		}
	}

	private void addRunTargetMenu(IMenuManager menuManager) {
		menuManager.add(this.runTargetImmediately);
		menuManager.add(this.runAnt);                   
	}
	
	private boolean shouldAddRunTargetMenu() {
		AntElementNode node= getSelectedNode();
		if (node instanceof AntProjectNode || node instanceof AntTargetNode) {
			return true;
		}
		return false;
	}
	
	private boolean shouldAddOpenWithMenu() {
		AntElementNode node= getSelectedNode();
		if (node instanceof AntImportNode) {
			return true;
		}
		if (node != null && node.isExternal()) {
			String path = node.getFilePath();
			if (path != null && path.length() > 0) {
				return true;
			}
		}
		return false;
	}

	private AntElementNode getSelectedNode() {
		ISelection iselection= getSelection();
		if (iselection instanceof IStructuredSelection) {
			IStructuredSelection selection= (IStructuredSelection)iselection;
			if (selection.size() == 1) {
				Object selected= selection.getFirstElement();
				if (selected instanceof AntElementNode) {
					return (AntElementNode)selected;
				}
			}
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class key) {
		if (key == IShowInSource.class) {
			return this;
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.IShowInSource#getShowInContext()
	 */
	public ShowInContext getShowInContext() {
		IFile file= null;
		if (fModel != null) {
			AntElementNode node= getSelectedNode();
			file= node.getIFile();
		}
		if (file != null) {
			ISelection selection= new StructuredSelection(file);
			return new ShowInContext(null, selection);
		} 
		return null;
	}
	
	public void select(AntElementNode node) {
		if (getTreeViewer() != null) {
			ISelection s= getTreeViewer().getSelection();
			if (s instanceof IStructuredSelection) {
				IStructuredSelection ss= (IStructuredSelection) s;
				List nodes= ss.toList();
				if (!nodes.contains(node)) {
					s= (node == null ? StructuredSelection.EMPTY : new StructuredSelection(node));
					getTreeViewer().setSelection(s, true);
				}
			}
		}
	}
}
