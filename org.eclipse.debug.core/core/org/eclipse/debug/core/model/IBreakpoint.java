package org.eclipse.debug.core.model;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugPlugin;

/**
 * A breakpoint is capable of suspending the execution of a
 * program at a specific location when a program is running
 * in debug mode. Each breakpoint has an associated marker which
 * stores and persists all attributes associated with a breakpoint.
 * <p>
 * A breakpoint is defined in two parts:
 * <ol>
 * <li>By an extension of kind <code>"org.eclipse.debug.core.breakpoints"</li>
 * <li>By a marker definition that corresponds to the above breakpoint extension</li>
 * </ol>
 * <p>
 * For example, following is a definition of corresponding breakpoint
 * and breakpoint marker definitions. Note that the <code>markerType</code>
 * attribute defined by the breakpoint extension corresponds to the 
 * type of the marker definition.
 * <pre>
 * &lt;extension point="org.eclipse.debug.core.breakpoints"&gt;
 *   &lt;breakpoint 
 *      id="com.example.Breakpoint"
 *      class="com.example.Breakpoint"
 *      markerType="com.example.BreakpointMarker"&gt;
 *   &lt;/breakpoint&gt;
 * &lt;/extension&gt;
 * &lt;extension point="org.eclipse.core.resources.markers"&gt;
 *   &lt;marker 
 *      id="com.example.BreakpointMarker"
 *      super type="org.eclipse.debug.core.breakpointMarker"
 *      attribute name ="exampleAttribute"&gt;
 *   &lt;/marker&gt;
 * &lt;/extension&gt;
 * </pre>
 * <p>
 * The breakpoint manager instantiates persisted breakpoints by
 * traversing all markers that are a subtype of
 * <code>"org.eclipse.debug.core.breakpointMarker"</code>, and 
 * instantiating the class defined by the <code>class</code> attribute
 * on the associated breakpoint extension. The method <code>setMarker</code>
 * is then called to associate a marker with the breakpoint.
 * </p>
 * <p>
 * Breakpoints may or may not be registered with the breakpoint manager, and
 * are persisted and restored as such. Since marker definitions only allow
 * all or none of a specific marker type to be persisted, breakpoints define
 * a <code>PERSISTED</code> attribute for selective persistence of breakpoints
 * of the same type.
 * </p>
 * 
 * @since 2.0
 */

public interface IBreakpoint extends IAdaptable {
	
	/**
	 * Root breakpoint marker type	
	 * (value <code>"org.eclipse.debug.core.breakpoint"</code>).
	 */
	public static final String BREAKPOINT_MARKER = DebugPlugin.PLUGIN_ID + ".breakpointMarker"; //$NON-NLS-1$
	
	/**
	 * Line breakpoint marker type.
	 * (value <code>"org.eclipse.debug.core.lineBreakpoint"</code>).
	 */
	public static final String LINE_BREAKPOINT_MARKER = DebugPlugin.PLUGIN_ID + ".lineBreakpointMarker"; //$NON-NLS-1$
			
	/**
	 * Enabled breakpoint marker attribute (value <code>"enabled"</code>).
	 * The attribute is a <code>boolean</code> corresponding to the
	 * enabled state of a breakpoint.
	 *
	 * @see org.eclipse.core.resources.IMarker#getAttribute(String, boolean)
	 */
	public static final String ENABLED= "enabled"; //$NON-NLS-1$
	
	/**
	 * Breakpoint marker attribute (value <code>"id"</code>).
	 * The attribute is a <code>String</code> corresponding to the
	 * identifier of the debug model a breakpoint is
	 * associated with.
	 */
	public static final String ID= "id"; //$NON-NLS-1$
	
	/**
	 * Registered breakpoint marker attribute (value <code>"registered"</code>).
	 * The attribute is a <code>boolean</code> corresponding to
	 * whether a breakpoint has been added to the breakpoint manager.
	 *
	 * @see org.eclipse.core.resources.IMarker#getAttribute(String, boolean)
	 */
	public static final String REGISTERED= "registered"; //$NON-NLS-1$	
	
	/**
	 * Persisted breakpoint marker attribute (value <code>"persisted"</code>).
	 * The attribute is a <code>boolean</code> corresponding to
	 * whether a breakpoint is to be persisted accross workspace
	 * invocations.
	 *
	 * @see org.eclipse.core.resources.IMarker#getAttribute(String, boolean)
	 */
	public static final String PERSISTED= "persisted"; //$NON-NLS-1$		
	
	/**
	 * Attribute name for the <code>"markerType"</code> attribute of
	 * a breakpoint extension.
	 */
	public static final String MARKER_TYPE= "markerType";	 //$NON-NLS-1$

	/**
	 * Deletes this breakpoint's underlying marker, and removes
	 * this breakpoint from the breakpoint manager.
	 *
	 * @exception CoreException if deleting the underlying marker throws
	 * 	a <code>CoreException<code>.
	 */
	public void delete() throws CoreException;
	
	/**
	 * Returns the marker associated with this breakpoint, or
	 * <code>null</code> if no marker is associated with this breakpoint.
	 * 
	 * @return associated marker, or <code>null</code> if there is
	 * 	no associated marker.
	 */
	public IMarker getMarker();
	/**
	 * Sets the marker associated with this breakpoint. This method is
	 * called once at breakpoint creation.
	 * 
	 * @param marker the marker to associate with this breakpoint
	 * @exception CoreException if an error occurs accessing the marker
	 */
	public void setMarker(IMarker marker) throws CoreException;
	/**
	 * Returns the identifier of the debug model this breakpoint is
	 * associated with.
	 * 
	 * @return the identifier of the debug model this breakpoint is
	 * 	associated with
	 */
	public String getModelIdentifier();
	/**
	 * Returns whether this breakpoint is enabled
	 * 
	 * @return whether this breakpoint is enabled
	 * @exception CoreException if a <code>CoreException</code> is 
	 * 	thrown when retrieving the enabled attribute from the underlying marker
	 */
	public boolean isEnabled() throws CoreException;
	/**
	 * Sets the enabled state of this breakpoint. This has no effect
	 * if the current enabled state is the same as specified by the
	 * enabled parameter.
	 * 
	 * @param enabled  whether this breakpoint should be enabled
	 * @exception CoreException if a <code>CoreException</code> is thrown
	 * 	when setting the attribute on the underlying marker.
	 */
	public void setEnabled(boolean enabled) throws CoreException;
	
	/**
	 * Returns whether this breakpoint is currently registered with
	 * the breakpoint manager.
	 * 
	 * @return whether this breakpoint is currently registered with
	 *  the breakpoint manager
	 * @exception CoreException if a <code>CoreException</code> is thrown
	 * 	when accessing the attribute on the underlying marker
	 */
	public boolean isRegistered() throws CoreException;
	
	/**
	 * Sets whether this breakpoint is currently registered with the
	 * breakpoint manager. This method is only used by the breakpoint
	 * manager, such that when breakpoints are restored on workspace
	 * startup, the breakpoint manager knows which breakpoints should
	 * be added to the breakpoint manager. 
	 * 
	 * @param registered whether this breakpoint is registered with the
	 *   breakpoint manager
	 * @exception CoreException if a <code>CoreException</code> is thrown
	 * 	when setting the attribute on the underlying marker.
	 */
	public void setRegistered(boolean registered) throws CoreException;
	
	/**
	 * Returns whether this breakpoint is to be persisted accross
	 * workspace invocations, or when a project is closed and re-opened.
	 * Since marker definitions only allow all/none of a specific type
	 * of marker to be persisted (rathern than selective markers of a
	 * specific type), breakpoints define this functionality.
	 * 
	 * @return whether this breakpoint is to be persisted
	 * @exception CoreException if a <code>CoreException</code> is thrown
	 * 	when accessing the attribute on the underlying marker
	 */
	public boolean isPersisted() throws CoreException;
	
	/**
	 * Sets whether this breakpoint is to be persisted accorss
	 * workspace invocations, or when a project is closed and re-opened.
	 * Since marker definitions only allow all/none of a specific type of
	 * marker to be persisted (rather than selective markers of a specific
	 * type), breakpoints define this functionality. Has no effect if this
	 * breakpoint's marker definition is defined as not persisted.
	 * 
	 * @param persist whether this breakpoint is to be persisted
	 * @exception CoreException if a <code>CoreException</code> is thrown
	 * 	when setting the attribute on the underlying marker.
	 */
	public void setPersisted(boolean registered) throws CoreException;	
}


