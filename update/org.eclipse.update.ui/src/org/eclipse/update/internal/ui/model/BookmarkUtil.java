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
package org.eclipse.update.internal.ui.model;

import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.xerces.parsers.*;
import org.eclipse.core.runtime.*;
import org.eclipse.update.internal.ui.*;
import org.w3c.dom.*;
import org.xml.sax.*;

public class BookmarkUtil {
	public static void parse(String fileName, Vector bookmarks) {
		File file = new File(fileName);
		if (!file.exists())
			return;
		DOMParser parser = new DOMParser();
		try {
			parser.parse(fileName);
			Document doc = parser.getDocument();
			Node root = doc.getDocumentElement();
			processRoot(root, bookmarks);
		} catch (SAXException e) {
			UpdateUI.logException(e);
		} catch (IOException e) {
			UpdateUI.logException(e);
		}
	}

	public static SiteBookmark[] getBookmarks(Vector bookmarks) {
		ArrayList result = new ArrayList();
		for (int i = 0; i < bookmarks.size(); i++) {
			processEntry(bookmarks.get(i), result);
		}
		return (SiteBookmark[]) result.toArray(new SiteBookmark[result.size()]);
	}

	public static BookmarkFolder getFolder(Vector bookmarks, IPath path) {
		NamedModelObject object = find(bookmarks, path);
		if (object != null && object instanceof BookmarkFolder)
			return (BookmarkFolder) object;
		return null;
	}

	public static NamedModelObject find(Vector bookmarks, IPath path) {
		Object[] array = bookmarks.toArray();
		return find(array, path);
	}

	private static NamedModelObject find(Object[] array, IPath path) {
		String name = path.segment(0);
		for (int i = 0; i < array.length; i++) {
			NamedModelObject obj = (NamedModelObject) array[i];
			if (obj.getName().equals(name)) {
				if (obj instanceof BookmarkFolder) {
					if (path.segmentCount() > 1) {
						IPath childPath = path.removeFirstSegments(1);
						BookmarkFolder folder = (BookmarkFolder) obj;
						return find(folder.getChildren(null), childPath);
					}
				}
				return obj;
			}
		}
		return null;
	}

	private static void processRoot(Node root, Vector bookmarks) {
		if (root.getNodeName().equals("bookmarks")) {
			NodeList children = root.getChildNodes();
			processChildren(children, null, bookmarks);
		}
	}
	private static void processChildren(
		NodeList children,
		BookmarkFolder folder,
		Vector bookmarks) {
		UpdateModel model = UpdateUI.getDefault().getUpdateModel();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			NamedModelObject object = null;
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				if (child.getNodeName().equals("site")) {
					object = createSite(child);

				} else if (child.getNodeName().equals("folder")) {
					object = createFolder(child);
				}
			}
			if (object != null) {
				if (folder != null) {
					folder.addChild(object);
				} else {
					bookmarks.add(object);
				}
				object.setModel(model);
			}
		}
	}

	private static SiteBookmark createSite(Node child) {
		String name = getAttribute(child, "name");
		URL url = null;
		try {
			url = new URL(getAttribute(child, "url"));
		} catch (MalformedURLException e) {
		}

		String web = getAttribute(child, "web");
		boolean webBookmark = (web != null && web.equals("true"));

		String sel = getAttribute(child, "selected");
		boolean selected = (sel != null && sel.equals("true"));

		SiteBookmark bookmark = new SiteBookmark(name, url, webBookmark, selected);

		String local = getAttribute(child, "local");
		bookmark.setLocal(local != null && local.equals("true"));

		String ign = getAttribute(child, "ignored-categories");
		if (ign != null) {
			StringTokenizer stok = new StringTokenizer(ign, ",");
			ArrayList array = new ArrayList();
			while (stok.hasMoreTokens()) {
				String tok = stok.nextToken();
				array.add(tok);
			}
			bookmark.setIgnoredCategories((String[]) array.toArray(new String[array.size()]));
		}
		return bookmark;
	}

	private static BookmarkFolder createFolder(Node child) {
		BookmarkFolder folder = new BookmarkFolder();
		String name = getAttribute(child, "name");
		folder.setName(name);
		if (child.hasChildNodes()) {
			NodeList children = child.getChildNodes();
			processChildren(children, folder, null);
		}
		return folder;
	}

	public static void store(String fileName, Vector bookmarks) {
		try {
			FileOutputStream fos = new FileOutputStream(fileName);
			OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF8");
			PrintWriter writer = new PrintWriter(osw);
			writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			writer.println("<bookmarks>");
			for (int i = 0; i < bookmarks.size(); i++) {
				Object obj = bookmarks.get(i);
				writeObject("   ", obj, writer);
			}
			writer.println("</bookmarks>");
			writer.flush();
			writer.close();
			osw.close();
			fos.close();
		} catch (IOException e) {
		}
	}
	private static void writeObject(
		String indent,
		Object obj,
		PrintWriter writer) {
		if (obj instanceof SiteBookmark) {
			SiteBookmark bookmark = (SiteBookmark) obj;
			String name = bookmark.getName();
			String url = bookmark.getURL().toString();
			String web = bookmark.isWebBookmark()?"true":"false";
			String sel = bookmark.isSelected()?"true":"false";
			String local = bookmark.isLocal() ? "true" : "false";
			String [] ign = bookmark.getIgnoredCategories();
			StringBuffer wign = new StringBuffer();
			for (int i = 0; i < ign.length; i++) {
				if (i > 0)
					wign.append(',');
				wign.append(ign[i]);
			}
			writer.print(indent + "<site name=\"" + name + "\" url=\"" + url + "\" web=\"" + web + "\" selected=\"" + sel + "\" local=\"" + local + "\"");
			if (wign.length() > 0)
				writer.print(" ignored-categories=\""+wign.toString()+"\"");
			writer.println("/>");
		} else if (obj instanceof BookmarkFolder) {
			BookmarkFolder folder = (BookmarkFolder) obj;
			String name = folder.getName();
			writer.println(indent + "<folder name=\"" + name + "\">");
			Object[] children = folder.getChildren(folder);
			String indent2 = indent + "   ";
			for (int i = 0; i < children.length; i++) {
				writeObject(indent2, children[i], writer);
			}
			writer.println(indent + "</folder>");
		}
	}

	private static String getAttribute(Node node, String name) {
		NamedNodeMap atts = node.getAttributes();
		Node att = atts.getNamedItem(name);
		if (att != null) {
			return att.getNodeValue();
		}
		return "";
	}
	private static void processFolder(BookmarkFolder folder, ArrayList result) {
		Object[] children = folder.getChildren(folder);
		for (int i = 0; i < children.length; i++) {
			processEntry(children[i], result);
		}
	}
	private static void processEntry(Object obj, ArrayList result) {
		if (obj instanceof SiteBookmark)
			result.add(obj);
		else if (obj instanceof BookmarkFolder) {
			processFolder((BookmarkFolder) obj, result);
		}
	}
}