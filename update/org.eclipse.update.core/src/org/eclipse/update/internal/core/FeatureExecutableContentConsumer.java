package org.eclipse.update.internal.core;

/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
 
import java.util.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.update.core.*;

/**
 * ContentConsumer Implementation for a FeatureExecutable
 */

public class FeatureExecutableContentConsumer extends FeatureContentConsumer {

	private IFeature feature;
	private boolean closed= false;
	private boolean aborted= false;	
	private ISiteContentConsumer contentConsumer;
	private List /* of ContentConsumer */ contentConsumers;
	private IFeatureContentConsumer parent = null;
	private List /* of IFeatureContentCOnsumer */ children;

	/*
	 * @see IContentConsumer#open(INonPluginEntry)
	 */
	public IContentConsumer open(INonPluginEntry nonPluginEntry)
		throws CoreException {
		ContentConsumer cons = new NonPluginEntryContentConsumer(
			getContentConsumer().open(nonPluginEntry));
		addContentConsumers(cons);
		return cons;
	}

	/*
	 * @see IContentConsumer#open(IPluginEntry)
	 */
	public IContentConsumer open(IPluginEntry pluginEntry) throws CoreException {
		ContentConsumer cons = new PluginEntryContentConsumer(getContentConsumer().open(pluginEntry));
		addContentConsumers(cons);
		return cons;		
	}

	/*
	 * @see IContentConsumer#addChild(IFeature)
	 */
	public void addChild(IFeature child) throws CoreException {
		IFeatureContentConsumer childConsumer = child.getFeatureContentConsumer();
		childConsumer.setParent(this);
		if (children==null) children = new ArrayList();
		children.add(childConsumer);
		return;
	}


	/*
	 * @see IFeatureContentConsumer#store(ContentReference, IProgressMonitor)
	 */
	public void store(ContentReference contentReference, IProgressMonitor monitor)
		throws CoreException {
		getContentConsumer().store(contentReference, monitor);
	}

	/*
	 * @see IFeatureContentConsumer#close()
	 */
	public IFeatureReference close() throws CoreException {
		
		if (!closed && getParent()!=null){
			closed=true;
			return null;
		}
		
		// close nested feature
		IFeatureContentConsumer[] children = getChildren();
		for (int i = 0; i < children.length; i++) {
			children[i].close();
		}

		// close plugin and non plugin content consumer
		if (contentConsumers!=null){
			Iterator iter = contentConsumers.iterator();
			while (iter.hasNext()) {
				ContentConsumer element = (ContentConsumer) iter.next();
				element.close();
			}
		}
		contentConsumers = null;
		
		if (contentConsumer != null)
			return contentConsumer.close();
		return null;
	}

	/*
	 * @see IFeatureContentConsumer#setFeature(IFeature)
	 */
	public void setFeature(IFeature feature) {
		this.feature= feature;
	}

	/*
	 * Sets the parent 
	 */
	public void setParent(IFeatureContentConsumer featureContentConsumer) {
		this.parent= featureContentConsumer;
	}

	/*
	 * returns the Content Consumer for the feature
	 * 
	 * Right now we are the only one to implement SiteContentConsumer
	 * Need to be exposed as API post v2.0
	 */
	private ISiteContentConsumer getContentConsumer() throws CoreException {
		if (contentConsumer == null)
			if (feature.getSite() instanceof SiteFile) {
				SiteFile site= (SiteFile) feature.getSite();
				contentConsumer= site.createSiteContentConsumer(feature);
			} else {
				throw new UnsupportedOperationException();
			}
		return contentConsumer;
	}

	/*
	 * Adds a ContentConsumer to the list
	 */
	private void addContentConsumers(ContentConsumer cons){
		if (contentConsumers == null)
			contentConsumers = new ArrayList();
		contentConsumers.add(cons);
	}

	/*
	 * @see IFeatureContentConsumer#abort()
	 */
	public void abort() throws CoreException {

		if (aborted) return;
		
		IFeatureContentConsumer[] children = getChildren();
		for (int i = 0; i < children.length; i++) {
			try {
			children[i].abort();
			} catch (Exception e){
				//do Nothing
			}
		}

		// do not close plugin and non plugin content consumer
		// the contentConsumer will abort them
		// we do not need to abort the NonPluginEntryContentConsumer and PluginEntryContentConsume

		//implement the cleanup
		if (contentConsumer!=null)
			contentConsumer.abort();
		
		aborted = true;
		throw Utilities.newCoreException("",null);
	}

	/*
	 * @see IFeatureContentConsumer#getFeature()
	 */
	public IFeature getFeature(){
		return feature;
	}

	/*
	 * @see IFeatureContentConsumer#getParent()
	 */
	public IFeatureContentConsumer getParent(){
		return parent;
	}

	/*
	 * @see IFeatureContentConsumer#getChildren()
	 */
	public IFeatureContentConsumer[] getChildren(){
		if (children==null)
			return new IFeatureContentConsumer[0];

		return (IFeatureContentConsumer[]) children.toArray(arrayTypeFor(children));
	}
}