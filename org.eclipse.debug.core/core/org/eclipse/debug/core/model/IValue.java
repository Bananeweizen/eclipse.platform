package org.eclipse.debug.core.model;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
import org.eclipse.debug.core.DebugException;

/**
 * A value represents the value of a variable.  A value is of element type <code>VALUE</code>.
 * A value does not have a parent - instead it has an associated variable.
 * A value representing a complex data structure contains variables.
 * <p>
 * An implementation may choose to re-use or discard
 * values on iterative thread suspensions. Clients
 * cannot assume that values are identical or equal across
 * iterative thread suspensions and must check for equality on iterative
 * suspensions if they wish to re-use the objects.
 * </p>
 * <p>
 * An implementation that preserves equality
 * across iterative suspensions may display more desirable behavior in
 * some clients. For example, if variables are preserved
 * while stepping, a UI client would be able to update the UI incrementally,
 * rather than collapse and redraw the entire list or tree.
 * </p>
 * <p>
 * Clients may implement this interface.
 * </p>
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @see IVariable
 */


public interface IValue extends IDebugElement {
	
	/**
	 * Returns a description of the type of data this value contains
	 * or references.
	 * 
	 * @return the name of this value's reference type
	 * @exception DebugException if this method fails.  Reasons include:
	 * <ul><li>Failure communicating with the VM.  The DebugException's
	 * status code contains the underlying exception responsible for
	 * the failure.</li>
	 */
	public String getReferenceTypeName() throws DebugException;
	
	/**
	 * Returns this value as a <code>String</code>.
	 *
	 * @return a String representation of this value
	 * @exception DebugException if this method fails.  Reasons include:
	 * <ul><li>Failure communicating with the VM.  The DebugException's
	 * status code contains the underlying exception responsible for
	 * the failure.</li>
	 */
	public String getValueString() throws DebugException;
		
	/**
	 * Returns whether this value is currently allocated.
	 * <p>
	 * For example, if this value represents
	 * an object that has been garbage collected, <code>false</code> is returned.
	 * </p>
	 * @return whether this value is currently allocated
	 * @exception DebugException if this method fails.  Reasons include:
	 * <ul><li>Failure communicating with the VM.  The DebugException's
	 * status code contains the underlying exception responsible for
	 * the failure.</li>
	 */
	public boolean isAllocated() throws DebugException;
	/**
	 * Returns the visible variables in this value. An empty
	 * collection is returned if there are no visible variables.
	 * 
	 * @return an array of visible variables
	 * @exception DebugException if this method fails.  Reasons include:
	 * <ul><li>Failure communicating with the VM.  The DebugException's
	 * status code contains the underlying exception responsible for
	 * the failure.</li>
	 */
	public IVariable[] getVariables() throws DebugException;
}