package org.ogema.timeseries.eval.garo.dp.csv;

import java.util.ArrayList;
import java.util.List;

import org.ogema.recordeddata.DataRecorder;
import org.ogema.serialization.jaxb.IntegerResource;
import org.ogema.serialization.jaxb.Resource;

import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiEvalDataProvider;
import de.iwes.timeseries.eval.garo.api.base.GaRoSelectionItem;

public class GaRoSelectionItemCSV1 extends GaRoSelectionItem {
	//only relevant for level GW_LEVEL
	private String gwId;
	private DataRecorder logData;
	private List<String> recIds;
	
	protected Resource resource;
	
	//only relevant for level ROOM_LEVEL, TS_LEVEL
	public GaRoSelectionItemCSV1 getGwSelectionItem() {
		return (GaRoSelectionItemCSV1) gwSelectionItem;
	}
	
	public GaRoSelectionItemCSV1(String gwId) {
		super(GaRoMultiEvalDataProvider.GW_LEVEL, gwId);
		//this.gatewayParser = gatewayParser;
		this.gwId = gwId;
	}
	public GaRoSelectionItemCSV1(String name, Resource room, GaRoSelectionItemCSV1 superSelectionItem) {
		super(GaRoMultiEvalDataProvider.ROOM_LEVEL, name);
		//this.gatewayParser = null;
		this.gwSelectionItem = superSelectionItem;
		//this.gwId = superSelectionItem.gwId;
		//this.roomId = room.getKey();
		this.resource = room;
	}
	public GaRoSelectionItemCSV1(String tsId, GaRoSelectionItemCSV1 superSelectionItem) {
		super(GaRoMultiEvalDataProvider.TS_LEVEL, tsId);
		//this.gatewayParser = null;
		this.gwId = superSelectionItem.gwId;
		this.gwSelectionItem = superSelectionItem.gwSelectionItem;
		this.roomSelectionItem = superSelectionItem;
		//this.tsId = tsId;
	}
	
	@Override
	protected List<String> getDevicePaths(GaRoSelectionItem roomSelItem) {
		//List<Resource> devices = ((GaRoSelectionItemSQL1)gwSelectionItem).getGwData().getDevicesByRoom(resource).get();
		List<String> result = new ArrayList<>();
		//for(Resource dev: devices) result.add(dev.getPath());
		return result;
	}
	
	@Override
	public TimeSeriesData getTimeSeriesData() {
		if(level == GaRoMultiEvalDataProvider.TS_LEVEL) {
			//RecordedDataStorage recData = getLogRecorder().getRecordedDataStorage(id);
			//return new TimeSeriesDataImpl(recData, id,
			//		id, InterpolationMode.STEPS);
		}
		return null;
	}
	
	//TODO: Shall be replaced
	//@Override
	protected Resource getResource() {
		if(resource == null) {
			switch(level) {
			case GaRoMultiEvalDataProvider.GW_LEVEL:
				throw new IllegalArgumentException("No gateway resource available");
			case GaRoMultiEvalDataProvider.ROOM_LEVEL:
				return resource;
			case GaRoMultiEvalDataProvider.TS_LEVEL:
				throw new UnsupportedOperationException("Access to resources of data row parents not implemented yet, but should be done!");
			}
		}
		return resource;
	}

	@Override
	public Integer getRoomType() {
		if(resource == null) return null;
		return getRoomTypeStatic(getResource());
	}
	
	public static Integer getRoomTypeStatic(Resource room) {
		Resource typeRes = room.get("type");
		if(typeRes instanceof IntegerResource)
			return ((IntegerResource)typeRes).getValue();
		return null;		
	}

	@Override
	public String getRoomName() {
		if(resource == null) return null;
		return getResource().getName();
	}

	@Override
	public String getPath() {
		if(resource == null) return null;
		return getResource().getPath();
	}
}
