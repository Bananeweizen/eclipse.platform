/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.launchConfigurations;


import java.net.URL;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.ant.core.AntCorePreferences;
import org.eclipse.ant.core.AntRunner;
import org.eclipse.ant.core.Property;
import org.eclipse.ant.core.Task;
import org.eclipse.ant.core.Type;
import org.eclipse.ant.internal.ui.model.AntUIPlugin;
import org.eclipse.ant.internal.ui.model.AntUtil;
import org.eclipse.ant.internal.ui.model.IAntUIConstants;
import org.eclipse.ant.internal.ui.model.IAntUIPreferenceConstants;
import org.eclipse.ant.internal.ui.preferences.MessageDialogWithToggle;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.RefreshTab;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.SocketUtil;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.externaltools.internal.launchConfigurations.ExternalToolsUtil;
import org.eclipse.ui.externaltools.internal.program.launchConfigurations.BackgroundResourceRefresher;

/**
 * Launch delegate for Ant builds
 */
public class AntLaunchDelegate implements ILaunchConfigurationDelegate {
	
	private static final String ANT_LOGGER_CLASS = "org.eclipse.ant.internal.ui.antsupport.logger.AntProcessBuildLogger"; //$NON-NLS-1$
	private static final String NULL_LOGGER_CLASS = "org.eclipse.ant.internal.ui.antsupport.logger.NullBuildLogger"; //$NON-NLS-1$
	private static final String REMOTE_ANT_LOGGER_CLASS = "org.eclipse.ant.internal.ui.antsupport.logger.RemoteAntBuildLogger"; //$NON-NLS-1$
	private static final String BASE_DIR_PREFIX = "-Dbasedir="; //$NON-NLS-1$
	private static final String INPUT_HANDLER_CLASS = "org.eclipse.ant.internal.ui.antsupport.inputhandler.AntInputHandler"; //$NON-NLS-1$	

	/**
	 * @see org.eclipse.debug.core.model.ILaunchConfigurationDelegate#launch(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String, org.eclipse.debug.core.ILaunch, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		if (monitor.isCanceled()) {
			return;
		}
		
		// migrate the config to the new classpath format if required
		AntUtil.migrateToNewClasspathFormat(configuration);
		
		String vmTypeID= null;
		try {
			//check if set to run in a separate VM
			vmTypeID = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_TYPE, (String)null);
		} catch (CoreException ce) {
			AntUIPlugin.log(ce);			
		}
		
		if (CommonTab.isLaunchInBackground(configuration)) {
			monitor.beginTask(MessageFormat.format(AntLaunchConfigurationMessages.getString("AntLaunchDelegate.Launching_{0}_1"), new String[] {configuration.getName()}), 10); //$NON-NLS-1$
		} else {
			monitor.beginTask(MessageFormat.format(AntLaunchConfigurationMessages.getString("AntLaunchDelegate.Running_{0}_2"), new String[] {configuration.getName()}), 100); //$NON-NLS-1$
		}
		
		// resolve location
		IPath location = ExternalToolsUtil.getLocation(configuration);
		monitor.worked(1);
		
		if (monitor.isCanceled()) {
			return;
		}
		
		if (vmTypeID == null && AntRunner.isBuildRunning()) {
			IStatus status= new Status(IStatus.ERROR, IAntUIConstants.PLUGIN_ID, 1, MessageFormat.format(AntLaunchConfigurationMessages.getString("AntLaunchDelegate.Build_In_Progress"), new String[]{location.toOSString()}), null); //$NON-NLS-1$
			throw new CoreException(status);
		}		
		
		// resolve working directory
		IPath workingDirectory = ExternalToolsUtil.getWorkingDirectory(configuration);
		String basedir = null;
		if (workingDirectory != null) {
			basedir= workingDirectory.toOSString();
		}
		monitor.worked(1);
		
		if (monitor.isCanceled()) {
			return;
		}

		// link the process to its build logger via a timestamp
		long timeStamp = System.currentTimeMillis();
		String idStamp = Long.toString(timeStamp);
		StringBuffer idProperty = new StringBuffer("-D"); //$NON-NLS-1$
		idProperty.append(AntProcess.ATTR_ANT_PROCESS_ID);
		idProperty.append('=');
		idProperty.append(idStamp);
		
		// resolve arguments
		String[] arguments = ExternalToolsUtil.getArguments(configuration);
		
		Map userProperties= AntUtil.getProperties(configuration);
		String[] propertyFiles= AntUtil.getPropertyFiles(configuration);
		String[] targets = AntUtil.getTargetsFromConfig(configuration);
		URL[] customClasspath= AntUtil.getCustomClasspath(configuration);
		String antHome= AntUtil.getAntHome(configuration);
		
		AntRunner runner= null;
		if (vmTypeID == null) {
			runner = configureAntRunner(configuration, location, basedir, idProperty, arguments, userProperties, propertyFiles, targets, customClasspath, antHome);
		}
		 
		monitor.worked(1);
								
		if (monitor.isCanceled()) {
			return;
		}
		boolean captureOutput= ExternalToolsUtil.getCaptureOutput(configuration);
		int port= -1;
		if (vmTypeID != null && captureOutput) {
			if (userProperties == null) {
				userProperties= new HashMap();
			}
			port= SocketUtil.findFreePort();
			userProperties.put(AntProcess.ATTR_ANT_PROCESS_ID, idStamp);
			userProperties.put("eclipse.connect.port", Integer.toString(port)); //$NON-NLS-1$
		}
		StringBuffer commandLine= generateCommandLine(location, arguments, userProperties, propertyFiles, targets, antHome, basedir, vmTypeID != null, captureOutput);
		
		if (vmTypeID != null) {
			monitor.beginTask(MessageFormat.format(AntLaunchConfigurationMessages.getString("AntLaunchDelegate.Launching_{0}_1"), new String[] {configuration.getName()}), 10); //$NON-NLS-1$
			runInSeparateVM(configuration, launch, monitor, idStamp, port, commandLine, captureOutput);
		} else {
			runInSameVM(configuration, launch, monitor, location, idStamp, runner, commandLine);
		}
		
		monitor.done();	
	}
	
	private void runInSameVM(ILaunchConfiguration configuration, ILaunch launch, IProgressMonitor monitor, IPath location, String idStamp, AntRunner runner, StringBuffer commandLine) throws CoreException {
		Map attributes= new HashMap(2);
		attributes.put(IProcess.ATTR_PROCESS_TYPE, IAntLaunchConfigurationConstants.ID_ANT_PROCESS_TYPE);
		attributes.put(AntProcess.ATTR_ANT_PROCESS_ID, idStamp);
				
		final AntProcess process = new AntProcess(location.toOSString(), launch, attributes);
		setProcessAttributes(process, idStamp, commandLine);
		
		if (CommonTab.isLaunchInBackground(configuration)) {
			final AntRunner finalRunner= runner;
			Runnable r = new Runnable() {
				public void run() {
					try {
						finalRunner.run(process);
					} catch (CoreException e) {
						handleException(e, AntLaunchConfigurationMessages.getString("AntLaunchDelegate.Failure")); //$NON-NLS-1$
					}
					process.terminated();
				}
			};
			Thread background = new Thread(r);
			background.start();
			monitor.worked(1);
			//refresh resources after process finishes
			if (RefreshTab.getRefreshScope(configuration) != null) {
				BackgroundResourceRefresher refresher = new BackgroundResourceRefresher(configuration, process);
				refresher.startBackgroundRefresh();
			}	
		} else {
			// execute the build 
			try {
				runner.run(monitor);
			} catch (CoreException e) {
				process.terminated();
				monitor.done();
				handleException(e,  AntLaunchConfigurationMessages.getString("AntLaunchDelegate.23")); //$NON-NLS-1$
				return;
			}
			process.terminated();
			
			// refresh resources
			RefreshTab.refreshResources(configuration, monitor);
		}
	}

	private AntRunner configureAntRunner(ILaunchConfiguration configuration, IPath location, String baseDir, StringBuffer idProperty, String[] arguments, Map userProperties, String[] propertyFiles, String[] targets, URL[] customClasspath, String antHome) throws CoreException {
		int argLength = 1; // at least one user property - timestamp
		if (arguments != null) {
			argLength += arguments.length;
		}		
		if (baseDir != null && baseDir.length() > 0) {
			argLength++;
		}
		String[] runnerArgs = new String[argLength];
		if (arguments != null) {
			System.arraycopy(arguments, 0, runnerArgs, 0, arguments.length);
		}
		if (baseDir != null && baseDir.length() > 0) {
			runnerArgs[runnerArgs.length - 2] = BASE_DIR_PREFIX + baseDir;
		}
		runnerArgs[runnerArgs.length -1] = idProperty.toString();
		
		AntRunner runner= new AntRunner();
		runner.setBuildFileLocation(location.toOSString());
		if (ExternalToolsUtil.getCaptureOutput(configuration)) {
			runner.addBuildLogger(ANT_LOGGER_CLASS);
		} else {
			runner.addBuildLogger(NULL_LOGGER_CLASS);
		}
		runner.setInputHandler(INPUT_HANDLER_CLASS);
		runner.setArguments(runnerArgs);
		if (userProperties != null) {
			runner.addUserProperties(userProperties);
		}

		if (propertyFiles != null) {
			runner.setPropertyFiles(propertyFiles);
		}

		if (targets != null) {
			runner.setExecutionTargets(targets);
		}

		if (customClasspath != null) {
			runner.setCustomClasspath(customClasspath);
		}

		if (antHome != null) {
			runner.setAntHome(antHome);
		}
		return runner;
	}

	private void handleException(final CoreException e, final String title) {
		IPreferenceStore store= AntUIPlugin.getDefault().getPreferenceStore();
		if (store.getBoolean(IAntUIPreferenceConstants.ANT_ERROR_DIALOG)) {
			AntUIPlugin.getStandardDisplay().asyncExec(new Runnable() {
				public void run() {
					MessageDialogWithToggle.openError(null, title, e.getMessage(), IAntUIPreferenceConstants.ANT_ERROR_DIALOG, AntLaunchConfigurationMessages.getString("AntLaunchDelegate.22"), AntUIPlugin.getDefault().getPreferenceStore()); //$NON-NLS-1$
				}
			});
		}
	}

	private void setProcessAttributes(IProcess process, String idStamp, StringBuffer commandLine) {
		// link the process to its build logger via a timestamp
		process.setAttribute(AntProcess.ATTR_ANT_PROCESS_ID, idStamp);
		
		// create "fake" command line for the process
		if (commandLine != null) {
			process.setAttribute(IProcess.ATTR_CMDLINE, commandLine.toString());
		}
		TaskLinkManager.registerAntBuild(process);
	}

	private StringBuffer generateCommandLine(IPath location, String[] arguments, Map userProperties, String[] propertyFiles, String[] targets, String antHome, String basedir, boolean separateVM, boolean captureOutput) {
		StringBuffer commandLine= new StringBuffer();

		if (!separateVM) {
			commandLine.append("ant"); //$NON-NLS-1$
		}
		
		if (arguments != null) {
			for (int i = 0; i < arguments.length; i++) {
				String arg = arguments[i];
				commandLine.append(' ');
				commandLine.append(arg);
			}
		}
		
		AntCorePreferences prefs= AntCorePlugin.getPlugin().getPreferences();
		if (propertyFiles == null) { //global
			String[] files= prefs.getCustomPropertyFiles();
			for (int i = 0; i < files.length; i++) {
				String path = files[i];
				commandLine.append(" -propertyfile \""); //$NON-NLS-1$
				commandLine.append(path);
				commandLine.append('\"');
			}
		} else {//"local" configuration
			for (int i = 0; i < propertyFiles.length; i++) {
				String path = propertyFiles[i];
				commandLine.append(" -propertyfile \""); //$NON-NLS-1$
				commandLine.append(path);
				commandLine.append('\"');
			}
		}
		//"local" configuration
		if (userProperties != null) {
			Iterator keys = userProperties.keySet().iterator();
			String key;
			while (keys.hasNext()) {
				key= (String)keys.next();
				appendProperty(commandLine, key, (String)userProperties.get(key));
			}
		}
		
		//global
		List properties= null;
		if (!separateVM) {
			properties= prefs.getProperties();
		} else {
			properties= prefs.getRemoteAntProperties();
		}
		
		String key;
		for (Iterator iter = properties.iterator(); iter.hasNext();) {
			Property property = (Property) iter.next();
			key= property.getName();
			String value= property.getValue();
			//if we have user properties this means that the user has chosen to override the global properties
			//if in a separate VM and have only two user properties these are really only Eclipse generated properties
			//and the user is still using the global properties
			if (value != null && (userProperties == null || (separateVM && userProperties.size() == 2))) {
				appendProperty(commandLine, key, value);
			}
		}
		
		if (basedir != null && basedir.length() > 0) {
			appendProperty(commandLine, "basedir", basedir); //$NON-NLS-1$
		}
		
		if (antHome != null) {
			commandLine.append(" \"-Dant.home="); //$NON-NLS-1$
			commandLine.append(antHome);
			commandLine.append('\"');
		}
		
		if (separateVM) { 
			if (commandLine.indexOf("-logger") == -1 && captureOutput) { //$NON-NLS-1$
				commandLine.append(" -logger "); //$NON-NLS-1$
				commandLine.append(REMOTE_ANT_LOGGER_CLASS);
			}
		} else {
			commandLine.append(" -inputhandler "); //$NON-NLS-1$
			commandLine.append(INPUT_HANDLER_CLASS);
		
			commandLine.append(" -logger "); //$NON-NLS-1$
			if (captureOutput) {
				commandLine.append(ANT_LOGGER_CLASS);
			} else {
				commandLine.append(NULL_LOGGER_CLASS);
			}
		}
		
		if (separateVM) {
			appendTaskAndTypes(prefs, commandLine);
		}
		commandLine.append(" -buildfile \""); //$NON-NLS-1$
		commandLine.append(location.toOSString());
		commandLine.append('\"');
		
		if (targets != null) {
			for (int i = 0; i < targets.length; i++) {
				commandLine.append(" \""); //$NON-NLS-1$
				commandLine.append(targets[i]);
				commandLine.append('\"');
			}
		}
		return commandLine;
	}
	
	private void appendTaskAndTypes(AntCorePreferences prefs, StringBuffer commandLine) {
		List tasks= prefs.getRemoteTasks();
		Iterator itr= tasks.iterator();
		while (itr.hasNext()) {
			Task task = (Task) itr.next();
			commandLine.append(" -eclipseTask "); //$NON-NLS-1$
			commandLine.append(task.getTaskName());
			commandLine.append(',');
			commandLine.append(task.getClassName());
		}
		
		List types= prefs.getRemoteTypes();
		itr= types.iterator();
		while (itr.hasNext()) {
			Type type = (Type) itr.next();
			commandLine.append(" -eclipseType "); //$NON-NLS-1$
			commandLine.append(type.getTypeName());
			commandLine.append(',');
			commandLine.append(type.getClassName());
		}
	}

	private void appendProperty(StringBuffer commandLine, String name, String value) {
		commandLine.append(" \"-D"); //$NON-NLS-1$
		commandLine.append(name);
		commandLine.append('='); 
		commandLine.append(value);
		commandLine.append(" \""); //$NON-NLS-1$
	}
	
	private void runInSeparateVM(ILaunchConfiguration configuration, ILaunch launch, IProgressMonitor monitor, String idStamp, int port, StringBuffer commandLine, boolean captureOutput) throws CoreException {
		if (captureOutput) {
			RemoteAntBuildListener client= new RemoteAntBuildListener(launch);
			if (port != -1) {
				client.startListening(port);
			}
		}
		
		ILaunchConfigurationWorkingCopy copy= configuration.getWorkingCopy();
		copy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, commandLine.toString());
		
		launch.setSourceLocator(new ISourceLocator() {
			/* (non-Javadoc)
			 * @see org.eclipse.debug.core.model.ISourceLocator#getSourceElement(org.eclipse.debug.core.model.IStackFrame)
			 */
			public Object getSourceElement(IStackFrame stackFrame) {
				return null;
			}
		});
		//copy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, "-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000"); //$NON-NLS-1$
		IProgressMonitor subMonitor= new SubProgressMonitor(monitor, 10);
		AntJavaLaunchDelegate delegate= new AntJavaLaunchDelegate();
		delegate.preLaunchCheck(copy, ILaunchManager.RUN_MODE, subMonitor);
		delegate.launch(copy, ILaunchManager.RUN_MODE, launch, subMonitor);
		final IProcess[] processes= launch.getProcesses();
		for (int i = 0; i < processes.length; i++) {
			setProcessAttributes(processes[i], idStamp, null);
		}

		if (CommonTab.isLaunchInBackground(configuration)) {
			// refresh resources after process finishes
			if (RefreshTab.getRefreshScope(configuration) != null) {
				BackgroundResourceRefresher refresher = new BackgroundResourceRefresher(configuration, processes[0]);
				refresher.startBackgroundRefresh();
			}
		} else {
			final boolean[] terminated= new boolean[1];
			terminated[0]= launch.isTerminated();
			IDebugEventSetListener listener= new IDebugEventSetListener() {
				public void handleDebugEvents(DebugEvent[] events) {
					for (int i = 0; i < events.length; i++) {
						DebugEvent event = events[i];
						for (int j= 0, numProcesses= processes.length; j < numProcesses; j++) {
							if (event.getSource() == processes[j] && event.getKind() == DebugEvent.TERMINATE) {
								terminated[0]= true;
								break;
							}
						}
					}
				}
			};
			DebugPlugin.getDefault().addDebugEventListener(listener);
			monitor.subTask(AntLaunchConfigurationMessages.getString("AntLaunchDelegate.28")); //$NON-NLS-1$
			while (!monitor.isCanceled() && !terminated[0]) {
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
				}
			}
			DebugPlugin.getDefault().removeDebugEventListener(listener);
			if (!monitor.isCanceled()) {
				// refresh resources
				RefreshTab.refreshResources(configuration, monitor);
			}
		}
	}
}