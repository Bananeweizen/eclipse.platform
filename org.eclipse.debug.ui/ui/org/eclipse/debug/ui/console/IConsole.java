package org.eclipse.debug.ui.console;

/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp.  All rights reserved.
This file is made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html
**********************************************************************/

import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

/**
 * A console that displays output and writes input to a process. Implementors of
 * <code>IConsoleColorProvider</code> should connect streams to a console
 * document when connected to.
 * <p>
 * Clients are <b>not</b> intended to implement this interface.
 * </p>
 * <p>
 * <b>This interface is still evolving</b>
 * </p>
 * @see org.eclipse.debug.internal.ui.views.console.IConsoleColorProvider
 * @since 2.1
 */
public interface IConsole {

	/**
	 * Connects this console to the given streams proxy. This associates the
	 * standard in, out, and error streams with the console. Keyboard input will
	 * be written to the given proxy.
	 * 
	 * @param streamsProxy	 */
	public void connect(IStreamsProxy streamsProxy);
	
	/**
	 * Connects this console to the given stream monitor, uniquely identified by
	 * the given identifier. This allows for more than the stanard (in, out,
	 * error) streams to be connected to the console.
	 * 
	 * @param streamMonitor	 * @param streamIdentifer	 */
	public void connect(IStreamMonitor streamMonitor, String streamIdentifer);
	
	/**
	 * Adds the given hyperlink to this console. The link will be notified when
	 * entered, exited, and activated.
	 * <p>
	 * If the link's region (offset/length) is within the console's document
	 * current bounds, it is added immediately. Otherwise, the link is added
	 * when the console's document grows to contain the link's region.
	 * </p>
	 * @param link the hyperlink to add 
	 * @param offset the character offset within the console document where the
	 * text assoicated with the hyperlink begins
	 * @param length the length of the associated hyperlink text
	 */
	public void addLink(IConsoleHyperlink link, int offset, int length);
	
	/**
	 * Returns the region of text associated with the given hyperlink, or
	 * <code>null</code> if the given hyperlink is not contained in this
	 * console.
	 * 
	 * @param link a console hyperlink
	 * @return region of text associated with the hyperlink, or <code>null</code>
	 */
	public IRegion getRegion(IConsoleHyperlink link);

	/**
	 * Returns the document associated with this console.
	 * 
	 * @return document	 */
	public IDocument getDocument(); 
	
	/**
	 * Returns the process associted with this console.
	 * 
	 * @return the process associated with this console
	 */
	public IProcess getProcess();
}
