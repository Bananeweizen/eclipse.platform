package org.eclipse.update.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.*;
import org.eclipse.update.core.*;
import org.eclipse.update.core.model.*;

/**
 * Site on the File System
 */
public class SiteFile extends Site {

	/**
	 * plugin entries 
	 */
	private List pluginEntries = new ArrayList(0);

	/**
	 * 
	 */
	public ISiteContentConsumer createSiteContentConsumer(IFeature targetFeature)
		throws CoreException {
		SiteFileContentConsumer consumer = new SiteFileContentConsumer(targetFeature);
		consumer.setSite(this);
		return consumer;
	}

	/**
	 * @see ISite#getDefaultInstallableFeatureType()
	 */
	public String getDefaultPackagedFeatureType() {
		return DEFAULT_INSTALLED_FEATURE_TYPE;
	}

	/*
	 * @see ISite#install(IFeature, IVerifier, IProgressMonitor)
	 */
	public IFeatureReference install(
		IFeature sourceFeature,
		IVerificationListener verificationListener,
		IProgressMonitor progress)
		throws CoreException {

		if (sourceFeature == null)
			return null;

		// make sure we have an InstallMonitor		
		InstallMonitor monitor;
		if (progress == null)
			monitor = null;
		else if (progress instanceof InstallMonitor)
			monitor = (InstallMonitor) progress;
		else
			monitor = new InstallMonitor(progress);

		// create new executable feature and install source content into it
		IFeature localFeature = createExecutableFeature(sourceFeature);

		IFeatureReference localFeatureReference =
			sourceFeature.install(localFeature, verificationListener, monitor);
		return localFeatureReference;
	}

	/*
	 * @see ISite#install(IFeature,IFeatureContentConsumer, IVerifier,IVerificationLIstener, IProgressMonitor)
	 */
	public IFeatureReference install(
		IFeature sourceFeature,
		IFeatureContentConsumer parentContentConsumer,
		IVerifier parentVerifier,
		IVerificationListener verificationListener,
		IProgressMonitor progress)
		throws CoreException {

		if (sourceFeature == null)
			return null;

		// make sure we have an InstallMonitor		
		InstallMonitor monitor;
		if (progress == null)
			monitor = null;
		else if (progress instanceof InstallMonitor)
			monitor = (InstallMonitor) progress;
		else
			monitor = new InstallMonitor(progress);

		// create new executable feature and install source content into it
		IFeature localFeature = createExecutableFeature(sourceFeature);
		parentContentConsumer.addChild(localFeature);
		
		// set the verifier
		IVerifier vr = sourceFeature.getFeatureContentProvider().getVerifier();
		if (vr!=null) vr.setParent(parentVerifier);

		IFeatureReference localFeatureReference =
			sourceFeature.install(localFeature, verificationListener, monitor);
		return localFeatureReference;
	}

	/*
	 * @see ISite#remove(IFeature, IProgressMonitor)
	 */
	public void remove(IFeature feature, IProgressMonitor progress)
		throws CoreException {

		// make sure we have an InstallMonitor		
		InstallMonitor monitor;
		if (progress == null)
			monitor = null;
		else if (progress instanceof InstallMonitor)
			monitor = (InstallMonitor) progress;
		else
			monitor = new InstallMonitor(progress);

		// Setup optional install handler
		InstallHandlerProxy handler =
			new InstallHandlerProxy(
				IInstallHandler.HANDLER_ACTION_UNINSTALL,
				feature,
				feature.getInstallHandlerEntry(),
				monitor);
		boolean success = false;
		Throwable originalException = null;

		try {
			handler.uninstallInitiated();

			// remove the feature and the plugins if they are not used and not activated
			// get the plugins from the feature
			IPluginEntry[] pluginsToRemove = getPluginEntriesOnlyReferencedBy(feature);

			if (monitor != null) {
				monitor.beginTask(
					Policy.bind("SiteFile.Removing") + feature.getLabel(),
					pluginsToRemove.length + 1);
				//$NON-NLS-1$
			}

			//finds the contentReferences for this IPluginEntry
			for (int i = 0; i < pluginsToRemove.length; i++) {
				remove(feature, pluginsToRemove[i], monitor);
			}

			// remove the feature content
			ContentReference[] references =
				feature.getFeatureContentProvider().getFeatureEntryArchiveReferences(monitor);
			for (int i = 0; i < references.length; i++) {
				try {
					UpdateManagerUtils.removeFromFileSystem(references[i].asFile());
					if (monitor != null)
						monitor.worked(1);
				} catch (IOException e) {
					String id =
						UpdateManagerPlugin.getPlugin().getDescriptor().getUniqueIdentifier();
					throw Utilities.newCoreException(
						Policy.bind(
							"SiteFile.CannotRemoveFeature",
							feature.getVersionedIdentifier().getIdentifier(),
							getURL().toExternalForm()),
						e);
					//$NON-NLS-1$
				}
			}

			handler.completeUninstall();

			// remove feature reference from the site
			IFeatureReference[] featureReferences = getFeatureReferences();
			if (featureReferences != null) {
				for (int indexRef = 0; indexRef < featureReferences.length; indexRef++) {
					IFeatureReference element = featureReferences[indexRef];
					if (element.equals(feature)) {
						removeFeatureReferenceModel((FeatureReferenceModel) element);
						break;
					}
				}
			}

			// remove any children feature
			IFeatureReference[] childrenRef = feature.getIncludedFeatureReferences();
			for (int i = 0; i < childrenRef.length; i++) {
				remove(childrenRef[i].getFeature(), monitor);
			}

			success = true;
		} catch (Throwable t) {
			originalException = t;
		} finally {
			Throwable newException = null;
			try {
				handler.uninstallCompleted(success);
			} catch (Throwable t) {
				newException = t;
			}
			if (originalException != null) // original exception wins
				throw Utilities.newCoreException(
					Policy.bind("InstallHandler.error", feature.getLabel()),
					originalException);
			//$NON-NLS-1$
			if (newException != null)
				throw Utilities.newCoreException(
					Policy.bind("InstallHandler.error", feature.getLabel()),
					newException);
			//$NON-NLS-1$
		}
	}

	/**
	 * returns the download size
	 * of the feature to be installed on the site.
	 * If the site is <code>null</code> returns the maximum size
	 * 
	 * If one plug-in entry has an unknown size.
	 * then the download size is unknown.
	 * 
	 * @see ISite#getDownloadSize(IFeature)
	 * 
	 */
	public long getDownloadSizeFor(IFeature feature) {
		long result = 0;
		IPluginEntry[] entriesToInstall = feature.getPluginEntries();
		IPluginEntry[] siteEntries = this.getPluginEntries();
		entriesToInstall = UpdateManagerUtils.diff(entriesToInstall, siteEntries);

		// FIXME Intersection for NonPluginEntry (using Install Handler)
		try {
			result =
				feature.getFeatureContentProvider().getDownloadSizeFor(entriesToInstall,
			/* non plugin entry []*/
			null);
		} catch (CoreException e) {
			UpdateManagerPlugin.warn(null,e);
			result = ContentEntryModel.UNKNOWN_SIZE;
		}
		return result;
	}

	/**
	 * returns the download size
	 * of the feature to be installed on the site.
	 * If the site is <code>null</code> returns the maximum size
	 * 
	 * If one plug-in entry has an unknown size.
	 * then the download size is unknown.
	 * 
	 * @see ISite#getDownloadSizeFor(IFeature)
	 * 
	 */
	public long getInstallSizeFor(IFeature feature) {
		long result = 0;
		IPluginEntry[] entriesToInstall = feature.getPluginEntries();
		IPluginEntry[] siteEntries = this.getPluginEntries();
		entriesToInstall = UpdateManagerUtils.diff(entriesToInstall, siteEntries);

		// FIXME Intersection for NonPluginEntry (using Install Handler)
		try {
			result =
				feature.getFeatureContentProvider().getInstallSizeFor(entriesToInstall,
			/* non plugin entry []*/
			null);
		} catch (CoreException e) {
			UpdateManagerPlugin.warn(null,e);
			result = ContentEntryModel.UNKNOWN_SIZE;
		}

		return result;
	}

	/**
	 * Adds a plugin entry 
	 * Either from parsing the file system or 
	 * installing a feature
	 * 
	 * We cannot figure out the list of plugins by reading the Site.xml as
	 * the archives tag are optionals
	 */
	public void addPluginEntry(IPluginEntry pluginEntry) {
		pluginEntries.add(pluginEntry);
	}

	/**
	 * @see IPluginContainer#getPluginEntries()
	 */
	public IPluginEntry[] getPluginEntries() {
		IPluginEntry[] result = new IPluginEntry[0];
		if (!(pluginEntries == null || pluginEntries.isEmpty())) {
			result = new IPluginEntry[pluginEntries.size()];
			pluginEntries.toArray(result);
		}
		return result;
	}

	/**
	 * @see IPluginContainer#getPluginEntryCount()
	 */
	public int getPluginEntryCount() {
		return getPluginEntries().length;
	}

	/**
	 * 
	 */
	private IFeature createExecutableFeature(IFeature sourceFeature)
		throws CoreException {
		IFeature result = null;
		IFeatureFactory factory =
			FeatureTypeFactory.getInstance().getFactory(DEFAULT_INSTALLED_FEATURE_TYPE);
		result = factory.createFeature(/*URL*/
		null, this);

		// at least set the version identifier to be the same
		((FeatureModel) result).setFeatureIdentifier(
			sourceFeature.getVersionedIdentifier().getIdentifier());
		((FeatureModel) result).setFeatureVersion(
			sourceFeature.getVersionedIdentifier().getVersion().toString());
		return result;
	}

	/**
	 * 
	 */
	private void remove(
		IFeature feature,
		IPluginEntry pluginEntry,
		InstallMonitor monitor)
		throws CoreException {

		if (pluginEntry == null)
			return;

		ContentReference[] references =
			feature.getFeatureContentProvider().getPluginEntryArchiveReferences(
				pluginEntry,
				monitor);
		for (int i = 0; i < references.length; i++) {
			try {
				UpdateManagerUtils.removeFromFileSystem(references[i].asFile());
				if (monitor != null)
					monitor.worked(1);
			} catch (IOException e) {
				throw Utilities.newCoreException(
					Policy.bind(
						"SiteFile.CannotRemovePlugin",
						pluginEntry.getVersionedIdentifier().toString(),
						getURL().toExternalForm()),
					e);
				//$NON-NLS-1$
			}
		}
	}
}