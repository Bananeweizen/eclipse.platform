package org.eclipse.ant.core;

/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp.  All rights reserved.
This file is made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html
**********************************************************************/

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.*;

import org.eclipse.ant.internal.core.IAntCoreConstants;
import org.eclipse.ant.internal.core.InternalCoreAntMessages;
import org.eclipse.core.boot.BootLoader;
import org.eclipse.core.boot.IPlatformRunnable;
import org.eclipse.core.runtime.*;

/**
 * Entry point for running Ant scripts inside Eclipse.
 */
public class AntRunner implements IPlatformRunnable {

	protected String buildFileLocation = IAntCoreConstants.DEFAULT_BUILD_FILENAME;
	protected List buildListeners;
	protected String[] targets;
	protected Map userProperties;
	protected int messageOutputLevel = 2; // Project.MSG_INFO
	protected String buildLoggerClassName;
	protected String[] arguments;

	/** 
	 * Constructs an instance of this class.
	 */
	public AntRunner() {
	}

	/**
	 * Sets the build file location on the file system.
	 * 
	 * @param buildFileLocation the file system location of the build file
	 */
	public void setBuildFileLocation(String buildFileLocation) {
		if (buildFileLocation == null) {
			this.buildFileLocation = IAntCoreConstants.DEFAULT_BUILD_FILENAME;
		} else {
			this.buildFileLocation = buildFileLocation;
		}
	}

	/**
	 * Set the message output level.
	 * <p>
	 * Valid values are:
	 * <ul>
	 * <li><code>org.apache.tools.ant.Project.ERR</code>, 
	 * <li><code>org.apache.tools.ant.Project.WARN</code>,
	 * <li><code>org.apache.tools.ant.Project.INFO</code>,
	 * <li><code>org.apache.tools.ant.Project.VERBOSE</code> or
	 * <li><code>org.apache.tools.ant.Project.DEBUG</code>
	 * </ul>
	 * 
	 * @param level the message output level
	 */
	public void setMessageOutputLevel(int level) {
		messageOutputLevel = level;
	}

	/**
	 * Sets the arguments to be passed to the script (e.g. -Dos=win32 -Dws=win32 -verbose).
	 * 
	 * @param arguments the arguments to be passed to the script
	 */
	public void setArguments(String arguments) {
		this.arguments = getArray(arguments);
	}

	/**
	 * Helper method to ensure an array is converted into an ArrayList.
	 */
	private String[] getArray(String args) {
		StringBuffer sb = new StringBuffer();
		boolean waitingForQuote = false;
		ArrayList result = new ArrayList();
		for (StringTokenizer tokens = new StringTokenizer(args, ", \"", true); tokens.hasMoreTokens();) { //$NON-NLS-1$
			String token = tokens.nextToken();
			if (waitingForQuote) {
				if (token.equals("\"")) { //$NON-NLS-1$
					result.add(sb.toString());
					sb.setLength(0);
					waitingForQuote = false;
				} else {
					sb.append(token);
				}
			} else {
				if (token.equals("\"")) { //$NON-NLS-1$
					// test if we have something like -Dproperty="value"
					if (result.size() > 0) {
						int index = result.size() - 1;
						String last = (String) result.get(index);
						if (last.charAt(last.length() - 1) == '=') {
							result.remove(index);
							sb.append(last);
						}
					}
					waitingForQuote = true;
				} else {
					if (!(token.equals(",") || token.equals(" "))) //$NON-NLS-1$ //$NON-NLS-2$
						result.add(token);
				}
			}
		}
		return (String[]) result.toArray(new String[result.size()]);
	}

	/**
	 * Sets the arguments to be passed to the script (e.g. -Dos=win32 -Dws=win32 -verbose).
	 * 
	 * @param arguments the arguments to be passed to the script
	 * @since 2.1
	 */
	public void setArguments(String[] arguments) {
		this.arguments = arguments;
	}

	/**
	 * Sets the targets and execution order.
	 * 
	 * @param executionTargets which targets should be run and in which order
	 */
	public void setExecutionTargets(String[] executionTargets) {
		this.targets = executionTargets;
	}

	/**
	 * Adds a build listener. The parameter <code>className</code>
	 * is the class name of a <code>org.apache.tools.ant.BuildListener</code>
	 * implementation. The class will be instantiated at runtime and the
	 * listener will be called on build events
	 * (<code>org.apache.tools.ant.BuildEvent</code>).
	 *
	 * @param className a build listener class name
	 */
	public void addBuildListener(String className) {
		if (className == null) {
			return;
		}
		if (buildListeners == null) {
			buildListeners = new ArrayList(5);
		}
		buildListeners.add(className);
	}

	/**
	 * Adds a build logger. The parameter <code>className</code>
	 * is the class name of a <code>org.apache.tools.ant.BuildLogger</code>
	 * implementation. The class will be instantiated at runtime and the
	 * logger will be called on build events
	 * (<code>org.apache.tools.ant.BuildEvent</code>).
	 *
	 * @param className a build logger class name
	 */
	public void addBuildLogger(String className) {
		buildLoggerClassName = className;
	}

	/**
	 * Adds user-defined properties. Keys and values must be String objects.
	 * 
	 * @param properties a Map of user-defined properties
	 */
	public void addUserProperties(Map properties) {
		userProperties = properties;
	}

	/**
	 * Returns the build file target information.
	 * 
	 * @return an array containing the target information
	 * 
	 * @see TargetInfo
	 * @since 2.1
	 */
	public TargetInfo[] getAvailableTargets() throws CoreException {
		Class classInternalAntRunner= null;
		Object runner= null;
		try {
			ClassLoader loader = AntCorePlugin.getPlugin().getClassLoader();
			classInternalAntRunner = loader.loadClass("org.eclipse.ant.internal.core.ant.InternalAntRunner"); //$NON-NLS-1$
			runner = classInternalAntRunner.newInstance();
			// set build file
			Method setBuildFileLocation = classInternalAntRunner.getMethod("setBuildFileLocation", new Class[] { String.class }); //$NON-NLS-1$
			setBuildFileLocation.invoke(runner, new Object[] { buildFileLocation });
			// get the info for each targets
			Method getTargets = classInternalAntRunner.getMethod("getTargets", null); //$NON-NLS-1$
			Object results = getTargets.invoke(runner, null);
			// collect the info into target objects
			String[][] infos = (String[][]) results;
			if (infos.length < 2) {
				return new TargetInfo[0];
			}
			// The last info is the name of the default target or null if none
			int count = infos.length - 1;
			String defaultName = infos[count][0];
			TargetInfo[] targets = new TargetInfo[count];
			for (int i = 0; i < count; i++) {
				String[] info = infos[i];
				boolean isDefault = info[0].equals(defaultName);
				targets[i] = new TargetInfo(info[0], info[1], isDefault);
			}
			return targets;
		} catch (NoClassDefFoundError e) {
			problemLoadingClass(e);
			//not possible to reach this line
			return new TargetInfo[0];
		} catch (ClassNotFoundException e) {
			problemLoadingClass(e);
			//not possible to reach this line
			return new TargetInfo[0];
		} catch (InvocationTargetException e) {
			handleInvocationTargetException(runner, classInternalAntRunner, e);
			//not possible to reach this line
			return new TargetInfo[0];
		} catch (Exception e) {
			String message = (e.getMessage() == null) ? InternalCoreAntMessages.getString("AntRunner.Build_Failed._3") : e.getMessage(); //$NON-NLS-1$
			throw new CoreException(new Status(IStatus.ERROR, AntCorePlugin.PI_ANTCORE, AntCorePlugin.ERROR_RUNNING_SCRIPT, message, e));
		}
	}

	/**
	 * Runs the build script. If a progress monitor is specified it will
	 * be available during the script execution as a reference in the
	 * Ant Project (<code>org.apache.tools.ant.Project.getReferences()</code>).
	 * A long-running task could, for example, get the monitor during its
	 * execution and check for cancellation. The key value to retrieve the
	 * progress monitor instance is <code>AntCorePlugin.ECLIPSE_PROGRESS_MONITOR</code>.
	 * 
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting and cancellation are not desired
	 */
	public void run(IProgressMonitor monitor) throws CoreException {
		long startTime = 0;
		if (IAntCoreConstants.DEBUG_BUILDFILE_TIMING) {
			startTime = System.currentTimeMillis();
		}
		Object runner= null;
		Class classInternalAntRunner= null;
		try {
			ClassLoader loader = AntCorePlugin.getPlugin().getClassLoader();
			classInternalAntRunner = loader.loadClass("org.eclipse.ant.internal.core.ant.InternalAntRunner"); //$NON-NLS-1$
			runner = classInternalAntRunner.newInstance();
			// set build file
			Method setBuildFileLocation = classInternalAntRunner.getMethod("setBuildFileLocation", new Class[] { String.class }); //$NON-NLS-1$
			setBuildFileLocation.invoke(runner, new Object[] { buildFileLocation });
			// add listeners
			if (buildListeners != null) {
				Method addBuildListeners = classInternalAntRunner.getMethod("addBuildListeners", new Class[] { List.class }); //$NON-NLS-1$
				addBuildListeners.invoke(runner, new Object[] { buildListeners });
			}
			
			if (buildLoggerClassName == null) {
				//indicate that the default logger is not to be used
				buildLoggerClassName= "";
			}
			// add build logger
			Method addBuildLogger = classInternalAntRunner.getMethod("addBuildLogger", new Class[] { String.class }); //$NON-NLS-1$
			addBuildLogger.invoke(runner, new Object[] { buildLoggerClassName });
			
			// add progress monitor
			if (monitor != null) {
				Method setProgressMonitor = classInternalAntRunner.getMethod("setProgressMonitor", new Class[] { IProgressMonitor.class }); //$NON-NLS-1$
				setProgressMonitor.invoke(runner, new Object[] { monitor });
			}
			// add properties
			if (userProperties != null) {
				Method addUserProperties = classInternalAntRunner.getMethod("addUserProperties", new Class[] { Map.class }); //$NON-NLS-1$
				addUserProperties.invoke(runner, new Object[] { userProperties });
			}
			// set message output level
			Method setMessageOutputLevel = classInternalAntRunner.getMethod("setMessageOutputLevel", new Class[] { int.class }); //$NON-NLS-1$
			setMessageOutputLevel.invoke(runner, new Object[] { new Integer(messageOutputLevel)});
			// set execution targets
			if (targets != null) {
				Method setExecutionTargets = classInternalAntRunner.getMethod("setExecutionTargets", new Class[] { String[].class }); //$NON-NLS-1$
				setExecutionTargets.invoke(runner, new Object[] { targets });
			} 
			// set extra arguments
			if (arguments != null && arguments.length > 0) {
				Method setArguments = classInternalAntRunner.getMethod("setArguments", new Class[] { String[].class }); //$NON-NLS-1$
				setArguments.invoke(runner, new Object[] { arguments });
			}
			// run
			Method run = classInternalAntRunner.getMethod("run", null); //$NON-NLS-1$
			run.invoke(runner, null);
		} catch (NoClassDefFoundError e) {
			problemLoadingClass(e);
		} catch (ClassNotFoundException e) {
			problemLoadingClass(e);
		} catch (InvocationTargetException e) {
			handleInvocationTargetException(runner, classInternalAntRunner, e);
		} catch (Exception e) {
			String message = (e.getMessage() == null) ? InternalCoreAntMessages.getString("AntRunner.Build_Failed._3") : e.getMessage(); //$NON-NLS-1$
			IStatus status= new Status(IStatus.ERROR, AntCorePlugin.PI_ANTCORE, AntCorePlugin.ERROR_RUNNING_SCRIPT, message, e);
			throw new CoreException(status);
		} finally {
			if (IAntCoreConstants.DEBUG_BUILDFILE_TIMING) {
				long finishTime = System.currentTimeMillis();
				System.out.println(InternalCoreAntMessages.getString("AntRunner.Buildfile_run_took___9") + (finishTime - startTime) + InternalCoreAntMessages.getString("AntRunner._milliseconds._10")); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	/**
	 * Handles exceptions that are loaded by the Ant Class Loader by
	 * asking the Internal Ant Runner class for the correct error message.
	 * 
	 * Handles nested NoClassDefFoundError and nested ClassNotFoundException	 */
	protected void handleInvocationTargetException(Object runner, Class classInternalAntRunner, InvocationTargetException e) throws CoreException {
		Throwable realException = e.getTargetException();
		String message= null;
		if (runner != null) {
			try {
				Method getBuildErrorMessage = classInternalAntRunner.getMethod("getBuildExceptionErrorMessage", new Class[] { Throwable.class }); //$NON-NLS-1$
				message= (String)getBuildErrorMessage.invoke(runner, new Object[] { realException });
			} catch (Exception ex) {
				//do nothing as already in error state
			}
		}
		// J9 throws NoClassDefFoundError nested in a InvocationTargetException
		if (message == null && ((realException instanceof NoClassDefFoundError) || (realException instanceof ClassNotFoundException))) {
			problemLoadingClass(realException);
			return;
		}
		if (message == null) {
			message = (realException.getMessage() == null) ? InternalCoreAntMessages.getString("AntRunner.Build_Failed._3") : realException.getMessage(); //$NON-NLS-1$
		}
		throw new CoreException(new Status(IStatus.ERROR, AntCorePlugin.PI_ANTCORE, AntCorePlugin.ERROR_RUNNING_SCRIPT, message, realException));
	}

	protected void problemLoadingClass(Throwable e) throws CoreException {
		String missingClassName= e.getMessage();
		String message;
		if (missingClassName != null) {
			missingClassName= missingClassName.replace('/', '.');
			message= InternalCoreAntMessages.getString("AntRunner.Could_not_find_one_or_more_classes._Please_check_the_Ant_classpath._2"); //$NON-NLS-1$
			message= MessageFormat.format(message, new String[]{missingClassName});
		} else {
			message= InternalCoreAntMessages.getString("AntRunner.Could_not_find_one_or_more_classes._Please_check_the_Ant_classpath._1"); //$NON-NLS-1$
		}
		IStatus status= new Status(IStatus.ERROR, AntCorePlugin.PI_ANTCORE, AntCorePlugin.ERROR_RUNNING_SCRIPT, message, e);
		AntCorePlugin.getPlugin().getLog().log(status);
		throw new CoreException(status);
	}

	/**
	 * Runs the build script.
	 */
	public void run() throws CoreException {
		run((IProgressMonitor) null);
	}

	/**
	 * Invokes the building of a project object and executes a build using either a given
	 * target or the default target. This method is called when running Eclipse headless
	 * and specifying <code>org.eclipse.ant.core.antRunner</code> as the application.
	 *
	 * @param argArray the command line arguments
	 * @exception Exception if a problem occurred during the script execution
	 */
	public Object run(Object argArray) throws Exception {
		// Add debug information if necessary - fix for bug 5672.
		// Since the platform parses the -debug command line arg
		// and removes it from the args passed to the applications,
		// we have to check if Eclipse is in debug mode in order to
		// forward the -debug argument to Ant.
		if (BootLoader.inDebugMode()) {
			String[] args = (String[]) argArray;
			String[] newArgs = new String[args.length + 1];
			for (int i = 0; i < args.length; i++) {
				newArgs[i] = args[i];
			}
			newArgs[args.length] = "-debug"; //$NON-NLS-1$
			argArray = newArgs;
		}
		ClassLoader loader = AntCorePlugin.getPlugin().getClassLoader();
		Class classInternalAntRunner = loader.loadClass("org.eclipse.ant.internal.core.ant.InternalAntRunner"); //$NON-NLS-1$
		Object runner = classInternalAntRunner.newInstance();
		Method run = classInternalAntRunner.getMethod("run", new Class[] { Object.class }); //$NON-NLS-1$
		run.invoke(runner, new Object[] { argArray });
		return null;
	}
}