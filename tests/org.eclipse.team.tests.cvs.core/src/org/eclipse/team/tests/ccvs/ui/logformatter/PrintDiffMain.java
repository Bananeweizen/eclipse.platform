package org.eclipse.team.tests.ccvs.ui.logformatter;

/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import java.io.File;

public class PrintDiffMain {
	public static void main(String[] args) {
		File newerFile, olderFile;
		int thresh = 1;
		boolean ignore = false;
		try {
			// lazy argument parsing
			newerFile = new File(args[args.length - 2]);
			olderFile = new File(args[args.length - 1]);
			for (int i = 0; i < args.length - 2; ++i) {
				if ("-i".equals(args[i])) {
					ignore = true;
				} else if ("-t".equals(args[i]) && i < args.length - 3) {
					thresh = Integer.parseInt(args[++i], 10);
				} else throw new IllegalArgumentException(args[i]);
			}
		} catch (Exception e) {
			System.err.println("Usage: java CompareSummaryMain [-t thresh] [-i] <newer log> <older log>");
			System.err.println("  -t thresh: minimum absolute non-negligible difference in ms");
			System.err.println("  -i       : ignore negligible changes in results");
			return;
		}
		
		try {
			// read and merge newer log
			RootEntry newerRoot = LogEntry.readLog(newerFile);
			MergeRunsVisitor mergeVisitor = new MergeRunsVisitor(null);
			newerRoot.accept(mergeVisitor);
			newerRoot = mergeVisitor.getMergedRoot();
			// read and merge older log
			RootEntry olderRoot = LogEntry.readLog(olderFile);
			olderRoot.accept(mergeVisitor);
			olderRoot = mergeVisitor.getMergedRoot();
			// compute and print the differences
			PrintDiffVisitor diffVisitor = new PrintDiffVisitor(System.out, olderRoot, thresh, ignore);
			newerRoot.accept(diffVisitor);
		} catch (Exception e) {
			System.err.println("An error occurred while parsing logs");
			e.printStackTrace();
			return;
		}
	}
}
