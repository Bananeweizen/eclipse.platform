package org.eclipse.ant.tests.core.testplugin;

/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp.  All rights reserved.
This file is made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html
**********************************************************************/

import java.util.*;
import java.util.ArrayList;
import java.util.List;

public class AntTestChecker {
	
	private static AntTestChecker deflt= null;
	
	private int taskStartedCount;
	
	private int taskFinishedCount;
	
	private int targetsStartedCount;
	
	private int targetsFinishedCount;
	
	private int buildsStartedCount;
	
	private int buildsFinishedCount;
	
	private List messages= new ArrayList();
	
	private List targets= new ArrayList();
	
	private List tasks= new ArrayList();
	
	private List projects= new ArrayList();
	
	private String currentProject;
	
	private String currentTask;
	
	private String currentTarget;
	
	private Hashtable userProperties;
	
	private List nameOfListeners= new ArrayList();
	
	private AntTestChecker()  {
	}
	
	/**
	 * Returns the singleton AntLoggerChecker
	 */
	public static AntTestChecker getDefault() {
		if (deflt == null) {
			deflt= new AntTestChecker();
		}
		return deflt;
	}
	
	/**
	 * Returns the singleton AntLoggerChecker
	 */
	public static void reset() {
		if (deflt != null) {
			deflt.resetState();
		}
	}
	/**
	 * @see org.apache.tools.ant.BuildListener#buildFinished(org.apache.tools.ant.BuildEvent)
	 */
	public void buildFinished(String projectName) {
		buildsFinishedCount++;
	}

	
	public void buildStarted(String projectName) {
		buildsStartedCount++;
		projects.add(projectName);
		currentProject= projectName;
	}

	
	public void messageLogged(String message) {
		messages.add(message);
	}

	/**
	 * @see org.apache.tools.ant.BuildListener#targetFinished(org.apache.tools.ant.BuildEvent)
	 */
	public void targetFinished(String targetName) {
		targetsFinishedCount++;
	}

	/**
	 * @see org.apache.tools.ant.BuildListener#targetStarted(org.apache.tools.ant.BuildEvent)
	 */
	public void targetStarted(String targetName) {
		targetsStartedCount++;
		targets.add(targetName);
	}

	/**
	 * @see org.apache.tools.ant.BuildListener#taskFinished(org.apache.tools.ant.BuildEvent)
	 */
	public void taskFinished(String taskName) {
		taskFinishedCount++;
	}

	/**
	 * @see org.apache.tools.ant.BuildListener#taskStarted(org.apache.tools.ant.BuildEvent)
	 */
	public void taskStarted(String taskName) {
		taskStartedCount++;
		tasks.add(taskName);
	}
	
	/**
	 * Returns the buildsFinishedCount.
	 * @return int
	 */
	public int getBuildsFinishedCount() {
		return buildsFinishedCount;
	}

	/**
	 * Returns the buildsStartedCount.
	 * @return int
	 */
	public int getBuildsStartedCount() {
		return buildsStartedCount;
	}

	/**
	 * Returns the messagesLoggedCount.
	 * @return int
	 */
	public int getMessagesLoggedCount() {
		return messages.size();
	}

	/**
	 * Returns the targetsFinishedCount.
	 * @return int
	 */
	public int getTargetsFinishedCount() {
		return targetsFinishedCount;
	}

	/**
	 * Returns the targetsStartedCount.
	 * @return int
	 */
	public int getTargetsStartedCount() {
		return targetsStartedCount;
	}

	/**
	 * Returns the taskFinishedCount.
	 * @return int
	 */
	public int getTaskFinishedCount() {
		return taskFinishedCount;
	}

	/**
	 * Returns the taskStartedCount.
	 * @return int
	 */
	public int getTaskStartedCount() {
		return taskStartedCount;
	}
	
	protected void resetState() {
		taskStartedCount= 0;
		taskFinishedCount= 0;
		targetsStartedCount= 0;
		targetsFinishedCount= 0;
		buildsStartedCount= 0;
		buildsFinishedCount= 0;
		messages= new ArrayList();
		tasks= new ArrayList();
		targets= new ArrayList();
		projects= new ArrayList();
		userProperties= null;
		currentProject= null;
		currentTarget= null;
		currentTask= null;
		nameOfListeners= new ArrayList();
	}
	
	
	public String getLastMessageLogged() {
		if (messages.isEmpty()) {
			return null;
		}
		return (String) messages.get(messages.size() - 1);
	}
	
	public void setUserProperties(Hashtable userProperties) {
		this.userProperties= userProperties;
	}

	public String getUserProperty(String name) {
		return (String)userProperties.get(name);
	}
	
	public List getMessages() {
		return messages;
	}
	
	public List getListeners() {
		return nameOfListeners;
	}
	
	public String getLastListener() {
		return (String)nameOfListeners.get(nameOfListeners.size() - 1);
	}


	public void addNameOfListener(String nameOfListener) {
		this.nameOfListeners.add(nameOfListener);
	}
}
