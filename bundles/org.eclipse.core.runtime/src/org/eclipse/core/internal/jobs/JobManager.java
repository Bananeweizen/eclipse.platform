/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.jobs;

import java.text.*;
import java.util.*;

import org.eclipse.core.internal.runtime.Assert;
import org.eclipse.core.internal.runtime.Policy;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;

/**
 * Implementation of API type IJobManager
 * 
 * Implementation note: all the data structures of this class are protected
 * by a single lock object held as a private field in this class.  The JobManager
 * instance itself is not used because this class is publicly reachable, and third
 * party clients may try to sychronize on it.
 * 
 * The WorkerPool class uses its own monitor for synchronizing its data
 * structures. To avoid deadlock between the two classes, the JobManager
 * must NEVER call the worker pool while its own monitor is held.
 */
public class JobManager implements IJobManager {
	private static final String OPTION_DEBUG_JOBS = Platform.PI_RUNTIME + "/jobs"; //$NON-NLS-1$
	private static final String OPTION_DEBUG_JOBS_TIMING = Platform.PI_RUNTIME + "/jobs/timing"; //$NON-NLS-1$
	private static final String OPTION_DEBUG_BEGIN_END = Platform.PI_RUNTIME + "/jobs/beginend"; //$NON-NLS-1$
	static final boolean DEBUG = Boolean.TRUE.toString().equalsIgnoreCase(Platform.getDebugOption(OPTION_DEBUG_JOBS));
	static final boolean DEBUG_TIMING = Boolean.TRUE.toString().equalsIgnoreCase(Platform.getDebugOption(OPTION_DEBUG_JOBS_TIMING));
	static final boolean DEBUG_BEGIN_END= Boolean.TRUE.toString().equalsIgnoreCase(Platform.getDebugOption(OPTION_DEBUG_BEGIN_END));
	private static final DateFormat DEBUG_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS"); //$NON-NLS-1$
	private static JobManager instance;
	protected static final long NEVER = Long.MAX_VALUE;
	private final ImplicitJobs implicitJobs = new ImplicitJobs(this);
	private final JobListeners jobListeners = new JobListeners();

	/**
	 * The lock for synchronizing all activity in the job manager.  To avoid deadlock,
	 * this lock must never be held for extended periods, and must never be
	 * held while third party code is being called.
	 */
	private final Object lock = new Object();

	private final LockManager lockManager = new LockManager();

	/**
	 * The pool of worker threads.
	 */
	private WorkerPool pool;

	private IProgressProvider progressProvider = null;
	/**
	 * Jobs that are currently running. Should only be modified from changeState
	 */
	private final HashSet running;

	/**
	 * Jobs that are sleeping.  Some sleeping jobs are scheduled to wake
	 * up at a given start time, while others will sleep indefinitely until woken.
	 * Should only be modified from changeState
	 */
	private final JobQueue sleeping;
	/**
	 * jobs that are waiting to be run. Should only be modified from changeState
	 */
	private final JobQueue waiting;
	public static void debug(String msg) {
		StringBuffer msgBuf = new StringBuffer(msg.length() + 40);
		if (DEBUG_TIMING) {
			DEBUG_FORMAT.format(new Date(), msgBuf, new FieldPosition(0));
			msgBuf.append('-');
		}
		msgBuf.append('[').append(Thread.currentThread()).append(']').append(msg);
		System.out.println(msgBuf.toString());
	}

	public static synchronized JobManager getInstance() {
		if (instance == null) {
			new JobManager();
		}
		return instance;
	}
	public static synchronized void shutdown() {
		if (instance != null)
			instance.doShutdown();
		instance = null;
	}
	private JobManager() {
		instance = this;
		synchronized (lock) {
			waiting = new JobQueue(false);
			sleeping = new JobQueue(true);
			running = new HashSet(10);
			pool = new WorkerPool(this);
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.IJobManager#addJobListener(org.eclipse.core.runtime.jobs.IJobChangeListener)
	 */
	public void addJobChangeListener(IJobChangeListener listener) {
		jobListeners.add(listener);
	}
	public void beginRule(ISchedulingRule rule, IProgressMonitor monitor) {
		implicitJobs.begin(rule, Policy.monitorFor(monitor));
	}
	/**
	 * @deprecated
	 */
	public void beginRule(ISchedulingRule rule) {
		implicitJobs.begin(rule, Policy.monitorFor(null));
	}
	/**
	 * Cancels a job
	 */
	protected boolean cancel(InternalJob job) {
		synchronized (lock) {
			switch (job.getState()) {
				case Job.NONE :
					return true;
				case Job.RUNNING :
					//cannot cancel a job that has already started
					IProgressMonitor monitor = job.getMonitor();
					if (monitor != null) {
						monitor.setCanceled(true);
						return false;
					}
			}
			changeState(job, Job.NONE);
		}
		//only notify listeners if the job was waiting or sleeping
		jobListeners.done((Job) job, Status.CANCEL_STATUS);
		return true;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.IJobManager#cancel(java.lang.String)
	 */
	public void cancel(Object family) {
		//don't synchronize because cancel calls listeners
		for (Iterator it = select(family).iterator(); it.hasNext();) {
			cancel((Job) it.next());
		}
	}
	/**
	 * Atomically updates the state of a job, adding or removing from the
	 * necessary queues or sets.
	 */
	private void changeState(InternalJob job, int newState) {
		synchronized (lock) {
			int oldState = job.internalGetState();
			switch (oldState) {
				case Job.NONE :
					break;
				case InternalJob.BLOCKED :
					//remove this job from the linked list of blocked jobs
					job.remove();
					break;
				case Job.WAITING :
					try {
						waiting.remove(job);
					} catch (RuntimeException e) {
						Assert.isLegal(false, "Tried to remove a job that wasn't in the queue"); //$NON-NLS-1$
					}
					break;
				case Job.SLEEPING :
					try {
						sleeping.remove(job);
					} catch (RuntimeException e) {
						Assert.isLegal(false, "Tried to remove a job that wasn't in the queue"); //$NON-NLS-1$
					}
					break;
				case Job.RUNNING :
					running.remove(job);
					break;
				default :
					Assert.isLegal(false, "Invalid job state: " + job + ", state: " + oldState); //$NON-NLS-1$ //$NON-NLS-2$
			}
			job.internalSetState(newState);
			switch (newState) {
				case Job.NONE :
				case InternalJob.BLOCKED :
					break;
				case Job.WAITING :
					waiting.enqueue(job);
					break;
				case Job.SLEEPING :
					sleeping.enqueue(job);
					break;
				case Job.RUNNING :
					running.add(job);
					break;
				default :
					Assert.isLegal(false, "Invalid job state: " + job + ", state: " + newState); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}
	/**
	 * Returns a new progress monitor for this job.  Never returns null.
	 */
	private IProgressMonitor createMonitor(Job job) {
		IProgressMonitor monitor = null;
		if (progressProvider != null)
			monitor = progressProvider.createMonitor(job);
		if (monitor == null)
			monitor = new NullProgressMonitor();
		((InternalJob) job).setMonitor(monitor);
		return monitor;
	}
	public Job currentJob() {
		Thread current = Thread.currentThread();
		if (current instanceof Worker)
			return ((Worker) current).currentJob();
		return null;
	}
	/**
	 * Returns the delay in milliseconds that a job with a given priority can
	 * tolerate waiting.
	 */
	private long delayFor(int priority) {
		//these values may need to be tweaked based on machine speed
		switch (priority) {
			case Job.INTERACTIVE :
				return 0L;
			case Job.SHORT :
				return 50L;
			case Job.LONG :
				return 100L;
			case Job.BUILD :
				return 500L;
			case Job.DECORATE :
				return 1000L;
			default :
				Assert.isTrue(false, "Job has invalid priority: " + priority); //$NON-NLS-1$
				return 0;
		}
	}
	/**
	 * Shuts down the job manager.  Currently running jobs will be told
	 * to stop, but worker threads may still continue processing.
	 */
	private void doShutdown() {
		//cancel all running jobs
		Job[] toCancel = null;
		synchronized (lock) {
			toCancel = (Job[]) running.toArray(new Job[running.size()]);
			//clean up
			sleeping.clear();
			waiting.clear();
			running.clear();
		}
		//cancel jobs outside sync block to avoid deadlock
		for (int i = 0; i < toCancel.length; i++)
			cancel(toCancel[i]);
		pool.shutdown();
	}
	/**
	 * Indicates that a job was running, and has now finished.
	 */
	protected void endJob(InternalJob job, IStatus result, boolean notify) {
		InternalJob blocked = null;
		synchronized (lock) {
			//if the job is finishing asynchronously, there is nothing more to do for now
			if (result == Job.ASYNC_FINISH)
				return;
			//if job is not known then it cannot be done
			if (job.getState() == Job.NONE)
				return;
			if (JobManager.DEBUG && notify)
				JobManager.debug("Ending job: " + job); //$NON-NLS-1$
			job.setResult(result);
			changeState(job, Job.NONE);
			job.setMonitor(null);
			job.setThread(null);
			blocked = job.previous();
			job.setPrevious(null);
		}

		//add any blocked jobs back to the wait queue
		while (blocked != null) {
			InternalJob previous = blocked.previous();
			//blocked job may have been canceled
			if (blocked.internalGetState() == InternalJob.BLOCKED) {
				changeState(blocked, Job.WAITING);
				pool.jobQueued(blocked);
			}
			blocked = previous;
		}
		//notify listeners outside sync block
		if (notify)
			jobListeners.done((Job)job, result);
	}
	public void endRule(ISchedulingRule rule) {
		implicitJobs.end(rule);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.IJobManager#find(java.lang.String)
	 */
	public Job[] find(Object family) {
		List members = select(family);
		return (Job[]) members.toArray(new Job[members.size()]);
	}
	/**
	 * Returns a running or blocked job whose scheduling rule conflicts with the 
	 * scheduling rule of the given waiting job.  Returns null if there are no 
	 * conflicting jobs.  A job can only run if there are no running jobs and no blocked
	 * jobs whose scheduling rule conflicts with its rule.
	 */
	protected InternalJob findBlockingJob(InternalJob waiting) {
		if (waiting.getRule() == null)
			return null;
		synchronized (lock) {
			for (Iterator it = running.iterator(); it.hasNext();) {
				InternalJob job = (InternalJob) it.next();
				//check the running job and all blocked jobs
				while (job != null) {
					if (waiting.isConflicting(job))
						return job;
					job = job.previous();
				}
			}
		}
		return null;
	}
	/**
	 * Returns the thread that owns the rule that is blocking this job from running, or 
	 * null if there is none.
	 */
	public Thread getBlockingThread(InternalJob job) {
		synchronized (lock) {
			if (job.internalGetState() != InternalJob.BLOCKED)
				return null;
			//if this job is blocked, then the head of the queue is the job that is blocking it
			InternalJob next = job.next();
			while (next.next() != null)
				next = next.next();
			return next == null ? null : next.getThread();
		}
	}
	public LockManager getLockManager() {
		return lockManager;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.Job#job(org.eclipse.core.runtime.jobs.Job)
	 */
	protected void join(InternalJob job) throws InterruptedException {
		final IJobChangeListener listener;
		final Semaphore barrier;
		synchronized (lock) {
			int state = job.getState();
			if (state == Job.NONE)
				return;
			//the semaphore will be released when the job is done
			barrier = new Semaphore(null);
			listener = new JobChangeAdapter() {
				public void done(IJobChangeEvent event) {
					barrier.release();
				}
			};
			job.addJobChangeListener(listener);
			//compute set of all jobs that must run before this one
			//add a listener that removes jobs from the blocking set when they finish
		}
		//wait until listener notifies this thread.
		try {
			barrier.acquire(Long.MAX_VALUE);
		} finally {
			job.removeJobChangeListener(listener);
		}
	}
	/* (non-Javadoc)
	 * @see IJobManager#join(String, IProgressMonitor)
	 */
	public void join(Object family, IProgressMonitor monitor) throws InterruptedException, OperationCanceledException {
		monitor = Policy.monitorFor(monitor);
		IJobChangeListener listener = null;
		final List jobs;
		final int jobCount;
		synchronized (lock) {
			//we never want to join sleeping jobs
			jobs = Collections.synchronizedList(select(family, Job.WAITING | Job.RUNNING | Job.SLEEPING));
			jobCount = jobs.size();
			if (jobCount == 0)
				return;
			listener = new JobChangeAdapter() {
				public void done(IJobChangeEvent event) {
					jobs.remove(event.getJob());
				}
			};
			addJobChangeListener(listener);
		}
		//spin until all jobs are completed
		try {
			monitor.beginTask(Policy.bind("jobs.waitFam"), jobCount); //$NON-NLS-1$
			monitor.subTask(Policy.bind("jobs.waitFamSub", Integer.toString(jobCount))); //$NON-NLS-1$
			int jobsLeft;
			int reportedWorkDone = 0;
			while ((jobsLeft = jobs.size()) > 0) {
				int actualWorkDone = jobCount - jobsLeft;
				if (reportedWorkDone < actualWorkDone) {
					monitor.worked(actualWorkDone - reportedWorkDone);
					reportedWorkDone = actualWorkDone;
					monitor.subTask(Policy.bind("jobs.waitFamSub", Integer.toString(jobsLeft))); //$NON-NLS-1$
				}
				if (Thread.interrupted())
					throw new InterruptedException();
				if (monitor.isCanceled())
					throw new OperationCanceledException();
				Thread.sleep(100);
			}
		} finally {
			monitor.done();
			removeJobChangeListener(listener);
		}
	}
	/* (non-Javadoc)
	 * @see IJobManager#newLock(java.lang.String)
	 */
	public ILock newLock() {
		return lockManager.newLock();
	}
	/**
	 * Removes and returns the first waiting job in the queue. Returns null if there
	 * are no items waiting in the queue.  If an item is removed from the queue,
	 * it is moved to the running jobs list.
	 */
	private Job nextJob() {
		synchronized (lock) {
			//tickle the sleep queue to see if anyone wakes up
			long now = System.currentTimeMillis();
			InternalJob job = sleeping.peek();
			while (job != null && job.getStartTime() < now) {
				job.setStartTime(now + delayFor(job.getPriority()));
				changeState(job, Job.WAITING);
				job = sleeping.peek();
			}
			//process the wait queue until we find a job whose rules are satisfied.
			while ((job = waiting.peek()) != null) {
				InternalJob blocker = findBlockingJob(job);
				if (blocker == null)
					break;
				//queue this job after the job that's blocking it
				changeState(job, InternalJob.BLOCKED);
				//assert job does not already belong to some other data structure
				Assert.isTrue(job.next() == null);
				Assert.isTrue(job.previous() == null);
				blocker.addLast(job);
			}
			//the job to run must be in the running list before we exit
			//the sync block, otherwise two jobs with conflicting rules could start at once
			if (job != null) {
				changeState(job, Job.RUNNING);
				if (JobManager.DEBUG)
					JobManager.debug("Starting job: " + job); //$NON-NLS-1$
			}
			return (Job) job;
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.IJobManager#removeJobListener(org.eclipse.core.runtime.jobs.IJobChangeListener)
	 */
	public void removeJobChangeListener(IJobChangeListener listener) {
		jobListeners.remove(listener);
	}
	/**
	 * Attempts to immediately start a given job.  Returns true if the job was
	 * successfully started, and false if it could not be started immediately
	 * due to a currently running job with a conflicting rule.  Listeners will never
	 * be notified of jobs that are run in this way.
	 */
	protected boolean runNow(InternalJob job) {
		synchronized (lock) {
			//cannot start if there is a conflicting job
			if (findBlockingJob(job) != null)
				return false;
			changeState(job, Job.RUNNING);
			job.run(new NullProgressMonitor());
		}
		return true;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.Job#schedule(long)
	 */
	protected void schedule(InternalJob job, long delay) {
		Assert.isNotNull(job, "Job is null"); //$NON-NLS-1$
		//call hook method outside sync block to avoid deadlock
		if (!job.shouldSchedule())
			return;
		synchronized (lock) {
			//can't schedule a job that is already waiting, sleeping, or running
			if (job.getState() != Job.NONE)
				return;
			if (delay > 0) {
				job.setStartTime(System.currentTimeMillis() + delay);
				changeState(job, Job.SLEEPING);
			} else {
				job.setStartTime(System.currentTimeMillis() + delayFor(job.getPriority()));
				changeState(job, Job.WAITING);
			}
		}
		//notify listeners outside sync block
		jobListeners.scheduled((Job) job, delay);

		//call the pool outside sync block to avoid deadlock
		pool.jobQueued(job);
	}
	/**
	 * Adds all family members in the list of jobs to the collection
	 */
	private void select(List members, Object family, InternalJob firstJob, int stateMask) {
		if (firstJob == null)
			return;
		InternalJob job = firstJob;
		do {
			//note that job state cannot be NONE at this point
			if ((family == null || job.belongsTo(family)) && ((job.getState() & stateMask) != 0))
				members.add(job);
			job = job.previous();
		} while (job != null && job != firstJob);
	}
	/**
	 * Returns a list of all jobs known to the job manager that belong to the given family.
	 */
	private List select(Object family) {
		return select(family, Job.WAITING | Job.SLEEPING | Job.RUNNING);
	}
	/**
	 * Returns a list of all jobs known to the job manager that belong to the given 
	 * family and are in one of the provided states.
	 */
	private List select(Object family, int stateMask) {
		List members = new ArrayList();
		synchronized (lock) {
			if ((stateMask & Job.RUNNING) != 0) {
				for (Iterator it = running.iterator(); it.hasNext();) {
					select(members, family, (InternalJob) it.next(), stateMask);
				}
			}
			if ((stateMask & Job.WAITING) != 0)
				select(members, family, waiting.peek(), stateMask);
			if ((stateMask & Job.SLEEPING) != 0)
				select(members, family, sleeping.peek(), stateMask);
		}
		return members;
	}
	/* (non-Javadoc)
	 * @see IJobManager#setLockListener(LockListener)
	 */
	public void setLockListener(LockListener listener) {
		lockManager.setLockListener(listener);
	}
	/**
	 * Changes a job priority.
	 */
	protected void setPriority(InternalJob job, int newPriority) {
		synchronized (lock) {
			int oldPriority = job.getPriority();
			if (oldPriority == newPriority)
				return;
			job.internalSetPriority(newPriority);
			//if the job is waiting to run, reshuffle the queue
			if (job.getState() == Job.WAITING) {
				long oldStart = job.getStartTime();
				job.setStartTime(oldStart + (delayFor(newPriority) - delayFor(oldPriority)));
				waiting.resort(job);
			}
		}
	}
	/* (non-Javadoc)
	 * @see IJobManager#setProgressProvider(IProgressProvider)
	 */
	public void setProgressProvider(IProgressProvider provider) {
		progressProvider = provider;
	}
	/**
	 * Puts a job to sleep. Returns true if the job was successfully put to sleep.
	 */
	protected boolean sleep(InternalJob job) {
		synchronized (lock) {
			switch (job.getState()) {
				case Job.RUNNING :
					//cannot be paused if it is already running
					if (job.getMonitor() != null)
						return false;
					//job hasn't started running yet (aboutToRun listener)
					break;
				case Job.SLEEPING :
					//update the job wake time
					job.setStartTime(NEVER);
					return true;
				case Job.NONE :
					return true;
				case Job.WAITING :
					//put the job to sleep
					break;
			}
			job.setStartTime(NEVER);
			changeState(job, Job.SLEEPING);
		}
		jobListeners.sleeping((Job) job);
		return true;
	}
	/* (non-Javadoc)
	 * @see IJobManager#sleep(String)
	 */
	public void sleep(Object family) {
		//don't synchronize because sleep calls listeners
		for (Iterator it = select(family).iterator(); it.hasNext();) {
			sleep((InternalJob) it.next());
		}
	}
	/**
	 * Returns the estimated time in milliseconds before the next job is scheduled
	 * to wake up. The result may be negative.  Returns JobManager.NEVER if
	 * there are no sleeping or waiting jobs.
	 */
	protected long sleepHint() {
		synchronized (lock) {
			if (!waiting.isEmpty())
				return 0L;
			InternalJob next = sleeping.peek();
			return next == null ? NEVER : next.getStartTime() - System.currentTimeMillis();
		}
	}
	/**
	 * Returns the next job to be run, or null if no jobs are waiting to run.
	 * The worker must call endJob when the job is finished running.  
	 */
	protected Job startJob() {
		while (true) {
			Job job = nextJob();
			if (job == null)
				return null;
			//must perform this outside sync block because it is third party code
			if (job.shouldRun()) {
				//check for listener veto
				jobListeners.aboutToRun(job);
				//listeners may have canceled or put the job to sleep
				if (job.getState() == Job.RUNNING) {
					((InternalJob) job).setMonitor(createMonitor(job));
					jobListeners.running(job);
					return job;
				}
			}
			if (job.getState() != Job.SLEEPING) {
				//job has been vetoed or canceled, so mark it as done
				endJob(job, Status.CANCEL_STATUS, true);
				continue;
			}
		}
	}
	/* (non-Javadoc)
	 * @see Job#wakeUp(String)
	 */
	protected void wakeUp(InternalJob job) {
		synchronized (lock) {
			//cannot wake up if it is not sleeping
			if (job.getState() != Job.SLEEPING)
				return;
			job.setStartTime(System.currentTimeMillis() + delayFor(job.getPriority()));
			changeState(job, Job.WAITING);
		}
		//call the pool outside sync block to avoid deadlock
		pool.jobQueued(job);

		jobListeners.awake((Job) job);
	}
	/* (non-Javadoc)
	 * @see IJobFamily#wakeUp(String)
	 */
	public void wakeUp(Object family) {
		//don't synchronize because wakeUp calls listeners
		for (Iterator it = select(family).iterator(); it.hasNext();) {
			wakeUp((InternalJob) it.next());
		}
	}


}