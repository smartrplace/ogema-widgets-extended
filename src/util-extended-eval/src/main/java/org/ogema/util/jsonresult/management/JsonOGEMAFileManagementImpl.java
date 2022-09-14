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
package org.ogema.util.jsonresult.management;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.model.jsonresult.JsonOGEMAFileData;
import org.ogema.model.jsonresult.JsonOGEMAFileManagementData;
import org.ogema.model.jsonresult.JsonOGEMAWorkspaceData;
import org.ogema.tools.resource.util.ResourceUtils;
import org.ogema.util.jsonresult.management.api.JsonOGEMAFileManagement;
import org.ogema.util.resourcebackup.ResourceImportExportManager;

import de.iwes.timeseries.eval.api.extended.util.MultiEvaluationUtils;

public abstract class JsonOGEMAFileManagementImpl<T, D extends JsonOGEMAFileData> implements JsonOGEMAFileManagement<T, D> {
	public static final int MAX_RESULT_WITH_SAME_BASE_NAME = 99;

	protected abstract Class<D> getDescriptorType();
	
	protected D getNewDescriptor(String name) {
		return (D) currentWorkspace.fileData().addDecorator(name, getDescriptorType());		
	}
	
	final protected Map<String, Class<? extends T>> knownClasses = new HashMap<>();
	final JsonOGEMAFileManagementData appData;
	final ApplicationManager appMan;
	
	protected JsonOGEMAWorkspaceData currentWorkspace;
	
	public JsonOGEMAFileManagementImpl(JsonOGEMAFileManagementData appData, ApplicationManager appMan) {
		this.appData = appData;
		this.appMan = appMan;
		init();
	}
	public JsonOGEMAFileManagementImpl(JsonOGEMAFileManagementData appData, String basePath, ApplicationManager appMan) {
		this.appData = appData;
		this.appMan = appMan;
		initAppData(basePath);
	}
	
	public JsonOGEMAFileManagementImpl(Resource parent, String managementName, String basePath, ApplicationManager appMan) {
		this.appData = parent.addDecorator(managementName, JsonOGEMAFileManagementData.class);
		this.appMan = appMan;
		initAppData(basePath);
	}
	private void initAppData(String basePath) {
		//appData.basePath().<StringResource>create().setValue(basePath);
		appData.workspaceData().create();
		//usually we create default workspace here and activate everything afterwards
		init();
		appData.activate(true);		
	}
	
	private void init() {
		currentWorkspace = appData.lastWorkspaceUsed();
		if(!currentWorkspace.isActive()) {
			currentWorkspace = getWorkspaceData(DEFAULT_WORKSPACE_NAME);
			if(currentWorkspace == null) {
				//create default
				setWorkspace(DEFAULT_WORKSPACE_NAME);
			}
		}
		
	}
	
	@Override
	public void registerClass(Class<? extends T> resultStructure) {
		knownClasses.put(resultStructure.getName(), resultStructure);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public D getFileInfo(String fileNameWithPath) {
		for(JsonOGEMAFileData fd: currentWorkspace.fileData().getAllElements()) {
			if(getFilePathInCurrentWorkspace(fd).equals(fileNameWithPath)) return (D)fd;
		}
		return null;
	}
	
	public int getStatus(String fileNameWithPath) {
		String[] els = fileNameWithPath.split("/");
		String[] els2 = els[els.length-1].split("\\\\");
		if(els2.length == 1 && els.length == 1) throw new IllegalArgumentException("fileName contains no status element:"+fileNameWithPath); 
		String statusString;
		if(els2.length == 1) {
			els2 = els[els.length-2].split("\\\\");
			statusString = els2[els2.length-1];
		} else statusString = els2[els2.length-2];
		switch(statusString) {
		case TEMPORARY_FOLDER_NAME:
			return 1;
		case EXPERIMENTAL_FOLDER_NAME:
			return 10;
		case MAJOR_FOLDER_NAME:
			return 100;
		default:
			throw new IllegalArgumentException("fileName contains no status element:"+fileNameWithPath+" statusString:"+statusString);	
		}
	}
	
	public String getPathInWorkspace(String fileNameWithPath) {
		String[] els = fileNameWithPath.split("/");
		String[] els2 = els[els.length-1].split("\\\\");
		return els2[els2.length-1];
	}

	@Override
	public D createFileInfo(String fileNameWithPath, Class<? extends T> resultStructure,
			String providerId) {
		D result = getFileInfo(fileNameWithPath);
		if(result == null) {
			int status = getStatus(fileNameWithPath);
			String relativePath = getPathInWorkspace(fileNameWithPath);
			registerClass((Class<? extends T>) resultStructure);
			result = getNewDescriptor(ResourceUtils.getValidResourceName(relativePath));
			result.status().<IntegerResource>create().setValue(status);
			result.workSpaceRelativePath().<StringResource>create().setValue(relativePath);
			if(providerId != null) {
				result.evaluationProviderId().<StringResource>create().setValue(providerId);
			}
			result.resultClassName().<StringResource>create().setValue(resultStructure.getName());
		}
		return result;
	}

	@Override
	public D createFileInfo(int status, String workSpaceRelativePath, Class<? extends T> resultStructure,
			String providerId) {
		return createFileInfo(getFilePath(null, status, workSpaceRelativePath), resultStructure, providerId);
	}

	@Override
	public ResourceList<JsonOGEMAFileData> getWorkspaceFileInfo() {
		return currentWorkspace.fileData();
	}

	/*@Override
	public boolean updateWorkspaceFileInfo(boolean referenceExperimental, boolean deleteExisting) {
		String majorPath = getFilePath(null, 100, null);
		Path major = Paths.get(majorPath);
		Iterator<File> it = FileUtils.iterateFiles(major.toFile(), new String[] {".json"}, true);
		while(it.hasNext()) {
			File file = it.next();
			createFileInfo(file.getAbsolutePath(), resultStructure, providerId);
		}
		
		throw new UnsupportedOperationException("not implemented yet!");
	}*/

	@Override
	public <M extends T> M importFromJSON(JsonOGEMAFileData fileData) {
		return importFromJSON(getFilePathInCurrentWorkspace(fileData), fileData.resultClassName().getValue());
	}

	@Override
	public <M extends T> M importFromJSON(String fileNameWithPath, String resultClassName) {
		@SuppressWarnings("unchecked")
		Class<M> resultClass = (Class<M>) knownClasses.get(resultClassName);
		return importFromJSON(fileNameWithPath, resultClass);
	}
	@Override
	public <M extends T> M importFromJSON(String fileNameWithPath, Class<M> resultClass) {
		if(resultClass == null)
			return null;
		M result = MultiEvaluationUtils.importFromJSON(fileNameWithPath, resultClass);
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <M extends T, N extends M> D saveResult(M result, Class<N> typeToUse, int status, String baseFileName,
			boolean overwriteIfExisting, String providerId) {
		if(baseFileName.contains("/") || baseFileName.contains("\\")) {
			baseFileName.replace("/", "_");
			baseFileName.replace("\\", "_");
		}
		String destPath = getJSONFilePath(getFilePath(null, status, null), baseFileName, overwriteIfExisting);
		System.out.println("Saving result to "+destPath);
		MultiEvaluationUtils.exportToJSONFile(destPath, result);
		Class<? extends M> type;
		if(typeToUse != null) type = typeToUse;
		else type = (Class<? extends M>) result.getClass();
		D resultRes = createFileInfo(destPath, type, providerId);
		return resultRes;
	}

	public static String getJSONFilePath(String dirPath, String baseFileName, boolean overwriteIfExisting) {
		//int i = 0;
		int j = 1;
		//String newPath = "";
		String jsonFileName = baseFileName;
		Path providerPath;
		if(jsonFileName.endsWith(".json"))
			providerPath = Paths.get(dirPath+"/"+jsonFileName);
		else
			providerPath = Paths.get(dirPath+"/"+jsonFileName+"Result.json");
		if((!Files.isRegularFile(providerPath)) || overwriteIfExisting) {
			//do nothing
			//newPath = providerPath.toString();
		} else {
			while(Files.isRegularFile(providerPath) && (j <= MAX_RESULT_WITH_SAME_BASE_NAME)) { //(i < j) {
				providerPath = Paths.get(j < 10?dirPath+"/"+jsonFileName+"Result_0"+j+".json":
					dirPath+"/"+jsonFileName+"Result_"+j+".json");
				
				//if(!Files.isRegularFile(providerPath)) {
					//newPath = j < 10?jsonFileName+"Result_0"+j+".json":jsonFileName+"Result_"+j+".json";
					//i++;
				//}
				j++;
				//i++;
			}
		}
		return providerPath.toString();
	}
	
	@Override
	public void setWorkspace(String workspace) {
		JsonOGEMAWorkspaceData newWS = getWorkspaceData(workspace);
		if(newWS == null) {
			String resName = ResourceUtils.getValidResourceName(workspace);
			newWS = appData.workspaceData().addDecorator(resName,
					JsonOGEMAWorkspaceData.class);
			newWS.fileData().create();
			//newWS.workspacePath().<StringResource>create().setValue(
			//		Paths.get(appData.basePath().getValue(), resName).toString());
			newWS.name().<StringResource>create().setValue(workspace);
			if(appData.isActive()) newWS.activate(true);
		}
		currentWorkspace = newWS;
		appData.lastWorkspaceUsed().setAsReference(newWS);
	}

	@Override
	public JsonOGEMAWorkspaceData getWorkspaceData(String workspace) {
		for(JsonOGEMAWorkspaceData ws: appData.workspaceData().getAllElements()) {
			if(ws.name().getValue().equals(workspace)) return ws;
		}
		return null;
	}
	
	@Override
	public List<JsonOGEMAWorkspaceData> getWorkspaces() {
		return appData.workspaceData().getAllElements();
	}

	/** 
	 * @param status see {@link JsonOGEMAFileData#status()}
	 */
	@Override
	public String getFilePath(String workspace, int status, String workSpaceRelativePath) {
		/*String resName;
		if(workspace == null) {
			resName = currentWorkspace.getLocationResource().getName(); //here we use the resource decorator name in the ResourceList
		} else {
			resName = ResourceUtils.getValidResourceName(workspace);
		}*/
		//JsonOGEMAWorkspaceData wsLoc;
		//if(workspace == null) wsLoc = currentWorkspace;
		//else wsLoc = getWorkspaceData(workspace);
		String statusName;
		if(status == 1) statusName = TEMPORARY_FOLDER_NAME;
		else if(status == 10) statusName = EXPERIMENTAL_FOLDER_NAME;
		else if(status == 100) statusName = MAJOR_FOLDER_NAME;
		else throw new IllegalStateException("Unknown status:"+status);
		//String basePath = appData.basePath().getValue();
		//if(workSpaceRelativePath == null) return Paths.get(basePath, resName, statusName).toString();
		//return Paths.get(appData.basePath().getValue(), resName, statusName, workSpaceRelativePath).toString();
		///JsonOGEMAWorkspaceData ws = wsLoc.getLocationResource();
		//String wsPath = ws.workspacePath().getValue(); //currentWorkspace.workspacePath().getValue();
		String resName = workspace!=null?ResourceUtils.getValidResourceName(workspace):currentWorkspace.getName();
		String wsPath = Paths.get(EvalResultManagementStd.FILE_PATH, resName).toString();
		if(workSpaceRelativePath == null) return Paths.get(wsPath, statusName).toString();
		return Paths.get(wsPath, statusName, workSpaceRelativePath).toString();
	}
	
	@Override
	public String getFilePathInCurrentWorkspace(JsonOGEMAFileData fileData) {
		return getFilePath(null, fileData.status().getValue(), fileData.workSpaceRelativePath().getValue());
	}
	
	@SuppressWarnings("unchecked")
	public <M extends T> List<D> getDataOfType(Class<M> type) {
		List<D> result = new ArrayList<>();
		for(JsonOGEMAFileData fd: currentWorkspace.fileData().getAllElements()) {
			if(fd.resultClassName().getValue().equals(type.getName())) result.add((D) fd);
		}
		return result ;
	}
	
	@SuppressWarnings("unchecked")
	public List<D> getDataOfProvider(String providerId) {
		List<D> result = new ArrayList<>();
		for(JsonOGEMAFileData fd: currentWorkspace.fileData().getAllElements()) {
			if(fd.evaluationProviderId().getValue().equals(providerId)) result.add((D) fd);
		}
		return result ;
	}

	@Override
	public boolean exportWorkspaceFileData(boolean exportTopLevelResource, String destinationFilePath,
			boolean exportAsXML) {
		throw new UnsupportedOperationException("not implemented yet!");
	}
	
	@Override
	public boolean exportFileData(JsonOGEMAFileData descriptor, String destinationFilePath,
			boolean exportAsXML) {
		if(descriptor == null) {
			boolean success = true;
			for(JsonOGEMAFileData desc: currentWorkspace.fileData().getAllElements()) {
				if(!exportFileDataInternal(desc, destinationFilePath, exportAsXML))
					success = false;
			}
			return success;
		} else return exportFileDataInternal(descriptor, destinationFilePath, exportAsXML);
	}
	
	/** Requires descriptor to be set*/
	private boolean exportFileDataInternal(JsonOGEMAFileData descriptor, String destinationFilePath,
			boolean exportAsXML) {
		if(destinationFilePath == null) {
			String s = getFilePath(null, descriptor.status().getValue(),
				descriptor.workSpaceRelativePath().getValue());
			destinationFilePath = FilenameUtils.getFullPath(s);
		}
		String fileName = FilenameUtils.getBaseName(descriptor.workSpaceRelativePath().getValue());
		ResourceImportExportManager expMan = new ResourceImportExportManager(appMan);
		expMan.runBackup(destinationFilePath, fileName, false, !exportAsXML, descriptor);
		return true;
	}
}
