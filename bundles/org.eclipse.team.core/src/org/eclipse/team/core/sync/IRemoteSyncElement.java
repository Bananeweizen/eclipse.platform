/*******************************************************************************
 * Copyright (c) 2000, 2002 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 * IBM - Initial API and implementation
 ******************************************************************************/
package org.eclipse.team.core.sync;


/**
 * <b>Note:</b> This class/interface is part of an interim API that is still under 
 * development and expected to change significantly before reaching stability. 
 * It is being made available at this early stage to solicit feedback from pioneering 
 * adopters on the understanding that any code that uses this API will almost 
 * certainly be broken (repeatedly) as the API evolves.
 * 
 * A <code>IRemoteSyncElement</code> describes the relative synchronization of a <b>local</b> 
 * and <b>remote</b> resource using a <b>base</b> resource for comparison.
 * <p>
 * Differences between the base and remote resources are classified as <b>incoming changes</b>; 
 * if there is a difference, the remote resource is considered the <b>incoming resource</b>. </p>
 * 
 * @see ILocalSyncElement
 * 
 * @since 2.0
 */
public interface IRemoteSyncElement extends ILocalSyncElement {
	
	/**
	 * Answer the remote sync element of this node. Returns <code>null</code> 
	 * if there is no remote.
	 * 
	 * @return the remote resource in this sync element, or <code>null</code> is there
	 * is none.
	 */
	public IRemoteResource getRemote();
		
	/**
	 * Answers <code>true</code> if the base tree is not to be considered during sync
	 * comparisons and <code>false</code> if it should. If the base tree is ignored the
	 * sync comparison can be based on isOutOfDate and isDirty methods only.
	 */
	public boolean isThreeWay();
}