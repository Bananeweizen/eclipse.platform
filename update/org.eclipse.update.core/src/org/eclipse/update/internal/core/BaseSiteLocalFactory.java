package org.eclipse.update.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
import org.eclipse.update.configuration.IConfiguredSite;
import org.eclipse.update.core.model.*;
import org.eclipse.update.internal.model.*;
import org.eclipse.update.internal.model.ConfigurationActivityModel;
import org.eclipse.update.internal.model.InstallConfigurationModel;
/**
 * 
 */
public class BaseSiteLocalFactory {
	/*
	 * 
	 */
	public InstallConfigurationModel createInstallConfigurationModel() {
		return new InstallConfiguration();
	}
	/*
	 * 
	 */
	public ConfigurationActivityModel createConfigurationAcivityModel() {
		return new ConfigurationActivity();
	}
	/*
	 * 
	 */
	public ConfiguredSiteModel createConfigurationSiteModel() {
		return new ConfiguredSite();
	}
	/*
	 * 
	 */
	public ConfigurationPolicyModel createConfigurationPolicyModel() {
		return new ConfigurationPolicy();
	}
	/**
	 * 
	 */
	public ConfiguredSiteModel createConfigurationSiteModel(
		SiteModel site,
		int policy) {
		//create config site
		ConfiguredSiteModel configSite= this.createConfigurationSiteModel();
		configSite.setSiteModel(site);
		ConfigurationPolicyModel policyModel= this.createConfigurationPolicyModel();
		policyModel.setPolicy(policy);
		configSite.setConfigurationPolicyModel(policyModel);
		((ConfigurationPolicy) policyModel).setConfiguredSite((IConfiguredSite)configSite);
		return configSite;
	}
}