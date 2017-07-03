// The MIT License (MIT)
//
// Copyright (c) 2017 Smart&Soft
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

package com.smartnsoft.droid4me.ui;

import android.app.Activity;
import android.content.Context;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.smartnsoft.droid4me.framework.DetailsProvider.ForList;

/**
 * A simple (not expandable) smart list view.
 *
 * @author Édouard Mercier
 * @since 2008.11.14
 */
public class SimpleWrappedListView<BusinessObjectClass, ViewClass extends View>
    extends WrappedListView<BusinessObjectClass, ListView, ViewClass>
{

  private final ListView listView;

  private final ForList<BusinessObjectClass, ViewClass> forListProvider;

  private BaseAdapter adapter;

  public SimpleWrappedListView(Activity activity, ForList<BusinessObjectClass, ViewClass> forListProvider)
  {
    super(activity);
    this.forListProvider = forListProvider;
    listView = computeListView(activity);
  }

  @Override
  public final ListView getListView()
  {
    return listView;
  }

  @Override
  public final void addHeaderFooterView(boolean onTop, boolean fixed, View view)
  {
    if (onTop == true)
    {
      if (fixed == true)
      {
        if (headerAdded == false)
        {
          initializeHeader();
        }
        headerLayout.addView(view, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        headerAdded = true;
      }
      else
      {
        listView.addHeaderView(view);
      }
    }
    else
    {
      if (fixed == true)
      {
        if (footerAdded == false)
        {
          initializeFooter();
        }
        footerLayout.addView(view, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        footerAdded = true;
      }
      else
      {
        listView.addFooterView(view);
      }
    }
  }

  @Override
  public final void addLeftRightView(boolean onLeft, View view)
  {
    if (onLeft == true)
    {
      if (leftAdded == false)
      {
        initializeLeft();
      }
      // if (VERSION.SDK_INT <= 4 && leftAdded == false)
      // {
      // // This works-around the bug http://code.google.com/p/android/issues/detail?id=3484
      // leftLayout.removeViewAt(0);
      // }
      leftLayout.addView(view, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
      leftAdded = true;
    }
    else
    {
      if (rightAdded == false)
      {
        initializeRight();
      }
      // if (VERSION.SDK_INT <= 4 && rightAdded == false)
      // {
      // // This works-around the bug http://code.google.com/p/android/issues/detail?id=3484
      // rightLayout.removeViewAt(0);
      // }
      rightLayout.addView(view, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
      rightAdded = true;
    }
  }

  public void setEmptyView(View view)
  {
    if (listView.getEmptyView() != null)
    {
      listWrapperLayout.removeView(listView.getEmptyView());
    }
    listView.setEmptyView(view);
    if (view != null)
    {
      listWrapperLayout.addView(view, new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }
  }

  @Override
  public void invalidateViews()
  {
    listView.invalidateViews();
  }

  @Override
  public final void setAdapter(BaseAdapter adapter)
  {
    this.adapter = adapter;
  }

  @Override
  public final void notifyDataSetChanged(boolean businessObjectCountAndSortingUnchanged)
  {
    if (businessObjectCountAndSortingUnchanged == false)
    {
      adapter.notifyDataSetInvalidated();
    }
    else
    {
      adapter.notifyDataSetChanged();
    }
  }

  @Override
  protected ListView computeListView(Activity activity)
  {
    return new TheListView(activity);
  }

  @Override
  protected final ForList<BusinessObjectClass, ViewClass> getForListProvider()
  {
    return forListProvider;
  }

  @Override
  protected final void setAdapter()
  {
    listView.setOnItemLongClickListener(new OnItemLongClickListener()
    {
      public boolean onItemLongClick(AdapterView<?> adapter, View view, int position, long rowId)
      {
        if (getListView().isEnabled() == false)
        {
          return false;
        }
        final int actualPosition = position - listView.getHeaderViewsCount();
        if (actualPosition < 0 || actualPosition >= getFilteredObjects().size())
        {
          return false;
        }
        setSelectedObject(getFilteredObjects().get(actualPosition));
        return false;
      }
    });
    listView.setOnItemClickListener(new OnItemClickListener()
    {
      public void onItemClick(AdapterView<?> adapterView, View view, int position, long id)
      {
        if (getListView().isEnabled() == false)
        {
          return;
        }
        final int actualPosition = position - listView.getHeaderViewsCount();
        if (actualPosition < 0 || actualPosition >= getFilteredObjects().size())
        {
          return;
        }
        setSelectedObject(getFilteredObjects().get(actualPosition));

        if (getListView().isEnabled() == false)
        {
          return;
        }
        if (actualPosition >= getFilteredObjects().size())
        {
          if (log.isErrorEnabled())
          {
            log.error("The selected row " + actualPosition + " exceeds the size of the filtered business objetcs list which is " + getFilteredObjects().size());
          }
          return;
        }
        if (onEventObjectListener != null)
        {
          onEventObjectListener.onClickedObject(view, getFilteredObjects().get(actualPosition), actualPosition);
        }
      }
    });
    listView.setOnItemSelectedListener(new OnItemSelectedListener()
    {
      public void onItemSelected(AdapterView<?> adapterView, View view, int position, long rowId)
      {
        final int actualPosition = position - listView.getHeaderViewsCount();
        if (getFilteredObjects() == null)
        {
          if (log.isErrorEnabled())
          {
            log.error("The row at position " + position + " has been marked as selected whereas no filter business objects are available yet");
          }
          return;
        }

        if (actualPosition < 0 || actualPosition >= getFilteredObjects().size())
        {
          // No item is selected
          setSelectedObject(null);
          return;
        }
        setSelectedObject(getFilteredObjects().get(actualPosition));
        if (onEventObjectListener != null)
        {
          onEventObjectListener.onSelectedObject(view, getSelectedObject(), actualPosition);
        }
        // log.debug("The view at position " + actualPosition + " is now selected, and corresponds to the business object with id '" +
        // forListProvider.getObjectId(getSelectedObject()) + "'");
      }

      public void onNothingSelected(AdapterView<?> adapterView)
      {
        setSelectedObject(null);
      }
    });
    listView.setAdapter(adapter);
  }

  @Override
  protected final void setSelected(int position)
  {
    listView.setSelection(position + listView.getHeaderViewsCount());
  }

  // Inspired from http://groups.google.com/group/android-developers/browse_thread/thread/59dbe46cfbc5672f/e3a5e21754a7a725?lnk=raot
  private final class TheListView
      extends ListView
      implements OnGestureListener
  {

    private final GestureDetector gestureDetector;

    public TheListView(Context context)
    {
      super(context);
      setId(android.R.id.list);
      gestureDetector = new GestureDetector(context, this);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
      // log.debug("onTouchEvent(" + event + ")");
      if (gestureDetector.onTouchEvent(event) == true)
      {
        // log.debug("onTouchEvent() returns true");
        return true;
      }
      return super.onTouchEvent(event);
    }

    public boolean onDown(MotionEvent event)
    {
      return false;
    }

    public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY)
    {
      // log.debug("onFling(" + event1 + "," + event2 + "," + velocityX + "," + velocityY + ")");
      if (getListView().isEnabled() == true && Math.abs(velocityX) > 1500 && onEventObjectListener != null && event1 != null)
      {
        final int position = listView.pointToPosition((int) event1.getX(), (int) event1.getY());
        if (position >= 0)
        {
          // This is the only that could be found in order to find back the associated view
          final BusinessObjectClass businessObject = getFilteredObjects().get(position);
          for (int index = 0; index < listView.getChildCount(); index++)
          {
            final int positionForView = listView.getPositionForView(listView.getChildAt(index));
            if (positionForView == position)
            {
              return onEventObjectListener.onWipedObject(listView.getChildAt(index), businessObject, velocityX > 0, position);
            }
          }
        }
      }
      return false;
    }

    public void onLongPress(MotionEvent event)
    {
    }

    public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX, float distanceY)
    {
      return false;
    }

    public void onShowPress(MotionEvent event)
    {
    }

    public boolean onSingleTapUp(MotionEvent event)
    {
      return false;
    }

  }

}
