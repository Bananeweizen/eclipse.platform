package org.eclipse.update.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.*;
import org.eclipse.update.core.*;
import org.eclipse.update.core.model.URLEntryModel;

/**
 * Factory for Feature Packaged
 */
public class FeaturePackagedFactory extends BaseFeatureFactory {

	/*
	 * @see IFeatureFactory#createFeature(URL,ISite)
	 */
	public IFeature createFeature(URL url,ISite site) throws CoreException {
		Feature feature = null;
		InputStream featureStream = null;
		
		try {		
			
			IFeatureContentProvider contentProvider = new FeaturePackagedContentProvider(url);		
			ContentReference manifest = contentProvider.getFeatureManifestReference(null/*IProgressMonitor*/);
			featureStream = manifest.getInputStream();
			feature = (Feature)parseFeature(featureStream);
			
			// if there is no update URL for the Feature
			// use the Site URL
			if (feature.getUpdateSiteEntry()==null){
				URLEntryModel entryModel = createURLEntryModel();
				URL siteUrl = site.getURL();
				if (siteUrl!=null){
					entryModel.setURLString(siteUrl.toExternalForm());
					entryModel.resolve(siteUrl,null);
					feature.setUpdateSiteEntryModel(entryModel);
				}
			}	
					
			feature.setFeatureContentProvider(contentProvider);
			feature.setSite(site);						
			URL baseUrl = null;
			try {
				baseUrl = new URL(manifest.asURL(),"."); // make sure we have URL to feature directory //$NON-NLS-1$
			} catch(MalformedURLException e) {	
			}
			feature.resolve(baseUrl, getResourceBundle(baseUrl));
			feature.markReadOnly();			
			
		} catch (IOException e) {
			// if we cannot find the feature or the feature.xml...
			// We should not stop the execution 
			// but we must Log it all the time, not only when debugging...
			String id = UpdateManagerPlugin.getPlugin().getDescriptor().getUniqueIdentifier();
			IStatus status = new Status(IStatus.WARNING, id, IStatus.OK, "IOException opening the 'feature.xml' stream in the feature archive:"  + url.toExternalForm(), e); //$NON-NLS-1$
			UpdateManagerPlugin.getPlugin().getLog().log(status);
		} catch (Exception e) { 
			// other errors, we stop execution
			if (e instanceof CoreException) {
				throw (CoreException)e;
			};
			throw Utilities.newCoreException(Policy.bind("FeatureFactory.ParsingError", url.toExternalForm()), e); //$NON-NLS-1$
		}finally {
			try {
				featureStream.close();
			} catch (Exception e) {
			}
		}
		return feature;
	}

}
