package org.ogema.timeseries.eval.garo.dp.csv;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.timeseries.eval.garo.api.base.EvaluationInputImplGaRo;
import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;
import de.iwes.timeseries.eval.garo.api.base.GaRoDataTypeI.Level;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiEvalDataProvider;
import de.iwes.timeseries.eval.garo.api.helper.base.GaRoEvalHelper;
import de.iwes.util.resource.ResourceHelper.DeviceInfo;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.html.selectiontree.SelectionItem;

/** This is a very simple data provider just providing data for a single gateway. If a timeseries shall
 * 	apply to a certain room the file name must have the format $Room$<roomId>.<timeSeriesId>.<subId>.csv .
 * 	If the file name has to structure like <timeSeriesId>.<subId>.csv several subId files are considered to
 *  contribute to a single timeseries. Otherwise the entire file name except for the file type ending is
 *  considered the timeseries id. If no room id is given the time series is applied to the bulding level
 *  so that only the default building overall room is used.<br>
 *  If a CSV file provides more than one time series (e.g. providing per row timeseriesID, timestamp and value)
 *  the file name should just be $Room$<roomId>.MultiTS.<subId>.csv or MultiTS.<subId>.csv .
 */
public class GaRoMultiEvalDataProviderCSV1 extends GaRoMultiEvalDataProvider<GaRoSelectionItemCSV1> { //HierarchyMultiEvalDataProviderGeneric<GaRoSelectionItemJAXB> {
	public static final String PROVIDER_ID = "GaRoMultiEvalDataProviderSQL1";
	public static final String SINGLE_GATEWAY_ID_PROPERTY = "org.ogema.timeseries.eval.garo.dp.sql1.gatewayid";
	public static final List<String> gwIds = Arrays.asList(new String[] {System.getProperty(SINGLE_GATEWAY_ID_PROPERTY)});
	public static final Path csvFileDirectory = Paths.get("../csvtimeseries");
	
	private List<SelectionItem> gwSelectionItems = null;
	
	public GaRoMultiEvalDataProviderCSV1() {
		super();
	}

	@Override
	public String id() {
		return PROVIDER_ID;
	}

	@Override
	public String label(OgemaLocale locale) {
		return "Multi-Gateway GaRo Dataprovider JAXB";
	}

	@Override
	public String description(OgemaLocale locale) {
		return "Multi-Gateway GaRo Dataprovider JAXB (base)";
	}

	@Override
	public List<SelectionItem> getOptions(int level, GaRoSelectionItemCSV1 superItem) {
		switch(level) {
		case GaRoMultiEvalDataProvider.GW_LEVEL:
			if(gwSelectionItems == null) {
				gwSelectionItems = new ArrayList<>();
				for(String gw: gwIds) gwSelectionItems.add(new GaRoSelectionItemCSV1(gw));
			}
			return gwSelectionItems;
		case GaRoMultiEvalDataProvider.ROOM_LEVEL:
			List<SelectionItem> result = new ArrayList<>();
			result.add(new GaRoSelectionItemCSV1(GaRoMultiEvalDataProvider.BUILDING_OVERALL_ROOM_ID, null, superItem));
			return result;
		case GaRoMultiEvalDataProvider.TS_LEVEL:
			//CloseableDataRecorder logData = superItem.getLogRecorder();
			List<String> recIds = superItem.getDevicePaths(); //.getLogDataIds();
			result = new ArrayList<>();
			if(superItem.resource == null) {
				for(String tsId: recIds) {
					result.add(new GaRoSelectionItemCSV1(tsId, superItem));
				}				
			} else {
				//here only use ids that belong to the room
				List<String> devicePaths = superItem.getDevicePaths();
				for(String tsId: recIds) {
					GaRoDataType gtype = GaRoEvalHelper.getDataType(tsId);
					//Gateway-specific types shall be evaluated for every room
					if(gtype != null &&  gtype.getLevel() == Level.GATEWAY) { //GaRoEvalHelper.getGatewayTypes().contains(gtype)) {
						result.add(new GaRoSelectionItemCSV1(tsId, superItem));
						continue;
					}
					for(String devE: devicePaths) {
						if(tsId.startsWith(devE)) {
							result.add(new GaRoSelectionItemCSV1(tsId, superItem));
							break;
						}
					}
				}
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
		//TODO
		return null;
		//return gatewayParser.getGatewayIds();
	}
	
	@Override
	public EvaluationInputImplGaRo getData(List<SelectionItem> items) {
		List<TimeSeriesData> tsList = new ArrayList<>();
		List<DeviceInfo> devList = new ArrayList<>();
		for(SelectionItem item: items) {
			tsList.add(terminalOption.getElement(item));
			//if(item instanceof GaRoSelectionItemJAXB) {
			//	GaRoSelectionItemJAXB jitem = (GaRoSelectionItemJAXB)item;
				String tsId = terminalOption.getElement(item).id();
				DeviceInfo dev = new DeviceInfo();
				dev.setDeviceResourceLocation(getDevicePath(tsId));
				dev.setDeviceName(getDeviceShortId(dev.getDeviceResourceLocation()));
				devList.add(dev);				
			//} else devList.add(null);
		}
		return new EvaluationInputImplGaRo(tsList, devList);
	}
	
	public static String getDevicePath(String tsId) {
		String[] subs = tsId.split("/");
		if(subs.length > 3) return subs[2];
		else return tsId; //throw new IllegalStateException("Not a valid tsId for Homematic:"+tsId);
	}
	
	public static String getDeviceShortId(String deviceLongId) {
		int len = deviceLongId.length();
		if(len < 4) return deviceLongId;
		String toTest;
		if(deviceLongId.charAt(len-2) == '_') {
			if(len < 6) return deviceLongId;
			toTest =deviceLongId.substring(len-6, len-2);
		} else toTest = deviceLongId.substring(len-4);
		return toTest;
	}
}
