package org.eclipse.debug.core;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.internal.core.DebugCoreMessages;
import org.eclipse.debug.internal.core.LaunchManager;

/**
 * A launch is the result of launching a debug session
 * and/or one or more system processes. This class provides
 * a public implementation of <code>ILaunch</code> for client
 * use.
 * <p>
 * Clients may instantiate this class. Clients may subclass this class.
 * All of the methods in this class that are part of the launcher interface are
 * final. Clients that subclass this class are not intended to change the behavior
 * or implementation of the provided methods. Subclassing is only intended
 * to add additional information to a specific launch. For example, a client that
 * implements a launch object representing a Java launch might store a classpath
 * with the launch.
 * </p>
 * @see ILaunch
 * @see ILaunchManager
 */

public class Launch extends PlatformObject implements ILaunch {
	
	/**
	 * The debug targets associated with this
	 * launch (the primary target is the first one
	 * in this collection), or empty if
	 * there are no debug targets.
	 */
	private List fTargets= new ArrayList();

	/**
	 * The configuration that was launched, or null.
	 */
	private ILaunchConfiguration fConfiguration= null;

	/**
	 * The system processes associated with
	 * this launch, or empty if none.
	 */
	private List fProcesses= new ArrayList();

	/**
	 * The source locator to use in the debug session
	 * or <code>null</code> if not supported.
	 */
	private ISourceLocator fLocator= null;

	/**
	 * The mode this launch was launched in.
	 */
	private String fMode;
	
	/**
	 * Table of client defined attributes
	 */
	private HashMap fAttributes;	
	
	/**
	 * Flag indiating that change notification should
	 * be suppressed. <code>true</code> until this
	 * launch has been initialzied.
	 */
	private boolean fSuppressChange = true;
		
	/**
	 * Constructs a launch with the specified attributes.
	 *
	 * @param launchConfiguration the configuration that was launched
	 * @param mode the mode of this launch - run or debug (constants
	 *  defined by <code>ILaunchManager</code>)
	 * @param locator the source locator to use for this debug session, or
	 * 	<code>null</code> if not supported
	 */
	public Launch(ILaunchConfiguration launchConfiguration, String mode, ISourceLocator locator) {		
		setLaunchConfiguration(launchConfiguration);
		setSourceLocator(locator);
		setLaunchMode(mode);
		fSuppressChange = false;
	}	
	
	/**
	 * @see org.eclipse.debug.core.model.ITerminate#canTerminate()
	 */
	public final boolean canTerminate() {
		return !isTerminated();
	}

	/**
	 * @see ILaunch#getChildren()
	 */
	public final Object[] getChildren() {
		ArrayList children = new ArrayList(getDebugTargets0());
		children.addAll(getProcesses0());
		return children.toArray();
	}

	/**
	 * @see ILaunch#getDebugTarget()
	 */
	public final IDebugTarget getDebugTarget() {
		if (!getDebugTargets0().isEmpty()) {
			return (IDebugTarget)getDebugTargets0().get(0);
		}
		return null;
	}
		
	/**
	 * Sets the configuration that was launched
	 * 
	 * @param configuration the configuration that was launched
	 */
	private void setLaunchConfiguration(ILaunchConfiguration configuration) {
		fConfiguration = configuration;
	}	

	/**
	 * @see ILaunch#getProcesses()
	 */
	public final IProcess[] getProcesses() {
		return (IProcess[])getProcesses0().toArray(new IProcess[getProcesses0().size()]);
	}
	
	/**
	 * Returns the processes associated with this
	 * launch, in its internal form - a list.
	 * 
	 * @return list of processes
	 */
	protected List getProcesses0() {
		return fProcesses;
	}	
	
	/**
	 * @see ILaunch#getSourceLocator()
	 */
	public final ISourceLocator getSourceLocator() {
		return fLocator;
	}
	
	/**
	 * @see ILaunch#setSourceLocator(ISourceLocator)
	 */
	public final void setSourceLocator(ISourceLocator sourceLocator) {
		fLocator = sourceLocator;
	}	

	/**
	 * @see org.eclipse.debug.core.model.ITerminate#isTerminated()
	 */
	public final boolean isTerminated() {
		if (getProcesses0().isEmpty() && getDebugTargets0().isEmpty()) {
			return false;
		}

		Iterator processes = getProcesses0().iterator();
		while (processes.hasNext()) {
			IProcess process = (IProcess)processes.next();
			if (!process.isTerminated()) {
				return false;
			}
		}
		
		Iterator targets = getDebugTargets0().iterator();
		while (targets.hasNext()) {
			IDebugTarget target = (IDebugTarget)targets.next();
			if (!(target.isTerminated() || target.isDisconnected())) {
				return false;
			}
		}
		
		return true;
	}

	/**
	 * @see org.eclipse.debug.core.model.ITerminate#terminate()
	 */
	public final void terminate() throws DebugException {
		MultiStatus status= 
			new MultiStatus(DebugPlugin.getUniqueIdentifier(), DebugException.REQUEST_FAILED, DebugCoreMessages.getString("Launch.terminate_failed"), null); //$NON-NLS-1$
		
		// terminate the system processes
		Iterator processes = getProcesses0().iterator();
		while (processes.hasNext()) {
			IProcess process = (IProcess)processes.next();
			if (process.canTerminate()) {
				try {
					process.terminate();
				} catch (DebugException e) {
					status.merge(e.getStatus());
				}
			}
		}
		
		// terminate or disconnect debug target if it is still alive
		Iterator targets = getDebugTargets0().iterator();
		while (targets.hasNext()) {
			IDebugTarget target= (IDebugTarget)targets.next();
			if (target != null) {
				if (target.canTerminate()) {
					try {
						target.terminate();
					} catch (DebugException e) {
						status.merge(e.getStatus());
					}
				} else {
					if (target.canDisconnect()) {
						try {
							target.disconnect();
						} catch (DebugException de) {
							status.merge(de.getStatus());
						}
					}
				}
			}
		}
		if (status.isOK())
			return;
		IStatus[] children= status.getChildren();
		if (children.length == 1) {
			throw new DebugException(children[0]);
		} else {
			throw new DebugException(status);
		}
	}

	/**
	 * @see ILaunch#getLaunchMode()
	 */
	public final String getLaunchMode() {
		return fMode;
	}
	
	/**
	 * Sets the mode in which this launch was 
	 * launched.
	 * 
	 * @param mode the mode in which this launch
	 *  was launched - one of the constants defined
	 *  by <code>ILaunchManager</code>.
	 */
	private void setLaunchMode(String mode) {
		fMode = mode;
	}
	
	/**
	 * @see ILaunch#getLaunchConfiguration()
	 */
	public ILaunchConfiguration getLaunchConfiguration() {
		return fConfiguration;
	}

	/**
	 * @see ILaunch#setAttribute(String, String)
	 */
	public void setAttribute(String key, String value) {
		if (fAttributes == null) {
			fAttributes = new HashMap(5);
		}
		fAttributes.put(key, value);		
	}

	/**
	 * @see ILaunch#getAttribute(String)
	 */
	public String getAttribute(String key) {
		if (fAttributes == null) {
			return null;
		}
		return (String)fAttributes.get(key);
	}

	/**
	 * @see ILaunch#getDebugTargets()
	 */
	public IDebugTarget[] getDebugTargets() {
		return (IDebugTarget[])fTargets.toArray(new IDebugTarget[fTargets.size()]);
	}
	
	/**
	 * Returns the debug targets associated with this
	 * launch, in its internal form - a list
	 * 
	 * @return list of debug targets
	 */
	protected List getDebugTargets0() {
		return fTargets;
	}	

	/**
	 * @see ILaunch#addDebugTarget(IDebugTarget)
	 */
	public final void addDebugTarget(IDebugTarget target) {
		if (target != null) {
			if (!getDebugTargets0().contains(target)) {
				getDebugTargets0().add(target);
				fireChanged();
			}
		}
	}
	
	/**
	 * @see ILaunch#removeDebugTarget(IDebugTarget)
	 */
	public final void removeDebugTarget(IDebugTarget target) {
		if (target != null) {
			if (getDebugTargets0().remove(target)) {
				fireChanged();
			}
		}
	}	
	
	/**
	 * @see ILaunch#addProcess(IProcess)
	 */
	public final void addProcess(IProcess process) {
		if (process != null) {
			if (!getProcesses0().contains(process)) {
				getProcesses0().add(process);
				fireChanged();
			}
		}
	}
	
	/**
	 * @see ILaunch#removeProcess(IProcess)
	 */
	public final void removeProcess(IProcess process) {
		if (process != null) {
			if (!getProcesses0().contains(process)) {
				getProcesses0().remove(process);
				fireChanged();
			}
		}
	}	
	
	/**
	 * Adds the given processes to this launch.
	 * 
	 * @param processes processes to add
	 */
	protected void addProcesses(IProcess[] processes) {
		if (processes != null) {
			for (int i = 0; i < processes.length; i++) {
				addProcess(processes[i]);
				fireChanged();
			}
		}
	}
	
	/**
	 * Notifies listeners that this launch has changed.
	 * Has no effect of this launch has not yet been
	 * properly created/initialized.
	 */
	protected void fireChanged() {
		if (!fSuppressChange) {
			((LaunchManager)DebugPlugin.getDefault().getLaunchManager()).fireUpdate(this, LaunchManager.CHANGED);
		}
	}

	/**
	 * @see ILaunch#hasChildren()
	 */
	public boolean hasChildren() {
		return getProcesses0().size() > 0 || (getDebugTargets0().size() > 0);
	}

}


