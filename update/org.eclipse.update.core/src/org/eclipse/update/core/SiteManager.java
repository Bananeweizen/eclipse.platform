package org.eclipse.update.core;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import java.net.URL;

import org.eclipse.core.boot.BootLoader;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.update.configuration.ILocalSite;
import org.eclipse.update.internal.core.InternalSiteManager;

/**
 * Site Manager.
 * A helper class used for creating site instance. 
 * Site manager is a singleton class. It cannot be instantiated; 
 * all functionality is provided by static methods.
 * 
 * @see org.eclipse.update.core.ISite
 * @see org.eclipse.update.configuration.ILocalSite
 * @see org.eclipse.update.configuration.IConfiguredSite
 * @since 2.0
 */
public class SiteManager {
	
	private static String os;
	private static String ws;
	private static String arch;	

	private SiteManager() {
	}

	/** 
	 * Returns a site object for the site specified by the argument URL.
	 * Typically, the URL references a site manifest file on an update 
	 * site. An update site acts as a source of features for installation
	 * actions.
	 * 
	 * @param siteURL site URL
	 * @return site object for the url
	 * @exception CoreException
	 * @since 2.0 
	 */
	public static ISite getSite(URL siteURL) throws CoreException {
		return InternalSiteManager.getSite(siteURL,true);
	}

	/** 
	 * Returns a site object for the site specified by the argument URL.
	 * Typically, the URL references a site manifest file on an update 
	 * site. An update site acts as a source of features for installation
	 * actions.
	 * 
	 * @param siteURL site URL
	 * @param usesCache <code>false</code> if the cache should be refreshed, and the site entirely reparsed, <code>false</code> otherwise.
	 * @return site object for the url
	 * @exception CoreException
	 * @since 2.0 
	 */
	public static ISite getSite(URL siteURL, boolean usesCache) throws CoreException {
		return InternalSiteManager.getSite(siteURL, usesCache);
	}

	/**
	 * Returns the "local site". A local site is a logical collection
	 * of configuration information plus one or more file system 
	 * installation directories, represented as intividual sites. 
	 * These are potential targets for installation actions.
	 * 
	 * @return the local site
	 * @exception CoreException
	 * @since 2.0 
	 */
	public static ILocalSite getLocalSite() throws CoreException {
		return InternalSiteManager.getLocalSite();
	}
	
	/**
	 * Trigger handling of newly discovered features. This method
	 * can be called by the executing application whenever it
	 * is invoked with the -newUpdates command line argument.
	 * 
	 * @throws CoreException if an error occurs.
	 * @since 2.0
	 */
	public static void handleNewChanges() throws CoreException{
		InternalSiteManager.handleNewChanges();
	}
	/**
	 * Returns system architecture specification. A comma-separated list of arch
	 * designators defined by the platform. 
	 * 
	 * This information is used as a hint by the installation and update
	 * support.
	 * 
     * @see BootLoader#ARCH_LIST
	 * @return system architecture specification
	 * @since 2.1
	 */
	public static String getOSArch(){
		if (arch==null) 
			arch = BootLoader.getOSArch();
		return arch;
	}

    /**
	 * Returns operating system specification. A comma-separated list of os
	 * designators defined by the platform.
	 * 
	 * This information is used as a hint by the installation and update
	 * support.
	 *
	 * @see BootLoader#OS_LIST
	 * @return the operating system specification.
	 * @since 2.1
	 */
	public static String getOS(){
		if (os==null)
			os = BootLoader.getOS();
		return os;
	}

	/**
	 * Returns system architecture specification. A comma-separated list of arch
	 * designators defined by the platform. 
	 * 
	 * This information is used as a hint by the installation and update
	 * support.
	 * 
	 * @see BootLoader#WS_LIST
	 * @return system architecture specification.
	 * @since 2.1
	 */
	public static String getWS() {
		if (ws==null)
			ws = BootLoader.getWS();
		return ws;
	}
	
	/**
	 * Sets the arch.
	 * @param arch The arch to set
	 */
	public static void setOSArch(String arch) {
		SiteManager.arch = arch;
	}

	/**
	 * Sets the os.
	 * @param os The os to set
	 */
	public static void setOS(String os) {
		SiteManager.os = os;
	}

	/**
	 * Sets the ws.
	 * @param ws The ws to set
	 */
	public static void setWS(String ws) {
		SiteManager.ws = ws;
	}

}