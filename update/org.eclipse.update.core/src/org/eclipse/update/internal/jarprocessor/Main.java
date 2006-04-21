/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.jarprocessor;

import java.io.*;
import java.util.zip.ZipException;

/**
 * @author aniefer
 *
 */
public class Main {

	public static class Options {
		public String outputDir = "."; //$NON-NLS-1$
		public String signCommand = null;
		public boolean pack = false;
		public boolean repack = false;
		public boolean unpack = false;
		public boolean verbose = false;
		public File input = null;
	}

	private static void printUsage() {
		System.out.println("[-option ...]... input"); //$NON-NLS-1$
		System.out.println("The following options are supported:"); //$NON-NLS-1$
		System.out.println("-repack         normalize jars "); //$NON-NLS-1$
		System.out.println("-sign <command> sign jars using <command>"); //$NON-NLS-1$
		System.out.println("-pack           pack the jars.  pack and repack are redundant unless"); //$NON-NLS-1$
		System.out.println("                sign is also specified."); //$NON-NLS-1$
		System.out.println("-unpack         unpack pack.gz files. Unpack is mutually exclusive"); //$NON-NLS-1$
		System.out.println("                with repack, sign and pack."); //$NON-NLS-1$
		System.out.println();
		System.out.println("-outputDir <dir>  the output directory"); //$NON-NLS-1$
		System.out.println("-verbose        verbose mode "); //$NON-NLS-1$
	}

	public static Options processArguments(String[] args) {
		if (args.length == 0) {
			printUsage();
			return null;
		}

		Options options = new Options();
		int i = 0;
		for (; i < args.length - 1; i++) {
			if (args[i].equals("-pack")) {//$NON-NLS-1$
				options.pack = true;
			} else if (args[i].equals("-unpack")) { //$NON-NLS-1$
				options.unpack = true;
			} else if (args[i].equals("-sign") && i < args.length - 2) { //$NON-NLS-1$
				if (args[i + 1].startsWith("-")) { //$NON-NLS-1$
					printUsage();
					return null;
				}
				options.signCommand = args[++i];
			} else if (args[i].equals("-repack")) { //$NON-NLS-1$
				options.repack = true;
			} else if (args[i].equals("-outputDir") && i < args.length - 2) { //$NON-NLS-1$
				if (args[i + 1].startsWith("-")) { //$NON-NLS-1$
					printUsage();
					return null;
				}
				options.outputDir = args[++i];
			} else if (args[i].equals("-verbose")) { //$NON-NLS-1$
				options.verbose = true;
			}
		}

		options.input = new File(args[i]);

		String problemMessage = null;
		String inputName = options.input.getName();
		if (options.unpack) {
			if (!JarProcessor.canPerformUnpack()) {
				problemMessage = "The unpack200 command cannot be found."; //$NON-NLS-1$
			} else 	if (options.input.isFile() && !inputName.endsWith(".zip") && !inputName.endsWith(".pack.gz")) { //$NON-NLS-1$ //$NON-NLS-2$
				problemMessage = "Input file is not a pack.gz file."; //$NON-NLS-1$
			} else 	if (options.pack || options.repack || options.signCommand != null) {
				problemMessage = "Pack, repack or sign cannot be specified with unpack."; //$NON-NLS-1$
			}
		} else {
			if (options.input.isFile() && !inputName.endsWith(".zip") && !inputName.endsWith(".jar")) { //$NON-NLS-1$ //$NON-NLS-2$
				problemMessage = "Input file is not a jar file."; //$NON-NLS-1$
			} else	if ((options.pack || options.repack) && !JarProcessor.canPerformPack()) {
				problemMessage = "The pack200 command can not be found."; //$NON-NLS-1$
			}
		}
		if(problemMessage != null){
			System.out.println(problemMessage);
			System.out.println();
			printUsage();
			return null;
		}

		return options;
	}

	public static void runJarProcessor(Options options) {
		if (options.input.getName().endsWith(".zip")) { //$NON-NLS-1$
			ZipProcessor processor = new ZipProcessor();
			processor.setWorkingDirectory(options.outputDir);
			processor.setSignCommand(options.signCommand);
			processor.setPack(options.pack);
			processor.setRepack(options.repack || (options.pack && options.signCommand != null));
			processor.setUnpack(options.unpack);
			processor.setVerbose(options.verbose);
			try {
				processor.processZip(options.input);
			} catch (ZipException e) {
				if (options.verbose)
					e.printStackTrace();
			} catch (IOException e) {
				if (options.verbose)
					e.printStackTrace();
			}
		} else {
			JarProcessor processor = new JarProcessor();
			processor.setWorkingDirectory(options.outputDir);
			processor.setVerbose(options.verbose);

			if (options.repack || (options.pack && options.signCommand != null))
				processor.addProcessStep(new PackUnpackStep(null, options.verbose));

			if (options.signCommand != null)
				processor.addProcessStep(new SignCommandStep(null, options.signCommand, options.verbose));

			if (options.pack)
				processor.addProcessStep(new PackStep(null, options.verbose));
			else if (options.unpack)
				processor.addProcessStep(new UnpackStep(null, options.verbose));

			try {
				processor.process(options.input, options.unpack ? Utils.PACK_GZ_FILTER : Utils.JAR_FILTER);
			} catch (FileNotFoundException e) {
				if (options.verbose)
					e.printStackTrace();
			}
		}
	}
	
	/**
	 * @param args
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) {
		Options options = processArguments(args);
		if (options == null)
			return;
		runJarProcessor(options);
	}

}
