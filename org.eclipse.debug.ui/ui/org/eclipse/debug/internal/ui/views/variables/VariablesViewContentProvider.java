/*******************************************************************************
 * 
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.views.variables;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILogicalStructureType;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IIndexedValue;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.views.IDebugExceptionHandler;
import org.eclipse.debug.ui.IDebugView;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * Provide the contents for a variables viewer.
 */
public class VariablesViewContentProvider implements ITreeContentProvider {
	
	/**
	 * The view that owns this content provider.
	 */
	private IDebugView fDebugView;
	
	/**
	 * A table that maps children to their parent element
	 * such that this content provider can walk back up the
	 * parent chain (since values do not know their
	 * parent).
	 * Map of <code>IVariable</code> (child) -> <code>IVariable</code> (parent).
	 */
	private HashMap fParentCache;
	
	/**
	 * Handler for exceptions as content is retrieved
	 */
	private IDebugExceptionHandler fExceptionHandler = null;
	
	/**
	 * Flag indicating whether contributed content providers should be used or not.
	 */
	private boolean fUseObjectBrowsers;
	
	/**
	 * Constructs a new provider
	 */
	public VariablesViewContentProvider(IDebugView view) {
		fParentCache = new HashMap(10);
		setDebugView(view);
	}

	/**
	 * Returns the <code>IVariable</code>s for the given <code>IDebugElement</code>.
	 */
	public Object[] getElements(Object parent) {
		return getChildren(parent);
	}

	/**
	 * @see ITreeContentProvider#getChildren(Object)
	 */
	public Object[] getChildren(Object parent) {
		Object[] children= null;
		try {
			if (parent instanceof IStackFrame) {
				children = ((IStackFrame)parent).getVariables();
			} else if (parent instanceof IVariable) {
				IVariable variable = (IVariable)parent;
				IValue value = variable.getValue();
				children = getModelSpecificChildren(variable, value);
			}
			if (children != null) {
				cache(parent, children);
				return children;
			}
		} catch (DebugException de) {
			if (getExceptionHandler() != null) {
				getExceptionHandler().handleException(de);
			} else {
				DebugUIPlugin.log(de);
			}
		}
		return new Object[0];
	}
	
	protected IVariable[] getModelSpecificChildren(IDebugElement parent, IValue value) throws DebugException {
		if (value== null) {
			return new IVariable[0];
		}
		if (value instanceof IndexedValuePartition) {
			return value.getVariables();
		} else {
			return getValueChildren(parent, value);
		}
	}
	
	/**
	 * Returns children for the given value, creating array paritions if required
	 * 
	 * @param parent expression or variable containing the given value
	 * @param value the value to retrieve children for
	 * @return children for the given value, creating array paritions if required
	 * @throws DebugException
	 */
	protected IVariable[] getValueChildren(IDebugElement parent, IValue value) throws DebugException {
		IValue logicalValue = getLogicalValue(value);
		if (logicalValue instanceof IIndexedValue) {
			IIndexedValue indexedValue = (IIndexedValue)logicalValue;
			int valueLength = indexedValue.getSize();
			int partitionLength = getArrayPartitionSize();
			if (valueLength > partitionLength) {
				int numPartitions = valueLength / partitionLength;
				if ((valueLength % partitionLength) > 0) {
					numPartitions++;
				}
				IVariable[] partitions = new IVariable[numPartitions];
				int partition = 0;
				// partition
				int offset = 0;
				while (offset < valueLength) {
					int partitionSize = partitionLength;
					if ((valueLength - offset) < partitionLength) {
						partitionSize = valueLength - offset;
					}
					partitions[partition] = new IndexedVariablePartition(parent, indexedValue, offset, partitionSize);
					partition++;
					offset+=partitionLength;
				}
				return partitions;
			}
		}
		return logicalValue.getVariables();
	}

	/**
	 * Returns any logical value for the raw value.
	 * 
	 * @param value
	 * @return
	 */
	private IValue getLogicalValue(IValue value) {
		if (isShowLogicalStructure()) {
			ILogicalStructureType[] types = DebugPlugin.getLogicalStructureTypes(value);
			if (types.length > 0) {
				IPreferenceStore store = DebugUIPlugin.getDefault().getPreferenceStore();
				ILogicalStructureType type = null;
				boolean exist = false;
				for (int i = 0; i < types.length; i++) {
					String key = VariablesView.LOGICAL_STRUCTURE_TYPE_PREFIX + types[i].getId();
					int setting = store.getInt(key);
					// 0 = never used, 1 = on, -1 = off
					if (setting != 0) {
						exist = true;
						if (setting == 1) {
							type = types[i];
							break;
						}
					} else {
						store.setValue(types[i].getId(), -1);
					}
				}
				if (type == null && !exist) {
					type = types[0];
					// choose first by default
					store.setValue(VariablesView.LOGICAL_STRUCTURE_TYPE_PREFIX + type.getId(), 1);
				}
				if (type != null) {
					try {
						return type.getLogicalStructure(value);
					} catch (CoreException e) {
						DebugUIPlugin.log(e);
					}
				}
			}
		}
		return value;
	}

	/**
	 * Caches the given elememts as children of the given
	 * parent.
	 * 
	 * @param parent parent element
	 * @param children children elements
	 */
	protected void cache(Object parent, Object[] children) {		
		for (int i = 0; i < children.length; i++) {
			Object child = children[i];
			// avoid cycles in the cache, which can happen for
			// recursive data structures
			if (!fParentCache.containsKey(child)) {
				fParentCache.put(child, parent);
			}
		}		
	}
	
	/**
	 * @see ITreeContentProvider#getParent(Object)
	 */
	public Object getParent(Object item) {
		return fParentCache.get(item);
	}

	/**
	 * Unregisters this content provider from the debug plugin so that
	 * this object can be garbage-collected.
	 */
	public void dispose() {
		fParentCache= null;
		setExceptionHandler(null);
	}
	
	protected void clearCache() {
		if (fParentCache != null) {
			fParentCache.clear();
		}
	}
	
	/**
	 * Remove the cached parent for the given children
	 * 
	 * @param children for which to remove cached parents
	 */
	public void removeCache(Object[] children) {
		if (fParentCache == null) {
			return;
		}
		for (int i = 0; i < children.length; i++) {
			fParentCache.remove(children[i]);	
		}
	}
	
	/**
	 * @see ITreeContentProvider#hasChildren(Object)
	 */
	public boolean hasChildren(Object element) {
		try {
			if (element instanceof IVariable) {
				if (element instanceof IndexedVariablePartition) {
					return true;
				}
				element = ((IVariable)element).getValue();
			}
			if (element instanceof IValue) {
				return ((IValue)element).hasVariables();
			}
			if (element instanceof IStackFrame) {
				return ((IStackFrame)element).hasVariables();
			}
		} catch (DebugException de) {
			DebugUIPlugin.log(de);
			return false;
		}
		return false;
	}
		
	/**
	 * @see IContentProvider#inputChanged(Viewer, Object, Object)
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		clearCache();
	}
	
	/**
	 * Return all cached decendants of the given parent.
	 * 
	 * @param parent the element whose decendants are to be calculated
	 * @return list of decendants that have been cached for
	 *  the given parent
	 */
	public List getCachedDecendants(Object parent) {
		Iterator children = fParentCache.keySet().iterator();
		List cachedChildren = new ArrayList(10);
		while (children.hasNext()) {
			Object child = children.next();
			if (isCachedDecendant(child, parent)) {
				cachedChildren.add(child);
			}
		}
		return cachedChildren;
	}
	
	/**
	 * Returns whether the given child is a cached descendant
	 * of the given parent.
	 * 
	 * @return whether the given child is a cached descendant
	 * of the given parent
	 */
	protected boolean isCachedDecendant(Object child, Object parent) {
		Object p = getParent(child);
		while (p != null) {
			if (p.equals(parent)) {
				return true;
			}
			p = getParent(p);
		}
		return false;
	}
	
	/**
	 * Extract the debug model id from the specified <code>IDebugElement</code>
	 * and return it.
	 */
	protected  String getDebugModelId(IDebugElement debugElement) {
		return debugElement.getModelIdentifier();
	}

	/**
	 * Sets an exception handler for this content provider.
	 * 
	 * @param handler debug exception handler or <code>null</code>
	 */
	protected void setExceptionHandler(IDebugExceptionHandler handler) {
		fExceptionHandler = handler;
	}
	
	/**
	 * Returns the exception handler for this content provider.
	 * 
	 * @return debug exception handler or <code>null</code>
	 */
	protected IDebugExceptionHandler getExceptionHandler() {
		return fExceptionHandler;
	}	
	
	/** 
	 * Show logical structure of values 
	 */
	public void setShowLogicalStructure(boolean flag) {
		fUseObjectBrowsers = flag;
	}
	
	public boolean isShowLogicalStructure() {
		return fUseObjectBrowsers;
	}
	
	private void setDebugView(IDebugView view) {
		fDebugView = view;
	}

	protected IDebugView getDebugView() {
		return fDebugView;
	}
	
	/**
	 * Returns the number of entries that should be displayed in each
	 * partition of an indexed collection.
	 * 
	 * @return the number of entries that should be displayed in each
	 * partition of an indexed collection
	 */
	protected int getArrayPartitionSize() {
		return ((VariablesView)getDebugView()).getArrayPartitionSize();
	}
	
}

