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
package org.eclipse.debug.internal.core;

 
import java.io.IOException;
import java.io.StringReader;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xerces.dom.DocumentImpl;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.debug.core.model.IPersistableSourceLocator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Launch configuration handle.
 * 
 * @see ILaunchConfiguration
 */
public class LaunchConfiguration extends PlatformObject implements ILaunchConfiguration {
	
	/**
	 * Location this configuration is stored in. This 
	 * is the key for a launch configuration handle.
	 */
	private IPath fLocation;
	
	/**
	 * Constructs a launch configuration in the given location.
	 * 
	 * @param location path to where this launch configuration's
	 *  underlying file is located
	 */
	protected LaunchConfiguration(IPath location) {
		setLocation(location);
	}
	
	/**
	 * Constructs a launch configuration from the given
	 * memento.
	 * 
	 * @param memento launch configuration memento
	 * @exception CoreException if the memento is invalid or
	 * 	an exception occurrs reading the memento
	 */
	protected LaunchConfiguration(String memento) throws CoreException {
		Exception ex = null;
		try {
			Element root = null;
			DocumentBuilder parser =
				DocumentBuilderFactory.newInstance().newDocumentBuilder();
			StringReader reader = new StringReader(memento);
			InputSource source = new InputSource(reader);
			root = parser.parse(source).getDocumentElement();
			
			String localString = root.getAttribute("local"); //$NON-NLS-1$
			String path = root.getAttribute("path"); //$NON-NLS-1$

			String message = null;				
			if (path == null) {
				message = DebugCoreMessages.getString("LaunchConfiguration.Invalid_launch_configuration_memento__missing_path_attribute_3"); //$NON-NLS-1$
			} else if (localString == null) {
				message = DebugCoreMessages.getString("LaunchConfiguration.Invalid_launch_configuration_memento__missing_local_attribute_4"); //$NON-NLS-1$
			}
			if (message != null) {
				IStatus s = newStatus(message, DebugException.INTERNAL_ERROR, null);
				throw new CoreException(s);
			}
			
			IPath location = null;
			boolean local = (Boolean.valueOf(localString)).booleanValue();
			if (local) {
				location = LaunchManager.LOCAL_LAUNCH_CONFIGURATION_CONTAINER_PATH.append(path);
			} else {
				location = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(path)).getLocation();
			}
			setLocation(location);
			if (location == null) {
				IStatus s = newStatus(MessageFormat.format(DebugCoreMessages.getString("LaunchConfiguration.Unable_to_restore_location_for_launch_configuration_from_memento__{0}_1"), new String[]{path}), DebugPlugin.INTERNAL_ERROR, null); //$NON-NLS-1$
				throw new CoreException(s);
			}
			return;
		} catch (ParserConfigurationException e) {
			ex = e;			
		} catch (SAXException e) {
			ex = e;
		} catch (IOException e) {
			ex = e;
		}
		IStatus s = newStatus(DebugCoreMessages.getString("LaunchConfiguration.Exception_occurred_parsing_memento_5"), DebugException.INTERNAL_ERROR, ex); //$NON-NLS-1$
		throw new CoreException(s);
	}
	
	/**
	 * Creates and returns a new error status based on 
	 * the given message, code, and exception.
	 * 
	 * @param message error message
	 * @param code error code
	 * @param e exception or <code>null</code>
	 * @return status
	 */
	protected IStatus newStatus(String message, int code, Throwable e) {
		return new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), code, message, e);
	}
	
	/**
	 * @see ILaunchConfiguration#launch(String, IProgressMonitor)
	 */
	public ILaunch launch(final String mode, IProgressMonitor monitor) throws CoreException {
		// bug 28245 - force the delegate to load in case it is interested in launch notifications
		final ILaunchConfigurationDelegate delegate= getDelegate(mode);
		
		final ILaunch launch = new Launch(this, mode, null);
		getLaunchManager().addLaunch(launch);
		Job job= new Job(MessageFormat.format(DebugCoreMessages.getString("LaunchConfiguration.12"), new String[] { getName() })) { //$NON-NLS-1$
			public IStatus run(IProgressMonitor monitor) {
				try {
					initializeSourceLocator(launch);
					delegate.launch(LaunchConfiguration.this, mode, launch, monitor);
				} catch (CoreException e) {
					// if there was an exception, and the launch is empty, remove it
					if (!launch.hasChildren()) {
						getLaunchManager().removeLaunch(launch);
					}
					return e.getStatus();
				}
				if (monitor.isCanceled()) {
					getLaunchManager().removeLaunch(launch);
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();

		return launch;
	}
	
	/**
	 * Set the source locator to use with the launch, if specified 
	 * by this configuration.
	 * 
	 * @param launch the launch on which to set the source locator
	 */
	protected void initializeSourceLocator(ILaunch launch) throws CoreException {
		if (launch.getSourceLocator() == null) {
			String type = getAttribute(ATTR_SOURCE_LOCATOR_ID, (String)null);
			if (type != null) {
				IPersistableSourceLocator locator = getLaunchManager().newSourceLocator(type);
				String memento = getAttribute(ATTR_SOURCE_LOCATOR_MEMENTO, (String)null);
				if (memento == null) {
					locator.initializeDefaults(this);
				} else {
					locator.initializeFromMemento(memento);
				}
				launch.setSourceLocator(locator);
			}
		}
	}

	/**
	 * @see ILaunchConfiguration#supportsMode(String)
	 */
	public boolean supportsMode(String mode) throws CoreException {
		return getType().supportsMode(mode);
	}

	/**
	 * A configuration's name is that of the last segment
	 * in it's location (subtract the ".launch" extension).
	 * 
	 * @see ILaunchConfiguration#getName()
	 */
	public String getName() {
		return getLastLocationSegment();
	}
	
	private String getLastLocationSegment() {
		String name = getLocation().lastSegment();
		name = name.substring(0, name.length() - (LAUNCH_CONFIGURATION_FILE_EXTENSION.length() + 1));
		return name;
	}

	/**
	 * @see ILaunchConfiguration#getLocation()
	 */
	public IPath getLocation() {
		return fLocation;
	}

	/**
	 * Sets the location of this configuration's underlying
	 * file.
	 * 
	 * @param location the location of this configuration's underlying
	 *  file
	 */
	private void setLocation(IPath location) {
		fLocation = location;
	}

	/**
	 * @see ILaunchConfiguration#exists()
	 */
	public boolean exists() {
		return getLocation().toFile().exists();
	}

	/**
	 * @see ILaunchConfiguration#getAttribute(String, int)
	 */
	public int getAttribute(String attributeName, int defaultValue) throws CoreException {
		return getInfo().getIntAttribute(attributeName, defaultValue);
	}

	/**
	 * @see ILaunchConfiguration#getAttribute(String, String)
	 */
	public String getAttribute(String attributeName, String defaultValue) throws CoreException {
		return getInfo().getStringAttribute(attributeName, defaultValue);
	}

	/**
	 * @see ILaunchConfiguration#getAttribute(String, boolean)
	 */
	public boolean getAttribute(String attributeName, boolean defaultValue) throws CoreException {
		return getInfo().getBooleanAttribute(attributeName, defaultValue);
	}

	/**
	 * @see ILaunchConfiguration#getAttribute(String, List)
	 */
	public List getAttribute(String attributeName, List defaultValue) throws CoreException {
		return getInfo().getListAttribute(attributeName, defaultValue);
	}

	/**
	 * @see ILaunchConfiguration#getAttribute(String, Map)
	 */
	public Map getAttribute(String attributeName, Map defaultValue) throws CoreException {
		return getInfo().getMapAttribute(attributeName, defaultValue);
	}

	/**
	 * @see ILaunchConfiguration#getType()
	 */
	public ILaunchConfigurationType getType() throws CoreException {
		return getInfo().getType();
	}

	/**
	 * @see ILaunchConfiguration#isLocal()
	 */
	public boolean isLocal() {
		IPath localPath = LaunchManager.LOCAL_LAUNCH_CONFIGURATION_CONTAINER_PATH;
		return localPath.isPrefixOf(getLocation());
	}

	/**
	 * @see ILaunchConfiguration#getWorkingCopy()
	 */
	public ILaunchConfigurationWorkingCopy getWorkingCopy() throws CoreException {
		return new LaunchConfigurationWorkingCopy(this);
	}
	
	/**
	 * @see ILaunchConfiguration#copy(String name)
	 */
	public ILaunchConfigurationWorkingCopy copy(String name) throws CoreException {
		ILaunchConfigurationWorkingCopy copy = new LaunchConfigurationWorkingCopy(this, name);
		return copy;
	}	

	/**
	 * @see ILaunchConfiguration#isWorkingCopy()
	 */
	public boolean isWorkingCopy() {
		return false;
	}

	/**
	 * @see ILaunchConfiguration#delete()
	 */
	public void delete() throws CoreException {
		if (exists()) {
			if (isLocal()) {
				if (!(getLocation().toFile().delete())) {
					throw new DebugException(
						new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(),
						 DebugException.REQUEST_FAILED, DebugCoreMessages.getString("LaunchConfiguration.Failed_to_delete_launch_configuration._1"), null) //$NON-NLS-1$
					);
				}
				// manually update the launch manager cache since there
				// will be no resource delta
				getLaunchManager().launchConfigurationDeleted(this);
			} else {
				// delete the resource using IFile API such that
				// resource deltas are fired.
				IFile file = getFile();
				if (file != null) {
					// validate edit
					if (file.isReadOnly()) {
						IStatus status = ResourcesPlugin.getWorkspace().validateEdit(new IFile[] {file}, null);
						if (!status.isOK()) {
							throw new CoreException(status);
						}
					}
					file.delete(true, null);
				} else {
					// Error - the exists test passed, but could not locate file 
				}
			}
		}
	}
	
	/**
	 * Returns the info object containing the attributes
	 * of this configuration
	 * 
	 * @return info for this handle
	 * @exception CoreException if unable to retrieve the
	 *  info object
	 */
	protected LaunchConfigurationInfo getInfo() throws CoreException {
		return getLaunchManager().getInfo(this);
	}
	
	/**
	 * Returns the launch configuration delegate for this
	 * launch configuration, for the specified launch mode.
	 * 
	 * @param mode launch mode
	 * @return launch configuration delegate
	 * @exception CoreException if the delegate was unable
	 *  to be created
	 */
	protected ILaunchConfigurationDelegate getDelegate(String mode) throws CoreException {
		return getType().getDelegate(mode);
	}
	
	/**
	 * Returns the launch manager
	 * 
	 * @return launch manager
	 */
	protected LaunchManager getLaunchManager() {
		return (LaunchManager)DebugPlugin.getDefault().getLaunchManager();
	}

	/**
	 * @see ILaunchConfiguration#getMemento()
	 */
	public String getMemento() throws CoreException {
		IPath relativePath = null;
		if (isLocal()) {
			IPath rootPath = LaunchManager.LOCAL_LAUNCH_CONFIGURATION_CONTAINER_PATH;
			IPath configPath = getLocation();
			relativePath = configPath.removeFirstSegments(rootPath.segmentCount());
			relativePath = relativePath.setDevice(null);
		} else {
			IFile file = getFile();
			if (file == null) {
				// cannot generate memento - missing file
				IStatus status = newStatus(MessageFormat.format(DebugCoreMessages.getString("LaunchConfiguration.Unable_to_generate_memento_for_{0},_shared_file_does_not_exist._1"), new String[]{getName()}), DebugException.INTERNAL_ERROR, null); //$NON-NLS-1$
				throw new CoreException(status); 
			}
			relativePath = getFile().getFullPath();
		}
		
		Document doc = new DocumentImpl();
		Element node = doc.createElement("launchConfiguration"); //$NON-NLS-1$
		doc.appendChild(node);
		node.setAttribute("local", (new Boolean(isLocal())).toString()); //$NON-NLS-1$
		node.setAttribute("path", relativePath.toString()); //$NON-NLS-1$
		
		try {
			return LaunchManager.serializeDocument(doc);
		} catch (IOException e) {
			IStatus status = newStatus(DebugCoreMessages.getString("LaunchConfiguration.Exception_occurred_creating_launch_configuration_memento_9"), DebugException.INTERNAL_ERROR,  e); //$NON-NLS-1$
			throw new CoreException(status);
		}
	}

	/**
	 * @see ILaunchConfiguration#getFile()
	 */	
	public IFile getFile() {
		if (isLocal()) {
			return null;
		}
		return ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(getLocation());
	}

	/**
	 * @see ILaunchConfiguration#contentsEqual(ILaunchConfiguration)
	 */
	public boolean contentsEqual(ILaunchConfiguration object) {
		try {
			if (object instanceof LaunchConfiguration) {
				LaunchConfiguration otherConfig = (LaunchConfiguration) object;
				return getName().equals(otherConfig.getName())
				 	 && getType().equals(otherConfig.getType())
				 	 && getLocation().equals(otherConfig.getLocation())
					 && getInfo().equals(otherConfig.getInfo());
			}
			return false;
		} catch (CoreException ce) {
			return false;
		}
	}

	/**
	 * Returns whether this configuration is equal to the
	 * given configuration. Two configurations are equal if
	 * they are stored in the same location (and neither one
	 * is a working copy).
	 * 
	 * @return whether this configuration is equal to the
	 *  given configuration
	 * @see Object#equals(Object)
	 */
	public boolean equals(Object object) {
		if (object instanceof ILaunchConfiguration) {
			if (isWorkingCopy()) {
				return this == object;
			} 
			ILaunchConfiguration config = (ILaunchConfiguration) object;
			if (!config.isWorkingCopy()) {
				return config.getLocation().equals(getLocation());
			}
		}
		return false;
	}
	
	/**
	 * @see Object#hashCode()
	 */
	public int hashCode() {
		return getLocation().hashCode();
	}
	
	/**
	 * Returns the container this launch configuration is 
	 * stored in, or <code>null</code> if this launch configuration
	 * is stored locally.
	 * 
	 * @return the container this launch configuration is 
	 * stored in, or <code>null</code> if this launch configuration
	 * is stored locally
	 */
	protected IContainer getContainer() {
		IFile file = getFile();
		if (file != null) {
			return file.getParent();
		}
		return null;
	}
	
	/**
	 * @see org.eclipse.debug.core.ILaunchConfiguration#getCategory()
	 */
	public String getCategory() throws CoreException {
		return getType().getCategory();
	}

	/**
	 * @see org.eclipse.debug.core.ILaunchConfiguration#getAttributes()
	 */
	public Map getAttributes() throws CoreException {
		LaunchConfigurationInfo info = getInfo();
		return info.getAttributes();
	}

}

