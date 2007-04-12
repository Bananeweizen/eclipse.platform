/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Brock Janiczak <brockj@tpg.com.au> - Bug 181919 LineReader creating unneeded garbage
 *******************************************************************************/
package org.eclipse.compare.internal.patch;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;

public class LineReader {

	private boolean fHaveChar= false;
	private int fLastChar;
	private boolean fSawEOF= false;
	private BufferedReader fReader;
	private boolean fIgnoreSingleCR= false;
	private StringBuffer fBuffer= new StringBuffer();
	
	public LineReader(BufferedReader reader) {
		fReader= reader;
		Assert.isNotNull(reader);
	}

	void ignoreSingleCR() {
		fIgnoreSingleCR= true;
	}
	
    /**
     * Reads a line of text. A line is considered to be terminated by any one
     * of a line feed ('\n'), a carriage return ('\r'), or a carriage return
     * followed immediately by a line-feed.
     * @return A string containing the contents of the line including
     *	the line-termination characters, or <code>null</code> if the end of the
     *	stream has been reached
     * @exception IOException If an I/O error occurs
     */
	/* package */ String readLine() throws IOException {
		try {
			while (!fSawEOF) {
				int c= readChar();
				if (c == -1) {
					fSawEOF= true;
					break;
				}
				fBuffer.append((char)c);
				if (c == '\n')
					break;
				if (c == '\r') {
					c= readChar();
					if (c == -1) {
						fSawEOF= true;
						break;	// EOF
					}
					if (c != '\n') {
						if (fIgnoreSingleCR) {
							fBuffer.append((char)c);	
							continue;
						}
						fHaveChar= true;
						fLastChar= c;
					} else
						fBuffer.append((char)c);	
					break;
				}
			}
			
			if (fBuffer.length() != 0) {
				return fBuffer.toString();
			}
			return null;
		} finally {
			fBuffer.setLength(0);
		}
	}
	
	/* package */ void close() {
		try {
			fReader.close();
		} catch (IOException ex) {
			// silently ignored
		}
	}
	
	public List readLines() {
		try {
			List lines= new ArrayList();
			String line;
			while ((line= readLine()) != null)
				lines.add(line);
			return lines;
		} catch (IOException ex) {
			// NeedWork
			//System.out.println("error while reading file: " + fileName + "(" + ex + ")");
		} finally {
			close();
		}
		return null;
	}
	
	/*
	 * Returns the number of characters in the given string without
	 * counting a trailing line separator.
	 */
	/* package */ int lineContentLength(String line) {
		if (line == null)
			return 0;
		int length= line.length();
		for (int i= length-1; i >= 0; i--) {
			char c= line.charAt(i);
			if (c =='\n' || c == '\r')
				length--;
			else
				break;
		}
		return length;
	}
	
	//---- private
	
	private int readChar() throws IOException {
		if (fHaveChar) {
			fHaveChar= false;
			return fLastChar;
		}
		return fReader.read();
	}
}
