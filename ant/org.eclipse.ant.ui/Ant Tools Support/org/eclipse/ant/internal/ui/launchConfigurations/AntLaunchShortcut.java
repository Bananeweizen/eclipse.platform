/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     John-Mason P. Shackelford - bug 34548
 *******************************************************************************/
package org.eclipse.ant.internal.ui.launchConfigurations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.ant.internal.ui.AntUtil;
import org.eclipse.ant.internal.ui.IAntUIConstants;
import org.eclipse.ant.internal.ui.IAntUIPreferenceConstants;
import org.eclipse.ant.internal.ui.model.AntElementNode;
import org.eclipse.ant.internal.ui.model.AntProjectNode;
import org.eclipse.ant.internal.ui.model.AntTargetNode;
import org.eclipse.ant.internal.ui.model.AntTaskNode;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.editors.text.ILocationProvider;
import org.eclipse.ui.externaltools.internal.launchConfigurations.ExternalToolsUtil;
import org.eclipse.ui.externaltools.internal.model.IExternalToolConstants;

import com.ibm.icu.text.MessageFormat;

/**
 * This class provides the Run/Debug As -> Ant Build launch shortcut.
 * 
 */
public class AntLaunchShortcut implements ILaunchShortcut {

	private boolean fShowDialog= false;
	private static final int MAX_TARGET_APPEND_LENGTH = 30;

	/**
	 * @see org.eclipse.debug.ui.ILaunchShortcut#launch(org.eclipse.jface.viewers.ISelection, java.lang.String)
	 */
	public void launch(ISelection selection, String mode) {
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection)selection;
			Object object = structuredSelection.getFirstElement();
			if (object instanceof IAdaptable) {
				IResource resource = (IResource)((IAdaptable)object).getAdapter(IResource.class);
				if (resource != null) {
					if (!("xml".equalsIgnoreCase(resource.getFileExtension()))) { //$NON-NLS-1$
						if (resource.getType() == IResource.FILE) {
							resource = resource.getParent();
						}
						resource = findBuildFile((IContainer)resource);
					} 
					if (resource != null) {
						launch(((IFile)resource).getLocation(), mode, null);
						return;
					}
				} else if (object instanceof AntElementNode){
					launch((AntElementNode) object, mode);
					return;
				}
			}
		}
		antFileNotFound();
	}
	
	/**
	 * Launches the given Ant node.
	 * <ul><li>AntProjectNodes: the default target is executed</li>
	 * <li>AntTargetNodes: that target is executed</li>
	 * <li>AntTaskNodes: the owning target is executed</li>
	 * </ul>
	 * @param node the Ant node to use as the context for the launch
     * @param mode the mode of the launch
	 */
	public void launch(AntElementNode node, String mode) {
		String selectedTargetName= null;
		if (node instanceof AntTargetNode) {
			AntTargetNode targetNode= (AntTargetNode) node;
			if (targetNode.isDefaultTarget()) {
				selectedTargetName= ""; //$NON-NLS-1$
			} else {
				selectedTargetName= targetNode.getTarget().getName();
			}
		} else if (node instanceof AntProjectNode) {
			selectedTargetName = ""; //$NON-NLS-1$
		} else if (node instanceof AntTaskNode) {
		    AntTaskNode taskNode= (AntTaskNode) node;
		    selectedTargetName= taskNode.getTask().getOwningTarget().getName();
		}
	
		if (selectedTargetName == null) {
			return;
		}
		IFile file = node.getBuildFileResource();
		if (file != null) {
			launch(file.getLocation(), mode, selectedTargetName);
			return;
		} 
		//external buildfile
		IWorkbenchPage page = AntUIPlugin.getActiveWorkbenchWindow().getActivePage();
		IPath filePath = null;
		IEditorPart editor= page.getActiveEditor();
		if (editor != null) {
		    IEditorInput editorInput = editor.getEditorInput();
		    ILocationProvider locationProvider= (ILocationProvider)editorInput.getAdapter(ILocationProvider.class);
		    if (locationProvider != null) {
		        filePath= locationProvider.getPath(editorInput);
		        if (filePath != null) {
					launch(filePath, mode, selectedTargetName);
					return;
				}
		    }
		}
		antFileNotFound();
	}
	
	/**
	 * Inform the user that an ant file was not found to run.
	 */
	private void antFileNotFound() {
		reportError(AntLaunchConfigurationMessages.AntLaunchShortcut_Unable, null);	
	}

	/**
	 * Returns a listing of <code>ILaunchConfiguration</code>s that correspond to the specified target name.
	 * Passing in the empty string will produce a listing of <code>ILaunchConfiguration</code>s matching the default target.
	 * 
	 * @param filepath the path to the buildfile to launch
	 * @param targetname the name of the target to launch the build on
	 * @return the list of <code>ILaunchConfiguration</code>s that correspond to the specified target name.
	 * 
	 * @since 3.4
	 */
	protected List collectConfigurations(IPath filepath, String targetname) {
		ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType type = manager.getLaunchConfigurationType(IAntLaunchConfigurationConstants.ID_ANT_LAUNCH_CONFIGURATION_TYPE);
		if(type != null) {
			try {
				ILaunchConfiguration[] configs = manager.getLaunchConfigurations(type);
				ArrayList list = new ArrayList();
				String targetattr = null;
				String[] targets = null;
				IPath location = null;
				for(int i = 0; i < configs.length; i++) {
					if(configs[i].exists()) {
						location = ExternalToolsUtil.getLocation(configs[i]);
						if(location != null && location.equals(filepath)) {
							targetattr = configs[i].getAttribute(IAntLaunchConfigurationConstants.ATTR_ANT_TARGETS, ""); //$NON-NLS-1$
							targets = AntUtil.parseString(targetattr, ","); //$NON-NLS-1$
							if(targets.length == 0) {
								if(targetattr.equals(targetname) || targetname == null) {
									list.add(configs[i]);
								}
							} else {
								if(Arrays.asList(targets).contains(targetname)) {
									list.add(configs[i]);
								}
							}
						}
					}
				}
				return list;
			} catch (CoreException e) {}
		}
		return new ArrayList();
	}
	
	/**
	 * Returns a unique name for a copy of the given launch configuration with the given
	 * targets. The name seed is the same as the name for a new launch configuration with
	 *   &quot; [targetList]&quot; appended to the end.
	 * @param filePath the path to the buildfile
     * @param projectName the name of the project containing the buildfile or <code>null</code> if no project is known
	 * @param targetAttribute the listing of targets to execute or <code>null</code> for default target execution
	 * @return a unique name for the copy
	 */
	public static String getNewLaunchConfigurationName(IPath filePath, String projectName, String targetAttribute) {
		StringBuffer buffer = new StringBuffer();
		if (projectName != null) {
			buffer.append(projectName);
			buffer.append(' ');
			buffer.append(filePath.lastSegment());
		} else {
			buffer.append(filePath.lastSegment());
		}
		
		if (targetAttribute != null) {
			buffer.append(" ["); //$NON-NLS-1$
			if (targetAttribute.length() > MAX_TARGET_APPEND_LENGTH + 3) {
				// The target attribute can potentially be a long, comma-separated list
				// of target. Make sure the generated name isn't extremely long.
				buffer.append(targetAttribute.substring(0, MAX_TARGET_APPEND_LENGTH));
				buffer.append("..."); //$NON-NLS-1$
			} else {
				buffer.append(targetAttribute);
			}
			buffer.append(']');
		}
		
		String name= DebugPlugin.getDefault().getLaunchManager().generateUniqueLaunchConfigurationNameFrom(buffer.toString());
		return name;
	}
	
	/**
	 * Launch the given targets in the given build file. The targets are
	 * launched in the given mode.
	 * 
	 * @param filePath the path to the build file to launch
	 * @param mode the mode in which the build file should be executed
	 * @param targetAttribute the targets to launch, in the form of the launch
	 * configuration targets attribute.
	 */
	public void launch(IPath filePath, String mode, String targetAttribute) {
		ILaunchConfiguration configuration = null;
		List configs = collectConfigurations(filePath, targetAttribute);
		if (configs.isEmpty()) {
			IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(filePath);
			configuration = createDefaultLaunchConfiguration(filePath, (resource == null ? null : resource.getProject()));
			try {
				if (targetAttribute != null && ! targetAttribute.equals(configuration.getAttribute(IAntLaunchConfigurationConstants.ATTR_ANT_TARGETS, ""))) { //$NON-NLS-1$
					String projectName = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, (String) null);
					String newName = getNewLaunchConfigurationName(filePath, projectName, targetAttribute);	
					ILaunchConfigurationWorkingCopy copy = configuration.getWorkingCopy();
					copy.rename(newName);
					copy.setAttribute(IAntLaunchConfigurationConstants.ATTR_ANT_TARGETS, targetAttribute);
					configuration = copy.doSave();
				}
			} catch (CoreException exception) {
				reportError(MessageFormat.format(AntLaunchConfigurationMessages.AntLaunchShortcut_Exception_launching, new String[] {filePath.toFile().getName()}), exception);
				return;
			}
		} else if (configs.size() == 1) {
				configuration= (ILaunchConfiguration)configs.get(0);
		} else {
			configuration = chooseConfig(configs);
			if(configuration == null) {
				//fail gracefully if the user cancels choosing a configuration
				return;
			}
		}
		if (configuration != null) {
			launch(mode, configuration);
		}
		else {
			antFileNotFound();
		}
	}
	
	/**
	 * Delegate method to launch the specified <code>ILaunchConfiguration</code> in the specified mode
	 * @param mode the mode to launch in
	 * @param configuration the <code>ILaunchConfiguration</code> to launch
	 */
	private void launch(String mode, ILaunchConfiguration configuration) {
        if (fShowDialog) {
			// Offer to save dirty editors before opening the dialog as the contents
			// of an Ant editor often affect the contents of the dialog.
			if (!DebugUITools.saveBeforeLaunch()) {
				return;
			}
			IStatus status = new Status(IStatus.INFO, IAntUIConstants.PLUGIN_ID, IAntUIConstants.STATUS_INIT_RUN_ANT, "", null); //$NON-NLS-1$
			String groupId;
			if (mode.equals(ILaunchManager.DEBUG_MODE)) {
			    groupId= IDebugUIConstants.ID_DEBUG_LAUNCH_GROUP;
			} else {
			    groupId= IExternalToolConstants.ID_EXTERNAL_TOOLS_LAUNCH_GROUP;
			}
			DebugUITools.openLaunchConfigurationDialog(AntUIPlugin.getActiveWorkbenchWindow().getShell(), configuration, groupId, status);
		} else {
			DebugUITools.launch(configuration, mode);
		}
    }

    /**
	 * Walks the file hierarchy looking for a build file.
	 * Returns the first build file found that matches the 
	 * search criteria.
	 */
	private IFile findBuildFile(IContainer parent) {
		String[] names= getBuildFileNames();
		if (names == null) {
			return null;
		}
		IResource file= null;
		while (file == null || file.getType() != IResource.FILE) {		
			for (int i = 0; i < names.length; i++) {
				String string = names[i];
				file= parent.findMember(string);
				if (file != null && file.getType() == IResource.FILE) {
					break;
				}
			}
			parent = parent.getParent();
			if (parent == null) {
				return null;
			}
		}
		return (IFile)file;
	}
	
	/**
	 * Returns an array of build file names from the ant preference store
	 * @return an array of build file names
	 */
	private String[] getBuildFileNames() {
		IPreferenceStore prefs= AntUIPlugin.getDefault().getPreferenceStore();
		String buildFileNames= prefs.getString(IAntUIPreferenceConstants.ANT_FIND_BUILD_FILE_NAMES);
		if (buildFileNames.length() == 0) {
			//the user has not specified any names to look for
			return null;
		}
		return AntUtil.parseString(buildFileNames, ","); //$NON-NLS-1$
	}
	
	/**
	 * Creates and returns a default launch configuration for the given file.
	 * 
	 * @param file
	 * @return default launch configuration
	 */
	public static ILaunchConfiguration createDefaultLaunchConfiguration(IFile file) {
		return createDefaultLaunchConfiguration(file.getFullPath(), file.getProject());
	}

	/**
	 * Creates and returns a default launch configuration for the given file path
	 * and project.
	 * 
	 * @param filePath the path to the buildfile
	 * @param project the project containing the buildfile or <code>null</code> if the
	 * buildfile is not contained in a project (is external).
	 * @return default launch configuration or <code>null</code> if one could not be created
	 */
	public static ILaunchConfiguration createDefaultLaunchConfiguration(IPath filePath, IProject project) {
		ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType type = manager.getLaunchConfigurationType(IAntLaunchConfigurationConstants.ID_ANT_LAUNCH_CONFIGURATION_TYPE);
				
		String projectName= project != null ? project.getName() : null;
		String name = getNewLaunchConfigurationName(filePath, projectName, null);
		try {
			ILaunchConfigurationWorkingCopy workingCopy = type.newInstance(null, name);
			if (project != null) {
				workingCopy.setAttribute(IExternalToolConstants.ATTR_LOCATION,
						VariablesPlugin.getDefault().getStringVariableManager().generateVariableExpression("workspace_loc", filePath.toString())); //$NON-NLS-1$
			} else {
				workingCopy.setAttribute(IExternalToolConstants.ATTR_LOCATION, filePath.toString());
			}
			workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CLASSPATH_PROVIDER, "org.eclipse.ant.ui.AntClasspathProvider"); //$NON-NLS-1$
			// set default for common settings
			CommonTab tab = new CommonTab();
			tab.setDefaults(workingCopy);
			tab.dispose();
			
			//set the project name so that the correct default VM install can be determined
			if (project != null) {
				workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, project.getName());
			}
			AntJRETab jreTab = new AntJRETab();
			jreTab.setDefaults(workingCopy);
			jreTab.dispose();
			
			IFile file= AntUtil.getFileForLocation(filePath.toString(), null);
			workingCopy.setMappedResources(new IResource[] {file});
			
			return workingCopy.doSave();
		} catch (CoreException e) {
			reportError(MessageFormat.format(AntLaunchConfigurationMessages.AntLaunchShortcut_2, new String[]{filePath.toString()}), e);
		}
		return null;
	}
	
	/**
	 * Returns a list of existing launch configuration for the given file.
	 * 
	 * @param file the buildfile resource
	 * @return list of launch configurations
	 */
	public static List findExistingLaunchConfigurations(IFile file) {
		List validConfigs= new ArrayList();
		if(file != null) {
			IPath filePath = file.getLocation();
			ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
			ILaunchConfigurationType type = manager.getLaunchConfigurationType(IAntLaunchConfigurationConstants.ID_ANT_LAUNCH_CONFIGURATION_TYPE);
			if (type != null) {
				ILaunchConfiguration[] configs = null;
				try {
					configs = manager.getLaunchConfigurations(type);
					if (configs != null && configs.length > 0 && filePath != null) {
						for (int i = 0; i < configs.length; i++) {
							ILaunchConfiguration configuration = configs[i];
							IPath location;
							location = ExternalToolsUtil.getLocation(configuration);
							if (filePath.equals(location)) {
								validConfigs.add(configuration);
							}
						}
					}
					else {
						reportError(AntLaunchConfigurationMessages.AntLaunchShortcut_0, null);
					}
				} catch (CoreException e) {
					reportError(AntLaunchConfigurationMessages.AntLaunchShortcut_3, e);
				}
				
			}
		}
		return validConfigs;
	}
	
	/**
	 * Prompts the user to choose from the list of given launch configurations
	 * and returns the config the user choose or <code>null</code> if the user
	 * pressed Cancel or if the given list is empty.
	 */
	public static ILaunchConfiguration chooseConfig(List configs) {
		if (configs.isEmpty()) {
			return null;
		}
		ILabelProvider labelProvider = DebugUITools.newDebugModelPresentation();
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(Display.getDefault().getActiveShell(), labelProvider);
		dialog.setElements(configs.toArray(new ILaunchConfiguration[configs.size()]));
		dialog.setTitle(AntLaunchConfigurationMessages.AntLaunchShortcut_4);
		dialog.setMessage(AntLaunchConfigurationMessages.AntLaunchShortcut_5);
		dialog.setMultipleSelection(false);
		int result = dialog.open();
		labelProvider.dispose();
		if (result == Window.OK) {
			return (ILaunchConfiguration) dialog.getFirstResult();
		}
		return null;
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchShortcut#launch(org.eclipse.ui.IEditorPart, java.lang.String)
	 */
	public void launch(IEditorPart editor, String mode) {
		IEditorInput input = editor.getEditorInput();
		IFile file = (IFile)input.getAdapter(IFile.class);
		IPath filepath = null;
		if (file != null) {
			filepath = file.getLocation();
		}
		if(filepath == null) {
		    ILocationProvider locationProvider= (ILocationProvider)input.getAdapter(ILocationProvider.class);
		    if (locationProvider != null) {
				filepath = locationProvider.getPath(input);
			}
		}
		if(filepath != null && "xml".equals(filepath.getFileExtension())) { //$NON-NLS-1$
			launch(filepath, mode, null);
			return;
		}
		antFileNotFound();
	}
	
	/**
	 * Opens an error dialog presenting the user with the specified message and throwable
	 * @param message
	 * @param throwable
	 */
	protected static void reportError(String message, Throwable throwable) {
		IStatus status = null;
		if (throwable instanceof CoreException) {
			status = ((CoreException)throwable).getStatus();
		} else {
			status = new Status(IStatus.ERROR, IAntUIConstants.PLUGIN_ID, 0, message, throwable);
		}
		ErrorDialog.openError(AntUIPlugin.getActiveWorkbenchWindow().getShell(), AntLaunchConfigurationMessages.AntLaunchShortcut_Error_7, AntLaunchConfigurationMessages.AntLaunchShortcut_Build_Failed_2, status); 
	}

	/**
	 * Sets whether to show the external tools launch configuration dialog
	 * 
	 * @param showDialog If true the launch configuration dialog will always be
	 * 			shown
	 */
	public void setShowDialog(boolean showDialog) {
		fShowDialog = showDialog;
	}
}
