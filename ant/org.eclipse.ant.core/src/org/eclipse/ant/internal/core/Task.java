package org.eclipse.ant.internal.core;

/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp.  All rights reserved.
This file is made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html
**********************************************************************/

import java.net.URL;

public class Task {

	protected String taskName;
	protected String className;
	protected URL library;

	/**
	 * Gets the className.
	 * @return Returns a String
	 */
	public String getClassName() {
		return className;
	}

	/**
	 * Sets the className.
	 * @param className The className to set
	 */
	public void setClassName(String className) {
		this.className = className;
	}

	/**
	 * Gets the library.
	 * @return Returns a URL
	 */
	public URL getLibrary() {
		return library;
	}

	/**
	 * Sets the library.
	 * @param library The library to set
	 */
	public void setLibrary(URL library) {
		this.library = library;
	}

	/**
	 * Gets the taskName.
	 * @return Returns a String
	 */
	public String getTaskName() {
		return taskName;
	}

	/**
	 * Sets the taskName.
	 * @param taskName The taskName to set
	 */
	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}
}