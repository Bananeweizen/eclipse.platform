package org.eclipse.update.core;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
import org.eclipse.update.core.model.ImportModel;
import org.eclipse.update.internal.core.UpdateCore;
import org.eclipse.update.internal.core.UpdateManagerUtils;

/**
 * Convenience implementation of a plug-in dependency.
 * <p>
 * This class may be instantiated or subclassed by clients.
 * </p> 
 * @see org.eclipse.update.core.IImport
 * @see org.eclipse.update.core.model.ImportModel
 * @since 2.0
 */
public class Import extends ImportModel implements IImport {

	//PERF: new instance variable
	private VersionedIdentifier versionId;

	/**
	 * Returns an identifier of the dependent plug-in.
	 * @see IImport#getIdentifier()
	 */
	public VersionedIdentifier getVersionedIdentifier() {
		if (versionId != null)
			return versionId;

		String id = getIdentifier();
		String ver = getVersion();
		if (id != null && ver != null) {
			try {
				versionId = new VersionedIdentifier(id, ver);
				return versionId;
			} catch (Exception e) {
				UpdateCore.warn("Unable to create versioned identifier:" + id + ":" + ver);
			}
		}

		
		versionId = new VersionedIdentifier("",null);
		return versionId;		
	}

	/**
	 * Returns the matching rule for the dependency.
	 * @see IImport#getRule()
	 */
	public int getRule() {
		return UpdateManagerUtils.getMatchingRule(getMatchingRuleName());
	}
	
	/**
	 * Returns the matching rule for the dependency identifier.
	 * @see IImport#getIdRule()
	 */
	public int getIdRule() {
		return UpdateManagerUtils.getMatchingIdRule(getMatchingIdRuleName());
	}
	
	/**
	 * 
	 * @see org.eclipse.update.core.IImport#getKind()
	 */

	/**
	 * Returns the dependency kind
	 * @see org.eclipse.update.core.IImport#getKind()
	 */
	public int getKind() {
		return isFeatureImport()?KIND_FEATURE:KIND_PLUGIN;
	}

}