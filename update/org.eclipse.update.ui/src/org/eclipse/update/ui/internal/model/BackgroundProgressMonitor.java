package org.eclipse.update.ui.internal.model;

import org.eclipse.core.runtime.IProgressMonitor;
import java.util.*;
import org.eclipse.swt.widgets.Display;

public class BackgroundProgressMonitor implements IProgressMonitor {
	private Vector monitors=new Vector();
	private boolean canceled=false;
	private String taskName;
	private String subTaskName;
	private int totalWorked=0;
	private int taskCount;
	private boolean inProgress;
	private Display display;
	
	public void setDisplay(Display display) {
		this.display = display;
	}
	
	public void addProgressMonitor(final IProgressMonitor monitor) {
		if (monitors.contains(monitor)==false) {
		   monitors.add(monitor);
		   if (inProgress && display!=null) {
		   	  	display.asyncExec(new Runnable() {
		   	  		public void run() {
		   	  		// we are late - catch up
		      			monitor.beginTask(taskName, taskCount);
		      			monitor.worked(totalWorked);
		      			if (subTaskName!=null)
		         			monitor.subTask(subTaskName);
		   	  		}
		   	  	});
		   }
		}
	}
	
	public void removeProgressMonitor(IProgressMonitor monitor) {
		if (monitors.contains(monitor))
		   monitors.remove(monitor);
	}

	/**
	 * @see IProgressMonitor#beginTask(String, int)
	 */
	public void beginTask(final String taskName, final int count) {
		totalWorked = 0;
		taskCount = count;
		this.taskName = taskName;
		subTaskName = null;
		display.asyncExec(new Runnable() {
			public void run() {
				for (Iterator iter=monitors.iterator(); iter.hasNext();) {
					IProgressMonitor m = (IProgressMonitor)iter.next();
					m.beginTask(taskName, count);
				}
			}
		});
	}

	/**
	 * @see IProgressMonitor#done()
	 */
	public void done() {
		display.asyncExec(new Runnable() {
			public void run() {
				for (Iterator iter=monitors.iterator(); iter.hasNext();) {
					IProgressMonitor m = (IProgressMonitor)iter.next();
					m.done();
				}
			}
		});
		inProgress = false;
	}

	/**
	 * @see IProgressMonitor#internalWorked(double)
	 */
	public void internalWorked(double arg0) {
	}

	/**
	 * @see IProgressMonitor#isCanceled()
	 */
	public boolean isCanceled() {
		return canceled;
	}

	/**
	 * @see IProgressMonitor#setCanceled(boolean)
	 */
	public void setCanceled(final boolean canceled) {
		display.asyncExec(new Runnable() {
			public void run() {
				for (Iterator iter=monitors.iterator(); iter.hasNext();) {
					IProgressMonitor m = (IProgressMonitor)iter.next();
					m.setCanceled(canceled);
				}
			}
		});
		this.canceled = canceled;
	}

	/**
	 * @see IProgressMonitor#setTaskName(String)
	 */
	public void setTaskName(final String name) {
		display.asyncExec(new Runnable() {
			public void run() {
				for (Iterator iter=monitors.iterator(); iter.hasNext();) {
					IProgressMonitor m = (IProgressMonitor)iter.next();
					m.setTaskName(name);
				}
			}
		});
		this.taskName = name;
	}

	/**
	 * @see IProgressMonitor#subTask(String)
	 */
	public void subTask(final String name) {
		subTaskName = name;
		display.asyncExec(new Runnable() {
			public void run() {
				for (Iterator iter=monitors.iterator(); iter.hasNext();) {
					IProgressMonitor m = (IProgressMonitor)iter.next();
					m.subTask(name);
				}
			}
		});
	}

	/**
	 * @see IProgressMonitor#worked(int)
	 */
	public void worked(final int amount) {
		totalWorked += amount;
		display.asyncExec(new Runnable() {
			public void run() {
				for (Iterator iter=monitors.iterator(); iter.hasNext();) {
					IProgressMonitor m = (IProgressMonitor)iter.next();
					m.worked(amount);
				}
			}
		});
	}
}

