/*******************************************************************************
 * Copyright (c) 2002 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 * IBM - Initial implementation
 ******************************************************************************/
package org.eclipse.team.internal.core.target;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.team.core.Team;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.core.Policy;
import org.eclipse.team.internal.core.TeamPlugin;

public interface ITeamStatusConstants {

	public static final IStatus OK_STATUS = Team.OK_STATUS;

	public static final IStatus NOT_CHECKED_OUT_STATUS =
		new Status(
			IStatus.ERROR,
			TeamPlugin.ID,
			TeamException.NOT_CHECKED_OUT,
			Policy.bind("teamStatus.notCheckedOut"), //$NON-NLS-1$
			null);

	public static final IStatus NOT_CHECKED_IN_STATUS =
		new Status(
			IStatus.ERROR,
			TeamPlugin.ID,
			TeamException.NOT_CHECKED_IN,
			Policy.bind("teamStatus.notCheckedIn"), //$NON-NLS-1$
			null);

	public static final IStatus NO_REMOTE_RESOURCE_STATUS =
		new Status(
			IStatus.ERROR,
			TeamPlugin.ID,
			TeamException.NO_REMOTE_RESOURCE,
			Policy.bind("teamStatus.noRemoteResource"), //$NON-NLS-1$
			null);

	public static final IStatus IO_FAILED_STATUS =
		new Status(
			IStatus.ERROR,
			TeamPlugin.ID,
			TeamException.IO_FAILED,
			Policy.bind("teamStatus.ioFailed"), //$NON-NLS-1$
			null);

	public static final IStatus CONFLICT_STATUS =
		new Status(
			IStatus.ERROR,
			TeamPlugin.ID,
			TeamException.CONFLICT,
			Policy.bind("teamStatus.conflict"), //$NON-NLS-1$
			null);

	public static final IStatus REQUIRED_CONFIGURATION_MISSING =
		new Status(
			IStatus.ERROR,
			TeamPlugin.ID,
			-100,
			Policy.bind("provider.configuration.missing"), //$NON-NLS-1$
			null);
			
	public static final IStatus INVALID_CONFIGURATION =
		new Status(
			IStatus.ERROR,
			TeamPlugin.ID,
			-101,
			Policy.bind("provider.configuration.invalid"), //$NON-NLS-1$
			null);
}