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
package org.eclipse.debug.internal.ui.launchConfigurations;

 
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.Pair;
import org.eclipse.debug.ui.ILaunchFilter;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;


/**
 * Proxy to a launch shortcut extension
 */
public class LaunchShortcutExtension implements ILaunchShortcut {
	
	private ImageDescriptor fImageDescriptor = null;
	private List fPerspectives = null;
	private ILaunchShortcut fDelegate = null;
	private Set fModes = null;
	private ILaunchFilter fLaunchFilter = null;
	private /* <Pair> */ List fFilters = null;
	
	/**
	 * The configuration element defining this tab.
	 */
	private IConfigurationElement fConfig;
	private /* <Pair> */ List fContextLabels;
	
	/**
	 * Constructs a launch configuration tab extension based
	 * on the given configuration element
	 * 
	 * @param element the configuration element defining the
	 *  attribtues of this launch configuration tab extension
	 * @return a new launch configuration tab extension
	 */
	public LaunchShortcutExtension(IConfigurationElement element) {
		setConfigurationElement(element);
	}
	
	/**
	 * Sets the configuration element that defines the attributes
	 * for this extension.
	 * 
	 * @param element configuration element
	 */
	private void setConfigurationElement(IConfigurationElement element) {
		fConfig = element;
	}
	
	/**
	 * Returns the configuration element that defines the attributes
	 * for this extension.
	 * 
	 * @param configuration element that defines the attributes
	 *  for this launch configuration tab extension
	 */
	public IConfigurationElement getConfigurationElement() {
		return fConfig;
	}
	
	/**
	 * Returns the label of this shortcut
	 * 
	 * @return the label of this shortcut, or <code>null</code> if not
	 *  specified
	 */
	public String getLabel() {
		return getConfigurationElement().getAttribute("label"); //$NON-NLS-1$
	}
	
	/**
	 * Returns the name filter of this shortcut or <code>null</code>
	 * 
	 * @return the name filter of this shortcut, or <code>null</code> if not
	 *  specified
	 */
	public String getNameFilter() {
		return getConfigurationElement().getAttribute("nameFilter"); //$NON-NLS-1$
	}
	
	/**
	 * Returns the contextual launch label of this shortcut
	 * 
	 * @return the contextual label of this shortcut, or <code>null</code> if not
	 *  specified
	 */
	public String getContextLabel(String mode) {
		// remember the list of context labels for this shortcut
		if (fContextLabels == null) {
			IConfigurationElement[] labels = getConfigurationElement().getChildren("contextLabel"); //$NON-NLS-1$
			fContextLabels = new ArrayList(labels.length);
			for (int i = 0; i < labels.length; i++) {
				fContextLabels.add(new Pair(labels[i].getAttribute("mode"), //$NON-NLS-1$
						labels[i].getAttribute("label"))); //$NON-NLS-1$
			}
		}
		// pick out the first occurance of the "name" bound to "mode"
		Iterator iter = fContextLabels.iterator();
		while (iter.hasNext()) {
			Pair p = (Pair) iter.next();
			if (p.firstAsString().equals(mode)) {
				return p.secondAsString();
			}
		}
		return LaunchConfigurationsMessages.getString("LaunchShortcutExtension.21"); //$NON-NLS-1$
	}
	
	/**
	 * Returns the filter class of this shortcut.
	 * 
	 * @return the filter class of this shortcut., or <code>null</code> if not
	 *  specified
	 */
	public ILaunchFilter getFilterClass() {
		if (fLaunchFilter == null) {
			try {
				// The underlying code logs an error if the filterClass is missing,
				// even though the attribute is optional, so check for existence first.
				if (fConfig.getAttribute("filterClass") != null) { //$NON-NLS-1$
					Object object = fConfig.createExecutableExtension("filterClass"); //$NON-NLS-1$
					if (object instanceof ILaunchFilter) {
						fLaunchFilter = (ILaunchFilter) object;
					}
				}
			} catch (CoreException e) {
				// silently ignore because filterClass is optional
				// DebugUIPlugin.errorDialog(DebugUIPlugin.getShell(), LaunchConfigurationsMessages.getString("LaunchShortcutExtension.Error_4"), LaunchConfigurationsMessages.getString("LaunchShortcutExtension.Unable_to_use_launch_shortcut_5"), e.getStatus()); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return fLaunchFilter;
	}
	
	/**
	 * Returns all of the filter elements of this shortcut as a List of String Pairs.
	 * 
	 * @return all of the filter elements of this shortcut, or <code>null</code> if not
	 *  specified
	 */
	public /* <Pair> */ List getFilters() {
		if (fFilters == null) {
			IConfigurationElement[] filters = getConfigurationElement().getChildren("filter"); //$NON-NLS-1$
			fFilters = new ArrayList(filters.length);
			for (int i = 0; i < filters.length; i++) {
				fFilters.add(new Pair(filters[i].getAttribute("name"), //$NON-NLS-1$
						filters[i].getAttribute("value"))); //$NON-NLS-1$
			}
		}
		return fFilters;
	}
	/**
	 * Returns the id of this shortcut
	 * 
	 * @return the id of this shortcut, or <code>null</code> if not specified
	 */
	public String getId() {
		return getConfigurationElement().getAttribute("id"); //$NON-NLS-1$
	}
	
	/**
	 * Returns the path of the icon for this shortcut, or <code>null</code>
	 * if none.
	 * 
	 * @return the path of the icon for this shortcut, or <code>null</code>
	 * if none
	 */
	protected String getIconPath() {
		return getConfigurationElement().getAttribute("icon"); //$NON-NLS-1$
	}	
	
	/**
	 * Returns the identifier of the help context associated with this launch
	 * shortcut, or <code>null</code> if one was not specified.
	 * 
	 * @return the identifier of this launch shortcut's help context or
	 * <code>null</code>
	 * @since 2.1
	 */	
	public String getHelpContextId() {
		return getConfigurationElement().getAttribute("helpContextId"); //$NON-NLS-1$		
	}
	
	/**
	 * Returns the category of this shortcut
	 *
	 * @return the category of this shortcut, or <code>null</code> if not
	 *  specified
	 */
	public String getCategory() {
		return getConfigurationElement().getAttribute("category"); //$NON-NLS-1$
	}	
	
	/**
	 * Returns the image for this shortcut, or <code>null</code> if none
	 * 
	 * @return the image for this shortcut, or <code>null</code> if none
	 */
	public ImageDescriptor getImageDescriptor() {
		if (fImageDescriptor == null) {
			URL iconURL = getConfigurationElement().getDeclaringExtension().getDeclaringPluginDescriptor().getInstallURL();
			String iconPath = getIconPath();
			try {
				iconURL = new URL(iconURL, iconPath);
				fImageDescriptor = ImageDescriptor.createFromURL(iconURL);
			} catch (MalformedURLException e) {
				DebugUIPlugin.log(e);
			}
		}
		return fImageDescriptor;
	}
	
	/**
	 * Returns the perspectives this shortcut is registered for.
	 * 
	 * @return list of Strings representing perspective identifiers 
	 */
	public List getPerspectives() {
		if (fPerspectives == null) {
			IConfigurationElement[] perspectives = getConfigurationElement().getChildren("perspective"); //$NON-NLS-1$
			fPerspectives = new ArrayList(perspectives.length);
			for (int i = 0; i < perspectives.length; i++) {
				fPerspectives.add(perspectives[i].getAttribute("id")); //$NON-NLS-1$
			}
		}
		return fPerspectives;
	}
	
	/**
	 * Returns this shortcut's delegate, or <code>null</code> if none
	 * 
	 * @return this shortcut's delegate, or <code>null</code> if none
	 */
	protected ILaunchShortcut getDelegate() {
		if (fDelegate == null) {
			try {
				fDelegate = (ILaunchShortcut)fConfig.createExecutableExtension("class"); //$NON-NLS-1$
			} catch (CoreException e) {
				DebugUIPlugin.errorDialog(DebugUIPlugin.getShell(), LaunchConfigurationsMessages.getString("LaunchShortcutExtension.Error_4"), LaunchConfigurationsMessages.getString("LaunchShortcutExtension.Unable_to_use_launch_shortcut_5"), e.getStatus()); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return fDelegate;
	}
	
	/**
	 * @see ILaunchShortcut#launch(IEditorPart, String)
	 */
	public void launch(IEditorPart editor, String mode) {
		ILaunchShortcut shortcut = getDelegate();
		if (shortcut != null) {
			shortcut.launch(editor, mode);
		}
	}

	/**
	 * @see ILaunchShortcut#launch(ISelection, String)
	 */
	public void launch(ISelection selection, String mode) {
		ILaunchShortcut shortcut = getDelegate();
		if (shortcut != null) {
			shortcut.launch(selection, mode);
		}		
	}
	
	/**
	 * Returns the set of modes this shortcut supports.
	 * 
	 * @return the set of modes this shortcut supports
	 */
	public Set getModes() {
		if (fModes == null) {
			String modes= getConfigurationElement().getAttribute("modes"); //$NON-NLS-1$
			if (modes == null) {
				return new HashSet(0);
			}
			StringTokenizer tokenizer= new StringTokenizer(modes, ","); //$NON-NLS-1$
			fModes = new HashSet(tokenizer.countTokens());
			while (tokenizer.hasMoreTokens()) {
				fModes.add(tokenizer.nextToken().trim());
			}
		}
		return fModes;
	}	

}

