package org.eclipse.update.internal.ui;

import java.net.URLEncoder;

import org.eclipse.update.internal.ui.preferences.AppServerPreferencePage;

/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

/**
 * This is a utility class used find information about the update
 * web application (handling web-triggered requests)
 * 
 * @since 2.0
 */
public abstract class WebInstallHandler {

	/**
	 * Returns the host identifier for the web application server
	 * listener for install requests
	 * 
	 * @return host identifier, or <code>null</code> if web application
	 * server is not available
	 * @since 2.0
	 */
	public static String getWebAppServerHost() {
		return UpdateUIPlugin.getDefault().getAppServerHost();
	}

	/**
	 * Returns the port identifier for the web application server
	 * listener for install requests
	 * 
	 * @return port identifier, or <code>0</code> if web application
	 * server is not available
	 * @since 2.0
	 */
	public static int getWebAppServerPort() {
		return UpdateUIPlugin.getDefault().getAppServerPort();
	}

	public static String getEncodedURLName(String urlName) {
		if (AppServerPreferencePage.getUseApplicationServer() == false
			|| AppServerPreferencePage.getEncodeURLs() == false)
			return urlName;
		return encode(urlName);
	}

	private static String encode(String urlName) {
		String callbackURL = getCallbackURLAsString();
		if (callbackURL == null)
			return urlName;
		String callbackParameter = "updateURL=" + callbackURL;
		if (urlName.indexOf('?') != -1)
			return urlName + "&" + callbackParameter;
		else
			return urlName + "?" + callbackParameter;
	}

	/**
	 * Returns the callback URL (as string) to be passed to the web page
	 * containing install triggers.
	 * 
	 * @return callback URL as string
	 * @since 2.0
	 */
	public static String getCallbackURLAsString() {
		String host = getWebAppServerHost();
		int port = getWebAppServerPort();
		if (host == null || port == 0)
			return null;
		else {
			String value =
				"http://"
					+ host
					+ ":"
					+ port
					+ "/"
					+ UpdateUIPlugin.WEB_APP_ID
					+ "/install";
			return URLEncoder.encode(value);
		}
	}
}