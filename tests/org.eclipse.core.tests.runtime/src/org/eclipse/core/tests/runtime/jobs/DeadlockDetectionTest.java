/**********************************************************************
 * Copyright (c) 2003 IBM Corporation and others. All rights reserved.   This
 * program and the accompanying materials are made available under the terms of
 * the Common Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.core.tests.runtime.jobs;

import java.util.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import org.eclipse.core.internal.jobs.JobManager;
import org.eclipse.core.internal.jobs.LockManager;
import org.eclipse.core.internal.jobs.OrderedLock;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IProgressMonitorWithBlocking;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ILock;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.tests.harness.FussyProgressMonitor;
import org.eclipse.core.tests.harness.TestProgressMonitor;


import junit.framework.TestCase;

/**
 * Tests implementation of ILock objects
 */
public class DeadlockDetectionTest extends TestCase {
	public DeadlockDetectionTest() {
		super(null);
	}
	public DeadlockDetectionTest(String name) {
		super(name);
	}
	/**
	 * Creates n runnables on the given lock and adds them to the given list.
	 */
	private void createRunnables(ILock[] locks, int n, ArrayList allRunnables, boolean cond) {
		for (int i = 0; i < n; i++) {
			allRunnables.add(new RandomTestRunnable(locks, "# " + (allRunnables.size() + 1), cond));
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				
			}
		}
	}
	/**
	 * Asks all threads to stop executing
	 */
	private void kill(ArrayList allRunnables) {
		for (Iterator it = allRunnables.iterator(); it.hasNext();) {
			RandomTestRunnable r = (RandomTestRunnable) it.next();
			r.kill();
		}
	}
	/**
	 * A monitor that will receive the event when a thread is blocked on a rule 
	 *
	 */
	private class BlockingMonitor extends TestProgressMonitor implements IProgressMonitorWithBlocking {
		int[] status;
		int index;
		boolean cancelled;
		
		public BlockingMonitor(int[] status, int index) {
			this.status = status;
			this.index = index;
			cancelled = false;
		}
		
		public void setBlocked(IStatus reason) {
			status[index] = StatusChecker.STATUS_BLOCKED;
		}

		public void clearBlocked() {
			//leave empty for now
		}

		public void setCanceled(boolean b) {
			cancelled = true;
		}
		
		public boolean isCanceled() {
			return cancelled;
		}
	}
	/**
	 * Test that deadlock between locks is detected and resolved.
	 * Test with 6 threads competing for 3 locks from a set of 6.
	 */
	public void testComplex() {
		ArrayList allRunnables = new ArrayList();
		LockManager manager = new LockManager();
		OrderedLock lock1 = manager.newLock();
		OrderedLock lock2 = manager.newLock();
		OrderedLock lock3 = manager.newLock();
		OrderedLock lock4 = manager.newLock();
		OrderedLock lock5 = manager.newLock();
		OrderedLock lock6 = manager.newLock();
		createRunnables(new ILock[] { lock1, lock2, lock3 }, 1, allRunnables, true);
		createRunnables(new ILock[] { lock2, lock3, lock4 }, 1, allRunnables, true);
		createRunnables(new ILock[] { lock3, lock4, lock5 }, 1, allRunnables, true);
		createRunnables(new ILock[] { lock4, lock5, lock6 }, 1, allRunnables, true);
		createRunnables(new ILock[] { lock5, lock6, lock1 }, 1, allRunnables, true);
		createRunnables(new ILock[] { lock6, lock1, lock2 }, 1, allRunnables, true);
		start(allRunnables);
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
		}
		kill(allRunnables);
		
		for(int i = 0; i < allRunnables.size(); i++) {
			try {
				((Thread)allRunnables.get(i)).join(100000);
			} catch (InterruptedException e1) {
			}
			assertTrue("1." + i, !((Thread)allRunnables.get(i)).isAlive());
		}
		//the underlying array has to be empty
		assertTrue("Locks not removed from graph.", manager.isEmpty());
	}
	/**
	 * Test simplest deadlock case (2 threads, 2 locks).
	 */
	public void testSimpleDeadlock() {
		ArrayList allRunnables = new ArrayList();
		LockManager manager = new LockManager();
		OrderedLock lock1 = manager.newLock();
		OrderedLock lock2 = manager.newLock();
		
		createRunnables(new ILock[] {lock1, lock2}, 1, allRunnables, false);
		createRunnables(new ILock[] {lock2, lock1}, 1, allRunnables, false);
		
		start(allRunnables);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}
		kill(allRunnables);
		
		for(int i = 0; i < allRunnables.size(); i++) {
			try {
				((Thread)allRunnables.get(i)).join(100000);
			} catch (InterruptedException e1) {
			}
			assertTrue("1." + i, !((Thread)allRunnables.get(i)).isAlive());
		}
		//the underlying array has to be empty
		assertTrue("Locks not removed from graph.", manager.isEmpty());
	}
	/**
	 * Test a more complicated scenario with 3 threads and 3 locks.
	 */
	public void testThreeLocks() {
		ArrayList allRunnables = new ArrayList();
		LockManager manager = new LockManager();
		OrderedLock lock1 = manager.newLock();
		OrderedLock lock2 = manager.newLock();
		OrderedLock lock3 = manager.newLock();
		
		createRunnables(new ILock[] {lock1, lock2}, 1, allRunnables, false);
		createRunnables(new ILock[] {lock2, lock3}, 1, allRunnables, false);
		createRunnables(new ILock[] {lock3, lock1}, 1, allRunnables, false);
		
		start(allRunnables);
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
		}
		kill(allRunnables);
		
		for(int i = 0; i < allRunnables.size(); i++) {
			try {
				((Thread)allRunnables.get(i)).join(100000);
			} catch (InterruptedException e1) {
			}
			assertTrue("1." + i, !((Thread)allRunnables.get(i)).isAlive());
		}
		//the underlying array has to be empty
		assertTrue("Locks not removed from graph.", manager.isEmpty());
	}
	
	/**
	 * Test simple deadlock with 2 threads trying to get 1 rule and 1 lock.
	 */
	public void testRuleLockInteraction() {
		final JobManager manager = JobManager.getInstance();
		final ILock lock = manager.newLock();
		final ISchedulingRule rule = new IdentityRule();
		final int [] status = {StatusChecker.STATUS_WAIT_FOR_START, StatusChecker.STATUS_WAIT_FOR_START};
		
		Thread first = new Thread("Test1") {
			public void run() {
				lock.acquire();
				status[0] = StatusChecker.STATUS_START;
				assertTrue("1.0", manager.getLockManager().isLockOwner());
				StatusChecker.waitForStatus(status, 0, StatusChecker.STATUS_RUNNING, 100);
				manager.beginRule(rule, null);
				assertTrue("2.0", manager.getLockManager().isLockOwner());
				manager.endRule(rule);
				lock.release();
				status[0] = StatusChecker.STATUS_DONE;
			}
		};
		
		Thread second = new Thread("Test2") {
			public void run() {
				manager.beginRule(rule, null);
				status[1] = StatusChecker.STATUS_START;
				assertTrue("1.0", manager.getLockManager().isLockOwner());
				StatusChecker.waitForStatus(status, 1, StatusChecker.STATUS_RUNNING, 100);
				lock.acquire();
				assertTrue("2.0", manager.getLockManager().isLockOwner());
				lock.release();
				manager.endRule(rule);
				status[1] = StatusChecker.STATUS_DONE;
			}
		};
		
		first.start();
		second.start();
		
		StatusChecker.waitForStatus(status, 0, StatusChecker.STATUS_START, 100);
		StatusChecker.waitForStatus(status, 1, StatusChecker.STATUS_START, 100);
		
		status[0] = StatusChecker.STATUS_RUNNING;
		status[1] = StatusChecker.STATUS_RUNNING;
		
		StatusChecker.waitForStatus(status, 0, StatusChecker.STATUS_DONE, 100);
		StatusChecker.waitForStatus(status, 1, StatusChecker.STATUS_DONE, 100);
		waitForThreadDeath(first);
		waitForThreadDeath(second);
		assertTrue("3.0", !first.isAlive());
		assertTrue("4.0", !second.isAlive());
		//the underlying array has to be empty
		assertTrue("Jobs not removed from graph.", manager.getLockManager().isEmpty());	
	}
	
	/**
	 * Test the interaction between jobs with rules and the acquisition of locks.
	 */
	public void testJobRuleLockInteraction() {
		final JobManager manager = JobManager.getInstance();
		final int [] status = {StatusChecker.STATUS_WAIT_FOR_START, StatusChecker.STATUS_WAIT_FOR_START};
		final ISchedulingRule rule1 = new IdentityRule();
		final ISchedulingRule rule2 = new IdentityRule();
		final ILock lock = manager.newLock();
		
		Job first = new Job("Test1") {
			protected IStatus run(IProgressMonitor monitor) {
				try {
					assertTrue("1.0", manager.getLockManager().isLockOwner());
					monitor.beginTask("Testing", 1);
					status[0] = StatusChecker.STATUS_START;
					lock.acquire();
					StatusChecker.waitForStatus(status, 0, StatusChecker.STATUS_RUNNING, 100);
					assertTrue("2.0", manager.getLockManager().isLockOwner());
					lock.release();
					monitor.worked(1);
					status[0] = StatusChecker.STATUS_DONE;
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};
		
		Job second = new Job("Test2") {
			protected IStatus run(IProgressMonitor monitor) {
				try {
					assertTrue("1.0", manager.getLockManager().isLockOwner());
					monitor.beginTask("Testing", 1);
					status[1] = StatusChecker.STATUS_START;
					lock.acquire();
					StatusChecker.waitForStatus(status, 1, StatusChecker.STATUS_RUNNING, 100);
					assertTrue("2.0", manager.getLockManager().isLockOwner());
					lock.release();
					monitor.worked(1);
					status[1] = StatusChecker.STATUS_DONE;
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};
		
		first.setRule(rule1);
		second.setRule(rule2);
		first.schedule();
		second.schedule();
		
		StatusChecker.waitForStatus(status, 0, StatusChecker.STATUS_START, 100);
		StatusChecker.waitForStatus(status, 1, StatusChecker.STATUS_START, 100);
		
		status[0] = StatusChecker.STATUS_RUNNING;
		status[1] = StatusChecker.STATUS_RUNNING;
		
		StatusChecker.waitForStatus(status, 0, StatusChecker.STATUS_DONE, 100);
		StatusChecker.waitForStatus(status, 1, StatusChecker.STATUS_DONE, 100);
		waitForCompletion(first);
		waitForCompletion(second);
		
		assertEquals("3.0", Job.NONE, first.getState());
		assertEquals("3.1", Status.OK_STATUS, first.getResult());
		assertEquals("4.0", Job.NONE, second.getState());
		assertEquals("4.1", Status.OK_STATUS, second.getResult());
		//the underlying array has to be empty
		assertTrue("Jobs not removed from graph.", manager.getLockManager().isEmpty());
	}
	/**
	 * Regression test for bug 46894. Stale entries left over in graph.
	 */
	public void testRuleHierarchyWaitReplace() {
		final JobManager manager = JobManager.getInstance();
		final int NUM_JOBS = 3;
		final int[] status = new int[NUM_JOBS];
		Arrays.fill(status, StatusChecker.STATUS_WAIT_FOR_START);
		RuleHierarchy.reset();
		final ISchedulingRule[] rules = {new RuleHierarchy(), new RuleHierarchy(), new RuleHierarchy()};
		final ILock[] locks = {manager.newLock(), manager.newLock()};
		Job[] jobs = new Job[NUM_JOBS];
		
		jobs[0] = new Job("Test 0") {
				protected IStatus run(IProgressMonitor monitor) {
					try {
						monitor.beginTask("Testing", 1);
						manager.beginRule(rules[0], null);
						status[0] = StatusChecker.STATUS_WAIT_FOR_RUN;
						StatusChecker.waitForStatus(status, 0, StatusChecker.STATUS_RUNNING, 100);
						manager.endRule(rules[0]);
						monitor.worked(1);
					} finally {
						monitor.done();
					}
					return Status.OK_STATUS;
				}
		};
		
		jobs[1] = new Job("Test 1") {
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Testing", 1);
					locks[0].acquire();
					StatusChecker.waitForStatus(status, 1, StatusChecker.STATUS_START, 100);
					manager.beginRule(rules[1], new BlockingMonitor(status, 1));
					status[1] = StatusChecker.STATUS_WAIT_FOR_RUN;
					locks[1].acquire();
					locks[1].release();
					StatusChecker.waitForStatus(status, 1, StatusChecker.STATUS_RUNNING, 100);
					manager.endRule(rules[1]);
					locks[0].release();
					monitor.worked(1);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};
		
		jobs[2] = new Job("Test 2") {
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Testing", 1);
					locks[1].acquire();
					StatusChecker.waitForStatus(status, 2, StatusChecker.STATUS_START, 100);
					manager.beginRule(rules[2], new BlockingMonitor(status, 2));
					status[2] = StatusChecker.STATUS_WAIT_FOR_RUN;
					StatusChecker.waitForStatus(status, 2, StatusChecker.STATUS_RUNNING, 100);
					manager.endRule(rules[2]);
					locks[1].release();
					monitor.worked(1);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};
		
		
			
		for (int i = 0; i < jobs.length; i++) {
			jobs[i].schedule();
		}
		//wait until the first job starts
		StatusChecker.waitForStatus(status, 0, StatusChecker.STATUS_WAIT_FOR_RUN, 100);
		//now let the second job start
		status[1] = StatusChecker.STATUS_START;
		//wait until it blocks on the beginRule call
		StatusChecker.waitForStatus(status, 1, StatusChecker.STATUS_BLOCKED, 100);
		
		//let the third job start, and wait until it too blocks
		status[2] = StatusChecker.STATUS_START;
		//wait until it blocks on the beginRule call
		StatusChecker.waitForStatus(status, 2, StatusChecker.STATUS_BLOCKED, 100);
		
		//end the first job
		status[0] = StatusChecker.STATUS_RUNNING;
				
		//wait until the second job gets the rule
		StatusChecker.waitForStatus(status, 1, StatusChecker.STATUS_WAIT_FOR_RUN, 100);
		//let the job finish
		status[1] = StatusChecker.STATUS_RUNNING;
		
		//now wait until the third job gets the rule 
		StatusChecker.waitForStatus(status, 2, StatusChecker.STATUS_WAIT_FOR_RUN, 100);
		//let the job finish
		status[2] = StatusChecker.STATUS_RUNNING;
		
		for(int i = 0; i < jobs.length; i++) {
			waitForCompletion(jobs[i]);
		}
			
		for(int i = 0; i < jobs.length; i++) {
			assertEquals("10." + i, Job.NONE, jobs[i].getState());
			assertEquals("10." + i, Status.OK_STATUS, jobs[i].getResult());
		}
		//the underlying graph has to be empty
		assertTrue("Jobs not removed from graph.", manager.getLockManager().isEmpty());
	}
	
	/**
	 * Regression test for bug 46894. Deadlock was not detected (before).
	 */
	public void testDetectDeadlock() {
		final JobManager manager = JobManager.getInstance();
		final int NUM_JOBS = 3;
		final int[] status = new int[NUM_JOBS];
		Arrays.fill(status, StatusChecker.STATUS_WAIT_FOR_START);
		RuleHierarchy.reset();
		final ISchedulingRule[] rules = {new RuleHierarchy(), new RuleHierarchy(), new RuleHierarchy()};
		final ILock lock = manager.newLock();
		Job[] jobs = new Job[NUM_JOBS];
		
		jobs[0] = new Job("Test 0") {
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Testing", 1);
					manager.beginRule(rules[1], null);
					status[0] = StatusChecker.STATUS_WAIT_FOR_RUN;
					StatusChecker.waitForStatus(status, 0, StatusChecker.STATUS_RUNNING, 100);
					manager.endRule(rules[1]);
					monitor.worked(1);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};
		
		jobs[1] = new Job("Test 1") {
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Testing", 1);
					lock.acquire();
					StatusChecker.waitForStatus(status, 1, StatusChecker.STATUS_START, 100);
					manager.beginRule(rules[0], new BlockingMonitor(status, 1));
					status[1] = StatusChecker.STATUS_WAIT_FOR_RUN;
					StatusChecker.waitForStatus(status, 1, StatusChecker.STATUS_RUNNING, 100);
					manager.endRule(rules[0]);
					lock.release();
					monitor.worked(1);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};
		
		jobs[2] = new Job("Test 2") {
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Testing", 1);
					StatusChecker.waitForStatus(status, 2, StatusChecker.STATUS_START, 100);
					manager.beginRule(rules[2], null);
					status[2] = StatusChecker.STATUS_WAIT_FOR_RUN;
					StatusChecker.waitForStatus(status, 2, StatusChecker.STATUS_RUNNING, 100);
					lock.acquire();
					lock.release();
					manager.endRule(rules[2]);
					monitor.worked(1);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};
			
		for (int i = 0; i < jobs.length; i++) {
			jobs[i].schedule();
		}
		//wait until the first job starts
		StatusChecker.waitForStatus(status, 0, StatusChecker.STATUS_WAIT_FOR_RUN, 100);
		//now let the third job start
		status[2] = StatusChecker.STATUS_START;
		//wait until it gets the rule
		StatusChecker.waitForStatus(status, 2, StatusChecker.STATUS_WAIT_FOR_RUN, 100);
		
		//let the second job start
		status[1] = StatusChecker.STATUS_START;
		//wait until it blocks on the beginRule call
		StatusChecker.waitForStatus(status, 1, StatusChecker.STATUS_BLOCKED, 100);
		
		//let the third job try for the lock
		status[2] = StatusChecker.STATUS_RUNNING;
		//end the first job
		status[0] = StatusChecker.STATUS_RUNNING;
				
		//wait until the second job gets the rule
		StatusChecker.waitForStatus(status, 1, StatusChecker.STATUS_WAIT_FOR_RUN, 100);
		//let the job finish
		status[1] = StatusChecker.STATUS_RUNNING;
		//wait until all jobs are done
		for(int i = 0; i < jobs.length; i++) {
			waitForCompletion(jobs[i]);
		}
		
		for(int i = 0; i < jobs.length; i++) {
			assertEquals("10." + i, Job.NONE, jobs[i].getState());
			assertEquals("10." + i, Status.OK_STATUS, jobs[i].getResult());
		}
		//the underlying graph has to be empty
		assertTrue("Jobs not removed from graph.", manager.getLockManager().isEmpty());
	}
	
	/**
	 * Test that when 3 columns and 1 row are empty, they are correctly removed from the graph.
	 */
	public void testMultipleColumnRemoval() {
		final JobManager manager = JobManager.getInstance();
		final int NUM_JOBS = 3;
		final int[] status = new int[NUM_JOBS];
		Arrays.fill(status, StatusChecker.STATUS_WAIT_FOR_START);
		RuleHierarchy.reset();
		final ISchedulingRule[] rules = {new RuleHierarchy(), new RuleHierarchy(), new RuleHierarchy()};
		final IProgressMonitor first = new BlockingMonitor(status, 1);
		final IProgressMonitor second = new BlockingMonitor(status, 2);
		Job[] jobs = new Job[NUM_JOBS];
		
		jobs[0] = new Job("Test 0") {
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Testing", 1);
					manager.beginRule(rules[0], null);
					status[0] = StatusChecker.STATUS_WAIT_FOR_RUN;
					StatusChecker.waitForStatus(status, 0, StatusChecker.STATUS_RUNNING, 100);
					manager.endRule(rules[0]);
					monitor.worked(1);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};
		
		jobs[1] = new Job("Test 1") {
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Testing", 1);
					StatusChecker.waitForStatus(status, 1, StatusChecker.STATUS_START, 100);
					manager.beginRule(rules[1], first);
					monitor.worked(1);
				} finally {
					status[1] = StatusChecker.STATUS_DONE;
					manager.endRule(rules[1]);
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};
		
		jobs[2] = new Job("Test 2") {
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Testing", 1);
					StatusChecker.waitForStatus(status, 2, StatusChecker.STATUS_START, 100);
					manager.beginRule(rules[2], second);
					monitor.worked(1);
				} finally {
					status[2] = StatusChecker.STATUS_DONE;
					manager.endRule(rules[2]);
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};
		//schedule all the jobs	
		for (int i = 0; i < jobs.length; i++) {
			jobs[i].schedule();
		}
		//wait until the first job starts
		StatusChecker.waitForStatus(status, 0, StatusChecker.STATUS_WAIT_FOR_RUN, 100);
		//now let the other two jobs start
		status[1] = StatusChecker.STATUS_START;
		status[2] = StatusChecker.STATUS_START;
		//wait until both are blocked on the beginRule call
		StatusChecker.waitForStatus(status, 1, StatusChecker.STATUS_BLOCKED, 100);
		StatusChecker.waitForStatus(status, 2, StatusChecker.STATUS_BLOCKED, 100);
		
		//cancel the blocked jobs
		first.setCanceled(true);
		second.setCanceled(true);
		
		//wait until both jobs are done
		StatusChecker.waitForStatus(status, 1, StatusChecker.STATUS_DONE, 100);
		StatusChecker.waitForStatus(status, 2, StatusChecker.STATUS_DONE, 100);
		
		//end the first job
		status[0] = StatusChecker.STATUS_RUNNING;
		//wait until all jobs are done
		for(int i = 0; i < jobs.length; i++) {
			waitForCompletion(jobs[i]);
		}
				
		//the underlying graph has to be empty
		assertTrue("Jobs not removed from graph.", manager.getLockManager().isEmpty());
	}
	
	/**
	 * Test that the graph is cleared after a thread stops waiting for a rule.
	 */
	public void testBeginRuleCancelAfterWait() {
		final JobManager manager = JobManager.getInstance();
		final ISchedulingRule rule1 = new RuleSetA();
		final ISchedulingRule rule2 = new RuleSetB();
		RuleSetA.conflict = true;
		final int[] status = {StatusChecker.STATUS_WAIT_FOR_START, StatusChecker.STATUS_WAIT_FOR_START};
		final IProgressMonitor canceller = new FussyProgressMonitor();
		
		Job ruleOwner = new Job("Test1") {
			protected IStatus run(IProgressMonitor monitor) {
				try {
					status[0] = StatusChecker.STATUS_START;
					manager.beginRule(rule1, null);
					StatusChecker.waitForStatus(status, 0, StatusChecker.STATUS_RUNNING, 1000);
					manager.endRule(rule1);
					monitor.worked(1);
				} finally {
					monitor.done();
					status[0] = StatusChecker.STATUS_DONE;
				}
				return Status.OK_STATUS;
			}
		};
		
		Job ruleWait = new Job("Test2") {
			protected IStatus run(IProgressMonitor monitor) {
				try {
					status[1] = StatusChecker.STATUS_RUNNING;
					manager.beginRule(rule2, canceller);
					monitor.worked(1);
				} finally {
					manager.endRule(rule2);
					monitor.done();
					status[1] = StatusChecker.STATUS_DONE;
				}
				return Status.OK_STATUS;
			}
		};
		
		ruleOwner.schedule();
		StatusChecker.waitForStatus(status, StatusChecker.STATUS_START);
		
		//schedule a job that is going to begin a conflicting rule and then cancel the wait
		ruleWait.schedule();
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
		}
		//cancel the wait for the rule
		canceller.setCanceled(true);
		//wait until the job completes
		StatusChecker.waitForStatus(status, 1, StatusChecker.STATUS_DONE, 100);
		
		//let the first job finish
		status[0] = StatusChecker.STATUS_RUNNING;
		StatusChecker.waitForStatus(status, StatusChecker.STATUS_DONE);
		int i = 0;
		waitForCompletion(ruleOwner);
		RuleSetA.conflict = false;
		//the underlying graph should now be empty
		assertTrue("Cancelled rule not removed from graph.", manager.getLockManager().isEmpty());
	}
	
	/**
	 * Test that implicit rules do not create extraneous entries
	 */
	public void testImplicitRules() {
		final JobManager manager = JobManager.getInstance();
		final int NUM_JOBS = 4;
		final int[] status = new int[NUM_JOBS];
		Arrays.fill(status, StatusChecker.STATUS_WAIT_FOR_START);
		RuleHierarchy.reset();
		final ISchedulingRule[] rules = {new RuleHierarchy(), new RuleHierarchy(), new RuleHierarchy(), new RuleHierarchy()};
		Job[] jobs = new Job[NUM_JOBS];
		
		jobs[0] = new Job("Test 0") {
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Testing", 1);
					manager.beginRule(rules[3], null);
					status[0] = StatusChecker.STATUS_WAIT_FOR_RUN;
					StatusChecker.waitForStatus(status, 0, StatusChecker.STATUS_RUNNING, 100);
					manager.endRule(rules[3]);
					monitor.worked(1);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};
		
		jobs[1] = new Job("Test 1") {
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Testing", 1);
					manager.beginRule(rules[2], null);
					status[1] = StatusChecker.STATUS_WAIT_FOR_RUN;
					StatusChecker.waitForStatus(status, 1, StatusChecker.STATUS_RUNNING, 100);
					manager.endRule(rules[2]);
					monitor.worked(1);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};
		
		jobs[2] = new Job("Test 2") {
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Testing", 1);
					StatusChecker.waitForStatus(status, 2, StatusChecker.STATUS_START, 100);
					manager.beginRule(rules[0], new BlockingMonitor(status, 2));
					status[2] = StatusChecker.STATUS_WAIT_FOR_RUN;
					StatusChecker.waitForStatus(status, 2, StatusChecker.STATUS_RUNNING, 100);
					manager.endRule(rules[0]);
					monitor.worked(1);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};
		
		jobs[3] = new Job("Test 3") {
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Testing", 1);
					StatusChecker.waitForStatus(status, 3, StatusChecker.STATUS_START, 100);
					manager.beginRule(rules[1], new BlockingMonitor(status, 3));
					status[3] = StatusChecker.STATUS_WAIT_FOR_RUN;
					StatusChecker.waitForStatus(status, 3, StatusChecker.STATUS_RUNNING, 100);
					manager.endRule(rules[1]);
					monitor.worked(1);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};
		
		for (int i = 0; i < jobs.length; i++) {
			jobs[i].schedule();
		}
		//wait until the first 2 jobs start
		StatusChecker.waitForStatus(status, 0, StatusChecker.STATUS_WAIT_FOR_RUN, 100);
		StatusChecker.waitForStatus(status, 1, StatusChecker.STATUS_WAIT_FOR_RUN, 100);
		//now let the third job start
		status[2] = StatusChecker.STATUS_START;
		//wait until it blocks on the beginRule call
		StatusChecker.waitForStatus(status, 2, StatusChecker.STATUS_BLOCKED, 100);
		
		//let the fourth job start
		status[3] = StatusChecker.STATUS_START;
		//wait until it blocks on the beginRule call
		StatusChecker.waitForStatus(status, 3, StatusChecker.STATUS_BLOCKED, 100);
		
		//end the first 2 jobs
		status[0] = StatusChecker.STATUS_RUNNING;
		status[1] = StatusChecker.STATUS_RUNNING;
		
		//wait until the third job gets the rule
		StatusChecker.waitForStatus(status, 2, StatusChecker.STATUS_WAIT_FOR_RUN, 100);
		//let the job finish
		status[2] = StatusChecker.STATUS_RUNNING;
		
		//wait until the fourth job gets the rule
		StatusChecker.waitForStatus(status, 3, StatusChecker.STATUS_WAIT_FOR_RUN, 100);
		//let the job finish
		status[3] = StatusChecker.STATUS_RUNNING;
		
		//wait until all jobs are done
		for(int i = 0; i < jobs.length; i++) {
			waitForCompletion(jobs[i]);
		}
		
		for(int i = 0; i < jobs.length; i++) {
			assertEquals("10." + i, Job.NONE, jobs[i].getState());
			assertEquals("10." + i, Status.OK_STATUS, jobs[i].getResult());
		}
		//the underlying graph has to be empty
		assertTrue("Jobs not removed from graph.", manager.getLockManager().isEmpty());
	}
	
	/**
	 * Regression test for bug 46894. Stale rules left over in graph.
	 */
	public void testRuleHierarchyLockInteraction() {
		final JobManager manager = JobManager.getInstance();
		final int NUM_JOBS = 5;
		final int[] status = new int[NUM_JOBS];
		Arrays.fill(status, StatusChecker.STATUS_WAIT_FOR_START);
		RuleHierarchy.reset();
		final ISchedulingRule[] rules = {new RuleHierarchy(), new RuleHierarchy(), new RuleHierarchy()};
		Job[] jobs = new Job[NUM_JOBS];
		
		jobs[0] = new Job("Test 0") {
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Testing", 1);
					manager.beginRule(rules[1], null);
					status[0] = StatusChecker.STATUS_WAIT_FOR_RUN;
					StatusChecker.waitForStatus(status, 0, StatusChecker.STATUS_RUNNING, 100);
					manager.endRule(rules[1]);
					monitor.worked(1);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};
		
		jobs[1] = new Job("Test 1") {
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Testing", 1);
					StatusChecker.waitForStatus(status, 1, StatusChecker.STATUS_START, 100);
					manager.beginRule(rules[2], null);
					status[1] = StatusChecker.STATUS_WAIT_FOR_RUN;
					StatusChecker.waitForStatus(status, 1, StatusChecker.STATUS_RUNNING, 100);
					manager.endRule(rules[2]);
					monitor.worked(1);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};
		
		jobs[2] = new Job("Test 2") {
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Testing", 1);
					StatusChecker.waitForStatus(status, 2, StatusChecker.STATUS_START, 100);
					manager.beginRule(rules[0], new BlockingMonitor(status, 2));
					status[2] = StatusChecker.STATUS_WAIT_FOR_RUN;
					StatusChecker.waitForStatus(status, 2, StatusChecker.STATUS_RUNNING, 100);
					manager.endRule(rules[0]);
					monitor.worked(1);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};
		
		jobs[3] = new Job("Test 3") {
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Testing", 1);
					StatusChecker.waitForStatus(status, 3, StatusChecker.STATUS_START, 100);
					manager.beginRule(rules[2], new BlockingMonitor(status, 3));
					status[3] = StatusChecker.STATUS_WAIT_FOR_RUN;
					StatusChecker.waitForStatus(status, 3, StatusChecker.STATUS_RUNNING, 100);
					manager.endRule(rules[2]);
					monitor.worked(1);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};
		
		jobs[4] = new Job("Test 4") {
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Testing", 1);
					StatusChecker.waitForStatus(status, 4, StatusChecker.STATUS_START, 100);
					manager.beginRule(rules[2], new BlockingMonitor(status, 4));
					status[4] = StatusChecker.STATUS_WAIT_FOR_RUN;
					StatusChecker.waitForStatus(status, 4, StatusChecker.STATUS_RUNNING, 100);
					manager.endRule(rules[2]);
					monitor.worked(1);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};
		
		for (int i = 0; i < jobs.length; i++) {
			jobs[i].schedule();
		}
		//wait until the first job starts
		StatusChecker.waitForStatus(status, 0, StatusChecker.STATUS_WAIT_FOR_RUN, 100);
		//now let the second job start
		status[1] = StatusChecker.STATUS_START;
		StatusChecker.waitForStatus(status, 1, StatusChecker.STATUS_WAIT_FOR_RUN, 100);
		
		//let the third job register the wait
		status[2] = StatusChecker.STATUS_START;
		//wait until the job is blocked on the scheduling rule
		StatusChecker.waitForStatus(status, 2, StatusChecker.STATUS_BLOCKED, 100);
		
		//let the fourth job register the wait
		status[3] = StatusChecker.STATUS_START;
		//wait until the job is blocked on the scheduling rule
		StatusChecker.waitForStatus(status, 3, StatusChecker.STATUS_BLOCKED, 100);
		
		//end the first job, and the second job
		status[0] = StatusChecker.STATUS_RUNNING;
		status[1] = StatusChecker.STATUS_RUNNING;
		
		//wait until the third job gets the rule
		StatusChecker.waitForStatus(status, 2, StatusChecker.STATUS_WAIT_FOR_RUN, 100);
		
		//let the fifth job start its wait
		status[4] = StatusChecker.STATUS_START;
		StatusChecker.waitForStatus(status, 4, StatusChecker.STATUS_BLOCKED, 100);
		
		//let the third job finish
		status[2] = StatusChecker.STATUS_RUNNING;
		
		//wait until the fourth job gets the rule
		StatusChecker.waitForStatus(status, 3, StatusChecker.STATUS_WAIT_FOR_RUN, 100);
		//let the fourth job end
		status[3] = StatusChecker.STATUS_RUNNING;
		
		//wait until the fifth job gets the rule
		StatusChecker.waitForStatus(status, 4, StatusChecker.STATUS_WAIT_FOR_RUN, 100);
		//let the fifth job end
		status[4] = StatusChecker.STATUS_RUNNING;
		
		for(int i = 0; i < jobs.length; i++) {
			waitForCompletion(jobs[i]);
		}
		
		for(int i = 0; i < jobs.length; i++) {
			assertEquals("10." + i, Job.NONE, jobs[i].getState());
			assertEquals("10." + i, Status.OK_STATUS, jobs[i].getResult());
		}
		//the underlying graph has to be empty
		assertTrue("Jobs not removed from graph.", manager.getLockManager().isEmpty());
	}
	
	/**
	 * Test that the deadlock detector resolves deadlock correctly.
	 * 60 threads are competing for 6 locks (need to acquire 3 locks at the same time).
	 */
	public void testVeryComplex() {
		ArrayList allRunnables = new ArrayList();
		LockManager manager = new LockManager();
		OrderedLock lock1 = manager.newLock();
		OrderedLock lock2 = manager.newLock();
		OrderedLock lock3 = manager.newLock();
		OrderedLock lock4 = manager.newLock();
		OrderedLock lock5 = manager.newLock();
		OrderedLock lock6 = manager.newLock();
		createRunnables(new ILock[] { lock1, lock2, lock3 }, 10, allRunnables, true);
		createRunnables(new ILock[] { lock2, lock3, lock4 }, 10, allRunnables, true);
		createRunnables(new ILock[] { lock3, lock4, lock5 }, 10, allRunnables, true);
		createRunnables(new ILock[] { lock4, lock5, lock6 }, 10, allRunnables, true);
		createRunnables(new ILock[] { lock5, lock6, lock1 }, 10, allRunnables, true);
		createRunnables(new ILock[] { lock6, lock1, lock2 }, 10, allRunnables, true);
		start(allRunnables);
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
		}
		kill(allRunnables);
		
		for(int i = 0; i < allRunnables.size(); i++) {
			try {
				((Thread)allRunnables.get(i)).join(100000);
			} catch (InterruptedException e1) {
			}
			assertTrue("1." + i, !((Thread)allRunnables.get(i)).isAlive());
		}
		//the underlying array has to be empty
		assertTrue("Locks not removed from graph.", manager.isEmpty());
	}
	/**
	 * Spin until the given job completes
	 */
	private void waitForCompletion(Job job) {
		int i = 0;
		while(job.getState() != Job.NONE) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
			assertTrue("Timeout waiting for job to end.", ++i < 100);
		}
	}
	/**
	 * Spin until the given thread dies
	 */
	private void waitForThreadDeath(Thread thread) {
		int i = 0;
		while(thread.isAlive()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
			assertTrue("Timeout waiting for job to end.", ++i < 100);
		}
	}
	
	/**
	 * Test a complex scenario of interaction between rules and locks.
	 * 15 jobs are competing for 5 rules and 5 locks.
	 * Each job must acquire 1 rule and 2 locks in random order.
	 */
	public void testComplexRuleLockInteraction() {
		final JobManager manager = JobManager.getInstance();
		final int NUM_LOCKS = 5;
		final int [] status = {StatusChecker.STATUS_WAIT_FOR_START};
		RuleHierarchy.reset();
		final ISchedulingRule[] rules = {new RuleHierarchy(), new RuleHierarchy(), new RuleHierarchy(), new RuleHierarchy(), new RuleHierarchy()};
		final ILock[] locks = {manager.newLock(), manager.newLock(), manager.newLock(), manager.newLock(), manager.newLock()};
		Job[] jobs = new Job[NUM_LOCKS*3];
		final Random random = new Random();
		
		for(int i = 0; i < jobs.length; i++) {
			jobs[i] = new Job("Test"+i) {
				protected IStatus run(IProgressMonitor monitor) {
					try {
						monitor.beginTask("Testing", IProgressMonitor.UNKNOWN);
						while(status[0] != StatusChecker.STATUS_DONE) {
							int indexRule = random.nextInt(NUM_LOCKS);
							int indexLock = random.nextInt(NUM_LOCKS);
							int secondIndex = random.nextInt(NUM_LOCKS);
							if((indexRule%2) == 0) {
								manager.beginRule(rules[indexRule], null);
								locks[indexLock].acquire();
								locks[secondIndex].acquire();
								assertTrue(indexRule + ".0", manager.getLockManager().isLockOwner());
								locks[secondIndex].release();
								locks[indexLock].release();
								manager.endRule(rules[indexRule]);
							}
							else {
								locks[indexLock].acquire();
								manager.beginRule(rules[indexRule], null);
								locks[secondIndex].acquire();
								assertTrue(indexLock + ".0", manager.getLockManager().isLockOwner());
								locks[secondIndex].release();
								manager.endRule(rules[indexRule]);
								locks[indexLock].release();
							}
							monitor.worked(1);
						}
					} catch(RuntimeException e) {
						e.printStackTrace();
					} finally {
						monitor.done();
					}
					return Status.OK_STATUS;
				}
			};
			jobs[i].schedule();
		}
		
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			
		}
		
		status[0] = StatusChecker.STATUS_DONE;
		
		for(int i = 0; i < jobs.length; i++) {
			int j = 0;
			while(jobs[i].getState() != Job.NONE) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {
					
				}
				//sanity check to avoid hanging tests
				assertTrue("Timeout waiting for jobs to finish.", ++j < 1000);
			}
		}
		
		for(int i = 0; i < jobs.length; i++) {
			assertEquals("10." + i, Job.NONE, jobs[i].getState());
			assertEquals("10." + i, Status.OK_STATUS, jobs[i].getResult());
		}
		//the underlying array has to be empty
		assertTrue("Jobs not removed from graph.", manager.getLockManager().isEmpty());
	}
	
	/**
	 * Test that when a job with a rule is cancelled, no stale entries are left in the graph.
	 */
	public void testJobRuleCancellation() {
		final JobManager manager = JobManager.getInstance();
		final ISchedulingRule rule = new IdentityRule();
		final int[] status = {StatusChecker.STATUS_WAIT_FOR_START};
		
		Job first = new Job("Test1") {
			protected IStatus run(IProgressMonitor monitor) {
				try {
					assertTrue("1.0", manager.getLockManager().isLockOwner());
					status[0] = StatusChecker.STATUS_START;
					StatusChecker.waitForStatus(status, 0, StatusChecker.STATUS_RUNNING, 1000);
					monitor.worked(1);
				} finally {
					monitor.done();
					status[0] = StatusChecker.STATUS_DONE;
				}
				return Status.OK_STATUS;
			}
		};
		
		Job second = new Job("Test2") {
			protected IStatus run(IProgressMonitor monitor) {
				try {
					assertTrue("2.0", manager.getLockManager().isLockOwner());
					monitor.worked(1);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};
		
		first.setRule(rule);
		second.setRule(rule);
		
		first.schedule();
		StatusChecker.waitForStatus(status, StatusChecker.STATUS_START);
				
		//schedule a job with the same rule and then cancel it
		second.schedule();
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
		}
		second.cancel();
		status[0] = StatusChecker.STATUS_RUNNING;
		StatusChecker.waitForStatus(status, StatusChecker.STATUS_DONE);
		waitForCompletion(first);
		//the underlying graph should now be empty
		assertTrue("Cancelled job not removed from graph.", manager.getLockManager().isEmpty());
	}
	/**
	 * Test that a lock which was acquired several times and then suspended to resolve deadlock
	 * is set correctly to the proper depth when it is reacquired by the thread that used to own it.
	 */
	public void testLockMultipleAcquireThenSuspend() {
		final JobManager manager = JobManager.getInstance();
		final ISchedulingRule rule = new IdentityRule();
		final ILock lock = manager.newLock();
		final int[] status = {StatusChecker.STATUS_WAIT_FOR_START, StatusChecker.STATUS_WAIT_FOR_START};
		
		Job first = new Job("Test1") {
			protected IStatus run(IProgressMonitor monitor) {
				try {
					manager.beginRule(rule, null);
					status[0] = StatusChecker.STATUS_WAIT_FOR_RUN;
					StatusChecker.waitForStatus(status, 0, StatusChecker.STATUS_START, 100);
					lock.acquire();
					lock.release();
					manager.endRule(rule);
					monitor.worked(1);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};
		
		Job second = new Job("Test2") {
			protected IStatus run(IProgressMonitor monitor) {
				try {
					lock.acquire();
					lock.acquire();
					lock.acquire();
					lock.acquire();
					status[1] = StatusChecker.STATUS_WAIT_FOR_RUN;
					StatusChecker.waitForStatus(status, 1, StatusChecker.STATUS_START, 100);
					manager.beginRule(rule, null);
					manager.endRule(rule);
					lock.release();
					status[1] = StatusChecker.STATUS_WAIT_FOR_RUN;
					StatusChecker.waitForStatus(status, 1, StatusChecker.STATUS_RUNNING, 100);
					lock.release();
					lock.release();
					lock.release();
					monitor.worked(1);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};
		//schedule the jobs
		first.schedule();
		second.schedule();
		//wait until one gets a rule, and the other acquires a lock
		StatusChecker.waitForStatus(status, 0, StatusChecker.STATUS_WAIT_FOR_RUN, 100);
		StatusChecker.waitForStatus(status, 1, StatusChecker.STATUS_WAIT_FOR_RUN, 100);
		
		//let the deadlock happen
		status[0] = StatusChecker.STATUS_START;
		status[1] = StatusChecker.STATUS_START;
		
		//wait until it is resolved and the second job releases the lock once
		StatusChecker.waitForStatus(status, 1, StatusChecker.STATUS_WAIT_FOR_RUN, 100);
		
		//the underlying graph should not be empty yet
		assertTrue("Held lock removed from graph.", !manager.getLockManager().isEmpty());
		
		//wait until the jobs are done
		status[1] = StatusChecker.STATUS_RUNNING;
		waitForCompletion(first);
		waitForCompletion(second);
		//the underlying graph should now be empty
		assertTrue("Jobs not removed from graph.", manager.getLockManager().isEmpty());
	}
		
	private void start(ArrayList allRunnables) {
		for (Iterator it = allRunnables.iterator(); it.hasNext();) {
			RandomTestRunnable r = (RandomTestRunnable) it.next();
			r.start();
		}
	}
}
