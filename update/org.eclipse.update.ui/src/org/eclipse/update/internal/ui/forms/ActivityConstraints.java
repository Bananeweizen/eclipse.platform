package org.eclipse.update.internal.ui.forms;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.core.boot.BootLoader;
import org.eclipse.core.runtime.*;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.ui.UpdateUIPlugin;
import org.eclipse.update.internal.ui.model.PendingChange;

/**
 *
 */
public class ActivityConstraints {
	private static final String KEY_ROOT_MESSAGE =
		"ActivityConstraints.rootMessage";
	private static final String KEY_ROOT_MESSAGE_INIT =
		"ActivityConstraints.rootMessageInitial";
	private static final String KEY_CHILD_MESSAGE =
		"ActivityConstraints.childMessage";
	private static final String KEY_PLATFORM = "ActivityConstraints.platform";
	private static final String KEY_PRIMARY = "ActivityConstraints.primary";
	private static final String KEY_OS = "ActivityConstraints.os";
	private static final String KEY_WS = "ActivityConstraints.ws";
	private static final String KEY_ARCH = "ActivityConstraints.arch";
	private static final String KEY_PREREQ = "ActivityConstraints.prereq";
	private static final String KEY_PREREQ_PERFECT =
		"ActivityConstraints.prereqPerfect";
	private static final String KEY_PREREQ_EQUIVALENT =
		"ActivityConstraints.prereqEquivalent";
	private static final String KEY_PREREQ_COMPATIBLE =
		"ActivityConstraints.prereqCompatible";
	private static final String KEY_PREREQ_GREATER =
		"ActivityConstraints.prereqGreaterOrEqual";
	private static final String KEY_CYCLE = "ActivityConstraints.cycle";
	private static final String KEY_CONFLICT = "ActivityConstraints.conflict";

	/*
	 * Called by UI before performing operation
	 */
	public static IStatus validatePendingChange(PendingChange job) {
		// check initial state
		ArrayList beforeStatus = new ArrayList();
		validateInitialState(beforeStatus);

		// check proposed change
		ArrayList status = new ArrayList();
		switch (job.getJobType()) {
			case PendingChange.UNCONFIGURE :
				validateUnconfigure(job.getFeature(), status);
				break;
			case PendingChange.CONFIGURE :
				validateConfigure(job.getFeature(), status);
				break;
			case PendingChange.INSTALL :
				validateInstall(job.getOldFeature(), job.getFeature(), status);
				break;
		}

		// report status
		if (status.size() > 0) {
			if (beforeStatus.size() > 0)
				return createMultiStatus(KEY_ROOT_MESSAGE_INIT, beforeStatus);
			else
				return createMultiStatus(KEY_ROOT_MESSAGE, status);
		}
		return null;
	}

	/*
	 * Called by UI before processing a delta
	 */
	public static IStatus validateSessionDelta(ISessionDelta delta) {
		// check initial state
		ArrayList beforeStatus = new ArrayList();
		validateInitialState(beforeStatus);

		// check proposed change
		ArrayList status = new ArrayList();
		switch (delta.getType()) {
			case ISessionDelta.ENABLE :
				validateDeltaConfigure(delta, status);
				break;
		}

		// report status
		if (status.size() > 0) {
			if (beforeStatus.size() > 0)
				return createMultiStatus(KEY_ROOT_MESSAGE_INIT, beforeStatus);
			else
				return createMultiStatus(KEY_ROOT_MESSAGE, status);
		}
		return null;
	}

	/*
	 * Called by the UI before doing a revert/ restore operation
	 */
	public static IStatus validatePendingRevert(IInstallConfiguration config) {
		// check initial state
		ArrayList beforeStatus = new ArrayList();
		validateInitialState(beforeStatus);

		// check proposed change
		ArrayList status = new ArrayList();
		validateRevert(config, status);

		// report status
		if (status.size() > 0) {
			if (beforeStatus.size() > 0)
				return createMultiStatus(KEY_ROOT_MESSAGE_INIT, beforeStatus);
			else
				return createMultiStatus(KEY_ROOT_MESSAGE, status);
		}
		return null;
	}

	/*
	 * Called by the UI before doing a one-click update operation
	 */
	public static IStatus validatePendingOneClickUpdate(PendingChange[] jobs) {
		// check initial state
		ArrayList beforeStatus = new ArrayList();
		validateInitialState(beforeStatus);

		// check proposed change
		ArrayList status = new ArrayList();
		validateOneClickUpdate(jobs, status);

		// report status
		if (status.size() > 0) {
			if (beforeStatus.size() > 0)
				return createMultiStatus(KEY_ROOT_MESSAGE_INIT, beforeStatus);
			else
				return createMultiStatus(KEY_ROOT_MESSAGE, status);
		}
		return null;
	}

	/*
	 * Check to see if we are not broken even before we start
	 */
	private static void validateInitialState(ArrayList status) {
		try {
			ArrayList features = computeFeatures();
			checkConstraints(features, status);
		} catch (CoreException e) {
			status.add(e.getStatus());
		}
	}

	/*
	 * handle unconfigure
	 */
	private static void validateUnconfigure(
		IFeature feature,
		ArrayList status) {
		try {
			ArrayList features = computeFeatures();
			features = computeFeaturesAfterOperation(features, null, feature);
			checkConstraints(features, status);

		} catch (CoreException e) {
			status.add(e.getStatus());
		}
	}

	/*
	 * handle configure
	 */
	private static void validateConfigure(IFeature feature, ArrayList status) {
		try {
			ArrayList features = computeFeatures();
			features = computeFeaturesAfterOperation(features, feature, null);
			checkConstraints(features, status);

		} catch (CoreException e) {
			status.add(e.getStatus());
		}
	}

	/*
	 * handle install and update
	 */
	private static void validateInstall(
		IFeature oldFeature,
		IFeature newFeature,
		ArrayList status) {
		try {
			ArrayList features = computeFeatures();
			features =
				computeFeaturesAfterOperation(features, newFeature, oldFeature);
			checkConstraints(features, status);

		} catch (CoreException e) {
			status.add(e.getStatus());
		}
	}

	/*
	 * handle revert and restore
	 */
	private static void validateRevert(
		IInstallConfiguration config,
		ArrayList status) {
		try {
			ArrayList features = computeFeaturesAfterRevert(config);
			checkConstraints(features, status);
			checkRevertConstraints(features, status);

		} catch (CoreException e) {
			status.add(e.getStatus());
		}
	}

	/*
	 * Handle delta addition
	 */
	private static void validateDeltaConfigure(
		ISessionDelta delta,
		ArrayList status) {
		try {
			ArrayList features = computeFeaturesAfterDelta(delta);
			checkConstraints(features, status);

		} catch (CoreException e) {
			status.add(e.getStatus());
		}
	}

	/*
	 * Handle one-click changes as a batch
	 */
	private static void validateOneClickUpdate(
		PendingChange[] jobs,
		ArrayList status) {
		try {
			ArrayList features = computeFeatures();
			for (int i = 0; i < jobs.length; i++) {
				IFeature newFeature = jobs[i].getFeature();
				IFeature oldFeature = jobs[i].getOldFeature();
				features =
					computeFeaturesAfterOperation(
						features,
						newFeature,
						oldFeature);
				checkConstraints(features, status);
				if (status.size() > 0) {
					IStatus conflict =
						createStatus(
							newFeature,
							UpdateUIPlugin.getResourceString(KEY_CONFLICT));
					status.add(conflict);
					return;
				}
			}

		} catch (CoreException e) {
			status.add(e.getStatus());
		}
	}

	/*
	 * Compute a list of configured features
	 */
	private static ArrayList computeFeatures() throws CoreException {

		ArrayList features = new ArrayList();
		ILocalSite localSite = SiteManager.getLocalSite();
		IInstallConfiguration config = localSite.getCurrentConfiguration();
		IConfiguredSite[] csites = config.getConfiguredSites();

		for (int i = 0; i < csites.length; i++) {
			IConfiguredSite csite = csites[i];
			IFeatureReference[] crefs = csite.getConfiguredFeatures();
			for (int j = 0; j < crefs.length; j++) {
				IFeatureReference cref = crefs[j];
				IFeature cfeature = cref.getFeature();
				features.add(cfeature);
			}
		}

		return features;
	}

	/*
	 * Compute the nested feature subtree starting at the specified base feature
	 */
	private static ArrayList computeFeatureSubtree(
		IFeature top,
		IFeature feature,
		ArrayList features,
		boolean tolerateMissingChildren)
		throws CoreException {

		// check arguments
		if (features == null)
			features = new ArrayList();
		if (top == null)
			return features;
		if (feature == null)
			feature = top;

		// check for <includes> cycle
		if (features.contains(feature)) {
			IStatus status =
				createStatus(top, UpdateUIPlugin.getResourceString(KEY_CYCLE));
			throw new CoreException(status);
		}

		// return specified base feature and all its children
		features.add(feature);
		IFeatureReference[] children = feature.getIncludedFeatureReferences();
		for (int i = 0; i < children.length; i++) {
			try {
				IFeature child = children[i].getFeature();
				features =
					computeFeatureSubtree(
						top,
						child,
						features,
						tolerateMissingChildren);
			} catch (CoreException e) {
				if (!tolerateMissingChildren)
					throw e;
			}
		}
		return features;
	}

	/*
	 * Compute a list of features that will be configured after the operation
	 */
	private static ArrayList computeFeaturesAfterOperation(
		ArrayList features,
		IFeature add,
		IFeature remove)
		throws CoreException {

		ArrayList addTree =
			computeFeatureSubtree(
				add,
				null,
				null,
				false /* do not tolerate missing children */
		);
		ArrayList removeTree =
			computeFeatureSubtree(
				remove,
				null,
				null,
				true /* tolerate missing children */
		);

		if (add != null)
			features.addAll(addTree);

		if (remove != null)
			features.removeAll(removeTree);

		return features;
	}

	/*
	 * Compute a list of features that will be configured after performing the revert
	 */
	private static ArrayList computeFeaturesAfterRevert(IInstallConfiguration config)
		throws CoreException {

		ArrayList list = new ArrayList();
		IConfiguredSite[] csites = config.getConfiguredSites();
		for (int i = 0; i < csites.length; i++) {
			IConfiguredSite csite = csites[i];
			IFeatureReference[] features = csite.getConfiguredFeatures();
			for (int j = 0; j < features.length; j++) {
				list.add(features[j].getFeature());
			}
		}
		return list;
	}

	/*
	 * Compute a list of features that will be configured after applying the
	 * specified delta
	 */
	private static ArrayList computeFeaturesAfterDelta(ISessionDelta delta)
		throws CoreException {

		IFeatureReference[] deltaRefs;
		if (delta == null)
			deltaRefs = new IFeatureReference[0];
		else
			deltaRefs = delta.getFeatureReferences();

		ArrayList features = new ArrayList(); // cumulative results list
		ILocalSite localSite = SiteManager.getLocalSite();
		IInstallConfiguration config = localSite.getCurrentConfiguration();
		IConfiguredSite[] csites = config.getConfiguredSites();

		// compute changes for each site
		for (int i = 0; i < csites.length; i++) {
			IConfiguredSite csite = csites[i];
			ArrayList siteFeatures = new ArrayList();

			// collect currently configured features on site
			IFeatureReference[] crefs = csite.getConfiguredFeatures();
			for (int j = 0; crefs != null && j < crefs.length; j++) {
				IFeatureReference cref = crefs[j];
				IFeature cfeature = cref.getFeature();
				siteFeatures.add(cfeature);
			}

			// add deltas for the site			
			for (int j = 0; j < deltaRefs.length; j++) {
				ISite deltaSite = deltaRefs[j].getSite();
				if (deltaSite.equals(csite.getSite())) {
					IFeature dfeature = deltaRefs[j].getFeature();
					if (!siteFeatures.contains(dfeature)) // don't add dups
						siteFeatures.add(dfeature);
				}
			}

			// reduce the list if needed	
			IFeature[] array =
				(IFeature[]) siteFeatures.toArray(
					new IFeature[siteFeatures.size()]);
			for (int j = 0; j < array.length; j++) {
				VersionedIdentifier id1 = array[j].getVersionedIdentifier();
				for (int k = 0; k < array.length; k++) {
					if (j == k)
						continue;
					VersionedIdentifier id2 = array[k].getVersionedIdentifier();
					if (id1.getIdentifier().equals(id2.getIdentifier())) {
						if (id2.getVersion().isGreaterThan(id1.getVersion())) {
							siteFeatures.remove(array[j]);
							break;
						}
					}
				}
			}

			// accumulate site results
			features.addAll(siteFeatures);
		}

		return features;
	}

	/*
	 * Compute a list of plugin entries for the specified features.
	 */
	private static ArrayList computePluginsForFeatures(ArrayList features)
		throws CoreException {
		if (features == null)
			return new ArrayList();

		HashMap plugins = new HashMap();
		for (int i = 0; i < features.size(); i++) {
			IFeature feature = (IFeature) features.get(i);
			IPluginEntry[] entries = feature.getPluginEntries();
			for (int j = 0; j < entries.length; j++) {
				IPluginEntry entry = entries[j];
				plugins.put(entry.getVersionedIdentifier(), entry);
			}
		}
		ArrayList result = new ArrayList();
		result.addAll(plugins.values());
		return result;
	}

	/*
	 * validate constraints
	 */
	private static void checkConstraints(ArrayList features, ArrayList status)
		throws CoreException {
		if (features == null)
			return;

		ArrayList plugins = computePluginsForFeatures(features);

		checkEnvironment(features, status);
		checkPlatformFeature(features, plugins, status);
		checkPrimaryFeature(features, status);
		checkPrereqs(features, plugins, status);
	}

	/*
	 * Verify all features are either portable, or match the current environment
	 */
	private static void checkEnvironment(
		ArrayList features,
		ArrayList status) {

		String os = BootLoader.getOS();
		String ws = BootLoader.getWS();
		String arch = BootLoader.getOSArch();

		for (int i = 0; i < features.size(); i++) {
			IFeature feature = (IFeature) features.get(i);
			String fos = feature.getOS();
			String fws = feature.getWS();
			String farch = feature.getArch();

			if (fos != null && !fos.equals("")) {
				if (!os.equals(fos)) {
					status.add(
						createStatus(
							feature,
							UpdateUIPlugin.getResourceString(KEY_OS)));
					continue;
				}
			}

			if (fws != null && !fws.equals("")) {
				if (!ws.equals(fws)) {
					status.add(
						createStatus(
							feature,
							UpdateUIPlugin.getResourceString(KEY_WS)));
					continue;
				}
			}

			if (farch != null && !farch.equals("")) {
				if (!arch.equals(farch)) {
					status.add(
						createStatus(
							feature,
							UpdateUIPlugin.getResourceString(KEY_ARCH)));
					continue;
				}
			}
		}
	}

	/*
	 * Verify we end up with a version of platform configured
	 */
	private static void checkPlatformFeature(
		ArrayList features,
		ArrayList plugins,
		ArrayList status) {

		String[] bootstrapPlugins =
			BootLoader
				.getCurrentPlatformConfiguration()
				.getBootstrapPluginIdentifiers();

		for (int i = 0; i < bootstrapPlugins.length; i++) {
			boolean found = false;
			for (int j = 0; j < plugins.size(); j++) {
				IPluginEntry plugin = (IPluginEntry) plugins.get(j);
				if (bootstrapPlugins[i]
					.equals(plugin.getVersionedIdentifier().getIdentifier())) {
					found = true;
					break;
				}
			}
			if (!found) {
				status.add(
					createStatus(
						null,
						UpdateUIPlugin.getResourceString(KEY_PLATFORM)));
				return;
			}
		}
	}

	/*
	 * Verify we end up with a version of primary feature configured
	 */
	private static void checkPrimaryFeature(
		ArrayList features,
		ArrayList status) {

		String featureId =
			BootLoader
				.getCurrentPlatformConfiguration()
				.getPrimaryFeatureIdentifier();

		for (int i = 0; i < features.size(); i++) {
			IFeature feature = (IFeature) features.get(i);
			if (featureId
				.equals(feature.getVersionedIdentifier().getIdentifier()))
				return;
		}

		status.add(
			createStatus(null, UpdateUIPlugin.getResourceString(KEY_PRIMARY)));
	}

	/*
	 * Verify we do not break prereqs
	 */
	private static void checkPrereqs(
		ArrayList features,
		ArrayList plugins,
		ArrayList status) {

		for (int i = 0; i < features.size(); i++) {
			IFeature feature = (IFeature) features.get(i);
			IImport[] imports = feature.getImports();

			for (int j = 0; j < imports.length; j++) {
				IImport iimport = imports[j];
				// for each import determine plugin, version, match we need
				VersionedIdentifier iid = iimport.getVersionedIdentifier();
				String id = iid.getIdentifier();
				PluginVersionIdentifier version = iid.getVersion();
				boolean ignoreVersion =
					version.getMajorComponent() == 0
						&& version.getMinorComponent() == 0
						&& version.getServiceComponent() == 0;
				int rule = iimport.getRule();
				if (rule == IImport.RULE_NONE)
					rule = IImport.RULE_COMPATIBLE;

				boolean found = false;
				for (int k = 0; k < plugins.size(); k++) {
					// see if we have a plugin that matches
					IPluginEntry plugin = (IPluginEntry) plugins.get(k);
					VersionedIdentifier pid = plugin.getVersionedIdentifier();
					PluginVersionIdentifier pversion = pid.getVersion();
					if (id.equals(pid.getIdentifier())) {
						// have a candidate
						if (ignoreVersion)
							found = true;
						else if (
							rule == IImport.RULE_PERFECT
								&& pversion.isPerfect(version))
							found = true;
						else if (
							rule == IImport.RULE_EQUIVALENT
								&& pversion.isEquivalentTo(version))
							found = true;
						else if (
							rule == IImport.RULE_COMPATIBLE
								&& pversion.isCompatibleWith(version))
							found = true;
						else if (
							rule == IImport.RULE_GREATER_OR_EQUAL
								&& pversion.isGreaterOrEqualTo(version))
							found = true;
					}
					if (found)
						break;
				}
				if (!found) {
					// report status
					String msg =
						UpdateUIPlugin.getFormattedMessage(
							KEY_PREREQ,
							new String[] { id });

					if (!ignoreVersion) {
						if (rule == IImport.RULE_PERFECT)
							msg =
								UpdateUIPlugin.getFormattedMessage(
									KEY_PREREQ_PERFECT,
									new String[] { id, version.toString()});
						else if (rule == IImport.RULE_EQUIVALENT)
							msg =
								UpdateUIPlugin.getFormattedMessage(
									KEY_PREREQ_EQUIVALENT,
									new String[] { id, version.toString()});
						else if (rule == IImport.RULE_COMPATIBLE)
							msg =
								UpdateUIPlugin.getFormattedMessage(
									KEY_PREREQ_COMPATIBLE,
									new String[] { id, version.toString()});
						else if (rule == IImport.RULE_GREATER_OR_EQUAL)
							msg =
								UpdateUIPlugin.getFormattedMessage(
									KEY_PREREQ_GREATER,
									new String[] { id, version.toString()});
					}

					status.add(createStatus(feature, msg));
				}
			}
		}
	}

	/*
	 * Verify we end up with valid nested features after revert
	 */
	private static void checkRevertConstraints(
		ArrayList features,
		ArrayList status) {

		for (int i = 0; i < features.size(); i++) {
			IFeature feature = (IFeature) features.get(i);
			try {
				computeFeatureSubtree(
					feature,
					null,
					null,
					false /* do not tolerate missing children */
				);
			} catch (CoreException e) {
				status.add(e.getStatus());
			}
		}

	}

	private static IStatus createMultiStatus(
		String rootKey,
		ArrayList children) {
		IStatus[] carray =
			(IStatus[]) children.toArray(new IStatus[children.size()]);
		String message = UpdateUIPlugin.getResourceString(rootKey);
		return new MultiStatus(
			UpdateUIPlugin.getPluginId(),
			IStatus.ERROR,
			carray,
			message,
			null);
	}

	private static IStatus createStatus(IFeature feature, String message) {

		String fullMessage;
		if (feature == null)
			fullMessage = message;
		else {
			PluginVersionIdentifier version =
				feature.getVersionedIdentifier().getVersion();
			fullMessage =
				UpdateUIPlugin.getFormattedMessage(
					KEY_CHILD_MESSAGE,
					new String[] {
						feature.getLabel(),
						version.toString(),
						message });
		}

		return new Status(
			IStatus.ERROR,
			UpdateUIPlugin.getPluginId(),
			IStatus.OK,
			fullMessage,
			null);
	}
}