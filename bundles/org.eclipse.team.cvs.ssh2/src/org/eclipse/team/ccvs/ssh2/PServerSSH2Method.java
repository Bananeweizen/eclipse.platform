/* -*-mode:java; c-basic-offset:2; -*- */
/*******************************************************************************
 * Copyright (c) 2003, Atsuhiko Yamanaka, JCraft,Inc. and others. All rights
 * reserved. This program and the accompanying materials are made available
 * under the terms of the Common Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: Atsuhiko Yamanaka, JCraft,Inc. - initial API and
 * implementation.
 ******************************************************************************/
package org.eclipse.team.ccvs.ssh2;

import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.IConnectionMethod;
import org.eclipse.team.internal.ccvs.core.IServerConnection;

public class PServerSSH2Method implements IConnectionMethod {
	public String getName() {
		return "pserverssh2";
	}
	public IServerConnection createConnection(ICVSRepositoryLocation root, String password) {
		return new PServerSSH2ServerConnection(root, password);
	}
	public void disconnect(ICVSRepositoryLocation location) {
	}
}