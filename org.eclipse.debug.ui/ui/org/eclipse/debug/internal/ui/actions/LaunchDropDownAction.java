package org.eclipse.debug.internal.ui.actions;

/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp.  All rights reserved.
This file is made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html
**********************************************************************/
 
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.ILaunchHistoryChangedListener;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationHistoryElement;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.SubContributionItem;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IActionDelegateWithEvent;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowPulldownDelegate2;

/**
 * Superclass of run & debug pulldown actions.
 */
public abstract class LaunchDropDownAction implements IWorkbenchWindowPulldownDelegate2,
														  IActionDelegateWithEvent,
														  ILaunchHistoryChangedListener {
	
	private ExecutionAction fLaunchAction;
	
	private Menu fCreatedMenu;
	
	private DebugWithConfigurationAction fDebugWithAction;
	private RunWithConfigurationAction fRunWithAction;
	
	/**
	 * This action delegate's proxy.
	 */
	private IAction fActionProxy;
	
	/**
	 * The tooltip that was specified in XML for this action.
	 */
	private String fOriginalTooltip;
	/**
	 * Flag which determines if the menu has actually been shown by the user.
	 * Until this flag is set, don't compute the launch config history and favorites.	 */
	private boolean fMenuShown= false;
	
	protected void setActionProxy(IAction action) {
		fActionProxy = action;
	}

	protected IAction getActionProxy() {
		return fActionProxy;
	}
	
	public LaunchDropDownAction(ExecutionAction launchAction) {
		setLaunchAction(launchAction);		
	}

	private void createMenuForAction(Menu parent, IAction action, int count) {
		if (count > 0) {
			StringBuffer label= new StringBuffer();
			if (count < 10) {
				//add the numerical accelerator
				label.append('&');
				label.append(count);
				label.append(' ');
			}
			label.append(action.getText());
			action.setText(label.toString());
		}
		ActionContributionItem item= new ActionContributionItem(action);
		item.fill(parent, -1);
	}

	/**
	 * Initialize this action so that it can dynamically set its tooltip.  Also set the enabled state
	 * of the underlying action based on whether there are any registered launch configuration types that 
	 * understand how to launch in the mode of this action.
	 */
	protected void initialize(IAction action) {
		DebugUIPlugin.getLaunchConfigurationManager().addLaunchHistoryListener(this);
		fOriginalTooltip = action.getToolTipText();
		setActionProxy(action);
		updateTooltip();	
		
		action.setEnabled(existsConfigTypesForMode());	
	}
	
	/**
	 * Return <code>true</code> if there are any registered launch configuration types for
	 * the mode of this action, <code>false</code> otherwise.
	 */
	protected boolean existsConfigTypesForMode() {
		ILaunchConfigurationType[] configTypes = getLaunchManager().getLaunchConfigurationTypes();
		for (int i = 0; i < configTypes.length; i++) {
			ILaunchConfigurationType configType = configTypes[i];
			if (configType.supportsMode(getMode())) {
				return true;
			}
		}		
		return false;
	}
	
	/**
	 * Updates this action's tooltip to correspond to the most recent launch.
	 */
	protected void updateTooltip() {
		LaunchConfigurationHistoryElement lastLaunched = getLastLaunch();
		String tooltip= fOriginalTooltip;
		if (lastLaunched != null) {
			tooltip= getLastLaunchPrefix() + lastLaunched.getLaunchConfiguration().getName();
		} else {
			tooltip= getStaticTooltip();
		}
		getActionProxy().setToolTipText(tooltip);
	}
	
	/**
	 * @see ILaunchHistoryChangedListener#launchHistoryChanged()
	 */
	public void launchHistoryChanged() {
		updateTooltip();
	}

	/**
	 * @see IWorkbenchWindowActionDelegate#dispose()
	 */
	public void dispose() {
		if (getCreatedMenu() != null) {
			getCreatedMenu().dispose();
		}
		DebugUIPlugin.getLaunchConfigurationManager().removeLaunchHistoryListener(this);
	}
	
	/**
	 * Return the last launch that occurred in the workspace.
	 */
	protected LaunchConfigurationHistoryElement getLastLaunch() {
		return DebugUIPlugin.getLaunchConfigurationManager().getLastLaunch();
	}

	/**
	 * @see IWorkbenchWindowPulldownDelegate#getMenu(Control)
	 */
	public Menu getMenu(Control parent) {
		if (getCreatedMenu() != null) {
			getCreatedMenu().dispose();
		}
		setCreatedMenu(new Menu(parent));
		return createMenu(getCreatedMenu());
	}
	
	/**
	 * @see IMenuCreator#getMenu(Menu)
	 */
	public Menu getMenu(Menu parent) {
		if (getCreatedMenu() != null) {
			getCreatedMenu().dispose();
		}
		setCreatedMenu(new Menu(parent));
		return createMenu(getCreatedMenu());
	}

	/**
	 * Create the drop-down menu based on whether the config style pref is set
	 */
	protected Menu createMenu(Menu menu) {
		if (fMenuShown) {
			LaunchConfigurationHistoryElement[] historyList= getHistory();
			LaunchConfigurationHistoryElement[] favoriteList = getFavorites();		
			
			// Add any favorites
			int total = 0;
			for (int i = 0; i < favoriteList.length; i++) {
				LaunchConfigurationHistoryElement launch= favoriteList[i];
				RelaunchHistoryLaunchAction newAction= new RelaunchHistoryLaunchAction(launch);
				createMenuForAction(menu, newAction, total + 1);
				total++;
			}		
			
			// Separator between favorites and history
			if (favoriteList.length > 0) {
				if (historyList.length > 0) {
					new MenuItem(menu, SWT.SEPARATOR);
				} else {
					createTopSeparator(menu);
				}
			}
			
			// Add history launches next
			for (int i = 0; i < historyList.length; i++) {
				LaunchConfigurationHistoryElement launch= historyList[i];
				RelaunchHistoryLaunchAction newAction= new RelaunchHistoryLaunchAction(launch);
				createMenuForAction(menu, newAction, total+1);
				total++;
			}
			
			// Separator between history and common actions
			if (historyList.length > 0) {
				createTopSeparator(menu);
			}
		}

		// Add the actions to bring up the dialog 
		if (getLaunchAction() != null) {
			// Cascading menu for config type 'shortcuts'
			if (getMode() == ILaunchManager.DEBUG_MODE) {
				createMenuForAction(menu, getDebugWithAction(), -1);
			} else {
				createMenuForAction(menu, getRunWithAction(), -1);				
			}
			
			// Add non-shortcutted access to the launch configuration dialog
			OpenLaunchConfigurationsAction action = null;
			if (getMode() == ILaunchManager.DEBUG_MODE) {
				action = new OpenDebugConfigurations();
			} else {
				action = new OpenRunConfigurations();
			}
			createMenuForAction(menu, action, -1);
		}
		return menu;		
	}
	
	/**
	 * Create a separator at the top of the history/favorites area.  This method may
	 * be overridden to prevent a separator from being placed at the top.
	 */
	protected void createTopSeparator(Menu menu) {
		new MenuItem(menu, SWT.SEPARATOR);
	}
	
	/**
	 * @see runWithEvent(IAction, Event)
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
	}
	
	/**
	 * @see IActionDelegateWithEvent#runWithEvent(IAction, Event)
	 */
	public void runWithEvent(IAction action, Event event) {
		getLaunchAction().runWithEvent(action, event);
	}

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection){
		if (fActionProxy == null) {
			initialize(action);
		} 
	}
	
	/**
	 * Returns the launch manager.
	 * 
	 * @return the launch manager
	 */
	protected ILaunchManager getLaunchManager() {
		return DebugPlugin.getDefault().getLaunchManager();
	}

	/**
	 * @see IWorkbenchWindowActionDelegate#init(IWorkbenchWindow)
	 */
	public void init(IWorkbenchWindow window){
	}
	
	/**
	 * Returns an array of previous launches applicable to this drop down.
	 */
	public abstract LaunchConfigurationHistoryElement[] getHistory();
	
	/**
	 * Returns an array of favorites applicable to this drop down.
	 */
	public abstract LaunchConfigurationHistoryElement[] getFavorites();	
	
	/**
	 * Returns the mode (e.g., 'run' or 'debug') of this drop down.
	 */
	public abstract String getMode();
	
	/**
	 * Return the String to use as the first part of the tooltip for this action
	 * when there is no launch history.
	 */
	protected abstract String getStaticTooltip();
	
	/**
	 * Return the String to use as the first part of the tooltip for this action
	 * when there is a launch history.
	 */
	protected abstract String getLastLaunchPrefix();
	
	protected ExecutionAction getLaunchAction() {
		return fLaunchAction;
	}

	protected void setLaunchAction(ExecutionAction launchAction) {
		fLaunchAction = launchAction;
	}
	
	/**
	 * A menu listener that is used to constantly flag the debug
	 * action set menu as dirty so that any underlying changes to the
	 * contributions will be shown.
	 * As well, if there are no history items, disable the action.
	 */
	protected MenuListener getDebugActionSetMenuListener() {
		return new MenuListener() {
			public void menuShown(MenuEvent e) {
				getActionProxy().setEnabled(getHistory().length > 0 || getFavorites().length > 0);
				IWorkbenchWindow window= DebugUIPlugin.getActiveWorkbenchWindow();
				if (window instanceof ApplicationWindow) {
					ApplicationWindow appWindow= (ApplicationWindow)window;
					IMenuManager manager= appWindow.getMenuBarManager();
					IContributionItem actionSetItem= manager.findUsingPath("org.eclipse.ui.run"); //$NON-NLS-1$
					if (actionSetItem instanceof SubContributionItem) {
						IContributionItem item= ((SubContributionItem)actionSetItem).getInnerItem();
						if (item instanceof IMenuManager) {
							fMenuShown= true;
							((IMenuManager)item).markDirty();
						}
					}
				}
			}
			public void menuHidden(MenuEvent e) {
			}
		};
	}
	
	protected Menu getCreatedMenu() {
		return fCreatedMenu;
	}

	protected void setCreatedMenu(Menu createdMenu) {
		fCreatedMenu = createdMenu;
	}
		
	protected IAction getDebugWithAction() {
		if (fDebugWithAction == null) {
			fDebugWithAction= new DebugWithConfigurationAction();
		} else {
			fDebugWithAction.dispose();
		}
		return fDebugWithAction;
	}

	protected IAction getRunWithAction() {
		if (fRunWithAction == null) {
			fRunWithAction= new RunWithConfigurationAction();
		} else {
			fRunWithAction.dispose();
		}
		return fRunWithAction;
	}
}