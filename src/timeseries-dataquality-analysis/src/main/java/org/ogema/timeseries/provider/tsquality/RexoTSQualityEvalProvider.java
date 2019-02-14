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
package org.ogema.timeseries.provider.tsquality;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.smartrplace.timeseries.tsquality.QualityEvalProviderBase;

import de.iwes.timeseries.eval.api.EvaluationProvider;
import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;
import de.iwes.timeseries.eval.garo.api.base.GaRoDataTypeParam;

/**
 * Evaluate basic time series qualities per gateway including gap evaluation
 */
@Service(EvaluationProvider.class)
@Component
public class RexoTSQualityEvalProvider extends QualityEvalProviderBase {
	/** Adapt these values to your provider*/
    public final static String ID = "rexo-quality_eval_provider";
    public final static String LABEL = "Rexometer Quality: Gap evaluation provider";
    public final static String DESCRIPTION = "Rexometer Quality: Provides gap evaluation, additional information in log file";

    public RexoTSQualityEvalProvider() {
        super(ID, LABEL, DESCRIPTION);
    }
    
    public static final GaRoDataTypeParam powerSubType = new GaRoDataTypeParam(GaRoDataType.PowerMeterSubphase, false);
    public static final GaRoDataTypeParam currentSubType = new GaRoDataTypeParam(GaRoDataType.PowerMeterCurrentSubphase, false);
    public static final GaRoDataTypeParam angleSubType = new GaRoDataTypeParam(GaRoDataType.PowerMeterReactiveAngleSubphase, false);
    public static final GaRoDataTypeParam gasType = new GaRoDataTypeParam(GaRoDataType.GasMeter, false);
    public static final GaRoDataTypeParam gasBatteryType = new GaRoDataTypeParam(GaRoDataType.GasMeterBatteryVoltage, false);

	@Override
	/** Provide your data types here*/
	public GaRoDataType[] getGaRoInputTypes() {
		return new GaRoDataType[] {
	        	powerType,
	        	powerSubType,
	        	currentSubType,
	        	angleSubType,
	        	gasType,
	        	gasBatteryType
		};
	}

	@Override
    protected GaRoDataTypeParam getParamType(int idxOfReqInput) {
    	switch(idxOfReqInput) {
    	case 0: return powerType;
    	case 1: return powerSubType;
    	case 2: return currentSubType;
    	case 3: return angleSubType;
    	case 4: return gasType;
    	case 5: return gasBatteryType;
    	default: throw new IllegalStateException("unsupported IDX:"+idxOfReqInput);
    	}
	}
}
