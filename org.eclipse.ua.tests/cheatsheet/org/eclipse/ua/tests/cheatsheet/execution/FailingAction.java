/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ua.tests.cheatsheet.execution;

import org.eclipse.jface.action.Action;

/**
 * Class used to test calling an action which returns a failure status
 */

public class FailingAction extends Action {
	
	@Override
	public void run() {
		// Return a failure status
		notifyResult(false);
	}

}
