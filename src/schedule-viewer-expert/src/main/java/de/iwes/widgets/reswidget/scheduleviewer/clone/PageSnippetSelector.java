package de.iwes.widgets.reswidget.scheduleviewer.clone;

import de.iwes.widgets.api.extended.html.bricks.PageSnippet;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

public class PageSnippetSelector extends PageSnippet {
	private static final long serialVersionUID = 1L;

	public PageSnippetSelector(WidgetPage<?> page, String id, boolean b) {
		super(page, id, b);
	}

	@Override
	public PageSnippetSelectorData createNewSession() {
		return new PageSnippetSelectorData(this);
	}

	@Override
	public PageSnippetSelectorData getData(OgemaHttpRequest req) {
		return (PageSnippetSelectorData) super.getData(req);
	}

}
