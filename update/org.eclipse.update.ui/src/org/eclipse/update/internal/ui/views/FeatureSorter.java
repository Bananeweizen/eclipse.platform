package org.eclipse.update.internal.ui.views;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.PluginVersionIdentifier;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.update.core.IFeature;
import org.eclipse.update.core.VersionedIdentifier;
import org.eclipse.update.internal.ui.model.IFeatureAdapter;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class FeatureSorter extends ViewerSorter {

	public int compare(Viewer viewer, Object e1, Object e2) {

		if (!(e1 instanceof IFeatureAdapter && e2 instanceof IFeatureAdapter))
			return super.compare(viewer, e1, e2);

		IFeatureAdapter a1 = (IFeatureAdapter) e1;
		IFeatureAdapter a2 = (IFeatureAdapter) e2;

		IFeature f1, f2;

		try {
			f1 = a1.getFeature();
			f2 = a2.getFeature();
		} catch (CoreException e) {
			return super.compare(viewer, e1, e2);
		}

		VersionedIdentifier vid1 = f1.getVersionedIdentifier();
		VersionedIdentifier vid2 = f2.getVersionedIdentifier();

		String name1;
		String name2;

		name1 = f1.getLabel();
		name2 = f2.getLabel();

		if (name1 == null)
			name1 = ""; //$NON-NLS-1$
		if (name2 == null)
			name2 = ""; //$NON-NLS-1$
		int result = collator.compare(name1, name2);
		if (result != 0)
			return result;
		// Compare versions
		PluginVersionIdentifier v1 = vid1.getVersion();
		PluginVersionIdentifier v2 = vid2.getVersion();
		if (v1.equals(v2))
			return 0;
		if (v2.isGreaterThan(v1))
			return -1;
		return 1;
	}
}
