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
package org.eclipse.update.internal.ui.search;

import java.io.File;
import java.util.*;
/**
 * @version 	1.0
 * @author
 */
public class MyComputerSearchSettings {
	private static final String SECTION = "myComputerSearchSettings";
	SearchObject search;
	private Vector drives = new Vector();
	private boolean masterSettings=false;
	
	public boolean isMyComputerSearched() {
		return false;
	}

	public MyComputerSearchSettings(SearchObject search) {
		this.search = search;
		load();
	}

	public DriveSearchSettings[] getDriveSettings() {
		return (DriveSearchSettings[]) drives.toArray(
			new DriveSearchSettings[drives.size()]);
	}

	public DriveSearchSettings getDriveSettings(String driveName) {
		for (int i = 0; i < drives.size(); i++) {
			DriveSearchSettings drive = (DriveSearchSettings) drives.get(i);
			if (drive.getName().equalsIgnoreCase(driveName)) {
				return drive;
			}
		}
		DriveSearchSettings drive = new DriveSearchSettings(driveName);
		drives.add(drive);
		return drive;
	}

	private void load() {
		String drivesString = search.getDriveSettings();
		if (drivesString != null) {
			StringTokenizer stok = new StringTokenizer(drivesString, File.pathSeparator);
			while (stok.hasMoreTokens()) {
				String driveString = stok.nextToken();
				DriveSearchSettings drive = new DriveSearchSettings();
				drive.load(driveString);
				drives.add(drive);
			}
		}
	}

	public void store() {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < drives.size(); i++) {
			DriveSearchSettings drive = (DriveSearchSettings) drives.get(i);
			buf.append(drive.encode());
			buf.append(File.pathSeparator);
		}
		search.setDriveSettings(buf.toString());
	}
}
