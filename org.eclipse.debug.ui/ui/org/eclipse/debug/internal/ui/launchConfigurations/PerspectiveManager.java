package org.eclipse.debug.internal.ui.launchConfigurations;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.text.MessageFormat;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.ILauncher;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.WorkbenchException;

/**
 * The perspective manager manages the 'perspective' settings
 * defined by lanuch configurations. Specifically it: <ul>
 * <li>changes perspectives as launches are registered</li>
 * <li>change perspective when a thread suspends</li>
 * <li>(will) open the console when there is program output</li>
 * </ul>
 * 
 * @see IDebugUIContants.ATTR_RUN_PERSPECTIVE
 * @see IDebugUIContants.ATTR_DEBUG_PERSPECTIVE
 */
public class PerspectiveManager implements ILaunchListener, IDebugEventListener {
	
	/**
	 * Singleton perspective manager
	 */
	private static PerspectiveManager fgManager;
	
	/**
	 * Constructs the perspective manager
	 */
	private PerspectiveManager() {
	}
	
	/**
	 * Returns the perspective manager
	 */
	public static PerspectiveManager getDefault() {
		if (fgManager == null) {
			fgManager = new PerspectiveManager();
		}
		return fgManager;
	}
	
	/**
	 * Called by the debug ui plug-in on startup.
	 * The perspective manager starts listening for
	 * launches to be registered.
	 */
	public void startup() {
		DebugPlugin plugin = DebugPlugin.getDefault();
		plugin.getLaunchManager().addLaunchListener(this);
		plugin.addDebugEventListener(this);
	}

	/**
	 * Called by the debug ui plug-in on shutdown.
	 * The perspective manager de-registers as a 
	 * launch listener.
	 */
	public void shutdown() {
		DebugPlugin plugin = DebugPlugin.getDefault();
		plugin.getLaunchManager().removeLaunchListener(this);
		plugin.removeDebugEventListener(this);
	}

	/**
	 * Do nothing.
	 * 
	 * @see ILaunchListener#launchDeregistered(ILaunch)
	 */
	public void launchDeregistered(ILaunch launch) {
	}

	/** 
	 * Switch to the perspective specified by the
	 * launch configuration.
	 * 
	 * @see ILaunchListener#launchRegistered(ILaunch)
	 */
	public void launchRegistered(ILaunch launch) {
		String perspectiveId = null;
		// check event filters
		if (DebugUIPlugin.getDefault().showLaunch(launch)) {
			try {
				perspectiveId = getPerspectiveId(launch);
			} catch (final CoreException e) {
				String name = DebugUIPlugin.getDefault().getModelPresentation().getText(launch);
				switchFailed(e.getStatus(), name);
			}
			if (perspectiveId != null) {
				switchToPerspective(perspectiveId);
			}
		}
	}


	/**
	 * Switches to the specified perspective
	 * 
	 * @param id perspective identifier
	 * 
	 * [Issue: what should we supply as input to a new page?] 
	 */
	protected void switchToPerspective(final String id) {
		final IWorkbenchWindow window = DebugUIPlugin.getActiveWorkbenchWindow();
		if (window != null) {
			final IWorkbenchPage page = findPage(id);
			if (page == null) {
				async(new Runnable() {
					public void run() {
						try {
							window.getWorkbench().openPage(id, ResourcesPlugin.getWorkspace().getRoot(),0);
						} catch (WorkbenchException e) {
							DebugUIPlugin.errorDialog(DebugUIPlugin.getShell(),
							"Error", 
							MessageFormat.format("Unable to switch to perspective: {0}", new String[]{id}),
							e.getStatus());
						}
					}
				});
				
			} else {
				if (page.equals(window.getActivePage())) {
					// no switch required
					return;
				} else {
					async(new Runnable() {
						public void run() {
							window.setActivePage(page);
						}
					});
					
				}
			}
			
		}
	}
		
	/**
	 * Returns a page in the current workbench window with the
	 * given identifier, or <code>null</code> if none.
	 * 
	 * @param id perpsective identifier
	 * @return workbench page, or <code>null</code>
	 */
	protected IWorkbenchPage findPage(String id) {
		IWorkbenchWindow window = DebugUIPlugin.getActiveWorkbenchWindow();
		if (window != null) {
			IWorkbenchPage[] pages = window.getPages();
			for (int i = 0; i < pages.length; i++) {
				if (pages[i].getPerspective().getId().equals(id)) {
					return pages[i];
				}
			}
		}
		return null;
	}
	
	/**
	 * Utility method to submit an asnychronous runnable to the UI
	 */
	protected void async(Runnable r) {
		Display d = DebugUIPlugin.getDefault().getDisplay();
		if (d != null) {
			d.asyncExec(r);
		}
	}
	
	/**
	 * Utility method to submit a synchronous runnable to the UI
	 */
	protected void sync(Runnable r) {
		Display d = DebugUIPlugin.getDefault().getDisplay();
		if (d != null) {
			d.syncExec(r);
		}
	}	

	/**
	 * Reports failure to switch perspectives to the user
	 * 
	 * @param status exception status describing failure
	 * @param launchName the name of the launch that the
	 *  failure is associated with
	 */
	protected void switchFailed(final IStatus status, final String launchName) {
		sync(new Runnable() {
			public void run() {
				DebugUIPlugin.errorDialog(DebugUIPlugin.getShell(), "Error", 
				 MessageFormat.format("Unable to switch perpsectives as specified by launch: {0}", new String[] {launchName}),
				 status);
			}});
	}
	
	/**
	 * On a SUSPEND event, show the debug view. If no debug view is open,
	 * switch to the perspective specified by the launcher.
	 *
	 * @see IDebugEventListener#handleDebugEvent(DebugEvent)
	 */
	public void handleDebugEvent(final DebugEvent event) {
		// open the debugger if this is a suspend event and the debug view is not yet open
		// and the preferences are set to switch
		if (event.getKind() == DebugEvent.SUSPEND && event.getDetail() == event.BREAKPOINT) {
			// apply event filters
			if (!DebugUIPlugin.getDefault().showSuspendEvent(event)) {
				return;
			}
			ILaunch launch = null;
			Object source = event.getSource();
			if (source instanceof IDebugElement) {
				launch = ((IDebugElement)source).getLaunch();
			} else if (source instanceof IProcess) {
				launch = ((IProcess)source).getLaunch();
			}
			String perspectiveId = null;
			try {
				perspectiveId = getPerspectiveId(launch);
			} catch (CoreException e) {
				DebugUIPlugin.logError(e);
			}
			// if no perspective specified, always switch to debg
			// perspective (unless the current perspective has a 
			// debug view open
			
			// this has to be done in an asynch, such that the workbench
			// window can be accessed
			final String id = perspectiveId;
			Runnable r = new Runnable() {
				public void run() {
					String targetId = id;
					if (targetId == null) {
						IWorkbenchWindow window = DebugUIPlugin.getActiveWorkbenchWindow();
						if (window != null) {
							IWorkbenchPage page = window.getActivePage();
							if (page != null) {
								IViewPart part = page.findView(IDebugUIConstants.ID_DEBUG_VIEW);
								if (part == null) {
									targetId = IDebugUIConstants.ID_DEBUG_PERSPECTIVE;
								}
							}
						}
					}
					if (targetId != null) {
						switchToPerspective(targetId);
					}
				}
			};
			async(r);
		}
	}	
	
	/** 
	 * Returns the perspective associated with the
	 * given launch, or <code>null</code> if none.
	 * 
	 * @param launch a launch
	 * @return the perspective associated with the launch,
	 * 	or <code>null</code>
	 * @exception CoreException if unable to retrieve a required
	 *  launch configuration attribute
	 */
	protected String getPerspectiveId(ILaunch launch) throws CoreException {
		ILaunchConfiguration config = launch.getLaunchConfiguration();
		String perspectiveId = null;
		String mode = launch.getLaunchMode();
		if (config == null) {
			// use the launcher's perspective id if specified
			ILauncher launcher = launch.getLauncher();
			if (launcher != null) {
				perspectiveId = launcher.getPerspectiveIdentifier();
			}
			if (perspectiveId == null) {
				// use the global peference to switch perspectives
				IPreferenceStore store = DebugUIPlugin.getDefault().getPreferenceStore();
				boolean showDebug = false;
				if (mode.equals(ILaunchManager.DEBUG_MODE)) {
					showDebug = store.getBoolean(IDebugUIConstants.PREF_AUTO_SHOW_DEBUG_VIEW);
				} else {
					showDebug = store.getBoolean(IDebugUIConstants.PREF_AUTO_SHOW_PROCESS_VIEW);
				}
				if (showDebug) {
					perspectiveId = IDebugUIConstants.ID_DEBUG_PERSPECTIVE;
				}
			}
		} else {
			if (mode.equals(ILaunchManager.DEBUG_MODE)) {
				perspectiveId = config.getAttribute(IDebugUIConstants.ATTR_TARGET_DEBUG_PERSPECTIVE, null);
			} else {
				perspectiveId = config.getAttribute(IDebugUIConstants.ATTR_TARGET_RUN_PERSPECTIVE, null);
			}
		}
		return perspectiveId;
	}
	
}
