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
package org.eclipse.team.internal.core.target;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public class UrlUtil {
	
	public static IPath getTrailingPath(URL fullURL, URL startingURL) {
		IPath fullPath = new Path(fullURL.getPath());
		IPath startingPath = new Path(startingURL.getPath());
		int matchingCount = fullPath.matchingFirstSegments(startingPath);
		return fullPath.removeFirstSegments(matchingCount);
	}
	
	public static URL concat(String root, IPath end) throws MalformedURLException {
		return new URL(concatString(root, end));
	}

	public static String concatString(String root, IPath end) {
		if(end.isEmpty())
			return root;
		if(root.length() == 0)
			return end.toString();
		boolean rootHasTrailing = root.charAt(root.length() - 1) == Path.SEPARATOR;  //has trailing '/'
		boolean endHasLeading = end.isAbsolute();	// has leading '/'
			
		if(rootHasTrailing && endHasLeading) //http://mysite/ + /myFolder
			return root + end.toString().substring(1);	 // we have two seperators, drop one
			
		if(!rootHasTrailing && !endHasLeading) //http://mysite + myFolder
			return root + Path.SEPARATOR + end.toString();
				
		return root + end.toString();	//have one separator between the two, just concat end
	}
	
	/*
	 * Return a string that is like url but guaranteed to end with a '/'
	 */
	public static String makeAbsolute(String url) {
		boolean hasTrailing = url.charAt(url.length() - 1) == Path.SEPARATOR;  //has trailing '/'
		if(hasTrailing)
			return url;
		return url + Path.SEPARATOR;
	}
	
	/*
	 * @see toTruncatedPath(URL, int)
	 */
	public static String toTruncatedPath(URL url, int split) {
		return toTruncatedPath(url.getPath(), split);
	}
	
    /*
	 * If the number of segments in the url path is greater than <code>split</code> then
	 * the returned path is truncated to <code>split</code> number of segments and '...' 
	 * is shown as the first segment of the path.
	 */
	public static String toTruncatedPath(String url, int split) {
		IPath path = new Path(url);
		path = path.setDevice(null); // clear the device id, in this case the http:
		int segments = path.segmentCount();
		if(segments>split) {				
			IPath last = path.removeFirstSegments(segments - split);
			return "..." + IPath.SEPARATOR + last.toString(); //$NON-NLS-1$
		}
		return path.toString();
	}
}
