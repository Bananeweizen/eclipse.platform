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
package org.eclipse.team.internal.ui.sync;

 
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;

/**
 * <b>Note:</b> This class/interface is part of an interim API that is still under 
 * development and expected to change significantly before reaching stability. 
 * It is being made available at this early stage to solicit feedback from pioneering 
 * adopters on the understanding that any code that uses this API will almost 
 * certainly be broken (repeatedly) as the API evolves.
 * 
 * Interface for a sync node that responds to team commands.
 */
public interface ITeamNode extends IDiffElement {
	// Possible values for the change direction
	static final int INCOMING = Differencer.RIGHT;
	static final int OUTGOING = Differencer.LEFT;
	static final int CONFLICTING = Differencer.CONFLICTING;
	static final int NO_CHANGE = Differencer.NO_CHANGE;
	
	/**
	 * Returns the change direction for this resource.  One of:
	 * INCOMING, OUTGOING, CONFLICTING, NO_CHANGE.
	 */
	public int getChangeDirection();
	
	/**
	 * Returns the type of change for this resource.  One of:
	 * CHANGE, DELETION, ADDITION
	 */
	public int getChangeType();
	
	/**
	 * Returns the core resource represented by this node.
	 */
	public IResource getResource();
}
