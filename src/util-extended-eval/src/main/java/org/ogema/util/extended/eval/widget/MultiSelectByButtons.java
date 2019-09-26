package org.ogema.util.extended.eval.widget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.WidgetStyle;
import de.iwes.widgets.api.widgets.html.HtmlItem;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.ButtonData;

public class MultiSelectByButtons extends HtmlItem {
	//currently only works for a static set for options
	protected final List<String> items;
	protected List<String> defaultSelectedItems = null;
	//protected final Map<String, Boolean> selected = new HashMap<String, Boolean>();
	protected final Map<String, Button> buttons = new HashMap<>();
	protected final StaticTable table;
	protected final WidgetStyle<Button> selectedColor;
	protected final WidgetStyle<Button> deselectedColor;
	
	public MultiSelectByButtons(List<String> items, String pid, WidgetPage<?> page) {
		this(items, pid, page, ButtonData.BOOTSTRAP_GREEN,
				ButtonData.BOOTSTRAP_LIGHT_BLUE); //use ButtonData.BOOTSTRAP_LIGHTGREY when dependency is availabel
	}
	public MultiSelectByButtons(List<String> items, String pid, WidgetPage<?> page,
			WidgetStyle<Button> selectedColor,
			WidgetStyle<Button> deselectedColor) {
		super(pid);
		this.items = items;
		this.selectedColor = selectedColor;
		this.deselectedColor = deselectedColor;
		int rows = (items.size()+1)/2;
		table = new StaticTable(rows, 2);
		int idx = 0;
		for(int row=0; row<rows; row++) {
			for(int col=0; col<2; col++) {
				if((col==1) && (idx >= items.size()))
					continue;
				String item = items.get(idx);
				//Default is all deselected here
				Button but = new Button(page, "selectButton"+idx,
						isDefaultSelected(item)?item:" "+item) {
					private static final long serialVersionUID = 1L;
					@Override
					public void onGET(OgemaHttpRequest req) {
						if(getText(req).startsWith(" ")) {
							//deselected
							removeStyle(selectedColor, req);
							addStyle(ButtonData.BOOTSTRAP_DEFAULT, req);
							addStyle(deselectedColor, req);
						} else {
							//removeStyle(ButtonData.BOOTSTRAP_DEFAULT, req);
							removeStyle(deselectedColor, req);
							addStyle(selectedColor, req);
						}
					}
					
					@Override
					public void onPrePOST(String data, OgemaHttpRequest req) {
						if(getText(req).startsWith(" ")) {
							setText(item, req);
						} else
							setText(" "+item, req);
					}
				};
				but.registerDependentWidget(but);
				//but.setDefaultMargin("3px", false, true, false, true);
				//but.setDefaultMinWidth("60px");
				//but.setDefaultPadding("100%");
				but.setDefaultWidth("100%"); //20em
				buttons.put(item, but);
				table.setContent(row, col, but);
				idx++;
			}
		}
		
	}
	
	public void setSelectItem(String item, boolean status, OgemaHttpRequest req) {
		Button but = buttons.get(item);
		if(status)  {
			but.setText(item, req);
		} else
			but.setText(" "+item, req);
	}

	public void setDefaultSelectedItemd(List<String> items) {
		defaultSelectedItems = items;
		for(Entry<String, Button> but: buttons.entrySet()) {
			boolean status = isDefaultSelected(but.getKey());
			if(status)  {
				but.getValue().setDefaultText(but.getKey());
			} else
				but.getValue().setDefaultText(" "+but.getKey());
		}
	}
	protected boolean isDefaultSelected(String item) {
		if(defaultSelectedItems == null) return false;
		return defaultSelectedItems.contains(item);
	}
	
	public List<String> getSelectedItems(OgemaHttpRequest req) {
		List<String> result = new ArrayList<>();
		for(Entry<String, Button> but: buttons.entrySet()) {
			if(!but.getValue().getText(req).startsWith(" "))
				result.add(but.getKey());
		}
		return result;
	}
	
	public StaticTable getStaticTable() {
		return table;
		
	}
}
