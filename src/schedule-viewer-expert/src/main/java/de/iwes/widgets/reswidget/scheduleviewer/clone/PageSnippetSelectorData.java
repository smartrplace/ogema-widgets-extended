package de.iwes.widgets.reswidget.scheduleviewer.clone;

import de.iwes.widgets.api.extended.html.bricks.PageSnippet;
import de.iwes.widgets.api.extended.html.bricks.PageSnippetData;

public class PageSnippetSelectorData extends PageSnippetData {
	volatile ScheduleSelector scheduleSelector;

	public PageSnippetSelectorData(PageSnippet snippet) {
		super(snippet);
	}

}
