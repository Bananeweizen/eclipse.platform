package org.eclipse.update.tests.core.boot;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.IOException;

import org.eclipse.core.boot.BootLoader;
import org.eclipse.core.boot.IPlatformConfiguration;
import org.eclipse.core.boot.IPlatformConfiguration.ISiteEntry;
import org.eclipse.core.boot.IPlatformConfiguration.ISitePolicy;

public class TestPlatCfgDefault
	extends PlatformConfigurationTestCase {
		
	public TestPlatCfgDefault(String arg0) {
		super(arg0);
	}
	
	public void testInitial() throws Exception {
		IPlatformConfiguration cfig = null;
		cfig = BootLoader.getCurrentPlatformConfiguration();
		ISiteEntry se = cfig.getConfiguredSites()[0];
		ISitePolicy sp = cfig.createSitePolicy(ISitePolicy.USER_EXCLUDE, new String[] {"1", "2","3","4","5","6","7","8","9","10","11","12"});
		se.setSitePolicy(sp);
		cfig.save();
		System.out.println("done ...");
	}
}

