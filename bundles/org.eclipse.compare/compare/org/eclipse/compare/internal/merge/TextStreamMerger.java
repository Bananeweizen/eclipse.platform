package org.eclipse.compare.internal.merge;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import org.eclipse.compare.*;
import org.eclipse.compare.rangedifferencer.RangeDifference;
import org.eclipse.compare.rangedifferencer.RangeDifferencer;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * A simple merger for streams containing text lines.
 */
public class TextStreamMerger implements IStreamMerger {

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.compare.internal.merge.IAutoMerger#automerge(java.io.OutputStream,
     *      org.eclipse.core.resources.IEncodedStorage,
     *      org.eclipse.core.resources.IEncodedStorage,
     *      org.eclipse.core.resources.IEncodedStorage,
     *      org.eclipse.core.runtime.IProgressMonitor)
     */
    public IStatus merge(OutputStream output, String outputEncoding,
			InputStream ancestor, String ancestorEncoding,
			InputStream target, String targetEncoding,
			InputStream other, String otherEncoding,
			IProgressMonitor monitor) {
        
        LineComparator a, t, o;

        try {
            a= new LineComparator(ancestor, ancestorEncoding);
            t= new LineComparator(target, targetEncoding);
            o= new LineComparator(other, otherEncoding);
        } catch (UnsupportedEncodingException e) {
            return new Status(Status.ERROR, CompareUI.PLUGIN_ID, 1, MergeMessages.getString("TextAutoMerge.inputEncodingError"), e); //$NON-NLS-1$
        }

        try {
            char lineSeparator= '\n';
            
            RangeDifference[] diffs = RangeDifferencer.findRanges(monitor, a, t, o);

            for (int i = 0; i < diffs.length; i++) {
                RangeDifference rd = diffs[i];
                switch (rd.kind()) {
                case RangeDifference.ANCESTOR:	// pseudo conflict
                case RangeDifference.NOCHANGE:
                case RangeDifference.RIGHT:
                    for (int j = rd.rightStart(); j < rd.rightEnd(); j++) {
                        String s= o.getLine(j);
                        output.write(s.getBytes(outputEncoding));
                        output.write(lineSeparator);
                    }
                    break;

                case RangeDifference.LEFT:
                    for (int j = rd.leftStart(); j < rd.leftEnd(); j++) {
                        String s= t.getLine(j);
                        output.write(s.getBytes(outputEncoding));
                        output.write(lineSeparator);
                    }
                    break;

                case RangeDifference.CONFLICT:
                    return new Status(Status.ERROR, CompareUI.PLUGIN_ID, CONFLICT, MergeMessages.getString("TextAutoMerge.conflict"), null); //$NON-NLS-1$

                default:
                    break;
                }
            }
 
        } catch (UnsupportedEncodingException e) {
            return new Status(Status.ERROR, CompareUI.PLUGIN_ID, 1, MergeMessages.getString("TextAutoMerge.outputEncodingError"), e); //$NON-NLS-1$
        } catch (IOException e) {
            return new Status(Status.ERROR, CompareUI.PLUGIN_ID, 1, MergeMessages.getString("TextAutoMerge.outputIOError"), e); //$NON-NLS-1$
        }

        return Status.OK_STATUS;
    }
}