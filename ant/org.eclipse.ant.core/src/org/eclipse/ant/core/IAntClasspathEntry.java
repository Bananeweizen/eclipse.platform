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
package org.eclipse.ant.core;

import java.net.URL;

/**
 * Represents an Ant classpath entry.
 *
 * @since 3.0
 */
public interface IAntClasspathEntry {

	/**
	 * Returns the label for this classpath entry.
	 * @return the label for this entry.
	 */
	public String getLabel();
	
	/**
	 * Returns the URL for this classpath entry or <code>null</code>
	 * if it cannot be resolved.
	 * 
	 * @return the url for this classpath entry.
	 */
	public URL getEntryURL();
}