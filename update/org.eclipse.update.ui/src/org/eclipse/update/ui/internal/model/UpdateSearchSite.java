package org.eclipse.update.ui.internal.model;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.net.URL;
import org.eclipse.update.core.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.views.properties.*;
import org.eclipse.ui.model.*;
import java.util.*;

public class UpdateSearchSite extends ModelObject 
							implements IWorkbenchAdapter,
										ISiteWrapper {
	private ISite site;
	private Vector candidates;
	private String label;
	
	public Object getAdapter(Class adapter) {
		if (adapter.equals(IWorkbenchAdapter.class)) {
			return this;
		}
		return super.getAdapter(adapter);
	}
	
	public UpdateSearchSite(String label, ISite site) {
		this.label = label;
		this.site = site;
		candidates = new Vector();
	}
	
	public ISite getSite() {
		return site;
	}
	
	public String getLabel() {
		return label;
	}
	
	public String toString() {
		return getLabel();
	}
	
	/**
	 * @see IWorkbenchAdapter#getChildren(Object)
	 */
	public Object[] getChildren(Object parent) {
		return candidates.toArray();
	}

	/**
	 * @see IWorkbenchAdapter#getImageDescriptor(Object)
	 */
	public ImageDescriptor getImageDescriptor(Object obj) {
		return null;
	}

	/**
	 * @see IWorkbenchAdapter#getLabel(Object)
	 */
	public String getLabel(Object obj) {
		return getLabel();
	}

	/**
	 * @see IWorkbenchAdapter#getParent(Object)
	 */
	public Object getParent(Object arg0) {
		return null;
	}
	
	public void addCandidate(IFeature candidate) {
		candidates.add(candidate);
	}
	/**
	 * @see ISiteWrapper#getURL()
	 */
	public URL getURL() {
		return site.getURL();
	}

}