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
package org.eclipse.update.internal.standalone;
import java.io.*;
import java.net.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.core.*;
import org.eclipse.update.internal.operations.*;
import org.eclipse.update.internal.search.*;
import org.eclipse.update.operations.*;
import org.eclipse.update.search.*;

public class InstallCommand extends ScriptedCommand {

	private IConfiguredSite targetSite;
	private UpdateSearchRequest searchRequest;
	private UpdateSearchResultCollector collector;
	private URL remoteSiteURL;
	private String featureId;
	private String version;

	public InstallCommand(
		String featureId,
		String version,
		String fromSite,
		String toSite,
		String verifyOnly)
		throws Exception {

		super(verifyOnly);

		try {
			this.featureId = featureId;
			this.version = version;

			this.remoteSiteURL = new URL(URLDecoder.decode(fromSite, "UTF-8"));

			// Get site to install to
			IConfiguredSite[] sites = getConfiguration().getConfiguredSites();
			if (toSite != null) {
				File sitePath = new File(toSite);
				if (!sitePath.exists())
					sitePath.mkdirs();
				URL toSiteURL = sitePath.toURL();
				ISite site = SiteManager.getSite(toSiteURL, null);
				if (site == null) {
					throw new Exception(
						"Cannot find site to install to: " + toSite);
				}
				targetSite = site.getCurrentConfiguredSite();
				if (targetSite == null) {
					targetSite = getConfiguration().createConfiguredSite(sitePath);
					// update the sites array to pick up new site
					sites = getConfiguration().getConfiguredSites();
				}
			}
			if (targetSite == null) {
				for (int i = 0; i < sites.length; i++) {
					if (sites[i].isProductSite()) {
						targetSite = sites[i];
						break;
					}
				}
			}
			UpdateSearchScope searchScope = new UpdateSearchScope();
			searchScope.addSearchSite(
				"remoteSite",
				remoteSiteURL,
				new String[0]);

			searchRequest =
				new UpdateSearchRequest(new SiteSearchCategory(), searchScope);
			VersionedIdentifier vid =
				new VersionedIdentifier(featureId, version);
			searchRequest.addFilter(
				new VersionedIdentifiersFilter(
					new VersionedIdentifier[] { vid }));
			searchRequest.addFilter(new EnvironmentFilter());
			searchRequest.addFilter(new BackLevelFilter());

			collector = new UpdateSearchResultCollector();

		} catch (MalformedURLException e) {
			throw e;
		} catch (CoreException e) {
			throw e;
		}
	}

	/**
	 */
	public boolean run() {
		try {
			searchRequest.performSearch(collector, new NullProgressMonitor());
			IInstallFeatureOperation[] operations = collector.getOperations();
			if (operations == null || operations.length == 0) {
				throw Utilities.newCoreException(
					"Feature "
						+ featureId
						+ " "
						+ version
						+ " cannot be found on "
						+ remoteSiteURL
						+ "\nor a newer version is already installed.",
					null);
			}
			JobTargetSite[] jobTargetSites =
				new JobTargetSite[operations.length];
			for (int i = 0; i < operations.length; i++) {
				jobTargetSites[i] = new JobTargetSite();
				jobTargetSites[i].job = operations[i];
				jobTargetSites[i].targetSite = operations[i].getTargetSite();
			}

			// Check for duplication conflicts
			ArrayList conflicts =
				DuplicateConflictsValidator.computeDuplicateConflicts(
					jobTargetSites,
					getConfiguration());
			if (conflicts != null) {
				throw Utilities.newCoreException("Duplicate conflicts", null);
			}

			if (isVerifyOnly()) {
				return (operations != null && operations.length > 1);
			}

			IBatchOperation installOperation =
				OperationsManager
					.getOperationFactory()
					.createBatchInstallOperation(
					operations);
			try {
				installOperation.execute(new NullProgressMonitor(), this);
				System.out.println(
					"Feature "
						+ featureId
						+ " "
						+ version
						+ " has successfully been installed");
				return true;
			} catch (Exception e) {
				throw Utilities.newCoreException(
					"Cannot install feature " + featureId + " " + version,
					e);
			}
		} catch (CoreException ce) {
			StandaloneUpdateApplication.exceptionLogged();
			UpdateCore.log(ce);
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.update.operations.IOperationListener#afterExecute(org.eclipse.update.operations.IOperation)
	 */
	public boolean afterExecute(IOperation operation, Object data) {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.update.operations.IOperationListener#beforeExecute(org.eclipse.update.operations.IOperation)
	 */
	public boolean beforeExecute(IOperation operation, Object data) {
		return true;
	}

	class UpdateSearchResultCollector implements IUpdateSearchResultCollector {
		private ArrayList operations = new ArrayList();

		public void accept(IFeature feature) {
			if (feature
				.getVersionedIdentifier()
				.getIdentifier()
				.equals(featureId)
				&& feature
					.getVersionedIdentifier()
					.getVersion()
					.toString()
					.equals(
					version)) {
				operations.add(
					OperationsManager
						.getOperationFactory()
						.createInstallOperation(
						getConfiguration(),
						targetSite,
						feature,
						null,
						null,
						null));
			}
		}
		public IInstallFeatureOperation[] getOperations() {
			IInstallFeatureOperation[] opsArray =
				new IInstallFeatureOperation[operations.size()];
			operations.toArray(opsArray);
			return opsArray;
		}
	}
}
