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
package org.eclipse.debug.core;


import org.eclipse.debug.core.model.IStreamMonitor;

/**
 * A stream listener is notified of changes
 * to a stream monitor.
 * <p>
 * Clients may implement this interface.
 * </p>
 * @see IStreamMonitor
 */
public interface IStreamListener {
	/**
	 * Notifies this listener that text has been appended to
	 * the given stream monitor.
	 *
	 * @param text the appended text
	 * @param monitor the stream monitor to which text was appended
	 */
	public void streamAppended(String text, IStreamMonitor monitor);
	/**
	 * Notifies this listener that given monitor's stream has closed.
	 * 
	 * @param monitor the stream monitor whose stream closed
	 * @since 3.0
	 */
	public void streamClosed(IStreamMonitor monitor);
}
