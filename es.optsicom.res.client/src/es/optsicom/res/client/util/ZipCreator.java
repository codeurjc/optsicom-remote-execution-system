/* ******************************************************************************
 * This file is part of Optsicom Remote Experiment System client
 * 
 * License:
 *   EPL: http://www.eclipse.org/legal/epl-v10.html
 *   See the LICENSE file in the project's top-level directory for details.
 *   
 * Contributors:
 *   Optsicom(http://www.optsicom.es), Sidelab (http://www.sidelab.es) 
 *   and others
 * **************************************************************************** */
package es.optsicom.res.client.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Class to create zip files.
 * 
 * <p>This class can be used as follows:</p>
 * 
 * <pre>
ZipCreate zipCreator = new ZipCreate();
		
// Sets the base folder. Files added will be relativized to this folder
zipCreator.setBaseDir("images");
String zipName = "images" + getTimeStamp() + ".zip";
zipCreator.zip("images", zipName);
</pre>
 * 
 * @author Patxi Gortázar (patxi.gortazar@gmail.com)
 * @see http://www.sidelab.es/
 * 
 */
public class ZipCreator {
	
	
	
	private static List<File> getFilesRecursively(File folder) {
		List<File> files = new ArrayList<File>();
		for(File f : folder.listFiles()) {
			if(f.isDirectory()) {
				files.addAll(getFilesRecursively(f));
			} else {
				files.add(f);
			}
		}
		return files;
	}

	private ZipOutputStream zipOut;
	private String baseDir;
	
	public ZipCreator(){
		zipOut = null;
	}
	
	/**
	 * The base folder to which absolute file paths added to this zip 
	 * file will be relativized. It must be a subpath of all other files added.
	 * 
	 * For instance, if we want to add the files under "/some_path/other_path/images"
	 * being their relative path the images folder, we should specify 
	 * "/some_path/other_path/images" as base dir. If we would like the images folder
	 * itself to be listed in the zip file, we would pass in "/some_path/other_path".  
	 * 
	 * @param baseDir The folder to which absolute file paths are relativized
	 */
	public void setBaseDir(String baseDir) {
		this.baseDir = new File(baseDir).getAbsolutePath();
	}

	/**
	 * Adds the files under the given folder (and any subfolder) to this zip file.
	 * 
	 * The folder is scanned recursively, so in some systems this may produce an
	 * infinite loop if symbolic links are present.
	 * 
	 * @param folder Files under this folder (and any subfolder) are added to the zip file.  
	 * @param outFileName The output filename to be used (the zip extension must be provided explicitly). 
	 * @throws ZipCreatorException If any exception is launched during the creation of the zip file, it is wrapped inside a {@link ZipCreatorException}.
	 */
	public void zip(String folder, String outFileName) throws ZipCreatorException {
//		List<File> files = getFilesRecursively(new File(folder));
//		zip(files.toArray(new File[0]), outFileName);
		zip(new File[] {new File(folder)}, outFileName);
	}
	
	/**
	 * Adds the files present in the array to this zip file. If the array
	 * contains folders, files contained in the folder (and any subfolder)
	 * are added to the zip file also.
	 * 
	 * @param files The files to be added.  
	 * @param outFileName The output filename to be used (the zip extension must be provided explicitly). 
	 * @throws ZipCreatorException If any exception is launched during the creation of the zip file, it is wrapped inside a {@link ZipCreatorException}.
	 */
    public void zip(File[] files, String outFileName) throws ZipCreatorException {
    	try {
    		File zipFile = new File(outFileName);
			zipFile.createNewFile();
			OutputStream fileOut = new FileOutputStream(zipFile);
    		zipOut = new ZipOutputStream(new BufferedOutputStream(fileOut));

			for(File file: files){
				writeToZip(file, zipOut);
				zipOut.flush();
			}
			
			zipOut.close();
		} catch (Exception e) {
			throw new ZipCreatorException(e);
		}
    }
    
    private void writeToZip(File file, ZipOutputStream zipOut) throws IOException,FileNotFoundException{
	    
	    if(file.isDirectory()){
	    	for(File child: file.listFiles()){
	    		writeToZip(child, zipOut);
	    	}
	    } else {
	        
	    	// Relativize the file to the base folder
 		    String relativePath = file.getAbsolutePath().substring(baseDir.length() + 1);
	    	
	    	// This is to avoid problems when opening the zip file in Linux systems 
	    	String pathForwardSlashes = relativePath.replaceAll("\\\\", "/");

			ZipEntry entry = new ZipEntry(pathForwardSlashes);
			zipOut.putNextEntry(entry);
			
			// Copy contents
	    	FileInputStream fileIn = new FileInputStream(file);
	    	copyInputStream(fileIn, zipOut);
			fileIn.close();
			zipOut.flush();	
			zipOut.closeEntry();
	    }   
    }
    
    /*
     * The streams are not closed. It is the user responsibility to close them
     */
	private final void copyInputStream(InputStream in, OutputStream out) throws IOException {
	    byte[] buffer = new byte[1024];
	    int len;

	    while((len = in.read(buffer)) >= 0) {
	      out.write(buffer, 0, len);
	    }
	}
    
}
