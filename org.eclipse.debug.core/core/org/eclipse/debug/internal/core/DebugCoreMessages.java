/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.debug.internal.core;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class DebugCoreMessages {

	private static final String RESOURCE_BUNDLE= "org.eclipse.debug.internal.core.DebugCoreMessages";//$NON-NLS-1$

	private static ResourceBundle fgResourceBundle= ResourceBundle.getBundle(RESOURCE_BUNDLE);

	private DebugCoreMessages() {
	}

	public static String getString(String key) {
		try {
			return fgResourceBundle.getString(key);
		} catch (MissingResourceException e) {
			return "!" + key + "!";//$NON-NLS-2$ //$NON-NLS-1$
		}
	}
}
