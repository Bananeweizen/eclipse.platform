/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Pawel Piech - Wind River - Bug 205335: ModelContentProvider does not cancel stale updates when switching viewer input
 *     Wind River Systems - Fix for viewer state save/restore [188704] 
 *     Pawel Piech (Wind River) - added support for a virtual tree model viewer (Bug 242489)
 *******************************************************************************/
package org.eclipse.debug.internal.ui.viewers.model;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.IRequest;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IChildrenUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementCompareRequest;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementMementoProvider;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementMementoRequest;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelChangedListener;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDeltaVisitor;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelProxy;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelProxyFactory;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerUpdateListener;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ModelDelta;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.progress.WorkbenchJob;

/**
 * Content provider for a virtual viewer.
 * 
 * @since 3.3
 */
abstract class ModelContentProvider implements IContentProvider, IModelChangedListener {

	private ITreeModelContentProviderTarget fViewer;

    /**
     * Flag indicating whether the viewer should ignore SELECT, REVEAL, 
     * EXPAND, and COLLAPSE flags of {@link IModelDelta} coming from the model.
     */
	private boolean fSuppressModelControlRequests = false;
	
	private Map fModelProxies = new LinkedHashMap(); // model proxy by element
	
	/**
	 * Map of nodes that have been filtered from the viewer.
	 */
	private FilterTransform fTransform = new FilterTransform();
	
	/**
	 * Model listeners
	 */
	private ListenerList fModelListeners = new ListenerList();
	
	/**
	 * Update listeners
	 */
	private ListenerList fUpdateListeners = new ListenerList();
	
	/**
	 * Map of updates in progress: element path -> list of requests
	 */
	private Map fRequestsInProgress = new HashMap();
	
	/**
	 * Map of dependent requests waiting for parent requests to complete:
	 *  element path -> list of requests
	 */
	private Map fWaitingRequests = new HashMap();
	
	/**
	 * Map of viewer states keyed by viewer input mementos
	 */
	private Map fViewerStates = new LRUMap(20);
	
	/**
	 * Pending viewer state to be restored
	 */
	private ModelDelta fPendingState = null;

	private static class CompareRequestKey {
		CompareRequestKey(TreePath path, IModelDelta delta) {
			fPath = path;
			fDelta = delta;
		}
		TreePath fPath;
		IModelDelta fDelta;
		
		public boolean equals(Object obj) {
			if (obj instanceof CompareRequestKey) {
				CompareRequestKey key = (CompareRequestKey)obj;
				return key.fDelta.equals(fDelta) && key.fPath.equals(fPath); 
			}
			return false;
		}
		 
		public int hashCode() {
			return fDelta.hashCode() + fPath.hashCode();
		}
	}
	
	private Map fCompareRequestsInProgress = new HashMap();
	
	/**
	 * Set of IMementoManager's that are currently saving state
	 */
	private Set fPendingStateSaves = new HashSet();
	
	/**
	 * Used to queue a viewer input for state restore
	 */
	private Object fQueuedRestore = null;
	
	/**
	 * Used to determine when restoration delta has been processed
	 */
	class CheckState implements IModelDeltaVisitor {
		private boolean complete = true;
		private IModelDelta topDelta = null;
		/* (non-Javadoc)
		 * @see org.eclipse.debug.internal.ui.viewers.provisional.IModelDeltaVisitor#visit(org.eclipse.debug.internal.ui.viewers.provisional.IModelDelta, int)
		 */
		public boolean visit(IModelDelta delta, int depth) {
			if (delta.getFlags() != IModelDelta.NO_CHANGE) {
				if (delta.getFlags() == IModelDelta.REVEAL && !(delta.getElement() instanceof IMemento)) {
					topDelta = delta;
				} else {
					complete = false;
					return false;
				}
			}
			return true;
		}
		
		public boolean isComplete() {
			return complete;
		}
		
		public IModelDelta getTopItemDelta() {
			return topDelta;
		}
	}
	
	/**
	 * LRU cache for viewer states 
	 */
	class LRUMap extends LinkedHashMap {
		private static final long serialVersionUID= 1L;
		private int fMaxSize;
		LRUMap(int maxSize) {
			super();
			fMaxSize = maxSize;
		}
		protected boolean removeEldestEntry(Entry eldest) {
			return size() > fMaxSize;
		}	
	}
	
	/**
	 * Update type constants
	 */
	static final int UPDATE_SEQUENCE_BEGINS = 0;
	static final int UPDATE_SEQUENCE_COMPLETE = 1;
	static final int UPDATE_BEGINS = 2;
	static final int UPDATE_COMPLETE = 3;
	
	/**
	 * Constant for an empty tree path.
	 */
	protected static final TreePath EMPTY_TREE_PATH = new TreePath(new Object[]{});
	
	// debug flags
    public static String DEBUG_PRESENTATION_ID = null;
	public static boolean DEBUG_CONTENT_PROVIDER = false;
	public static boolean DEBUG_UPDATE_SEQUENCE = false;
    public static boolean DEBUG_STATE_SAVE_RESTORE = false;
    public static boolean DEBUG_DELTAS = false;

	static {
        DEBUG_PRESENTATION_ID = Platform.getDebugOption("org.eclipse.debug.ui/debug/viewers/presentationId"); //$NON-NLS-1$
        if (!DebugUIPlugin.DEBUG || "".equals(DEBUG_PRESENTATION_ID)) { //$NON-NLS-1$
            DEBUG_PRESENTATION_ID = null;
        }
		DEBUG_CONTENT_PROVIDER = DebugUIPlugin.DEBUG && "true".equals( //$NON-NLS-1$
		 Platform.getDebugOption("org.eclipse.debug.ui/debug/viewers/contentProvider")); //$NON-NLS-1$
		DEBUG_UPDATE_SEQUENCE = DebugUIPlugin.DEBUG && "true".equals( //$NON-NLS-1$
		 Platform.getDebugOption("org.eclipse.debug.ui/debug/viewers/updateSequence")); //$NON-NLS-1$
		DEBUG_STATE_SAVE_RESTORE = DebugUIPlugin.DEBUG && "true".equals( //$NON-NLS-1$
	            Platform.getDebugOption("org.eclipse.debug.ui/debug/viewers/stateSaveRestore")); //$NON-NLS-1$
        DEBUG_DELTAS = DebugUIPlugin.DEBUG && "true".equals( //$NON-NLS-1$
                Platform.getDebugOption("org.eclipse.debug.ui/debug/viewers/deltas")); //$NON-NLS-1$
	} 	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	public synchronized void dispose() {
		// cancel pending updates
		synchronized (fRequestsInProgress) {
			Iterator iterator = fRequestsInProgress.values().iterator();
			while (iterator.hasNext()) {
				List requests = (List) iterator.next();
				Iterator reqIter = requests.iterator();
				while (reqIter.hasNext()) {
					((IRequest) reqIter.next()).cancel();
				}
			}
			fWaitingRequests.clear();
		}
		fModelListeners.clear();
		fUpdateListeners.clear();
		disposeAllModelProxies();
		fViewer = null;
	}
	
	public synchronized boolean isDisposed() {
		return fViewer == null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer,
	 *      java.lang.Object, java.lang.Object)
	 */
	public synchronized void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		fViewer = (ITreeModelContentProviderTarget)viewer;
		if (oldInput != null) {
			for (Iterator itr = fCompareRequestsInProgress.values().iterator(); itr.hasNext(); ) {
				((ElementCompareRequest)itr.next()).cancel();
				itr.remove();
			}
			saveViewerState(oldInput);
		}
		if (newInput != oldInput) {
			cancelSubtreeUpdates(TreePath.EMPTY);
			disposeAllModelProxies();
            cancelSubtreeUpdates(TreePath.EMPTY);			
			fTransform.clear();
			if (newInput != null) {
				installModelProxy(newInput);
				restoreViewerState(newInput);
			}
		}
	}

	/**
	 * Restores the viewer state unless a save is taking place.  If a save is
	 * taking place, the restore is queued.
	 * @param input viewer input
	 */
	protected synchronized void restoreViewerState(final Object input) {
		fPendingState = null;
		if (isSavingState()) {
			fQueuedRestore = input;
		} else {
			startRestoreViewerState(input);
		}
	}
	
	/**
	 * Restores viewer state for the given input
	 * 
	 * @param input viewer input
	 */
	private synchronized void startRestoreViewerState(final Object input) {
		fPendingState = null;
		final IElementMementoProvider defaultProvider = ViewerAdapterService.getMementoProvider(input);
		if (defaultProvider != null) {
			// build a model delta representing expansion and selection state
			final ModelDelta delta = new ModelDelta(input, IModelDelta.NO_CHANGE);
			final XMLMemento inputMemento = XMLMemento.createWriteRoot("VIEWER_INPUT_MEMENTO"); //$NON-NLS-1$
			final IMementoManager manager = new IMementoManager() {
			
				private IElementMementoRequest fRequest;
				
				/* (non-Javadoc)
				 * @see org.eclipse.debug.internal.ui.viewers.model.provisional.viewers.IMementoManager#requestComplete(org.eclipse.debug.internal.ui.viewers.model.provisional.IElementMementoRequest)
				 */
				public synchronized void requestComplete(IElementMementoRequest request) {
					if (!request.isCanceled() && (request.getStatus() == null || request.getStatus().isOK())) {
						XMLMemento keyMemento = (XMLMemento) delta.getElement();
						StringWriter writer = new StringWriter();
						try {
							keyMemento.save(writer);
							final String keyMementoString = writer.toString();
							final ModelDelta stateDelta = (ModelDelta) fViewerStates.get(keyMementoString);
							if (stateDelta != null) {
								stateDelta.setElement(input);
								// begin restoration
								UIJob job = new UIJob("restore state") { //$NON-NLS-1$
									public IStatus runInUIThread(IProgressMonitor monitor) {
										if (!isDisposed() && input.equals(getViewer().getInput())) {
			                                if (DEBUG_STATE_SAVE_RESTORE && (DEBUG_PRESENTATION_ID == null || DEBUG_PRESENTATION_ID.equals(getPresentationContext().getId()))) {
			                                    System.out.println("RESTORE: " + stateDelta.toString()); //$NON-NLS-1$
			                                }
										    fViewerStates.remove(keyMementoString);
											fPendingState = stateDelta;
											doInitialRestore(fPendingState);
										}
										return Status.OK_STATUS;
									}
								
								};
								job.setSystem(true);
								job.schedule();
							}
						} catch (IOException e) {
							DebugUIPlugin.log(e);
						}
					}
				}
			
				/* (non-Javadoc)
				 * @see org.eclipse.debug.internal.ui.viewers.model.provisional.viewers.IMementoManager#processReqeusts()
				 */
				public void processReqeusts() {
					defaultProvider.encodeElements(new IElementMementoRequest[]{fRequest});
				}
			
				/* (non-Javadoc)
				 * @see org.eclipse.debug.internal.ui.viewers.model.provisional.viewers.IMementoManager#addRequest(org.eclipse.debug.internal.ui.viewers.model.provisional.IElementMementoRequest)
				 */
				public synchronized void addRequest(IElementMementoRequest req) {
					fRequest = req;
				}
			
			};
			manager.addRequest(new ElementMementoRequest(ModelContentProvider.this, getViewer().getInput(), manager, getPresentationContext(),
									delta.getElement(), getViewerTreePath(delta), inputMemento, delta));
			manager.processReqeusts();
		}
	}
	
    /**
     * Restore selection/expansion based on items already in the viewer
     */
	protected abstract void doInitialRestore(ModelDelta delta);

	/**
     * @param delta
     */
	abstract void doRestore(final ModelDelta delta, boolean knowsHasChildren, boolean knowsChildCount);

	/**
	 * Perform any restoration required for the given tree path.
	 * 
	 * @param path
	 */
	protected synchronized void doRestore(final TreePath path, final int modelIndex, final boolean knowsHasChildren, final boolean knowsChildCount) {
		if (fPendingState == null) { 
			return;
		}
		
		IModelDeltaVisitor visitor = new IModelDeltaVisitor() {
			public boolean visit(final IModelDelta delta, int depth) {
				
				Object element = delta.getElement();
				Object potentialMatch = depth != 0 ? path.getSegment(depth - 1) : getViewer().getInput();
				// Only process if the depth in the delta matches the tree path.
				if (depth == path.getSegmentCount()) {
					if (element instanceof IMemento) {
						IElementMementoProvider provider = ViewerAdapterService.getMementoProvider(potentialMatch);
						if (provider == null) {
							provider = ViewerAdapterService.getMementoProvider(getViewer().getInput());
						}
						if (provider != null) {
							CompareRequestKey key = new CompareRequestKey(path, delta);
							ElementCompareRequest existingRequest = (ElementCompareRequest)fCompareRequestsInProgress.get(key); 
							if (existingRequest != null) {
								// Check all the running compare updates for a matching tree path.  
								// If found, just update the flags.  
								existingRequest.setKnowsHasChildren(knowsHasChildren);
								existingRequest.setKnowsChildCount(knowsChildCount);								
							} else {
								// Start a new compare request
								ElementCompareRequest compareRequest = new ElementCompareRequest(
										ModelContentProvider.this, getViewer().getInput(), potentialMatch, path, (IMemento) element, (ModelDelta)delta, modelIndex, knowsHasChildren, knowsChildCount);
								fCompareRequestsInProgress.put(key, compareRequest);
								provider.compareElements(new IElementCompareRequest[]{ compareRequest });
							}
						}
					} else if (element.equals(potentialMatch)) {
						// Element comparison already succeeded, and it matches our element.
						// Call restore with delta to process the delta flags.
						doRestore((ModelDelta)delta, knowsHasChildren, knowsChildCount);
					}
					return false;
				} 
				// Only follow the paths that match the delta.
				return element.equals(potentialMatch);
			}
		};
		fPendingState.accept(visitor);
	}

	void compareFinished(ElementCompareRequest request, ModelDelta delta) {
		fCompareRequestsInProgress.remove(request);
		if (!request.isCanceled()) {
			if (request.isEqual()) {
				delta.setElement(request.getElement());
				doRestore(delta, request.knowsHasChildren(), request.knowChildCount());
			} else if (request.getModelIndex() != -1){
				// Comparison failed. 
				// Check if the delta has a reveal flag, and if its index matches the index
				// of the element that it was compared against.  If this is the case, 
				// strip the reveal flag from the delta as it is most likely not applicable 
				// anymore.
				if ( (delta.getFlags() & IModelDelta.REVEAL) != 0 && delta.getIndex() == request.getModelIndex() ) {
	                delta.setFlags(delta.getFlags() & ~IModelDelta.REVEAL);
				}
			}
		}
	}
	
	/**
	 * Saves the viewer's state for the previous input.
	 * 	 * @param oldInput
	 */
	protected void saveViewerState(Object input) {
		IElementMementoProvider stateProvider = ViewerAdapterService.getMementoProvider(input);
		if (stateProvider != null) {
            if (DEBUG_STATE_SAVE_RESTORE && (DEBUG_PRESENTATION_ID == null || DEBUG_PRESENTATION_ID.equals(getPresentationContext().getId()))) {
                System.out.println("SAVE BEGIN: " + input); //$NON-NLS-1$
            }

			// build a model delta representing expansion and selection state
			final ModelDelta saveDeltaRoot = new ModelDelta(input, IModelDelta.NO_CHANGE);
            buildViewerState(saveDeltaRoot);
            if (DEBUG_STATE_SAVE_RESTORE && (DEBUG_PRESENTATION_ID == null || DEBUG_PRESENTATION_ID.equals(getPresentationContext().getId()))) {
                System.out.println("SAVE DELTA FROM VIEW: " + saveDeltaRoot); //$NON-NLS-1$
            }
		   
			if (fPendingState != null) {
			    // If the restore for the current input was never completed, preserve 
			    // that restore along with the restore that was completed.
                if (DEBUG_STATE_SAVE_RESTORE && (DEBUG_PRESENTATION_ID == null || DEBUG_PRESENTATION_ID.equals(getPresentationContext().getId()))) {
                    System.out.println("SAVE OUTSTANDING RESTORE: " + fPendingState); //$NON-NLS-1$
                }
                
                IModelDeltaVisitor pendingStateVisitor = new IModelDeltaVisitor() {
                    public boolean visit(IModelDelta pendingDeltaNode, int depth) {
                        // Ignore the top element.
                        if (pendingDeltaNode.getParentDelta() == null) {
                            return true;
                        }
                        
                        // Find the node in the save delta which is the parent 
                        // of to the current pending delta node.
                        // If the parent node cannot be found, it means that 
                        // most likely the user collapsed the parent node before
                        // the children were ever expanded.
                        // If the pending state node already exists in the parent
                        // node, it is already processed and we can skip it.
                        // If the pending state node does not contain any flags, 
                        // we can also skip it.
                        ModelDelta saveDeltaNode = findSaveDelta(saveDeltaRoot, pendingDeltaNode);
                        if (saveDeltaNode != null && 
                            !isDeltaInParent(pendingDeltaNode, saveDeltaNode) && 
                            pendingDeltaNode.getFlags() != IModelDelta.NO_CHANGE) 
                        {
                            // There should be only one delta element with 
                            // the REVEAL flag in the entire save delta.  The
                            // reveal flag in the pending delta trumps the one
                            // in the save delta because most likely the restore
                            // operation did not yet complete the reveal 
                            // operation.
                            if ( (pendingDeltaNode.getFlags() & IModelDelta.REVEAL) != 0) {
                                clearRevealFlag(saveDeltaRoot);
                            }
                            saveDeltaNode.setChildCount(pendingDeltaNode.getParentDelta().getChildCount());
                            copyIntoDelta(pendingDeltaNode, saveDeltaNode);
                        } else {
                            if (DEBUG_STATE_SAVE_RESTORE && (DEBUG_PRESENTATION_ID == null || DEBUG_PRESENTATION_ID.equals(getPresentationContext().getId()))) {
                                System.out.println(" Skipping: " + pendingDeltaNode.getElement()); //$NON-NLS-1$
                            }
                        }
                        
                        // If the pending delta node has a memento element, its 
                        // children should also be mementos therefore the copy
                        // delta operation should have added all the children
                        // of this pending delta node into the save delta. 
                        if (pendingDeltaNode.getElement() instanceof IMemento) {
                            return false;
                        } else {
                            return pendingDeltaNode.getChildCount() > 0;
                        }
                    }
                };
                fPendingState.accept(pendingStateVisitor);
		    }		            

			if (saveDeltaRoot.getChildDeltas().length > 0) {
				// encode delta with mementos in place of elements, in non-UI thread
				encodeDelta(saveDeltaRoot, stateProvider);
			} else {
	            if (DEBUG_STATE_SAVE_RESTORE && (DEBUG_PRESENTATION_ID == null || DEBUG_PRESENTATION_ID.equals(getPresentationContext().getId()))) {
	                System.out.println("SAVE CANCELED, NO DATA"); //$NON-NLS-1$
	            }			
			}
		}
	}
	
	private void clearRevealFlag(ModelDelta saveRootDelta) {
        IModelDeltaVisitor clearDeltaVisitor = new IModelDeltaVisitor() {
            public boolean visit(IModelDelta delta, int depth) {
                if ( (delta.getFlags() & IModelDelta.REVEAL) != 0) {
                    ((ModelDelta)delta).setFlags(delta.getFlags() & ~IModelDelta.REVEAL);
                }
                return true;
            }
        };
        saveRootDelta.accept(clearDeltaVisitor);
	}
	
	private ModelDelta findSaveDelta(ModelDelta saveDeltaRoot, IModelDelta pendingStateDelta) {
	    // Create a path of elements to the pendingStateDelta.
	    LinkedList deltaPath = new LinkedList();
    	IModelDelta delta = pendingStateDelta;
	    while (delta.getParentDelta() != null) {
	        delta = delta.getParentDelta();
	        deltaPath.addFirst(delta);
	    }
	    
	    // For each element in the patch of the pendingStateDelta, find the corresponding
	    // element in the partially restored delta being saved.  
	    Iterator itr = deltaPath.iterator();
	    // Skip the root element
	    itr.next();
	    ModelDelta saveDelta = saveDeltaRoot;
	    outer: while (itr.hasNext()) {
	        IModelDelta itrDelta = (IModelDelta)itr.next();
	        for (int i = 0; i < saveDelta.getChildDeltas().length; i++) {
	            if ( deltasEqual(saveDelta.getChildDeltas()[i], itrDelta) ) {
	                saveDelta = (ModelDelta)saveDelta.getChildDeltas()[i];
	                continue outer;
	            }
	        }
	        return null;
	    }
	    return saveDelta;
	}
	
	private boolean deltasEqual(IModelDelta d1, IModelDelta d2) {
	    // Note: don't compare the child count, because it is
	    // incorrect for nodes which have not been expanded yet.
	    return d1.getElement().equals(d2.getElement()) &&
	           d1.getIndex() == d2.getIndex();
	}

	private boolean isDeltaInParent(IModelDelta delta, ModelDelta destParent) {
        for (int i = 0; i < destParent.getChildDeltas().length; i++) {
            if ( deltasEqual(destParent.getChildDeltas()[i], delta) ) {
                return true;
            }
        }
        return false;
	}	

	private void copyIntoDelta(IModelDelta delta, ModelDelta destParent) {
	    // Search the destination and make sure that the same delta 
	    // doesn't exist already.
        
        ModelDelta newDelta = destParent.addNode(delta.getElement(), delta.getIndex(), delta.getFlags(), delta.getChildCount());
        for (int i = 0; i < delta.getChildDeltas().length; i++) {
            copyIntoDelta(delta.getChildDeltas()[i], newDelta);
        }
	}

	/**
	 * Encodes delta elements into mementos using the given provider.
	 *  
	 * @param delta
	 * @param stateProvider
	 */
	protected void encodeDelta(final ModelDelta rootDelta, final IElementMementoProvider defaultProvider) {
		final XMLMemento inputMemento = XMLMemento.createWriteRoot("VIEWER_INPUT_MEMENTO"); //$NON-NLS-1$
		final XMLMemento childrenMemento = XMLMemento.createWriteRoot("CHILDREN_MEMENTO"); //$NON-NLS-1$
		final IMementoManager manager = new IMementoManager() {
		
			/**
			 * list of memento requests
			 */
			private List requests = new ArrayList();
			private boolean abort = false; 
			
			/* (non-Javadoc)
			 * @see org.eclipse.debug.internal.ui.viewers.model.provisional.viewers.IMementoManager#requestComplete(org.eclipse.debug.internal.ui.viewers.model.provisional.IElementMementoRequest)
			 */
			public synchronized void requestComplete(IElementMementoRequest request) {
				if (!abort) {
					if (!request.isCanceled() && (request.getStatus() == null || request.getStatus().isOK())) {
						requests.remove(request);
						if (requests.isEmpty()) {
							XMLMemento keyMemento = (XMLMemento) rootDelta.getElement();
							StringWriter writer = new StringWriter();
							try {
								keyMemento.save(writer);
								fViewerStates.put(writer.toString(), rootDelta);
							} catch (IOException e) {
								DebugUIPlugin.log(e);
							}
	                        if (DEBUG_STATE_SAVE_RESTORE && (DEBUG_PRESENTATION_ID == null || DEBUG_PRESENTATION_ID.equals(getPresentationContext().getId()))) {
	                            System.out.println("SAVE COMPLETED: " + rootDelta); //$NON-NLS-1$
	                        }
							stateSaveComplete(this);
						}
					} else {
						abort = true;
						Iterator iterator = requests.iterator();
						while (iterator.hasNext()) {
							IElementMementoRequest req = (IElementMementoRequest) iterator.next();
							req.cancel();
						}
						requests.clear();
		                if (DEBUG_STATE_SAVE_RESTORE && (DEBUG_PRESENTATION_ID == null || DEBUG_PRESENTATION_ID.equals(getPresentationContext().getId()))) {
		                    System.out.println("SAVE ABORTED: " + rootDelta.getElement()); //$NON-NLS-1$
		                }
						stateSaveComplete(this);
					}
				}
			}
		
			/* (non-Javadoc)
			 * @see org.eclipse.debug.internal.ui.viewers.model.provisional.viewers.IMementoManager#processReqeusts()
			 */
			public synchronized void processReqeusts() {
				Map providers = new HashMap();
				Iterator iterator = requests.iterator();
				while (iterator.hasNext()) {
					IElementMementoRequest request = (IElementMementoRequest) iterator.next();
					IElementMementoProvider provider = ViewerAdapterService.getMementoProvider(request.getElement());
					if (provider == null) {
						provider = defaultProvider;
					}
					List reqs = (List) providers.get(provider);
					if (reqs == null) {
						reqs = new ArrayList();
						providers.put(provider, reqs);
					}
					reqs.add(request);
				}
				iterator = providers.entrySet().iterator();
				while (iterator.hasNext()) {
					Entry entry = (Entry) iterator.next();
					IElementMementoProvider provider = (IElementMementoProvider) entry.getKey();
					List reqs = (List) entry.getValue();
					provider.encodeElements((IElementMementoRequest[]) reqs.toArray(new IElementMementoRequest[reqs.size()]));
				}
			}
		
			/* (non-Javadoc)
			 * @see org.eclipse.debug.internal.ui.viewers.model.provisional.viewers.IMementoManager#addRequest(org.eclipse.debug.internal.ui.viewers.model.provisional.IElementMementoRequest)
			 */
			public synchronized void addRequest(IElementMementoRequest request) {
				requests.add(request);
			}
		
		};
		IModelDeltaVisitor visitor = new IModelDeltaVisitor() {
			public boolean visit(IModelDelta delta, int depth) {
				if (delta.getParentDelta() == null) {
					manager.addRequest(
						new ElementMementoRequest(ModelContentProvider.this, getViewer().getInput(), manager, getPresentationContext(),
							delta.getElement(), getViewerTreePath(delta), inputMemento, (ModelDelta)delta));
			} else {
			    if (!(delta.getElement() instanceof XMLMemento)) {
					manager.addRequest(
						new ElementMementoRequest(ModelContentProvider.this, getViewer().getInput(), manager, getPresentationContext(),
								delta.getElement(), getViewerTreePath(delta), childrenMemento.createChild("CHILD_ELEMENT"), (ModelDelta)delta)); //$NON-NLS-1$
			    }
			}
			return true;
		}
		};
		rootDelta.accept(visitor);
		stateSaveStarted(manager);
		manager.processReqeusts();
	}
	
	/**
	 * Called when a state save is starting.
	 * 
	 * @param manager
	 */
	private synchronized void stateSaveStarted(IMementoManager manager) {
		fPendingStateSaves.add(manager);
	}
	
	/**
	 * Called when a state save is complete.
	 * 
	 * @param manager
	 */
	private synchronized void stateSaveComplete(IMementoManager manager) {
		fPendingStateSaves.remove(manager);
		if (fQueuedRestore != null) {
			Object temp = fQueuedRestore;
			fQueuedRestore = null;
			restoreViewerState(temp);
		}
	}
	
	/**
	 * Returns whether any state saving is in progress.
	 * 
	 * @return whether any state saving is in progress
	 */
	private synchronized boolean isSavingState() {
		return !fPendingStateSaves.isEmpty();
	}

	/**
	 * Builds a delta with the given root delta for expansion/selection state.
	 * 
	 * @param delta root delta
	 */
	protected abstract void buildViewerState(ModelDelta delta);

	/**
	 * Uninstalls the model proxy installed for the given element, if any.
	 * 
	 * @param element
	 */
	protected synchronized void disposeModelProxy(Object element) {
		IModelProxy proxy = (IModelProxy) fModelProxies.remove(element);
		if (proxy != null) {
			proxy.dispose();
		}
	}

	/**
	 * Uninstalls each model proxy
	 */
	protected synchronized void disposeAllModelProxies() {
		Iterator updatePolicies = fModelProxies.values().iterator();
		while (updatePolicies.hasNext()) {
			IModelProxy proxy = (IModelProxy) updatePolicies.next();
			proxy.dispose();
		}
		fModelProxies.clear();
	}

	protected synchronized IModelProxy[] getModelProxies() {
	    return (IModelProxy[])fModelProxies.values().toArray(new IModelProxy[fModelProxies.size()]);
	}
	
	protected synchronized IModelProxy getElementProxy(TreePath path) {
	    for (int i = path.getSegmentCount() - 1; i >= 0; i--) {
	        IModelProxy proxy = (IModelProxy)fModelProxies.get(path.getSegment(i));
	        if (proxy != null) {
	            return proxy;
	        }
	    }
	    return null;
	}
	
	/**
	 * Installs the model proxy for the given element into this content provider
	 * if not already installed.
	 * 
	 * @param element
	 *            element to install an update policy for
	 */
	protected synchronized void installModelProxy(Object element) {
		if (!fModelProxies.containsKey(element)) {
			IModelProxyFactory modelProxyFactory = ViewerAdapterService.getModelProxyFactory(element);
			if (modelProxyFactory != null) {
				final IModelProxy proxy = modelProxyFactory.createModelProxy(
						element, getPresentationContext());
				if (proxy != null) {
					fModelProxies.put(element, proxy);
					Job job = new Job("Model Proxy installed notification job") {//$NON-NLS-1$
						protected IStatus run(IProgressMonitor monitor) {
							if (!monitor.isCanceled()) {
							    IPresentationContext context = null;
							    Viewer viewer = null;
								synchronized (ModelContentProvider.this) {
									if (!isDisposed()) {
									    context = getPresentationContext();
									    viewer = (Viewer)getViewer();
									}
								}
								if (context != null && !proxy.isDisposed()) {
    								proxy.init(context);
    								proxy.addModelChangedListener(ModelContentProvider.this);
    								proxy.installed(viewer);
								}
							}
							return Status.OK_STATUS;
						}
						/* (non-Javadoc)
						 * @see org.eclipse.core.runtime.jobs.Job#shouldRun()
						 */
						public boolean shouldRun() {
						    return !isDisposed();
						}
					};
					job.setSystem(true);
					job.schedule();
				}
			}
		}
	}

	/**
	 * Returns the presentation context for this content provider.
	 * 
	 * @return presentation context
	 */
	protected abstract IPresentationContext getPresentationContext();

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.debug.internal.ui.viewers.provisional.IModelChangedListener#modelChanged(org.eclipse.debug.internal.ui.viewers.provisional.IModelDelta)
	 */
	public synchronized void modelChanged(final IModelDelta delta, final IModelProxy proxy) {
	    if (fViewer != null && !proxy.isDisposed()) {
    		WorkbenchJob job = new WorkbenchJob(fViewer.getDisplay(), "process model delta") { //$NON-NLS-1$
    			public IStatus runInUIThread(IProgressMonitor monitor) {
                    if (!proxy.isDisposed()) {
                        if (DEBUG_DELTAS && (DEBUG_PRESENTATION_ID == null || DEBUG_PRESENTATION_ID.equals(getPresentationContext().getId()))) {
                            DebugUIPlugin.debug("RECEIVED DELTA: " + delta.toString()); //$NON-NLS-1$
                        }
    
                        updateNodes(new IModelDelta[] { delta }, false);
                        
                        // Call model listeners after updating the viewer model.
                        Object[] listeners = fModelListeners.getListeners();
                        for (int i = 0; i < listeners.length; i++) {
                            ((IModelChangedListener)listeners[i]).modelChanged(delta, proxy);
                        }
                    }
                    return Status.OK_STATUS;
    	        }
    		};
    		job.setSystem(true);
    		job.schedule();
	    }
	}

	/**
	 * Turns on the mode which causes the model viewer to ignore SELECT, 
	 * EXPAND, and COLLAPSE flags of {@link IModelDelta}.
	 *  
	 * @param suppress If <code>true</code> it turns on the suppress mode.
	 */
    public void setSuppressModelControlDeltas(boolean suppress) {
        fSuppressModelControlRequests = suppress;
    }
    
    /**
     * Returns true if the viewer is currently in the mode to ignore SELECT, 
     * REVEAL, EXPAND, and COLLAPSE flags of {@link IModelDelta}.
     *  
     * @return Returns <code>true</code> if in suppress mode.
     */
    public boolean isSuppressModelControlDeltas() {
        return fSuppressModelControlRequests;
    }

    public void updateModel(IModelDelta delta) {
        updateNodes(new IModelDelta[] { delta }, true);
    }
    
    /**
     * Updates the viewer with the following deltas. 
     * 
     * @param nodes Model deltas to be processed.
     * @param override If true, it overrides the mode which suppresses 
     * processing of SELECT, REVEAL, EXPAND, COLLAPSE flags of 
     * {@link IModelDelta}.
     */
	protected void updateNodes(IModelDelta[] nodes, boolean override) {
		for (int i = 0; i < nodes.length; i++) {
			IModelDelta node = nodes[i];
			int flags = node.getFlags();

			if ((flags & IModelDelta.ADDED) != 0) {
				handleAdd(node);
			}
			if ((flags & IModelDelta.REMOVED) != 0) {
				handleRemove(node);
			}
			if ((flags & IModelDelta.CONTENT) != 0) {
				handleContent(node);
			}
			if ((flags & IModelDelta.STATE) != 0) {
				handleState(node);
			}
			if ((flags & IModelDelta.INSERTED) != 0) {
				handleInsert(node);
			}
			if ((flags & IModelDelta.REPLACED) != 0) {
				handleReplace(node);
			}
			if ((flags & IModelDelta.INSTALL) != 0) {
				handleInstall(node);
			}
			if ((flags & IModelDelta.UNINSTALL) != 0) {
				handleUninstall(node);
			}
            if (!fSuppressModelControlRequests || override) {
                if ((flags & IModelDelta.EXPAND) != 0) {
                    handleExpand(node);
                }
                if ((flags & IModelDelta.COLLAPSE) != 0) {
                    handleCollapse(node);
                }
                if ((flags & IModelDelta.SELECT) != 0) {
                    handleSelect(node);
                }
                if ((flags & IModelDelta.REVEAL) != 0) {
                    handleReveal(node);
                }
            }
			updateNodes(node.getChildDeltas(), override);
		}
	}

	protected abstract void handleState(IModelDelta delta);

	protected abstract void handleSelect(IModelDelta delta);

	protected abstract void handleExpand(IModelDelta delta);
	
	protected abstract void handleCollapse(IModelDelta delta);

	protected abstract void handleContent(IModelDelta delta);

	protected abstract void handleRemove(IModelDelta delta);

	protected abstract void handleAdd(IModelDelta delta);

	protected abstract void handleInsert(IModelDelta delta);

	protected abstract void handleReplace(IModelDelta delta);
	
	protected abstract void handleReveal(IModelDelta delta);
	
	protected void handleInstall(IModelDelta delta) {
		installModelProxy(delta.getElement());
	}
	
	protected void handleUninstall(IModelDelta delta) {
		disposeModelProxy(delta.getElement());
	}	

	/**
	 * Returns a tree path for the node, *not* including the root element.
	 * 
	 * @param node
	 *            model delta
	 * @return corresponding tree path
	 */
	protected TreePath getViewerTreePath(IModelDelta node) {
		ArrayList list = new ArrayList();
		IModelDelta parentDelta = node.getParentDelta();
		while (parentDelta != null) {
			list.add(0, node.getElement());
			node = parentDelta;
			parentDelta = node.getParentDelta();
		}
		return new TreePath(list.toArray());
	}

	/**
	 * Returns the viewer this content provider is working for.
	 * 
	 * @return viewer
	 */
	protected ITreeModelContentProviderTarget getViewer() {
		return fViewer;
	}
	
	/**
	 * Translates and returns the given child index from the viewer coordinate
	 * space to the model coordinate space.
	 *  
	 * @param parentPath path to parent element
	 * @param index index of child element in viewer (filtered) space
	 * @return index of child element in model (raw) space
	 */
	public /* protected */ int viewToModelIndex(TreePath parentPath, int index) {
		return fTransform.viewToModelIndex(parentPath, index);
	}
	
	/**
	 * Translates and returns the given child count from the viewer coordinate
	 * space to the model coordinate space.
	 *  
	 * @param parentPath path to parent element
	 * @param count number of child elements in viewer (filtered) space
	 * @return number of child elements in model (raw) space
	 */
	public /* protected */ int viewToModelCount(TreePath parentPath, int count) {
		return fTransform.viewToModelCount(parentPath, count);
	}	
	
	/**
	 * Translates and returns the given child index from the model coordinate
	 * space to the viewer coordinate space.
	 *  
	 * @param parentPath path to parent element
	 * @param index index of child element in model (raw) space
	 * @return index of child element in viewer (filtered) space or -1 if filtered
	 */
	public int modelToViewIndex(TreePath parentPath, int index) {
		return fTransform.modelToViewIndex(parentPath, index);
	}	
	
	/**
	 * Translates and returns the given child count from the model coordinate
	 * space to the viewer coordinate space.
	 *  
	 * @param parentPath path to parent element
	 * @param count child count element in model (raw) space
	 * @return child count in viewer (filtered) space
	 */
	public int modelToViewChildCount(TreePath parentPath, int count) {
		return fTransform.modelToViewCount(parentPath, count);
	}	
	
	/**
	 * Notes that the child at the specified index of the given parent element
	 * has been filtered from the viewer. Returns whether the child at the given
	 * index was already filtered.
	 * 
	 * @param parentPath path to parent element
	 * @param index index of child element to be filtered
	 * @param element the filtered element
	 * @return whether the child was already filtered
	 */
	protected boolean addFilteredIndex(TreePath parentPath, int index, Object element) {
		return fTransform.addFilteredIndex(parentPath, index, element);
	}
	
	/**
	 * Notes that the element at the given index has been removed from its parent
	 * and filtered indexes should be updated accordingly.
	 * 
	 * @param parentPath path to parent element
	 * @param index index of element that was removed
	 */
	protected void removeElementFromFilters(TreePath parentPath, int index) {
		fTransform.removeElementFromFilters(parentPath, index);
	}
	
	/**
	 * Removes the given element from filtered elements of the given parent
	 * element. Return true if the element was removed, otherwise false.
	 * 
	 * @param parentPath path to parent element
	 * @param element element to remove
	 * @return whether the element was removed
	 */
	protected boolean removeElementFromFilters(TreePath parentPath, Object element) {
		return fTransform.removeElementFromFilters(parentPath, element);
	}	
	
	/**
	 * The child count for a parent has been computed. Ensure any filtered items
	 * above the given count are cleared.
	 * 
	 * @param parentPath path to parent element
	 * @param childCount number of children
	 */
	protected void setModelChildCount(TreePath parentPath, int childCount) {
		fTransform.setModelChildCount(parentPath, childCount);
	}
	
	/**
 	 * Returns whether the given element is filtered.
	 * 
	 * @param parentElementOrTreePath
	 *            the parent element or path
	 * @param element
	 *            the child element
	 * @return whether to filter the element
	 */
	public boolean shouldFilter(Object parentElementOrTreePath, Object element) {
		ViewerFilter[] filters = fViewer.getFilters();
		if (filters.length > 0) {
			for (int j = 0; j < filters.length; j++) {
				if (!(filters[j].select((Viewer)fViewer, parentElementOrTreePath, element))) {
					return true;
				}
			}
		}
		return false;
	}	
	
	/**
	 * Returns whether the given index of the specified parent was previously filtered.
	 * 
	 * @param parentPath
	 * @param index
	 * @return whether the element at the given index was filtered
	 */
	protected boolean isFiltered(TreePath parentPath, int index) {
		return fTransform.isFiltered(parentPath, index);
	}
	
	/**
	 * Notification the given element is being unmapped.
	 * 
	 * @param path
	 */
	public void unmapPath(TreePath path) {
		//System.out.println("Unmap " + path.getLastSegment());
		fTransform.clear(path);
		cancelSubtreeUpdates(path);
	}

	/**
	 * Returns filtered children or <code>null</code>
	 * @param parent
	 * @return filtered children or <code>null</code>
	 */
	protected int[] getFilteredChildren(TreePath parent) {
		return fTransform.getFilteredChildren(parent);
	}
	
	protected void clearFilteredChild(TreePath parent, int modelIndex) {
		fTransform.clear(parent, modelIndex);
	}
	
	protected void clearFilters(TreePath parent) {
		fTransform.clear(parent);
	}

	protected synchronized void checkIfRestoreComplete() {
		if (fPendingState == null) {
			return;
		}
		CheckState state = new CheckState();
		fPendingState.accept(state);
		if (state.isComplete()) {
            if (DEBUG_STATE_SAVE_RESTORE && (DEBUG_PRESENTATION_ID == null || DEBUG_PRESENTATION_ID.equals(getPresentationContext().getId()))) {
                System.out.println("RESTORE COMPELTE: " + fPendingState); //$NON-NLS-1$
            }
			fPendingState = null;
		}
	}
	
	public void addViewerUpdateListener(IViewerUpdateListener listener) {
		fUpdateListeners.add(listener);
	}
	
	public void removeViewerUpdateListener(IViewerUpdateListener listener) {
		fUpdateListeners.remove(listener);
	}
	
	/**
	 * Notification an update request has started
	 * 
	 * @param update
	 */
	void updateStarted(ViewerUpdateMonitor update) {
		boolean begin = false;
		synchronized (fRequestsInProgress) {
			begin = fRequestsInProgress.isEmpty();
			List requests = (List) fRequestsInProgress.get(update.getSchedulingPath());
			if (requests == null) {
				requests = new ArrayList();
				fRequestsInProgress.put(update.getSchedulingPath(), requests);
			}
			requests.add(update);
		}
		if (begin) {
			if (DEBUG_UPDATE_SEQUENCE && (DEBUG_PRESENTATION_ID == null || DEBUG_PRESENTATION_ID.equals(getPresentationContext().getId()))) {
				System.out.println("MODEL SEQUENCE BEGINS"); //$NON-NLS-1$
			}
			notifyUpdate(UPDATE_SEQUENCE_BEGINS, null);
		}
		if (DEBUG_UPDATE_SEQUENCE && (DEBUG_PRESENTATION_ID == null || DEBUG_PRESENTATION_ID.equals(getPresentationContext().getId()))) {
			System.out.println("\tBEGIN - " + update); //$NON-NLS-1$
		}
		notifyUpdate(UPDATE_BEGINS, update);
	}
	
	/**
	 * Notification an update request has completed
	 * 
	 * @param update
	 */
	void updateComplete(ViewerUpdateMonitor update) {
		boolean end = false;
		synchronized (fRequestsInProgress) {
			List requests = (List) fRequestsInProgress.get(update.getSchedulingPath());
			if (requests != null) {
				requests.remove(update);
				trigger(update);
				if (requests.isEmpty()) {
					fRequestsInProgress.remove(update.getSchedulingPath());
				}
			}
			end = fRequestsInProgress.isEmpty();
		}
		notifyUpdate(UPDATE_COMPLETE, update);
		if (DEBUG_UPDATE_SEQUENCE && (DEBUG_PRESENTATION_ID == null || DEBUG_PRESENTATION_ID.equals(getPresentationContext().getId()))) {
			System.out.println("\tEND - " + update); //$NON-NLS-1$
		}
		if (end) {
			if (DEBUG_UPDATE_SEQUENCE && (DEBUG_PRESENTATION_ID == null || DEBUG_PRESENTATION_ID.equals(getPresentationContext().getId()))) {
				System.out.println("MODEL SEQUENCE ENDS"); //$NON-NLS-1$
			}
			notifyUpdate(UPDATE_SEQUENCE_COMPLETE, null);
		}
	}
	
	protected void notifyUpdate(final int type, final IViewerUpdate update) {
		if (!fUpdateListeners.isEmpty()) {
			Object[] listeners = fUpdateListeners.getListeners();
			for (int i = 0; i < listeners.length; i++) {
				final IViewerUpdateListener listener = (IViewerUpdateListener) listeners[i];
				SafeRunner.run(new ISafeRunnable() {
					public void run() throws Exception {
						switch (type) {
							case UPDATE_SEQUENCE_BEGINS:
								listener.viewerUpdatesBegin();
								break;
							case UPDATE_SEQUENCE_COMPLETE:
								listener.viewerUpdatesComplete();
								break;
							case UPDATE_BEGINS:
								listener.updateStarted(update);
								break;
							case UPDATE_COMPLETE:
								listener.updateComplete(update);
								break;
						}
					}
					public void handleException(Throwable exception) {
						DebugUIPlugin.log(exception);
					}
				});
			}
		}
	}	
	
	protected void cancelSubtreeUpdates(TreePath path) {
		synchronized (fRequestsInProgress) {
			Iterator iterator = fRequestsInProgress.entrySet().iterator();
			while (iterator.hasNext()) {
				Entry entry = (Entry) iterator.next();
				TreePath entryPath = (TreePath) entry.getKey();
				if (entryPath.startsWith(path, null)) {
					List requests = (List) entry.getValue();
					Iterator reqIter = requests.iterator();
					while (reqIter.hasNext()) {
						((IRequest)reqIter.next()).cancel();
					}
				}
			}
			List purge = new ArrayList(); 
			iterator = fWaitingRequests.keySet().iterator();
			while (iterator.hasNext()) {
				TreePath entryPath = (TreePath) iterator.next();
				if (entryPath.startsWith(path, null)) {
					purge.add(entryPath);
				}
			}
			iterator = purge.iterator();
			while (iterator.hasNext()) {
				fWaitingRequests.remove(iterator.next());
			}
		}
		for (Iterator itr = fCompareRequestsInProgress.keySet().iterator(); itr.hasNext(); ) {
			CompareRequestKey key = (CompareRequestKey)itr.next();
			if (key.fPath.startsWith(path, null)) {
				ElementCompareRequest compareRequest = (ElementCompareRequest)fCompareRequestsInProgress.get(key);
				compareRequest.cancel();
				itr.remove();
			}
		}
	}
	
	/**
	 * Returns whether this given request should be run, or should wait for parent
	 * update to complete.
	 * 
	 * @param update
	 * @return whether to start the given request
	 */
	void schedule(ViewerUpdateMonitor update) {
		synchronized (fRequestsInProgress) {
			TreePath schedulingPath = update.getSchedulingPath();
			List requests = (List) fWaitingRequests.get(schedulingPath);
			if (requests == null) {
				// no waiting requests
				TreePath parentPath = schedulingPath;
				while (fRequestsInProgress.get(parentPath) == null) {
					parentPath = parentPath.getParentPath();
					if (parentPath == null) {
						// no running requests: start request
						update.start();
						return;
					}
				}
				// request running on parent, add to waiting list
				requests = new ArrayList();
				requests.add(update);
				fWaitingRequests.put(schedulingPath, requests);
			} else {
				// there are waiting requests: coalesce with existing request?
				Iterator reqIter = requests.iterator();
				while (reqIter.hasNext()) {
					ViewerUpdateMonitor waiting = (ViewerUpdateMonitor) reqIter.next();
					if (waiting.coalesce(update)) {
						// coalesced with existing request, done
						return;
					}
				}
				// add to list of waiting requests
				requests.add(update);
				return;
			}
		}
	}
	
	/**
	 * Triggers waiting requests based on the given request that just completed.
	 * 
	 * TODO: should we cancel child updates if a request has been canceled?
	 * 
	 * @param request
	 */
	void trigger(ViewerUpdateMonitor request) {
		if (fWaitingRequests.isEmpty()) {
			return;
		}
		TreePath schedulingPath = request.getSchedulingPath();
		List waiting = (List) fWaitingRequests.get(schedulingPath);
		if (waiting == null) {
			// no waiting, update the entry with the shortest path
			int length = Integer.MAX_VALUE;
			Iterator entries = fWaitingRequests.entrySet().iterator();
			Entry candidate = null;
			while (entries.hasNext()) {
				Entry entry = (Entry) entries.next();
				TreePath key = (TreePath) entry.getKey();
				if (key.getSegmentCount() < length) {
					candidate = entry;
					length = key.getSegmentCount();
				}
			}
			if (candidate != null) {
				startHighestPriorityRequest((TreePath) candidate.getKey(), (List) candidate.getValue());
			}
		} else {
			// start the highest priority request
			startHighestPriorityRequest(schedulingPath, waiting);
		}
	}

	/**
	 * @param key
	 * @param waiting
	 */
	private void startHighestPriorityRequest(TreePath key, List waiting) {
		int priority = 4;
		ViewerUpdateMonitor next = null;
		Iterator requests = waiting.iterator();
		while (requests.hasNext()) {
			ViewerUpdateMonitor vu = (ViewerUpdateMonitor) requests.next();
			if (vu.getPriority() < priority) {
				next = vu;
				priority = next.getPriority();
			}
		}
		waiting.remove(next);
		if (waiting.isEmpty()) {
			fWaitingRequests.remove(key);
		}
		next.start();
	}
	
	/**
	 * Registers the given listener for model delta notification.
	 * 
	 * @param listener model delta listener
	 */
	public void addModelChangedListener(IModelChangedListener listener) {
		fModelListeners.add(listener); 
	}
	
	/**
	 * Unregisters the given listener from model delta notification.
	 * 
	 * @param listener model delta listener
	 */
	public void removeModelChangedListener(IModelChangedListener listener) {
		fModelListeners.remove(listener);
	}
	
	/**
	 * Returns the element corresponding to the given tree path.
	 * 
	 * @param path tree path
	 * @return model element
	 */
	protected Object getElement(TreePath path) {
		if (path.getSegmentCount() > 0) {
			return path.getLastSegment();
		}
		return getViewer().getInput();
	}
	
	/**
	 * Reschedule any children updates in progress for the given parent
	 * that have a start index greater than the given index. An element
	 * has been removed at this index, invalidating updates in progress.
	 * 
	 * @param parentPath view tree path to parent element
	 * @param modelIndex index at which an element was removed
	 */
	protected void rescheduleUpdates(TreePath parentPath, int modelIndex) {
		synchronized (fRequestsInProgress) {
			List requests = (List)fRequestsInProgress.get(parentPath);
			List reCreate = null;
			if (requests != null) {
				Iterator iterator = requests.iterator();
				while (iterator.hasNext()) {
					IViewerUpdate update = (IViewerUpdate) iterator.next();
					if (update instanceof IChildrenUpdate) {
						IChildrenUpdate childrenUpdate = (IChildrenUpdate) update;
						if (childrenUpdate.getOffset() > modelIndex) {
							childrenUpdate.cancel();
							if (reCreate == null) {
								reCreate  = new ArrayList();
							}
							reCreate.add(childrenUpdate);
							if (DEBUG_CONTENT_PROVIDER && getPresentationContext().getId().equals(DEBUG_PRESENTATION_ID)) {
								System.out.println("canceled update in progress handling REMOVE: " + childrenUpdate); //$NON-NLS-1$
							}
						}
					}
				}
			}
			requests = (List)fWaitingRequests.get(parentPath);
			if (requests != null) {
				Iterator iterator = requests.iterator();
				while (iterator.hasNext()) {
					IViewerUpdate update = (IViewerUpdate) iterator.next();
					if (update instanceof IChildrenUpdate) {
						IChildrenUpdate childrenUpdate = (IChildrenUpdate) update;
						if (childrenUpdate.getOffset() > modelIndex) {
							((ChildrenUpdate)childrenUpdate).setOffset(childrenUpdate.getOffset() - 1);
							if (DEBUG_CONTENT_PROVIDER && (DEBUG_PRESENTATION_ID == null || DEBUG_PRESENTATION_ID.equals(getPresentationContext().getId()))) {
								System.out.println("modified waiting update handling REMOVE: " + childrenUpdate); //$NON-NLS-1$
							}
						}
					}
				}
			}
			// re-schedule canceled updates at new position.
			// have to do this last else the requests would be waiting and
			// get modified.
			if (reCreate != null) {
				Iterator iterator = reCreate.iterator();
				while (iterator.hasNext()) {
					IChildrenUpdate childrenUpdate = (IChildrenUpdate) iterator.next();
					int start = childrenUpdate.getOffset() - 1;
					int end = start + childrenUpdate.getLength();
					for (int i = start; i < end; i++) {
						((TreeModelContentProvider)this).doUpdateElement(parentPath, i);
					}
				}
			}
		}
	}
}
