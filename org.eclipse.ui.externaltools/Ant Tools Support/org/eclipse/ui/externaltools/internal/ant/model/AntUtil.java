package org.eclipse.ui.externaltools.internal.ant.model;

/**********************************************************************
Copyright (c) 2002 IBM Corp. and others. All rights reserved.
This file is made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html
�
Contributors:
**********************************************************************/

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.ant.core.AntRunner;
import org.eclipse.ant.core.TargetInfo;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.externaltools.internal.ant.view.AntView;
import org.eclipse.ui.externaltools.model.IExternalToolConstants;

/**
 * General utility class dealing with Ant build files
 */
public final class AntUtil {
	public static final String RUN_TARGETS_ATTRIBUTE = IExternalToolConstants.TOOL_TYPE_ANT_BUILD + ".runTargets"; //$NON-NLS-1$;
	public static final String ATTRIBUTE_SEPARATOR = ","; //$NON-NLS-1$;
	
	/**
	 * No instances allowed
	 */
	private AntUtil() {
		super();
	}
	
	/**
	 * Returns a single-string of the strings for storage.
	 * 
	 * @param targets the array of strings
	 * @return a single-string representation of the strings or
	 * <code>null</code> if the array is empty.
	 */
	public static String combineStrings(String[] strings) {
		if (strings.length == 0)
			return null;

		if (strings.length == 1)
			return strings[0];

		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < strings.length - 1; i++) {
			buf.append(strings[i]);
			buf.append(ATTRIBUTE_SEPARATOR);
		}
		buf.append(strings[strings.length - 1]);
		return buf.toString();
	}

	/**
	 * Returns an array of targets to be run, or <code>null</code> if none are
	 * specified (indicating the default target should be run).
	 *
	 * @param configuration launch configuration
	 * @return array of target names, or <code>null</code>
	 * @throws CoreException if unable to access the associated attribute
	 */
	public static String[] getTargetsFromConfig(ILaunchConfiguration configuration) throws CoreException {
		String attribute = configuration.getAttribute(IExternalToolConstants.ATTR_ANT_TARGETS, (String) null);
		if (attribute == null) {
			return null;
		} else {
			return AntUtil.parseRunTargets(attribute);
		}
	}
	
	/**
	 * Returns a map of properties to be defined for the build, or
	 * <code>null</code> if none are specified (indicating no additional
	 * properties specified for the build).
	 *
	 * @param configuration launch configuration
	 * @return map of properties (name --> value), or <code>null</code>
	 * @throws CoreException if unable to access the associated attribute
	 */
	public static Map getProperties(ILaunchConfiguration configuration) throws CoreException {
		Map map = configuration.getAttribute(IExternalToolConstants.ATTR_ANT_PROPERTIES, (Map) null);
		return map;
	}
	
	/**
	 * Returns a String specifying the ant home to use for the build, or
	 * <code>null</code> if none is specified.
	 *
	 * @param configuration launch configuration
	 * @return String specifying ant home to use, or <code>null</code>
	 * @throws CoreException if unable to access the associated attribute
	 */
	public static String getAntHome(ILaunchConfiguration configuration) throws CoreException {
		return configuration.getAttribute(IExternalToolConstants.ATTR_ANT_HOME, (String) null);
	}

	/**
	 * Returns an array of property files to be used for the build, or
	 * <code>null</code> if none are specified (indicating no additional
	 * property files specified for the build).
	 *
	 * @param configuration launch configuration
	 * @return array of property file names, or <code>null</code>
	 * @throws CoreException if unable to access the associated attribute
	 */
	public static String[] getPropertyFiles(ILaunchConfiguration configuration) throws CoreException {
		String attribute = configuration.getAttribute(IExternalToolConstants.ATTR_ANT_PROPERTY_FILES, (String) null);
		if (attribute == null) {
			return null;
		} else {
			return AntUtil.parseString(attribute, ","); //$NON-NLS-1$
		}
	}
	
	/**
	 * Returns the list of all targets for the Ant build file specified by
	 * the provided IPath, or <code>null</code> if no targets found.
	 * 
	 * @param path the location of the ant build file to get the targets from
	 * @return a list of <code>TargetInfo</code>
	 * 
	 * @throws CoreException if file does not exist, IO problems, or invalid format.
	 */
	public static TargetInfo[] getTargets(String path) throws CoreException {
		AntRunner runner = new AntRunner();
		runner.setBuildFileLocation(path);
	 	return runner.getAvailableTargets();
	}
	
	/**
	 * Returns the list of urls that define the custom classpath for the Ant
	 * build , or <code>null</code> if the global classpath is to be used.
	 *
	 * @param configuration launch configuration
	 * @return a list of <code>URL</code>
	 *
	 * @throws CoreException if file does not exist, IO problems, or invalid format.
	 */
	public static URL[] getCustomClasspath(ILaunchConfiguration config) throws CoreException {
		String classpathString= config.getAttribute(IExternalToolConstants.ATTR_ANT_CUSTOM_CLASSPATH, (String)null);
		if (classpathString == null) {
			return null;
		}
		List antURLs= new ArrayList();
		List userURLs= new ArrayList();
		getCustomClasspaths(config, antURLs, userURLs);
		URL[] custom= new URL[antURLs.size() + userURLs.size()];
		antURLs.addAll(userURLs);
		return (URL[])antURLs.toArray(custom);
	}
	
	public static void getCustomClasspaths(ILaunchConfiguration config, List antURLs, List userURLs) {
		String classpathString= null;
		try {
			classpathString = config.getAttribute(IExternalToolConstants.ATTR_ANT_CUSTOM_CLASSPATH, (String) null);
		} catch (CoreException e) {
		}
		if (classpathString == null) {
			return;
		}
		String antString= null;
		String userString= null;
		int delim= classpathString.indexOf('*');

		if (delim == -1) {
			antString= classpathString;
		} else {
			antString= classpathString.substring(0, delim);
			userString= classpathString.substring(delim+1);
		}

		String[] antStrings= AntUtil.parseString(antString, AntUtil.ATTRIBUTE_SEPARATOR);
		for (int i = 0; i < antStrings.length; i++) {
			String string = antStrings[i];
			try {
				antURLs.add(new URL("file:" + string)); //$NON-NLS-1$
			} catch (MalformedURLException e) {
			}
		}
		if (userString != null) {
			String[] userStrings=AntUtil.parseString(userString, AntUtil.ATTRIBUTE_SEPARATOR);
			for (int i = 0; i < userStrings.length; i++) {
				String string = userStrings[i];
				try {
					userURLs.add(new URL("file:" + string)); //$NON-NLS-1$
				} catch (MalformedURLException e) {
				}
			}

		}
	}
	
	/**
	 * Returns the currently displayed Ant View if it is open.
	 * 
	 * @return the Ant View open in the current workbench page or
	 * <code>null</code> if there is none.
	 */
	public static AntView getAntView() {
		IWorkbenchWindow window= PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window != null) {
			IWorkbenchPage page= window.getActivePage(); 
			if (page != null) {
				return (AntView) page.findView(IExternalToolConstants.ANT_VIEW_ID);
			}
		}
		return null;
	}
	
	/**
	 * Returns whether the target described by the given
	 * <code>TargetInfo</code> is a sub-target.
	 * 
	 * @param info the info of the target in question
	 * @return <code>true</code> if the target is a sub-target,
	 * 		<code>false</code> otherwise
	 */
	public static boolean isSubTarget(TargetInfo info) {
		return info.getDescription() == null;
	}
	
	/**
	 * Returns the list of target names to run
	 * 
	 * @param extraAttibuteValue the external tool's extra attribute value
	 * 		for the run targets key.
	 * @return a list of target names
	 */
	public static String[] parseRunTargets(String extraAttibuteValue) {
		return parseString(extraAttibuteValue, ATTRIBUTE_SEPARATOR);
	}
	
	/**
	 * Returns the list of Strings that were delimiter separated.
	 * 
	 * @param extraAttibuteValue the external tool's extra attribute value
	 * @return a list of Strings
	 */
	public static String[] parseString(String attibuteValue, String delim) {
		if (attibuteValue == null) {
			return new String[0];
		}
		
		// Need to handle case where separator character is
		// actually part of the target name!
		StringTokenizer tokenizer = new StringTokenizer(attibuteValue, delim);
		String[] results = new String[tokenizer.countTokens()];
		for (int i = 0; i < results.length; i++) {
			results[i] = tokenizer.nextToken();
		}
		
		return results;
	}
}
