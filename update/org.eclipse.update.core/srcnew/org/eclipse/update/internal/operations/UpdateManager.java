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
package org.eclipse.update.internal.operations;


import java.lang.reflect.*;
import java.text.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.core.*;
import org.eclipse.update.operations.*;


/**
 * Helper class for performing update related tasks, queries, etc.
 * All the methods are static and this class should be a singleton.
 */
public class UpdateManager {
	private static final String KEY_SAVED_CONFIG =
		"MultiInstallWizard.savedConfig";
	private static final String RESOURCE_BUNDLE =
		"org.eclipse.update.internal.operations.UpdateManagerResources";
	private static ResourceBundle bundle;
	private static UpdateManagerAuthenticator authenticator;
	private static IOperationValidator validator;
	
	private static final String PREFIX = UpdateCore.getPlugin().getDescriptor().getUniqueIdentifier();
	public static final String P_MAPPINGS_FILE = PREFIX + ".mappingsFile";

	static {
		bundle = ResourceBundle.getBundle(RESOURCE_BUNDLE);
	}

	/**
	 * Private constructor
	 */
	private UpdateManager() {
	}
	
	/**
	 * Returns the string from the plugin's resource bundle,
	 * or 'key' if not found.
	 */
	public static String getString(String key) {
		try {
			return bundle.getString(key);
		} catch (MissingResourceException e) {
			return key;
		}
	}

	public static String getFormattedMessage(String key, String[] args) {
		String text = getString(key);
		return MessageFormat.format(text, args);
	}

	public static String getFormattedMessage(String key, String arg) {
		String text = getString(key);
		return MessageFormat.format(text, new String[] { arg });
	}

	public static String getPluginId() {
		return UpdateCore.getPlugin().getDescriptor().getUniqueIdentifier();
	}


	public static void logException(Throwable e) {
		logException(e, true);
	}

	public static void logException(Throwable e, boolean showErrorDialog) {
		if (e instanceof InvocationTargetException) {
			e = ((InvocationTargetException) e).getTargetException();
		}

		IStatus status = null;
		if (e instanceof CoreException) {
			status = ((CoreException) e).getStatus();
		} else {
			String message = e.getMessage();
			if (message == null)
				message = e.toString();
			status = new Status(IStatus.ERROR, getPluginId(), IStatus.OK, message, e);
		}
		log(status, showErrorDialog);
	}

	public static void log(IStatus status, boolean showErrorDialog) {
		if (status.getSeverity() != IStatus.INFO) {
//			if (showErrorDialog)
//				ErrorDialog.openError(getActiveWorkbenchShell(), null, null, status);
			//ResourcesPlugin.getPlugin().getLog().log(status);
			Platform.getPlugin("org.eclipse.core.runtime").getLog().log(status);
		} else {
//			MessageDialog.openInformation(getActiveWorkbenchShell(), null, status.getMessage());
		}
	}

	public static IFeature[] searchSite(String featureId, IConfiguredSite site, boolean onlyConfigured) throws CoreException {
		IFeatureReference[] references = null;

		if (onlyConfigured)
			references = site.getConfiguredFeatures();
		else
			references = site.getSite().getFeatureReferences();
		Vector result = new Vector();

		for (int i = 0; i < references.length; i++) {
			IFeature feature = references[i].getFeature(null);
			String id = feature.getVersionedIdentifier().getIdentifier();
			if (featureId.equals(id)) {
				result.add(feature);
			}
		}
		return (IFeature[]) result.toArray(new IFeature[result.size()]);
	}

	public static IFeature[] getInstalledFeatures(IFeature feature) {
		return getInstalledFeatures(feature, true);
	}

	public static IFeature[] getInstalledFeatures(IFeature feature, boolean onlyConfigured) {
		return getInstalledFeatures(feature.getVersionedIdentifier(), onlyConfigured);
	}
	public static IFeature[] getInstalledFeatures(VersionedIdentifier vid, boolean onlyConfigured) {
		Vector features = new Vector();
		try {
			ILocalSite localSite = SiteManager.getLocalSite();
			IInstallConfiguration config = localSite.getCurrentConfiguration();
			IConfiguredSite[] isites = config.getConfiguredSites();
			String id = vid.getIdentifier();

			for (int i = 0; i < isites.length; i++) {
				IConfiguredSite isite = isites[i];
				IFeature[] result = UpdateManager.searchSite(id, isite, onlyConfigured);
				for (int j = 0; j < result.length; j++) {
					IFeature installedFeature = result[j];
					features.add(installedFeature);
				}
			}
		} catch (CoreException e) {
			UpdateManager.logException(e);
		}
		return (IFeature[]) features.toArray(new IFeature[features.size()]);
	}
	
	public static boolean isPatch(IFeature candidate) {
		IImport[] imports = candidate.getImports();

		for (int i = 0; i < imports.length; i++) {
			IImport iimport = imports[i];
			if (iimport.isPatch()) return true;
		}
		return false;
	}

	public static boolean isPatch(IFeature target, IFeature candidate) {
		VersionedIdentifier vid = target.getVersionedIdentifier();
		IImport[] imports = candidate.getImports();

		for (int i = 0; i < imports.length; i++) {
			IImport iimport = imports[i];
			if (iimport.isPatch()) {
				VersionedIdentifier ivid = iimport.getVersionedIdentifier();
				if (vid.equals(ivid)) {
					// Bingo.
					return true;
				}
			}
		}
		return false;
	}

	public static IInstallConfiguration getBackupConfigurationFor(IFeature feature) {
		VersionedIdentifier vid = feature.getVersionedIdentifier();
		String key = "@" + vid.getIdentifier() + "_" + vid.getVersion();
		try {
			ILocalSite lsite = SiteManager.getLocalSite();
			IInstallConfiguration[] configs = lsite.getPreservedConfigurations();
			for (int i = 0; i < configs.length; i++) {
				IInstallConfiguration config = configs[i];
				if (config.getLabel().startsWith(key))
					return config;
			}
		} catch (CoreException e) {
		}
		return null;
	}
	
	
	public static boolean hasLicense(IFeature feature) {
		IURLEntry info = feature.getLicense();
		if (info == null)
			return false;
		String licenseTxt = info.getAnnotation();
		if (licenseTxt == null)
			return false;
		return licenseTxt.trim().length() > 0;
	}
	public static boolean hasOptionalFeatures(IFeatureReference fref) {
		try {
			return hasOptionalFeatures(fref.getFeature(null));
		} catch (CoreException e) {
			return false;
		}
	}
	public static boolean hasOptionalFeatures(IFeature feature) {
		try {
			IIncludedFeatureReference[] irefs = feature.getIncludedFeatureReferences();
			for (int i = 0; i < irefs.length; i++) {
				IIncludedFeatureReference iref = irefs[i];
				if (iref.isOptional())
					return true;
				// see if it has optional children
				IFeature child = iref.getFeature(null);
				if (hasOptionalFeatures(child))
					return true;
			}
		} catch (CoreException e) {
		}
		return false;
	}

	public static IFeature getLocalFeature(
		IConfiguredSite csite,
		IFeature feature)
		throws CoreException {
		IFeatureReference[] refs = csite.getConfiguredFeatures();
		for (int i = 0; i < refs.length; i++) {
			IFeatureReference ref = refs[i];
			VersionedIdentifier refVid = ref.getVersionedIdentifier();
			if (feature.getVersionedIdentifier().equals(refVid))
				return ref.getFeature(null);
		}
		return null;
	}
	
	public static IConfiguredSite getConfigSite(
		IFeature feature,
		IInstallConfiguration config)
		throws CoreException {
		IConfiguredSite[] configSites = config.getConfiguredSites();
		for (int i = 0; i < configSites.length; i++) {
			IConfiguredSite site = configSites[i];
			if (site.getSite().equals(feature.getSite())) {
				return site;
			}
		}
		return null;
	}
	
	public static IConfiguredSite getDefaultTargetSite(
		IInstallConfiguration config,
		IInstallFeatureOperation pendingChange) {
		return getDefaultTargetSite(config, pendingChange, true);
	}

	public  static IConfiguredSite getDefaultTargetSite(
		IInstallConfiguration config,
		IInstallFeatureOperation pendingChange,
		boolean checkAffinityFeature) {
		IFeature oldFeature = pendingChange.getOldFeature();
		IFeature newFeature = pendingChange.getFeature();
		if (oldFeature != null) {
			// We should install into the same site as
			// the old feature
			try {
				return getConfigSite(oldFeature, config);
			} catch (CoreException e) {
				logException(e);
				return null;
			}
		}

		// This is a new install. Check if there is 
		// a disabled feature with the same ID
		String newFeatureID =
			newFeature.getVersionedIdentifier().getIdentifier();
		IConfiguredSite sameSite = getSiteWithFeature(config, newFeatureID);
		if (sameSite != null) {
			return sameSite;
		}

		if (checkAffinityFeature) {
			return getAffinitySite(config, newFeature);
		}
		return null;
	}
	
	public static IConfiguredSite getAffinitySite(
		IInstallConfiguration config,
		IFeature newFeature) {
		// check if the affinity feature is installed
		String affinityID = newFeature.getAffinityFeature();
		if (affinityID != null) {
			IConfiguredSite affinitySite =
				getSiteWithFeature(config, affinityID);
			if (affinitySite != null)
				return affinitySite;
		}
		return null;
	}

	public static IConfiguredSite getSiteWithFeature(
		IInstallConfiguration config,
		String featureID) {
		if (featureID == null)
			return null;
		IConfiguredSite[] sites = config.getConfiguredSites();
		for (int i = 0; i < sites.length; i++) {
			IConfiguredSite site = sites[i];
			IFeatureReference[] refs = site.getFeatureReferences();
			for (int j = 0; j < refs.length; j++) {
				IFeatureReference ref = refs[j];
				try {
					IFeature feature = ref.getFeature(null);
					if (featureID
						.equals(
							feature
								.getVersionedIdentifier()
								.getIdentifier())) {
						// found it
						return site;
					}
				} catch (CoreException e) {
					logException(e);
				}
			}
		}
		return null;
	}
	
	public static void collectOldFeatures(
		IFeature feature,
		IConfiguredSite targetSite,
		ArrayList result)
		throws CoreException {
		IIncludedFeatureReference[] included =
			feature.getIncludedFeatureReferences();
		for (int i = 0; i < included.length; i++) {
			IIncludedFeatureReference iref = included[i];

			IFeature ifeature;

			try {
				ifeature = iref.getFeature(null);
			} catch (CoreException e) {
				if (iref.isOptional())
					continue;
				throw e;
			}
			// find other features and unconfigure
			String id = iref.getVersionedIdentifier().getIdentifier();
			IFeature[] sameIds = UpdateManager.searchSite(id, targetSite, true);
			for (int j = 0; j < sameIds.length; j++) {
				IFeature sameId = sameIds[j];
				// Ignore self.
				if (sameId.equals(ifeature))
					continue;
				result.add(sameId);
			}
			collectOldFeatures(ifeature, targetSite, result);
		}
	}
	
	public static void saveLocalSite() throws CoreException {
		ILocalSite localSite = SiteManager.getLocalSite();
		localSite.save();
	}


	public static IInstallConfiguration createInstallConfiguration() throws CoreException{
		try {
			ILocalSite localSite = SiteManager.getLocalSite();
			IInstallConfiguration config =
				localSite.cloneCurrentConfiguration();
			config.setLabel(Utilities.format(config.getCreationDate()));
			return config;
		} catch (CoreException e) {
			// Let callers handle logging
			//logException(e);
			throw e;
		}
	}

	public static void makeConfigurationCurrent(
		IInstallConfiguration config,
		IInstallFeatureOperation job)
		throws CoreException {
		ILocalSite localSite = SiteManager.getLocalSite();
		if (job != null && job.getFeature().isPatch()) {
			// Installing a patch - preserve the current configuration
			IInstallConfiguration cconfig =
				localSite.getCurrentConfiguration();
			IInstallConfiguration savedConfig =
				localSite.addToPreservedConfigurations(cconfig);
			VersionedIdentifier vid =
				job.getFeature().getVersionedIdentifier();
			String key = "@" + vid.getIdentifier() + "_" + vid.getVersion();
			String newLabel =
				getFormattedMessage(KEY_SAVED_CONFIG, key);
			savedConfig.setLabel(newLabel);
			OperationsManager.fireObjectChanged(savedConfig, null);
		}
		localSite.addConfiguration(config);
	}

	public static boolean isNestedChild(IInstallConfiguration config, IFeature feature) {
		IConfiguredSite[] csites = config.getConfiguredSites();
		try {
			for (int i = 0; csites != null && i < csites.length; i++) {
				IFeatureReference[] refs = csites[i].getConfiguredFeatures();
				for (int j = 0; refs != null && j < refs.length; j++) {
					IFeature parent = refs[j].getFeature(null);
					IFeatureReference[] children =
						parent.getIncludedFeatureReferences();
					for (int k = 0;
						children != null && k < children.length;
						k++) {
						IFeature child = children[k].getFeature(null);
						if (feature.equals(child))
							return true;
					}
				}
			}
		} catch (CoreException e) {
			// will return false
		}
		return false;
	}
	
	
	public static boolean hasObsoletePatches(IFeature feature) {
		// Check all the included features that
		// are unconfigured, and see if their patch 
		// references are better than the original.
		try {
			IFeatureReference[] irefs = feature.getIncludedFeatureReferences();
			for (int i = 0; i < irefs.length; i++) {
				IFeatureReference iref = irefs[i];
				IFeature ifeature = iref.getFeature(null);
				IConfiguredSite csite = ifeature.getSite().getCurrentConfiguredSite();
				if (!csite.isConfigured(ifeature)) {
					if (!isPatchHappy(ifeature))
						return false;
				}
			}
		} catch (CoreException e) {
			return false;
		}
		// All checks went well
		return true;
	}
	
	public static boolean isPatchHappy(IFeature feature) throws CoreException {
		// If this is a patch and it includes 
		// another patch and the included patch
		// is disabled but the feature it was declared
		// to patch is now newer (and is presumed to
		// contain the now disabled patch), and
		// the newer patched feature is enabled,
		// a 'leap of faith' assumption can be
		// made:

		// Although the included patch is disabled,
		// the feature it was designed to patch
		// is now newer and most likely contains
		// the equivalent fix and more. Consequently,
		// we can claim that the status and the error
		// icon overlay are misleading because
		// all the right plug-ins are configured.
		IImport[] imports = feature.getImports();
		IImport patchReference = null;
		for (int i = 0; i < imports.length; i++) {
			IImport iimport = imports[i];
			if (iimport.isPatch()) {
				patchReference = iimport;
				break;
			}
		}
		if (patchReference == null)
			return false;
		VersionedIdentifier refVid = patchReference.getVersionedIdentifier();

		// Find the patched feature and 
		IConfiguredSite csite = feature.getSite().getCurrentConfiguredSite();
		if (csite == null)
			return false;

		IFeatureReference[] crefs = csite.getConfiguredFeatures();
		for (int i = 0; i < crefs.length; i++) {
			IFeatureReference cref = crefs[i];
			VersionedIdentifier cvid = cref.getVersionedIdentifier();
			if (cvid.getIdentifier().equals(refVid.getIdentifier())) {
				// This is the one.
				if (cvid.getVersion().isGreaterThan(refVid.getVersion())) {
					// Bingo: we found the referenced feature
					// and its version is greater - 
					// we can assume that it contains better code
					// than the patch that referenced the
					// older version.
					return true;
				}
			}
		}
		return false;
	}


	
	/**
	 * Gets the authenticator.
	 * @return Returns a UpdateManagerAuthenticator
	 */
	public static UpdateManagerAuthenticator getAuthenticator() {
		if (authenticator == null)
			authenticator = new UpdateManagerAuthenticator();
		return authenticator;
	}

	public static IOperationValidator getValidator() {
		if (validator == null)
			// in the future this will be pluggable
			validator = new OperationValidator();
		return validator;
	}
}
