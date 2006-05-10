/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.tests.ccvs.core.subscriber;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.internal.ccvs.ui.subscriber.OverrideAndUpdateSubscriberOperation;

public class TestOverrideAndUpdateOperation extends OverrideAndUpdateSubscriberOperation {	

	private boolean prompted = false; 
	
	protected TestOverrideAndUpdateOperation(IDiffElement[] elements) {
		super(null, elements);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.actions.TeamOperation#canRunAsJob()
	 */
	protected boolean canRunAsJob() {
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.subscriber.CVSSubscriberOperation#promptForOverwrite(org.eclipse.team.core.synchronize.SyncInfoSet)
	 */
	protected boolean promptForOverwrite(SyncInfoSet syncSet) {
		TestOverrideAndUpdateOperation.this.prompted = true;
		return true;
	}
	
	public boolean isPrompted() {
		return this.prompted;
	}
}
