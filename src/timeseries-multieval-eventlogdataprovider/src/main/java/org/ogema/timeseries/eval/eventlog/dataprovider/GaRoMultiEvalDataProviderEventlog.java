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
package org.ogema.timeseries.eval.eventlog.dataprovider;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.model.prototypes.PhysicalElement;

import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.timeseries.eval.garo.api.base.EvaluationInputImplGaRo;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiEvalDataProvider;
import de.iwes.util.resource.ResourceHelper.DeviceInfo;
import de.iwes.widgets.html.selectiontree.SelectionItem;

/** Note that this class should inherit from {@link GaRoMultiEvalDataProvider}, but the seconds generic
 * parameter of HierarchyMultiEvalDataProviderGeneric shall be set explicitly here*/
public class GaRoMultiEvalDataProviderEventlog extends GaRoMultiEvalDataProvider<GaRoSelectionItemEventlog> {
	//HierarchyMultiEvalDataProviderGeneric<GaRoSelectionItemResource> {
	private final ApplicationManager appMan;
	private final String basePathStr = System.getProperty("org.smartrplace.analysis.backup.parser.basepath");
	private final Path basePath = Paths.get(basePathStr);
	
	private List<SelectionItem> gwSelectionItems = null;
	//private List<SelectionItem> roomSelectionItems = null;
	/*if true the gateways available are fixed and usually less entries than
	 *the original size providing all gateways that are available in the input data 
	*/
	private boolean fixGwSelectionItems = false;
	
	public GaRoMultiEvalDataProviderEventlog(ApplicationManager appMan) {
		super();
		//super(new String[]{GaRoMultiEvalDataProvider.GW_LINKINGOPTION_ID, GaRoMultiEvalDataProvider.ROOM_LINKINGOPTION_ID, "timeSeries"});
		//gwSelectionItems = new ArrayList<>();
		//gwSelectionItems.add(new GaRoSelectionItemEventlog(LOCAL_GATEWAY_ID));
		this.appMan = appMan; 
	}

	@Override
	protected List<SelectionItem> getOptions(int level, GaRoSelectionItemEventlog superItem) {
		switch(level) {
		case GaRoMultiEvalDataProvider.GW_LEVEL:
			if(fixGwSelectionItems) return gwSelectionItems;
			File[] directories = basePath.toFile().listFiles(File::isDirectory);
			gwSelectionItems = new ArrayList<>();
			for(File d: directories) {
				gwSelectionItems.add(new GaRoSelectionItemEventlog(d.getName()));
			}
			return gwSelectionItems;
		case GaRoMultiEvalDataProvider.ROOM_LEVEL:
			//if(fixRoomSelectionItems) return roomSelectionItems;
			List<SelectionItem> roomSelectionItems = new ArrayList<>();
			roomSelectionItems.add(new GaRoSelectionItemEventlog(GaRoMultiEvalDataProvider.BUILDING_OVERALL_ROOM_ID, superItem));
			return roomSelectionItems;
		case GaRoMultiEvalDataProvider.TS_LEVEL:
			//CloseableDataRecorder logData = superItem.getLogRecorder();
			List<SelectionItem> result = new ArrayList<>();
			//add resource types that are overall
			Path gwDir = basePath.resolve(superItem.id()).resolve("logs");
			File[] logFiles = gwDir.toFile().listFiles();
			for(File l: logFiles) {
				//TODO: In reality deflate and take events from the files
			}
			result.add(new GaRoSelectionItemEventlog("logFileTimes", "", superItem));
			return result;
		default:
			throw new IllegalArgumentException("unknown level");
		}
	}
	
	public static List<SingleValueResource> getRecordedDataOfDevice(PhysicalElement device) {
		throw new UnsupportedOperationException("not implemented yet!");
	}

	@Override
	public void setGatewaysOffered(List<SelectionItem> gwSelectionItemsToOffer) {
		gwSelectionItems = gwSelectionItemsToOffer;
		fixGwSelectionItems = true;
	}

	@Override
	public boolean providesMultipleGateways() {
		return false;
	}

	@Override
	public List<String> getGatewayIds() {
		return Arrays.asList(LOCAL_GATEWAY_ID);
	}

	@Override
	public EvaluationInputImplGaRo getData(List<SelectionItem> items) {
		List<TimeSeriesData> tsList = new ArrayList<>();
		List<DeviceInfo> devList = new ArrayList<>();
		for(SelectionItem item: items) {
			tsList.add(terminalOption.getElement(item));
			if(item instanceof GaRoSelectionItemEventlog) {
				DeviceInfo dev = new DeviceInfo();
				GaRoSelectionItemEventlog gitem = ((GaRoSelectionItemEventlog)item);
				dev.setDeviceName(gitem.id()+"#"+gitem.eventId);
				dev.setDeviceResourceLocation(gitem.id()+"#"+gitem.eventId);
				devList.add(dev);				
			} else devList.add(null);
		}
		return new EvaluationInputImplGaRo(tsList, devList);
	}
}
