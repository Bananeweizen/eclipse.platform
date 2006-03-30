/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.ui.mapping;

import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.ui.IMemento;

/**
 * The compare adapter provides compare support for the model objects
 * associated with a model provider.
 * <p>
 * Clients should not implement this interface but should subclass {@link SynchronizationCompareAdapter}
 * instead.
 * 
 * @since 3.2
 */
public interface ISynchronizationCompareAdapter {
	
	/**
	 * Return whether their is a compare input associated with the given object.
	 * In otherwords, return <code>true</code> if {@link #asCompareInput(ISynchronizationContext, Object) }
	 * would return a value and <code>false</code> if it would return <code>null</code>.
	 * @param context the synchronization context
	 * @param object the object.
	 * @return whether their is a compare input associated with the given object
	 */
	boolean hasCompareInput(ISynchronizationContext context, Object object);
	
	/**
	 * Return a compare input for the given model object. Creation of the input
	 * should be fast. Synchronization information calculations that are longer
	 * running should be performed in the
	 * {@link ISynchronizationCompareInput#prepareInput(org.eclipse.compare.CompareConfiguration, IProgressMonitor)}
	 * method. Clients should call this method once per context before obtaining
	 * any compare inputs from the adapter. A <code>null</code> should be
	 * returned if the model object is in-sync or otherwise cannot be compared.
	 * <p>
	 * Model providers can choose to return an instance of {@link ISynchronizationCompareInput}
	 * if they wish to tailor the compare editor used to show the compare input
	 * of provide an {@link SaveableComparison} in order to have more control
	 * over the save lifecycle during a merge.
	 * 
	 * @param context the synchronization context
	 * @param o the model object
	 * @return a compare input or <code>null</code> if the model object is
	 *         in-sync or otherwise cannot be compared.
	 */
	ICompareInput asCompareInput(ISynchronizationContext context, Object o);
	
	/**
	 * Get the name associated with the model object of the given mapping.
	 * This name sould be suitable for display to the user.
	 * @param mapping the mapping
	 * @return the name of the mapping's model object
	 */
	public String getName(ResourceMapping mapping);
	
	/**
	 * Get the path associated with the model object
	 * of the given mapping.
	 * Ths path sould be suitable for display to the user.
	 * @param mapping the mapping
	 * @return the path of the model object of the mapping
	 */
	public String getPathString(ResourceMapping mapping);
	
	/**
	 * Save the given resource mappings from this adapters 
	 * model provider into the given memento in a form
	 * that can be restored at a future time.
	 * @param mappings the resource mappings to save
	 * @param memento the memento where the mappings should be saved
	 */
	public void save(ResourceMapping[] mappings, IMemento memento);
	
	/**
	 * Restore the previosuly saved resource mappings.
	 * @param memento a memento
	 * @return the mappings restored from the given memento
	 */
	public ResourceMapping[] restore(IMemento memento);

}
