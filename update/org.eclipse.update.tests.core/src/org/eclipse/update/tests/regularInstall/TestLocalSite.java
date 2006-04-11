/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.tests.regularInstall;
import java.io.*;
import java.net.*;
import org.eclipse.update.configuration.*;
import org.eclipse.update.configurator.*;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.core.*;
import org.eclipse.update.internal.model.*;
import org.eclipse.update.tests.*;

public class TestLocalSite extends UpdateManagerTestCase {
	
	/**
	 * Test the getFeatures()
	 */
	public TestLocalSite(String arg0) {
		super(arg0);
	}
	
	public void testCreationConfigFile() throws Exception {

		//clean up
		File localFile = new File(((LocalSite)SiteManager.getLocalSite()).getLocationURL().getFile());
		UpdateManagerUtils.removeFromFileSystem(localFile);	
		InternalSiteManager.localSite=null;	


		ILocalSite site = SiteManager.getLocalSite();
		site.save();
		assertTrue("config file hasn't been saved in :"+localFile.getAbsolutePath(), localFile.exists());
		
		// cleanup
		UpdateManagerUtils.removeFromFileSystem(localFile);		
		

	}
	
	public void testDefaultConfigFile() throws Exception {

		//clean up

		File localFile = new File(((LocalSite)SiteManager.getLocalSite()).getLocationURL().getFile());
		UpdateManagerUtils.removeFromFileSystem(localFile);	
		InternalSiteManager.localSite=null;

		ILocalSite site = SiteManager.getLocalSite();
		assertTrue("The local site does not contain an history of install configuration",site.getConfigurationHistory().length!=0);
		assertTrue("The local site does not contain an current install configuration",site.getCurrentConfiguration()!=null);
		assertTrue("The local site does not contain a default configuration site for the current install config",site.getCurrentConfiguration().getConfiguredSites().length!=0);
		
		System.out.println("Default Config Site is :"+site.getCurrentConfiguration().getConfiguredSites()[0].getSite().getURL().toExternalForm());
		
		// cleanup	
		UpdateManagerUtils.removeFromFileSystem(localFile);		
		

	}
	
/*	public void testInstallFeatureSaveConfig() throws Exception {

		//clean up
		LocalSite siteLocal = (LocalSite)SiteManager.getLocalSite();
		URL newURL = new URL(siteLocal.getLocationURL(),LocalSite.SITE_LOCAL_FILE);
		File localFile = new File(newURL.getFile());
		UpdateManagerUtils.removeFromFileSystem(localFile);
		UpdateManagerUtils.removeFromFileSystem(new File(((InstallConfiguration)siteLocal.getCurrentConfiguration()).getURL().getFile()));	
		InternalSiteManager.localSite=null;		

		ILocalSite site = SiteManager.getLocalSite();
		ISite remoteSite = SiteManager.getSite(SOURCE_FILE_SITE_INSTALLED);
		IFeature feature = remoteSite.getFeatureReferences()[0].getFeature();
		int oldNumber = site.getCurrentConfiguration().getConfiguredSites().length;
		
		// we are not checking if this is read only
		IInstallConfiguration newConfig = site.cloneCurrentConfiguration();
		newConfig.setLabel("new Label");		
		//IInstallConfiguration newConfig = site.getCurrentConfiguration();
		IConfiguredSite configSite = newConfig.getConfiguredSites()[0];
		ConfigurationPolicyModel configPolicy = new BaseSiteLocalFactory().createConfigurationPolicyModel();
		configPolicy.setPolicy(IPlatformConfiguration.ISitePolicy.USER_INCLUDE);
		((ConfiguredSite)configSite).setConfigurationPolicy((ConfigurationPolicy)configPolicy);
		int oldNumberOfhistory = site.getConfigurationHistory().length;		
		site.addConfiguration(newConfig);	
		assertNotNull(feature);	

		((ConfiguredSite)configSite).isUpdatable(true);
		remove(feature,configSite);				
		configSite.install(feature,null,null);
				
		// teh current one points to a real fature
		// does not throw error.
		IConfiguredSite configSite2 = site.getCurrentConfiguration().getConfiguredSites()[0];
		assertTrue("No Configured features found",configSite.getConfiguredFeatures().length>0);
		IFeatureReference ref = configSite2.getConfiguredFeatures()[0];
		IFeature feature2 = ref.getFeature();
		//String configuredFeature = feature2.getLabel();

		assertEquals(feature2.getVersionedIdentifier().toString(),"org.eclipse.update.core.tests.feature3_1.0.0");
		assertTrue("Wrong id  version of feature",feature2.getVersionedIdentifier().toString().equalsIgnoreCase("org.eclipse.update.core.tests.feature3_1.0.0"));
		
		// only one feature configured
		assertTrue("too many features configured",configSite2.getConfiguredFeatures().length==1);
		
		// no feature unconfigured
		assertTrue("too many unconfigured features",((ConfiguredSite)configSite2).getConfigurationPolicy().getUnconfiguredFeatures().length==0);
		
		// test only 2 install config in local site
		//assertEquals("wrong number of history in Local site:",oldNumberOfhistory+1,site.getConfigurationHistory().length);
		
		// test same # of sites in current config
		assertTrue("Wrong number of config sites in current config",site.getCurrentConfiguration().getConfiguredSites().length==oldNumber);
		
		//test only one feature for the site
		assertTrue("wrong number of configured features for config site",site.getCurrentConfiguration().getConfiguredSites()[0].getConfiguredFeatures().length==1);
		
		// test only 2 activities
		assertTrue("Wrong number of activities for install config",site.getCurrentConfiguration().getActivities().length==2);

		site.save();		

		// check
		// there are 2 configuration
		String time = ""+site.getCurrentConfiguration().getCreationDate().getTime();
		URL location = ((LocalSite)site).getLocationURL();
		File file = new File(new URL(location,"Config"+time+".xml").getFile());
		assertTrue("new configuration does not exist", file.exists());
		
		// cleanup
		localFile = new File(new URL(location,LocalSite.SITE_LOCAL_FILE).getFile());
		UpdateManagerUtils.removeFromFileSystem(localFile);		
		localFile = new File(new URL(location,LocalSite.DEFAULT_CONFIG_FILE).getFile());
		UpdateManagerUtils.removeFromFileSystem(localFile);	
		localFile = new File(new URL(location,Site.DEFAULT_FEATURE_PATH+File.separator+feature.getVersionedIdentifier().toString()).getFile());		
		UpdateManagerUtils.removeFromFileSystem(localFile);	
		localFile = new File(new URL(location,Site.DEFAULT_FEATURE_PATH+File.separator+feature2.getVersionedIdentifier().toString()).getFile());		
		UpdateManagerUtils.removeFromFileSystem(localFile);	
		UpdateManagerUtils.removeFromFileSystem(file);		
		localFile = new File(feature2.getURL().getFile());
		UpdateManagerUtils.removeFromFileSystem(localFile);
	}
*/
	
	public void testRetriveConfig() throws Exception {

		//clean up
		File localFile = new File(((LocalSite)SiteManager.getLocalSite()).getLocationURL().getFile());
		UpdateManagerUtils.removeFromFileSystem(localFile);	
		InternalSiteManager.localSite=null;		

		ILocalSite site = SiteManager.getLocalSite();
		ISite remoteSite = SiteManager.getSite(SOURCE_FILE_SITE_INSTALLED,null);
		IFeature feature = remoteSite.getFeatureReferences()[0].getFeature(null);
		int oldNumber = site.getCurrentConfiguration().getConfiguredSites().length;		
		
		// we are not checking if this is read only
		IInstallConfiguration newConfig = site.cloneCurrentConfiguration();
		newConfig.setLabel("new Label");		
		IConfiguredSite configSite = newConfig.getConfiguredSites()[0];
		ConfigurationPolicyModel configPolicy = new BaseSiteLocalFactory().createConfigurationPolicyModel();
		configPolicy.setPolicy(IPlatformConfiguration.ISitePolicy.USER_INCLUDE);
		((ConfiguredSite)configSite).setConfigurationPolicyModel(configPolicy);	
		int oldNumberOfhistory = site.getConfigurationHistory().length;			
		site.addConfiguration(newConfig);
		assertNotNull(feature);			
		
		((ConfiguredSite)configSite).setUpdatable(true);
		remove(feature,configSite);			
		configSite.install(feature,null,null);
		site.save();
		
		// we created the second xml file

		//do not cleanup, we want to reuse previously created local site
		// but force re-read of xml File
		InternalSiteManager.localSite=null;
		site = SiteManager.getLocalSite();
		
		// check
		// there are 2 configuration
		String time = ""+site.getCurrentConfiguration().getCreationDate().getTime();
		URL location = ((LocalSite)site).getLocationURL();		
		File file = new File(new URL(location,"platform.xml").getFile());
		assertTrue("new configuration does not exist", file.exists());
		
		// teh current one points to a real fature
		// does not throw error.
		IConfiguredSite configSite2 = site.getCurrentConfiguration().getConfiguredSites()[0];
		IFeatureReference[] refs = configSite2.getConfiguredFeatures();
		IFeature feature2 = null;
		for (int i = 0; i < refs.length; i++) {
			IFeature feature3 = refs[i].getFeature(null);			
			if ("org.eclipse.update.core.tests.feature3_1.0.0".equals(feature3.getVersionedIdentifier().toString())){
				feature2 = feature3;
				break;
			}		
		}
		assertNotNull("Feature 2 is Null",feature2);
		assertTrue("Wrong id  version of feature",feature2.getVersionedIdentifier().toString().equalsIgnoreCase("org.eclipse.update.core.tests.feature3_1.0.0"));
		
		// test only 2 install config in local site
//		assertEquals("wrong number of history in Local site:",oldNumberOfhistory+1,site.getConfigurationHistory().length);
		
		// test # of sites in current config
		assertTrue("Wrong number of config sites in current config",site.getCurrentConfiguration().getConfiguredSites().length==oldNumber);
		
		//test only one feature for the site
//		assertTrue("wrong number of configured features for config site",site.getCurrentConfiguration().getConfiguredSites()[0].getConfiguredFeatures().length==1);
		
		// test only 2 activities
//		assertTrue("Wrong number of activities for install config",site.getCurrentConfiguration().getActivities().length==2);
		
		
		// cleanup
		localFile = new File(((LocalSite)SiteManager.getLocalSite()).getLocationURL().getFile());
		UpdateManagerUtils.removeFromFileSystem(localFile);		
		localFile = new File(new URL(location,Site.DEFAULT_FEATURE_PATH+File.separator+feature.getVersionedIdentifier().toString()).getFile());		
		UpdateManagerUtils.removeFromFileSystem(localFile);					
		UpdateManagerUtils.removeFromFileSystem(file);		
	}

/*	public void testRetriveConfigHTTPInstall() throws Exception {

		//clean up
		File localFile = new File(((LocalSite)SiteManager.getLocalSite()).getLocationURL().getFile());
		UpdateManagerUtils.removeFromFileSystem(localFile);
		InternalSiteManager.localSite=null;		

		ILocalSite site = SiteManager.getLocalSite();
		ISite remoteSite = SiteManager.getSite(SOURCE_HTTP_SITE,null);
		IFeature feature = remoteSite.getFeatureReferences()[0].getFeature(null);
		
		// we are not checking if this is read only
		IInstallConfiguration newConfig = site.cloneCurrentConfiguration();
		newConfig.setLabel("new Label");		
		IConfiguredSite configSite = newConfig.getConfiguredSites()[0];
		ConfigurationPolicyModel configPolicy = new BaseSiteLocalFactory().createConfigurationPolicyModel();
		configPolicy.setPolicy(IPlatformConfiguration.ISitePolicy.USER_INCLUDE);
		((ConfiguredSite)configSite).setConfigurationPolicyModel((ConfigurationPolicyModel)configPolicy);	
		int oldNumberOfhistory = site.getConfigurationHistory().length;			
		site.addConfiguration(newConfig);
		
	
		((ConfiguredSite)configSite).setUpdatable(true);				
		configSite.install(feature,null,null);
		site.save();

		//do not cleanup, we want to reuse previously created local site
		// but force re-read of xml File
		InternalSiteManager.localSite=null;
		site = SiteManager.getLocalSite();
		feature = remoteSite.getFeatureReferences()[0].getFeature(null);
		int oldNumber = site.getCurrentConfiguration().getConfiguredSites().length;		
		
		// check
		// there are 2 configuration
		String time = ""+site.getCurrentConfiguration().getCreationDate().getTime();
		URL location = ((LocalSite)site).getLocationURL();		
		File file = new File(new URL(location,"platform.xml").getFile());
		assertTrue("new configuration does not exist", file.exists());
		
		// teh current one points to a real fature
		// does not throw error.
		IConfiguredSite configSite2 = site.getCurrentConfiguration().getConfiguredSites()[0];
		
		IFeatureReference[] refs = configSite2.getConfiguredFeatures();
		boolean found = false;
		IFeature feature2 = null;
		for (int i = 0; !found && i < refs.length; i++) {
			IFeature feature3 = refs[i].getFeature(null);			
			if ("org.test1.ident1_1.0.0".equals(feature3.getVersionedIdentifier().toString())){
				feature2 = feature3;
				found = true;
			}		
		}

		//String configuredFeature = feature2.getLabel();
		assertTrue("cannot find feature org.test1.ident1_1.0.0 in configured Site",found);
		assertTrue("Wrong id  version of feature",feature2.getVersionedIdentifier().toString().equalsIgnoreCase("org.test1.ident1_1.0.0"));
		
		// test only 2 install config in local site
//		assertEquals("wrong number of history in Local site:",oldNumberOfhistory+1,site.getConfigurationHistory().length);
		
		// test same number of sites in current config
		assertTrue("Wrong number of config sites in current config",site.getCurrentConfiguration().getConfiguredSites().length==oldNumber);
		
		//test only one feature for the site
//		assertTrue("wrong number of configured features for config site",site.getCurrentConfiguration().getConfiguredSites()[0].getConfiguredFeatures().length==1);
		
		// test only 2 activities
//		assertTrue("Wrong number of activities for install config",site.getCurrentConfiguration().getActivities().length==2);
		
		
		// cleanup
		localFile = new File(location.getFile());
		UpdateManagerUtils.removeFromFileSystem(localFile);		
		localFile = new File(new URL(location,Site.DEFAULT_FEATURE_PATH+File.separator+feature.getVersionedIdentifier().toString()).getFile());		
		UpdateManagerUtils.removeFromFileSystem(localFile);							
		UpdateManagerUtils.removeFromFileSystem(file);		
		localFile = new File(feature2.getURL().getFile());
		UpdateManagerUtils.removeFromFileSystem(localFile);
	}*/
	
//TODO uncomment this once site disabling is supported
public void testRetriveConfigHTTPInstallNotEnable() throws Exception {
/*
	//clean up
	LocalSite siteLocal = (LocalSite)SiteManager.getLocalSite();
	URL newURL = new URL(siteLocal.getLocationURL(),LocalSite.SITE_LOCAL_FILE);
	File localFile = new File(newURL.getFile());
	UpdateManagerUtils.removeFromFileSystem(localFile);
	UpdateManagerUtils.removeFromFileSystem(new File(((InstallConfiguration)siteLocal.getCurrentConfiguration()).getURL().getFile()));	
	InternalSiteManager.localSite=null;		

	ILocalSite site = SiteManager.getLocalSite();
	ISite remoteSite = SiteManager.getSite(SOURCE_HTTP_SITE);
	IFeature feature = remoteSite.getFeatureReferences()[0].getFeature(null);
		
	// we are not checking if this is read only
	IInstallConfiguration newConfig = site.cloneCurrentConfiguration();
	newConfig.setLabel("new Label");		
	IConfiguredSite configSite = newConfig.getConfiguredSites()[0];
	ConfigurationPolicyModel configPolicy = new BaseSiteLocalFactory().createConfigurationPolicyModel();
	configPolicy.setPolicy(IPlatformConfiguration.ISitePolicy.USER_INCLUDE);
	((ConfiguredSite)configSite).setConfigurationPolicyModel((ConfigurationPolicyModel)configPolicy);	
	int oldNumberOfhistory = site.getConfigurationHistory().length;			
	site.addConfiguration(newConfig);
		
	
	((ConfiguredSite)configSite).setUpdatable(true);
	configSite.install(feature,null,null);
	((ConfiguredSite)configSite).setEnabled(false);	
	site.save();

	//do not cleanup, we want to reuse previously created local site
	// but force re-read of xml File
	InternalSiteManager.localSite=null;
	site = SiteManager.getLocalSite();
	feature = remoteSite.getFeatureReferences()[0].getFeature(null);
	int oldNumber = site.getCurrentConfiguration().getConfiguredSites().length;		
		
	// check
	// there are 2 configuration
	String time = ""+site.getCurrentConfiguration().getCreationDate().getTime();
	URL location = ((LocalSite)site).getLocationURL();		
	File file = new File(location.getFile());
	assertTrue("new configuration does not exist", file.exists());
		
	// teh current one points to a real fature
	// does not throw error.
	IConfiguredSite configSite2 = site.getCurrentConfiguration().getConfiguredSites()[0];
		
	IFeatureReference[] refs = configSite2.getConfiguredFeatures();
	boolean found = false;
	IFeature feature2 = null;
	for (int i = 0; !found && i < refs.length; i++) {
		IFeature feature3 = refs[i].getFeature(null);			
		if ("org.test1.ident1_1.0.0".equals(feature3.getVersionedIdentifier().toString())){
			feature2 = feature3;
			found = true;
		}		
	}

	//String configuredFeature = feature2.getLabel();
	assertTrue("found feature org.test1.ident1_1.0.0 in disabled configured Site",!found);
		
	//test no configured features
//	assertTrue("wrong number of configured features for config site",site.getCurrentConfiguration().getConfiguredSites()[0].getConfiguredFeatures().length==0);
	
	
	configSite2.setEnabled(true);
	refs = configSite2.getConfiguredFeatures();	
	for (int i = 0; i < refs.length; i++) {
		IFeature feature3 = refs[i].getFeature(null);			
		if ("org.test1.ident1_1.0.0".equals(feature3.getVersionedIdentifier().toString())){
			feature2 = feature3;
			found = true;
		}		
	}

	//String configuredFeature = feature2.getLabel();
	assertTrue("cannot find feature org.test1.ident1_1.0.0 in configured Site",found);
	assertTrue("Wrong id  version of feature",feature2.getVersionedIdentifier().toString().equalsIgnoreCase("org.test1.ident1_1.0.0"));
		
	// test only 2 install config in local site
//	assertEquals("wrong number of history in Local site:",oldNumberOfhistory+1,site.getConfigurationHistory().length);
		
	// test same number of sites in current config
//	assertTrue("Wrong number of config sites in current config",site.getCurrentConfiguration().getConfiguredSites().length==oldNumber);
		
	//test only one feature for the site
//	assertTrue("wrong number of configured features for config site",site.getCurrentConfiguration().getConfiguredSites()[0].getConfiguredFeatures().length==1);
		
	// test only 2 activities
//	assertTrue("Wrong number of activities for install config",site.getCurrentConfiguration().getActivities().length==2);
		
		
	// cleanup
	localFile = new File(new URL(location,LocalSite.SITE_LOCAL_FILE).getFile());
	UpdateManagerUtils.removeFromFileSystem(localFile);		
	localFile = new File(new URL(location,LocalSite.DEFAULT_CONFIG_FILE).getFile());
	UpdateManagerUtils.removeFromFileSystem(localFile);	
	localFile = new File(new URL(location,Site.DEFAULT_FEATURE_PATH+File.separator+feature.getVersionedIdentifier().toString()).getFile());		
	UpdateManagerUtils.removeFromFileSystem(localFile);	
	UpdateManagerUtils.removeFromFileSystem(new File(((InstallConfiguration)site.getCurrentConfiguration()).getURL().getFile()));						
	UpdateManagerUtils.removeFromFileSystem(file);		
	localFile = new File(feature2.getURL().getFile());
	UpdateManagerUtils.removeFromFileSystem(localFile);*/
}


}

