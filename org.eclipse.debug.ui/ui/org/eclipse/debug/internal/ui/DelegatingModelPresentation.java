package org.eclipse.debug.internal.ui;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugConstants;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.ILauncher;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.ITerminate;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * A model presentation that delegates to the appropriate extension. This
 * presentation contains a table of specialized presentations that are defined
 * as <code>org.eclipse.debug.ui.debugModelPresentations</code> extensions. When
 * asked to render an object from a debug model, this presentation delegates
 * to the extension registered for that debug model. 
 */
public class DelegatingModelPresentation implements IDebugModelPresentation {
	
	/**
	 * A mapping of attribute ids to their values
	 * @see IDebugModelPresentation#setAttribute
	 */
	private HashMap fAttributes= new HashMap(3);
	/**
	 * A table of label providers keyed by debug model identifiers.
	 */
	private HashMap fLabelProviders= new HashMap(5);

	/**
	 * Constructs a new DelegatingLabelProvider that delegates to extensions
	 * of kind <code>org.eclipse.debug.ui.debugLabelProvider</code>
	 */
	public DelegatingModelPresentation() {
		IPluginDescriptor descriptor= DebugUIPlugin.getDefault().getDescriptor();
		IExtensionPoint point= descriptor.getExtensionPoint(IDebugUIConstants.ID_DEBUG_MODEL_PRESENTATION);
		if (point != null) {
			IExtension[] extensions= point.getExtensions();
			for (int i= 0; i < extensions.length; i++) {
				IExtension extension= extensions[i];
				IConfigurationElement[] configElements= extension.getConfigurationElements();
				for (int j= 0; j < configElements.length; j++) {
					IConfigurationElement elt= configElements[j];
					String id= elt.getAttribute("id"); //$NON-NLS-1$
					if (id != null) {
						IDebugModelPresentation lp= new LazyModelPresentation(elt);
						getLabelProviders().put(id, lp);
					}
				}
			}
		}
	}

	/**
	 * Delegate to all extensions.
	 *
	 * @see IBaseLabelProvider#addListener(ILabelProviderListener)
	 */
	public void addListener(ILabelProviderListener listener) {
		Iterator i= getLabelProviders().values().iterator();
		while (i.hasNext()) {
			((ILabelProvider) i.next()).addListener(listener);
		}
	}

	/**
	 * Delegate to all extensions.
	 *
	 * @see IBaseLabelProvider#dispose()
	 */
	public void dispose() {
		Iterator i= getLabelProviders().values().iterator();
		while (i.hasNext()) {
			((ILabelProvider) i.next()).dispose();
		}
	}

	/**
	 * @see IDebugModelPresentation#getImage(Object)
	 */
	public Image getImage(Object item) {
		if (item instanceof IDebugElement || item instanceof IMarker || item instanceof IBreakpoint) {
			IDebugModelPresentation lp= getConfiguredPresentation(item);
			if (lp != null) {
				Image image= lp.getImage(item);
				if (image != null) {
					return image;
				}
			}
			// default to show the simple element name
			return getDefaultImage(item);
		} else {
			ImageRegistry iRegistry= DebugUIPlugin.getDefault().getImageRegistry();
			if (item instanceof IProcess) {
				if (((IProcess) item).isTerminated()) {
					return iRegistry.get(IDebugUIConstants.IMG_OBJS_OS_PROCESS_TERMINATED);
				} else {
					return iRegistry.get(IDebugUIConstants.IMG_OBJS_OS_PROCESS);
				}
			} else
				if (item instanceof ILauncher) {
					return getLauncherImage((ILauncher)item);
				} else
					if (item instanceof ILaunch) {
						ILaunch launch = (ILaunch) item;
						String mode= launch.getLaunchMode();
						if (mode.equals(ILaunchManager.DEBUG_MODE)) {
							return iRegistry.get(IDebugUIConstants.IMG_ACT_DEBUG);
						} else {
							return iRegistry.get(IDebugUIConstants.IMG_ACT_RUN);
						}
					} else
						if (item instanceof InspectItem) {
							return iRegistry.get(IDebugUIConstants.IMG_OBJS_EXPRESSION);
						} else
							if (item instanceof IAdaptable) {
								IWorkbenchAdapter de= (IWorkbenchAdapter) ((IAdaptable) item).getAdapter(IWorkbenchAdapter.class);
								if (de != null) {
									ImageDescriptor descriptor= de.getImageDescriptor(item);
									if( descriptor != null) {
										return descriptor.createImage();
									}
								}
							}
	
			return null;

		}
	}

	protected Image getLauncherImage(ILauncher launcher) {
		return DebugPluginImages.getImage(launcher.getIdentifier());
	}
	
	/**
	 * @see IDebugModelPresentation#getEditorInput(Object)
	 */
	public IEditorInput getEditorInput(Object item) {
		IDebugModelPresentation lp= getConfiguredPresentation(item);
		if (lp != null) {
			return lp.getEditorInput(item);
		}
		return null;
	}
	
	/**
	 * @see IDebugModelPresentation#getEditorId(IEditorInput, Object)
	 */
	public String getEditorId(IEditorInput input, Object objectInput) {
		IDebugModelPresentation lp= getConfiguredPresentation(objectInput);
		if (lp != null) {
			return lp.getEditorId(input, objectInput);
		}
		return null;
	}


	/**
	 * Returns a default image for the debug element
	 */
	protected String getDefaultText(Object element) {
		if (element instanceof IDebugElement) {
			try {
				switch (((IDebugElement) element).getElementType()) {
					case IDebugElement.DEBUG_TARGET:
						return ((IDebugTarget)element).getName();
					case IDebugElement.THREAD:
						return ((IThread)element).getName();
					case IDebugElement.STACK_FRAME:
						return ((IStackFrame)element).getName();
					case IDebugElement.VARIABLE:
						return ((IVariable)element).getName();
					default:
						return ""; //$NON-NLS-1$
				}
			} catch (DebugException de) {
				DebugUIPlugin.logError(de);
			}
		} else
			if (element instanceof IMarker) {
				IMarker m= (IMarker) element;
				try {
					if (m.exists() && m.isSubtypeOf(IDebugConstants.BREAKPOINT_MARKER)) {
						return DebugUIMessages.getString("DelegatingModelPresentation.Breakpoint_3"); //$NON-NLS-1$
					}
				} catch (CoreException e) {
					DebugUIPlugin.logError(e);
				}
			}
		return DebugUIMessages.getString("DelegatingModelPresentation.<unknown>_4"); //$NON-NLS-1$
	}

	/**
	 * Returns a default image for the debug element
	 */
	protected Image getDefaultImage(Object element) {
		ImageRegistry iRegistry= DebugUIPlugin.getDefault().getImageRegistry();
		if (element instanceof IThread) {
			IThread thread = (IThread)element;
			if (thread.isSuspended()) {
				return iRegistry.get(IDebugUIConstants.IMG_OBJS_THREAD_SUSPENDED);
			} else if (thread.isTerminated()) {
				return iRegistry.get(IDebugUIConstants.IMG_OBJS_THREAD_TERMINATED);
			} else {
				return iRegistry.get(IDebugUIConstants.IMG_OBJS_THREAD_RUNNING);
			}
		} else
			if (element instanceof IStackFrame) {
				return iRegistry.get(IDebugUIConstants.IMG_OBJS_STACKFRAME);
			} else
				if (element instanceof IProcess) {
					if (((IProcess) element).isTerminated()) {
						return iRegistry.get(IDebugUIConstants.IMG_OBJS_OS_PROCESS_TERMINATED);
					} else {
						return iRegistry.get(IDebugUIConstants.IMG_OBJS_OS_PROCESS);
					}
				} else
					if (element instanceof IDebugTarget) {
						IDebugTarget target= (IDebugTarget) element;
						if (target.isTerminated() || target.isDisconnected()) {
							return iRegistry.get(IDebugUIConstants.IMG_OBJS_DEBUG_TARGET_TERMINATED);
						} else {
							return iRegistry.get(IDebugUIConstants.IMG_OBJS_DEBUG_TARGET);
						}
					} else
						if (element instanceof IMarker) {
							try {
								IMarker marker= (IMarker) element;
								IBreakpoint breakpoint= DebugPlugin.getDefault().getBreakpointManager().getBreakpoint(marker);
								if (breakpoint != null && marker.exists()) {
									if (breakpoint.isEnabled()) {
										return DebugPluginImages.getImage(IDebugUIConstants.IMG_OBJS_BREAKPOINT);
									} else {
										return DebugPluginImages.getImage(IDebugUIConstants.IMG_OBJS_BREAKPOINT_DISABLED);
									}
								}
							} catch (CoreException e) {
								DebugUIPlugin.logError(e);
							}
						}
		return null;
	}

	/**
	 * @see IDebugModelPresentation#getText(Object)
	 */
	public String getText(Object item) {
		boolean displayVariableTypes= showVariableTypeNames();
		if (item instanceof InspectItem) {
			return getInspectItemText((InspectItem)item);
		} else if (item instanceof IDebugElement || item instanceof IMarker || item instanceof IBreakpoint) { 
			IDebugModelPresentation lp= getConfiguredPresentation(item);
			if (lp != null) {
				String label= lp.getText(item);
				if (label != null) {
					return label;
				}
			}
			if (item instanceof IVariable) {
				IVariable var= (IVariable) item;
				StringBuffer buf= new StringBuffer();
				try {
					IValue value = var.getValue();
					
					if (displayVariableTypes) {
						buf.append(value.getReferenceTypeName());
						buf.append(' ');
					}
					buf.append(var.getName());
					buf.append(" = "); //$NON-NLS-1$
					buf.append(value.getValueString());
					return buf.toString();
				} catch (DebugException de) {
				}
			}
			// default to show the simple element name
			return getDefaultText(item);
		} else {

			String label= null;
			if (item instanceof IProcess) {
				label= ((IProcess) item).getLabel();
			} else
				if (item instanceof ILauncher) {
					label = ((ILauncher)item).getLabel();
				} else
					if (item instanceof ILaunch) {
						label= getLaunchText((ILaunch) item);
					} else if (item instanceof InspectItem) {
						try {
							InspectItem var= (InspectItem) item;
							StringBuffer buf= new StringBuffer();
							buf.append(var.getLabel());
							buf.append(" = "); //$NON-NLS-1$
							IValue value = var.getValue();
							if (displayVariableTypes) {
								buf.append(value.getReferenceTypeName());
								buf.append(' ');
							}
							buf.append(value.getValueString());
							return buf.toString();
						} catch (DebugException de) {
							return getDefaultText(item);
						}
					} else {
						label= getDesktopLabel(item);
					}

			if ((item instanceof ITerminate) && ((ITerminate) item).isTerminated()) {
				label= DebugUIMessages.getString("DelegatingModelPresentation.<terminated>__7") + label; //$NON-NLS-1$
			}
			return label;
		}
	}
	
	/**
	 * @see IDebugModelPresentation#getDetail(IValue)
	 */
	public String getDetail(IValue value) {
		IDebugModelPresentation lp= getConfiguredPresentation(value);
		if (lp != null) {
			String detail= lp.getDetail(value);
			if (detail != null) {
				return detail;
			} else {
				detail = lp.getText(value);
			}
			if (detail != null) {
				return detail;
			}
		}
		return getText(value);
	}	

	/**
	 * InspectItems have their left halves rendered here, and their
	 * right halves rendered by the registered model presentation.
	 */
	protected String getInspectItemText(InspectItem inspectItem) {
		StringBuffer buffer= new StringBuffer(inspectItem.getLabel());
		String valueString= null;
		IDebugModelPresentation lp= getConfiguredPresentation(inspectItem);
		IValue value= inspectItem.getValue();
		if (lp != null) {
			valueString= lp.getText(value);
		} 
		if ((valueString == null) || (valueString.length() < 1)) {
			try {
				valueString= value.getValueString();
			} catch (DebugException de) {
			}
		}
		if (valueString != null && valueString.length() > 0) {
			buffer.append("= "); //$NON-NLS-1$
			buffer.append(valueString);		
		}
		return buffer.toString();
	}

	/**
	 * Delegate to all extensions.
	 *
	 * @see IBaseLabelProvider#removeListener(ILabelProviderListener)
	 */
	public void removeListener(ILabelProviderListener listener) {
		Iterator i= getLabelProviders().values().iterator();
		while (i.hasNext()) {
			((ILabelProvider) i.next()).removeListener(listener);
		}
	}

	public String getDesktopLabel(Object object) {
		if (object instanceof IAdaptable) {
			IWorkbenchAdapter de= (IWorkbenchAdapter) ((IAdaptable) object).getAdapter(IWorkbenchAdapter.class);
			if (de != null) {
				return de.getLabel(object);
			}
		}

		return DebugUIMessages.getString("DelegatingModelPresentation.<unknown>_9"); //$NON-NLS-1$
	}

	/**
	 * Delegate to the appropriate label provider.
	 *
	 * @see IBaseLabelProvider#isLabelProperty(Object, String)
	 */
	public boolean isLabelProperty(Object element, String property) {
		if (element instanceof IDebugElement) {
			IDebugModelPresentation lp= getConfiguredPresentation((IDebugElement) element);
			if (lp != null) {
				return lp.isLabelProperty(element, property);
			}
		}

		return true;
	}

	/**
	 * Returns a configured model presentation for the given object,
	 * or <code>null</code> if one is not registered.
	 */
	protected IDebugModelPresentation getConfiguredPresentation(Object element) {
		String id= null;
		if (element instanceof IDebugElement) {
			IDebugElement de= (IDebugElement) element;
			id= de.getModelIdentifier();
		} else if (element instanceof InspectItem) {
			IValue value= ((InspectItem)element).getValue();
			id= value.getModelIdentifier();
		} else if (element instanceof IMarker) {
			IMarker m= (IMarker) element;
			IBreakpoint bp = DebugPlugin.getDefault().getBreakpointManager().getBreakpoint(m);
			if (bp != null) {
				id= bp.getModelIdentifier();
			}
		} else if (element instanceof IBreakpoint) {
			id = ((IBreakpoint)element).getModelIdentifier();
		}
		if (id != null) {
			return getPresentation(id);
		}

		return null;
	}
	
	/**
	 * Returns the presentation registered for the given id, or <code>null</code>
	 * of nothing is registered for the id.
	 */
	protected IDebugModelPresentation getPresentation(String id) {
		IDebugModelPresentation lp= (IDebugModelPresentation) getLabelProviders().get(id);
		if (lp != null) {
			Iterator keys= getAttributes().keySet().iterator();
			while (keys.hasNext()) {
				String key= (String)keys.next();
				lp.setAttribute(key, getAttributes().get(key));
			}
			return lp;
		}
		return null;
	}

	/**
	 * Used to render launch history items in the re-launch drop downs
	 */
	protected String getLaunchText(ILaunch launch) {
		StringBuffer buff= new StringBuffer(getDesktopLabel(launch.getElement()));
		buff.append(" ["); //$NON-NLS-1$
		buff.append(getText(launch.getLauncher()));
		buff.append("]"); //$NON-NLS-1$
		return buff.toString();
	}
	
	/**
	 * @see IDebugModelPresentation
	 */
	public void setAttribute(String id, Object value) {
		if (value == null) {
			return;
		}
		getAttributes().put(id, value);
	}

	protected boolean showVariableTypeNames() {
		Boolean show= (Boolean) fAttributes.get(DISPLAY_VARIABLE_TYPE_NAMES);
		show= show == null ? new Boolean(false) : show;
		return show.booleanValue();
	}
	
	protected boolean showQualifiedNames() {
		Boolean show= (Boolean) fAttributes.get(DISPLAY_QUALIFIED_NAMES);
		show= show == null ? new Boolean(false) : show;
		return show.booleanValue();
	}
	
	protected HashMap getAttributes() {
		return fAttributes;
	}

	protected void setAttributes(HashMap attributes) {
		fAttributes = attributes;
	}

	protected HashMap getLabelProviders() {
		return fLabelProviders;
	}

	protected void setLabelProviders(HashMap labelProviders) {
		fLabelProviders = labelProviders;
	}
}

