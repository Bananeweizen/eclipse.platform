/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.tests.ccvs.ui.old;


import java.io.PrintStream;

public class PrintTextSummaryVisitor implements ILogEntryVisitor {
	private PrintStream os;
	private String indent;
	private int totalAverageTime;
	
	/**
	 * Creates a visitor to print a summary of all entries contained in a log.
	 * @param os the output stream
	 */
	public PrintTextSummaryVisitor(PrintStream os) {
		this.os = os;
		this.indent = "";
		this.totalAverageTime = 0;
	}
	
	protected void visitContainer(LogEntryContainer container) {
		int oldTotalAverageTime = totalAverageTime;
		indent += "  ";
		container.acceptChildren(this);
		int averageTime = totalAverageTime - oldTotalAverageTime;
		StringBuffer line = new StringBuffer(indent);
		line.append("* total: ");
		line.append(Integer.toString(averageTime));
		line.append(" ms");
		os.println(line);
		indent = indent.substring(2);
	}

	/**
	 * Prints the root entry information.
	 */
	public void visitRootEntry(RootEntry entry) {
		entry.acceptChildren(this);
	}
	
	/**
	 * Prints the total average time spent by all subgroups and subtasks.
	 */
	public void visitCaseEntry(CaseEntry entry) {
		StringBuffer line = new StringBuffer(indent);
		line.append("%%% ");
		line.append(entry.getName());
		line.append(", class=");
		line.append(entry.getClassName());
		line.append(':');
		os.println(line);
		visitContainer(entry);
		os.println();
	}

	/**
	 * Prints the total average time spent by all subtasks.
	 */
	public void visitGroupEntry(GroupEntry entry) {		
		StringBuffer line = new StringBuffer(indent);
		line.append("+ ");
		line.append(entry.getName());
		line.append(':');
		os.println(line);
		visitContainer(entry);
	}
	
	/**
	 * Prints the average amount of time spent by a task.
	 */
	public void visitTaskEntry(TaskEntry task) {
		StringBuffer line = new StringBuffer(indent);
		line.append("- ");
		line.append(task.getName());
		line.append(": ");
		if (task.getTotalRuns() != 0) {
			int averageTime = task.getAverageMillis();
			totalAverageTime += averageTime;
			line.append(Integer.toString(averageTime));
			line.append(" ms");
			if (task.getTotalRuns() > 1) {
				line.append(" avg. over ");
				line.append(Integer.toString(task.getTotalRuns()));
				line.append(" runs");
				if (averageTime != 0) {
					int confidence = task.getConfidenceInterval();
					line.append(" (95% C.I. +/- ");
					line.append(Integer.toString(confidence));
					line.append(" ms = ");
					line.append(Util.formatPercentageRatio(confidence, averageTime));
					line.append(")");
				}
			}
		} else {
			line.append("skipped!");
		}
		os.println(line);
	}
}
