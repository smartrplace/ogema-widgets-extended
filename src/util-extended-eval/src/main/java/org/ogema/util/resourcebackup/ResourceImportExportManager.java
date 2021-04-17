/**
 * ﻿Copyright 2014-2018 Fraunhofer-Gesellschaft zur Förderung der angewandten Wissenschaften e.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ogema.util.resourcebackup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.logging.OgemaLogger;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.resourcemanager.InvalidResourceTypeException;
import org.ogema.model.action.Action;
import org.ogema.model.gateway.LocalGatewayInformation;
import org.ogema.model.locations.Room;
import org.ogema.model.stakeholders.LegalEntity;

import de.iwes.util.performanceeval.ExecutionTimeLogger;

public class ResourceImportExportManager {
    private static final String JSON_EXTENSION1 = ".ogj";
    private static final String XML_EXTENSION1 = ".ogx";
    private static final String JSON_EXTENSION2 = ".json";
    private static final String XML_EXTENSION2 = ".xml";

    private final ApplicationManager appMan;
	private final OgemaLogger log;
	
	public ResourceImportExportManager(ApplicationManager appMan) {
		this.appMan = appMan;
		this.log = appMan.getLogger();
	}

	final static boolean isSpecialDir(String destDirStr) {
		return destDirStr.contains("extBackup") || destDirStr.contains("replay-on-clean");
	}
	
	/**
	 * @param destDirStr directory to write to the full result path will be
	 * 		aggegated from destDirStr, baseFileName and an extension automatically
	 * 		generated based on the file type to write
	 * @return
	 * @throws SecurityException if access to some configured file is denied
	 */
	public File runBackup(final String destDirStr, String baseFileName,
			boolean includeStandardReferences, boolean writeJSON,
			Resource resourceToBackup) {
		return AccessController.doPrivileged(new PrivilegedAction<File>() {

			@Override
			public File run() {
				return runBackupUnprivileged(destDirStr, baseFileName, includeStandardReferences, writeJSON, resourceToBackup);
			}
		});
		
	}
	
	public File runBackupUnprivileged(String destDirStr, String baseFileName,
			boolean includeStandardReferences, boolean writeJSON,
			Resource resourceToBackup) {

		
		File destDir = new File(destDirStr);
		File destBaseFile = new File(destDirStr, baseFileName);

		if(!destDir.exists()) {
			destDir.mkdirs();
		} 
		/*if(overwrite) {
			for( File f : destDir.listFiles()) {
				if(!f.getName().endsWith(".zip")) {
					if(f.isDirectory()) {
						try {
							FileUtils.deleteDirectory(f); // FIXME apache lib requires (java.lang.RuntimePermission "getClassLoader")
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} else {
						f.delete();
					}
				}
			}
//    		try{FileUtils.cleanDirectory(destDir);} catch (IOException e) {e.printStackTrace();}
		}*/
		
		RefData refData = new RefData(includeStandardReferences, writeJSON);
		
		writeResourceAndReferences(destBaseFile, resourceToBackup, refData);
		
//		zip the file
/*		if(!overwrite) {
			String customName = destDir.getName();
			Path zipFile = destDir.getParentFile().toPath().resolve(customName + ".zip");
			File zip = zipFile.toFile();
			ZipUtil.compressEntireDirectory(zipFile, destDir.toPath());
			try {
				FileUtils.deleteDirectory(destDir);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return zip;
		}*/
		return destDir;
	}
	
	private class RefData {
		@SuppressWarnings("rawtypes")
		Class[] refTypes = {Room.class, LegalEntity.class, Action.class, LocalGatewayInformation.class};
		List<Resource> refResources = new ArrayList<Resource>();
		
		final boolean writeJson;
		final boolean includeStdRefs;
		
		public RefData(boolean includeStdRefs, boolean writeJson) {
			this.includeStdRefs = includeStdRefs;
			this.writeJson = writeJson;
		}
	}
	
	@SuppressWarnings("unchecked")
	private void writeResourceAndReferences(File baseFilePath, Resource res, RefData data) {
		//write rooms and users
//		log.debug("parentDir : " + parentDir + ", res : " + res + ", data : " + data);
		int count = 1;
		if(data.includeStdRefs) for(Class<? extends Resource> resType: data.refTypes) {
			for(Resource refRes: res.getSubResources(resType, true)) {
				Resource locRefRes = refRes.getLocationResource();
				if(locRefRes.isTopLevel()) {
					writeIfNotYetWrittenResource(locRefRes, new File(baseFilePath.getPath()+count), data);
				}
			}
		}
		writeIfNotYetWrittenResource(res, baseFilePath, data);
	}
	
	private void writeIfNotYetWrittenResource(Resource res, File baseFileName, RefData data) {
		Resource locRefRes = res.getLocationResource();
		if(data.refResources.contains(locRefRes)) return;
		if(data.writeJson) {
			writeJsonFile(baseFileName, res);
		} else {
			writeXmlFile(baseFileName, res);
		}
		data.refResources.add(locRefRes);
	}
	
	/**
	 * This method is used to import resources from serialized files.
	 * 
	 * We distinguish three cases
	 * <ul>
	 * 	<li>c == null: in this case we assume that the parameter directoryName indeed is the path to a directory,
	 * 		which contains .ogx files, which we import into the system (on clean start)
	 * <li>c != null && c.overwriteExistingBackup == false: in this case directoryName denotes a zip file in c.destinationDirectory,
	 * 		containing again .ogx files (or .xml, .ogj, .json), which we import by means of the serialization manager
	 * <li>c != null && c.overwriteExistingBackup == true: in this case directoryName denotes a single .ogx file (or similar) 
	 * 		in c.destinationDirectory, which we import again.
	 * </ul>
	 * 
	 * @param directoryPath	
	 * 		either a directory containing .ogx files (in case c == null) -> all files are imported
	 * 		or a filename of a zip file (if c != null and c.overwriteExistingBackup == false)
	 * 		or a filename of an ogx file (id != null and c.overwriteExistingBackup == true)
	 *  
	 * return null or error message
	 * @throws SecurityException if file access is not granted
	 */
	public ImportResult replayBackup(String directoryPath) {
		final File folder = new File(directoryPath);
		if(!folder.exists()) {
			final String message = "Replay directory "+folder.getPath()+" does not exist"; 
			appMan.getLogger().debug(message);
			return new ImportResult(message);
		}
		int lastImported = -1;
		int thisImported = 0;
		ImportResult result = null;
		while (thisImported > lastImported) {
			lastImported = thisImported;
			result = replayBackup0(folder);
			if (result.getNrOfFiles() < 2)
				break;
			thisImported = result.getTotalNrOfImportedResources();
		}
		return result;
	}
	
	private ImportResult replayBackup0(final File folder) {
		ExecutionTimeLogger etl = new ExecutionTimeLogger("REPLAY_ON_CLEAN", appMan);
		final AtomicInteger fileCnt = new AtomicInteger(0);
	    String message = null;
	    final List<Resource> imported = new ArrayList<>();
	    //we need to filter the zips out here
	    if (folder.isDirectory()) {
		    File[] files = folder.listFiles(new FilenameFilter() {
			    public boolean accept(File dir, String name) {
			        return !name.toLowerCase().endsWith(".zip");
			    }
			});
		 
		    Arrays.sort(files);
		    for (final File fileEntry : files) {
		    	boolean xml = isXmlFile(fileEntry);
		    	boolean json = isJsonFile(fileEntry);
		    	//TODO add ogx and ogj
		        if (!fileEntry.isDirectory() && (xml || json) ){
		        	appMan.getLogger().debug("Replay of file {}",fileEntry.getPath());
		    		try {
		    			final Resource result;
		    			if (json) 
		    				result = installJson(fileEntry.toPath());
		    			else 
		    				result = installXml(fileEntry.toPath());
		    			if (result != null) {
		    				imported.add(result);
			    			fileCnt.getAndIncrement();
		    			}
					} catch (Exception e) {
						if (message == null) {
							message = "Error reading XML/JSON file " + fileEntry.getPath() + ": " + e;
						} else {
							message.concat(", "+fileEntry.getPath());
						}
						log.error(message,e);
					}
		    		etl.intermediateStep(fileEntry.getName());
		        }
		    }
	    } else if(folder.getPath().endsWith(".zip")) { // import .zip file
	    	try (FileSystem fs = FileSystems.newFileSystem(folder.toPath(), (ClassLoader)null)) {
	    		final Path base = fs.getRootDirectories().iterator().next();
	    		// using somewhat cumbersome Java7 method here, Java8 is more convenient
	    		Files.walkFileTree(base, new FileVisitor<Path>() {

					@Override
					public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
						return FileVisitResult.CONTINUE;
//						return FileVisitResult.SKIP_SUBTREE;
					}

					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
	 	    			if (isXmlFile(file)) {
	 	    				try {
	 	    					final Resource result = installXml(file);
	 	    					if (result != null) {
	 	    						imported.add(result);
	 	    						fileCnt.getAndIncrement();
	 	    					}
	 	    				} catch (Exception e) {
	 	    					log.error("Could not import file {}: ",file,e);
	 	    				}
	 	    			}
		    			else if (isJsonFile(file)){
	 	    				try {
	 	    					final Resource result = installJson(file);
	 	    					if (result != null) {
	 	    						imported.add(result);
	 	    						fileCnt.getAndIncrement();
	 	    					}
	 	    				} catch (Exception e) {
	 	    					log.error("Could not import file {}: ",file,e);
	 	    				}
	 	    			}
	 	    			return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
						log.warn("Error accessing file {} in zip folder {}: ", file, folder, exc);
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
						return FileVisitResult.CONTINUE;
					}
				});
//	    		message = cnt.get() + " files successfully imported from zip file " + folder.getPath();
	    		log.info("{} files successfully imported from zip file {}", fileCnt.get(), folder.getPath());
	    	} catch (IOException e1) {
	    		message = "Could not access zip file "+ folder.getPath() + ": " + e1;
				log.error(message);
				return new ImportResult(message);
			}
	    } else { // import single .ogx file /* XML,json */
	    	try {
		    	if (isXmlFile(folder)) {
		    		final Resource result = installXml(folder.toPath());
		    		if (result != null) {
		    			imported.add(result);
		    			fileCnt.getAndIncrement();
		    		}
		    	}
		    	else if (isJsonFile(folder)) {
		    		final Resource result = installJson(folder.toPath());
		    		if (result != null) {
		    			imported.add(result);
		    			fileCnt.getAndIncrement();
		    		}
		    	}
	    	} catch (Exception e) {
	    		message = "Error reading XML/JSON file " + folder.getPath() + ": " + e;
	    		log.error(message,e);
	    	}
	    }
	    etl.finish();
	    return new ImportResult(message, imported, fileCnt.get());
	}
	
	private void writeJsonFile(File destFileBase, Resource res) {
		//System.out.println("Parent:"+parentDir+" prefix:"+namePrefix);
		File ownFile = new File(destFileBase.getAbsolutePath()+JSON_EXTENSION1);
		//System.out.println("File:"+ namePrefix+"_"+res.getName()+JSON_EXTENSION);
		try (PrintWriter out = new PrintWriter(ownFile, "UTF-8")) {
			out.print(appMan.getSerializationManager(20, false, true).toJson(res));
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	public static String getResourceDefaultFileName(Resource res, String namePrefix) {
		return namePrefix+"_"+res.getName();		
	}
	
	private void writeXmlFile(File destFileBase, Resource res) {
		//System.out.println("Parent:"+parentDir+" prefix:"+namePrefix);
		File ownFile = new File(destFileBase.getAbsolutePath()+XML_EXTENSION1);
		//System.out.println("File:"+ namePrefix+"_"+res.getName()+XML_EXTENSION);
		try (PrintWriter out = new PrintWriter(ownFile, "UTF-8")) {
			out.print(appMan.getSerializationManager(20, false, true).toXml(res));
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
    private Resource installJson(Path file) throws Exception {
//        try (FileInputStream fis = new FileInputStream(file); InputStreamReader in = new InputStreamReader(fis, Charset.forName("UTF-8"))) {
    	try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
    		log.debug("importing JSON file {}",file);
            return appMan.getSerializationManager().createFromJson(reader);
        } catch(InvalidResourceTypeException e) {
        	log.warn("Resource type in file {} not found",file);
        	return null;
        }
    }

    private Resource installXml(Path file) throws Exception {
    	try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
//        try (FileInputStream fis = new FileInputStream(file); InputStreamReader in = new InputStreamReader(fis, Charset.forName("UTF-8"))) {
        	log.debug("importing XML file {}",file);
        	return appMan.getSerializationManager().createFromXml(reader);
        } catch(InvalidResourceTypeException e) {
        	log.warn("Resource type in file {} not found",file);
        	return null;
        }
    }
    
    private static final boolean isXmlFile(Path file) {
        return isXmlType(file.getFileName().toString().toLowerCase());
    }
    
    private static final boolean isXmlFile(File file) {
        return isXmlType(file.getName().toLowerCase());
    }
    
    private static boolean isJsonFile(File file) {
        return isJsonType(file.getName().toLowerCase());
    }
    
    static boolean isJsonFile(Path file) {
        return isJsonType(file.getFileName().toString().toLowerCase());
    }
    
    private static final boolean isXmlType(String filename) {
    	return filename.endsWith(XML_EXTENSION1) || filename.endsWith(XML_EXTENSION2);
    }
    
    private static final boolean isJsonType(String filename) {
    	return filename.endsWith(JSON_EXTENSION1) || filename.endsWith(JSON_EXTENSION2);
    }
    
    public static class ImportResult {
    	
    	private final String message;
    	private final List<Resource> resources;
    	private final int nrFiles;
    	
    	public ImportResult(String errorMessage) {
    		this(Objects.requireNonNull(errorMessage), null, 0);
		}
    	
    	ImportResult(String message, Collection<Resource> resources, int nrFiles) {
    		this.message = message;
    		this.resources = resources == null || resources.isEmpty() ? Collections.emptyList() : new ArrayList<>(resources);
    		this.nrFiles = nrFiles;
		}
    	
    	public String getMessage() {
    		return message;
    	}
    	
    	public List<Resource> getImportedResources() {
    		return resources;
    	}
    	
    	public int getTotalNrOfImportedResources() {
    		int cnt = 0;
    		for (Resource r: resources) {
    			cnt += r.getSubResources(true).size()+1;
    		}
    		return cnt;
    	}
    	
    	public int getNrOfFiles() {
    		return nrFiles;
    	}
    	
    	@Override
    	public String toString() {
    		if (message != null)
    			return message;
    		return "Imported " + resources.size() + " resources";
    	}
    	
    }
    
    /**Move all top-level resources of the type specified into the resource list*/
    public static <T extends Resource, S extends T> int moveImportedIntoList(ResourceList<T> destList, Class<S> type,
    		ApplicationManager appMan) {
    	List<S> tops = appMan.getResourceAccess().getToplevelResources(type);
    	int count = 0;
    	for(T res: tops) {
    		destList.addDecorator(res.getName(), res);
    	}
    	return count;
    }
}
