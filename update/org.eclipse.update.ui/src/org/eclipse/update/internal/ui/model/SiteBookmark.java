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
package org.eclipse.update.internal.ui.model;

import java.net.URL;
import java.util.Vector;

import org.eclipse.core.runtime.*;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.ui.UpdateUI;

public class SiteBookmark extends NamedModelObject 
							implements ISiteAdapter {
	public static final int USER = 0;
	public static final int LOCAL = 1;
	public static final int LOCAL_BOOKMARK = 2;
	public static final String P_URL="p_url";
	public static final String P_TYPE="p_type";
	
	private URL url;
	transient private ISite site;
	transient private Vector catalog;
	transient private SiteCategory otherCategory;
	private int type;
	private boolean webBookmark;
	private boolean selected;
	private String [] ignoredCategories;

	public SiteBookmark() {
	}
	
	public SiteBookmark(String name, URL url, boolean webBookmark) {
		this(name, url, webBookmark, false);
	}
	
	public SiteBookmark(String name, URL url, boolean webBookmark, boolean selected) {
		super(name);
		this.url = url;
		this.webBookmark = webBookmark;
		this.selected = selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}
	
	public boolean isSelected() {
		return selected;
	}
	
	public String [] getIgnoredCategories() {
		return ignoredCategories;
	}
	
	public void setIgnoredCategories(String [] categories) {
		this.ignoredCategories = categories;
	}
	
	public void setType(int type) {
		this.type = type;
	}
	
	public int getType() {
		return type;
	}
	
	public void setWebBookmark(boolean value) {
		if (type==LOCAL_BOOKMARK) return;
		this.webBookmark = value;
		notifyObjectChanged(P_TYPE);
	}
	
	public boolean isWebBookmark() {
		return webBookmark;
	}
	
	public URL getURL() {
		return url;
	}


	public void setURL(URL url) {
		this.url = url;
		site = null;
		notifyObjectChanged(P_URL);
	}
	
	public ISite getSite(IProgressMonitor monitor) {
		return getSite(true, monitor);
	}
	
	public ISite getSite(boolean showDialogIfFailed, IProgressMonitor monitor) {
		if (site==null) {
			try {
				connect(monitor);
			}
			catch (CoreException e) {
				UpdateUI.logException(e, showDialogIfFailed);
			}
		}
		return site;
	}
	
	public boolean isSiteConnected() {
		return site!=null;
	}
	
	public void connect(IProgressMonitor monitor) throws CoreException {
		connect(true, monitor);
	}
	
	public void connect(boolean useCache, IProgressMonitor monitor) throws CoreException {
		if (monitor==null) monitor = new NullProgressMonitor();
		monitor.beginTask("", 2);
		monitor.subTask(UpdateUI.getFormattedMessage("SiteBookmark.connecting", url.toString()));
		site = SiteManager.getSite(url, useCache, new SubProgressMonitor(monitor, 1));
		if (site!=null) createCatalog(new SubProgressMonitor(monitor, 1));
		else
			catalog = new Vector();
	}
	
	private void createCatalog(IProgressMonitor monitor) {
		catalog = new Vector();
		otherCategory = new SiteCategory(this, null, null);
		// Add all the categories
		ICategory [] categories;
		categories = site.getCategories();
		
		ISiteFeatureReference [] featureRefs;
		featureRefs = site.getRawFeatureReferences();
		
		monitor.beginTask("", featureRefs.length + categories.length);

		for (int i=0; i<categories.length; i++) {
			ICategory category = categories[i];
			addCategoryToCatalog(category);
			monitor.worked(1);
		}
		// Add features to categories

		for (int i=0; i<featureRefs.length; i++) {
			ISiteFeatureReference featureRef = featureRefs[i];
			addFeatureToCatalog(featureRef);
			monitor.worked(1);
		}
		if (otherCategory.getChildCount()>0)
		   catalog.add(otherCategory);
	}

	public Object [] getCatalog(boolean withCategories, IProgressMonitor monitor) {
		if (withCategories)
			return catalog.toArray();
		else {
			// Make a flat catalog
			Vector flatCatalog = new Vector();
			for (int i=0; i<catalog.size(); i++) {
				SiteCategory category = (SiteCategory)catalog.get(i);
				category.addFeaturesTo(flatCatalog);
			}
			return flatCatalog.toArray();
		}
	}
	
	private void addCategoryToCatalog(ICategory category) {
		String name = category.getName();
		int loc = name.indexOf('/');
		if (loc == -1) {
			// first level
			catalog.add(new SiteCategory(this, name, category));
		}
		else {
			IPath path = new Path(name);
			name = path.lastSegment().toString();
			path = path.removeLastSegments(1);
			SiteCategory parentCategory = findCategory(path, catalog.toArray());
			if (parentCategory!=null) {
				parentCategory.add(new SiteCategory(this, name, category));
			}
		}
	}
	private void addFeatureToCatalog(ISiteFeatureReference feature) {
		ICategory [] categories = feature.getCategories();
		boolean orphan = true;

		for (int i=0; i<categories.length; i++) {
			ICategory category = categories[i];
			String name = category.getName();
			IPath path = new Path(name);
			SiteCategory parentCategory = findCategory(path, catalog.toArray());
			if (parentCategory!=null) {
		   		parentCategory.add(new FeatureReferenceAdapter(feature));
		   		orphan = false;
			}
		}
		if (orphan)
			otherCategory.add(new FeatureReferenceAdapter(feature));
	}
	
	private SiteCategory findCategory(IPath path, Object [] children) {
		for (int i=0; i<children.length; i++) {
			Object child = children[i];
			if (child instanceof SiteCategory) {
				SiteCategory sc = (SiteCategory)child;
				if (sc.getName().equals(path.segment(0))) {
				   if (path.segmentCount()==1) return sc;
					else {
						path = path.removeFirstSegments(1);
						return findCategory(path, sc.getChildren());
					}
				}
			}
		}
		return null;
	}
	/**
	 * @see ISiteAdapter#getLabel()
	 */
	public String getLabel() {
		return getName();
	}


}
