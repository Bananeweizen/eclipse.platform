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
package org.eclipse.debug.internal.ui.preferences;

 
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.ui.AbstractDebugView;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.IDebugView;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;

public class DebugActionGroupsManager implements IMenuListener {
	
	// constants indicating action locations
	public static final int TYPE_TOOLBAR = 0;
	public static final int TYPE_VIEW_MENU = 1;
	public static final int TYPE_CONTEXT_MENU = 2;
	
	protected List fDebugViews= new ArrayList(6);
	protected Map fDebugViewsWithMenu= new HashMap(6);
	protected Map fDebugActionGroups;
	protected Map fDebugActionGroupActionIds;
	protected Map fDebugActionGroupActions = new HashMap();
	
	protected static DebugActionGroupsManager fgManager;

	private DebugActionGroupsManager() {
		//@see getDefault()
	}
	
	/**
	 * Returns the debug action groups manager
	 */
	public static DebugActionGroupsManager getDefault() {
		if (fgManager == null) {
			fgManager = new DebugActionGroupsManager();
			fgManager.startup();
		}
		return fgManager;
	}
	
	/**
	 * Returns whether the singleton instance of the manager exists
	 */
	public static boolean defaultExists() {
		return fgManager != null;
	}
	
	/**
	 * Called by the debug ui plug-in on startup.
	 */
	public void startup() {
		initialize();
	}

	/**
	 * Called by the debug ui plug-in on shutdown.
	 */
	public void shutdown() {
		for (Iterator iterator = fDebugActionGroupActions.values().iterator(); iterator.hasNext();) {
			List actions= (List)iterator.next();
			for (Iterator itr = actions.iterator(); itr.hasNext();) {
				DebugActionGroupAction action = (DebugActionGroupAction) itr.next();
				action.dispose();
			}
		}
	}

	private List persistedEnabledActionGroups() {

		String enabled= DebugUIPlugin.getDefault().getPreferenceStore().getString(IDebugPreferenceConstants.PREF_ENABLED_DEBUG_ACTION_GROUPS);
		if (enabled != null) {
			return parseList(enabled);
		}
		return Collections.EMPTY_LIST;
	}
	
	private List persistedDisabledActionGroups() {

		String enabled= DebugUIPlugin.getDefault().getPreferenceStore().getString(IDebugPreferenceConstants.PREF_DISABLED_DEBUG_ACTION_GROUPS);
		if (enabled != null) {
			return parseList(enabled);
		}
		return Collections.EMPTY_LIST;
	}
	
	/**
	 * Create the mapping of actions to debug action groups
	 */
	private void initialize() {
		IPluginDescriptor descriptor = DebugUIPlugin.getDefault().getDescriptor();
		IExtensionPoint extensionPoint =
			descriptor.getExtensionPoint(IDebugUIConstants.EXTENSION_POINT_DEBUG_ACTION_GROUPS);
		IConfigurationElement[] infos = extensionPoint.getConfigurationElements();
		if (infos.length == 0) {
			return;
		}
		
		fDebugActionGroupActionIds = new HashMap();
		fDebugActionGroups = new HashMap(10);
		List userEnabledGroups= persistedEnabledActionGroups();
		List userDisabledGroups= persistedDisabledActionGroups();
		
		for (int i = 0; i < infos.length; i++) {
			IConfigurationElement configurationElement = infos[i];
			String id = configurationElement.getAttribute("id"); //$NON-NLS-1$
			String visible = configurationElement.getAttribute("visible"); //$NON-NLS-1$
			boolean isVisible = true;
			if (visible != null) {
				isVisible = Boolean.valueOf(visible).booleanValue();
			}
			if (!isVisible && userEnabledGroups.contains(id)) {
				isVisible= true;
			} else if (isVisible && userDisabledGroups.contains(id)) {
				isVisible= false;
			}
			
			String name = configurationElement.getAttribute("name"); //$NON-NLS-1$

			if (id != null && name != null) {
				if (fDebugActionGroups.get(id) != null) {
					//duplicate id..report error and ignore extension
					DebugUIPlugin.logErrorMessage(MessageFormat.format(DebugPreferencesMessages.getString("DebugActionGroupsManager.Duplicate_action_group_id"), new String[]{id})); //$NON-NLS-1$
					continue;
				}
				
				IConfigurationElement[] children = configurationElement.getChildren();
				if (children.length == 0) {
					//empty action group
					continue;
				}
				DebugActionGroup viewActionSet = new DebugActionGroup(id, name, isVisible);
				fDebugActionGroups.put(id, viewActionSet);
				for (int j = 0; j < children.length; j++) {
					IConfigurationElement actionElement = children[j];
					String actionId = actionElement.getAttribute("id"); //$NON-NLS-1$
					if (actionId != null) {
						viewActionSet.add(actionId);
						fDebugActionGroupActionIds.put(actionId, viewActionSet.getId());
					}
				}

			} else {
				// invalid debug action group
				String errorId= ""; //$NON-NLS-1$
				if (id != null) {
					errorId= ": "  + id; //$NON-NLS-1$
				}
				DebugUIPlugin.logErrorMessage(DebugPreferencesMessages.getString("DebugActionGroupsManager.Improperly_specified_debug_action_group_4") + errorId); //$NON-NLS-1$
			}
		}
	}
	
	/**
	 * Updates the debug view groups for all registered views.
	 */
	public void updateDebugActionGroups() {
		for (Iterator iterator = fDebugViews.iterator(); iterator.hasNext();) {
			IDebugView view = (IDebugView) iterator.next();
			updateDebugActionGroups(view);
		}
	}

	protected void updateDebugActionGroups(IViewPart viewPart) {
		IDebugView debugView= (IDebugView)viewPart.getAdapter(IDebugView.class);
		if (debugView == null) {
			return;
		}
		
		IActionBars actionBars = viewPart.getViewSite().getActionBars();
		IToolBarManager toolBarManager = actionBars.getToolBarManager();
		boolean refresh = false;
		if (processContributionItems(toolBarManager.getItems(), viewPart.getTitle(), viewPart.getSite().getId(),TYPE_TOOLBAR)) {
			toolBarManager.markDirty();
			refresh = true;
		}
		IMenuManager menuManager = actionBars.getMenuManager();
		if (processContributionItems(menuManager.getItems(), viewPart.getTitle(), viewPart.getSite().getId(), TYPE_VIEW_MENU)) {
			menuManager.markDirty();
			refresh = true;
		}
		
		if (refresh) {
			actionBars.updateActionBars();
		}
	}
	
	protected boolean processContributionItems(IContributionItem[] items, String viewName, String viewId, int type) {
		boolean visibilityChanged = false;
		for (int i = 0; i < items.length; i++) {
			IContributionItem iContributionItem = items[i];
			if (!(iContributionItem instanceof ActionContributionItem)) {
				continue;
			}
			ActionContributionItem item= (ActionContributionItem)iContributionItem;
			String id = item.getId();
			if (id != null) {
				String viewActionSetId = (String) fDebugActionGroupActionIds.get(id);
				if (viewActionSetId != null) {
					DebugActionGroup actionSet = (DebugActionGroup) fDebugActionGroups.get(viewActionSetId);
					if (actionSet != null) {
						iContributionItem.setVisible(actionSet.isVisible());
						visibilityChanged = true;
						DebugActionGroupAction action= new DebugActionGroupAction(id, item.getAction().getText(), viewName, viewId, item.getAction().getImageDescriptor(), type);
						List actions= (List)fDebugActionGroupActions.get(id);
						if (actions == null) {
							actions= new ArrayList(1);
							actions.add(action);
							fDebugActionGroupActions.put(id, actions);
						} else if (!actions.contains(action)) {
							actions.add(action);
						}
					}
				}
			}
		}
		return visibilityChanged;
	}
	
	/**
	 * Adds this view to the collections of views that are
	 * affected by debug action groups.  Has no effect if the view was
	 * previously registered.
	 */
	public void registerView(final IDebugView view) {
		if (fDebugActionGroupActionIds == null || fDebugViews.contains(view)) {
			return;
		}
		List menus= null;
		if (view instanceof AbstractDebugView) {
			menus= ((AbstractDebugView)view).getContextMenuManagers();
		}
		if (menus == null) {
			menus= new ArrayList(1);
			menus.add(view.getContextMenuManager());
		}
		Iterator itr= menus.iterator();
		while (itr.hasNext()) {
			IMenuManager menu = (IMenuManager) itr.next();
			if (menu != null) {
				menu.addMenuListener(this);
			}
			
		}
		
		final List contextMenus= menus;
		final Display display= view.getSite().getPage().getWorkbenchWindow().getShell().getDisplay();
		if (display != null && !display.isDisposed()) {
			display.asyncExec(new Runnable() {
				public void run() {
					if (!display.isDisposed()) {
						updateDebugActionGroups(view);
						fDebugViews.add(view);
						Iterator iter= contextMenus.iterator();
						while (iter.hasNext()) {
							IMenuManager menu = (IMenuManager)iter.next();
							if (menu != null) {
								fDebugViewsWithMenu.put(menu, view);
								//fake a showing of the context menu to get a 
								//look at all of the items in the menu
								Menu swtMenu= ((MenuManager)menu).getMenu();
								if (!swtMenu.isDisposed()) {
									swtMenu.notifyListeners(SWT.Show, new Event());
									swtMenu.notifyListeners(SWT.Hide, new Event());
								}
							}
						}
					}
				}
			});
		}
	}
	
	/**
	 * Removes this view from the collections of views that are
	 * affected by debug action groups.  Has no effect if the view was
	 * not previously registered.
	 */
	public void deregisterView(IDebugView view) {
		if (fDebugActionGroupActionIds != null && fDebugViews.remove(view)) {
			List managers= null;
			if (view instanceof AbstractDebugView) {
				managers= ((AbstractDebugView)view).getContextMenuManagers();
			}
			if (managers == null) {
				managers= new ArrayList(1);
				managers.add(view.getContextMenuManager());
			}
			Iterator mitr= managers.iterator();
			while (mitr.hasNext()) {
				IMenuManager manager = (IMenuManager)mitr.next();
				
				if (manager != null) {
					manager.removeMenuListener(this);
					fDebugViewsWithMenu.remove(manager);
				}
			}
			Collection actionCollections= fDebugActionGroupActions.values();
			List removed= new ArrayList();
			for (Iterator iterator = actionCollections.iterator(); iterator.hasNext();) {
				List actions= (List)iterator.next();
				for (Iterator itr = actions.iterator(); itr.hasNext();) {	
					DebugActionGroupAction action = (DebugActionGroupAction) itr.next();
					if (action.getViewId().equals(view.getSite().getId())) {
						removed.add(action.getId());
						action.dispose();
					}
				}
			}
			
			for (Iterator iterator = removed.iterator(); iterator.hasNext();) {
				String actionId= (String)iterator.next();
				fDebugActionGroupActions.remove(actionId);
			}
		}
	}
	
	/**
	 * @see IMenuListener#menuAboutToShow(IMenuManager)
	 */
	public void menuAboutToShow(IMenuManager manager) {
		IDebugView view= (IDebugView)fDebugViewsWithMenu.get(manager);
		if (view != null) {
			String viewName= view.getTitle();
			String viewId= view.getSite().getId();
			processContributionItems(manager.getItems(), viewName, viewId, TYPE_CONTEXT_MENU);
		}
	}
	
	/**
	 * Debug view action group extensions
	 */
	protected class DebugActionGroup {

		private String fId;
		private boolean fVisible;
		private String fName;
		private List fActionIds = new ArrayList();

		protected DebugActionGroup(String id, String name, boolean visible) {
			fId = id;
			fVisible = visible;
			fName = name;
		}

		/**
		 * @see Object#hashCode()
		 */
		public int hashCode() {
			return fId.hashCode();
		}

		/**
		 * @see Object#equals(Object)
		 */
		public boolean equals(Object obj) {
			if (obj instanceof DebugActionGroup) {
				DebugActionGroup s = (DebugActionGroup) obj;
				return fId == s.fId;
			}
			return false;
		}

		protected void add(String actionId) {
			fActionIds.add(actionId);
		}

		protected String getName() {
			return fName;
		}

		protected boolean isVisible() {
			return fVisible;
		}

		protected void setVisible(boolean visible) {
			fVisible = visible;
		}

		protected List getActionIds() {
			return fActionIds;
		}
		
		protected String getId() {
			return fId;
		}
	}
	
	/**
	 * Debug view action extensions
	 */
	protected class DebugActionGroupAction {

		private String fId;
		private String fName;
		private String fViewName;
		private String fViewId;
		private ImageDescriptor fImageDescriptor;
		private Image fImage;
		private int fType;

		protected DebugActionGroupAction(String id, String name, String viewName, String viewId, ImageDescriptor imageDescriptor, int type) {
			fType = type;
			fId = id;
			fName = cleanName(name);
			fImageDescriptor= imageDescriptor;
			fViewName= viewName;
			fViewId= viewId;
		}

		/**
		 * @see Object#hashCode()
		 */
		public int hashCode() {
			return fId.hashCode() | fViewId.hashCode() | fType;
		}

		/**
		 * @see Object#equals(Object)
		 */
		public boolean equals(Object obj) {
			if (obj instanceof DebugActionGroupAction) {
				DebugActionGroupAction s = (DebugActionGroupAction) obj;
				return getId() == s.getId() && getViewId() == s.getViewId() && fType == s.fType;
			}
			return false;
		}

		protected String getName() {
			StringBuffer buff= new StringBuffer(fName);
			buff.append(" ("); //$NON-NLS-1$
			buff.append(fViewName);
			buff.append(DebugPreferencesMessages.getString("DebugActionGroupsManager._view__6")); //$NON-NLS-1$
			buff.append(getDescriptor());
			buff.append(')');
			return buff.toString();
		}

		protected Image getImage() {
			if (fImage == null && fImageDescriptor != null) {
				fImage= fImageDescriptor.createImage(true);
			}
			return fImage;
		}
		
		protected void dispose() {
			if (fImage != null) {
				fImage.dispose();
			}
		}
		
		protected String getDescriptor() {
			switch (fType) {
				case TYPE_TOOLBAR:
					return DebugPreferencesMessages.getString("DebugActionGroupsManager.toolbar_7"); //$NON-NLS-1$
				case TYPE_CONTEXT_MENU:
					return DebugPreferencesMessages.getString("DebugActionGroupsManager.context_menu_8"); //$NON-NLS-1$
				case TYPE_VIEW_MENU:
				    return DebugPreferencesMessages.getString("DebugActionGroupsManager.pull-down_menu_1"); //$NON-NLS-1$
			}
			return ""; //$NON-NLS-1$
		}
		
		/**
		 * Removes the '&' accelerator indicator from a label, if any.
		 * Removes the hot key indicator, if any.
		 */	
		protected String cleanName(String name) {
			int i = name.indexOf('@');
			if (i >= 0) {
				name = name.substring(0, i);
			}
			i = name.indexOf('&');
			if (i >= 0) {
				name = name.substring(0, i) + name.substring(i+1);
			}
		
			return name;
		}
		
		protected String getId() {
			return fId;
		}
		
		protected String getViewId() {
			return fViewId;
		}
	}
	
	/**
	 * Parses the comma separated string into list of strings
	 * 
	 * @return list
	 */
	protected List parseList(String listString) {
		List list = new ArrayList(10);
		StringTokenizer tokenizer = new StringTokenizer(listString, ","); //$NON-NLS-1$
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			list.add(token);
		}
		return list;
	}
	
	/**
	 * Serializes the array of strings into one comma
	 * separated string.
	 * 
	 * @param list array of strings
	 * @return a single string composed of the given list
	 */
	protected String serializeList(List list) {
		if (list == null) {
			return ""; //$NON-NLS-1$
		}
		StringBuffer buffer = new StringBuffer();
		int i= 0;
		for (Iterator iterator = list.iterator(); iterator.hasNext(); i++) {
			String element = (String) iterator.next();
			if (i > 0) {
				buffer.append(',');
			}
			buffer.append(element);
		}
		return buffer.toString();
	}	
}
