package org.eclipse.debug.internal.ui.views;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.HashMap;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IExpression;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.ISuspendResume;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;

/**
 * Handles debug events, updating the launch view and viewer.
 */
public class LaunchViewEventHandler extends AbstractDebugEventHandler implements ILaunchListener{
		
	/**
	 * Stack frame counts keyed by thread.  Used to optimize thread refreshing.
	 */
	private HashMap fStackFrameCountByThread = new HashMap(5);
	
	/**
	 * Constructs an event handler for the given launch
	 * view and viewer.
	 * 
	 * @param view launch view
	 * @param viewer launch viewer
	 */
	public LaunchViewEventHandler(LaunchView view, LaunchViewer viewer) {
		super(view, viewer);
		DebugPlugin plugin= DebugPlugin.getDefault();
		plugin.getLaunchManager().addLaunchListener(this);
	}
	

	/**
	 * @see AbstractDebugEventHandler#doHandleDebugEvent(DebugEvent)
	 */
	protected void doHandleDebugEvent(DebugEvent event) {
		Object element= event.getSource();
		if (element instanceof IVariable || element instanceof IValue || element instanceof IExpression) {
			// the debug view does not show variables
			return;
		}
		switch (event.getKind()) {
			case DebugEvent.CREATE :
				insert(element);
				break;
			case DebugEvent.TERMINATE :
				if (element instanceof IThread) {
					clearSourceSelection((IThread)element);
					fStackFrameCountByThread.remove(element);
					remove(element);
				} else {
					clearSourceSelection(null);
					Object parent = ((ITreeContentProvider)getTreeViewer().getContentProvider()).getParent(element);
					refresh(parent);
				}
				updateButtons();
				break;
			case DebugEvent.RESUME :
				doHandleResumeEvent(event, element);
				break;
			case DebugEvent.SUSPEND :
				doHandleSuspendEvent(element);
				break;
			case DebugEvent.CHANGE :
				refresh(element);
				updateButtons();
				break;
		}
	}
		
	protected void doHandleResumeEvent(DebugEvent event, Object element) {
		if (element instanceof ISuspendResume) {
			if (((ISuspendResume)element).isSuspended()) {
				IThread thread = getThread(element);
				if (thread != null) {								
					resetStackFrameCount(thread);
				}
				return;
			}
		}		
		clearSourceSelection(null);
		if (event.isStepStart()) {
			IThread thread = getThread(element);
			if (thread != null) {								
				getLaunchViewer().updateStackFrameIcons(thread);
				resetStackFrameCount(thread);
			}
		} else {
			refresh(element);
			if (element instanceof IThread) {
				//select and reveal will update buttons
				//via selection changed callback
				selectAndReveal(element);
				resetStackFrameCount((IThread)element);
				return;
			}
		}			
		labelChanged(element);
		updateButtons();
	}
	
	protected void resetStackFrameCount(IThread thread) {
		fStackFrameCountByThread.put(thread, new Integer(0));
	}

	protected void doHandleSuspendEvent(Object element) {
		if (element instanceof IThread) {
			doHandleSuspendThreadEvent((IThread)element);
		}
		updateButtons();
		getLaunchView().showMarkerForCurrentSelection();
	}
	
	// This method exists to provide some optimization for refreshing suspended
	// threads.  If the number of stack frames has changed from the last suspend, 
	// we do a full refresh on the thread.  Otherwise, we only do a surface-level
	// refresh on the thread, then refresh each of the children, which eliminates
	// flicker when do quick stepping (e.g., holding down the F6 key) within a method.
	protected void doHandleSuspendThreadEvent(IThread thread) {
		// if the thread has already resumed, do nothing
		if (!thread.isSuspended()) {
			return;
		}
		
		// Get the current stack frames from the thread
		IStackFrame[] stackFrames = null;
		try {
			stackFrames = thread.getStackFrames();
		} catch (DebugException de) {
			DebugUIPlugin.logError(de);
			return;
		}
		
		int currentStackFrameCount = stackFrames.length;
		
		// Retrieve the old and current counts of stack frames for this thread
		int oldStackFrameCount = 0;
		Integer oldStackFrameCountObject = (Integer)fStackFrameCountByThread.get(thread);
		if (oldStackFrameCountObject != null) {
			oldStackFrameCount = oldStackFrameCountObject.intValue();
		}
		
		// Compare old & current stack frame counts.  We need to refresh the thread
		// parent if there are fewer stack frame children
		boolean refreshNeeded = true;
		if (currentStackFrameCount == oldStackFrameCount) {
			refreshNeeded = false;
		}
		
		// Auto-expand the thread.  If we are also refreshing the thread,
		// then we don't need to worry about any children, since refreshing
		// the parent handles this
		getLaunchView().autoExpand(thread, refreshNeeded, refreshNeeded);
		if (refreshNeeded) {
			// Update the stack frame count for the thread
			oldStackFrameCountObject = new Integer(currentStackFrameCount);
			fStackFrameCountByThread.put(thread, oldStackFrameCountObject);
			return;
		} else {
			labelChanged(thread);
		}
		
		// Auto-expand each stack frame child.  This has the effect of adding 
		// any new stack frames, and updating any existing stack frames
		if ((stackFrames != null) && (currentStackFrameCount > 0)) {
			for (int i = currentStackFrameCount - 1; i > 0; i--) {
				getLaunchView().autoExpand(stackFrames[i], true, false);				
			}
			// Treat the first stack frame differently, since we want to select it
			getLaunchView().autoExpand(stackFrames[0], true, true);				
		}	
		
		// Update the stack frame count for the thread
		oldStackFrameCountObject = new Integer(currentStackFrameCount);
		fStackFrameCountByThread.put(thread, oldStackFrameCountObject);
	}
	
	/**
	 * Helper method to update the buttons of the viewer - must be called in UI thread
	 */
	protected void updateButtons() {
		// fire a selection change such that the debug menu can
		// update
		getView().getSite().getSelectionProvider().setSelection(getViewer().getSelection());
	}

	/**
	 * @see ILaunchListener#launchRemoved(ILaunch)
	 */
	public void launchRemoved(final ILaunch launch) {
		Runnable r= new Runnable() {
			public void run() {
				remove(launch);
				ILaunchManager lm= DebugPlugin.getDefault().getLaunchManager();
				IDebugTarget[] targets= lm.getDebugTargets();
				if (targets.length > 0) {
					IDebugTarget target= targets[targets.length - 1];
					try {
						IThread[] threads= target.getThreads();
						for (int i=0; i < threads.length; i++) {
							if (threads[i].isSuspended()) {
								getLaunchView().autoExpand(threads[i], false, true);
								return;
							}
						}						
					} catch (DebugException de) {
						DebugUIPlugin.logError(de);
					}
					
					getLaunchView().autoExpand(target.getLaunch(), false, true);
				}
				updateButtons();
			}
		};

		getView().asyncExec(r);
	}
	
	/**
	 * @see ILaunchListener#launchChanged(ILaunch)
	 */
	public void launchChanged(final ILaunch launch) {
	}

	/**
	 * @see ILaunchListener#launchAdded(ILaunch)
	 */
	public void launchAdded(final ILaunch newLaunch) {
		Runnable r= new Runnable() {
			public void run() {		
				if (DebugUIPlugin.getDefault().getPreferenceStore().getBoolean(IDebugUIConstants.PREF_AUTO_REMOVE_OLD_LAUNCHES)) {
					ILaunchManager lManager= DebugPlugin.getDefault().getLaunchManager();
					Object[] launches= lManager.getLaunches();
					for (int i= 0; i < launches.length; i++) {
						ILaunch launch= (ILaunch)launches[i];
						if (launch != newLaunch && launch.isTerminated()) {
							lManager.removeLaunch(launch);
						}
					}
				}
				insert(newLaunch);
				getLaunchView().autoExpand(newLaunch, false, true);
			}
		};

		getView().syncExec(r);
	}

	/**
	 * De-registers this event handler from the debug model.
	 */
	public void dispose() {
		super.dispose();
		DebugPlugin plugin= DebugPlugin.getDefault();
		plugin.getLaunchManager().removeLaunchListener(this);
	}

	/**
	 * Clear the selection in the editor - must be called in UI thread
	 */
	private void clearSourceSelection(IThread thread) {
		if (getViewer() != null) {
			if (thread != null) {
				IStructuredSelection selection= (IStructuredSelection)getLaunchViewer().getSelection();
				Object element= selection.getFirstElement();
				if (element instanceof IStackFrame) {
					IStackFrame stackFrame = (IStackFrame) element;
					if (!stackFrame.getThread().equals(thread)) {
						//do not clear the source selection
						//a thread has terminated that is not the
						//parent of the currently selected stack frame
						return;
					}
				}
			}
		
			getLaunchView().clearSourceSelection();
		}
	}

	/**
	 * Returns this event handler's launch viewer
	 * 
	 * @return launch viewer
	 */
	protected LaunchViewer getLaunchViewer() {
		return (LaunchViewer)getViewer();
	}
	
	/**
	 * Returns this event handler's launch view
	 * 
	 * @return launch view
	 */
	protected LaunchView getLaunchView() {
		return (LaunchView)getView();
	}		
	
	private IThread getThread(Object element) {
		IThread thread = null;
		if (element instanceof IThread) {
			thread = (IThread) element;
		} else if (element instanceof IStackFrame) {
			thread = ((IStackFrame)element).getThread();
		}
		return thread;
	}
}

