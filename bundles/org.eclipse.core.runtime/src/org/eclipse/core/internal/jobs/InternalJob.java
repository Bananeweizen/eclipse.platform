/**********************************************************************
 * Copyright (c) 2003 IBM Corporation and others. All rights reserved.   This
 * program and the accompanying materials are made available under the terms of
 * the Common Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.core.internal.jobs;

import java.util.*;

import org.eclipse.core.internal.runtime.Assert;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.*;

/**
 * Internal implementation class for jobs.
 */
public abstract class InternalJob extends PlatformObject implements Comparable {

	/** 
	 * Job state code (value 8) indicating that a job is blocked by another currently
	 * running job.  From an API point of view, this is the same as WAITING.
	 */
	public static final int BLOCKED = 0x08;

	//flag mask bits
	private static final int M_STATE = 0xFF;
	private static final int M_SYSTEM = 0x0100;
	private static final JobManager manager = JobManager.getInstance();
	private static int nextJobNumber = 0;
	private volatile int flags = Job.NONE;

	private final int jobNumber = nextJobNumber++;
	private List listeners;
	private IProgressMonitor monitor;
	private String name;
	/**
	 * The job ahead of me in a queue or list.
	 */
	private InternalJob next;
	/**
	 * The job behind me in a queue or list.
	 */
	private InternalJob previous;
	private int priority = Job.LONG;
	private IStatus result;
	private ISchedulingRule schedulingRule;
	/**
	 * If the job is waiting, this represents the time the job should start by.  If
	 * this job is sleeping, this represents the time the job should wake up.
	 */
	private long startTime;
	/*
	 * The thread that is currently running this job
	 */
	private Thread thread = null;

	protected InternalJob(String name) {
		Assert.isNotNull(name);
		this.name = name;
	}
	/* (non-Javadoc)
	 * @see Job#addJobListener(IJobChangeListener)
	 */
	protected void addJobChangeListener(IJobChangeListener listener) {
		if (listeners == null)
			listeners = Collections.synchronizedList(new ArrayList(2));
		listeners.add(listener);
	}
	/**
	 * Adds an entry at the end of the list of which this item is the head.
	 */
	final void addLast(InternalJob entry) {
		if (previous == null) {
			previous = entry;
			entry.next = this;
			entry.previous = null;
		} else {
			Assert.isTrue(previous.next() == this);
			previous.addLast(entry);
		}
	}
	protected boolean belongsTo(Object family) {
		return false;
	}
	protected boolean cancel() {
		return manager.cancel(this);
	}
	public final int compareTo(Object otherJob) {
		return (int) (((InternalJob) otherJob).startTime - startTime);
	}
	protected void done(IStatus result) {
		manager.endJob(this, result, true);
	}
	/**
	 * Returns the job listeners that are only listening to this job.  Returns null
	 * if this job has no listeners.
	 */
	final List getListeners() {
		return listeners;
	}
	final IProgressMonitor getMonitor() {
		return monitor;
	}
	protected String getName() {
		return name;
	}
	protected int getPriority() {
		return priority;
	}
	protected IStatus getResult() {
		return result;
	}
	protected ISchedulingRule getRule() {
		return schedulingRule;
	}
	/*package*/
	final long getStartTime() {
		return startTime;
	}
	protected int getState() {
		int state = flags & M_STATE;
		//blocked state is equivalent to waiting state for clients
		return state == BLOCKED ? Job.WAITING : state;
	}
	/* (non-javadoc)
	 * @see Job.getThread
	 */
	protected Thread getThread() {
		return thread;
	}
	final int internalGetState() {
		return flags & M_STATE;
	}
	/*
	 * Must be called from JobManager#setPriority
	 */
	final void internalSetPriority(int newPriority) {
		this.priority = newPriority;
	}
	/*
	 * Must be called from JobManager#setRule
	 */
	final void internalSetRule(ISchedulingRule rule) {
		this.schedulingRule = rule;
	}
	/*
	 * Must be called from JobManager#changeState
	 */
	final void internalSetState(int i) {
		flags = (flags & ~M_STATE) | i;
	}
	/**
	 * Returns true if this job conflicts with the given job, and false otherwise.
	 */
	final boolean isConflicting(InternalJob otherJob) {
		ISchedulingRule otherRule = otherJob.getRule();
		if (schedulingRule == null || otherRule == null)
			return false;
		//if one of the rules is a compound rule, it must be asked the question.
		if (schedulingRule.getClass() == MultiRule.class)
			return schedulingRule.isConflicting(otherRule);
		else
			return otherRule.isConflicting(schedulingRule);
	}
	protected boolean isSystem() {
		return (flags & M_SYSTEM) != 0;
	}
	protected void join() throws InterruptedException {
		manager.join(this);
	}
	/**
	 * Returns the next entry (ahead of this one) in the list, or null if there is no next entry
	 */
	final InternalJob next() {
		return next;
	}
	/**
	 * Returns the previous entry (behind this one) in the list, or null if there is no previous entry
	 */
	final InternalJob previous() {
		return previous;
	}
	/**
	 * Removes this entry from any list it belongs to.  Returns the receiver.
	 */
	final InternalJob remove() {
		if (next != null)
			next.setPrevious(previous);
		if (previous != null)
			previous.setNext(next);
		next = previous = null;
		return this;
	}
	protected abstract IStatus run(IProgressMonitor monitor);
	/* (non-Javadoc)
	 * @see Job#removeJobListener(IJobChangeListener)
	 */
	protected void removeJobChangeListener(IJobChangeListener listener) {
		if (listeners != null)
			listeners.remove(listener);
		if (listeners.isEmpty())
			listeners = null;
	}
	protected void schedule(long delay) {
		manager.schedule(this, delay);
	}
	final void setMonitor(IProgressMonitor monitor) {
		this.monitor = monitor;
	}
	final void setNext(InternalJob entry) {
		this.next = entry;
	}
	final void setPrevious(InternalJob entry) {
		this.previous = entry;
	}
	protected void setPriority(int newPriority) {
		switch (newPriority) {
			case Job.INTERACTIVE:
			case Job.SHORT:
			case Job.LONG:
			case Job.BUILD:
			case Job.DECORATE:
				manager.setPriority(this, newPriority);
				break;
			default:
				throw new IllegalArgumentException(String.valueOf(newPriority));
		}
	}
	final void setResult(IStatus result) {
		this.result = result;
	}
	protected void setRule(ISchedulingRule rule) {
		manager.setRule(this, rule);
	}
	final void setStartTime(long time) {
		startTime = time;
	}
	protected void setSystem(boolean value) {
		flags = value ? flags | M_SYSTEM : flags & ~M_SYSTEM;
	}
	/* (non-javadoc)
	 * @see Job.setThread
	 */
	protected void setThread(Thread thread) {
		this.thread = thread;
	}
	/* (Non-javadoc)
	 * @see Job#shouldSchedule
	 */
	protected boolean shouldSchedule() {
		return true;
	}
	protected boolean sleep() {
		return manager.sleep(this);
	}
	public String toString() {
		return getName() + "(" + jobNumber + ")"; //$NON-NLS-1$//$NON-NLS-2$
	}
	protected void wakeUp() {
		manager.wakeUp(this);
	}
}