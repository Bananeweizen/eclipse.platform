package org.eclipse.update.core;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.*;
import org.eclipse.update.core.model.FeatureReferenceModel;
import org.eclipse.update.core.model.SiteModel;
import org.eclipse.update.internal.core.*;

/**
 * Convenience implementation of a feature reference.
 * <p>
 * This class may be instantiated or subclassed by clients.
 * </p> 
 * @see org.eclipse.update.core.IFeatureReference
 * @see org.eclipse.update.core.model.FeatureReferenceModel
 * @since 2.0
 */
public class FeatureReference
	extends FeatureReferenceModel
	implements IFeatureReference {

	private IFeature feature;
	private List categories;

	/**
	 * Feature reference default constructor
	 */
	public FeatureReference() {
		super();
	}

	/**
	 * Returns the feature this reference points to
	 *  @return the feature on the Site
	 */
	public IFeature getFeature() throws CoreException {

		String type = getType();
		if (feature == null) {

			if (type == null || type.equals("")) { //$NON-NLS-1$
				// ask the Site for the default type 
				type = getSite().getDefaultPackagedFeatureType();
			}

			feature = createFeature(type, getURL(), getSite());
		}

		return feature;
	}

	/**
	 * Returns the update site for the referenced feature
	 * 
	 * @see IFeatureReference#getSite()
	 * @since 2.0 
	 */
	public ISite getSite() {
		return (ISite) getSiteModel();
	}

	/**
	 * Returns an array of categories the referenced feature belong to.
	 * 
	 * @see IFeatureReference#getCategories()
	 * @since 2.0 
	 */
	public ICategory[] getCategories() {

		if (categories == null) {
			categories = new ArrayList();
			String[] categoriesAsString = getCategoryNames();
			for (int i = 0; i < categoriesAsString.length; i++) {
				ICategory siteCat = getSite().getCategory(categoriesAsString[i]);
				if (siteCat != null)
					categories.add(siteCat);
				else {
					//DEBUG
					if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_WARNINGS) {
						String siteURL =
							getSite().getURL() != null ? getSite().getURL().toExternalForm() : null;
						UpdateManagerPlugin.getPlugin().debug(
							"Category " + categoriesAsString[i] + " not found in Site:" + siteURL);
					}
				}
			}
		}

		ICategory[] result = new ICategory[0];

		if (!(categories == null || categories.isEmpty())) {
			result = new ICategory[categories.size()];
			categories.toArray(result);
		}
		return result;
	}

	/**
	 * Adds a category to the referenced feature.
	 * 
	 * @see IFeatureReference#addCategory(ICategory)
	 * @since 2.0 
	 */
	public void addCategory(ICategory category) {
		this.addCategoryName(category.getName());
	}

	/** 
	 * Sets the feature reference URL.
	 * This is typically performed as part of the feature reference creation
	 * operation. Once set, the url should not be reset.
	 * 
	 * @see IFeatureReference#setURL(URL)
	 * @since 2.0 
	 */
	public void setURL(URL url) throws CoreException {
		if (url != null) {
			setURLString(url.toExternalForm());
			try {
				resolve(url, null);
			} catch (MalformedURLException e) {
				throw Utilities.newCoreException(
					Policy.bind("FeatureReference.UnableToResolveURL", url.toExternalForm()),
					e);
				//$NON-NLS-1$
			}
		}
	}

	/**
	 * Associates a site with the feature reference.
	 * This is typically performed as part of the feature reference creation
	 * operation. Once set, the site should not be reset.
	 * 
	 * @see IFeatureReference#setSite(ISite)
	 * @since 2.0 
	 */
	public void setSite(ISite site) {
		setSiteModel((SiteModel) site);
	}

	/*
	 * create an instance of a concrete feature corresponding to this reference
	 */
	private IFeature createFeature(String featureType, URL url, ISite site)
		throws CoreException {
		IFeature feature = null;
		IFeatureFactory factory =
			FeatureTypeFactory.getInstance().getFactory(featureType);
		feature = factory.createFeature(url, site);
		return feature;
	}
}