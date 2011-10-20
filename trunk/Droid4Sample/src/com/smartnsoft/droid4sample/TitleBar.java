package com.smartnsoft.droid4sample;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Handler;
import android.util.AndroidRuntimeException;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.smartnsoft.droid4me.app.ActivityController;
import com.smartnsoft.droid4me.app.AppPublics;
import com.smartnsoft.droid4me.app.ProgressHandler;
import com.smartnsoft.droid4me.app.Smarted;
import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;

/**
 * Gathers in one place the handling of the "Android 2.0" title bar
 * 
 * @author Édouard Mercier
 * @since 2011.06.22
 */
class TitleBar
{

  static class TitleBarAttributes
      extends ProgressHandler
  {

    protected static class ActionDetail
    {

      private final View block;

      private final int resourceId;

      private final View.OnClickListener onClickListener;

      private ActionDetail(View block, int resourceId, View.OnClickListener onClickListener)
      {
        this.block = block;
        this.resourceId = resourceId;
        this.onClickListener = onClickListener;
      }

    }

    private final View view;

    private boolean visible = true;

    private View.OnClickListener refreshOnClickListener;

    private final View homeBlock;

    final ImageButton home;

    private final ImageView titleImage;

    final TextView titleText;

    private final View action1Block;

    final ImageButton action1;

    private final View action2Block;

    final ImageButton action2;

    private final View action3Block;

    final ImageButton action3;

    private final View action4Block;

    final ImageButton action4;

    private final View refreshBlock;

    private final View refreshSeparator;

    private final ImageButton refresh;

    private final ProgressBar refreshProgress;

    private final Map<ImageButton, TitleBarAttributes.ActionDetail> actions = new HashMap<ImageButton, TitleBarAttributes.ActionDetail>();

    private TitleBarAttributes.ActionDetail homeDetail;

    private CharSequence titleString;

    private int titleImageResourceId = -1;

    private boolean enabled = true;

    protected TitleBarAttributes(Activity activity, View view)
    {
      this.view = view;
      homeBlock = view.findViewById(R.id.titleBarHomeBlock);
      home = (ImageButton) view.findViewById(R.id.titleBarHome);
      titleImage = (ImageView) view.findViewById(R.id.titleBarTitleImage);
      titleText = (TextView) view.findViewById(R.id.titleBarTitleText);
      setTitle(activity.getTitle());
      action1Block = view.findViewById(R.id.titleBarAction1Block);
      action1 = (ImageButton) view.findViewById(R.id.titleBarAction1);
      action2Block = view.findViewById(R.id.titleBarAction2Block);
      action2 = (ImageButton) view.findViewById(R.id.titleBarAction2);
      action3Block = view.findViewById(R.id.titleBarAction3Block);
      action3 = (ImageButton) view.findViewById(R.id.titleBarAction3);
      action4Block = view.findViewById(R.id.titleBarAction4Block);
      action4 = (ImageButton) view.findViewById(R.id.titleBarAction4);
      refreshBlock = view.findViewById(R.id.titleBarRefreshBlock);
      refreshSeparator = view.findViewById(R.id.titleBarRefreshSeparator);
      refresh = (ImageButton) view.findViewById(R.id.titleBarRefresh);
      refreshProgress = (ProgressBar) view.findViewById(R.id.titleBarRefreshProgress);
      setShowHome(-1, null);
      setShowRefresh(null);
      setShowAction1(-1, null);
      setShowAction2(-1, null);
      setShowAction3(-1, null);
    }

    protected TitleBarAttributes(Activity activity, TitleBarAttributes titleBar)
    {
      view = titleBar.view;
      homeBlock = titleBar.homeBlock;
      home = titleBar.home;
      titleImage = titleBar.titleImage;
      titleText = titleBar.titleText;
      action1Block = titleBar.action1Block;
      action1 = titleBar.action1;
      action2Block = titleBar.action2Block;
      action2 = titleBar.action2;
      action3Block = titleBar.action3Block;
      action3 = titleBar.action3;
      action4Block = titleBar.action4Block;
      action4 = titleBar.action4;
      refreshBlock = titleBar.refreshBlock;
      refreshSeparator = titleBar.refreshSeparator;
      refresh = titleBar.refresh;
      refreshProgress = titleBar.refreshProgress;
      titleString = activity.getTitle();
    }

    public final void setEnabled(boolean enabled)
    {
      this.enabled = enabled;
      if (enabled == true)
      {
        apply();
      }
    }

    public void setTitle(CharSequence title)
    {
      if (enabled == true)
      {
        titleImage.setVisibility(View.GONE);
        titleText.setText(title);
        titleText.setVisibility(View.VISIBLE);
      }
      // We remember the values
      titleString = title;
    }

    public void setTitle(int imageResourceId)
    {
      if (enabled == true)
      {
        if (imageResourceId == -1)
        {
          return;
        }
        titleImage.setImageResource(imageResourceId);
        titleImage.setVisibility(View.VISIBLE);
        titleText.setVisibility(View.GONE);
      }
      // We remember the values
      titleImageResourceId = imageResourceId;
    }

    public void setShowHome(int iconResourceId, View.OnClickListener onClickListener)
    {
      if (iconResourceId != -1)
      {
        homeBlock.setVisibility(View.VISIBLE);
        home.setImageResource(iconResourceId);
      }
      else
      {
        homeBlock.setVisibility(View.GONE);
        home.setImageDrawable(null);
      }
      home.setEnabled(onClickListener != null);
      home.setOnClickListener(onClickListener);

      // We remember the values
      homeDetail = new TitleBarAttributes.ActionDetail(null, iconResourceId, onClickListener);
    }

    public void setShowAction1(int iconResourceId, View.OnClickListener onClickListener)
    {
      setShowAction(action1Block, action1, iconResourceId, onClickListener);
    }

    public void setShowAction2(int iconResourceId, View.OnClickListener onClickListener)
    {
      setShowAction(action2Block, action2, iconResourceId, onClickListener);
    }

    public void setShowAction3(int iconResourceId, View.OnClickListener onClickListener)
    {
      setShowAction(action3Block, action3, iconResourceId, onClickListener);
    }

    public void setShowAction4(int iconResourceId, View.OnClickListener onClickListener)
    {
      setShowAction(action4Block, action4, iconResourceId, onClickListener);
    }

    protected void setShowAction(View actionBlockView, ImageButton actionButton, int iconResourceId, View.OnClickListener onClickListener)
    {
      if (enabled == true)
      {
        if (iconResourceId != -1)
        {
          actionButton.setImageResource(iconResourceId);
        }
        else
        {
          actionButton.setImageDrawable(null);
        }
        actionBlockView.setVisibility(onClickListener != null ? View.VISIBLE : View.GONE);
        actionButton.setOnClickListener(onClickListener);
      }

      // We remember the values
      if (onClickListener == null)
      {
        actions.remove(actionButton);
      }
      else
      {
        actions.put(actionButton, new TitleBarAttributes.ActionDetail(actionBlockView, iconResourceId, onClickListener));
      }
    }

    public void setShowRefresh(View.OnClickListener onClickListener)
    {
      if (enabled == true)
      {
        refreshBlock.setVisibility(View.VISIBLE);
        refreshSeparator.setVisibility(onClickListener != null ? View.VISIBLE : View.INVISIBLE);
        refresh.setVisibility(onClickListener != null ? View.VISIBLE : View.INVISIBLE);
        refresh.setOnClickListener(onClickListener);
      }
      // We remember the listener
      this.refreshOnClickListener = onClickListener;
    }

    public void onProgress(boolean isLoading)
    {
      toggleRefresh(isLoading);
    }

    public void toggleRefresh(boolean isLoading)
    {
      if (enabled == true)
      {
        if (refreshOnClickListener != null)
        {
          refresh.setVisibility(isLoading == true ? View.INVISIBLE : View.VISIBLE);
        }
        refreshProgress.setVisibility(isLoading ? View.VISIBLE : View.INVISIBLE);
      }
    }

    public void toggleVisibility(Handler handler)
    {
      if (view.getParent() != null && view.getParent() instanceof View)
      {
        final View titleContainer = (View) view.getParent();
        if (visible == true)
        {
          titleContainer.setVisibility(View.INVISIBLE);
        }
        else
        {
          titleContainer.setVisibility(View.VISIBLE);
        }
        visible = !visible;
      }
    }

    @Override
    protected void dismiss(Activity activity, Object progressExtra)
    {
      toggleRefresh(false);
    }

    @Override
    protected void show(Activity activity, Object progressExtra)
    {
      toggleRefresh(true);
    }

    protected void apply()
    {
      if (log.isDebugEnabled())
      {
        log.debug("Applying the title bar");
      }
      final Map<ImageButton, TitleBarAttributes.ActionDetail> actions = forgetActions();
      for (Entry<ImageButton, TitleBarAttributes.ActionDetail> entry : actions.entrySet())
      {
        setShowAction(entry.getValue().block, entry.getKey(), entry.getValue().resourceId, entry.getValue().onClickListener);
      }
      setTitle(titleString);
      setTitle(titleImageResourceId);
      setShowHome(homeDetail == null ? -1 : homeDetail.resourceId, homeDetail == null ? null : homeDetail.onClickListener);
      setShowRefresh(refreshOnClickListener);
    }

    protected Map<ImageButton, TitleBarAttributes.ActionDetail> forgetActions()
    {
      final Map<ImageButton, TitleBarAttributes.ActionDetail> previousActions = new HashMap<ImageButton, TitleBarAttributes.ActionDetail>(actions);
      setShowAction1(-1, null);
      setShowAction2(-1, null);
      setShowAction3(-1, null);
      setShowAction4(-1, null);
      return previousActions;
    }
  }

  static interface TitleBarDiscarded
  {
  }

  static interface TitleBarSetupFeature
  {
    void onTitleBarSetup(TitleBarAttributes titleBarAttributes);
  }

  static interface TitleBarRefreshFeature
  {
    void onTitleBarRefresh();
  }

  static interface TitleBarShowLogoffFeature
  {
  }

  static interface TitleBarShowHomeFeature
      extends TitleBarShowLogoffFeature
  {
  }

  final class TitleBarAggregate
      extends AppPublics.LoadingBroadcastListener
      implements View.OnClickListener
  {

    private final boolean customTitleSupported;

    private final Intent homeActivityIntent;

    private TitleBarAttributes attributes;

    private TitleBarRefreshFeature onRefresh;

    protected boolean titleBarFeaturesSet;

    public TitleBarAggregate(Activity activity, boolean customTitleSupported, Intent homeActivityIntent)
    {
      super(activity, true);
      this.customTitleSupported = customTitleSupported;
      this.homeActivityIntent = homeActivityIntent;
    }

    TitleBarAttributes getAttributes()
    {
      return attributes;
    }

    void setAttributes(TitleBarAttributes titleBarAttributes)
    {
      attributes = titleBarAttributes;
    }

    protected void setTitleBar(Activity activity, int defaultHomeResourceId)
    {
      final TitleBarAttributes titleBarAttributes = getAttributes();

      titleBarAttributes.apply();

      if (titleBarFeaturesSet == false)
      {
        if (activity instanceof TitleBar.TitleBarSetupFeature)
        {
          final TitleBar.TitleBarSetupFeature titleBarSetupFeature = (TitleBar.TitleBarSetupFeature) activity;
          titleBarSetupFeature.onTitleBarSetup(titleBarAttributes);
        }
        if (activity instanceof TitleBar.TitleBarShowHomeFeature)
        {
          titleBarAttributes.setShowHome(defaultHomeResourceId, this);
        }
        if (activity instanceof TitleBar.TitleBarRefreshFeature)
        {
          setOnRefresh((TitleBar.TitleBarRefreshFeature) activity);
        }
        titleBarFeaturesSet = true;
      }
    }

    public void setOnRefresh(TitleBarRefreshFeature titleBarRefreshFeature)
    {
      this.onRefresh = titleBarRefreshFeature;
      attributes.setShowRefresh(titleBarRefreshFeature != null ? this : null);
    }

    @Override
    protected void onLoading(boolean isLoading)
    {
      attributes.toggleRefresh(isLoading);
    }

    public void onClick(View view)
    {
      if (view == attributes.home && homeActivityIntent != null)
      {
        getActivity().startActivity(homeActivityIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        getActivity().finish();
      }
      else if (view == attributes.refresh && onRefresh != null)
      {
        onRefresh.onTitleBarRefresh();
      }
    }

  }

  protected static final Logger log = LoggerFactory.getInstance(TitleBar.class);

  public final static String DO_NOT_APPLY_TITLE_BAR = "doNotApplyTitleBar";

  protected final Intent homeActivityIntent;

  protected final int defaultHomeResourceId;

  protected final int defaultThemeResourceId;

  public TitleBar(Intent homeActivityIntent, int defaultHomeResourceId, int defaultThemeResourceId)
  {
    this.homeActivityIntent = homeActivityIntent;
    this.defaultHomeResourceId = defaultHomeResourceId;
    this.defaultThemeResourceId = defaultThemeResourceId;
  }

  @SuppressWarnings("unchecked")
  public final boolean onLifeCycleEvent(Activity activity, ActivityController.Interceptor.InterceptorEvent event)
  {
    if (event == ActivityController.Interceptor.InterceptorEvent.onSuperCreateBefore && !(activity instanceof TitleBar.TitleBarDiscarded))
    {
      int activityTheme = 0;
      try
      {
        activityTheme = activity.getPackageManager().getActivityInfo(activity.getComponentName(), 0).theme;
      }
      catch (NameNotFoundException exception)
      {
        if (log.isErrorEnabled())
        {
          log.error("Cannot determine the activity '" + activity.getClass().getName() + "' theme", exception);
        }
      }
      activity.setTheme(activityTheme > 0 ? activityTheme : defaultThemeResourceId);
      if (activity instanceof Smarted<?>)
      {
        boolean requestWindowFeature;
        if (activity.getParent() == null)
        {
          try
          {
            requestWindowFeature = activity.requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
          }
          catch (AndroidRuntimeException exception)
          {
            // This means that the activity does not support custom titles
            return true;
          }
        }
        else
        {
          requestWindowFeature = true;
        }
        // We test whether we can customize the title bar
        final TitleBarAggregate titleBarAggregate = new TitleBarAggregate(activity, requestWindowFeature, homeActivityIntent);
        final Smarted<TitleBarAggregate> smartedActivity = (Smarted<TitleBarAggregate>) activity;
        smartedActivity.setAggregate(titleBarAggregate);
        return true;
      }
    }
    else if (event == ActivityController.Interceptor.InterceptorEvent.onContentChanged && !(activity instanceof TitleBarDiscarded))
    {
      if (activity instanceof Smarted<?>)
      {
        final Smarted<TitleBarAggregate> smartedActivity = (Smarted<TitleBarAggregate>) activity;
        final TitleBarAggregate titleBarAggregate = smartedActivity.getAggregate();
        if (titleBarAggregate != null && titleBarAggregate.customTitleSupported == true && titleBarAggregate.getAttributes() == null)
        {
          final TitleBarAttributes parentTitleBarAttributes;
          if (activity.getParent() != null && activity.getParent() instanceof Smarted<?>)
          {
            final Smarted<TitleBarAggregate> parentSmartedActivity = (Smarted<TitleBarAggregate>) activity.getParent();
            parentTitleBarAttributes = parentSmartedActivity.getAggregate().getAttributes();
          }
          else
          {
            parentTitleBarAttributes = null;
          }
          final Activity actualActivity = activity.getParent() == null ? activity : activity.getParent();
          if (parentTitleBarAttributes == null)
          {
            actualActivity.getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_bar);
            titleBarAggregate.setAttributes(new TitleBarAttributes(activity, actualActivity.findViewById(R.id.titleBar)));
          }
          else
          {
            titleBarAggregate.setAttributes(new TitleBarAttributes(activity, parentTitleBarAttributes));
          }
          if (activity.getIntent().hasExtra(TitleBar.DO_NOT_APPLY_TITLE_BAR) == true)
          {
            titleBarAggregate.getAttributes().setEnabled(false);
          }
          titleBarAggregate.setTitleBar(activity, defaultHomeResourceId);
          smartedActivity.registerBroadcastListeners(new AppPublics.BroadcastListener[] { titleBarAggregate });
        }
      }
      return true;
    }
    else if (event == ActivityController.Interceptor.InterceptorEvent.onResume && !(activity instanceof TitleBar.TitleBarDiscarded))
    {
      if (activity.getIntent().hasExtra(TitleBar.DO_NOT_APPLY_TITLE_BAR) == false)
      {
        if (activity.getParent() != null && activity instanceof Smarted<?> && activity.getParent() instanceof Smarted<?>)
        {
          final Smarted<TitleBarAggregate> smartedActivity = (Smarted<TitleBarAggregate>) activity;
          final TitleBarAggregate titleBarAggregate = smartedActivity.getAggregate();
          if (titleBarAggregate != null && titleBarAggregate.customTitleSupported == true)
          {
            titleBarAggregate.setTitleBar(activity, defaultHomeResourceId);
          }
          return true;
        }
      }
    }
    return false;
  }

}
