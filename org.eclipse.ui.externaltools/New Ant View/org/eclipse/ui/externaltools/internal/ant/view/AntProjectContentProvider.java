package org.eclipse.ui.externaltools.internal.ant.view;

/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp.  All rights reserved.
This file is made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html
**********************************************************************/

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.externaltools.internal.ant.view.elements.AntNode;
import org.eclipse.ui.externaltools.internal.ant.view.elements.DependencyNode;
import org.eclipse.ui.externaltools.internal.ant.view.elements.ExecutionPathNode;
import org.eclipse.ui.externaltools.internal.ant.view.elements.ProjectNode;
import org.eclipse.ui.externaltools.internal.ant.view.elements.RootNode;
import org.eclipse.ui.externaltools.internal.ant.view.elements.TargetErrorNode;
import org.eclipse.ui.externaltools.internal.ant.view.elements.TargetNode;

/**
 * Content provider that provides a tree of ant projects.
 */
public class AntProjectContentProvider implements ITreeContentProvider {

	/**
	 * The root node of the project tree
	 */
	private RootNode rootNode;

	/**
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
	 */
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	/**
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	public void dispose() {
	}

	/**
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput instanceof RootNode) {
			rootNode= (RootNode) newInput;
		}
	}
	
	/**
	 * Adds the given buildfile to the tree. Has no effect if the given build
	 * file is already present in the tree.
	 * 
	 * @param buildFileName the string representing the path to a build file
	 */
	public void addProject(ProjectNode project) {
		ProjectNode[] projects= getRootNode().getProjects();
		for (int i = 0; i < projects.length; i++) {
			ProjectNode node = projects[i];
			if (node.getBuildFileName().equals(project.getBuildFileName())) {
				return;
			}
		}
		getRootNode().addProject(project);
	}
	
	/**
	 * Returns the root node of the tree or <code>null</code> if no root has
	 * been set as the input
	 * 
	 * @return RootNode the root node of the tree
	 */
	public RootNode getRootNode() {
		return rootNode;
	}
	
	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(Object)
	 */
	public Object[] getChildren(Object element) {
		if (element instanceof RootNode) {
			return ((RootNode) element).getProjects();
		} else if (element instanceof ProjectNode) {
			return ((ProjectNode) element).getTargets();
		} else if (element instanceof TargetErrorNode) {
			return null;
		} else if (element instanceof TargetNode) {
			return new Object[] {((TargetNode) element).getDependencies(), ((TargetNode) element).getExecutionPath()};
		} else if (element instanceof DependencyNode) {
			return ((DependencyNode) element).getDependencies();
		} else if (element instanceof ExecutionPathNode) {
			return ((ExecutionPathNode) element).getTargets();
		}
		return null;
	}
	
	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(Object)
	 */
	public Object getParent(Object element) {
		if (element instanceof AntNode) {
			return ((AntNode) element).getParent();
		}
		return null;
	}
	
	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(Object)
	 */
	public boolean hasChildren(Object element) {
		if (element instanceof RootNode) {
			return ((RootNode) element).getProjects().length > 0;
		} else if (element instanceof ProjectNode) {
			return ((ProjectNode) element).getTargets().length > 0;
		} else if (element instanceof TargetErrorNode) {
			return false;
		} else if (element instanceof TargetNode) {
			return true;
		} else if (element instanceof DependencyNode) {
			return true;
		} else if (element instanceof ExecutionPathNode) {
			return true;
		}
		return false;
	}

}
