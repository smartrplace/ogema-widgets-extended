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
package org.ogema.util.jsonresult.management.api;

import java.util.List;

import org.ogema.core.model.ResourceList;
import org.ogema.model.jsonresult.JsonOGEMAFileData;
import org.ogema.model.jsonresult.JsonOGEMAWorkspaceData;

import de.iwes.util.resource.ResourceHelper;

/** Manage storage of Java objects in files
 * Each file management instance can manage any objects extending T, so for a general storage
 * management set T to Object*/
public interface JsonOGEMAFileManagement<T, D extends JsonOGEMAFileData> {
	public static final String TEMPORARY_FOLDER_NAME = "temporary";
	public static final String EXPERIMENTAL_FOLDER_NAME = "experiment";
	public static final String MAJOR_FOLDER_NAME = "major";
	
	public static final String DEFAULT_WORKSPACE_NAME = "DefaultWS3";
	
	/** Register a result class so that files that store a result of this class can be opened
	 * by the management
	 */
	void registerClass(Class<? extends T> resultStructure);
	
	/** Read file to get all the data in the resource
	 * @param resultStructure the class will be registered automatically if
	 * 		not yet known
	 * @param provider may be null
	 * */
	D createFileInfo(String fileNameWithPath, Class<? extends T> resultStructure,
			String providerId);
	D createFileInfo(int status, String workSpaceRelativePath, Class<? extends T> resultStructure,
			String providerId);
	
	/** Usually a management implementation will setup a ResourceList for each workspace
	 * where all result file descriptors are collected. This list can be obtained here. If
	 * for some reason a different structure is required the method may return null and
	 * a another custom method would have to be used. 
	 */
	ResourceList<JsonOGEMAFileData> getWorkspaceFileInfo();
	
	/** Create or update list of file results in a workspace
	 * 
	 * @param referenceExperimental if true also entries for experimental files will be made,
	 * 		otherwise only major results are referenced. Temporary results are never
	 *      referenced (this could be done for single files via {@link #createFileInfo(String)}.
	 * @param deleteExisting only relevant if an existing ResourceList is provided. If true
	 * 		all entries for files not found anymore or that would not be included based on the
	 *      parameters of the call will be deleted.
	 * @return true if the resource structure was newly created or changed, false if no changes
	 * 		were made
	 * Note: This does not work like this as we need a class structure for a file
	 */
	//boolean updateWorkspaceFileInfo(boolean referenceExperimental, boolean deleteExisting);
	
	/** Read from JSON assuming that the respective result class has been registered before*/
	<M extends T> M importFromJSON(JsonOGEMAFileData fileData);
	
	/** 
	 *  
	 * @param fileNameWithPath full system file path
	 * @param structure
	 * @return
	 */
	<M extends T> M importFromJSON(String fileNameWithPath, String resultClassName);
	<M extends T> M importFromJSON(String fileNameWithPath, Class<M> resultClass);
	
	/** Save a result into a file and create a file descriptor resource.
	 * 
	 * @param result result to save. 
	 * @param typeToUse if null tlass of the result will be registered automatically if
	 * 		not yet known. If not null the class given here will be used instead. This is
	 * 		relevant if the class of the result object is extended by additional elements and
	 * 		a special class is provided that needs to be used for reading the result.
	 * @param status
	 * @param baseFileName
	 * @param overwriteIfExisting if true the baseFileName will be used to append .json if not yet there,
	 *  		but no further changes will be made. If false (default) a number will be appended to
	 *  		make the file name unique and avoid overwriting. If overwriting takes places an
	 *  		existing JsonOGEMAFileData object shall be deleted for the
	 *  		file overwritten
	 * @param fileDataList
	 * @param provider may be null
	 * @return if status is not temporary the result resource will be added to the fileDataList,
	 * 			otherwise it is added as general temporary resource (see {@link ResourceHelper#getSampleResource(Class)})
	 */
	<M extends T, N extends M> JsonOGEMAFileData saveResult(M result, Class<N> typeToUse, int status,
			String baseFileName, boolean overwriteIfExisting, String providerId);
	
	/**If workspace is set the workspace argument in all methods can be left null and this 
	 * workspace will be used as default. Custom implementations of the EvalResultManagement may
	 * also be fixed to a certain workspace, here this method would not have any effect.<br>
	 * If the ResourceList for the result descriptors for the workspace is not available yet and/or
	 * the system file folder structure is not available yet it shall be created.*/
	void setWorkspace(String workspace);
	/**
	 * 
	 * @param workspace
	 * @return data for workspace specified or current workspace if workspace parameter is null
	 */
	JsonOGEMAWorkspaceData getWorkspaceData(String workspace);
	
	/** Return all workspaces known to the management that can be accessed via
	 * {@link #getWorkspaceData(String)}
	 */
	List<JsonOGEMAWorkspaceData> getWorkspaces();
	
	/** Export workspace resource data, especially ResourceList with JsonOGEMAFileData,
	 * into JSON or XML file. Note that import shall be organized via replay-on-clean
	 * currently.
	 * @param exportTopLevelResource if true the entire JsonOGEMAFileData will be
	 * 		exported, otherwise only the current workspace
	 * @param destinationFilePath if null the base workspace folder or the 
	 * 		descriptor folder for JSON files will be used
	 * @param exportAsXML if false the export format will be JSON, otherwise XML
	 * @return true if success
	 */
	boolean exportWorkspaceFileData(boolean exportTopLevelResource, String destinationFilePath,
			boolean exportAsXML);
	/** Export descriptor for a single result file into JSON or XML file. Note that import shall be organized via replay-on-clean
	 * currently.
	 * @param descriptor data to export. If null all descriptors for which no export exists in a 
	 * 		standard file name/position shall be exported
	 * @param destinationFilePath if null the base workspace folder or the 
	 * 		descriptor folder for JSON files will be used
	 * @param exportAsXML if false the export format will be JSON, otherwise XML
	 * @return true if success
	 */
	boolean exportFileData(JsonOGEMAFileData descriptor, String destinationFilePath,
			boolean exportAsXML);
	//JsonOGEMAWorkspaceData importWorkspaceFileData(String sourceFilePath);
	
	String getFilePath(String workspace, int status, String workSpaceRelativePath);
	String getFilePathInCurrentWorkspace(JsonOGEMAFileData fileData);
	
	/** Get all file descriptor resources that saved a certain structure
	 * 
	 * @param type class to search for
	 * @return
	 */
	<M extends T> List<D> getDataOfType(Class<M> type);
	
	/** Get all file descriptor resources that are indicated to be generated by certain provider*/
	List<D> getDataOfProvider(String providerId);
	
	D getFileInfo(String fileNameWithPath);
}
