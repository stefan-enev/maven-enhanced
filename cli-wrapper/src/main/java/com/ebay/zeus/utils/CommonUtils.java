package com.ebay.zeus.utils;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;


public class CommonUtils {

	public static final String SPACE = " ";

	public static boolean isHostReachable(String host) {
		boolean result = false;

		try {
			InetAddress address = InetAddress.getByName(host);
			result = address.isReachable(2000);
		} catch (UnknownHostException e) {
			// e.printStackTrace();
		} catch (IOException e) {
			// e.printStackTrace();
		}

		return result;
	}

	public static String searchAndReplaceText(String inputStr,
			String patternStr, String replaceStr) {

		String outstr = null;
		Pattern pattern = Pattern.compile(patternStr);
		Matcher matcher = pattern.matcher(inputStr);
		outstr = matcher.replaceAll(replaceStr);

		return outstr;
	}

	public static String replaceMultipleSpacesWithSingleSpace(String input) {
		String output;

		String temp = new String(searchAndReplaceText(input, "  ", " "));

		do {
			output = searchAndReplaceText(temp, "  ", " ");

			if (output.equals(temp)) {
				break;
			}

			temp = new String(output);

		} while (true);

		return output;
	}

	/**
	 * @return
	 * @throws ServiceException
	 */
	public static Properties getProperty(String path) throws CommonException {
		Properties prop = new Properties();
		try {
			InputStream stream = CommonUtils.class.getResourceAsStream(path);

			prop.load(stream);
			stream.close();
		} catch (IOException e) {
			throw new CommonException("property load failed", e);
		}

		return prop;
	}

	/**
	 * get exception stack trace output string
	 * @param aThrowable
	 * @return output string
	 */
	public static String getStackTrace(Throwable aThrowable) {
		final Writer result = new StringWriter();
		final PrintWriter printWriter = new PrintWriter(result);
		aThrowable.printStackTrace(printWriter);
		return result.toString();
	}

	public static void main(String[] args) {

		if (isHostReachable("repository.corp.ebay.com")) {
			System.out.println("reachable");
		} else {
			System.out.println("un reachable");
		}

		/*
		 * String input = "com.ebay.ebox.tools.release";
		 * 
		 * String output = CommonUtils.searchAndReplaceText(input, "\\.", "/");
		 * 
		 * String input =
		 * "/ebay/software/java_pkgs/maven/2.0.9/bin/mvn  org.codehaus.mojo:wagon-maven-plugin:download-single   -Dwagon.url=http://repository.corp.ebay.com/ebox/common/com/ebay/ebox/tools/release/configuration/config-files/LATEST/        -Dwagon.fromFile=config-files-LATEST.zip  -Dwagon.toFile=/home/snambi/hudsontesthome/jobs/v1/workspace/work/config-files.zip"
		 * ;
		 * 
		 * String output = replaceMultipleSpacesWithSingleSpace(input);
		 * 
		 * System.out.println( input); System.out.println( output); String str
		 * ="<conversion><viewRoot>V3_VIEW_ROOT</viewRoot></conversion>"; String
		 * pattern = "V3_VIEW_ROOT"; File dir = new
		 * File("D:\\hudsonhome2\\jobs\\t2\\");
		 * 
		 * String dirStr = dir.getAbsolutePath(); String ss =
		 * dirStr.replaceAll("\\\\", "\\\\\\\\");
		 * 
		 * String outStr = searchAndReplaceText(str, pattern, ss);
		 * 
		 * System.out.println(outStr);
		 */

	}

	public static void copyFile(String source, String destination) {

		File sFile = new File(source);
		File dFile = new File(destination);

		try {
			InputStream input = new FileInputStream(sFile);
			OutputStream output = new FileOutputStream(dFile);

			byte[] buffer = new byte[1024];
			int len;

			while ((len = input.read(buffer)) > 0) {
				output.write(buffer, 0, len);
			}

			input.close();
			output.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String getPath(Map<String, String> env) {

		String path = null;

		if (isLinux()) {
			path = env.get("PATH");
		} else {
			path = env.get("Path");
		}

		return path;
	}

	public static String readTextFile(String filename) {

		StringBuilder sb = new StringBuilder();

		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(filename));
			String str;
			while ((str = reader.readLine()) != null) {
				sb.append(str);
				sb.append("\n");
			}

			reader.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return sb.toString();
	}

	public static void writeTextFile(String filename, String data) {

		try {
			PrintWriter output = new PrintWriter(new FileWriter(filename));
			output.write(data);
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static boolean isLinux() {

		boolean result = false;
		String os = System.getProperty("os.name");
		if (os.toUpperCase().equals("LINUX")) {
			result = true;
		}

		return result;
	}

	public static boolean isWindows() {

		boolean result = false;
		String os = System.getProperty("os.name");
		if (os.toUpperCase().contains("WINDOWS")) {
			result = true;
		}

		return result;
	}

	public static boolean isEmpty(String value) {
		boolean result = false;

		if (value == null || value.trim().equals("")) {
			result = true;
		}

		return result;
	}

	public static boolean deleteDirectory(File path) {
		if (path.exists()) {
			File[] files = path.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					deleteDirectory(files[i]);
				} else {
					if (!files[i].delete()) {
						return false; // Could not delete a single child file,
										// so return false for fail
					}
				}
			}
		}
		return (path.delete());
	}
    /**
     * copy input to output stream - available in several StreamUtils or Streams classes 
     */    
    public static void copy(InputStream input, OutputStream output) throws IOException {
    	byte[] BUFFER = new byte[4096 * 1024];
        int bytesRead;
        while ((bytesRead = input.read(BUFFER))!= -1) {
            output.write(BUFFER, 0, bytesRead);
        }
    }

	public static void unzip(File file, File directory, PrintStream logger) {

		try {

			ZipFile zipFile = new ZipFile(file);

			// first create directories
			Enumeration<ZipEntry> entries1 = (Enumeration<ZipEntry>) zipFile
					.entries();
			while (entries1.hasMoreElements()) {
				ZipEntry entry = entries1.nextElement();
				if (entry.isDirectory()) {
					File dir = new File(directory.getAbsolutePath()
							+ File.separator + entry.getName());
					if (!dir.exists()) {
						logger.println("extracting file : "
								+ dir.getAbsolutePath());
						dir.mkdir();
					}
				}
			}

			// now extract the files
			Enumeration<ZipEntry> entries2 = (Enumeration<ZipEntry>) zipFile
					.entries();
			while (entries2.hasMoreElements()) {

				ZipEntry entry = entries2.nextElement();

				if (!entry.isDirectory()) {

					File outfile = new File(directory.getAbsolutePath()
							+ File.separator + entry.getName());

					InputStream input = zipFile.getInputStream(entry);
					OutputStream output = new BufferedOutputStream(
							new FileOutputStream(outfile.getAbsolutePath()));

					byte[] buffer = new byte[1024];
					int len;

					logger.println("extracting file : "
							+ outfile.getAbsolutePath());
					while ((len = input.read(buffer)) >= 0) {
						output.write(buffer, 0, len);
					}

					input.close();
					output.close();
				}
			}

			logger.println(zipFile.getName() + " successfully unzipped");

		} catch (ZipException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String convertListToString(List<String> array) {

		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < array.size(); i++) {
			sb.append(array.get(i));
			if (i != (array.size() - 1)) {
				sb.append(",");
			}
		}

		return sb.toString();
	}

	public static char[] removeCarriageReturns(char[] chararray) {

		// first count the size of the array
		int size = 0;
		for (int i = 0; i < chararray.length; i++) {
			if (chararray[i] != '\r') {
				size++;
			}
		}

		char[] array = new char[size];
		int j = 0;
		for (int i = 0; i < chararray.length; i++) {
			if (chararray[i] != '\r') {
				array[j] = chararray[i];
				j++;
			}
		}

		return array;
	}

	public static boolean isQA() {
		String hostName = null;
		try {
			hostName = InetAddress.getLocalHost().getCanonicalHostName();
		} catch (UnknownHostException e) {
			// Use CORP by default
			System.err
					.println("Unable to detect environment [assuming that we are not in qa]"
							+ e.getMessage());
			return false;
		}

		if (hostName != null && hostName.trim().length() > 0) {
			if (hostName.contains(".qa.ebay.com")) {
				return true;
			}
		}
		return false;
	}
}