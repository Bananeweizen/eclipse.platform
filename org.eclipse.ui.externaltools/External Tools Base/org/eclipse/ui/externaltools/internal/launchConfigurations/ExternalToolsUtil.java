/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Keith Seitz (keiths@redhat.com) - environment variables contribution (Bug 27243(
 *******************************************************************************/
package org.eclipse.ui.externaltools.internal.launchConfigurations;


import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.ui.RefreshTab;
import org.eclipse.ui.externaltools.internal.model.IExternalToolConstants;
import org.eclipse.ui.externaltools.internal.registry.ExternalToolMigration;

/**
 * Utilities for external tool launch configurations.
 * <p>
 * This class it not intended to be instantiated.
 * </p>
 */
public class ExternalToolsUtil {

	private static final String LAUNCH_CONFIG_HANDLE = "LaunchConfigHandle"; //$NON-NLS-1$

	/**
	 * Argument parsing constants
	 */
	private static final char ARG_DELIMITER = ' ';
	private static final char ARG_DBL_QUOTE = '"';
	private static final char ARG_BACKSLASH = '\\';
	
	/**
	 * Not to be instantiated.
	 */
	private ExternalToolsUtil() {
	}

	/**
	 * Throws a core exception with an error status object built from
	 * the given message, lower level exception, and error code.
	 * 
	 * @param message the status message
	 * @param exception lower level exception associated with the
	 *  error, or <code>null</code> if none
	 * @param code error code
	 */
	protected static void abort(String message, Throwable exception, int code) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, IExternalToolConstants.PLUGIN_ID, code, message, exception));
	}
	
	/**
	 * Expands and returns the location attribute of the given launch
	 * configuration. The location is
	 * verified to point to an existing file, in the local file system.
	 * 
	 * @param configuration launch configuration
	 * @return an absolute path to a file in the local file system  
	 * @throws CoreException if unable to retrieve the associated launch
	 * configuration attribute, if unable to resolve any variables, or if the
	 * resolved location does not point to an existing file in the local file
	 * system
	 */
	public static IPath getLocation(ILaunchConfiguration configuration) throws CoreException {
		String location = configuration.getAttribute(IExternalToolConstants.ATTR_LOCATION, (String) null);
		if (location == null) {
			abort(MessageFormat.format(ExternalToolsLaunchConfigurationMessages.getString("ExternalToolsUtil.Location_not_specified_by_{0}_1"), new String[] { configuration.getName()}), null, 0); //$NON-NLS-1$
		} else {
			String expandedLocation = getStringVariableManager().performStringSubstitution(location);
			if (expandedLocation == null || expandedLocation.length() == 0) {
				String msg = MessageFormat.format(ExternalToolsLaunchConfigurationMessages.getString("ExternalToolsUtil.invalidLocation_{0}"), new Object[] { configuration.getName()}); //$NON-NLS-1$
				abort(msg, null, 0);
			} else {
				File file = new File(expandedLocation);
				if (file.isFile()) {
					return new Path(expandedLocation);
				} else {
					String msg = MessageFormat.format(ExternalToolsLaunchConfigurationMessages.getString("ExternalToolsUtil.invalidLocation_{0}"), new Object[] { configuration.getName()}); //$NON-NLS-1$
					abort(msg, null, 0);
				}
			}
		}
		// execution will not reach here
		return null;
	}
	
	/**
	 * Returns a boolean specifying whether or not output should be captured for
	 * the given configuration
	 * 
	 * @param configuration the configuration from which the value will be
	 * extracted
	 * @return boolean specifying whether or not output should be captured
	 * @throws CoreException if unable to access the associated attribute
	 */
	public static boolean getCaptureOutput(ILaunchConfiguration configuration) throws CoreException {
		return configuration.getAttribute(IExternalToolConstants.ATTR_CAPTURE_OUTPUT, true);
	}

	/**
	 * Expands and returns the working directory attribute of the given launch
	 * configuration. Returns <code>null</code> if a working directory is not
	 * specified. If specified, the working is verified to point to an existing
	 * directory in the local file system.
	 * 
	 * @param configuration launch configuration
	 * @return an absolute path to a directory in the local file system, or
	 * <code>null</code> if unspecified
	 * @throws CoreException if unable to retrieve the associated launch
	 * configuration attribute, if unable to resolve any variables, or if the
	 * resolved location does not point to an existing directory in the local
	 * file system
	 */
	public static IPath getWorkingDirectory(ILaunchConfiguration configuration) throws CoreException {
		String location = configuration.getAttribute(IExternalToolConstants.ATTR_WORKING_DIRECTORY, (String) null);
		if (location != null) {
			String expandedLocation = getStringVariableManager().performStringSubstitution(location);
			if (expandedLocation.length() > 0) {
				File path = new File(expandedLocation);
				if (path.isDirectory()) {
					return new Path(expandedLocation);
				} else {
					String msg = MessageFormat.format(ExternalToolsLaunchConfigurationMessages.getString("ExternalToolsUtil.invalidDirectory_{0}"), new Object[] { expandedLocation, configuration.getName()}); //$NON-NLS-1$
					abort(msg, null, 0);
				}
			}
		}
		return null;
	}

	/**
	 * Expands and returns the arguments attribute of the given launch
	 * configuration. Returns <code>null</code> if arguments are not specified.
	 * 
	 * @param configuration launch configuration
	 * @return an array of resolved arguments, or <code>null</code> if
	 * unspecified
	 * @throws CoreException if unable to retrieve the associated launch
	 * configuration attribute, or if unable to resolve any variables
	 */
	public static String[] getArguments(ILaunchConfiguration configuration) throws CoreException {
		String args = configuration.getAttribute(IExternalToolConstants.ATTR_TOOL_ARGUMENTS, (String) null);
		if (args != null) {
			String expanded = getStringVariableManager().performStringSubstitution(args);
			return parseStringIntoList(expanded);
		}
		return null;
	}

	private static IStringVariableManager getStringVariableManager() {
		return VariablesPlugin.getDefault().getStringVariableManager();
	}
	
	/**
	 * Returns whether the given launch configuration is enabled. This property
	 * is intended only to apply to external tool builder configurations and
	 * determines whether the project builder will launch the configuration
	 * when it builds.
	 *  
	 * @param configuration the configuration for which the enabled state should
	 * 		be determined.
	 * @return whether the given configuration is enabled to be run when a build occurs.
	 * @throws CoreException if unable to access the associated attribute
	 */
	public static boolean isBuilderEnabled(ILaunchConfiguration configuration) throws CoreException {
		return configuration.getAttribute(IExternalToolConstants.ATTR_BUILDER_ENABLED, true);
	}
	
	/**
	 * Returns the collection of resources for the build scope as specified by the given launch configuration.
	 * 
	 * @param configuration launch configuration
	 * @param context context used to expand variables
	 * @throws CoreException if an exception occurs while retrieving the resources
	 */
	public static IResource[] getResourcesForBuildScope(ILaunchConfiguration configuration) throws CoreException {
		String scope = configuration.getAttribute(IExternalToolConstants.ATTR_BUILD_SCOPE, (String) null);
		if (scope == null) {
			return null;
		}
	
		return RefreshTab.getRefreshResources(scope);
	}

	/**
	 * Returns a launch configuration from the given ICommand arguments. If the
	 * given arguments are from an old-style external tool, an unsaved working
	 * copy will be created from the arguments and returned.
	 * 
	 * @param commandArgs the builder ICommand arguments
	 * @param newName a new name for the config if the one in the command is
	 * invalid
	 * @return a launch configuration, a launch configuration working copy, or
	 * <code>null</code> if not possible.
	 */
	public static ILaunchConfiguration configFromBuildCommandArgs(Map commandArgs) {
		String configHandle = (String) commandArgs.get(LAUNCH_CONFIG_HANDLE);
		if (configHandle == null) {
			// Probably an old-style external tool. Try to migrate.
			return ExternalToolMigration.configFromArgumentMap(commandArgs);
		}
		try {
			return DebugPlugin.getDefault().getLaunchManager().getLaunchConfiguration(configHandle);
		} catch (CoreException e) {
			return null;
		}
	}
	
	/**
	 * Parses the argument text into an array of individual
	 * strings using the space character as the delimiter.
	 * An individual argument containing spaces must have a
	 * double quote (") at the start and end. Two double 
	 * quotes together is taken to mean an embedded double
	 * quote in the argument text.
	 * 
	 * @param arguments the arguments as one string
	 * @return the array of arguments
	 */
	public static String[] parseStringIntoList(String arguments) {
		if (arguments == null || arguments.length() == 0) {
			return new String[0];
		}
		
		List list = new ArrayList(10);
		boolean inQuotes = false;
		int start = 0;
		int end = arguments.length();
		StringBuffer buffer = new StringBuffer(end);
		
		while (start < end) {
			char ch = arguments.charAt(start);
			start++;
			
			switch (ch) {
				case ARG_DELIMITER :
					if (inQuotes) {
						buffer.append(ch);
					} else {
						if (buffer.length() > 0) {
							list.add(buffer.toString());
							buffer.setLength(0);
						}
					}
					break;
					
				case ARG_BACKSLASH:
					if (start < end && (arguments.charAt(start) == ARG_DBL_QUOTE)) {
						// Escaped double-quote
						buffer.append('\"');
						start++;
					} else {
						buffer.append(ch);
					}
					break;
	
				case ARG_DBL_QUOTE :
					inQuotes = !inQuotes;
					break;
						
				default :
					buffer.append(ch);
					break;
			}
			
		}
		
		if (buffer.length() > 0) {
			list.add(buffer.toString());
		}
			
		String[] results = new String[list.size()];
		list.toArray(results);
		return results;
	}	
}
