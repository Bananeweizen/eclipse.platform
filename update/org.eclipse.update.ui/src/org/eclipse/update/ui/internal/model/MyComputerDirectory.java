package org.eclipse.update.ui.internal.model;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.net.URL;
import org.eclipse.update.core.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.views.properties.*;
import org.eclipse.ui.model.*;
import java.util.*;
import org.eclipse.update.internal.ui.*;
import java.io.*;
import org.eclipse.ui.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.custom.BusyIndicator;

public class MyComputerDirectory
	extends ModelObject
	implements IWorkbenchAdapter {
	private ModelObject parent;
	private File file;
	private boolean root;
	Object[] children;

	public MyComputerDirectory(ModelObject parent, File file, boolean root) {
		this.parent = parent;
		this.file = file;
		this.root = root;
	}

	public MyComputerDirectory(ModelObject parent, File file) {
		this(parent, file, false);
	}

	public Object getAdapter(Class adapter) {
		if (adapter.equals(IWorkbenchAdapter.class)) {
			return this;
		}
		return super.getAdapter(adapter);
	}

	public String getName() {
		return root ? file.getPath() : file.getName();
	}

	public String toString() {
		return getName();
	}

	public boolean hasChildren(Object parent) {
		if (file.isDirectory()) {
			final boolean [] result = new boolean[1];
			BusyIndicator.showWhile(UpdateUIPlugin.getActiveWorkbenchShell().getDisplay(), new Runnable() {
				public void run() {
					File[] children = file.listFiles();
					result[0] = children != null && children.length > 0;
				}
			});
			return result[0];
		}
		return false;
	}

	/**
	 * @see IWorkbenchAdapter#getChildren(Object)
	 */

	public Object[] getChildren(Object parent) {
		BusyIndicator
			.showWhile(
				UpdateUIPlugin.getActiveWorkbenchShell().getDisplay(),
				new Runnable() {
			public void run() {
				File[] files = file.listFiles();

				children = new Object[files.length];
				for (int i = 0; i < files.length; i++) {
					File file = files[i];

					if (file.isDirectory()) {
						/*
						SiteBookmark site = createSite(file);
						if (site != null)
							children[i] = site;
						else
						*/
					    children[i] = new MyComputerDirectory(MyComputerDirectory.this, file);
					} else {
						children[i] = new MyComputerFile(MyComputerDirectory.this, file);
					}
				}
			}
		});
		return children;
	}

	private SiteBookmark createSite(File file) {
		try {
			URL url = new URL("file:" + file.getAbsolutePath() + File.separator);
			SiteBookmark site = new SiteBookmark(file.getName(), url);
			site.connect();
			return site;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * @see IWorkbenchAdapter#getImageDescriptor(Object)
	 */
	public ImageDescriptor getImageDescriptor(Object obj) {
		return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
			ISharedImages.IMG_OBJ_FOLDER);
	}

	public Image getImage(Object obj) {
		return PlatformUI.getWorkbench().getSharedImages().getImage(
			ISharedImages.IMG_OBJ_FOLDER);
	}

	/**
	 * @see IWorkbenchAdapter#getLabel(Object)
	 */
	public String getLabel(Object obj) {
		return getName();
	}

	/**
	 * @see IWorkbenchAdapter#getParent(Object)
	 */
	public Object getParent(Object arg0) {
		return parent;
	}
}