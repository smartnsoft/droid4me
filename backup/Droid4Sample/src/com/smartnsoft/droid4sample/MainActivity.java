package com.smartnsoft.droid4sample;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import com.smartnsoft.droid4me.app.WrappedSmartListActivity;
import com.smartnsoft.droid4me.framework.Commands;
import com.smartnsoft.droid4me.framework.SmartAdapters.BusinessViewWrapper;
import com.smartnsoft.droid4me.menu.StaticMenuCommand;

/**
 * The starting screen of the application.
 * 
 * @author Ãdouard Mercier
 * @since 2011.10.19
 */
public final class MainActivity
    extends WrappedSmartListActivity<TitleBar.TitleBarAggregate>
{

  private final static class Sample
  {

    private final String label;

    public Sample(String label)
    {
      this.label = label;
    }

  }

  private final static class SampleAttributes
  {

    private TextView label;

    public SampleAttributes(View view)
    {
      label = (TextView) view.findViewById(R.id.label);
    }

    public void update(Sample businessObject)
    {
      label.setText(businessObject.label);
    }

  }

  private final static class SampleWrapper
      extends BusinessViewWrapper<Sample>
  {

    public SampleWrapper(Sample businessObject)
    {
      super(businessObject);
    }

    @Override
    protected View createNewView(Activity activity, Sample businessObject)
    {
      return activity.getLayoutInflater().inflate(R.layout.main_sample, null);
    }

    @Override
    protected Object extractNewViewAttributes(Activity activity, View view, Sample businessObject)
    {
      return new SampleAttributes(view);
    }

    @Override
    protected void updateView(Activity activity, Object viewAttributes, View view, Sample businessObject, int position)
    {
      ((SampleAttributes) viewAttributes).update(businessObject);
    }

  }

  public List<? extends BusinessViewWrapper<?>> retrieveBusinessObjectsList()
      throws BusinessObjectUnavailableException
  {
    final List<BusinessViewWrapper<?>> wrappers = new ArrayList<BusinessViewWrapper<?>>();
    for (int index = 0; index < 20; index++)
    {
      wrappers.add(new SampleWrapper(new Sample("Label " + Integer.toString(index + 1))));
    }
    return wrappers;
  }

  @Override
  public List<StaticMenuCommand> getMenuCommands()
  {
    final List<StaticMenuCommand> commands = new ArrayList<StaticMenuCommand>();
    commands.add(new StaticMenuCommand(R.string.Main_menu_settings, '1', 's', android.R.drawable.ic_menu_preferences, new Commands.StaticEnabledExecutable()
    {
      @Override
      public void run()
      {
        startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
      }
    }));
    commands.add(new StaticMenuCommand(R.string.Main_menu_about, '2', 'a', android.R.drawable.ic_menu_info_details, new Commands.StaticEnabledExecutable()
    {
      @Override
      public void run()
      {
        startActivity(new Intent(getApplicationContext(), AboutActivity.class));
      }
    }));
    return commands;
  }

}
