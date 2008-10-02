/*******************************************************************************
 * Copyright (c) 2008 Oakland Software Incorporated and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *		Oakland Software Incorporated - initial API and implementation
 *		IBM Corporation - implementation
 *******************************************************************************/
package org.eclipse.core.internal.net.proxy.unix;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.Properties;

import org.eclipse.core.internal.net.AbstractProxyProvider;
import org.eclipse.core.internal.net.Activator;
import org.eclipse.core.internal.net.Policy;
import org.eclipse.core.internal.net.ProxyData;
import org.eclipse.core.net.proxy.IProxyData;

public class UnixProxyProvider extends AbstractProxyProvider {

	private static final String LIBRARY_GCONF2 = "gconf-2"; //$NON-NLS-1$

	private static final String LIBRARY_NAME = "gnomeproxy-1.0.0"; //$NON-NLS-1$

	private static boolean isGnomeLibLoaded = false;

	static {
		// We have to load this here otherwise gconf seems to have problems
		// causing hangs and various other bad behavior,
		// please don't move this to be initialized on another thread.
		loadGnomeLib();
	}

	public UnixProxyProvider() {
		// Nothing to initialize
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.internal.net.AbstractProxyProvider#getProxyData(java.net.URI)
	 */
	public IProxyData[] getProxyData(URI uri) {
		ProxyData pd = getSystemProxyInfo(uri.getScheme());
		return pd != null ? new IProxyData[] { pd } : new IProxyData[0];
	}

	public String[] getNonProxiedHosts() {
		String[] npHosts;
		
		if (Policy.DEBUG_SYSTEM_PROVIDERS)
			Policy.debug("Getting no_proxy"); //$NON-NLS-1$

		// First try the environment variable which is a URL
		String npEnv = getEnv("no_proxy"); //$NON-NLS-1$
		if (npEnv != null) {
			npHosts = npEnv.split(","); //$NON-NLS-1$
			for (int i = 0; i < npHosts.length; i++)
				npHosts[i] = npHosts[i].trim();
			if (Policy.DEBUG_SYSTEM_PROVIDERS) {
				Policy.debug("Got Env no_proxy: " + npEnv); //$NON-NLS-1$
				debugPrint(npHosts);
			}
			return npHosts;
		}

		if (isGnomeLibLoaded) {
			try {
				npHosts = getGConfNonProxyHosts();
				if (npHosts != null && npHosts.length > 0) {
					if (Policy.DEBUG_SYSTEM_PROVIDERS) {
						Policy.debug("Got Gnome no_proxy"); //$NON-NLS-1$
						debugPrint(npHosts);
					}
					return npHosts;
				}
			} catch (UnsatisfiedLinkError e) {
				// The library should be loaded, so this is a real exception
				Activator.logError(
						"Problem during accessing Gnome library", e); //$NON-NLS-1$
			}
		}

		return new String[0];
	}

	// Returns null if something wrong or there is no proxy for the protocol
	protected ProxyData getSystemProxyInfo(String protocol) {
		ProxyData pd = null;
		String envName = null;
		
		if (Policy.DEBUG_SYSTEM_PROVIDERS)
			Policy.debug("Getting proxies for: " + protocol); //$NON-NLS-1$

		try {
			// protocol schemes are ISO 8859 (ASCII)
			protocol = protocol.toLowerCase(Locale.ENGLISH);

			// First try the environment variable which is a URL
			envName = protocol + "_proxy"; //$NON-NLS-1$
			String proxyEnv = getEnv(envName);
			if (Policy.DEBUG_SYSTEM_PROVIDERS)
				Policy.debug("Got proxyEnv: " + proxyEnv); //$NON-NLS-1$

			if (proxyEnv != null) {
				URI uri = new URI(proxyEnv);
				pd = new ProxyData(protocol);
				pd.setHost(uri.getHost());
				pd.setPort(uri.getPort());
				String userInfo = uri.getUserInfo();
				if (userInfo != null) {
					String user = null;
					String password = null;
					int pwInd = userInfo.indexOf(':');
					if (pwInd >= 0) {
						user = userInfo.substring(0, pwInd);
						password = userInfo.substring(pwInd + 1);
					} else {
						user = userInfo;
					}
					pd.setUserid(user);
					pd.setPassword(password);
				}
				pd.setSource("LINUX_ENV"); //$NON-NLS-1$
				if (Policy.DEBUG_SYSTEM_PROVIDERS)
					Policy.debug("Got Env proxy: " + pd); //$NON-NLS-1$
				return pd;
			}
		} catch (Exception e) {
			Activator.logError(
					"Problem during accessing system variable: " + envName, e); //$NON-NLS-1$
		}

		if (isGnomeLibLoaded) {
			try {
				// Then ask Gnome
				pd = getGConfProxyInfo(protocol);
				if (Policy.DEBUG_SYSTEM_PROVIDERS)
					Policy.debug("Got Gnome proxy: " + pd); //$NON-NLS-1$
				pd.setSource("LINUX_GNOME"); //$NON-NLS-1$
				return pd;
			} catch (UnsatisfiedLinkError e) {
				// The library should be loaded, so this is a real exception
				Activator.logError(
						"Problem during accessing Gnome library", e); //$NON-NLS-1$
			}
		}

		return null;
	}

	private String getEnv(String env) {
		Properties props = new Properties();
		try {
			props.load(Runtime.getRuntime().exec("env").getInputStream()); //$NON-NLS-1$
		} catch (IOException e) {
			Activator.logError(
					"Problem during accessing system variable: " + env, e); //$NON-NLS-1$
		}
		return props.getProperty(env);
	}

	private static void loadGnomeLib() {
		try {
			System.loadLibrary(LIBRARY_GCONF2);
		} catch (final UnsatisfiedLinkError e) {
			// Expected on systems that are missing Gnome
			if (Policy.DEBUG_SYSTEM_PROVIDERS)
				Policy.debug("Could not load library: " //$NON-NLS-1$
						+ System.mapLibraryName(LIBRARY_GCONF2));
			return;
		}

		try {
			System.loadLibrary(LIBRARY_NAME);
			isGnomeLibLoaded = true;
			if (Policy.DEBUG_SYSTEM_PROVIDERS)
				Policy.debug("Loaded " + //$NON-NLS-1$
						System.mapLibraryName(LIBRARY_NAME) + " library"); //$NON-NLS-1$
		} catch (final UnsatisfiedLinkError e) {
			// Expected on systems that are missing Gnome library
			if (Policy.DEBUG_SYSTEM_PROVIDERS)
				Policy.debug("Could not load library: " //$NON-NLS-1$
						+ System.mapLibraryName(LIBRARY_NAME));
		}
	}


	private void debugPrint(String[] strs) {
		for (int i = 0; i < strs.length; i++)
			System.out.println(i + ": " + strs[i]); //$NON-NLS-1$
	}

	protected static native void gconfInit();

	protected static native ProxyData getGConfProxyInfo(String protocol);

	protected static native String[] getGConfNonProxyHosts();
}
