package org.eclipse.update.internal.ui.model;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.File;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.IWorkbenchAdapter;

public class MyComputerFile extends UIModelObject implements IWorkbenchAdapter {
	private UIModelObject parent;
	private File file;

	public MyComputerFile(UIModelObject parent, File file) {
		this.parent = parent;
		this.file = file;
	}
	
	public Object getAdapter(Class adapter) {
		if (adapter.equals(IWorkbenchAdapter.class)) {
			return this;
		}
		return super.getAdapter(adapter);
	}
	
	public String getName() {
		return file.getName();
	}
	
	public String toString() {
		return getName();
	}
	
	/**
	 * @see IWorkbenchAdapter#getChildren(Object)
	 */
	
	public Object[] getChildren(Object parent) {
		return new Object[0];
	}


	/**
	 * @see IWorkbenchAdapter#getImageDescriptor(Object)
	 */
	public ImageDescriptor getImageDescriptor(Object obj) {
		return PlatformUI.getWorkbench().getEditorRegistry().getImageDescriptor(getName());
	}
	
	/**
	 * @see IWorkbenchAdapter#getLabel(Object)
	 */
	public String getLabel(Object obj) {
		return getName();
	}


	/**
	 * @see IWorkbenchAdapter#getParent(Object)
	 */
	public Object getParent(Object arg0) {
		return parent;
	}
}