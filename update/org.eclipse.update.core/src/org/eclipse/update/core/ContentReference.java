package org.eclipse.update.core;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import java.io.*;
import java.net.URL;

import org.eclipse.update.internal.core.*;

/**
 * Content reference implements a general access wrapper 
 * to feature and site content. The reference specifies
 * a "symbolic" path identifier for the content, and the actual
 * reference as a file, or a URL.
 * <p>
 * This class may be instantiated or subclassed by clients.
 * </p> 
 * @see org.eclipse.update.core.JarContentReference
 * @see org.eclipse.update.core.JarEntryContentReference
 * @since 2.0
 */
public class ContentReference {

	/**
	 * Unknown size indication
	 * @since 2.0
	 */
	public static final long UNKNOWN_SIZE = -1;

	/**
	 * Default executable permission when installing a content reference
	 * Will add executable bit if necessary
	 * 
	 * @since 2.0.1
	 */
	public static final int DEFAULT_EXECUTABLE_PERMISSION = -1;

	private static final String FILE_URL_PROTOCOL = "file"; //$NON-NLS-1$

	private String id;
	private URL url; // reference is either URL reference *OR*
	private File file; //    local file reference
	private Response response;
	private int permission; 
	
	// <true> if a copy of a Contentreferenec in a temp local directory
	private boolean tempLocal = false;

	/*
	 * do not allow default contruction
	 */
	private ContentReference() {
	}

	/**
	 * Create content reference from URL.
	 * 
	 * @param id "symbolic" path identifier
	 * @param url actual referenced URL
	 * @since 2.0
	 */
	public ContentReference(String id, URL url) {
		this.id = (id == null ? "" : id); //$NON-NLS-1$
		this.url = url; // can be null
		this.file = null;
	}

	/**
	 * Create content reference from file.
	 * 
	 * @param id "symbolic" path identifier
	 * @param file actual referenced file
	 * @since 2.0
	 */
	public ContentReference(String id, File file) {
		this.id = (id == null ? "" : id); //$NON-NLS-1$
		this.file = file; // can be null
		this.url = null;
	}

	/**
	 * A factory method to create a content reference of
	 * the same type.
	 * 
	 * @param id "symbolic" path identifier
	 * @param file actual referenced file
	 * @return content reference of the same type
	 * @since 2.0
	 */
	public ContentReference createContentReference(String id, File file) {
		return new ContentReference(id, file,true);
	}
	/**
	 * 
	 */
	private ContentReference(String id, File file, boolean b) {
		this(id,file);
		setTempLocal(b);
	}

	/**
	 * Retrieves the "symbolic" path identifier for the reference.
	 * 
	 * @return "symbolic" path identifier
	 * @since 2.0
	 */
	public String getIdentifier() {
		return id;
	}

	/**
	 * Creates an input stream for the reference.
	 * 
	 * @return input stream
	 * @exception IOException unable to create stream
	 * @since 2.0
	 */
	public InputStream getInputStream() throws IOException {
		if (file != null)
			return new FileInputStream(file);
		else if (url != null) {
			if (response == null) {
				URL resolvedURL = URLEncoder.encode(url);
				response = UpdateManagerPlugin.getPlugin().get(resolvedURL);
				UpdateManagerUtils.checkConnectionResult(response,resolvedURL);
			}
			return response.getInputStream();
		} else
			throw new IOException(Policy.bind("ContentReference.UnableToCreateInputStream", this.toString())); //$NON-NLS-1$
	}

	/**
	 * Returns the size of the referenced input, if it can be determined.
	 * 
	 * @return input size, or @see #UNKNOWN_SIZE if size cannot be determined.
	 * @since 2.0
	 */
	public long getInputSize() throws IOException {
		if (file != null)
			return file.length();
		else if (url != null) {
			if (response == null) {
				URL resolvedURL = null;
				try {
					resolvedURL = URLEncoder.encode(url);
					response = UpdateManagerPlugin.getPlugin().get(resolvedURL);
				} catch (IOException e) {
					return ContentReference.UNKNOWN_SIZE;
				}
				UpdateManagerUtils.checkConnectionResult(response,resolvedURL);			
			}
			long size = response.getContentLength();
			return size == -1 ? ContentReference.UNKNOWN_SIZE : size;
		} else
			return ContentReference.UNKNOWN_SIZE;
	}

	/**
	 * Indicates whether the reference is a local file reference.
	 * 
	 * @return <code>true</code> if the reference is local, 
	 * otherwise <code>false</code>
	 * @since 2.0
	 */
	public boolean isLocalReference() {
		/*if (file != null)
			return true;
		else if (url != null)
			return FILE_URL_PROTOCOL.equals(url.getProtocol());
		else
			return false;*/
		// only temp files are considered local
		return tempLocal;
	}

	/**
	 * Returns the content reference as a file. Note, that this method
	 * <b>does not</b> cause the file to be downloaded if it
	 * is not already local.
	 * 
	 * @return reference as file
	 * @exception IOException reference cannot be returned as file
	 * @since 2.0
	 */
	public File asFile() throws IOException {
		if (file != null)
			return file;

		if (url != null && FILE_URL_PROTOCOL.equals(url.getProtocol())) {
			File result = new File(url.getFile());
			if (result.exists())
				return result;
			else 
				throw new IOException(Policy.bind("ContentReference.FileDoesNotExist", this.toString())); //$NON-NLS-1$ 			
		}

		throw new IOException(Policy.bind("ContentReference.UnableToReturnReferenceAsFile", this.toString())); //$NON-NLS-1$ 
	}

	/**
	 * Returns the content reference as a URL.
	 * 
	 * @return reference as URL
	 * @exception IOException reference cannot be returned as URL
	 * @since 2.0
	 */
	public URL asURL() throws IOException {
		if (url != null)
			return url;

		if (file != null)
			return file.toURL();

		throw new IOException(Policy.bind("ContentReference.UnableToReturnReferenceAsURL", this.toString())); //$NON-NLS-1$
	}

	/**
	 * Return string representation of this reference.
	 * 
	 * @return string representation
	 * @since 2.0
	 */
	public String toString() {
		if (file != null)
			return file.getAbsolutePath();
		else
			return url.toExternalForm();
	}
	/**
	 * Returns the permission for this file.
	 * 
	 * @return the content reference permission
	 * @see DEFAULT_PERMISSION
	 * @since 2.0.1
	 */
	public int getPermission() {
		return permission;
	}

	/**
	 * Sets the permission of this content reference.
	 * 
	 * @param permission The permission to set
	 */
	public void setPermission(int permission) {
		this.permission = permission;
	}

	/**
	 * Sets if a content reference is considered local 
	 * 
	 * @param tempLocal <code>true</code> if the file is considered local
	 */
	protected void setTempLocal(boolean tempLocal) {
		this.tempLocal = tempLocal;
	}

}