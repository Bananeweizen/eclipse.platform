package org.eclipse.ant.internal.ui;/* * (c) Copyright IBM Corp. 2000, 2001. * All Rights Reserved. */import java.lang.reflect.InvocationTargetException;import java.util.*;import org.apache.tools.ant.Target;import org.eclipse.ant.core.*;import org.eclipse.core.resources.IFile;import org.eclipse.core.runtime.*;import org.eclipse.jface.dialogs.ErrorDialog;import org.eclipse.jface.operation.IRunnableWithProgress;import org.eclipse.jface.wizard.Wizard;import org.eclipse.ui.IPageService;import org.eclipse.ui.IViewPart;import org.eclipse.ui.IWorkbenchPage;import org.eclipse.ui.IWorkbenchWindow;import org.eclipse.ui.PartInitException;import org.eclipse.ui.PlatformUI;
/** * The wizard used to run an Ant script file written in xml. * <p> * Note: Currently there is only one page in this wizard. */
public class AntLaunchWizard extends Wizard {
	/**	 * The project described in the xml file.	 */	
	private EclipseProject project = null;	/**	 * The view that receives output logs from this plugin.	 */		private AntConsole console = null;		/**	 * The first page of the wizard.	 */
	private AntLaunchWizardPage page1 = null;		/**	 * The file that contains the Ant script.	 */
	private IFile antFile = null;
		/**	 * The string used to separate the previously selected target names in the persistent properties of the file.	 */
	private final static String SEPARATOR_TARGETS = "\"";		/**	 * The identifier of the target property.	 */
	private final static String PROPERTY_SELECTEDTARGETS = "selectedTargets";		/**	 * The identifier of the log property.	 */	private final static String PROPERTY_LOG = "ShowLogs";
	/**	 * The identifier of the arguments property.	 */	private final static String PROPERTY_ARGUMENTS = "Arguments";	/**	 * The code indicating an exception during ant script execution.	 */	private final static int EXCEPTION_ANT_EXECUTION = 0x01;		/**	 * Creates a new wizard, given the project described in the file and the file itself.	 * 	 * @param project	 * @param antFile	 */
	public AntLaunchWizard(EclipseProject project,IFile antFile) {
		super();
		this.project = project;
		this.antFile = antFile;		setWindowTitle(Policy.bind("wizard.title"));	}
	/**	 * Adds pages to the wizard and initialize them.	 * 	 */	
	public void addPages() {
		page1 = new AntLaunchWizardPage(project);		addPage(page1);
		page1.setInitialTargetSelections(getTargetNamesToPreselect());		page1.setInitialArguments(getInitialArguments());		page1.setDisplayLog(getShouldLogMessages());	}
	/**	 * Retrieves (from the persistent properties of the file) the targets selected	 * during the last build of the file.	 * 	 * @return String[] the name of the targets	 */	
	public String[] getTargetNamesToPreselect() {
		String propertyString = null;
		try {
			propertyString = antFile.getPersistentProperty(
				new QualifiedName(AntUIPlugin.PI_ANTUI,PROPERTY_SELECTEDTARGETS));
		} catch (CoreException e) {
			new Status(
				IStatus.WARNING,
				AntUIPlugin.PI_ANTUI,
				IStatus.WARNING,
				Policy.bind("status.targetPropertyNotRead", antFile.getFullPath().toString()),
				e);
		}
				if (propertyString == null)			return new String[0];
		StringTokenizer tokenizer = new StringTokenizer(propertyString,SEPARATOR_TARGETS);
		String result[] = new String[tokenizer.countTokens()];
		int index = 0;
		while (tokenizer.hasMoreTokens())
			result[index++] = tokenizer.nextToken();

		return result;
	}		/**	 * Retrieves (from the persistent properties of the file) the arguments specified	 * during the last build of the file.	 * 	 * @return String the arguments string	 */		public String getInitialArguments() {		String propertyString = null;		try {			propertyString = antFile.getPersistentProperty(				new QualifiedName(AntUIPlugin.PI_ANTUI,PROPERTY_ARGUMENTS));		} catch (CoreException e) {			new Status(				IStatus.WARNING,				AntUIPlugin.PI_ANTUI,				IStatus.WARNING,				Policy.bind("status.argumentPropertyNotRead", antFile.getFullPath().toString()),				e);		}				return propertyString;	}		/**	 * Retrieves (from the persistent properties of the file) the choice of the user	 * to show or not the output log.	 * 	 * @return boolean true if the user wants to show it, false if not	 */		public boolean getShouldLogMessages() {		boolean result = false;		try {			String resultString = antFile.getPersistentProperty(				new QualifiedName(AntUIPlugin.PI_ANTUI,PROPERTY_LOG));			if (resultString != null )				result = (resultString.equals("true")) ? true : false;		} catch (CoreException e) {			new Status(				IStatus.WARNING,				AntUIPlugin.PI_ANTUI,				IStatus.WARNING,				Policy.bind("status.logPropertyNotRead", antFile.getFullPath().toString()),				e);		}		return result;	}
			
	/**	 * Builds the Ant file according to the selected targets and the arguments given in the command line.	 *	 * @return boolean	 */			
	public boolean performFinish() {		final Vector targetVect = page1.getSelectedTargets();		final String[] args = createArgumentsArray(targetVect);		if (page1.shouldLogMessages()) {			try {				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();				page.showView(AntConsole.CONSOLE_ID);				console = (AntConsole)page.findView(AntConsole.CONSOLE_ID);				console.clearOutput();			} catch (PartInitException e) {				AntUIPlugin.getPlugin().getLog().log(					new Status(						IStatus.ERROR,						AntUIPlugin.PI_ANTUI,						0,						Policy.bind("status.consoleNotInitialized"),						e));			}		}				// trick to be able to define the buildListener later		final UIBuildListener[] buildListener = new UIBuildListener[1];		
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {				
				monitor.beginTask(Policy.bind("monitor.runningAnt"), targetVect.size());

				try {
					AntRunner runner = new AntRunner();					buildListener[0] = new UIBuildListener(runner, monitor, antFile, console);					runner.run(args, buildListener[0]);
				}				catch (BuildCanceledException e) {
					throw new InterruptedException();
				}
				catch (Exception e) {					throw new InvocationTargetException(e,e.getMessage());
				}				finally {
					monitor.done();
				}
			};
		};				try {			this.getContainer().run(true,true,runnable);		} catch (InterruptedException e) {			return false;					} catch (InvocationTargetException e) {			IStatus status = new Status(				IStatus.ERROR,				AntUIPlugin.PI_ANTUI,				EXCEPTION_ANT_EXECUTION,				e.getMessage(),				e);			ErrorDialog.openError(				getShell(),				Policy.bind("dialog.antScriptProblems"),				Policy.bind("dialog.problemOccured"),				status);			return false;		}		
		storeTargetsOnFile(targetVect);		storeShouldLogMessages();		storeArguments();		
		return true;
	}		/**	 * Creates an array that contains all the arguments needed to run AntRunner: 	 * 	- the name of the file to build	 *  - the arguments such as "-verbose", ...	 *  - target names	 * 	 * @param targets the vector that contains the targets built during the parsing	 * @return String[] the tokenized arguments	 */	protected String[] createArgumentsArray(Vector targets) {			Vector argsVector = new Vector();		Vector targetVect = targets;		String argString = page1.getArgumentsFromField().trim();				// if there are arguments, then we have to tokenize them		if (argString.length() != 0) {			int indexOfToken;						// Checks if the string starts with a bracket or not so that we know where the composed arguments start			if (argString.charAt(0)=='"') 				indexOfToken = 1;			else				indexOfToken=0;					// First tokenize the command line with the separator bracket			StringTokenizer tokenizer1 = new StringTokenizer(argString,"\"");					while (tokenizer1.hasMoreTokens()) {				if (indexOfToken%2 == 0) {					// this is a string that needs to be tokenized with the separator space					StringTokenizer tokenizer2 = new StringTokenizer(tokenizer1.nextToken()," ");					while (tokenizer2.hasMoreTokens())						argsVector.add(tokenizer2.nextToken());				} else {					argsVector.add(tokenizer1.nextToken());				}				indexOfToken++;			}				}						// Finally create the array of String for AntRunner		String args[] = new String[argsVector.size()+targetVect.size()+2];		int index = 0;		args[index++] = "-buildfile";		args[index++] = antFile.getLocation().toOSString();				Iterator argsIterator = argsVector.iterator();		while (argsIterator.hasNext())			args[index++] = (String) argsIterator.next();		argsIterator = targetVect.iterator();		while (argsIterator.hasNext())			args[index++] = ((Target) argsIterator.next()).getName();						return args;			}
	/**	 * Stores the name of the selected targets in the persistent properties of the file,	 * so that next time the user wants to build this file, those targets are pre-selected.	 * 	 * @param targets the vector that contains the targets built during the parsing	 */
	protected void storeTargetsOnFile(Vector targets) {
		StringBuffer targetString = new StringBuffer();
		Iterator targetsIt = targets.iterator();
		
		while (targetsIt.hasNext()) {
			targetString.append(((Target)targetsIt.next()).getName());
			targetString.append(SEPARATOR_TARGETS);
		}
		
		try {
			antFile.setPersistentProperty(
				new QualifiedName(AntUIPlugin.PI_ANTUI,PROPERTY_SELECTEDTARGETS),
				targetString.toString());
		} catch (CoreException e) {
			AntUIPlugin.getPlugin().getLog().log(
				new Status(
					IStatus.WARNING,
					AntUIPlugin.PI_ANTUI,
					IStatus.WARNING,
					Policy.bind("status.targetPropertyNotWritten", antFile.getFullPath().toString()),
					e));
		}
	}		/**	 * Stores the user's will concerning the log that shows on success.	 */	protected void storeShouldLogMessages() {				try {			antFile.setPersistentProperty(				new QualifiedName(AntUIPlugin.PI_ANTUI,PROPERTY_LOG),				page1.shouldLogMessages() ? "true" : "false");		} catch (CoreException e) {			AntUIPlugin.getPlugin().getLog().log(				new Status(					IStatus.WARNING,					AntUIPlugin.PI_ANTUI,					IStatus.WARNING,					Policy.bind("status.logPropertyNotWritten", antFile.getFullPath().toString()),					e));		}	}		/**	 * Stores the user's specified execution arguments	 */	protected void storeArguments() {		try {			antFile.setPersistentProperty(				new QualifiedName(AntUIPlugin.PI_ANTUI,PROPERTY_ARGUMENTS),				page1.getArguments());		} catch (CoreException e) {			AntUIPlugin.getPlugin().getLog().log(				new Status(					IStatus.WARNING,					AntUIPlugin.PI_ANTUI,					IStatus.WARNING,					Policy.bind("status.argumentPropertyNotWritten", antFile.getFullPath().toString()),					e));		}	}	/**	 * Returns true if the wizard can finish, i.e. if a target is selected or if an argument 	 * has benn entered in the command line.	 * 	 * @return boolean true if the wizard can finish, false if not	 */		public boolean canFinish() {		return (page1.getSelectedTargets().size() != 0) || (page1.getArgumentsFromField().trim() != "");	}
}
