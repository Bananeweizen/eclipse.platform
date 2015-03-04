/*******************************************************************************
 * Copyright (c) 2002, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tools.resources.metadata;

import java.io.*;
import org.eclipse.core.internal.localstore.SafeChunkyInputStream;
import org.eclipse.core.tools.metadata.*;

/**
 * A dumper for .syncinfo.snap files.
 * 
 * @see org.eclipse.core.tools.resources.metadata.SyncInfoSnapshotDumpingStrategy_3
 */
public class SyncInfoSnapshotDumper extends MultiStrategyDumper {
	static final byte INDEX = 1;
	static final byte QNAME = 2;

	@Override
	protected IStringDumpingStrategy getStringDumpingStrategy(DataInputStream dataInput) throws Exception {
		int versionId = dataInput.readInt();
		if (versionId != 3)
			throw new DumpException("Unknown sync info's snapshot file version: " + versionId); //$NON-NLS-1$
		return new SyncInfoSnapshotDumpingStrategy_3();
	}

	@Override
	protected InputStream openInputStream(File file) throws IOException {
		return new SafeChunkyInputStream(file);
	}
}
