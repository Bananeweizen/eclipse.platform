/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.compare.structuremergeviewer;

import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.widgets.*;

import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.compare.*;
import org.eclipse.compare.internal.*;


/**
 * A diff tree viewer that can be configured with a <code>IStructureCreator</code>
 * to retrieve a hierarchical structure from the input object (an <code>ICompareInput</code>)
 * and perform a two-way or three-way compare on it.
 * <p>
 * This <code>DiffTreeViewer</code> supports the so called "smart" mode of the structure creator
 * by installing a button in the viewer's pane title bar.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed outside
 * this package.
 * </p>
 *
 * @see IStructureCreator
 * @see ICompareInput
 */
public class StructureDiffViewer extends DiffTreeViewer {
		
	private Differencer fDifferencer;
	private boolean fThreeWay= false;
	
	private ITypedElement fAncestorInput;
	private ITypedElement fLeftInput;
	private ITypedElement fRightInput;
	
	private IStructureComparator fAncestorStructure;
	private IStructureComparator fLeftStructure;
	private IStructureComparator fRightStructure;
	
	private IStructureCreator fStructureCreator;
	private IDiffContainer fRoot;
	private IContentChangeListener fContentChangedListener;
	private ICompareInputChangeListener fThreeWayInputChangedListener;
	private CompareViewerSwitchingPane fParent;
		
	/**
	 * Creates a new viewer for the given SWT tree control with the specified configuration.
	 *
	 * @param tree the tree control
	 * @param configuration the configuration for this viewer
	 */
	public StructureDiffViewer(Tree tree, CompareConfiguration configuration) {
		super(tree, configuration);
		Composite c= tree.getParent();
		if (c instanceof CompareViewerSwitchingPane)
			fParent= (CompareViewerSwitchingPane) c;
		initialize();
	}
	
	/**
	 * Creates a new viewer under the given SWT parent with the specified configuration.
	 *
	 * @param parent the SWT control under which to create the viewer
	 * @param configuration the configuration for this viewer
	 */
	public StructureDiffViewer(Composite parent, CompareConfiguration configuration) {
		super(parent, configuration);
		if (parent instanceof CompareViewerSwitchingPane)
			fParent= (CompareViewerSwitchingPane) parent;
		initialize();
	}
	
	private void initialize() {
		
		setAutoExpandLevel(3);
		
		fContentChangedListener= new IContentChangeListener() {
			public void contentChanged(IContentChangeNotifier changed) {
				StructureDiffViewer.this.contentChanged(changed);
			}
		};
		fThreeWayInputChangedListener= new ICompareInputChangeListener() {
			public void compareInputChanged(ICompareInput input) {
				StructureDiffViewer.this.compareInputChanged(input);
			}
		};
	}
	
	/**
	 * Configures the <code>StructureDiffViewer</code> with a structure creator.
	 * The structure creator is used to create a hierarchical structure
	 * for each side of the viewer's input element of type <code>ICompareInput</code>.
	 * <p>
	 * If the structure creator's <code>canRewriteTree</code> returns <code>true</code>
	 * the "smart" button in the viewer's pane control bar is enabled.
	 *
	 * @param structureCreator the new structure creator
	 */
	public void setStructureCreator(IStructureCreator structureCreator) {
		if (fStructureCreator != structureCreator) {
			fStructureCreator= structureCreator;
			Control tree= getControl();
			if (tree != null && !tree.isDisposed())
				tree.setData(CompareUI.COMPARE_VIEWER_TITLE, getTitle());
		}
	}
	
	/**
	 * Returns the structure creator or <code>null</code> if no
	 * structure creator has been set with <code>setStructureCreator</code>.
	 *
	 * @return the structure creator or <code>null</code>
	 */
	public IStructureCreator getStructureCreator() {
		return fStructureCreator;
	}
	
	/**
	 * Reimplemented to get the descriptive title for this viewer from the <code>IStructureCreator</code>.
	 */
	public String getTitle() {
		if (fStructureCreator != null)
			return fStructureCreator.getName();
		return super.getTitle();
	}
	
	/**
	 * Overridden because the input of this viewer is not identical to the root of the tree.
	 * The tree's root is a IDiffContainer that was returned from the method <code>diff</code>.
	 * 
	 * @return the root of the diff tree produced by method <code>diff</code>
	 */
	protected Object getRoot() {
		return fRoot;
	}
	
	/**
	 * Overridden to create the comparable structures from the input object
	 * and to feed them through the differencing engine. Note: for this viewer
	 * the value from <code>getInput</code> is not identical to <code>getRoot</code>.
	 */
	protected void inputChanged(Object input, Object oldInput) {
		if (input instanceof ICompareInput) {
			compareInputChanged((ICompareInput) input);
			if (input != oldInput)
				navigate(true);
		}
	}
	
	/* (non Javadoc)
	 * Overridden to unregister all listeners.
	 */
	protected void handleDispose(DisposeEvent event) {
		
		compareInputChanged(null);
		
		fContentChangedListener= null;
		fThreeWayInputChangedListener= null;
				
		super.handleDispose(event);
	}
	
	/**
	 * Recreates the comparable structures for the input sides.
	 */
	protected void compareInputChanged(ICompareInput input) {
		ITypedElement t= null;
		boolean changed= false;
		
		if (input != null)
			t= input.getAncestor();
			
		fThreeWay= (t != null);
		
		if (t != fAncestorInput) {
			if (fAncestorInput instanceof IContentChangeNotifier)
				((IContentChangeNotifier)fAncestorInput).removeContentChangeListener(fContentChangedListener);
			fAncestorInput= t;
			if (fAncestorInput != null) {
				fAncestorStructure= fStructureCreator.getStructure(fAncestorInput);
				changed= true;
			} else
				fAncestorStructure= null;
			if (fAncestorInput instanceof IContentChangeNotifier)
				((IContentChangeNotifier)fAncestorInput).addContentChangeListener(fContentChangedListener);
		}
		
		if (input != null)
			t= input.getLeft();
		if (t != fLeftInput) {
			if (fLeftInput instanceof IContentChangeNotifier)
				((IContentChangeNotifier)fLeftInput).removeContentChangeListener(fContentChangedListener);
			fLeftInput= t;
			if (fLeftInput != null) {
				fLeftStructure= fStructureCreator.getStructure(fLeftInput);
				changed= true;
			} else
				fLeftStructure= null;
			if (fLeftInput instanceof IContentChangeNotifier)
				((IContentChangeNotifier)fLeftInput).addContentChangeListener(fContentChangedListener);
		}
		
		if (input != null)
			t= input.getRight();
		if (t != fRightInput) {
			if (fRightInput instanceof IContentChangeNotifier)
				((IContentChangeNotifier)fRightInput).removeContentChangeListener(fContentChangedListener);
			fRightInput= t;
			if (fRightInput != null) {
				fRightStructure= fStructureCreator.getStructure(fRightInput);
				changed= true;
			} else
				fRightStructure= null;
			if (fRightInput instanceof IContentChangeNotifier)
				((IContentChangeNotifier)fRightInput).addContentChangeListener(fContentChangedListener);
		}
		
		if (changed)
			diff();
	}
	
	/**
	 * Calls <code>diff</code> whenever the byte contents changes.
	 */
	protected void contentChanged(IContentChangeNotifier changed) {
		
		if (fStructureCreator == null)
			return;
			
		if (changed != null) {
			if (changed == fAncestorInput) {
				fAncestorStructure= fStructureCreator.getStructure(fAncestorInput);
			} else if (changed == fLeftInput) {
				fLeftStructure= fStructureCreator.getStructure(fLeftInput);
			} else if (changed == fRightInput) {
				fRightStructure= fStructureCreator.getStructure(fRightInput);
			} else
				return;
		} else {
			fAncestorStructure= fStructureCreator.getStructure(fAncestorInput);
			fLeftStructure= fStructureCreator.getStructure(fLeftInput);
			fRightStructure= fStructureCreator.getStructure(fRightInput);
		}
		
		diff();
	}

	protected void preDiffHook(IStructureComparator ancestor, IStructureComparator left, IStructureComparator right) {
		// we do nothing here
	}
	
	/**
	 * Runs the difference engine and refreshes the tree.
	 */
	protected void diff() {
		
		preDiffHook(fAncestorStructure, fLeftStructure, fRightStructure);
							
		String message= null;
		
		if ((fThreeWay && fAncestorStructure == null) || fLeftStructure == null || fRightStructure == null) {
			// could not get structure of one (or more) of the legs
			fRoot= null;
			message= CompareMessages.getString("StructureDiffViewer.StructureError");	//$NON-NLS-1$
			
		} else {	// calculate difference of the two (or three) structures

			if (fDifferencer == null)
				fDifferencer= new Differencer() {
					protected boolean contentsEqual(Object o1, Object o2) {
						return StructureDiffViewer.this.contentsEqual(o1, o2);
					}
					protected Object visit(Object data, int result, Object ancestor, Object left, Object right) {
						Object o= super.visit(data, result, ancestor, left, right);
						if (fLeftIsLocal && o instanceof DiffNode)
							((DiffNode)o).swapSides(fLeftIsLocal);
						return o;
					}
				};
			
			fRoot= (IDiffContainer) fDifferencer.findDifferences(fThreeWay, null, null,
					fAncestorStructure, fLeftStructure, fRightStructure);
					
			if (fRoot == null || fRoot.getChildren().length == 0) {
				message= CompareMessages.getString("StructureDiffViewer.NoStructuralDifferences");	//$NON-NLS-1$
			} else {
				postDiffHook(fDifferencer, fRoot);
			}
		}
		if (fParent != null)
			fParent.setTitleArgument(message);
			
		refresh(getRoot());
	}
	
	protected void postDiffHook(Differencer differencer, IDiffContainer root) {
		// we do nothing here
	}
	
	/**
	 * Performs a byte compare on the given objects.
	 * Called from the difference engine.
	 * Returns <code>null</code> if no structure creator has been set.
	 */
	private boolean contentsEqual(Object o1, Object o2) {
		if (fStructureCreator != null) {
			boolean ignoreWhiteSpace= Utilities.getBoolean(getCompareConfiguration(), CompareConfiguration.IGNORE_WHITESPACE, false);		
			String s1= fStructureCreator.getContents(o1, ignoreWhiteSpace);
			String s2= fStructureCreator.getContents(o2, ignoreWhiteSpace);
			return s1.equals(s2);
		}
		return false;
	}
	
	/**
	 * Tracks property changes of the configuration object.
	 * Clients may override to track their own property changes.
	 * In this case they must call the inherited method.
	 */
	protected void propertyChange(PropertyChangeEvent event) {
		String key= event.getProperty();
		if (key.equals(CompareConfiguration.IGNORE_WHITESPACE))
			diff();
		else
			super.propertyChange(event);
	}
		
	/**
	 * Overridden to call the <code>save</code> method on the structure creator after
	 * nodes have been copied from one side to the other side of an input object.
	 *
	 * @param leftToRight if <code>true</code> the left side is copied to the right side.
	 * If <code>false</code> the right side is copied to the left side
	 */
	protected void copySelected(boolean leftToRight) {
		super.copySelected(leftToRight);
		
		if (fStructureCreator != null)
			fStructureCreator.save(
							leftToRight ? fRightStructure : fLeftStructure,
							leftToRight ? fRightInput : fLeftInput);
	}
}

