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
package org.eclipse.debug.internal.ui;

 
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationManager;
import org.eclipse.debug.internal.ui.launchConfigurations.PerspectiveManager;
import org.eclipse.debug.internal.ui.preferences.DebugActionGroupsManager;
import org.eclipse.debug.internal.ui.preferences.IDebugPreferenceConstants;
import org.eclipse.debug.internal.ui.stringsubstitution.SelectedResourceManager;
import org.eclipse.debug.internal.ui.views.console.ConsoleDocumentManager;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.w3c.dom.Document;

/**
 * The Debug UI Plugin.
 *
 */
public class DebugUIPlugin extends AbstractUIPlugin implements ILaunchListener {															   
									   	
	/**
	 * The singleton debug plugin instance
	 */
	private static DebugUIPlugin fgDebugUIPlugin= null;
	
	/**
	 * A utility presentation used to obtain labels
	 */
	protected static IDebugModelPresentation fgPresentation = null;

	/**
	 * Default label provider
	 */	
	private static DefaultLabelProvider fgDefaultLabelProvider;
	
	/**
	 * Flag indicating whether the debug UI is in trace
	 * mode. When in trace mode, extra debug information
	 * is produced.
	 */
	private boolean fTrace = false;	
	
	/**
	 * Singleton console document manager
	 */
	private ConsoleDocumentManager fConsoleDocumentManager = null;
	
	/**
	 * Perspective manager
	 */
	private PerspectiveManager fPerspectiveManager = null;
	
	/**
	 * Launch configuration manager
	 */
	private LaunchConfigurationManager fLaunchConfigurationManager = null;
	
	/**
	 * Returns whether the debug UI plug-in is in trace
	 * mode.
	 * 
	 * @return whether the debug UI plug-in is in trace
	 *  mode
	 */
	public boolean isTraceMode() {
		return fTrace;
	}
	
	/**
	 * Logs the given message if in trace mode.
	 * 
	 * @param String message to log
	 */
	public static void logTraceMessage(String message) {
		if (getDefault().isTraceMode()) {
			IStatus s = new Status(IStatus.WARNING, IDebugUIConstants.PLUGIN_ID, IDebugUIConstants.INTERNAL_ERROR, message, null);
			getDefault().getLog().log(s);
		}
	}

	/**
	 * Constructs the debug UI plugin
	 */
	public DebugUIPlugin(IPluginDescriptor descriptor) {
		super(descriptor);
		fgDebugUIPlugin= this;
	}
		
	protected ILaunchManager getLaunchManager() {
		return DebugPlugin.getDefault().getLaunchManager();
	}
	
	/**
	 * Returns the singleton instance of the debug plugin.
	 */
	public static DebugUIPlugin getDefault() {
		return fgDebugUIPlugin;
	}
	
	/**
	 * Convenience method which returns the unique identifier of this plugin.
	 */
	public static String getUniqueIdentifier() {
		if (getDefault() == null) {
			// If the default instance is not yet initialized,
			// return a static identifier. This identifier must
			// match the plugin id defined in plugin.xml
			return "org.eclipse.debug.ui"; //$NON-NLS-1$
		}
		return getDefault().getDescriptor().getUniqueIdentifier();
	}

	public static IDebugModelPresentation getModelPresentation() {
		if (fgPresentation == null) {
			fgPresentation = new DelegatingModelPresentation();
		}
		return fgPresentation;
	}
	
	public LaunchConfigurationManager getLaunchConfigurationManager() {
		if (fLaunchConfigurationManager == null) {
			fLaunchConfigurationManager = new LaunchConfigurationManager();
		} 
		return fLaunchConfigurationManager;
	}


	
	public static IWorkbenchWindow getActiveWorkbenchWindow() {
		return getDefault().getWorkbench().getActiveWorkbenchWindow();
	}
	
	public static Shell getShell() {
		if (getActiveWorkbenchWindow() != null) {
			return getActiveWorkbenchWindow().getShell();
		}
		return null;
	}

	/**
	 * Returns the default label provider for the debug UI.
	 */
	public static ILabelProvider getDefaultLabelProvider() {
		if (fgDefaultLabelProvider == null) {
			fgDefaultLabelProvider = new DefaultLabelProvider();
		}
		return fgDefaultLabelProvider;
	}

	/**
	 * Creates an extension.  If the extension plugin has not
	 * been loaded a busy cursor will be activated during the duration of
	 * the load.
	 *
	 * @param element the config element defining the extension
	 * @param classAttribute the name of the attribute carrying the class
	 * @return the extension object
	 */
	public static Object createExtension(final IConfigurationElement element, final String classAttribute) throws CoreException {
		// If plugin has been loaded create extension.
		// Otherwise, show busy cursor then create extension.
		IPluginDescriptor plugin = element.getDeclaringExtension().getDeclaringPluginDescriptor();
		if (plugin.isPluginActivated()) {
			return element.createExecutableExtension(classAttribute);
		} else {
			final Object [] ret = new Object[1];
			final CoreException [] exc = new CoreException[1];
			BusyIndicator.showWhile(null, new Runnable() {
				public void run() {
					try {
						ret[0] = element.createExecutableExtension(classAttribute);
					} catch (CoreException e) {
						exc[0] = e;
					}
				}
			});
			if (exc[0] != null) {
				throw exc[0];
			}
			else {
				return ret[0];
			}
		}
	}	
	
	protected ImageRegistry createImageRegistry() {
		return DebugPluginImages.initializeImageRegistry();
	}

	/**
	 * Shuts down this plug-in and discards all plug-in state.
	 * If a plug-in has been started, this method is automatically
	 * invoked by the platform core when the workbench is closed.
	 * <p> 
	 * This method is intended to perform simple termination
	 * of the plug-in environment. The platform may terminate invocations
	 * that do not complete in a timely fashion.
	 * </p><p>
	 * By default this will save the preference and dialog stores (if they are in use).
	 * </p><p>
	 * Subclasses which override this method must call super first.
	 * </p>
	 */
	public void shutdown() throws CoreException {
		if (fPerspectiveManager != null) {
			fPerspectiveManager.shutdown();
		}
		if (DebugActionGroupsManager.defaultExists()) {
			DebugActionGroupsManager.getDefault().shutdown();
		}
		if (fLaunchConfigurationManager != null) {
			fLaunchConfigurationManager.shutdown();
		}
		if (fConsoleDocumentManager != null) {
			fConsoleDocumentManager.shutdown();
		}
		
		ColorManager.getDefault().dispose();
		if (fgPresentation != null) {
			fgPresentation.dispose();
		}
		
		super.shutdown();
	}

	/**
	 * @see AbstractUIPlugin#startup()
	 */
	public void startup() throws CoreException {
		super.startup();
		
		// Listen to launches to lazily create "launch processors"
		DebugPlugin.getDefault().getLaunchManager().addLaunchListener(this);
		
		IAdapterManager manager= Platform.getAdapterManager();
		DebugUIPropertiesAdapterFactory propertiesFactory = new DebugUIPropertiesAdapterFactory();
		manager.registerAdapters(propertiesFactory, IDebugElement.class);
		manager.registerAdapters(propertiesFactory, IProcess.class);
		DebugUIAdapterFactory uiFactory = new DebugUIAdapterFactory();
		manager.registerAdapters(uiFactory, ILaunchConfiguration.class);
		manager.registerAdapters(uiFactory, ILaunchConfigurationType.class);
		getStandardDisplay().asyncExec(
			new Runnable() {
				public void run() {
					//initialize the selected resource manager
					SelectedResourceManager.getDefault();
				}
			});	
	}

	/**
	 * @see AbstractUIPlugin#initializeDefaultPreferences
	 */
	protected void initializeDefaultPreferences(IPreferenceStore prefs) {
		//Debug PreferencePage
		prefs.setDefault(IDebugUIConstants.PREF_BUILD_BEFORE_LAUNCH, true);
		prefs.setDefault(IDebugUIConstants.PREF_SAVE_DIRTY_EDITORS_BEFORE_LAUNCH_RADIO, IDebugUIConstants.PREF_PROMPT_SAVE_DIRTY_EDITORS_BEFORE_LAUNCH);
		prefs.setDefault(IDebugUIConstants.PREF_SHOW_DEBUG_PERSPECTIVE_DEFAULT, IDebugUIConstants.ID_DEBUG_PERSPECTIVE);
		prefs.setDefault(IDebugUIConstants.PREF_SHOW_RUN_PERSPECTIVE_DEFAULT, IDebugUIConstants.PERSPECTIVE_NONE);
		prefs.setDefault(IDebugUIConstants.PREF_AUTO_REMOVE_OLD_LAUNCHES, false);
		prefs.setDefault(IDebugUIConstants.PREF_ACTIVATE_WORKBENCH, true);
		prefs.setDefault(IDebugUIConstants.PREF_WAIT_FOR_BUILD, AlwaysNeverDialog.PROMPT);
		prefs.setDefault(IDebugUIConstants.PREF_REUSE_EDITOR, true);
		
		//ConsolePreferencePage
		prefs.setDefault(IDebugPreferenceConstants.CONSOLE_WRAP, false);
		prefs.setDefault(IDebugPreferenceConstants.CONSOLE_WIDTH, 80);
		prefs.setDefault(IDebugPreferenceConstants.CONSOLE_OPEN_ON_OUT, true);
		prefs.setDefault(IDebugPreferenceConstants.CONSOLE_OPEN_ON_ERR, true);
		prefs.setDefault(IDebugPreferenceConstants.CONSOLE_LIMIT_CONSOLE_OUTPUT, true);
		prefs.setDefault(IDebugPreferenceConstants.CONSOLE_LOW_WATER_MARK, 80000);
		prefs.setDefault(IDebugPreferenceConstants.CONSOLE_HIGH_WATER_MARK, 100000);
		
		PreferenceConverter.setDefault(prefs, IDebugPreferenceConstants.CONSOLE_SYS_OUT_RGB, new RGB(0, 0, 255));
		PreferenceConverter.setDefault(prefs, IDebugPreferenceConstants.CONSOLE_SYS_IN_RGB, new RGB(0, 200, 125));
		PreferenceConverter.setDefault(prefs, IDebugPreferenceConstants.CONSOLE_SYS_ERR_RGB, new RGB(255, 0, 0));
		
		//LaunchHistoryPreferencePage
		prefs.setDefault(IDebugUIConstants.PREF_MAX_HISTORY_SIZE, 10);
		
		//VariableViewsPreferencePage
		prefs.setDefault(IDebugPreferenceConstants.VARIABLES_DETAIL_PANE_ORIENTATION, IDebugPreferenceConstants.VARIABLES_DETAIL_PANE_UNDERNEATH);
		PreferenceConverter.setDefault(prefs, IDebugPreferenceConstants.CHANGED_VARIABLE_RGB, new RGB(255, 0, 0));
		prefs.setDefault(IDebugPreferenceConstants.PREF_DETAIL_PANE_WORD_WRAP, false);
		
		// Variable/Expression view default settings
		prefs.setDefault(IDebugUIConstants.ID_VARIABLE_VIEW + '+' + "org.eclipse.debug.ui.ShowDetailPaneAction", true); //$NON-NLS-1$
		prefs.setDefault(IDebugUIConstants.ID_EXPRESSION_VIEW + '+' + "org.eclipse.debug.ui.ShowDetailPaneAction", true); //$NON-NLS-1$
		prefs.setDefault(IDebugUIConstants.ID_VARIABLE_VIEW + '+' + "org.eclipse.debug.ui.ShowTypeNamesAction", false); //$NON-NLS-1$
		prefs.setDefault(IDebugUIConstants.ID_EXPRESSION_VIEW + '+' + "org.eclipse.debug.ui.ShowTypeNamesAction", false);		 //$NON-NLS-1$
	}

	protected IProcess getProcessFromInput(Object input) {
		IProcess processInput= null;
		if (input instanceof IProcess) {
			processInput= (IProcess) input;
		} else
			if (input instanceof ILaunch) {
				IDebugTarget target= ((ILaunch) input).getDebugTarget();
				if (target != null) {
					processInput= target.getProcess();
				} else {
					IProcess[] processes= ((ILaunch) input).getProcesses();
					if ((processes != null) && (processes.length > 0)) {
						processInput= processes[0];
					}
				}
			} else
				if (input instanceof IDebugElement) {
					processInput= ((IDebugElement) input).getDebugTarget().getProcess();
				}

		return processInput;
	}

	/**
	 * Utility method with conventions
	 */
	public static void errorDialog(Shell shell, String title, String message, IStatus s) {
		// if the 'message' resource string and the IStatus' message are the same,
		// don't show both in the dialog
		if (s != null && message.equals(s.getMessage())) {
			message= null;
		}
		ErrorDialog.openError(shell, title, message, s);
	}
	
	/**
	 * Utility method with conventions
	 */
	public static void errorDialog(Shell shell, String title, String message, Throwable t) {
		IStatus status;
		if (t instanceof CoreException) {
			status= ((CoreException)t).getStatus();
			// if the 'message' resource string and the IStatus' message are the same,
			// don't show both in the dialog
			if (status != null && message.equals(status.getMessage())) {
				message= null;
			}
		} else {
			status= new Status(IStatus.ERROR, getUniqueIdentifier(), IDebugUIConstants.INTERNAL_ERROR, "Error within Debug UI: ", t); //$NON-NLS-1$
			log(status);	
		}
		ErrorDialog.openError(shell, title, message, status);
	}

	/**
	 * Logs the specified status with this plug-in's log.
	 * 
	 * @param status status to log
	 */
	public static void log(IStatus status) {
		getDefault().getLog().log(status);
	}
	
	/**
	 * Logs the specified throwable with this plug-in's log.
	 * 
	 * @param t throwable to log 
	 */
	public static void log(Throwable t) {
		log(newErrorStatus("Error logged from Debug UI: ", t)); //$NON-NLS-1$
	}
	
	/**
	 * Logs the given message if in debug mode.
	 * 
	 * @param String message to log
	 */
	public static void logDebugMessage(String message) {
		if (getDefault().isDebugging()) {
			logErrorMessage(message);
		}
	}
	
	/**
	 * Logs an internal error with the specified message.
	 * 
	 * @param message the error message to log
	 */
	public static void logErrorMessage(String message) {
		// this message is intentionally not internationalized, as an exception may
		// be due to the resource bundle itself
		log(newErrorStatus("Internal message logged from Debug UI: " + message, null)); //$NON-NLS-1$	
	}
	
	/**
	 * Returns a new error status for this plugin with the given message
	 * @param message the message to be included in the status
	 * @param exception the exception to be included in the status or <code>null</code> if none
	 * @return a new error status
	 */
	public static IStatus newErrorStatus(String message, Throwable exception) {
		return new Status(IStatus.ERROR, getUniqueIdentifier(), IDebugUIConstants.INTERNAL_ERROR, message, exception);
	}
	
	/**
	 * Save all dirty editors of all the workbench pages.
	 * Returns whether the operation succeeded.
	 * 
	 * @return whether all saving was completed
	 */
	protected static boolean saveAllPages(boolean confirm) {
		if (getActiveWorkbenchWindow() == null) {
			return false;
		}
		IWorkbench wb = getActiveWorkbenchWindow().getWorkbench();
		IWorkbenchWindow[] windows = wb.getWorkbenchWindows();
		for (int i = 0; i < windows.length; i++) {
			IWorkbenchPage[] pages = windows[i].getPages();
			for (int j = 0; j < pages.length; j++) {
				if (!pages[j].saveAllEditors(confirm)) {
					return false;
				}
			}
		}
		return true;
	}	
	
	/**
	 * Save & build the workspace according to the user-specified preferences.  Return <code>false</code> if
	 * any problems were encountered, <code>true</code> otherwise.
	 */
	public static boolean saveAndBuild() {
		boolean status = true;
		String saveDirty = getDefault().getPreferenceStore().getString(IDebugUIConstants.PREF_SAVE_DIRTY_EDITORS_BEFORE_LAUNCH_RADIO);
		boolean buildBeforeLaunch = getDefault().getPreferenceStore().getBoolean(IDebugUIConstants.PREF_BUILD_BEFORE_LAUNCH);
		boolean autobuilding = ResourcesPlugin.getWorkspace().isAutoBuilding();
		
		// If we're ignoring dirty editors, check if we need to build
		if (saveDirty.equals(IDebugUIConstants.PREF_NEVER_SAVE_DIRTY_EDITORS_BEFORE_LAUNCH)) {
			if (buildBeforeLaunch) {
				return doBuild();
			}
		} else {
			boolean prompt = false;
			if (saveDirty.equals(IDebugUIConstants.PREF_PROMPT_SAVE_DIRTY_EDITORS_BEFORE_LAUNCH)) {
				prompt = true;				
			} 
			status = saveAllPages(prompt);
			if (status && !autobuilding && buildBeforeLaunch) {
				status = doBuild();
			}
		}
				
		return status;
	}
	
	private static boolean doBuild() {
		ProgressMonitorDialog dialog= new ProgressMonitorDialog(getShell());
		try {
			dialog.run(true, true, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException {
					try {
						ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				}
			});
		} catch (InterruptedException e) {
			// cancelled by user
			return false;
		} catch (InvocationTargetException e) {
			String title= DebugUIMessages.getString("DebugUIPlugin.Run/Debug_1"); //$NON-NLS-1$
			String message= DebugUIMessages.getString("DebugUIPlugin.Build_error._Check_log_for_details._2"); //$NON-NLS-1$
			Throwable t = e.getTargetException();
			errorDialog(getShell(), title, message, t);
			return false;
		}
		return true;
	}	
	
	/**
	 * Returns the standard display to be used. The method first checks, if
	 * the thread calling this method has an associated display. If so, this
	 * display is returned. Otherwise the method returns the default display.
	 */
	public static Display getStandardDisplay() {
		Display display= Display.getCurrent();
		if (display == null) {
			display= Display.getDefault();
		}
		return display;		
	}	
	
	/**
	 * Returns the a color based on the type of output.
	 * Valid types:
	 * <li>CONSOLE_SYS_OUT_RGB</li>
	 * <li>CONSOLE_SYS_ERR_RGB</li>
	 * <li>CONSOLE_SYS_IN_RGB</li>
	 * <li>CHANGED_VARIABLE_RGB</li>
	 */
	public static Color getPreferenceColor(String type) {
		return ColorManager.getDefault().getColor(PreferenceConverter.getColor(getDefault().getPreferenceStore(), type));
	}

	/**
	 * Returns the console document manager. The manager will be created lazily on 
	 * the first access.
	 * 
	 * @return ConsoleDocumentManager
	 */
	public  ConsoleDocumentManager getConsoleDocumentManager() {
		if (fConsoleDocumentManager == null) {
			fConsoleDocumentManager = new ConsoleDocumentManager();
		}
		return fConsoleDocumentManager;
	}
	
	/**
	 * Serializes a XML document into a string - encoded in UTF8 format,
	 * with platform line separators.
	 * 
	 * @param doc document to serialize
	 * @return the document as a string
	 * @throws TransformerException if an unrecoverable error occurs during the serialization
	 * @throws IOException if the encoding attempted to be used is not supported
	 */
	public static String serializeDocument(Document doc) throws TransformerException, IOException {
		ByteArrayOutputStream s= new ByteArrayOutputStream();
		
		TransformerFactory factory= TransformerFactory.newInstance();
		
		Transformer transformer= factory.newTransformer();
		transformer.setOutputProperty(OutputKeys.METHOD, "xml"); //$NON-NLS-1$
		transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$
		
		DOMSource source= new DOMSource(doc);
		StreamResult outputTarget= new StreamResult(s);
		transformer.transform(source, outputTarget);
		
		return s.toString("UTF8"); //$NON-NLS-1$			
	}
	
	/**
	 * Returns a Document that can be used to build a DOM tree
	 * @return the Document
	 * @throws ParserConfigurationException if an exception occurs creating the document builder
	 * @since 3.0
	 */
	public static Document getDocument() throws ParserConfigurationException {
		DocumentBuilderFactory dfactory= DocumentBuilderFactory.newInstance();

		DocumentBuilder docBuilder= dfactory.newDocumentBuilder();
		Document doc= docBuilder.newDocument();
		return doc;
	}
	
	/**
	 * Determines and returns the selection in the specified window.  If nothing is
	 * actually selected, look for an active editor.
	 */
	public static IStructuredSelection resolveSelection(IWorkbenchWindow window) {
		if (window == null) {
			return null;
		}
		ISelection selection= window.getSelectionService().getSelection();
		if (selection == null || selection.isEmpty() || !(selection instanceof IStructuredSelection)) {
			// there is no obvious selection - go fishing
			selection= null;
			IWorkbenchPage page= window.getActivePage();
			if (page == null) {
				//workspace is closed
				return null;
			}

			// first, see if there is an active editor, and try its input element
			IEditorPart editor= page.getActiveEditor();
			Object element= null;
			if (editor != null) {
				element= editor.getEditorInput();
			}

			if (selection == null && element != null) {
				selection= new StructuredSelection(element);
			}
		}
		return (IStructuredSelection)selection;
	}
	
	/**
	 * When the first launch is added, instantiate launch processors,
	 * and stop listening to launch notifications.
	 * <p>
	 * Launch processors are:
	 * <ul>
	 * <li>console document manager</li>
	 * <li>perspective manager</li>
	 * </ul>
	 * </p>
	 * @see org.eclipse.debug.core.ILaunchListener#launchAdded(org.eclipse.debug.core.ILaunch)
	 */
	public void launchAdded(ILaunch launch) {
		DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(this);
		getConsoleDocumentManager().startup();
		
		if (fPerspectiveManager == null) {
			PerspectiveManager manager = getPerspectiveManager();
			manager.launchAdded(launch);
		}
		
		getLaunchConfigurationManager().startup();
	}
	
	/**
	 * Returns the persepective manager - instantiating it if required.
	 * 
	 * @return
	 */
	public PerspectiveManager getPerspectiveManager() {
		if (fPerspectiveManager == null) {
			fPerspectiveManager = new PerspectiveManager();
			fPerspectiveManager.startup();			
		}
		return fPerspectiveManager;
	}

	/**
	 * @see org.eclipse.debug.core.ILaunchListener#launchChanged(org.eclipse.debug.core.ILaunch)
	 */
	public void launchChanged(ILaunch launch) {
	}

	/**
	 * @see org.eclipse.debug.core.ILaunchListener#launchRemoved(org.eclipse.debug.core.ILaunch)
	 */
	public void launchRemoved(ILaunch launch) {
	}

	/**
	 * Save dirty editors before launching, according to preferences.
	 * 
	 * @return whether to proceed with launch 
	 */
	public static boolean preLaunchSave() {
		String saveDirty = getDefault().getPreferenceStore().getString(IDebugUIConstants.PREF_SAVE_DIRTY_EDITORS_BEFORE_LAUNCH_RADIO);
		if (saveDirty.equals(IDebugUIConstants.PREF_NEVER_SAVE_DIRTY_EDITORS_BEFORE_LAUNCH)) {
			return true;
		} else {
			return saveAllPages(saveDirty.equals(IDebugUIConstants.PREF_PROMPT_SAVE_DIRTY_EDITORS_BEFORE_LAUNCH));
		}
	}
	
	/**
	 * Builds the workspace (according to preferences) and launches the given launch
	 * configuration in the specified mode.
	 * 
	 * @param configuration the configuration to launch
	 * @param mode launch mode - run or debug
	 * @param monitor progress monitor
	 * @exception CoreException if an exception occurs while building or launching
	 * @return resulting launch
	 */
	public static ILaunch buildAndLaunch(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor) throws CoreException {
		boolean buildBeforeLaunch = getDefault().getPreferenceStore().getBoolean(IDebugUIConstants.PREF_BUILD_BEFORE_LAUNCH);
		boolean autobuilding = ResourcesPlugin.getWorkspace().isAutoBuilding();
		IProgressMonitor subMonitor = monitor;
		String message = MessageFormat.format("{0}...", new String[]{configuration.getName()}); //$NON-NLS-1$
		if (!autobuilding && buildBeforeLaunch) {
			monitor.beginTask(message, 200);
			subMonitor = new SubProgressMonitor(monitor, 100);
			ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, subMonitor);
			subMonitor = new SubProgressMonitor(monitor, 100);
		} else {
			Job[] build = Platform.getJobManager().find(ResourcesPlugin.FAMILY_AUTO_BUILD); 
			while (build.length > 0) {
				boolean join= waitForBuild(build[0].getState(), buildBeforeLaunch);
				if (join) {
					try {
						subMonitor = new SubProgressMonitor(monitor, 100);
						subMonitor.beginTask(DebugUIMessages.getString("DebugUIPlugin.14"), 100); //$NON-NLS-1$
						build[0].join();
					} catch (InterruptedException e) {
						throw new CoreException(new Status(IStatus.ERROR, DebugUIPlugin.getUniqueIdentifier(), IStatus.ERROR, DebugUIMessages.getString("DebugUIPlugin.15"), e)); //$NON-NLS-1$
					}
				}
				build= Platform.getJobManager().find(ResourcesPlugin.FAMILY_AUTO_BUILD);
			}
			subMonitor.beginTask(message, 100);
		}
		return configuration.launch(mode, subMonitor);
	}
	
	/**
	 * Returns whether a launch should wait for a build to finish before proceeding. If
	 * a build is running and the user has asked to build before launching, always wait.
	 * If a build is running and the user hasn't specified that they want to build before
	 * launching, warn them (because the workspace state is changing) before proceeding.
	 * If the user has asked to build before launching and a build is waiting, also ask them
	 * because the build won't begin for an indeterminite amount of time.
	 * 
	 * @param buildState the state of the build job
	 * @param buildBeforeLaunch whether or not the user has specified that they want to build
	 *  before launching
	 * @return whether a launch should wait for a build to finish before proceeding
	 */
	private static boolean waitForBuild(final int buildState, final boolean buildBeforeLaunch) {
		if (buildState == Job.RUNNING && buildBeforeLaunch) {
			return true;
		}
		String waitPreference= DebugUIPlugin.getDefault().getPreferenceStore().getString(IDebugUIConstants.PREF_WAIT_FOR_BUILD);
		if (AlwaysNeverDialog.ALWAYS.equals(waitPreference)) {
			return true;
		} else if (AlwaysNeverDialog.NEVER.equals(waitPreference)) {
			return false;
		}
		final boolean[] wait= new boolean[1];
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				String message= null;
				if (buildState == Job.RUNNING) {
					message= DebugUIMessages.getString("DebugUIPlugin.16"); //$NON-NLS-1$
				} else if (buildState == Job.WAITING) {
					message= DebugUIMessages.getString("DebugUIPlugin.17"); //$NON-NLS-1$
				}
				if (message != null) {
					wait[0]= AlwaysNeverDialog.openQuestion(getShell(), DebugUIMessages.getString("DebugUIPlugin.18"), message, IDebugUIConstants.PREF_WAIT_FOR_BUILD, DebugUIPlugin.getDefault().getPreferenceStore()); //$NON-NLS-1$
				} else {
					// Unknown or sleeping job. Don't wait.
					wait[0]= false;
				}
			}
		});
		return wait[0];
	}
}

