package com.smartnsoft.droid4me.app;

import android.app.Activity;
import android.content.SharedPreferences;

import com.smartnsoft.droid4me.framework.ActivityResultHandler.CompositeHandler;
import com.smartnsoft.droid4me.menu.MenuHandler;

/**
 * Enables to define a common additional contract when extending an {@link Activity} which is to be droid4mized.
 * 
 * @see Droid4mizer
 * @author Édouard Mercier
 * @since 2011.06.14
 */
public interface Droid4mizerInterface
{

  void onBeforeRetrievingDisplayObjects();

  MenuHandler.Composite getCompositeActionHandler();

  CompositeHandler getCompositeActivityResultHandler();

  SharedPreferences getPreferences();

}
