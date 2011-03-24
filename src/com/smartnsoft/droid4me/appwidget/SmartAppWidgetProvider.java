package com.smartnsoft.droid4me.appwidget;

import android.appwidget.AppWidgetProvider;

import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;

/**
 * Just wraps an {@link AppWidgetProvider Android application widget}.
 * 
 * @author Édouard Mercier
 * @since 2011.03.24
 */
public abstract class SmartAppWidgetProvider
    extends AppWidgetProvider
{

  protected static final Logger log = LoggerFactory.getInstance(SmartAppWidgetProvider.class);

}
