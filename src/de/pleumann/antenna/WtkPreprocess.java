/* -----------------------------------------------------------------------------
 * Antenna - An Ant-to-end solution for wireless Java 
 *
 * Copyright (c) 2002-2004 Joerg Pleumann <joerg@pleumann.de>
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * -----------------------------------------------------------------------------
 */
package de.pleumann.antenna;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.MatchingTask;

import antenna.preprocessor.IPreprocessor;
import antenna.preprocessor.PreprocessorException;
import antenna.preprocessor.v3.ILineFilter;
import antenna.preprocessor.v3.ILogger;
import antenna.preprocessor.v3.PPException;
import antenna.preprocessor.v3.Preprocessor;
import de.pleumann.antenna.misc.Conditional;
import de.pleumann.antenna.misc.Strings;
//import de.pleumann.antenna.device.DeviceProps;
//import de.pleumann.antenna.device.Devices;
//import de.pleumann.antenna.misc.Utility;

// actual implementation of task depends on backend version.
public class WtkPreprocess extends MatchingTask {

	private Conditional condition;

	private File sourceDir;

	private File targetDir;

	private String newext;

	private String encoding;

	private int mode = IPreprocessor.MODE_NORMAL | IPreprocessor.MODE_INDENT;

	private Preprocessor m_preprocessor;

	/**
	 * A comma separated list of symbols
	 */
	private String m_symbols;

	/**
	 * Current device name, may be null.
	 */
	// private String m_device;

	private String m_saveSymbols;

	/**
	 * Preprocessing backend version number
	 */
	private String m_backendVersion = "3";

	private boolean m_printSymbols = false;

	private Vector m_symbolsFile = new Vector();

	/**
	 * Preprocessor debub level
	 */
	private String m_debugLevel;

	public void init() {
		super.init();
		// utility = Utility.getInstance(getProject(), this);

		condition = new Conditional(getProject());
		sourceDir = getProject().resolveFile(".");
		createInclude().setName("**/*.java");
	}

	public void setSrcdir(File value) {
		// System.out.println("src=" + value);
		sourceDir = value;
	}

	public void setDestdir(File value) {
		targetDir = value;
//		System.out.println("desc=" + value);
	}

	public void setSymbols(String symbols) {
//		System.out.println("symbols=" + symbols);
		m_symbols = symbols;
	}

	public void setVerbose(boolean verbose) {
		if (verbose) {
			mode = mode | IPreprocessor.MODE_VERBOSE;
		} else {
			mode = mode & ~IPreprocessor.MODE_VERBOSE;
		}
	}

	public void setBackup(boolean backup) {
		if (backup) {
			mode = mode | IPreprocessor.MODE_BACKUP;
		} else {
			mode = mode & ~IPreprocessor.MODE_BACKUP;
		}
	}

	public void setIndent(boolean indent) {
		if (indent) {
			mode = mode | IPreprocessor.MODE_INDENT;
		} else {
			mode = mode & ~IPreprocessor.MODE_INDENT;
		}
	}

	public void setTest(boolean test) {
		if (test) {
			mode = mode | IPreprocessor.MODE_TEST;
		} else {
			mode = mode & ~IPreprocessor.MODE_TEST;
		}
	}

	public void setFilter(boolean filter) {
		if (filter) {
			mode = mode | IPreprocessor.MODE_FILTER;
		} else {
			mode = mode & ~IPreprocessor.MODE_FILTER;
		}
	}

	public void setNewext(String newext) {
		this.newext = newext;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public void setIf(String s) {
		condition.setIf(s);
	}

	public void setUnless(String s) {
		condition.setUnless(s);
	}

	public boolean isActive() {
		return condition.isActive();
	}

	public void execute() throws BuildException {
		if (!isActive())
			return;

		if (targetDir == null) {
			throw new BuildException("Need a target directory");
		}

		StringTokenizer tok = new StringTokenizer(sourceDir.toString(), ""
				+ File.pathSeparatorChar);
		while (tok.hasMoreElements()) {
			String dir = tok.nextToken();
			try {
				IPreprocessor pp;
				ILineFilter filter = null;

				ILogger logger = null;
				final Project project = getProject();
				filter = new ILineFilter() {

					public String filter(String line) {

						return project.replaceProperties(line);

					}

				};

				logger = new ILogger() {

					public void log(String message) {

						project.log(message);

					}

				};

				m_preprocessor = new Preprocessor(logger, filter);

				try {
					setDebugLevel(m_debugLevel);
				} catch (Exception e) {
					log(e.getMessage(), Project.MSG_WARN);
				}


				for (int i = 0; i < m_symbolsFile.size(); i++) {
					Symbols_File f = (Symbols_File) m_symbolsFile.get(i);
					if (f.name != null) {
						File file = new File(f.name);
						if (!file.exists()) {
							log("Symbols file not found : "
									+ file.getAbsolutePath(), Project.MSG_INFO);
						} else {
							try {
								m_preprocessor.addDefines(file);
							} catch (PPException e) {
								log("Error preprocessing symbols"
										+ file.getAbsolutePath(),
										Project.MSG_ERR);
								e.printStackTrace();
							} catch (IOException e) {
								log("IOException adding "
										+ file.getAbsolutePath(),
										Project.MSG_ERR);
								e.printStackTrace();
							}
						}
					} else if (f.list != null) {
						StringTokenizer t = new StringTokenizer(f.list, " ,");
						while (t.hasMoreElements()) {
							File ff = new File(t.nextToken());
							if (!ff.exists()) {
								log("Symbols file not found : "
										+ ff.getAbsolutePath(),
										Project.MSG_WARN);
								continue;
							}

							try {
								m_preprocessor.addDefines(ff);
							} catch (Exception e) {
								log("Error adding symbols from "
										+ ff.getAbsolutePath(),
										Project.MSG_WARN);
							}
						}
					}
				}


				m_preprocessor.addDefines(m_symbols);
				if (m_printSymbols) {
					// pp.printSymbols();
					getProject().log(
							"Symbols: "
									+ m_preprocessor.getDefines().toString());
				}

				if (m_saveSymbols != null) {
					try {
						outputDefinesToFile(new File(m_saveSymbols), encoding);
					} catch (IOException e) {
						log("Error saving defines to file " + m_saveSymbols,
								Project.MSG_WARN);
					}
				}

				File file = new File(dir);
				preprocess(file, mode, newext, encoding);
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new BuildException("Preprocessing failed: "
						+ ex.getMessage(), ex);
			}
		}
	}

	public void outputDefinesToFile(File file, String encoding)

	throws PreprocessorException, IOException {

		FileOutputStream out = null;

		try {

			out = new FileOutputStream(file);

			out.write(m_preprocessor.getDefines().toString().getBytes(

			encoding != null ? encoding : "UTF-8"));

			out.flush();

		} finally {

			if (out != null)

				out.close();

		}

	}

	// this method allow using a different filter to handle the lines.
	public void preprocess(File sourceDir, int mode, String newext,
			String encoding) throws PreprocessorException, IOException {

		m_preprocessor.setVerbose((mode & IPreprocessor.MODE_VERBOSE) != 0);
		String[] files = getDirectoryScanner(sourceDir).getIncludedFiles();
		log("Preprocessing " + files.length + " file(s) at " + sourceDir);

		String filename = ""; // For exception handling

		try {
			for (int i = 0; i < files.length; i++) {
				filename = files[i];

				String sourceFile = "" + sourceDir + File.separatorChar
						+ filename;

				Strings lines = loadFile(encoding, new File(sourceFile));

				m_preprocessor.setFile(new File(sourceFile));

				boolean modified = m_preprocessor.preprocess(lines.getVector(),
						encoding);
				
				// if preprocessing modifies the file, or
				// we are putting the output in a different directory
				// or we are changing the file extension
				// then we have to write a new file
				if (modified || !sourceDir.equals(targetDir)
						|| (newext != null)) {
					try {
						if ((mode & IPreprocessor.MODE_VERBOSE) != 0) {
							System.out.println(filename + " ... modified");
						}

						if ((mode & IPreprocessor.MODE_TEST) == 0) {
							String targetFile;

							if (newext != null) {
								int dot = filename.indexOf('.');

								if (dot != -1) {
									filename = filename.substring(0, dot)
											+ newext;
								} else {
									filename = filename + newext;
								}
							}

							targetFile = "" + targetDir + File.separatorChar
									+ filename;

							File file = new File(targetFile + "~");
							file.delete();
							if (!new File(targetFile).renameTo(file)
									&& (targetDir == null)) {
								throw new java.io.IOException();
							}

							new File(targetFile).getParentFile().mkdirs();

							if (encoding != null && encoding.length() > 0) {
								lines.saveToFile(targetFile, encoding);
							} else {
								lines.saveToFile(targetFile);
							}

							if ((mode & IPreprocessor.MODE_BACKUP) == 0) {
								file.delete(); // ??????
							}
						}
					} catch (java.io.UnsupportedEncodingException uee) {
						throw new PreprocessorException("Unknown encoding \""
								+ encoding, new File(files[i]));
					} catch (java.io.IOException e) {
						throw new PreprocessorException("File write error",
								new File(files[i]));
					}
				} else {
					if ((mode & IPreprocessor.MODE_VERBOSE) != 0) {
						System.out.println(filename + " ... not modified");
					}
				}
			}
		} catch (IOException e) {
			if ((mode & IPreprocessor.MODE_VERBOSE) == 0) {
				System.out.println(filename + " ... not modified, "
						+ e.getMessage());
			}

			throw e;
		} catch (PreprocessorException error) {
			if ((mode & IPreprocessor.MODE_VERBOSE) == 0) {
				System.out.println(filename + " ... not modified, "
						+ error.getMessage());
			}

			throw error;
		} catch (PPException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	private Strings loadFile(String encoding, File sourceFile)
			throws PreprocessorException {
		Strings lines = new Strings();
		try {
			if (encoding != null && encoding.length() > 0)
				lines.loadFromFile(sourceFile, encoding);
			else
				lines.loadFromFile(sourceFile);
		} catch (java.io.UnsupportedEncodingException e) {
			throw new PreprocessorException("Unknown encoding \"" + encoding
					+ "\"", sourceFile, e);
		} catch (java.io.IOException e) {
			throw new PreprocessorException("File read error", sourceFile, e);
		}
		return lines;
	}

	// public void setDevice(String device)
	// {
	// m_device = device;
	// }

	// private String getDeviceDefines()
	// {
	// DeviceProps deviceProps = Devices.getDevice(m_device);
	// String deviceDefines = "";
	// if (deviceProps == null)
	// {
	// getProject().log("Warning: unknown device \"" + m_device + "\"");
	// }
	// else
	// {
	// deviceDefines = deviceProps.getDefinesString();
	// }
	// return deviceDefines;
	// }

	private String addSymbols(String current, String deviceDefines) {
		String s = "";
		s = append(m_symbols, s);
		s = append(s, deviceDefines);
		return s;
	}

	private String append(String cur, String s) {
		if (cur == null || cur.length() == 0)
			return s;
		String ss = s != null && s.length() > 0 ? cur + "," + s : cur;
		return ss;
	}

	public void setVersion(String backendVersion) {
		m_backendVersion = backendVersion;
	}

	/**
	 * If true, active symbols will be printed before preprocessing.
	 */
	public void setPrintSymbols(boolean printSymbols) {
		m_printSymbols = printSymbols;
	}

	public static class Symbols_File {
		private String name;
		private String list;

		public void setName(String name) {
			this.name = name;
		}

		public void setList(String list) {
			this.list = list;
		}

	}

	// public Symbols_File createSymbols_File()
	// {
	// Symbols_File a = new Symbols_File();
	// m_symbolsFile.addElement(a);
	// return a;
	// }

	public void setSaveSymbols(String file) {
		m_saveSymbols = file;
	}

	public void setDebugLevel(String debug) throws PreprocessorException {
		m_debugLevel = debug;
		String level = debug;
		if ("none".equals(level) || level == null) {

			m_preprocessor.getDefines().undefine("DEBUG");

		} else {

			if (level.equalsIgnoreCase("debug")

			|| level.equalsIgnoreCase("info")

			|| level.equalsIgnoreCase("warn")

			|| level.equalsIgnoreCase("error")

			|| level.equalsIgnoreCase("fatal")) {

				try

				{

					m_preprocessor.addDefines("DEBUG=" + level);

				} catch (Exception e)

				{

					// throw new
					// PreprocessorException("Error adding defines",e);

				}

			} else {

				throw new PreprocessorException(

				"Unsupported debug level "

				+ level

				+ ", Supported values are [debug|info|warn|error|fatal|none]");

			}

		}
	}

	// public void setDeviceDBPath(String path) throws BuildException
	// {
	// try
	// {
	// Devices.setDatabaseDir(path);
	// }
	// catch (Exception e)
	// {
	// throw new BuildException(e);
	// }
	// }

	public static void main(String[] args) throws Exception{
//		WtkPreprocess pre = new WtkPreprocess();
//		pre.setSrcdir(new File("/Users/aoro/Documents/workspace/T1/src/test/"));
//		pre.setDestdir(new File("/Users/aoro/Documents/workspace/T1/bin"));
//		pre.setSymbols("symbols=abc,defo=lll,test=false");
//		pre.execute();
		
		Preprocessor pp = new Preprocessor(null, null);
		pp.addDefines("symbols=abc,defo=lll,test=false");
		Strings lines = new Strings();
		lines.loadFromFile("/Users/aoro/Documents/workspace/T1/src/test/Test.java");
		pp.setFile(new File("/Users/aoro/Documents/workspace/T1/src/test/Test.java"));
		pp.preprocess(lines.getVector(), "utf-8");

	}
}
