/*******************************************************************************
 * Copyright (c) 2002, 2003 GEBIT Gesellschaft fuer EDV-Beratung
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
 *******************************************************************************/

package org.eclipse.ant.internal.ui.editor.outline;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.ant.internal.ui.editor.xml.IAntEditorConstants;
import org.eclipse.ant.internal.ui.editor.xml.XmlAttribute;
import org.eclipse.ant.internal.ui.editor.xml.XmlElement;
import org.eclipse.ant.internal.ui.model.AntImageDescriptor;
import org.eclipse.ant.internal.ui.model.AntUIImages;
import org.eclipse.ant.internal.ui.model.AntUIPlugin;
import org.eclipse.ant.internal.ui.model.AntUtil;
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
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.ListenerList;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
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
	
	private IDocumentModelListener fListener;
	private AntModel fModel;
	private XMLCore fCore;
	private ListenerList fPostSelectionChangedListeners= new ListenerList();
	private boolean fIsModelEmpty= true;
	private boolean fFilterInternalTargets;
	private boolean fSort;

	private static ViewerSorter fSorter;
	
	private class AntOutlineSorter extends ViewerSorter {
		/**
		 * @see org.eclipse.jface.viewers.ViewerSorter#compare(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
		 */
		public int compare(Viewer viewer, Object e1, Object e2) {
			if (!(e1 instanceof XmlElement && e2 instanceof XmlElement)) {
				return super.compare(viewer, e1, e2);
			}
			String name1= ((XmlElement) e1).getDisplayName();
			String name2= ((XmlElement) e2).getDisplayName();
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
			XmlElement tempParentElement = (XmlElement)parentNode;
			List tempChilds = new ArrayList();
			List children= tempParentElement.getChildNodes();
			if (!isFilterInternalTargets()) {
				tempChilds.addAll(children);
			} else {
				// Filter out private targets
				Iterator iter= children.iterator();
				while (iter.hasNext()) {
					XmlElement element = (XmlElement) iter.next();
					if (!isInternalTarget(element)) {
						tempChilds.add(element);
					}
				}
			}
			Object[] tempChildObjects = new Object[tempChilds.size()];
			for(int i=0; i<tempChilds.size(); i++) {
				tempChildObjects[i] = tempChilds.get(i);
			}
			return tempChildObjects;
		}
		
		/**
		 * Returns whether the given target is an internal target. Internal
		 * targets are targets which have no description. The default target
		 * is never considered internal.
		 * @param element the target to examine
		 * @return whether the given target is an internal target
		 */
		private boolean isInternalTarget(XmlElement element) {
			return !isDefaultTargetNode(element) && "target".equals(element.getName()) && element.getAttributeNamed(IAntEditorConstants.ATTR_DESCRIPTION) == null; //$NON-NLS-1$
		}


		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(Object)
		 */
		public Object getParent(Object aNode) {
			XmlElement tempElement = (XmlElement)aNode;
			return tempElement.getParentNode();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(Object)
		 */
		public boolean hasChildren(Object aNode) {
			return ((XmlElement)aNode).getChildNodes().size() > 0;
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
	private class LabelProvider implements ILabelProvider, IColorProvider {
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IBaseLabelProvider#addListener(ILabelProviderListener)
		 */
		public void addListener(ILabelProviderListener listener) {
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
		 */
		public void dispose() {
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(Object, String)
		 */
		public boolean isLabelProperty(Object element, String property) {
			return false;
		}


		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IBaseLabelProvider#removeListener(ILabelProviderListener)
		 */
		public void removeListener(ILabelProviderListener listener) {
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(Object)
		 */
		public Image getImage(Object anElement) {
			XmlElement tempElement = (XmlElement)anElement;
			if("target".equals(tempElement.getName())) { //$NON-NLS-1$
				ImageDescriptor base = null;
				int flags = 0;
				
				if (tempElement.isErrorNode()) {
					flags = flags | AntImageDescriptor.HAS_ERRORS;
				}
				if (isDefaultTargetNode(tempElement)) {
					flags = flags | AntImageDescriptor.DEFAULT_TARGET;
					base = AntUIImages.getImageDescriptor(IAntUIConstants.IMG_ANT_DEFAULT_TARGET);
				} else if (tempElement.getAttributeNamed(IAntEditorConstants.ATTR_DESCRIPTION) == null) {
					base = AntUIImages.getImageDescriptor(IAntUIConstants.IMG_ANT_TARGET_INTERNAL);
				} else {
					base = AntUIImages.getImageDescriptor(IAntUIConstants.IMG_ANT_TARGET);
				}
				return AntUIImages.getImage(new AntImageDescriptor(base, flags));				
			}
			if("project".equals(tempElement.getName())) { //$NON-NLS-1$
				return getProjectImage(tempElement);
			}
			
			if("property".equals(tempElement.getName())) { //$NON-NLS-1$
				return AntUIImages.getImage(IAntUIConstants.IMG_PROPERTY);
			}
			
			XmlAttribute attribute= tempElement.getAttributeNamed(IAntEditorConstants.ATTR_TYPE);
			if (attribute != null && IAntEditorConstants.TYPE_EXTERNAL.equals(attribute.getValue())) {
				return getProjectImage(tempElement);
			}

			if (attribute != null && IAntEditorConstants.TYPE_UNKNOWN.equals(attribute.getValue())) {
				int flags= 0;
				ImageDescriptor base= AntUIImages.getImageDescriptor(IAntUIConstants.IMG_TASK_PROPOSAL);
				if (tempElement.isErrorNode()) {
					flags |= AntImageDescriptor.HAS_ERRORS;
				}
				return AntUIImages.getImage(new AntImageDescriptor(base, flags));
			}

			if (tempElement.isErrorNode()) {
				return AntUIImages.getImage(IAntUIConstants.IMG_ANT_TARGET_ERROR);
			}
			return AntUIImages.getImage(IAntUIConstants.IMG_TASK_PROPOSAL);
		}
		
		private Image getProjectImage(XmlElement tempElement) {
			int flags = 0;
			ImageDescriptor base = AntUIImages.getImageDescriptor(IAntUIConstants.IMG_ANT_PROJECT);
			if (tempElement.isErrorNode()) {
				flags = flags | AntImageDescriptor.HAS_ERRORS;
			}
			return AntUIImages.getImage(new AntImageDescriptor(base, flags));
		}
        
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ILabelProvider#getText(Object)
		 */
		public String getText(Object aNode) {
			XmlElement element= (XmlElement) aNode;
			StringBuffer displayName= new StringBuffer(element.getDisplayName());
			if (element.isExternal() && (!element.isRootExternal() || (element.getParentNode() != null && element.getParentNode().isExternal()))) {
				displayName.append(AntOutlineMessages.getString("AntEditorContentOutlinePage._[external]_1")); //$NON-NLS-1$
			}
			return displayName.toString();
		}

		public Color getForeground(Object element) {
			if (isDefaultTargetNode((XmlElement) element)) {
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
		AntUIPlugin.getDefault().getPreferenceStore().setValue(IAntUIPreferenceConstants.ANTEDITOR_FILTER_INTERNAL_TARGETS, filter);
		getTreeViewer().refresh();
	}
	
	/**
	 * Returns whether internal targets are currently being filtered out of
	 * the outline.
	 * 
	 * @return whether or not internal targets are being filtered out
	 */
	protected boolean isFilterInternalTargets() {
		return fFilterInternalTargets;
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
	 * Returns whether the given element represents a project's default target
	 * 
	 * @param node the node to examine
	 * @return whether the given node is a default target
	 */
	private boolean isDefaultTargetNode(XmlElement node) {
		XmlAttribute type= node.getAttributeNamed(IAntEditorConstants.ATTR_TYPE);
		if (type == null || !type.getValue().equals(IAntEditorConstants.TYPE_TARGET)) {
			return false;
		}
		XmlElement parent= node.getParentNode();
		if (parent != null) {
			type= parent.getAttributeNamed(IAntEditorConstants.ATTR_TYPE);
			while (parent != null && (type == null || !type.getValue().equals(IAntEditorConstants.TYPE_PROJECT))) {
				parent= parent.getParentNode();
				if (parent != null) {
					type= parent.getAttributeNamed(IAntEditorConstants.ATTR_TYPE);
				}
			}
		} 
		if (parent == null) {
			return false;
		}
		
		XmlAttribute defaultTarget= parent.getAttributeNamed(IAntEditorConstants.ATTR_DEFAULT);
		XmlAttribute nameAttribute= node.getAttributeNamed(IAntEditorConstants.ATTR_NAME);
		return defaultTarget != null && nameAttribute != null && defaultTarget.getValue().equals(nameAttribute.getValue());
	}
   
	/**
	 * Creates a new AntEditorContentOutlinePage.
	 */
	public AntEditorContentOutlinePage(XMLCore core) {
		super();
		fCore= core;
		fFilterInternalTargets= AntUIPlugin.getDefault().getPreferenceStore().getBoolean(IAntUIPreferenceConstants.ANTEDITOR_FILTER_INTERNAL_TARGETS);
		fSort= AntUIPlugin.getDefault().getPreferenceStore().getBoolean(IAntUIPreferenceConstants.ANTEDITOR_SORT);
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
		
		openWithMenu= new AntOpenWithMenu(this.getSite().getPage());
		
		viewer.addPostSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				firePostSelectionChanged(event.getSelection());
			}
		});
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
		menuManager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}
	
	private void addOpenWithMenu(IMenuManager menuManager) {
		IStructuredSelection selection= (IStructuredSelection)getSelection();
		XmlElement element= (XmlElement)selection.getFirstElement();
		String path = getElementPath(element);
		if (path != null) {
			IFile file= AntUtil.getFileForLocation(path, null);
			if (file != null) {
				menuManager.add(new Separator("group.open")); //$NON-NLS-1$
				IMenuManager submenu= new MenuManager(AntOutlineMessages.getString("AntEditorContentOutlinePage.Open_With_1"));  //$NON-NLS-1$
				openWithMenu.setFile(file);
				submenu.add(openWithMenu);
				menuManager.appendToGroup("group.open", submenu); //$NON-NLS-1$
			}
		}
	}

	private String getElementPath(XmlElement element) {
		String path= element.getFilePath();
		if (element.isRootExternal()){
			List children= element.getChildNodes();
			if (!children.isEmpty()) {
				XmlElement child= (XmlElement)children.get(0);
				path= child.getFilePath();
			}
		}
		return path;
	}
	
	private boolean shouldAddOpenWithMenu() {
		ISelection iselection= getSelection();
		if (iselection instanceof IStructuredSelection) {
			IStructuredSelection selection= (IStructuredSelection)iselection;
			if (selection.size() == 1) {
				Object selected= selection.getFirstElement();
				if (selected instanceof XmlElement) {
					XmlElement element= (XmlElement)selected;
					if (element.isExternal()) {
						String path = getElementPath(element);
						if (path.length() == 0) {
							return false;
						}
						
						XmlElement parent= element.getParentNode();
						while (parent != null) {
							String parentPath= getElementPath(parent);
							if (path != null && !path.equals(parentPath)) {
								return true;
							}
							parent= parent.getParentNode();
						}
					}	
				}
			}
		}
		return false;
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
		if (fModel != null) {
			ILocationProvider locationProvider= fModel.getLocationProvider();
			IFile file= AntUtil.getFile(locationProvider.getLocation().toOSString());
			ISelection selection= new StructuredSelection(file);
			return new ShowInContext(null, selection);
		}
		return null;
	}
}
