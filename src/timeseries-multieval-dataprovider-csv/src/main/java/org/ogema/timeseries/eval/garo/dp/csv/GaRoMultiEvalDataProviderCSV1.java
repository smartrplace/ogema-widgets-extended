/**
 * ﻿Copyright 2018-2019 Fraunhofer-Gesellschaft zur Förderung der angewandten Wissenschaften e.V.
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

package org.ogema.timeseries.eval.garo.dp.csv;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ogema.core.timeseries.InterpolationMode;
import org.ogema.timeseries.eval.garo.dp.csv.util.CSVFileReader;
import org.ogema.timeseries.eval.garo.dp.csv.util.GatewayTimeseriesData;
import org.ogema.tools.timeseriesimport.api.ImportConfiguration;
import org.ogema.tools.timeseriesimport.api.ImportConfigurationBuilder;
import org.ogema.tools.timeseriesimport.api.TimeseriesImport;

import de.iwes.timeseries.eval.api.DataProvider;
import de.iwes.timeseries.eval.api.DataProviderType;
import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.timeseries.eval.garo.api.base.EvaluationInputImplGaRo;
import de.iwes.timeseries.eval.garo.api.base.GaRoDataTypeI;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiEvalDataProvider;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiEvaluationInput;
import de.iwes.timeseries.eval.garo.api.base.GaRoSelectionItem;
import de.iwes.timeseries.eval.garo.api.helper.base.GaRoEvalHelper;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider.KPIPageDefinition;
import de.iwes.util.resource.ResourceHelper.DeviceInfo;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.selectiontree.SelectionItem;


/** This is a very simple data provider just providing data for a single gateway. If a timeseries shall
 * 	apply to a certain room the file name must have the format $Room$<roomId>.<timeSeriesId>.<subId>.csv .
 * 	If the file name has to structure like <timeSeriesId>.<subId>.csv several subId files are considered to
 *  contribute to a single timeseries. Otherwise the entire file name except for the file type ending is
 *  considered the timeseries id. If no room id is given the time series is applied to the bulding level
 *  so that only the default building overall room is used.<br>
 *  If a CSV file provides more than one time series (e.g. providing per row timeseriesID, timestamp and value)
 *  the file name should just be $Room$<roomId>.MultiTS.<subId>.csv or MultiTS.<subId>.csv .<br>
 * Note that only timeseries ids that are identified by {@link GaRoEvalHelper#getDataType(String)} can be
 * processed by standard evaluation currently.
 */
public class GaRoMultiEvalDataProviderCSV1 extends GaRoMultiEvalDataProvider<GaRoSelectionItemCSV1> { //HierarchyMultiEvalDataProviderGeneric<GaRoSelectionItemJAXB> {
	public static final String ROOM_IDENT_STRING = "$Room$";
	public static final String MULTI_TS_IDENT_STRING = "MultiTS";
	
	public static final String PROVIDER_ID = "GaRoMultiEvalDataProviderCSV1";
	public static final String SINGLE_GATEWAY_ID_PROPERTY = "org.ogema.timeseries.eval.garo.dp.csv1.gatewayid";
	public static final List<String> gwIds = Arrays.asList(new String[] {System.getProperty(SINGLE_GATEWAY_ID_PROPERTY)});
	/** Directory where DataProvider searches for CSV files*/
	public static final Path csvFileDirectory = Paths.get("./csvtimeseries");
	public static final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
	
	private List<SelectionItem> gwSelectionItems = null;
	
	protected final GatewayTimeseriesData tsDataAll = new GatewayTimeseriesData();
	protected final ImportConfiguration csvConfig = ImportConfigurationBuilder.newInstance().
			setDateTimeFormat(dateTimeFormat).
			setInterpolationMode(InterpolationMode.STEPS).setDelimiter(';').setDecimalSeparator(',').build();
	protected final CSVFileReader fileReader;
	
	public GaRoMultiEvalDataProviderCSV1(TimeseriesImport csvImport) {
		super();
		fileReader = new CSVFileReader(csvFileDirectory, tsDataAll, csvConfig, csvImport);
	}

	@Override
	public String id() {
		return PROVIDER_ID;
	}

	@Override
	public String label(OgemaLocale locale) {
		return "Single-Gateway GaRo Dataprovider CSV";
	}

	@Override
	public String description(OgemaLocale locale) {
		return "Single-Gateway GaRo Dataprovider CSV (base)";
	}

	@Override
	public List<SelectionItem> getOptions(int level, GaRoSelectionItemCSV1 superItem) {
		switch(level) {
		case GaRoMultiEvalDataProvider.GW_LEVEL:
			if(gwSelectionItems == null) {
				gwSelectionItems = new ArrayList<>();
				for(String gw: gwIds) gwSelectionItems.add(new GaRoSelectionItemCSV1(gw, fileReader));
				
			}
			return gwSelectionItems;
		case GaRoMultiEvalDataProvider.ROOM_LEVEL:
			List<SelectionItem> result = new ArrayList<>();
			fileReader.getData(true);
			for(String roomId: tsDataAll.roomData.keySet()) {
				result.add(new GaRoSelectionItemCSV1(roomId, superItem));
			}
			return result;
		case GaRoMultiEvalDataProvider.TS_LEVEL:
			List<String> recIds = superItem.getDevicePaths();
			result = new ArrayList<>();
			for(String tsId: recIds) {
				result.add(new GaRoSelectionItemCSV1(tsId, superItem.id(), superItem));
			}				
			return result;
		default:
			throw new IllegalArgumentException("unknown level");
		}
	}

	/** Set gateways to be offered by this data provider instance. This is relevant if a
	 * MultiEvalation shall not evaluate all gateways in the data set
	 * 
	 * @param gwSelectionItemsToOffer must be a subset of the original result of
	 * {@link #getOptions(int, GaRoSelectionItemCSV1)} with level GW_LEVEL
	 */
	@Override
	public void setGatewaysOffered(List<SelectionItem> gwSelectionItemsToOffer) {
		//we only provide a single gateway anyways, so we have to do nothing here
	}
	
	@Override
	public boolean providesMultipleGateways() {
		return false;
	}

	public void close() {}


	@Override
	public List<String> getGatewayIds() {
		return gwIds;
	}
	
	@Override
	public EvaluationInputImplGaRo getData(List<SelectionItem> items) {
		List<TimeSeriesData> tsList = new ArrayList<>();
		List<DeviceInfo> devList = new ArrayList<>();
		for(SelectionItem item: items) {
			tsList.add(terminalOption.getElement(item));
			//String tsId = terminalOption.getElement(item).id();
			if(item instanceof GaRoSelectionItemCSV1) {
				GaRoSelectionItemCSV1 gitem = (GaRoSelectionItemCSV1)item;
				DeviceInfo dev = new DeviceInfo();
				dev.setDeviceResourceLocation(gitem.getPath());
				dev.setDeviceName(gitem.getPath());
				devList.add(dev);
			}
		}
		return new EvaluationInputImplGaRo(tsList, devList);
	}
	
	
	public GaRoMultiEvaluationInput provideMultiEvaluationInput(DataProviderType type, DataProvider<?> dataProvider,
			GaRoDataTypeI terminalDataType, List<String> topLevelIdsToEvaluate, String topLevelOptionId) {
		return new GaRoMultiEvaluationInput(type, dataProvider, terminalDataType, topLevelIdsToEvaluate, topLevelOptionId) {
			@Override
			protected boolean useDataProviderItemTerminal(GaRoSelectionItem item) {
				if(super.useDataProviderItemTerminal(item)) return true;
				return item.id().equals(terminalDataType.label(null));
			}
		};
	}

}
