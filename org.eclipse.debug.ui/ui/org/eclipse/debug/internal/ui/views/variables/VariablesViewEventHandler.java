package org.eclipse.debug.internal.ui.views.variables;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.model.ISuspendResume;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.internal.ui.views.AbstractDebugEventHandler;
import org.eclipse.debug.ui.AbstractDebugView;

/**
 * Updates the variables view
 */
public class VariablesViewEventHandler extends AbstractDebugEventHandler {
	
	/**
	 * Constructs a new event handler on the given view
	 * 
	 * @param view variables view
	 */
	public VariablesViewEventHandler(AbstractDebugView view) {
		super(view);
	}
	
	/**
	 * @see AbstractDebugEventHandler#handleDebugEvents(DebugEvent[])
	 */
	protected void doHandleDebugEvents(DebugEvent[] events) {	
		for (int i = 0; i < events.length; i++) {	
			DebugEvent event = events[i];
			switch (event.getKind()) {
				case DebugEvent.SUSPEND:
					if (event.getDetail() != DebugEvent.EVALUATION_IMPLICIT) {
						if (event.getSource() instanceof ISuspendResume) {
							if (!((ISuspendResume)event.getSource()).isSuspended()) {
								// no longer suspended
								return;
							}
						}						
						// Don't refresh everytime an implicit evaluation finishes
						refresh();
						if (event.getDetail() == DebugEvent.STEP_END) {
							getVariablesView().populateDetailPane();
						}
						// return since we've done a complete refresh
						return;
					}
					break;
				case DebugEvent.CHANGE:
					if (event.getDetail() == DebugEvent.STATE) {
						// only process variable state changes
						if (event.getSource() instanceof IVariable) {
							refresh(event.getSource());
						}
					} else {
						refresh();
						// return since we've done a complete refresh
						return;
					}
					break;
				case DebugEvent.RESUME:
					doHandleResumeEvent(event);
					break;
				case DebugEvent.TERMINATE:
					doHandleTerminateEvent(event);
					break;
			}
		}
	}

	/**
	 * Clear the variables immediately upon resume.
	 */
	protected void doHandleResumeEvent(DebugEvent event) {
		if (!event.isStepStart() && !event.isEvaluation()) {
			// Clear existing variables from the view
			getViewer().setInput(null);
			// clear the cache of expanded variables for the resumed thread/target
			getVariablesView().clearExpandedVariables(event.getSource());
		}
	}

	/**
	 * Clear any cached variable expansion state for the
	 * terminated thread/target.
	 */
	protected void doHandleTerminateEvent(DebugEvent event) {
		getVariablesView().clearExpandedVariables(event.getSource());
	}


	protected VariablesView getVariablesView() {
		return (VariablesView)getView();
	}
}

