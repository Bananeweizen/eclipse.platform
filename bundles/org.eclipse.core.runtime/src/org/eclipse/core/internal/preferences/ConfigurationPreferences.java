/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.preferences;

import java.net.URL;
import org.eclipse.core.internal.runtime.InternalPlatform;
import org.eclipse.core.internal.runtime.Policy;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

/**
 * @since 3.0
 */
public class ConfigurationPreferences extends EclipsePreferences {

	// cached values
	private int segmentCount;
	private String qualifier;
	private IPath location;
	private EclipsePreferences loadLevel;

	/**
	 * Default constructor. Should only be called by #createExecutableExtension.
	 */
	public ConfigurationPreferences() {
		this(null, null);
	}

	private ConfigurationPreferences(IEclipsePreferences parent, String name) {
		super(parent, name);
		initialize();
	}

	/*
	 * Parse this node's absolute path and initialize some cached values for
	 * later use.
	 */
	private void initialize() {
		// cache the segment count
		IPath path = new Path(absolutePath());
		segmentCount = path.segmentCount();
		if (segmentCount < 2)
			return;

		// cache the qualifier
		String scope = path.segment(0);
		if (ConfigurationScope.SCOPE.equals(scope))
			qualifier = path.segment(1);

		// cache the location
		if (qualifier == null)
			return;
		URL url = InternalPlatform.getDefault().getConfigurationLocation().getURL();
		if (url != null)
			location = new Path(url.getFile()).append(DEFAULT_PREFERENCES_DIRNAME).append(qualifier).append(DEFAULT_PREFERENCES_FILENAME);
	}

	/*
	 * @see org.osgi.service.prefs.Preferences#flush()
	 */
	public void flush() throws BackingStoreException {
		if (location == null) {
			if (InternalPlatform.DEBUG_PREFERENCES)
				System.out.println("Unable to determine location of preference file for node: " + absolutePath()); //$NON-NLS-1$
			return;
		}
		EclipsePreferences node = getLoadLevel();
		if (node == null) {
			if (InternalPlatform.DEBUG_PREFERENCES)
				System.out.println("Preference node is not a load root: " + absolutePath()); //$NON-NLS-1$
			return;
		}
		node.save(location);
	}

	/*
	 * @see org.osgi.service.prefs.Preferences#sync()
	 */
	public void sync() throws BackingStoreException {
		if (location == null) {
			if (InternalPlatform.DEBUG_PREFERENCES)
				System.out.println("Unable to determine location of preference file for node: " + absolutePath()); //$NON-NLS-1$
			return;
		}
		EclipsePreferences node = getLoadLevel();
		if (node == null) {
			if (InternalPlatform.DEBUG_PREFERENCES)
				System.out.println("Preference node is not a load root: " + absolutePath()); //$NON-NLS-1$
			return;
		}
		node.load(location);
		node.flush();
	}

	/*
	 * Return the node at which these preferences are loaded/saved.
	 */
	private EclipsePreferences getLoadLevel() {
		if (loadLevel == null) {
			if (qualifier == null)
				return null;
			// Make it relative to this node rather than navigating to it from the root.
			// Walk backwards up the tree starting at this node.
			// This is important to avoid a chicken/egg thing on startup.
			EclipsePreferences node = this;
			for (int i = 2; i < segmentCount; i++)
				node = (EclipsePreferences) node.parent();
			loadLevel = node;
		}
		return loadLevel;
	}

	private boolean isLoadLevel() {
		return getLoadLevel() == this;
	}

	/*
	 * @see org.eclipse.core.runtime.preferences.IScope#create(org.eclipse.core.runtime.preferences.IEclipsePreferences, java.lang.String)
	 */
	public IEclipsePreferences create(IEclipsePreferences nodeParent, String nodeName) {
		ConfigurationPreferences result = new ConfigurationPreferences(nodeParent, nodeName);
		if (result.isLoadLevel() && !isLoading())
			try {
				isLoading = true;
				result.sync();
			} catch (BackingStoreException e) {
				String message = Policy.bind("preferences.syncException", result.absolutePath()); //$NON-NLS-1$
				IStatus status = new Status(IStatus.ERROR, Platform.PI_RUNTIME, IStatus.ERROR, message, e);
				InternalPlatform.getDefault().log(status);
			} finally {
				isLoading = false;
			}
		return result;
	}
}
