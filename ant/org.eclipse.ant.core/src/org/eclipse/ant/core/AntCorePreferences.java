/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Thierry Lach (thierry.lach@bbdodetroit.com) - bug 40502
 *******************************************************************************/
package org.eclipse.ant.core;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.ant.internal.core.AntClasspathEntry;
import org.eclipse.ant.internal.core.IAntCoreConstants;
import org.eclipse.ant.internal.core.InternalCoreAntMessages;
import org.eclipse.core.boot.BootLoader;
import org.eclipse.core.internal.plugins.PluginClassLoader;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.ILibrary;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.core.runtime.IPluginPrerequisite;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Status;


/**
 * Represents the Ant Core plug-in's preferences providing utilities for
 * extracting, changing and updating the underlying preferences.
 * @since 2.1
 */
public class AntCorePreferences implements org.eclipse.core.runtime.Preferences.IPropertyChangeListener {

	private List defaultTasks;
	private List defaultTypes;
	private List extraClasspathURLs;
	private List defaultProperties;
	private IAntClasspathEntry[] defaultAntHomeEntries;
	
	private Task[] customTasks;
	private Task[] oldCustomTasks;
	private Type[] customTypes;
	private Type[] oldCustomTypes;
	private IAntClasspathEntry[] antHomeEntries;
	private IAntClasspathEntry[] additionalEntries;
	private Property[] customProperties;
	private Property[] oldCustomProperties;
	private String[] customPropertyFiles;
	
	private List pluginClassLoaders;
	
	private ClassLoader[] orderedPluginClassLoaders;
	
	private String antHome;
	
	private boolean runningHeadless= false;
	
	

	protected AntCorePreferences(List defaultTasks, List defaultExtraClasspath, List defaultTypes, boolean headless) {
		this(defaultTasks, defaultExtraClasspath, defaultTypes, Collections.EMPTY_LIST, headless);
	}
	
	protected AntCorePreferences(List defaultTasks, List defaultExtraClasspath, List defaultTypes, List defaultProperties, boolean headless) {
		runningHeadless= headless;
		initializePluginClassLoaders();
		extraClasspathURLs = new ArrayList(20);
		this.defaultTasks = computeDefaultTasks(defaultTasks);
		this.defaultTypes = computeDefaultTypes(defaultTypes);
		computeDefaultExtraClasspathEntries(defaultExtraClasspath);
		computeDefaultProperties(defaultProperties);
		restoreCustomObjects();
		
	}
	
	/**
	 * When a preference changes, update the in-memory cache of the preference.
	 * @see org.eclipse.core.runtime.Preferences.IPropertyChangeListener#propertyChange(org.eclipse.core.runtime.Preferences.PropertyChangeEvent)
	 */
	public void propertyChange(Preferences.PropertyChangeEvent event) {
		Preferences prefs = AntCorePlugin.getPlugin().getPluginPreferences();
		String property= event.getProperty();
		if (property.equals(IAntCoreConstants.PREFERENCE_TASKS) || property.startsWith(IAntCoreConstants.PREFIX_TASK)) {
			restoreTasks(prefs);
		} else if (property.equals(IAntCoreConstants.PREFERENCE_TYPES) || property.startsWith(IAntCoreConstants.PREFIX_TYPE)) {
			restoreTypes(prefs);
		} else if (property.equals(IAntCoreConstants.PREFERENCE_ANT_HOME_ENTRIES)) {
			restoreAntHomeEntries(prefs);
		} else if (property.equals(IAntCoreConstants.PREFERENCE_ADDITIONAL_ENTRIES)) {
			restoreAdditionalEntries(prefs);
		} else if (property.equals(IAntCoreConstants.PREFERENCE_ANT_HOME)) {
			restoreAntHome(prefs);
		} else if (property.equals(IAntCoreConstants.PREFERENCE_PROPERTIES) || property.startsWith(IAntCoreConstants.PREFIX_PROPERTY)) {
			restoreCustomProperties(prefs);
		} else if (property.equals(IAntCoreConstants.PREFERENCE_PROPERTY_FILES)) {
			restoreCustomPropertyFiles(prefs);
		}
	}

	/**
	 * Restores the in-memory model of the preferences from the preference store
	 */
	private void restoreCustomObjects() {
		Preferences prefs = AntCorePlugin.getPlugin().getPluginPreferences();
		restoreAntHome(prefs);
		restoreTasks(prefs);
		restoreTypes(prefs);
		restoreAntHomeEntries(prefs);
		restoreAdditionalEntries(prefs);
		restoreCustomProperties(prefs);
		restoreCustomPropertyFiles(prefs);
		prefs.addPropertyChangeListener(this);
	}
	
	private void restoreTasks(Preferences prefs) {
		 String tasks = prefs.getString(IAntCoreConstants.PREFERENCE_TASKS);
		 if (tasks.equals("")) { //$NON-NLS-1$
			 customTasks = new Task[0];
		 } else {
			 customTasks = extractTasks(prefs, getArrayFromString(tasks));
		 }
	}
	
	private void restoreTypes(Preferences prefs) {
		String types = prefs.getString(IAntCoreConstants.PREFERENCE_TYPES);
		if (types.equals("")) {//$NON-NLS-1$
			customTypes = new Type[0];
		} else {
			customTypes = extractTypes(prefs, getArrayFromString(types));
		}
	}
	
	private void restoreAntHomeEntries(Preferences prefs) {
		String entries = prefs.getString("ant_urls"); //old constant //$NON-NLS-1$
		if (entries.equals("")) {//$NON-NLS-1$
			prefs.getString(IAntCoreConstants.PREFERENCE_ANT_HOME_ENTRIES);
		}
		if (entries.equals("")) {//$NON-NLS-1$
			antHomeEntries = getDefaultAntHomeEntries();
		} else {
			antHomeEntries = extractEntries(getArrayFromString(entries));
		}
	}
	
	private void restoreAdditionalEntries(Preferences prefs) {
		String entries = prefs.getString("urls"); //old constant //$NON-NLS-1$
		if (entries.equals("")) {//$NON-NLS-1$
			entries = prefs.getString(IAntCoreConstants.PREFERENCE_ADDITIONAL_ENTRIES);
		}
		if (entries.equals("")) {//$NON-NLS-1$
			IAntClasspathEntry toolsJarEntry= getToolsJarEntry();
			if (toolsJarEntry != null) {
				additionalEntries = new IAntClasspathEntry[] { toolsJarEntry };
			}
		} else {
			additionalEntries = extractEntries(getArrayFromString(entries));
		}
	}
	
	private void restoreAntHome(Preferences prefs) {
		antHome= prefs.getString(IAntCoreConstants.PREFERENCE_ANT_HOME);
		if (antHome == null || antHome.length() == 0) {
			antHome= getDefaultAntHome();
		}
	}
	
	/**
	 * Returns the absolute path of the default ant.home to use for the build.
	 * The default is the org.apache.ant plugin folder provided with Eclipse.
	 * 
	 * @return String absolute path of the default ant.home
	 * @since 3.0
	 */
	public String getDefaultAntHome() {
		IAntClasspathEntry[] entries= getDefaultAntHomeEntries();
		if (entries.length > 0) {
			URL antjar= entries[0].getEntryURL();
			IPath antHomePath= new Path(antjar.getFile());
			//parent directory of the lib directory
			antHomePath= antHomePath.removeLastSegments(2);
			return antHomePath.toFile().getAbsolutePath();
		} 
		return null;
	}
	
	private void restoreCustomProperties(Preferences prefs) {
		String properties = prefs.getString(IAntCoreConstants.PREFERENCE_PROPERTIES);
		if (properties.equals("")) {//$NON-NLS-1$
			customProperties = new Property[0];
		} else {
			customProperties = extractProperties(prefs, getArrayFromString(properties));
		}
	}
	
	private void restoreCustomPropertyFiles(Preferences prefs) {
		String propertyFiles= prefs.getString(IAntCoreConstants.PREFERENCE_PROPERTY_FILES);
		if (propertyFiles.equals("")) { //$NON-NLS-1$
			customPropertyFiles= new String[0];
		} else {
			customPropertyFiles= getArrayFromString(propertyFiles);
		}
	}

	protected Task[] extractTasks(Preferences prefs, String[] tasks) {
		List result = new ArrayList(tasks.length);
		for (int i = 0; i < tasks.length; i++) {
			try {
				String taskName = tasks[i];
				String[] values = getArrayFromString(prefs.getString(IAntCoreConstants.PREFIX_TASK + taskName));
				if (values.length < 2) {
					continue;
				}
				Task task = new Task();
				task.setTaskName(taskName);
				task.setClassName(values[0]);
				task.setLibrary(new URL(values[1]));
				result.add(task);
			} catch (MalformedURLException e) {
				// if the URL does not have a valid format, just log and ignore the exception
				IStatus status = new Status(IStatus.ERROR, AntCorePlugin.PI_ANTCORE, AntCorePlugin.ERROR_MALFORMED_URL, InternalCoreAntMessages.getString("AntCorePreferences.Malformed_URL._1"), e); //$NON-NLS-1$
				AntCorePlugin.getPlugin().getLog().log(status);
			}
		}
		return (Task[]) result.toArray(new Task[result.size()]);
	}

	protected Type[] extractTypes(Preferences prefs, String[] types) {
		List result = new ArrayList(types.length);
		for (int i = 0; i < types.length; i++) {
			try {
				String typeName = types[i];
				String[] values = getArrayFromString(prefs.getString(IAntCoreConstants.PREFIX_TYPE + typeName));
				if (values.length < 2) {
					continue;
				}
				Type type = new Type();
				type.setTypeName(typeName);
				type.setClassName(values[0]);
				type.setLibrary(new URL(values[1]));
				result.add(type);
			} catch (MalformedURLException e) {
				// if the URL does not have a valid format, just log and ignore the exception
				IStatus status = new Status(IStatus.ERROR, AntCorePlugin.PI_ANTCORE, AntCorePlugin.ERROR_MALFORMED_URL, InternalCoreAntMessages.getString("AntCorePreferences.Malformed_URL._1"), e);  //$NON-NLS-1$
				AntCorePlugin.getPlugin().getLog().log(status);
			}
		}
		return (Type[]) result.toArray(new Type[result.size()]);
	}
	
	protected Property[] extractProperties(Preferences prefs, String[] properties) {
		Property[] result = new Property[properties.length];
		for (int i = 0; i < properties.length; i++) {
			String propertyName = properties[i];
			String[] values = getArrayFromString(prefs.getString(IAntCoreConstants.PREFIX_PROPERTY + propertyName));
			if (values.length < 1) {
				continue;
			}
			Property property = new Property();
			property.setName(propertyName);
			property.setValue(values[0]);
			result[i]= property;
		}
		return result;
	}

	protected IAntClasspathEntry[] extractEntries(String[] entries) {
		List result = new ArrayList(entries.length);
		for (int i = 0; i < entries.length; i++) {
			result.add(new AntClasspathEntry(entries[i]));
		}
		return (IAntClasspathEntry[]) result.toArray(new IAntClasspathEntry[result.size()]);
	}

	/**
	 * Returns the array of URLs that is the default set of URLs defining
	 * the Ant classpath.
	 * 
	 * Ant running through the command line tries to find tools.jar to help the
	 * user. Try emulating the same behaviour here.
	 *
	 * @return the default set of URLs defining the Ant classpath
	 * @deprecated
	 */
	public URL[] getDefaultAntURLs() {
		IAntClasspathEntry[] entries= getDefaultAntHomeEntries();
		List result= new ArrayList(3);
		for (int i = 0; i < entries.length; i++) {
			IAntClasspathEntry entry = entries[i];
			result.add(entry.getEntryURL());
		}
		URL toolsURL= getToolsJarURL();
		if (toolsURL != null) {
			result.add(toolsURL);
		}
		return (URL[]) result.toArray(new URL[result.size()]);
	}
	
	/**
	 * Returns the array of classpath entires that is the default set of entires defining
	 * the Ant classpath.
	 *
	 * @return the default set of classpath entries defining the Ant classpath
	 */
	public IAntClasspathEntry[] getDefaultAntHomeEntries() {
		if (defaultAntHomeEntries== null) {
			List result = new ArrayList(2);
			Plugin antPlugin= Platform.getPlugin("org.apache.ant"); //$NON-NLS-1$
			if (antPlugin != null) {
				IPluginDescriptor descriptor = antPlugin.getDescriptor(); 
				addLibraries(descriptor, result);
			}
			
			defaultAntHomeEntries= (IAntClasspathEntry[]) result.toArray(new IAntClasspathEntry[result.size()]);
		}
		return defaultAntHomeEntries;
	}
	
	/**
	 * Returns the array of URLs that is the set of URLs defining the Ant
	 * classpath.
	 * 
	 * @return the set of URLs defining the Ant classpath
	 * @deprecated use getAntHomeClasspathEntries and getToolsJarEntry
	 */
	public URL[] getAntURLs() {
		int extra= 0;
		IAntClasspathEntry entry= getToolsJarEntry();
		if (entry != null) {
			extra++;
		}
		URL[] urls= new URL[antHomeEntries.length + extra];
		int i;
		for (i = 0; i < antHomeEntries.length; i++) {
			URL url = antHomeEntries[i].getEntryURL();
			if (url != null) {
				urls[i]=url;
			}
		}
		if (extra >0) {
			urls[i]= entry.getEntryURL();
		}
		return urls;
		
	}

	protected List computeDefaultTasks(List tasks) {
		List result = new ArrayList(tasks.size());
		for (Iterator iterator = tasks.iterator(); iterator.hasNext();) {
			IConfigurationElement element = (IConfigurationElement) iterator.next();
			if (runningHeadless) {
				String headless = element.getAttribute(AntCorePlugin.HEADLESS);
				if (headless != null) {
					boolean headlessType= Boolean.valueOf(headless).booleanValue();
					if (!headlessType) {
						continue;
					}
				}
			}
			Task task = new Task();
			task.setTaskName(element.getAttribute(AntCorePlugin.NAME));
			task.setClassName(element.getAttribute(AntCorePlugin.CLASS));
			String library = element.getAttribute(AntCorePlugin.LIBRARY);
			if (library == null) {
				IStatus status = new Status(IStatus.ERROR, AntCorePlugin.PI_ANTCORE, AntCorePlugin.ERROR_LIBRARY_NOT_SPECIFIED, MessageFormat.format(InternalCoreAntMessages.getString("AntCorePreferences.Library_not_specified_for__{0}_4"), new String[]{task.getTaskName()}), null); //$NON-NLS-1$
				AntCorePlugin.getPlugin().getLog().log(status);
				continue;
			}
			
			IPluginDescriptor descriptor = element.getDeclaringExtension().getDeclaringPluginDescriptor();
			task.setPluginLabel(descriptor.getLabel());
			try {
				URL url = Platform.asLocalURL(new URL(descriptor.getInstallURL(), library));
				if (new File(url.getPath()).exists()) {
					if (!extraClasspathURLs.contains(url)) {
						extraClasspathURLs.add(url);
					}
					result.add(task);
					addPluginClassLoader(descriptor.getPluginClassLoader());
					task.setLibrary(url);
				} else {
					//task specifies a library that does not exist
					IStatus status = new Status(IStatus.ERROR, AntCorePlugin.PI_ANTCORE, AntCorePlugin.ERROR_LIBRARY_NOT_SPECIFIED, MessageFormat.format(InternalCoreAntMessages.getString("AntCorePreferences.No_library_for_task"), new String[]{url.toExternalForm(), descriptor.getLabel()}), null); //$NON-NLS-1$
					AntCorePlugin.getPlugin().getLog().log(status);
					continue;
				}
			} catch (Exception e) {
				// if the URL does not have a valid format, just log and ignore the exception
				IStatus status = new Status(IStatus.ERROR, AntCorePlugin.PI_ANTCORE, AntCorePlugin.ERROR_MALFORMED_URL, InternalCoreAntMessages.getString("AntCorePreferences.Malformed_URL._1"), e); //$NON-NLS-1$
				AntCorePlugin.getPlugin().getLog().log(status);
				continue;
			}
		}
		return result;
	}

	protected List computeDefaultTypes(List types) {
		List result = new ArrayList(types.size());
		for (Iterator iterator = types.iterator(); iterator.hasNext();) {
			IConfigurationElement element = (IConfigurationElement) iterator.next();
			if (runningHeadless) {
				String headless = element.getAttribute(AntCorePlugin.HEADLESS);
				if (headless != null) {
					boolean headlessTask= Boolean.valueOf(headless).booleanValue();
					if (!headlessTask) {
						continue;
					}
				}
			}
			Type type = new Type();
			IPluginDescriptor descriptor = element.getDeclaringExtension().getDeclaringPluginDescriptor();
			type.setPluginLabel(descriptor.getLabel());
			type.setTypeName(element.getAttribute(AntCorePlugin.NAME));
			type.setClassName(element.getAttribute(AntCorePlugin.CLASS));
			String library = element.getAttribute(AntCorePlugin.LIBRARY);
			if (library == null) {
				IStatus status = new Status(IStatus.ERROR, AntCorePlugin.PI_ANTCORE, AntCorePlugin.ERROR_LIBRARY_NOT_SPECIFIED, MessageFormat.format(InternalCoreAntMessages.getString("AntCorePreferences.Library_not_specified_for__{0}_4"), new String[]{type.getTypeName()}), null); //$NON-NLS-1$
				AntCorePlugin.getPlugin().getLog().log(status);
				continue;
			}
			try {
				URL url = Platform.asLocalURL(new URL(descriptor.getInstallURL(), library));
				if (new File(url.getPath()).exists()) {
					if (!extraClasspathURLs.contains(url)) {
						extraClasspathURLs.add(url);
					}
					result.add(type);
					addPluginClassLoader(descriptor.getPluginClassLoader());
					type.setLibrary(url);
				} else {
					//type specifies a library that does not exist
					IStatus status = new Status(IStatus.ERROR, AntCorePlugin.PI_ANTCORE, AntCorePlugin.ERROR_LIBRARY_NOT_SPECIFIED, MessageFormat.format(InternalCoreAntMessages.getString("AntCorePreferences.No_library_for_type"), new String[]{url.toExternalForm(), descriptor.getLabel()}), null); //$NON-NLS-1$
					AntCorePlugin.getPlugin().getLog().log(status);
					continue;
				}
			} catch (Exception e) {
				// if the URL does not have a valid format, just log and ignore the exception
				IStatus status = new Status(IStatus.ERROR, AntCorePlugin.PI_ANTCORE, AntCorePlugin.ERROR_MALFORMED_URL, InternalCoreAntMessages.getString("AntCorePreferences.Malformed_URL._1"), e); //$NON-NLS-1$
				AntCorePlugin.getPlugin().getLog().log(status);
				continue;
			}
		}
		return result;
	}

	/**
	 * Computes the extra classpath entries defined plugins and fragments.
	 */
	protected void computeDefaultExtraClasspathEntries(List entries) {
		for (Iterator iterator = entries.iterator(); iterator.hasNext();) {
			IConfigurationElement element = (IConfigurationElement) iterator.next();
			if (runningHeadless) {
				String headless = element.getAttribute(AntCorePlugin.HEADLESS);
				if (headless != null) {
					boolean headlessEntry= Boolean.valueOf(headless).booleanValue();
					if (!headlessEntry) {
						continue;
					}
				}
			}
			String library = element.getAttribute(AntCorePlugin.LIBRARY);
			IPluginDescriptor descriptor = element.getDeclaringExtension().getDeclaringPluginDescriptor();
			try {
				URL url = Platform.asLocalURL(new URL(descriptor.getInstallURL(), library));
				
				if (new File(url.getPath()).exists()) {
					if (!extraClasspathURLs.contains(url)) {
						extraClasspathURLs.add(url);
					}
					addPluginClassLoader(descriptor.getPluginClassLoader());
				} else {
					//extra classpath entry that does not exist
					IStatus status = new Status(IStatus.ERROR, AntCorePlugin.PI_ANTCORE, AntCorePlugin.ERROR_LIBRARY_NOT_SPECIFIED, MessageFormat.format(InternalCoreAntMessages.getString("AntCorePreferences.No_library_for_extraClasspathEntry"), new String[]{url.toExternalForm(), descriptor.getLabel()}), null); //$NON-NLS-1$
					AntCorePlugin.getPlugin().getLog().log(status);
					continue;
				}
			} catch (Exception e) {
				// if the URL does not have a valid format, just log and ignore the exception
				IStatus status = new Status(IStatus.ERROR, AntCorePlugin.PI_ANTCORE, AntCorePlugin.ERROR_MALFORMED_URL, InternalCoreAntMessages.getString("AntCorePreferences.Malformed_URL._1"), e); //$NON-NLS-1$
				AntCorePlugin.getPlugin().getLog().log(status);
				continue;
			}
		}
	}
	
	/**
	 * Scan the Ant property extensions for properties to set.
	 * 
	 * @since 3.0
	 */
	private void computeDefaultProperties(List properties) {
		defaultProperties = new ArrayList(properties.size());
		for (Iterator iterator = properties.iterator(); iterator.hasNext();) {
			IConfigurationElement element = (IConfigurationElement) iterator.next();
			if (runningHeadless) {
				String headless = element.getAttribute(AntCorePlugin.HEADLESS);
				if (headless != null) {
					boolean headlessProperty= Boolean.valueOf(headless).booleanValue();
					if (!headlessProperty) {
						continue;
					}
				}
			}
			String name = element.getAttribute(AntCorePlugin.NAME);
			if (name == null) {
				continue;
			}
			String value = element.getAttribute(AntCorePlugin.VALUE);
			if (value != null) {
				Property property = new Property(name, value);
				IPluginDescriptor descriptor= element.getDeclaringExtension().getDeclaringPluginDescriptor();
				property.setPluginLabel(descriptor.getLabel());
				defaultProperties.add(property);
			} else {
				Property property = new Property();
				property.setName(name);
				IPluginDescriptor descriptor= element.getDeclaringExtension().getDeclaringPluginDescriptor();
				property.setPluginLabel(descriptor.getLabel());
				String className = element.getAttribute(AntCorePlugin.CLASS);
				property.setValueProvider(className, descriptor.getPluginClassLoader());
				defaultProperties.add(property);
			}
		}
	}

	/**
	 * Returns the IAntClasspathEntry for the tools.jar associated with the path supplied
	 * May return <code>null</code> if no tools.jar is found (e.g. the path
	 * points to a JRE install).
	 * 
	 * @return IAntClasspathEntry tools.jar IAntClasspathEntry or <code>null</code>
	 * @since 3.0
	 */
	public IAntClasspathEntry getToolsJarEntry(IPath javaHomePath) {
		if (javaHomePath.lastSegment().equalsIgnoreCase("jre")) { //$NON-NLS-1$
			javaHomePath = javaHomePath.removeLastSegments(1);
		}
		javaHomePath = javaHomePath.append("lib").append("tools.jar"); //$NON-NLS-1$ //$NON-NLS-2$
		File tools = javaHomePath.toFile();
		if (!tools.exists()) {
			//attempt to find in the older 1.1.* 
			javaHomePath= javaHomePath.removeLastSegments(1);
			javaHomePath= javaHomePath.append("classes.zip"); //$NON-NLS-1$
			tools = javaHomePath.toFile();
			if (!tools.exists()) {
				return null;
			}
		}
			return new AntClasspathEntry(tools.getAbsolutePath()); 
	}

	/**
	 * Returns the URL for the tools.jar associated with the "java.home"
	 * location. May return <code>null</code> if no tools.jar is found (e.g. "java.home"
	 * points to a JRE install).
	 * 
	 * @return URL tools.jar URL or <code>null</code>
	 * @deprecated
	 */
	public URL getToolsJarURL() {
		IPath path = new Path(System.getProperty("java.home")); //$NON-NLS-1$
		return getToolsJarEntry(path).getEntryURL();
	}
	
	/**
	 * Returns the IAntClasspathEntry for the tools.jar associated with the "java.home"
	 * location. May return <code>null</code> if no tools.jar is found (e.g. "java.home"
	 * points to a JRE install).
	 * 
	 * @return IAntClasspathEntry tools.jar IAntClasspathEntry or <code>null</code>
	 */
	public IAntClasspathEntry getToolsJarEntry() {
		IPath path = new Path(System.getProperty("java.home")); //$NON-NLS-1$
		return getToolsJarEntry(path);
	}

	private void addLibraries(IPluginDescriptor source, List destination) {
		URL root = source.getInstallURL();
		ILibrary[] libraries = source.getRuntimeLibraries();
		for (int i = 0; i < libraries.length; i++) {
			try {
				URL url = new URL(root, libraries[i].getPath().toString());
				destination.add(new AntClasspathEntry(Platform.asLocalURL(url).getFile()));
			} catch (Exception e) {
				// if the URL does not have a valid format, just log and ignore the exception
				IStatus status = new Status(IStatus.ERROR, AntCorePlugin.PI_ANTCORE, AntCorePlugin.ERROR_MALFORMED_URL, InternalCoreAntMessages.getString("AntCorePreferences.Malformed_URL._1"), e);  //$NON-NLS-1$
				AntCorePlugin.getPlugin().getLog().log(status);
			}
		}
	}

	protected void addPluginClassLoader(ClassLoader loader) {
		if (!pluginClassLoaders.contains(loader)) {
			pluginClassLoaders.add(loader);
		}
	}

	/**
	 * Returns the list of urls added to the classpath by the extra classpath
	 * entries extension point.
	 * 
	 * @return the list of extra classpath URLs
	 */
	public URL[] getExtraClasspathURLs() {
		return (URL[])extraClasspathURLs.toArray(new URL[extraClasspathURLs.size()]);
	}
	
	/**
	 * Returns the entire set of URLs that define the Ant runtime classpath.
	 * Includes the Ant URLs, the custom URLs and extra classpath URLs.
	 * 
	 * @return the entire runtime classpath of URLs
	 */
	public URL[] getURLs() {
		List result = new ArrayList(50);
		if (antHomeEntries != null) {
			for (int i = 0; i < antHomeEntries.length; i++) {
				IAntClasspathEntry entry = antHomeEntries[i];
				result.add(entry.getEntryURL());	
			}
		}
		if (additionalEntries != null && additionalEntries.length > 0) {
			for (int i = 0; i < additionalEntries.length; i++) {
				IAntClasspathEntry entry = additionalEntries[i];
				result.add(entry.getEntryURL());	
			}
		}
		if (extraClasspathURLs != null) {
			result.addAll(extraClasspathURLs);
		}
		return (URL[]) result.toArray(new URL[result.size()]);
	}
	
	/**
	 * Returns the entire set of classpath entires that define the Ant runtime classpath.
	 * Includes the Ant home entries, the additional entries and extra classpath entries.
	 * 
	 * @return the entire runtime classpath
	 */
	public IAntClasspathEntry[] getClasspathEntries() {
		List result = new ArrayList(50);
		if (antHomeEntries != null) {
			result.addAll(Arrays.asList(antHomeEntries));
		}
		if (additionalEntries != null && additionalEntries.length > 0) {
			result.addAll(Arrays.asList(additionalEntries));
		}
		if (extraClasspathURLs != null) {
			result.addAll(extraClasspathURLs);
		}
		return (IAntClasspathEntry[]) result.toArray(new IAntClasspathEntry[result.size()]);
	}
	
	

	protected ClassLoader[] getPluginClassLoaders() {
		if (orderedPluginClassLoaders == null) {
			Iterator classLoaders= pluginClassLoaders.iterator();
			Map idToLoader= new HashMap(pluginClassLoaders.size());
			IPluginDescriptor[] descriptors= new IPluginDescriptor[pluginClassLoaders.size()];
			int i= 0;
			while (classLoaders.hasNext()) {
				PluginClassLoader loader = (PluginClassLoader) classLoaders.next();
				IPluginDescriptor descriptor= loader.getPluginDescriptor();
				idToLoader.put(descriptor.getUniqueIdentifier(), loader);
				descriptors[i]= descriptor;
				i++;
			}
			String[] ids= computePrerequisiteOrderPlugins(descriptors);
			orderedPluginClassLoaders= new ClassLoader[pluginClassLoaders.size()];
			for (int j = 0; j < ids.length; j++) {
				String id = ids[j];
				orderedPluginClassLoaders[j]= (ClassLoader)idToLoader.get(id);
			}
		}
		return orderedPluginClassLoaders;
	}

	/**
	 * Copied from org.eclipse.pde.internal.build.Utils
	 */
	private String[] computePrerequisiteOrderPlugins(IPluginDescriptor[] plugins) {
		List prereqs = new ArrayList(9);
		Set pluginList = new HashSet(plugins.length);
		for (int i = 0; i < plugins.length; i++) {
			pluginList.add(plugins[i].getUniqueIdentifier());
		}
		
		// create a collection of directed edges from plugin to prereq
		for (int i = 0; i < plugins.length; i++) {
			boolean boot = false;
			boolean runtime = false;
			boolean found = false;
			IPluginPrerequisite[] prereqList = plugins[i].getPluginPrerequisites();
			if (prereqList != null) {
				for (int j = 0; j < prereqList.length; j++) {
					// ensure that we only include values from the original set.
					String prereq = prereqList[j].getUniqueIdentifier();
					boot = boot || prereq.equals(BootLoader.PI_BOOT);
					runtime = runtime || prereq.equals(Platform.PI_RUNTIME);
					if (pluginList.contains(prereq)) {
						found = true;
						prereqs.add(new String[] { plugins[i].getUniqueIdentifier(), prereq });
					}
				}
			}

			// if we didn't find any prereqs for this plugin, add a null prereq
			// to ensure the value is in the output	
			if (!found) {
				prereqs.add(new String[] { plugins[i].getUniqueIdentifier(), null });
			}

			// if we didn't find the boot or runtime plugins as prereqs and they are in the list
			// of plugins to build, add prereq relations for them.  This is required since the 
			// boot and runtime are implicitly added to a plugin's requires list by the platform runtime.
			// Note that we should skip the xerces plugin as this would cause a circularity.
			if (plugins[i].getUniqueIdentifier().equals("org.apache.xerces")) //$NON-NLS-1$
				continue;
			if (!boot && pluginList.contains(BootLoader.PI_BOOT) && !plugins[i].getUniqueIdentifier().equals(BootLoader.PI_BOOT))
				prereqs.add(new String[] { plugins[i].getUniqueIdentifier(), BootLoader.PI_BOOT });
			if (!runtime && pluginList.contains(Platform.PI_RUNTIME) && !plugins[i].getUniqueIdentifier().equals(Platform.PI_RUNTIME) && !plugins[i].getUniqueIdentifier().equals(BootLoader.PI_BOOT))
				prereqs.add(new String[] { plugins[i].getUniqueIdentifier(), Platform.PI_RUNTIME });
		}

		// do a topological sort, insert the fragments into the sorted elements
		String[][] prereqArray = (String[][]) prereqs.toArray(new String[prereqs.size()][]);
		return computeNodeOrder(prereqArray);
	}
	
	/**
	 * Copied from org.eclipse.pde.internal.build.Utils
	 */
	private String[] computeNodeOrder(String[][] specs) {
		Map counts = computeCounts(specs);
		List nodes = new ArrayList(counts.size());
		while (!counts.isEmpty()) {
			List roots = findRootNodes(counts);
			if (roots.isEmpty())
				break;
			for (Iterator i = roots.iterator(); i.hasNext();)
				counts.remove(i.next());
			nodes.addAll(roots);
			removeArcs(specs, roots, counts);
		}
		String[] result = new String[nodes.size()];
		nodes.toArray(result);

		return result;
	}
	
	/**
	 * Copied from org.eclipse.pde.internal.build.Utils
	 */
	private void removeArcs(String[][] mappings, List roots, Map counts) {
		for (Iterator j = roots.iterator(); j.hasNext();) {
			String root = (String) j.next();
			for (int i = 0; i < mappings.length; i++) {
				if (root.equals(mappings[i][1])) {
					String input = mappings[i][0];
					Integer count = (Integer) counts.get(input);
					if (count != null)
						counts.put(input, new Integer(count.intValue() - 1));
				}
			}
		}
	}
	
	/**
	 * Copied from org.eclipse.pde.internal.build.Utils
	 */
	private List findRootNodes(Map counts) {
		List result = new ArrayList(5);
		for (Iterator i = counts.keySet().iterator(); i.hasNext();) {
			String node = (String) i.next();
			int count = ((Integer) counts.get(node)).intValue();
			if (count == 0)
				result.add(node);
		}
		return result;
	}
	
	/**
	 * Copied from org.eclipse.pde.internal.build.Utils
	 */
	private Map computeCounts(String[][] mappings) {
		Map counts = new HashMap(5);
		for (int i = 0; i < mappings.length; i++) {
			String from = mappings[i][0];
			Integer fromCount = (Integer) counts.get(from);
			String to = mappings[i][1];
			if (to == null)
				counts.put(from, new Integer(0));
			else {
				if (((Integer) counts.get(to)) == null)
					counts.put(to, new Integer(0));
				fromCount = fromCount == null ? new Integer(1) : new Integer(fromCount.intValue() + 1);
				counts.put(from, fromCount);
			}
		}
		return counts;
	}
	
	private void initializePluginClassLoaders() {
		pluginClassLoaders = new ArrayList(10);
		// ant.core should always be present (provides access to Xerces as well)
		pluginClassLoaders.add(Platform.getPlugin(AntCorePlugin.PI_ANTCORE).getDescriptor().getPluginClassLoader());
	}

	/**
	 * Returns the default and custom tasks.
	 * 
	 * @return the list of default and custom tasks.
	 */
	public List getTasks() {
		List result = new ArrayList(10);
		if (defaultTasks != null && !defaultTasks.isEmpty()) {
			result.addAll(defaultTasks);
		}
		if (customTasks != null && customTasks.length != 0) {
			result.addAll(Arrays.asList(customTasks));
		}
		return result;
	}

	/**
	 * Returns the user defined custom tasks
	 * @return the user defined tasks
	 */
	public Task[] getCustomTasks() {
		return customTasks;
	}

	/**
	 * Returns the user defined custom types
	 * @return the user defined types
	 */
	public Type[] getCustomTypes() {
		return customTypes;
	}

	/**
	 * Returns the custom user properties specified for Ant builds.
	 * 
	 * @return the properties defined for Ant builds.
	 */
	public Property[] getCustomProperties() {
		return customProperties;
	}
	
	/**
	 * Returns the default and custom properties.
	 * 
	 * @return the list of default and custom properties.
	 * @since 3.0
	 */
	public List getProperties() {
		List result = new ArrayList(10);
		if (defaultProperties != null && !defaultProperties.isEmpty()) {
			result.addAll(defaultProperties);
		}
		if (customProperties != null && customProperties.length != 0) {
			result.addAll(Arrays.asList(customProperties));
		}
		return result;
	}
	
	/**
	 * Returns the custom property files specified for Ant builds.
	 * 
	 * @return the property files defined for Ant builds.
	 */
	public String[] getCustomPropertyFiles() {
		return customPropertyFiles;
	}
	
	/**
	 * Returns the custom URLs specified for the Ant classpath
	 * 
	 * @return the urls defining the Ant classpath
	 * @deprecated
	 */
	public URL[] getCustomURLs() {
		URL[] urls= new URL[additionalEntries.length];
		int i;
		for (i = 0; i < additionalEntries.length; i++) {
			URL url = additionalEntries[i].getEntryURL();
			if (url != null) {
				urls[i]=url;
			}
		}
	
		return urls;
	}

	/**
	 * Sets the user defined custom tasks.
	 * To commit the changes, updatePluginPreferences must be
	 * called.
	 * @param tasks
	 */
	public void setCustomTasks(Task[] tasks) {
		oldCustomTasks= customTasks;
		customTasks = tasks;
	}

	/**
	 * Sets the user defined custom types.
	 * To commit the changes, updatePluginPreferences must be
	 * called.
	 * @param tasks
	 */
	public void setCustomTypes(Type[] types) {
		oldCustomTypes= customTypes;
		customTypes = types;
	}

	/**
	 * Sets the custom URLs specified for the Ant classpath.
	 * To commit the changes, updatePluginPreferences must be
	 * called.
	 * 
	 * @param the urls defining the Ant classpath
	 * @deprecated use setAdditionalEntries(IAntClasspathEntry)[]
	 */
	public void setCustomURLs(URL[] urls) {
		additionalEntries= new IAntClasspathEntry[urls.length];
		for (int i = 0; i < urls.length; i++) {
			URL url = urls[i];
			IAntClasspathEntry entry= new AntClasspathEntry(url.getFile());
			additionalEntries[i]= entry;
		}
	}
	
	/**
	 * Sets the Ant URLs specified for the Ant classpath. To commit the changes,
	 * updatePluginPreferences must be called.
	 * 
	 * @param the urls defining the Ant classpath
	 * @deprecated use setAntHomeEntires(IAntClasspathEntry[])
	 */
	public void setAntURLs(URL[] urls) {
		antHomeEntries= new IAntClasspathEntry[urls.length];
		for (int i = 0; i < urls.length; i++) {
			URL url = urls[i];
			IAntClasspathEntry entry= new AntClasspathEntry(url.getFile());
			antHomeEntries[i]= entry;
		}
	}
	
	/**
	 * Sets the custom property files specified for Ant builds. To commit the
	 * changes, updatePluginPreferences must be called.
	 * 
	 * @param the absolute paths defining the property files to use.
	 */
	public void setCustomPropertyFiles(String[] paths) {
		customPropertyFiles = paths;
	}
	
	/**
	 * Sets the custom user properties specified for Ant builds. To commit the
	 * changes, updatePluginPreferences must be called.
	 * 
	 * @param the properties defining the Ant properties
	 */
	public void setCustomProperties(Property[] properties) {
		oldCustomProperties= customProperties;
		customProperties = properties;
	}

	/**
	 * Returns the default and custom types.
	 * 
	 * @return all of the defined types
	 */
	public List getTypes() {
		List result = new ArrayList(10);
		if (defaultTypes != null && !defaultTypes.isEmpty()) {
			result.addAll(defaultTypes);
		}
		if (customTypes != null && customTypes.length != 0) {
			result.addAll(Arrays.asList(customTypes));
		}
		return result;
	}
	
	/**
	 * Returns the default types defined via the type extension point
	 * 
	 * @return all of the default types
	 */
	public List getDefaultTypes() {
		List result = new ArrayList(10);
		if (defaultTypes != null && !defaultTypes.isEmpty()) {
			result.addAll(defaultTypes);
		}
		return result;
	}
	
	/**
	 * Returns the default tasks defined via the task extension point
	 * 
	 * @return all of the default tasks
	 */
	public List getDefaultTasks() {
		List result = new ArrayList(10);
		if (defaultTasks != null && !defaultTasks.isEmpty()) {
			result.addAll(defaultTasks);
		}
		return result;
	}
	
	/**
	 * Returns the default properties defined via the properties extension point
	 * 
	 * @return all of the default properties
	 * @since 3.0
	 */
	public List getDefaultProperties() {
		List result = new ArrayList(10);
		if (defaultProperties != null && !defaultProperties.isEmpty()) {
			result.addAll(defaultProperties);
		}
		return result;
	}

	/**
	 * Convert a list of tokens into an array using "," as the tokenizer.
	 */
	protected String[] getArrayFromString(String list) {
		String separator= ","; //$NON-NLS-1$
		if (list == null || list.trim().equals("")) { //$NON-NLS-1$
			return new String[0];
		}
		ArrayList result = new ArrayList();
		for (StringTokenizer tokens = new StringTokenizer(list, separator); tokens.hasMoreTokens();) {
			String token = tokens.nextToken().trim();
			if (!token.equals("")) { //$NON-NLS-1$
				result.add(token);
			}
		}
		return (String[]) result.toArray(new String[result.size()]);
	}

	/**
	 * Updates the underlying plugin preferences to the current state.
	 */
	public void updatePluginPreferences() {
		Preferences prefs = AntCorePlugin.getPlugin().getPluginPreferences();
		prefs.removePropertyChangeListener(this);
		updateTasks(prefs);
		updateTypes(prefs);
		updateAntHomeEntries(prefs);
		updateAdditionalEntries(prefs);
		updateProperties(prefs);
		updatePropertyFiles(prefs);
		AntCorePlugin.getPlugin().savePluginPreferences();
		prefs.addPropertyChangeListener(this);
	}

	protected void updateTasks(Preferences prefs) {
		if (oldCustomTasks != null) {
			for (int i = 0; i < oldCustomTasks.length; i++) {
				Task oldTask = oldCustomTasks[i];
				prefs.setToDefault(IAntCoreConstants.PREFIX_TASK + oldTask.getTaskName());
			}
			oldCustomTasks= null;	
		}	
		
		if (customTasks.length == 0) {
			prefs.setValue(IAntCoreConstants.PREFERENCE_TASKS, ""); //$NON-NLS-1$
			return;
		}
		StringBuffer tasks = new StringBuffer();
		for (int i = 0; i < customTasks.length; i++) {
			tasks.append(customTasks[i].getTaskName());
			tasks.append(',');
			prefs.setValue(IAntCoreConstants.PREFIX_TASK + customTasks[i].getTaskName(), customTasks[i].getClassName() + "," + customTasks[i].getLibrary().toExternalForm()); //$NON-NLS-1$
		}
		prefs.setValue(IAntCoreConstants.PREFERENCE_TASKS, tasks.toString());
	}

	protected void updateTypes(Preferences prefs) {
		if (oldCustomTypes != null) {
			for (int i = 0; i < oldCustomTypes.length; i++) {
				Type oldType = oldCustomTypes[i];
				prefs.setToDefault(IAntCoreConstants.PREFIX_TYPE + oldType.getTypeName());
			}
			oldCustomTypes= null;	
		}	
				
		if (customTypes.length == 0) {
			prefs.setValue(IAntCoreConstants.PREFERENCE_TYPES, ""); //$NON-NLS-1$
			return;
		}
		StringBuffer types = new StringBuffer();
		for (int i = 0; i < customTypes.length; i++) {
			types.append(customTypes[i].getTypeName());
			types.append(',');
			prefs.setValue(IAntCoreConstants.PREFIX_TYPE + customTypes[i].getTypeName(), customTypes[i].getClassName() + "," + customTypes[i].getLibrary().toExternalForm()); //$NON-NLS-1$
		}
		prefs.setValue(IAntCoreConstants.PREFERENCE_TYPES, types.toString());
	}
	
	protected void updateProperties(Preferences prefs) {
		if (oldCustomProperties != null) {
			for (int i = 0; i < oldCustomProperties.length; i++) {
				Property oldProperty = oldCustomProperties[i];
				prefs.setToDefault(IAntCoreConstants.PREFIX_PROPERTY + oldProperty.getName());
			}
			oldCustomProperties= null;
		}
		
		if (customProperties.length == 0) {
			prefs.setValue(IAntCoreConstants.PREFERENCE_PROPERTIES, ""); //$NON-NLS-1$
			return;
		}
		StringBuffer properties = new StringBuffer();
		for (int i = 0; i < customProperties.length; i++) {
			properties.append(customProperties[i].getName());
			properties.append(',');
			prefs.setValue(IAntCoreConstants.PREFIX_PROPERTY + customProperties[i].getName(), customProperties[i].getValue()); //$NON-NLS-1$
		}
		prefs.setValue(IAntCoreConstants.PREFERENCE_PROPERTIES, properties.toString());
	}

	protected void updateAdditionalEntries(Preferences prefs) {
		String serialized= null;
		if (additionalEntries.length == 1 && additionalEntries[0].getLabel().equals(getToolsJarEntry().getLabel())) {
			serialized= ""; //$NON-NLS-1$
		} else {
			StringBuffer entries = new StringBuffer();
			for (int i = 0; i < additionalEntries.length; i++) {
				entries.append(additionalEntries[i].getLabel());
				entries.append(',');
			}
			serialized= entries.toString();
		}
		
		prefs.setValue(IAntCoreConstants.PREFERENCE_ADDITIONAL_ENTRIES, serialized);
		
		String prefAntHome= ""; //$NON-NLS-1$
		if (antHome != null && !antHome.equals(getDefaultAntHome())) {
			prefAntHome= antHome;
		} 
		prefs.setValue(IAntCoreConstants.PREFERENCE_ANT_HOME, prefAntHome);
	}
	
	protected void updateAntHomeEntries(Preferences prefs) {
		//see if the custom entries are just the default entries
		IAntClasspathEntry[] defaultEntries= getDefaultAntHomeEntries();
		boolean dflt= false;
		if (defaultEntries.length == antHomeEntries.length) {
			dflt= true;
			for (int i = 0; i < antHomeEntries.length; i++) {
				if (!antHomeEntries[i].equals(defaultEntries[i])) {
					dflt= false;
					break;
				}
			}
		}
		if (dflt) {
			//always want to recalculate the default Ant urls
			//to pick up any changes in the default Ant classpath
			prefs.setValue(IAntCoreConstants.PREFERENCE_ANT_HOME_ENTRIES, ""); //$NON-NLS-1$
			return;
		}
		StringBuffer entries = new StringBuffer();
		for (int i = 0; i < antHomeEntries.length; i++) {
			entries.append(antHomeEntries[i]);
			entries.append(',');
		}
		
		prefs.setValue(IAntCoreConstants.PREFERENCE_ANT_HOME_ENTRIES, entries.toString());
	}
	
	protected void updatePropertyFiles(Preferences prefs) {
		StringBuffer files = new StringBuffer();
		for (int i = 0; i < customPropertyFiles.length; i++) {
			files.append(customPropertyFiles[i]);
			files.append(',');
		}
		
		prefs.setValue(IAntCoreConstants.PREFERENCE_PROPERTY_FILES, files.toString());
	}
	
	/**
	 * Sets the string that defines the Ant home set by the user.
	 * May be set to <code>null</code>.
	 * 
	 * @param the fully qualified path to Ant home
	 */
	public void setAntHome(String antHome) {
		this.antHome= antHome;
	}
	
	/**
	 * Returns the string that defines the Ant home set by the user or the location 
	 * of the Eclipse Ant plugin if Ant home has not been specifically set by the user.
	 * Can return <code>null</code>
	 * 
	 * @return the fully qualified path to Ant home
	 */
	public String getAntHome() {
		return antHome;
	}
	
	/**
	 * Returns the set of classpath entries that compose the libraries added to the
	 * Ant runtime classpath from the Ant home location.
	 * 
	 * @return the set of ant home classpath entries
	 * @since 3.0
	 */
	public IAntClasspathEntry[] getAntHomeClasspathEntries() {
		return antHomeEntries;
	}
	
	/**
	 * Returns the set of classpath entries that the user has added to the
	 * Ant runtime classpath.
	 * 
	 * @return the set of user classpath entries
	 * @since 3.0
	 */
	public IAntClasspathEntry[] getAdditionalClasspathEntries() {
		return additionalEntries;
	}
	
	/**
	 * Sets the set of classpath entries that compose the libraries added to the
	 * Ant runtime classpath from the Ant home location.
	 * 
	 * @param the set of ant home classpath entries
	 * @since 3.0
	 */
	public void setAntHomeClasspathEntries(IAntClasspathEntry[] entries) {
		antHomeEntries= entries;
	}
	
	/**
	 * Sets the set of classpath entries that the user has added to the 
	 * Ant runtime classpath.
	 * 
	 * @param the set of user classpath entries
	 * @since 3.0
	 */
	public void setAdditionalClasspathEntries(IAntClasspathEntry[] entries) {
		additionalEntries= entries;
	}

	/**
	 * Returns the URL for the remote JAR to added to the classpath for a remote
	 * Ant build
	 * 
	 * @return the URL of the remoteAnt.jar
	 * @since 3.0
	 */
	public URL getRemoteAntURL() {
		Plugin antUIPlugin= Platform.getPlugin("org.eclipse.ant.ui"); //$NON-NLS-1$
		if (antUIPlugin != null) {
			IPluginDescriptor descriptor = antUIPlugin.getDescriptor();
			try {
				URL root = descriptor.getInstallURL();
				return Platform.asLocalURL(new URL(root, "lib/remoteAnt.jar")); //$NON-NLS-1$;
			} catch (MalformedURLException e) {
			} catch (IOException e1) {
			}
		}
		return null;
	}

	/**
	 * Returns the list of URLs to added to the classpath for a remote
	 * Ant build
	 * 
	 * @return the list of classpath entries
	 * @since 3.0
	 */
	public URL[] getRemoteAntURLs() {
		List result = new ArrayList(30);
		if (antHomeEntries != null) {
			for (int i = 0; i < antHomeEntries.length; i++) {
				IAntClasspathEntry entry = antHomeEntries[i];
				result.add(entry.getEntryURL());
			}
		}
		URL remote= getRemoteAntURL();
		if (remote != null) {
			result.add(remote);
		}
		
		IAntClasspathEntry entry= getToolsJarEntry();
		if (entry != null) {
			result.add(entry.getEntryURL());
		}
		return (URL[]) result.toArray(new URL[result.size()]);
	}
}