package org.eclipse.ui.externaltools.internal.ant.editor.text;

/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp.  All rights reserved.
This file is made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html
**********************************************************************/

//
// Copyright:
// GEBIT Gesellschaft fuer EDV-Beratung
// und Informatik-Technologien mbH, 
// Berlin, Duesseldorf, Frankfurt (Germany) 2002
// All rights reserved.
// 

/*
 * This file originates from an internal package of Eclipse's 
 * Manifest Editor. It has been copied by GEBIT to here in order to
 * permanently use those features. It has been renamed by GEBIT 
 * after copying.
 */

import org.eclipse.jface.text.rules.IWhitespaceDetector;

/**
 * @version 22.10.2002
 */
public class PlantyWhitespaceDetector implements IWhitespaceDetector {

	public boolean isWhitespace(char c) {
		return (c == ' ' || c == '\t' || c == '\n' || c == '\r');
	}
}
