package org.eclipse.debug.core;

/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp.  All rights reserved.
This file is made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html
**********************************************************************/
 
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;

/**
 * Describes and creates instances of a specific type of
 * launch configuration. Launch configuration types are
 * defined by extensions.
 * <p>
 * A launch configuration type extension is defined in <code>plugin.xml</code>.
 * Following is an example definition of a launch configuration
 * type extension.
 * <pre>
 * &lt;extension point="org.eclipse.debug.core.launchConfigurationTypes"&gt;
 *   &lt;launchConfigurationType 
 *      id="com.example.ExampleIdentifier"
 *      delegate="com.example.ExampleLaunchConfigurationDelegate"
 *      modes="run, debug"
 *      name="Example Application"&gt;
 *   &lt;/launchConfigurationType&gt;
 * &lt;/extension&gt;
 * </pre>
 * The attributes are specified as follows:
 * <ul>
 * <li><code>id</code> specifies a unique identifier for this launch configuration
 *  type.</li>
 * <li><code>delegate</code> specifies the fully qualified name of the java class
 *   that implements <code>ILaunchConfigurationDelegate</code>. Launch configuration
 *   instances of this type will delegate to instances of this class
 *   to perform launching.</li>
 * <li><code>modes</code> specifies a comma separated list of the modes this
 *    type of launch configuration suports - <code>"run"</code> and/or <code>"debug"</code>.</li>
 * <li><code>name</code> specifies a human readable name for this type
 *    of launch configuration.</li>
 * <li><code>category</code> is an optional attribute that specifies a category
 * for this launch configuration type. Categories are client defined. This
 * attribute was added in the 2.1 release.</li>
 * </ul>
 * </p>
 * <p>
 * The <code>category</code> attribute has been added in release 2.1, such that other
 * tools may re-use the launch configuration framework for purposes other than
 * the standard running and debugging of programs under developement. Such that
 * clients may access arbitrary attribtes specified in launch configuration type
 * extension definitions, the method <code>getAttribute</code> has also been
 * added. Launch configurations that are to be recognized as standard run/debug
 * launch configurations should not specify the <code>category</code> attribute.
 * </p>
 * <p>
 * This interface is not intended to be implemented by clients. Clients
 * that define a launch configuration delegate extension implement the
 * <code>ILaunchConfigurationDelegate</code> interface.
 * </p>
 * @see ILaunchConfiguration
 * @since 2.0
 */
public interface ILaunchConfigurationType extends IAdaptable {
		
	/**
	 * Returns whether this type of launch configuration supports
	 * the specified mode.
	 * 
	 * @param mode a mode in which a configuration can be launched, one of
	 *  the mode constants defined by <code>ILaunchManager</code> - <code>RUN_MODE</code> or
	 *  <code>DEBUG_MODE</code>.
	 * @return whether this kind of launch configuration supports the
	 *  specified mode
	 */
	public boolean supportsMode(String mode);
	
	/**
	 * Returns the name of this type of launch configuration.
	 * 
	 * @return the name of this type of launch configuration
	 */
	public String getName();
	
	/**
	 * Returns the unique identifier for this type of launch configuration
	 * 
	 * @return the unique identifier for this type of launch configuration
	 */
	public String getIdentifier();
	
	/**
	 * Returns whether this launch configuration type is public.  Public configuration
	 * types are available for use by the user, for example, the user can create new
	 * configurations based on public types through the UI.  Private types are not
	 * accessbile in this way, but are still available through the methods on 
	 * <code>ILaunchManager</code>.
	 * 
	 * @return whether this launch configuration type is public.
	 */
	public boolean isPublic();
	
	/**
	 * Returns a new launch configuration working copy of this type,
	 * that resides in the specified container, with the given name.
	 * When <code>container</code> is </code>null</code>, the configuration
	 * will reside locally in the metadata area.
	 * Note: a launch configuration is not actually created until the working copy is saved.
	 * 
	 * @param container the container in which the new configuration will
	 *  reside, or <code>null</code> if the configuration should reside
	 *  locally with the metadata.
	 * @param name name for the launch configuration
	 * @return a new launch configuration working copy instance of this type
	 * @exception CoreException if an instance of this type
	 *  of launch configuration could not be created for any
	 *  reason
	 */
	public ILaunchConfigurationWorkingCopy newInstance(IContainer container, String name) throws CoreException;
	
	/**
	 * Returns the launch configuration delegate for launch
	 * configurations of this type. The first time this method
	 * is called, the delegate is instantiated.
	 * 
	 * @return launch configuration delegate
	 * @exception CoreException if unable to instantiate the
	 *  delegate
	 */	
	public ILaunchConfigurationDelegate getDelegate() throws CoreException;
	
	/**
	 * Returns this launch configuration type's category, or <code>null</code>
	 * if unspecified. This corresponds to the category attribute specified in
	 * the extension definition.
	 * 
	 * @return this launch configuration type's category, or <code>null</code>
	 * @since 2.1
	 */
	public String getCategory();
	
	/**
	 * Returns the attribute with the given name, as specified by this launch
	 * configuration type's extension definition, or <code>null</code> if
	 * unspecified.
	 * 
	 * @param attributeName attribute name
	 * @return the specified extension attribute, or <code>null</code>
	 * @since 2.1
	 */
	public String getAttribute(String attributeName);	
		
}
