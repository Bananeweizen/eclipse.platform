package org.eclipse.debug.core.model;


/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */
 
import org.eclipse.debug.core.ILaunch;
import org.eclipse.core.runtime.IAdaptable;

/**
 * A process represents a program running in normal (non-debug) mode.
 * Processes support setting and getting of client defined attributes.
 * This way, clients can annotate a process with any extra information
 * important to them. For example, classpath annotations, or command
 * line arguments used to launch the process may be important to a client.
 * <p>
 * Clients may implement this interface, however, the debug plug-in
 * provides an implementation of this interface for a
 * <code>java.lang.Process</code>. 
 * </p>
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @see org.eclipse.debug.core.DebugPlugin#newProcess(Process, String)
 */
public interface IProcess extends IAdaptable, ITerminate {

	/**
	 * Returns a human-readable label for this process.
	 *
	 * @return a label
	 */
	String getLabel();
	/**
	 * Returns the <code>ILaunch</code> this element originated from, or
	 * <code>null</code> if this element has not yet been registered with
	 * an <code>ILaunch</code>. This is a convenience method for
	 * <code>ILaunchManager.findLaunch(IProcess)</code>.
	 *
	 * @return the launch this process is contained in
	 */
	ILaunch getLaunch();
	/**
	 * Returns a proxy to the standard input, output, and error streams 
	 * for this process, or <code>null</code> if not supported.
	 *
	 * @return a streams proxy, or <code>null</code> if not supported
	 */
	IStreamsProxy getStreamsProxy();
	
	/**
	 * Sets the value of a client defined attribute.
	 *
	 * @param key the attribute key
	 * @param value the attribute value
	 */
	void setAttribute(String key, String value);
	
	/**
	 * Returns the value of a client defined attribute.
	 *
	 * @param key the attribute key
	 * @return value the attribute value, or <code>null</code> if undefined
	 */
	String getAttribute(String key);

}


