package org.eclipse.debug.core;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.EventObject;

import org.eclipse.debug.internal.core.DebugCoreMessages;

/**
 * A debug event describes an event in a program being debugged or
 * in a running process. Debug models and process implementations
 * are required to generate debug events as specified by this class.
 * <p>
 * The following list defines the events generated for each debug
 * model element.
 * The <code>getSource()</code> method of a debug event
 * returns the element associated with the event.
 * Creation events are guaranteed to occur in a top
 * down order - that is, parents are created before children.
 * Termination events are guaranteed to occur in a bottom up order -
 * that is, children before parents. However, termination events are not guaranteed
 * for all  elements that are created. That is, terminate events can be coalesced - a 
 * terminate event for a parent signals that all children have been terminated.
 * </p>
 * <p>
 * The generic <code>CHANGE</code> event can be fired at any time by any element.
 * Generally, a client of a debug model, such as as a UI, can get sufficient
 * information to update by listening/responding to the other event kinds. However,
 * if a debug model needs to inform clients of a change that is not specified
 * by create/terminate/suspend/resume, the <code>CHANGE</code> event may be used.
 * For example, generally, the only way a thread or any of its children can change
 * state between a suspend and resume operation, is if the thread or owning debug
 * target is termianted. However, if a debug model supports some other operation
 * that would allow a debug element to change state while suspended, the debug model
 * would fire a change event for that element. The valid detail codes for a
 * change event are:<ul>
 * <li><code>STATE</code> - indicates the state of an element has changed, but its
 *  children are not effected. A client would use a state change event to update
 *  a label of the effected element, but would not update any children.</li>
 * <li><code>CONTENT</code> - indicates that a debug element's value or contents have
 *  changed in some way. For example, when the value of a variable is changed
 *  explicitly, the variable should fire a content change event.</li>
 * </ul>
 * </p>
 * <ul>
 * <li><code>IProcess</code>
 *	<ul>
 *	<li><code>CREATE</code> - a process has been created and is executing.</li>
 *	<li><code>TERMINATE</code> - a process has terminated.</li>
 *  </ul>
 * <li><code>IDebugTarget</code>
 *	<ul>
 *	<li><code>CREATE</code> - a debug target has been created and is ready
 *		to begin a debug session.</li>
 *	<li><code>TERMINATE</code> - a debug target has terminated and the debug
 *		session has ended.</li>
 *  <li><code>SUSPEND</code> - a debug target has suspended. Event detail provides
 *		the reason for the suspension:<ul>
 *		<li><code>STEP_END</code> - a request to step has completed</li>
 *		<li><code>BREAKPOINT</code> - a breakpoint has been hit</li>
 *		<li><code>CLIENT_REQUEST</code> - a client request has caused the target to suspend
 * 			(i.e. an explicit call to <code>suspend()</code>)</li>
 * 		<li><code>UNSPECIFIED</code> - the reason for the suspend is not specified</li>
 *		</ul>
 *	</li>
 *  <li><code>RESUME</code> - a debug target has resumed. Event detail provides
 *		the reason for the resume:<ul>
 *		<li><code>STEP_INTO</code> - a target is being resumed because of a request to step into</li>
 * 		<li><code>STEP_OVER</code> - a target is being resumed because of a request to step over</li>
 * 		<li><code>STEP_RETURN</code> - a target is being resumed because of a request to step return</li>
 *		<li><code>CLIENT_REQUEST</code> - a client request has caused the target to be resumed
 * 			(i.e. an explicit call to <code>resume()</code>)</li>
 * 		<li><code>UNSPECIFIED</code> - The reason for the resume is not specified</li>
 *		</ul>
 *	</li>
 *	</ul>
 * </li>
 * <li><code>IThread</code>
 *	<ul>
 *	<li><code>CREATE</code> - a thread has been created in a debug target.</li>
 *	<li><code>TERMINATE</code> - a thread has terminated.</li>
 *	<li><code>SUSPEND</code> - a thread has suspended execution. Event detail provides
 *		the reason for the suspension:<ul>
 *		<li><code>STEP_END</code> - a request to step has completed</li>
 *		<li><code>BREAKPOINT</code> - a breakpoint has been hit</li>
 *		<li><code>CLIENT_REQUEST</code> - a client request has caused the thread to suspend
 * 			(i.e. an explicit call to <code>suspend()</code>)</li>
 * 		<li><code>EVALUATION</code> - an expression evaluation has ended that may
 * 			have had side effects in the debug target.</li>
 * 		<li><code>EVALUATION_READ_ONLY</code> - an expression evaluation has ended that
 * 			had no side effects in the debug target.</li>
 * 		<li><code>UNSPECIFIED</code> - the reason for the suspend is not specified</li>
 *		</ul>
 *	</li>
 *	<li><code>RESUME</code> - a thread has resumed execution. Event detail provides
 *		the reason for the resume:<ul>
 *		<li><code>STEP_INTO</code> - a thread is being resumed because of a request to step into</li>
 * 		<li><code>STEP_OVER</code> - a thread is being resumed because of a request to step over</li>
 * 		<li><code>STEP_RETURN</code> - a thread is being resumed because of a request to step return</li>
 *		<li><code>CLIENT_REQUEST</code> - a client request has caused the thread to be resumed
 * 			(i.e. an explicit call to <code>resume()</code>)</li>
 * 		<li><code>EVALUATION</code> - an expression evaluation has started that may
 * 			have side effects in the debug target.</li>
 * 		<li><code>EVALUATION_READ_ONLY</code> - an expression evaluation has started that
 * 			will have no side effects in the debug target.</li>
 * 		<li><code>UNSPECIFIED</code> - The reason for the resume is not specified</li>
 *		</ul>
 *	</li>
 *    </ul>
 * </li>
 * <li><code>IStackFrame</code> - no events are specified for stack frames.
 *	When a thread is suspended, it has stack frames. When a thread resumes,
 *	stack frames are unavailable.
 * </li>
 * <li><code>IVariable</code> - no events are specified for variables.
 *	When a thread is suspended, stack frames have variables. When a thread resumes,
 *	variables are unavailable.
 * </li>
 * <li><code>IValue</code> - no events are specified for values.
 * </li>
 * <li><code>IProcess</code>
 *	<ul>
 *	<li><code>CREATE</code> - a system process has been created.</li>
 *	<li><code>TERMINATE</code> - a system process has terminated.</li>
 *	</ul>
 * </li>
 * </ul>
 * <p>
 * Clients may instantiate this class. Clients are not intended to subclass this class.
 * </p>
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 */
public final class DebugEvent extends EventObject {
	
	/**
	 * Resume event.
	 */
	public static final int RESUME= 0x0001;

	/**
	 * Suspend event.
	 */
	public static final int SUSPEND= 0x0002;

	/**
	 * Create event.
	 */
	public static final int CREATE= 0x0004;

	/**
	 * Terminate event.
	 */
	public static final int TERMINATE= 0x0008;
	
	/**
	 * Change event.
	 */
	public static final int CHANGE= 0x0010;

	/**
	 * Step start detail. Indicates a thread was resumed by a step
	 * into action.
	 * @since 2.0
	 */
	public static final int STEP_INTO= 0x0001;
	
	/**
	 * Step start detail. Indicates a thread was resumed by a step
	 * over action.
	 * @since 2.0
	 */
	public static final int STEP_OVER= 0x0002;
	
	/**
	 * Step start detail. Indicates a thread was resumed by a step
	 * return action.
	 * @since 2.0
	 */
	public static final int STEP_RETURN= 0x0004;		

	/**
	 * Step end detail. Indicates a thread was suspended due
	 * to the completion of a step action.
	 */
	public static final int STEP_END= 0x0008;
	
	/**
	 * Breakpoint detail. Indicates a thread was suspended by
	 * a breakpoint.
	 */
	public static final int BREAKPOINT= 0x0010;
	
	/**
	 * Client request detail. Indicates a thread was suspended due
	 * to a client request.
	 */
	public static final int CLIENT_REQUEST= 0x0020;
	
	/**
	 * Evaluation detail. Indicates that a thread was resumed or
	 * suspended to perform an expression evaluation.
	 * 
	 * @since 2.0
	 */
	public static final int EVALUATION = 0x0040;
	
	/**
	 * Evaluation detail. Indicates that a thread was resumed or
	 * suspended to perform an expression evaluation that has no
	 * update side effects. Clients may use this detail event
	 * for efficiency when it is known that an evaluation had 
	 * no side effects.
	 * 
	 * @since 2.0
	 */
	public static final int EVALUATION_READ_ONLY = 0x0080;

	/**
	 * State change detail. Indicates the state of a single 
	 * debug element has changed. Only valid for <code>CHANGE</code>
	 * events.
	 * 
	 * @since 2.0
	 */
	public static final int STATE = 0x0100;
	
	/**
	 * Content change detail. Indicates the content of a debug element
	 * (and potentially its children) has changed. Only valid for
	 * <code>CHANGE</code> events.
	 * 
	 * @since 2.0
	 */
	public static final int CONTENT = 0x0200;	
			
	/**
	 * Constant indicating that the kind or detail of a debug
	 * event is unspecified.
	 */
	public static final int UNSPECIFIED = 0;
	
	/**
	 * The kind of event - one of the kind constants defined by
	 * this class.
	 */
	private int fKind= UNSPECIFIED;

	/**
	 * The detail of the event - one of the detail constants defined by
	 * this class.
	 */
	private int fDetail= UNSPECIFIED;
	/**
	 * Constructs a new debug event of the given kind with a detail code of
	 * <code>UNSPECIFIED</code>.
	 *
	 * @param eventSource the object associated with the event
	 * @param kind the kind of debug event (one of the
	 *	kind constants defined by this class)
	 */
	public DebugEvent(Object eventSource, int kind) {
		this(eventSource, kind, UNSPECIFIED);
	}

	/**
	 * Constructs a new debug event of the given kind with the given detail.
	 *
	 * @param eventSource the object associated with the event
	 * @param kind the kind of debug event (one of the
	 *	kind constants defined by this class)
	 * @param detail extra information about the event (one of the
	 *	detail constants defined by this class)
	 */
	public DebugEvent(Object eventSource, int kind, int detail) {
		super(eventSource);
		if ((kind & (RESUME | SUSPEND | CREATE | TERMINATE | CHANGE)) == 0)
			throw new IllegalArgumentException(DebugCoreMessages.getString("DebugEvent.illegal_kind")); //$NON-NLS-1$
		if (detail != UNSPECIFIED && (detail & (STEP_END | STEP_INTO | STEP_OVER | STEP_RETURN | BREAKPOINT | CLIENT_REQUEST |EVALUATION | EVALUATION_READ_ONLY | STATE | CONTENT)) == 0)
			throw new IllegalArgumentException(DebugCoreMessages.getString("DebugEvent.illegal_detail")); //$NON-NLS-1$
		fKind= kind;
		fDetail= detail;
	}

	/**
	 * Returns a constant describing extra detail about the event - one
	 * of the detail constants defined by this class, possibly
	 * <code>UNSPECIFIED</code>.
	 *
	 * @return the detail code
	 */
	public int getDetail() {
		return fDetail;
	}

	/**
	 * Returns this event's kind - one of the kind constants defined by this class.
	 * 
	 * @return the kind code
	 */
	public int getKind() {
		return fKind;
	}
	
	/**
	 * Returns whether this event's detail indicates the
	 * beginning of a step event. This event's detail is one
	 * of <code>STEP_INTO</code>, <code>STEP_OVER</code>, or
	 * <code>STEP_RETURN</code>.
	 * 
	 * @return whether this event's detail indicates the beginning
	 *  of a step event.
	 * @since 2.0
	 */
	public boolean isStepStart() {
		return (getDetail() & (STEP_INTO | STEP_OVER | STEP_RETURN)) > 0;
	}
	
	/**
	 * Returns whether this event's detail indicates an
	 * evaluation. This event's detail is one
	 * of <code>EVALUATION</code>, or <code>EVALUATION_READ_ONLY</code>.
	 * 
	 * @return whether this event's detail indicates an evaluation.
	 * @since 2.0
	 */
	public boolean isEvaluation() {
		return (getDetail() & (EVALUATION | EVALUATION_READ_ONLY)) > 0;
	}	
	
	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buf = new StringBuffer("DebugEvent["); //$NON-NLS-1$
		if (getSource() != null) {
			buf.append(getSource().toString());
		} else {
			buf.append("null"); //$NON-NLS-1$
		}
		buf.append(", "); //$NON-NLS-1$
		switch (getKind()) {
			case CREATE:
				buf.append("CREATE"); //$NON-NLS-1$
				break;
			case TERMINATE:
				buf.append("TERMINATE"); //$NON-NLS-1$
				break;
			case RESUME:
				buf.append("RESUME"); //$NON-NLS-1$
				break;
			case SUSPEND:
				buf.append("SUSPEND"); //$NON-NLS-1$
				break;				
			case CHANGE:
				buf.append("CHANGE"); //$NON-NLS-1$
				break;
			case UNSPECIFIED:
				buf.append("UNSPECIFIED"); //$NON-NLS-1$
				break;
		}
		buf.append(", "); //$NON-NLS-1$
		switch (getDetail()) {
			case BREAKPOINT:
				buf.append("BREAKPOINT"); //$NON-NLS-1$
				break;
			case CLIENT_REQUEST:
				buf.append("CLIENT_REQUEST"); //$NON-NLS-1$
				break;
			case STEP_END:
				buf.append("STEP_END"); //$NON-NLS-1$
				break;
			case STEP_INTO:
				buf.append("STEP_INTO"); //$NON-NLS-1$
				break;
			case STEP_OVER:
				buf.append("STEP_OVER"); //$NON-NLS-1$
				break;
			case STEP_RETURN:
				buf.append("STEP_RETURN"); //$NON-NLS-1$
				break;
			case EVALUATION:
				buf.append("EVALUATION"); //$NON-NLS-1$
				break;
			case EVALUATION_READ_ONLY:
				buf.append("EVALUATION_READ_ONLY"); //$NON-NLS-1$
				break;								
			case STATE:
				buf.append("STATE"); //$NON-NLS-1$
				break;			
			case CONTENT:
				buf.append("CONTENT"); //$NON-NLS-1$
				break;					
			case UNSPECIFIED:
				buf.append("UNSPECIFIED"); //$NON-NLS-1$
				break;
		}
		buf.append("]"); //$NON-NLS-1$
		return buf.toString();
	}
}

