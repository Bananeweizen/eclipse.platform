/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.compare.patch;

import java.util.List;

import org.eclipse.jface.util.Assert;


/**
 * A Hunk describes a range of changed lines and some context lines.
 */
/* package */ class Hunk {
	
	Diff fParent;
	int fOldStart, fOldLength;
	int fNewStart, fNewLength;
	String[] fLines;
	boolean fIsEnabled= true;
	
	
	/* package */ Hunk(Diff parent, int[] oldRange, int[] newRange, List lines) {
		
		fParent= parent;
		if (fParent != null)
			fParent.add(this);
		
		if (oldRange[0] > 0)
			fOldStart= oldRange[0]-1;	// line number start at 0!
		else
			fOldStart= 0;
		fOldLength= oldRange[1];
		if (newRange[0] > 0)
			fNewStart= newRange[0]-1;	// line number start at 0!
		else
			fNewStart= 0;
		fNewLength= newRange[1];
		
		fLines= (String[]) lines.toArray(new String[lines.size()]);
	}
	
	void reverse() {
		int t= fOldStart;
		fOldStart= fNewStart;
		fNewStart= t;
		
		t= fOldLength;
		fOldLength= fNewLength;
		fNewLength= t;
		
		for (int i= 0; i < fLines.length; i++) {
			String line= fLines[i];
			char c= line.charAt(0);
			switch (c) {
			case '+':
				fLines[i]= '-' + line.substring(1);
				break;
			case '-':
				fLines[i]= '+' + line.substring(1);
				break;
			default:
				break;
			}
		}
	}

	/**
	 * Returns the contents of this hunk.
	 * Each line starts with a control character. Their meaning is as follows:
	 * <ul>
	 * <li>
	 * '+': add the line
	 * <li>
	 * '-': delete the line
	 * <li>
	 * ' ': no change, context line
	 * </ul>
	 */
	String getContent() {
		StringBuffer sb= new StringBuffer();
		for (int i= 0; i < fLines.length; i++) {
			String line= fLines[i];
			sb.append(line.substring(0, Patcher.length(line)));
			sb.append('\n');
		}
		return sb.toString();
	}
	
	/**
	 * Returns a descriptive String for this hunk.
	 * It is in the form old_start,old_length -> new_start,new_length.
	 */
	String getDescription() {
		StringBuffer sb= new StringBuffer();
		sb.append(Integer.toString(fOldStart));
		sb.append(',');
		sb.append(Integer.toString(fOldLength));
		sb.append(" -> "); //$NON-NLS-1$
		sb.append(Integer.toString(fNewStart));
		sb.append(',');
		sb.append(Integer.toString(fNewLength));
		return sb.toString();
	}
}
