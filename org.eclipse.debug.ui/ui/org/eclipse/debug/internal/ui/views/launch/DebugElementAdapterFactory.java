/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.views.launch;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.debug.core.IExpressionManager;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IExpression;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IMemoryBlockRetrieval;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IRegisterGroup;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.internal.ui.contexts.provisional.ISourceDisplayAdapter;
import org.eclipse.debug.internal.ui.elements.adapters.AsynchronousDebugLabelAdapter;
import org.eclipse.debug.internal.ui.elements.adapters.DebugTargetContentAdapter;
import org.eclipse.debug.internal.ui.elements.adapters.ExpressionContentAdapter;
import org.eclipse.debug.internal.ui.elements.adapters.ExpressionLabelAdapter;
import org.eclipse.debug.internal.ui.elements.adapters.ExpressionManagerContentAdapter;
import org.eclipse.debug.internal.ui.elements.adapters.LauchManagerContentAdapter;
import org.eclipse.debug.internal.ui.elements.adapters.LaunchContentAdapter;
import org.eclipse.debug.internal.ui.elements.adapters.MemoryBlockContentAdapter;
import org.eclipse.debug.internal.ui.elements.adapters.MemoryBlockLabelAdapter;
import org.eclipse.debug.internal.ui.elements.adapters.MemoryRetrievalContentAdapter;
import org.eclipse.debug.internal.ui.elements.adapters.MemorySegmentLabelAdapter;
import org.eclipse.debug.internal.ui.elements.adapters.ProcessContentAdapter;
import org.eclipse.debug.internal.ui.elements.adapters.RegisterGroupContentAdapter;
import org.eclipse.debug.internal.ui.elements.adapters.StackFrameContentAdapter;
import org.eclipse.debug.internal.ui.elements.adapters.StackFrameSourceDisplayAdapter;
import org.eclipse.debug.internal.ui.elements.adapters.ThreadContentAdapter;
import org.eclipse.debug.internal.ui.elements.adapters.VariableColumnFactoryAdapter;
import org.eclipse.debug.internal.ui.elements.adapters.VariableContentAdapter;
import org.eclipse.debug.internal.ui.elements.adapters.VariableLabelAdapter;
import org.eclipse.debug.internal.ui.viewers.provisional.IAsynchronousContentAdapter;
import org.eclipse.debug.internal.ui.viewers.provisional.IAsynchronousLabelAdapter;
import org.eclipse.debug.internal.ui.viewers.provisional.IColumnEditorFactoryAdapter;
import org.eclipse.debug.internal.ui.viewers.provisional.IColumnPresenetationFactoryAdapter;
import org.eclipse.debug.internal.ui.viewers.provisional.IModelProxyFactoryAdapter;
import org.eclipse.debug.internal.ui.viewers.provisional.IModelSelectionPolicyAdapter;
import org.eclipse.debug.internal.ui.viewers.update.DefaultModelProxyFactory;
import org.eclipse.debug.internal.ui.viewers.update.DefaultSelectionPolicy;
import org.eclipse.debug.internal.ui.views.memory.renderings.MemorySegment;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.IWorkbenchAdapter2;
import org.eclipse.ui.progress.IDeferredWorkbenchAdapter;

/**
 * DebugElementAdapterFactory
 */
public class DebugElementAdapterFactory implements IAdapterFactory {
	
	private static IModelProxyFactoryAdapter fgModelProxyFactoryAdapter = new DefaultModelProxyFactory();
	private static ISourceDisplayAdapter fgStackFrameSourceDisplayAdapter = new StackFrameSourceDisplayAdapter();
    
    private static IAsynchronousLabelAdapter fgDebugLabelAdapter = new AsynchronousDebugLabelAdapter();
    private static IAsynchronousLabelAdapter fgVariableLabelAdapter = new VariableLabelAdapter();
    private static IAsynchronousLabelAdapter fgExpressionLabelAdapter = new ExpressionLabelAdapter();
    private static IAsynchronousLabelAdapter fgMemoryBlockLabelAdapter = new MemoryBlockLabelAdapter();
    private static IAsynchronousLabelAdapter fgTableRenderingLineLabelAdapter = new MemorySegmentLabelAdapter();
    
    private static IAsynchronousContentAdapter fgAsyncLaunchManager = new LauchManagerContentAdapter();
    private static IAsynchronousContentAdapter fgAsyncLaunch = new LaunchContentAdapter();
    private static IAsynchronousContentAdapter fgAsyncTarget = new DebugTargetContentAdapter();
    private static IAsynchronousContentAdapter fgAsyncProcess = new ProcessContentAdapter();
    private static IAsynchronousContentAdapter fgAsyncThread = new ThreadContentAdapter();
    private static IAsynchronousContentAdapter fgAsyncFrame = new StackFrameContentAdapter();
    private static IAsynchronousContentAdapter fgAsyncVariable = new VariableContentAdapter();
    private static IAsynchronousContentAdapter fgAsyncRegisterGroup = new RegisterGroupContentAdapter();
    private static IAsynchronousContentAdapter fgAsyncExpressionManager = new ExpressionManagerContentAdapter();
    private static IAsynchronousContentAdapter fgAsyncExpression = new ExpressionContentAdapter();
    private static IAsynchronousContentAdapter fgAsyncMemoryRetrieval = new MemoryRetrievalContentAdapter();
    private static IAsynchronousContentAdapter fgAsyncMemoryBlock = new MemoryBlockContentAdapter();
    
    private static IColumnPresenetationFactoryAdapter fgVariableColumnFactory = new VariableColumnFactoryAdapter();

    /* (non-Javadoc)
     * @see org.eclipse.core.runtime.IAdapterFactory#getAdapter(java.lang.Object, java.lang.Class)
     */
    public Object getAdapter(Object adaptableObject, Class adapterType) {
        if (adapterType.isInstance(adaptableObject)) {
			return adaptableObject;
		}
        
        if (adapterType.equals(IAsynchronousContentAdapter.class)) {
            if (adaptableObject instanceof ILaunchManager) {
                return fgAsyncLaunchManager;
            }
            if (adaptableObject instanceof ILaunch) {
                return fgAsyncLaunch;
            }
            if (adaptableObject instanceof IDebugTarget) {
                return fgAsyncTarget;
            }
            if (adaptableObject instanceof IMemoryBlockRetrieval) {
            	return fgAsyncMemoryRetrieval;
            }
            if (adaptableObject instanceof IMemoryBlock) {
            	return fgAsyncMemoryBlock;
            }
            if (adaptableObject instanceof IProcess) {
                return fgAsyncProcess;
            }
            if (adaptableObject instanceof IThread) {
                return fgAsyncThread;
            }
            if (adaptableObject instanceof IStackFrame) {
                return fgAsyncFrame;
            }
            if (adaptableObject instanceof IVariable) {
                return fgAsyncVariable;
            }
            if (adaptableObject instanceof IRegisterGroup) {
            		return fgAsyncRegisterGroup;
            }
            if (adaptableObject instanceof IExpressionManager) {
            	return fgAsyncExpressionManager;
            }
            if (adaptableObject instanceof IExpression) {
            	return fgAsyncExpression;
            }
        }
        
        if (adapterType.equals(IAsynchronousLabelAdapter.class)) {
            if (adaptableObject instanceof IExpression) {
                return fgExpressionLabelAdapter;
            }
            if (adaptableObject instanceof IVariable) {
                return fgVariableLabelAdapter;
            }
        	if (adaptableObject instanceof IMemoryBlock) {
        		return fgMemoryBlockLabelAdapter;
        	}
        	
        	if (adaptableObject instanceof MemorySegment) {
        		return fgTableRenderingLineLabelAdapter;
        	}
        	
        	return fgDebugLabelAdapter;
        }
        
        if (adapterType.equals(IModelProxyFactoryAdapter.class)) {
        	if (adaptableObject instanceof IDebugTarget ||
        			adaptableObject instanceof IProcess || adaptableObject instanceof ILaunchManager ||
        			adaptableObject instanceof IStackFrame || adaptableObject instanceof IExpressionManager ||
        			adaptableObject instanceof IExpression || adaptableObject instanceof IMemoryBlockRetrieval ||
        			adaptableObject instanceof IMemoryBlock)
        	return fgModelProxyFactoryAdapter;
        }
        
        if (adapterType.equals(ISourceDisplayAdapter.class)) {
        	if (adaptableObject instanceof IStackFrame) {
        		return fgStackFrameSourceDisplayAdapter;
        	}
        }
        
        if (adapterType.equals(IModelSelectionPolicyAdapter.class)) {
        	if (adaptableObject instanceof IDebugElement) {
        		return new DefaultSelectionPolicy((IDebugElement)adaptableObject);
        	}
        }
        
        if (adapterType.equals(IColumnPresenetationFactoryAdapter.class)) {
        	if (adaptableObject instanceof IStackFrame) {
        		return fgVariableColumnFactory;
        	}
        }
        
        if (adapterType.equals(IColumnEditorFactoryAdapter.class)) {
        	if (adaptableObject instanceof IVariable) {
        		return fgVariableColumnFactory;
        	}
        }        
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.core.runtime.IAdapterFactory#getAdapterList()
     */
    public Class[] getAdapterList() {
        return new Class[] {IWorkbenchAdapter.class, IWorkbenchAdapter2.class, IDeferredWorkbenchAdapter.class, IAsynchronousLabelAdapter.class, IAsynchronousContentAdapter.class,
        		IModelProxyFactoryAdapter.class, ISourceDisplayAdapter.class, IModelSelectionPolicyAdapter.class, IColumnPresenetationFactoryAdapter.class, IColumnEditorFactoryAdapter.class};
    }

}
