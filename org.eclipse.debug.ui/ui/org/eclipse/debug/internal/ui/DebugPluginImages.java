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


import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;

/**
 * The images provided by the debug plugin.
 */
public class DebugPluginImages {

	/** 
	 * The image registry containing <code>Image</code>s.
	 */
	private static ImageRegistry imageRegistry;
	
	/**
	 * A table of all the <code>ImageDescriptor</code>s.
	 */
	private static HashMap imageDescriptors;

	private static final String ATTR_LAUNCH_CONFIG_TYPE_ICON = "icon"; //$NON-NLS-1$
	private static final String ATTR_LAUNCH_CONFIG_TYPE_ID = "configTypeID"; //$NON-NLS-1$
	
	/* Declare Common paths */
	private static URL ICON_BASE_URL= null;

	static {
		String pathSuffix = "icons/full/"; //$NON-NLS-1$
			
		try {
			ICON_BASE_URL= new URL(DebugUIPlugin.getDefault().getDescriptor().getInstallURL(), pathSuffix);
		} catch (MalformedURLException e) {
			// do nothing
		}
	}

	// Use IPath and toOSString to build the names to ensure they have the slashes correct
	private final static String CTOOL= "ctool16/"; //basic colors - size 16x16 //$NON-NLS-1$
	private final static String LOCALTOOL= "clcl16/"; //basic colors - size 16x16 //$NON-NLS-1$
	private final static String DLCL= "dlcl16/"; //disabled - size 16x16 //$NON-NLS-1$
	private final static String ELCL= "elcl16/"; //enabled - size 16x16 //$NON-NLS-1$
	private final static String OBJECT= "obj16/"; //basic colors - size 16x16 //$NON-NLS-1$
	private final static String WIZBAN= "wizban/"; //basic colors - size 16x16 //$NON-NLS-1$
	private final static String OVR= "ovr16/"; //basic colors - size 7x8 //$NON-NLS-1$
	private final static String VIEW= "cview16/"; // views //$NON-NLS-1$
	
	/**
	 * Declare all images
	 */
	private static void declareImages() {
		// Actions
		declareRegistryImage(IDebugUIConstants.IMG_ACT_DEBUG, CTOOL + "debug_exc.gif"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_ACT_RUN, CTOOL + "run_exc.gif"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_ACT_SYNCED, LOCALTOOL + "synced.gif"); //$NON-NLS-1$
		
		//menus
		declareRegistryImage(IDebugUIConstants.IMG_LCL_CHANGE_VARIABLE_VALUE, LOCALTOOL + "changevariablevalue_co.gif"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_DLCL_CHANGE_VARIABLE_VALUE, DLCL + "changevariablevalue_co.gif"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_ELCL_CHANGE_VARIABLE_VALUE, ELCL + "changevariablevalue_co.gif"); //$NON-NLS-1$
		
		declareRegistryImage(IDebugUIConstants.IMG_LCL_CONTENT_ASSIST, LOCALTOOL + "metharg_obj.gif"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_DLCL_CONTENT_ASSIST, DLCL + "metharg_obj.gif"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_ELCL_CONTENT_ASSIST, ELCL + "metharg_obj.gif"); //$NON-NLS-1$
		
		//Local toolbars
		declareRegistryImage(IDebugUIConstants.IMG_LCL_DETAIL_PANE, LOCALTOOL + "toggledetailpane_co.gif"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_LCL_DETAIL_PANE_UNDER, LOCALTOOL + "det_pane_under.gif"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_LCL_DETAIL_PANE_RIGHT, LOCALTOOL + "det_pane_right.gif"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_LCL_DETAIL_PANE_HIDE, LOCALTOOL + "det_pane_hide.gif"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_LCL_LOCK, LOCALTOOL + "lock_co.gif"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_LCL_TYPE_NAMES, LOCALTOOL + "tnames_co.gif"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_LCL_DISCONNECT, LOCALTOOL + "disconnect_co.gif"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_LCL_REMOVE_ALL, LOCALTOOL + "rem_all_co.gif"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_LCL_SHOW_LOGICAL_STRUCTURE, LOCALTOOL + "var_cntnt_prvdr.gif"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_LCL_AVAILABLE_LOGICAL_STRUCTURES, LOCALTOOL + "var_avail_str.gif"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_LCL_COLLAPSE_ALL, LOCALTOOL + "collapseall.gif"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_LCL_TERMINATE, LOCALTOOL + "terminate_co.gif"); //$NON-NLS-1$
			
		// disabled local toolbars
		declareRegistryImage(IInternalDebugUIConstants.IMG_DLCL_DETAIL_PANE, DLCL + "toggledetailpane_co.gif"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_DLCL_DETAIL_PANE_UNDER, DLCL + "det_pane_under.gif"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_DLCL_DETAIL_PANE_RIGHT, DLCL + "det_pane_right.gif"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_DLCL_DETAIL_PANE_HIDE, DLCL + "det_pane_hide.gif"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_DLCL_LOCK, DLCL + "lock_co.gif"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_DLCL_TYPE_NAMES, DLCL + "tnames_co.gif"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_DLCL_SHOW_LOGICAL_STRUCTURE, DLCL + "var_cntnt_prvdr.gif"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_DLCL_AVAILABLE_LOGICAL_STRUCTURES, DLCL + "var_avail_str.gif"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_DLCL_COLLAPSE_ALL, DLCL + "collapseall.gif"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_DLCL_TERMINATE, DLCL + "terminate_co.gif"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_DLCL_REMOVE_ALL, DLCL + "rem_all_co.gif"); //$NON-NLS-1$
		
		// enabled local toolbars
		declareRegistryImage(IInternalDebugUIConstants.IMG_ELCL_DETAIL_PANE, ELCL + "toggledetailpane_co.gif"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_ELCL_DETAIL_PANE_UNDER, ELCL + "det_pane_under.gif"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_ELCL_DETAIL_PANE_RIGHT, ELCL + "det_pane_right.gif"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_ELCL_DETAIL_PANE_HIDE, ELCL + "det_pane_hide.gif"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_ELCL_LOCK, ELCL + "lock_co.gif"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_ELCL_TYPE_NAMES, ELCL + "tnames_co.gif"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_ELCL_SHOW_LOGICAL_STRUCTURE, ELCL + "var_cntnt_prvdr.gif"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_ELCL_AVAILABLE_LOGICAL_STRUCTURES, ELCL + "var_avail_str.gif"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_ELCL_COLLAPSE_ALL, ELCL + "collapseall.gif"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_ELCL_TERMINATE, ELCL + "terminate_co.gif"); //$NON-NLS-1$
		
		//Object
		declareRegistryImage(IDebugUIConstants.IMG_OBJS_LAUNCH_DEBUG, OBJECT + "ldebug_obj.gif"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_OBJS_LAUNCH_RUN, OBJECT + "lrun_obj.gif"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_OBJS_LAUNCH_RUN_TERMINATED, OBJECT + "terminatedlaunch_obj.gif"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_OBJS_DEBUG_TARGET, OBJECT + "debugt_obj.gif"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_OBJS_DEBUG_TARGET_SUSPENDED, OBJECT + "debugts_obj.gif"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_OBJS_DEBUG_TARGET_TERMINATED, OBJECT + "debugtt_obj.gif"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_OBJS_THREAD_RUNNING, OBJECT + "thread_obj.gif"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_OBJS_THREAD_SUSPENDED, OBJECT + "threads_obj.gif"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_OBJS_THREAD_TERMINATED, OBJECT + "threadt_obj.gif"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_OBJS_STACKFRAME, OBJECT + "stckframe_obj.gif"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_OBJS_STACKFRAME_RUNNING, OBJECT + "stckframe_running_obj.gif"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_OBJS_VARIABLE, OBJECT + "genericvariable_obj.gif"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_OBJS_BREAKPOINT, OBJECT + "brkp_obj.gif"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_OBJS_BREAKPOINT_DISABLED, OBJECT + "brkpd_obj.gif"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_OBJS_OS_PROCESS, OBJECT + "osprc_obj.gif"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_OBJS_OS_PROCESS_TERMINATED, OBJECT + "osprct_obj.gif"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_OBJS_EXPRESSION, OBJECT + "expression_obj.gif"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_OBJS_INSTRUCTION_POINTER_TOP, OBJECT + "inst_ptr_top.gif"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_OBJS_INSTRUCTION_POINTER, OBJECT + "inst_ptr.gif"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_OBJS_ARRAY_PARTITION, OBJECT + "arraypartition_obj.gif"); //$NON-NLS-1$
		
		// tabs
		declareRegistryImage(IInternalDebugUIConstants.IMG_OBJS_COMMON_TAB, OBJECT + "common_tab.gif"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_OBJS_REFRESH_TAB, OBJECT + "refresh_tab.gif"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_OBJS_PERSPECTIVE_TAB, OBJECT + "persp_tab.gif"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_OBJS_ENVIRONMENT, OBJECT + "environment_obj.gif"); //$NON-NLS-1$
		
		// Views
		declareRegistryImage(IDebugUIConstants.IMG_VIEW_BREAKPOINTS, VIEW + "breakpoint_view.gif"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_VIEW_EXPRESSIONS, VIEW + "watchlist_view.gif"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_VIEW_LAUNCHES, VIEW + "debug_view.gif"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_VIEW_VARIABLES, VIEW + "variable_view.gif"); //$NON-NLS-1$
		
		// Perspectives
		declareRegistryImage(IDebugUIConstants.IMG_PERSPECTIVE_DEBUG, VIEW + "debug_persp.gif"); //$NON-NLS-1$
		
		//Wizard Banners
		declareRegistryImage(IDebugUIConstants.IMG_WIZBAN_DEBUG, WIZBAN + "debug_wiz.gif"); //$NON-NLS-1$
		declareRegistryImage(IDebugUIConstants.IMG_WIZBAN_RUN, WIZBAN + "run_wiz.gif"); //$NON-NLS-1$
			
		// Overlays
		declareRegistryImage(IDebugUIConstants.IMG_OVR_ERROR, OVR + "error.gif"); //$NON-NLS-1$
		declareRegistryImage(IInternalDebugUIConstants.IMG_OVR_TRANSPARENT, OVR + "transparent.gif"); //$NON-NLS-1$		
				
		// launch configuration types
		IPluginDescriptor pluginDescriptor = DebugUIPlugin.getDefault().getDescriptor();
		IExtensionPoint extensionPoint= pluginDescriptor.getExtensionPoint(IDebugUIConstants.EXTENSION_POINT_LAUNCH_CONFIGURATION_TYPE_IMAGES);
		IConfigurationElement[] configElements= extensionPoint.getConfigurationElements();
		for (int i = 0; i < configElements.length; i++) {
			IConfigurationElement configElement = configElements[i];
			URL iconURL = configElement.getDeclaringExtension().getDeclaringPluginDescriptor().getInstallURL();
			String iconPath = configElement.getAttribute(ATTR_LAUNCH_CONFIG_TYPE_ICON);
			ImageDescriptor imageDescriptor = ImageDescriptor.getMissingImageDescriptor();
			try {
				iconURL = new URL(iconURL, iconPath);
				imageDescriptor = ImageDescriptor.createFromURL(iconURL);
			} catch (MalformedURLException mue) {
				DebugUIPlugin.log(mue);
			}
			String configTypeID = configElement.getAttribute(ATTR_LAUNCH_CONFIG_TYPE_ID);
			if (configTypeID == null) {
				// bug 12652
				configTypeID = configElement.getAttribute("type"); //$NON-NLS-1$
			}			
			imageRegistry.put(configTypeID, imageDescriptor);				
			imageDescriptors.put(configTypeID, imageDescriptor);
		}	
	}

	/**
	 * Declare an Image in the registry table.
	 * @param key 	The key to use when registering the image
	 * @param path	The path where the image can be found. This path is relative to where
	 *				this plugin class is found (i.e. typically the packages directory)
	 */
	private final static void declareRegistryImage(String key, String path) {
		ImageDescriptor desc= ImageDescriptor.getMissingImageDescriptor();
		try {
			desc= ImageDescriptor.createFromURL(makeIconFileURL(path));
		} catch (MalformedURLException me) {
			DebugUIPlugin.log(me);
		}
		imageRegistry.put(key, desc);
		imageDescriptors.put(key, desc);
	}
	
	/**
	 * Returns the ImageRegistry.
	 */
	public static ImageRegistry getImageRegistry() {
		if (imageRegistry == null) {
			initializeImageRegistry();
		}
		return imageRegistry;
	}

	/**
	 *	Initialize the image registry by declaring all of the required
	 *	graphics. This involves creating JFace image descriptors describing
	 *	how to create/find the image should it be needed.
	 *	The image is not actually allocated until requested.
	 *
	 * 	Prefix conventions
	 *		Wizard Banners			WIZBAN_
	 *		Preference Banners		PREF_BAN_
	 *		Property Page Banners	PROPBAN_
	 *		Color toolbar			CTOOL_
	 *		Enable toolbar			ETOOL_
	 *		Disable toolbar			DTOOL_
	 *		Local enabled toolbar	ELCL_
	 *		Local Disable toolbar	DLCL_
	 *		Object large			OBJL_
	 *		Object small			OBJS_
	 *		View 					VIEW_
	 *		Product images			PROD_
	 *		Misc images				MISC_
	 *
	 *	Where are the images?
	 *		The images (typically gifs) are found in the same location as this plugin class.
	 *		This may mean the same package directory as the package holding this class.
	 *		The images are declared using this.getClass() to ensure they are looked up via
	 *		this plugin class.
	 *	@see JFace's ImageRegistry
	 */
	public static ImageRegistry initializeImageRegistry() {
		imageRegistry= new ImageRegistry(DebugUIPlugin.getStandardDisplay());
		imageDescriptors = new HashMap(30);
		declareImages();
		return imageRegistry;
	}

	/**
	 * Returns the <code>Image<code> identified by the given key,
	 * or <code>null</code> if it does not exist.
	 */
	public static Image getImage(String key) {
		return getImageRegistry().get(key);
	}
	
	/**
	 * Returns the <code>ImageDescriptor<code> identified by the given key,
	 * or <code>null</code> if it does not exist.
	 */
	public static ImageDescriptor getImageDescriptor(String key) {
		if (imageDescriptors == null) {
			initializeImageRegistry();
		}
		return (ImageDescriptor)imageDescriptors.get(key);
	}
	
	private static URL makeIconFileURL(String iconPath) throws MalformedURLException {
		if (ICON_BASE_URL == null) {
			throw new MalformedURLException();
		}
			
		return new URL(ICON_BASE_URL, iconPath);
	}
}


