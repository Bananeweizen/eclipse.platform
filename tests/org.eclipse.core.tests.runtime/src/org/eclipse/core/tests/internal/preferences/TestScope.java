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
package org.eclipse.core.tests.internal.preferences;

import org.eclipse.core.internal.preferences.EclipsePreferences;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.preferences.*;
import org.osgi.service.prefs.BackingStoreException;

/**
 * @since 3.0
 */
public class TestScope extends EclipsePreferences implements IScopeContext {

	public static final String SCOPE = "test"; //$NON-NLS-1$
	private String qualifier;
	private int segmentCount;
	private EclipsePreferences loadLevel;

	public TestScope() {
		this(null, null);
	}

	private TestScope(IEclipsePreferences parent, String key) {
		super(parent, key);
		// cache the segment count
		IPath path = new Path(absolutePath());
		segmentCount = path.segmentCount();
		if (segmentCount < 2)
			return;

		// cache the qualifier
		String scope = path.segment(0);
		if (TestScope.SCOPE.equals(scope))
			qualifier = path.segment(1);

		// cache the location
		if (qualifier == null)
			return;
	}

	protected IEclipsePreferences getLoadLevel() {
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

	public IPath getLocation() {
		return null;
	}

	public String getName() {
		return SCOPE;
	}

	public IEclipsePreferences getNode(String qualifier) {
		return (IEclipsePreferences) Platform.getPreferencesService().getRootNode().node(SCOPE).node(qualifier);
	}

	protected EclipsePreferences internalCreate(IEclipsePreferences nodeParent, String nodeName, Plugin context) {
		return new TestScope(nodeParent, nodeName);
	}

	public boolean isDirty() {
		return dirty;
	}

	void setDirty(boolean value) {
		dirty = value;
	}
}