/*
 * (C) Copyright 2009-2014 Smart&Soft SAS (http://www.smartnsoft.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Smart&Soft - initial API and implementation
 */
package com.smartnsoft.droid4me.debug;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.smartnsoft.droid4me.app.ActivityController.Interceptor;
import com.smartnsoft.droid4me.app.Droid4mizer;
import com.smartnsoft.droid4me.download.BitmapDownloader.AnalyticsDisplayer;

/**
 * An interceptor which may be used to display debug and development indicators on the screen.
 * 
 * @author Édouard Mercier
 * @since 2014.01.18
 */
public class Droid4meDebugInterceptor
    implements Interceptor
{

  private static class Droi4mizerAnalyticsDisplayer
  {

    private final NumberFormat memoryDecimalFormat = new DecimalFormat("0.000");

    private TextView allocatedCount;

    private TextView aliveCount;

    private TextView freeMemory;

    private TextView maxMemory;

    private TextView totalMemory;

    public View getView(Context context)
    {
      final LinearLayout container = new LinearLayout(context);
      container.setOrientation(LinearLayout.VERTICAL);
      allocatedCount = createTextView(context, container);
      aliveCount = createTextView(context, container);
      aliveCount.setTextColor(Color.GREEN);
      freeMemory = createTextView(context, container);
      maxMemory = createTextView(context, container);
      totalMemory = createTextView(context, container);
      return container;
    }

    public void updateView()
    {
      final AtomicInteger allocatedCountHolder = new AtomicInteger();
      final AtomicInteger aliveCountHolder = new AtomicInteger();
      Droid4mizer.getStatistics(allocatedCountHolder, aliveCountHolder);
      allocatedCount.setText(Integer.toString(allocatedCountHolder.get()));
      aliveCount.setText(Integer.toString(aliveCountHolder.get()));
      freeMemory.setText(memoryDecimalFormat.format((float) (Runtime.getRuntime().freeMemory()) / (1024f * 1204f)) + " MB");
      maxMemory.setText(memoryDecimalFormat.format((float) (Runtime.getRuntime().maxMemory()) / (1024f * 1204f)) + " MB");
      totalMemory.setText(memoryDecimalFormat.format((float) (Runtime.getRuntime().totalMemory()) / (1024f * 1204f)) + " MB");
    }

    private TextView createTextView(Context context, final LinearLayout container)
    {
      final TextView textView = new TextView(context);
      textView.setTextColor(Color.WHITE);
      textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 8f);
      container.addView(textView);
      return textView;
    }
  }

  private final class DebugAggregate
  {

    private AnalyticsDisplayer analyticsDisplayer;

    private Droi4mizerAnalyticsDisplayer droi4mizerAnalyticsDisplayer;

    private PopupWindow popupWindow;

    public PopupWindow getPopupWindow(Context context, boolean createIfNecessary, AtomicBoolean hasBeenCreated)
    {
      if (popupWindow == null && createIfNecessary == true)
      {
        popupWindow = new PopupWindow(context);

        analyticsDisplayer = new AnalyticsDisplayer();
        final View view1 = this.analyticsDisplayer.getView(context);

        droi4mizerAnalyticsDisplayer = new Droi4mizerAnalyticsDisplayer();
        final View view2 = droi4mizerAnalyticsDisplayer.getView(context);

        final LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.HORIZONTAL);
        addView(container, "Droid4mizer", view2);
        addView(container, "BitmapDownloader", view1);
        popupWindow.setContentView(container);
        popupWindow.setWidth(panelWidth);
        popupWindow.setHeight(panelHeight);
        if (hasBeenCreated != null)
        {
          hasBeenCreated.set(true);
        }
      }
      return popupWindow;
    }

    public void onResume()
    {
      analyticsDisplayer.plug();
      droi4mizerAnalyticsDisplayer.updateView();
    }

    public void onDestroy()
    {
      if (analyticsDisplayer != null)
      {
        analyticsDisplayer.unplug();
      }
    }

    private void addView(ViewGroup container, String groupTitle, View view)
    {
      final LinearLayout intermediateContainer = new LinearLayout(container.getContext());
      intermediateContainer.setOrientation(LinearLayout.VERTICAL);
      {
        final TextView title = new TextView(container.getContext());
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 8f);
        title.setText(groupTitle);
        intermediateContainer.addView(title);
      }
      intermediateContainer.addView(view);
      final ViewGroup.MarginLayoutParams marginLayoutParams = new ViewGroup.MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
      marginLayoutParams.leftMargin = 4;
      marginLayoutParams.rightMargin = 4;
      container.addView(intermediateContainer, marginLayoutParams);
    }
  }

  public final int anchorViewResourceId;

  public final int panelWidth;

  public final int panelHeight;

  private final static Map<Activity, DebugAggregate> debugAggregates = new HashMap<Activity, DebugAggregate>();

  public Droid4meDebugInterceptor(int anchorViewResourceId, int panelWidth, int panelHeight)
  {
    this.anchorViewResourceId = anchorViewResourceId;
    this.panelWidth = panelWidth;
    this.panelHeight = panelHeight;
  }

  @Override
  public void onLifeCycleEvent(final Activity activity, Object component, InterceptorEvent event)
  {
    if (event == InterceptorEvent.onResume || event == InterceptorEvent.onDestroy)
    {
      if (event == InterceptorEvent.onDestroy)
      {
        // We remove the popup window
        final DebugAggregate debugAggregate = getDebugAggregate(false, activity);
        if (debugAggregate != null)
        {
          final PopupWindow popupWindow = debugAggregate.getPopupWindow(activity.getApplicationContext(), false, null);
          if (popupWindow != null)
          {
            popupWindow.dismiss();
          }
          debugAggregate.onDestroy();
          discardDebugAggregate(activity);
        }
      }
      else
      {
        final View anchorView = activity.findViewById(anchorViewResourceId);
        if (anchorView != null)
        {
          final DebugAggregate debugAggregate = getDebugAggregate(true, activity);
          final AtomicBoolean hasBeenCreated = new AtomicBoolean();
          final PopupWindow popupWindow = debugAggregate.getPopupWindow(activity.getApplicationContext(), true, hasBeenCreated);
          debugAggregate.onResume();
          if (hasBeenCreated.get() == true)
          {
            anchorView.post(new Runnable()
            {
              @Override
              public void run()
              {
                // We do that in the next UI thread run, so as to prevent from a "BadTokenException and says "Unable to add window -- token null
                // is not valid"", as explained at http://stackoverflow.com/questions/4187673/problems-creating-a-popup-window-in-android-activity
                popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, 0, activity.getResources().getDisplayMetrics().heightPixels - panelHeight);
              }
            });
          }
        }
      }
    }
  }

  private DebugAggregate getDebugAggregate(boolean createIfNecessary, Activity activity)
  {
    DebugAggregate debugAggregate = Droid4meDebugInterceptor.debugAggregates.get(activity);
    if (createIfNecessary == true)
    {
      if (debugAggregate == null)
      {
        debugAggregate = new DebugAggregate();
        Droid4meDebugInterceptor.debugAggregates.put(activity, debugAggregate);
      }
    }
    return debugAggregate;
  }

  private void discardDebugAggregate(Activity activity)
  {
    Droid4meDebugInterceptor.debugAggregates.remove(activity);
  }

}
