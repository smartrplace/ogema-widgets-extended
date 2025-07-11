package org.ogema.util.extended.eval.widget;

import org.ogema.core.model.simple.IntegerResource;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.WidgetStyle;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.button.Button;

public abstract class IntegerResourceMultiButton extends IntegerMultiButton {
	private static final long serialVersionUID = -4536262212000581377L;
	
	protected final IntegerResource res;
	
	@Override
	protected int getState(OgemaHttpRequest req) {
		return res.getValue();
	}

	@Override
	protected void setState(int state, OgemaHttpRequest req) {
		if(state == 0) {
			if(res.exists()) {
				res.setValue(0);
				res.delete();
			}
		} else
			ValueResourceHelper.setCreate(res, state);
	}
	
	public IntegerResourceMultiButton(WidgetPage<?> page, String id, IntegerResource res) {
		super(page, id);
		this.res = res;
	}

	public IntegerResourceMultiButton(WidgetPage<?> page, String id, IntegerResource res, WidgetStyle<Button>[] optionColors) {
		super(page, id, optionColors);
		this.res = res;
	}

	public IntegerResourceMultiButton(OgemaWidget parent, String id, boolean isPolling, OgemaHttpRequest req,
			IntegerResource res, WidgetStyle<Button>[] optionColors) {
		super(parent, id, isPolling, req, optionColors);
		this.res = res;
	}
}
