/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.core.model;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.internal.core.DebugCoreMessages;
import org.eclipse.debug.internal.core.NullStreamsProxy;
import org.eclipse.debug.internal.core.StreamsProxy;

/**
 * Standard implementation of an <code>IProcess</code> that wrappers a system
 * process (<code>java.lang.Process</code>).
 * <p>
 * Clients may subclass this class. Clients that need to replace the implementation
 * of a streams proxy associated with an <code>IProcess</code> should subclass this
 * class. Generally clients should not instantiate this class directly, but should
 * instead call <code>DebugPlugin.newProcess(...)</code>, which can delegate to an
 * <code>IProcessFactory</code> if one is referenced by the associated launch configuration.
 * </p>
 * @see org.eclipse.debug.core.model.IProcess
 * @see org.eclipse.debug.core.IProcessFactory
 * @since 3.0
 */
public class RuntimeProcess extends PlatformObject implements IProcess {

	private static final int MAX_WAIT_FOR_DEATH_ATTEMPTS = 10;
	private static final int TIME_TO_WAIT_FOR_THREAD_DEATH = 500; // ms

	/**
	 * The launch this process is contained in
	 */
	private ILaunch fLaunch;

	/**
	 * The system process represented by this <code>IProcess</code>
	 */
	private Process fProcess;

	/**
	 * This process's exit value
	 */
	private int fExitValue;

	/**
	 * The monitor which listens for this runtime process' system process
	 * to terminate.
	 */
	private ProcessMonitorThread fMonitor;

	/**
	 * The streams proxy for this process
	 */
	private IStreamsProxy fStreamsProxy;

	/**
	 * The name of the process
	 */
	private String fName;

	/**
	 * Whether this process has been terminated
	 */
	private boolean fTerminated;

	/**
	 * Table of client defined attributes
	 */
	private Map<String, String> fAttributes;

	/**
	 * Whether output from the process should be captured or swallowed
	 */
	private boolean fCaptureOutput = true;

	/**
	 * Constructs a RuntimeProcess on the given system process
	 * with the given name, adding this process to the given
	 * launch.
	 *
	 * @param launch the parent launch of this process
	 * @param process underlying system process
	 * @param name the label used for this process
	 * @param attributes map of attributes used to initialize the attributes
	 *   of this process, or <code>null</code> if none
	 */
	public RuntimeProcess(ILaunch launch, Process process, String name, Map<String, String> attributes) {
		setLaunch(launch);
		initializeAttributes(attributes);
		fProcess= process;
		fName= name;
		fTerminated= true;
		try {
			fExitValue = process.exitValue();
		} catch (IllegalThreadStateException e) {
			fTerminated= false;
		}

		String captureOutput = launch.getAttribute(DebugPlugin.ATTR_CAPTURE_OUTPUT);
		fCaptureOutput = !("false".equals(captureOutput)); //$NON-NLS-1$

		fStreamsProxy= createStreamsProxy();
		fMonitor = new ProcessMonitorThread(this);
		fMonitor.start();
		launch.addProcess(this);
		fireCreationEvent();
	}

	/**
	 * Initialize the attributes of this process to those in the given map.
	 *
	 * @param attributes attribute map or <code>null</code> if none
	 */
	private void initializeAttributes(Map<String, String> attributes) {
		if (attributes != null) {
			for (Entry<String, String> entry : attributes.entrySet()) {
				setAttribute(entry.getKey(), entry.getValue());
			}
		}
	}

	/**
	 * @see ITerminate#canTerminate()
	 */
	@Override
	public synchronized boolean canTerminate() {
		return !fTerminated;
	}

	/**
	 * @see IProcess#getLabel()
	 */
	@Override
	public String getLabel() {
		return fName;
	}

	/**
	 * Sets the launch this process is contained in
	 *
	 * @param launch the launch this process is contained in
	 */
	protected void setLaunch(ILaunch launch) {
		fLaunch = launch;
	}

	/**
	 * @see IProcess#getLaunch()
	 */
	@Override
	public ILaunch getLaunch() {
		return fLaunch;
	}

	/**
	 * Returns the underlying system process associated with this process.
	 *
	 * @return system process
	 */
	protected Process getSystemProcess() {
		return fProcess;
	}

	/**
	 * @see ITerminate#isTerminated()
	 */
	@Override
	public synchronized boolean isTerminated() {
		return fTerminated;
	}

	/**
	 * @see ITerminate#terminate()
	 */
	@Override
	public void terminate() throws DebugException {
		if (!isTerminated()) {
			if (fStreamsProxy instanceof StreamsProxy) {
				((StreamsProxy) fStreamsProxy).kill();
			}
			Process process = getSystemProcess();
			if (process != null) {

				List<ProcessHandle> descendants; // only a snapshot!
				try {
					descendants = process.descendants().collect(Collectors.toList());
				} catch (UnsupportedOperationException e) {
					// JVM may not support toHandle() -> assume no descendants
					descendants = Collections.emptyList();
				}

				process.destroy();
				descendants.forEach(ProcessHandle::destroy);
			}
			int attempts = 0;
			while (attempts < MAX_WAIT_FOR_DEATH_ATTEMPTS) {
				try {
					process = getSystemProcess();
					if (process != null) {
						fExitValue = process.exitValue(); // throws exception if process not exited
					}
					return;
				} catch (IllegalThreadStateException ie) {
				}
				try {
					if (process != null) {
						process.waitFor(TIME_TO_WAIT_FOR_THREAD_DEATH, TimeUnit.MILLISECONDS);
					} else {
						Thread.sleep(TIME_TO_WAIT_FOR_THREAD_DEATH);
					}
				} catch (InterruptedException e) {
				}
				attempts++;
			}
			// clean-up
			if (fMonitor != null) {
				fMonitor.killThread();
				fMonitor = null;
			}
			IStatus status = new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugException.TARGET_REQUEST_FAILED, DebugCoreMessages.RuntimeProcess_terminate_failed, null);
			throw new DebugException(status);
		}
	}

	/**
	 * Notification that the system process associated with this process
	 * has terminated.
	 */
	protected void terminated() {
		setAttribute(DebugPlugin.ATTR_TERMINATE_TIMESTAMP, Long.toString(System.currentTimeMillis()));

		if (fStreamsProxy instanceof StreamsProxy) {
			((StreamsProxy)fStreamsProxy).close();
		}


		// Avoid calling IProcess.exitValue() inside a sync section (Bug 311813).
		int exitValue = -1;
		boolean running = false;
		try {
			exitValue = fProcess.exitValue();
		} catch (IllegalThreadStateException ie) {
			running = true;
		}

		synchronized (this) {
			fTerminated= true;
			if (!running) {
				fExitValue = exitValue;
			}
			fProcess= null;
		}
		fireTerminateEvent();
	}

	/**
	 * @see IProcess#getStreamsProxy()
	 */
	@Override
	public IStreamsProxy getStreamsProxy() {
		if (!fCaptureOutput) {
			return null;
		}
		return fStreamsProxy;
	}

	/**
	 * Creates and returns the streams proxy associated with this process.
	 *
	 * @return streams proxy
	 */
	protected IStreamsProxy createStreamsProxy() {
		if (!fCaptureOutput) {
			return new NullStreamsProxy(getSystemProcess());
		}
		String encoding = getLaunch().getAttribute(DebugPlugin.ATTR_CONSOLE_ENCODING);
		Charset charset = null;
		if (encoding != null) {
			try {
				charset = Charset.forName(encoding);
			} catch (UnsupportedCharsetException | IllegalCharsetNameException e) {
				DebugPlugin.log(e);
			}
		}
		return new StreamsProxy(getSystemProcess(), charset);
	}

	/**
	 * Fires a creation event.
	 */
	protected void fireCreationEvent() {
		fireEvent(new DebugEvent(this, DebugEvent.CREATE));
	}

	/**
	 * Fires the given debug event.
	 *
	 * @param event debug event to fire
	 */
	protected void fireEvent(DebugEvent event) {
		DebugPlugin manager= DebugPlugin.getDefault();
		if (manager != null) {
			manager.fireDebugEventSet(new DebugEvent[]{event});
		}
	}

	/**
	 * Fires a terminate event.
	 */
	protected void fireTerminateEvent() {
		fireEvent(new DebugEvent(this, DebugEvent.TERMINATE));
	}

	/**
	 * Fires a change event.
	 */
	protected void fireChangeEvent() {
		fireEvent(new DebugEvent(this, DebugEvent.CHANGE));
	}

	/**
	 * @see IProcess#setAttribute(String, String)
	 */
	@Override
	public void setAttribute(String key, String value) {
		if (fAttributes == null) {
			fAttributes = new HashMap<>(5);
		}
		Object origVal = fAttributes.get(key);
		if (origVal != null && origVal.equals(value)) {
			return; //nothing changed.
		}

		fAttributes.put(key, value);
		fireChangeEvent();
	}

	/**
	 * @see IProcess#getAttribute(String)
	 */
	@Override
	public String getAttribute(String key) {
		if (fAttributes == null) {
			return null;
		}
		return fAttributes.get(key);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter.equals(IProcess.class)) {
			return (T) this;
		}
		if (adapter.equals(IDebugTarget.class)) {
			ILaunch launch = getLaunch();
			IDebugTarget[] targets = launch.getDebugTargets();
			for (IDebugTarget target : targets) {
				if (this.equals(target.getProcess())) {
					return (T) target;
				}
			}
			return null;
		}
		if (adapter.equals(ILaunch.class)) {
			return (T) getLaunch();
		}
		//CONTEXTLAUNCHING
		if(adapter.equals(ILaunchConfiguration.class)) {
			return (T) getLaunch().getLaunchConfiguration();
		}
		return super.getAdapter(adapter);
	}
	/**
	 * @see IProcess#getExitValue()
	 */
	@Override
	public synchronized int getExitValue() throws DebugException {
		if (isTerminated()) {
			return fExitValue;
		}
		throw new DebugException(new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugException.TARGET_REQUEST_FAILED, DebugCoreMessages.RuntimeProcess_Exit_value_not_available_until_process_terminates__1, null));
	}

	/**
	 * Monitors a system process, waiting for it to terminate, and
	 * then notifies the associated runtime process.
	 */
	class ProcessMonitorThread extends Thread {

		/**
		 * Whether the thread has been told to exit.
		 */
		protected boolean fExit;
		/**
		 * The underlying <code>java.lang.Process</code> being monitored.
		 */
		protected Process fOSProcess;
		/**
		 * The <code>IProcess</code> which will be informed when this
		 * monitor detects that the underlying process has terminated.
		 */
		protected RuntimeProcess fRuntimeProcess;

		/**
		 * The <code>Thread</code> which is monitoring the underlying process.
		 */
		protected Thread fThread;

		/**
		 * A lock protecting access to <code>fThread</code>.
		 */
		private final Object fThreadLock = new Object();

		/**
		 * @see Thread#run()
		 */
		@Override
		public void run() {
			synchronized (fThreadLock) {
				if (fExit) {
					return;
				}
				fThread = Thread.currentThread();
			}
			while (fOSProcess != null) {
				try {
					fOSProcess.waitFor();
				} catch (InterruptedException ie) {
					// clear interrupted state
					Thread.interrupted();
				} finally {
					fOSProcess = null;
					fRuntimeProcess.terminated();
				}
			}
			fThread = null;
		}

		/**
		 * Creates a new process monitor and starts monitoring the process for
		 * termination.
		 *
		 * @param process process to monitor for termination
		 */
		public ProcessMonitorThread(RuntimeProcess process) {
			super(DebugCoreMessages.ProcessMonitorJob_0);
			setDaemon(true);
			fRuntimeProcess= process;
			fOSProcess= process.getSystemProcess();
		}

		/**
		 * Kills the monitoring thread.
		 *
		 * This method is to be useful for dealing with the error
		 * case of an underlying process which has not informed this
		 * monitor of its termination.
		 */
		protected void killThread() {
			synchronized (fThreadLock) {
				if (fThread == null) {
					fExit = true;
				} else {
					fThread.interrupt();
				}
			}
		}
	}
}
