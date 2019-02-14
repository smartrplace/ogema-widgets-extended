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
package com.iee.app.evaluationofflinecontrol;

import java.util.Map;

import org.ogema.util.jsonresult.management.api.EvalResultManagement;

import de.iwes.timeseries.eval.api.DataProvider;
import de.iwes.timeseries.eval.api.EvaluationProvider;
import de.iwes.widgets.api.services.MessagingService;
import de.iwes.widgets.api.widgets.WidgetApp;
import de.iwes.widgets.api.widgets.navigation.NavigationMenu;

public interface OfflineEvalServiceAccessBase {
	public WidgetApp getWidgetApp();
	public NavigationMenu getMenu();

	public EvalResultManagement evalResultMan();
	
	public Map<String, EvaluationProvider> getEvaluations();
	public Map<String, DataProvider<?>> getDataProviders();
	public MessagingService messageService();
}
