package org.eclipse.update.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.*;
import org.eclipse.update.core.*;
import org.eclipse.update.core.model.*;
import org.xml.sax.SAXException;

public class SiteFileFactory extends BaseSiteFactory {

	// private when parsing file system
	private Site site;

	/**
	 * manages the versionedIdentifier and location of parsed plugins
	 */
	public class PluginIdentifier {
		private VersionedIdentifier id;
		private File location;
		public PluginIdentifier(VersionedIdentifier id, File location) {
			this.id = id;
			this.location = location;
		}

		public String getIdentifier() {
			if (id != null)
				return id.getIdentifier();
			return null;
		}

		public Version getVersion() {
			if (id != null)
				return id.getVersion();
			return null;
		}

		public File getLocation() {
			return location;
		}

		public String toString() {
			if (id != null)
				return id.toString();
			return "";
		}
	}

	/*
	 * @see ISiteFactory#createSite(URL,boolean)
	 */
	public ISite createSite(URL url, boolean forceCreation) throws IOException, ParsingException, InvalidSiteTypeException {

		Site site = null;
		InputStream siteStream = null;
		SiteModelFactory factory = (SiteModelFactory) this;

		try {
			// if url points to a directory
			// attempt to parse site.xml
			String path = url.getFile();
			File siteLocation = new File(path);
			if (siteLocation.isDirectory()) {
				if (new File(siteLocation, Site.SITE_XML).exists()) {
					siteStream = new FileInputStream(new File(siteLocation, Site.SITE_XML));
					site = (Site) factory.parseSite(siteStream);
				} else {
					if (forceCreation) {
						// parse siteLocation
						site = parseSite(siteLocation);
					} else {
						throw new IOException("Cannot retrieve a SiteFile from a directory. The URL should point to a file.");
					}
				}
			} else {
				 // we are not pointing to a directory
				 // attempt to parse the file
				try {
					URL resolvedURL = URLEncoder.encode(url);
					siteStream = resolvedURL.openStream();
					site = (Site) factory.parseSite(siteStream);
				} catch (IOException e) {
					if (forceCreation) {
						// attempt to parse parent directory
						File file = new File(url.getFile());	
						File parentDirectory =file.getParentFile();
						
						// create directory if it doesn't exist						
						if (parentDirectory!=null && !parentDirectory.exists()){
								parentDirectory.mkdirs();
						}
						
						if (parentDirectory==null || !parentDirectory.isDirectory())
							throw new ParsingException(new Exception("Cannot obtain the parent directory from the file:"+file));


						site = parseSite(parentDirectory);
					} else
						throw e;
				}
			}

			SiteContentProvider contentProvider = new SiteFileContentProvider(url);
			site.setSiteContentProvider(contentProvider);
			contentProvider.setSite(site);
			site.resolve(url, getResourceBundle(url));
			site.markReadOnly();

		} finally {
			try {
				// FIXME why do we need to check ???
				if (siteStream != null)
					siteStream.close();
			} catch (Exception e) {
			}
		}
		return site;
	}
	/**
	 * Method parseSite.
	 */
	public Site parseSite(File directory) throws ParsingException {

		this.site = (Site) createSiteMapModel();

		if (!directory.exists())
			throw new ParsingException(new Exception("The file:"+directory+" does not exist. Cannot parse site"));

		File pluginPath = new File(directory, Site.DEFAULT_PLUGIN_PATH);
		File fragmentPath = new File(directory,Site.DEFAULT_FRAGMENT_PATH);

		try {
			// FIXME: fragments
			//PACKAGED
			parsePackagedFeature(directory); // in case it contains JAR files

			parsePackagedPlugins(pluginPath);

			parsePackagedPlugins(fragmentPath);

			// EXECUTABLE	
			parseExecutableFeature(directory);

			parseExecutablePlugin(pluginPath);

			parseExecutablePlugin(fragmentPath);

		} catch (CoreException e) {
			throw new ParsingException(e.getStatus().getException());
		}

		return (Site) site;

	}

	/**
	 * Method parseFeature.
	 * @throws CoreException
	 */
	private void parseExecutableFeature(File directory) throws CoreException {

		File featureDir = new File(directory, Site.INSTALL_FEATURE_PATH);
		if (featureDir.exists()) {
			String[] dir;
			FeatureReferenceModel featureRef;
			URL featureURL;
			String newFilePath = null;

			try {
				// handle the installed featuresConfigured under featuresConfigured subdirectory
				dir = featureDir.list();
				for (int index = 0; index < dir.length; index++) {

					SiteFileFactory archiveFactory = new SiteFileFactory();
					// the URL must ends with '/' for the bundle to be resolved
					newFilePath = dir[index] + (dir[index].endsWith("/") ? "/" : "");
					featureURL = new File(featureDir,newFilePath).toURL();
					IFeature newFeature = createFeature(featureURL);

					if (newFeature!=null){
						featureRef = archiveFactory.createFeatureReferenceModel();
						featureRef.setSiteModel(site);
						featureRef.setURLString(featureURL.toExternalForm());
						((Site) site).addFeatureReferenceModel(featureRef);
					}
				}
			} catch (MalformedURLException e) {
				String id = UpdateManagerPlugin.getPlugin().getDescriptor().getUniqueIdentifier();
				IStatus status = new Status(IStatus.ERROR, id, IStatus.OK, "Error creating file URL for:" + newFilePath, e);
				throw new CoreException(status);
			}
		}
	}

	/**
	* Method parseFeature.
	* @throws CoreException
	*/
	private void parsePackagedFeature(File directory) throws CoreException {

		// FEATURES
		File featureDir = new File(directory, Site.DEFAULT_FEATURE_PATH);
		if (featureDir.exists()) {
			String[] dir;
			FeatureReferenceModel featureRef;
			URL featureURL;
			String newFilePath = null;

			try {
				// handle the installed featuresConfigured under featuresConfigured subdirectory
				dir = featureDir.list(FeaturePackagedContentProvider.filter);
				for (int index = 0; index < dir.length; index++) {

					featureURL = new File(featureDir, dir[index]).toURL();
					IFeature newFeature = createFeature(featureURL);

					SiteFileFactory archiveFactory = new SiteFileFactory();
					featureRef = archiveFactory.createFeatureReferenceModel();
					featureRef.setSiteModel(site);
					featureRef.setURLString(featureURL.toExternalForm());
					site.addFeatureReferenceModel(featureRef);

				}
			} catch (MalformedURLException e) {
				String id = UpdateManagerPlugin.getPlugin().getDescriptor().getUniqueIdentifier();
				IStatus status = new Status(IStatus.ERROR, id, IStatus.OK, "Error creating file URL for:" + newFilePath, e);
				throw new CoreException(status);
			}
		}
	}

	/**
	 * Method parsePlugins.
	 * 
	 * look into each plugin/fragment directory, crack the plugin.xml open (or fragment.xml ???)
	 * get id and version, calculate URL...	
	 * 
	 * @return VersionedIdentifier
	 * @throws CoreException
	 */
	private void parseExecutablePlugin(File dir) throws CoreException {
		PluginIdentifier plugin = null;
		String id = UpdateManagerPlugin.getPlugin().getDescriptor().getUniqueIdentifier();
		MultiStatus parsingStatus = new MultiStatus(id, IStatus.WARNING, "Error parsing plugin.xml in " + dir, new Exception());

		try {
			if (dir.exists() && dir.isDirectory()) {
				File[] files = dir.listFiles();
				for (int i = 0; i < files.length; i++) {
					if (files[i].isDirectory()) {
						File pluginFile = null;

						if (!(pluginFile = new File(files[i], "plugin.xml")).exists()) {
							pluginFile = new File(files[i], "fragment.xml");
						}

						if (pluginFile != null && pluginFile.exists()) {
							IPluginEntry entry = new DefaultPluginParser().parse(new FileInputStream(pluginFile));
							VersionedIdentifier identifier = entry.getVersionIdentifier();
							plugin = new PluginIdentifier(identifier, pluginFile);

							addParsedPlugin(plugin);

						}
					} // files[i] is a directory
				}
			} // path is a directory
		} catch (Exception e) {
			IStatus status = new Status(IStatus.ERROR, id, IStatus.OK, "Error parsing file :" + dir + " \r\n" + e.getMessage(), e);
			throw new CoreException(status);
		}

	}

	/**
	 * Method addParsedPlugins.
	 * @param model
	 * @throws CoreException
	 */
	private void addParsedPlugin(PluginIdentifier plugin) throws CoreException {

		String id = UpdateManagerPlugin.getPlugin().getDescriptor().getUniqueIdentifier();

		// tranform each Plugin and Fragment in an Archive for the Site
		// and a pluginEntry
		String location = null;
		try {
			if (plugin != null) {
				URLEntry info;
				PluginEntry entry = new PluginEntry();
				entry.setContainer(site);
				entry.setPluginIdentifier(plugin.getIdentifier());
				entry.setPluginVersion(plugin.getVersion().toString());
				((Site) site).addPluginEntry(entry);

				SiteFileFactory archiveFactory = new SiteFileFactory();
				// the id is plugins\<pluginid>_<ver>.jar as per the specs
				ArchiveReferenceModel archive = archiveFactory.createArchiveReferenceModel();
				String pluginID = Site.DEFAULT_PLUGIN_PATH + plugin.toString() + FeaturePackagedContentProvider.JAR_EXTENSION;
				archive.setPath(pluginID);
				location = plugin.getLocation().toURL().toExternalForm();
				URL url = new URL(location);
				archive.setURLString(url.toExternalForm());
				((Site) site).addArchiveReferenceModel(archive);
			}
		} catch (MalformedURLException e) {
			IStatus status = new Status(IStatus.ERROR, id, IStatus.OK, "Error creating file URL for plugin:" + location, e);
			throw new CoreException(status);
		}
	}

	/**
	 * 
	 */
	private void parsePackagedPlugins(File pluginDir) throws CoreException {

		File file = null;
		String[] dir;

		String id = UpdateManagerPlugin.getPlugin().getDescriptor().getUniqueIdentifier();
		PluginIdentifier plugin;
		MultiStatus parsingStatus = new MultiStatus(id, IStatus.WARNING, "Error parsing plugin.xml", new Exception());

		try {
			if (pluginDir.exists()) {
				dir = pluginDir.list(FeaturePackagedContentProvider.filter);
				for (int i = 0; i < dir.length; i++) {

					file = new File(pluginDir, dir[i]);
					JarContentReference jarReference = new JarContentReference(null, file);
					ContentReference ref = jarReference.peek("plugin.xml", null, null);
					if (ref == null)
						jarReference.peek("fragment.xml", null, null);

					if (ref != null) {
						IPluginEntry entry = new DefaultPluginParser().parse(ref.getInputStream());
						VersionedIdentifier identifier = entry.getVersionIdentifier();
						plugin = new PluginIdentifier(identifier, file);
						addParsedPlugin(plugin);
					} //ref!=null
				} //for
			}

		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, id, IStatus.OK, "Error accessing plugin.xml in file :" + file, e));
		}

	}

	/**
	 * 
	 */
	private IFeature createFeature(URL url) throws CoreException {
		FeatureReference ref = new FeatureReference();
		ref.setSite(site);
		ref.setURL(url);
		return ref.getFeature();
	}

	/*
	 * @see SiteModelFactory#createSiteMapModel()
	 */
	public SiteMapModel createSiteMapModel() {
		return new SiteFile();
	}

	/*
	 * @see SiteModelFactory#canParseSiteType(String)
	 */
	public boolean canParseSiteType(String type) {
		return (super.canParseSiteType(type) || SiteFileContentProvider.SITE_TYPE.equalsIgnoreCase(type));
	}

}