package org.eclipse.debug.ui;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.ui.IEditorInput;
 
/**
 * A source presentation is used to resolve an editor in
 * which to display a debug model element, breakpoint, or
 * source element. By default, a debug model presentation
 * (which implements this interface) is used to resolve
 * editors when performing source lookup. However, a source
 * locator may override default editor resolution by implementing
 * this interface. 
 * <p>
 * Source lookup consists of the following steps:<ol>
 * <li>Locating a source element - the source locator associated
 *  with a launch is queried for the source element associated
 *  with a stack frame.</li>
 * <li>Resolving an editor in which to display a source element -
 *  by default, the debug model presentation associated with the
 *  debug model being debugged is queried for an editor input
 *  and editor id in which to display a source element. However,
 *  clients may override editor resolution by specifying a source
 *  locator that is an instance of <code>ISourcePresentation</code>.
 *  When a source presentation is specified as a source locator,
 *  the source presentation is used to resolve an editor, rather
 *  than the default debug model presentation.</li>
 * </ol>
 * </p>
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 */ 
public interface ISourcePresentation {

	/**
	 * Returns an editor input that should be used to display the given object
	 * in an editor or <code>null</code> if unable to provide an editor input
	 * for the given object.
	 *
	 * @param element a debug model element, breakpoint, or a source element
	 *  that was returned by a source locator's <code>getSourceElement(IStackFrame)</code>
	 *  method
	 * @return an editor input, or <code>null</code> if none
	 */
	public IEditorInput getEditorInput(Object element);
	
	/**
	 * Returns the id of the editor to use to display the
	 * given editor input and object, or <code>null</code> if
	 * unable to provide an editor id.
	 *
	 * @param input an editor input that was previously retrieved from this
	 *    source presentation's <code>getEditorInput</code> method
	 * @param element the object that was used in the call to
	 *  <code>getEditorInput</code>, that corresponds to the given editor
	 *  input
	 * @return an editor id, or <code>null</code> if none
	 */
	public String getEditorId(IEditorInput input, Object element);
}
