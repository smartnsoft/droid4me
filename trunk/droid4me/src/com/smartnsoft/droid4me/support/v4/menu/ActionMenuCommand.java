package com.smartnsoft.droid4me.support.v4.menu;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuCompat;
import android.view.Menu;
import android.view.MenuItem;

import com.smartnsoft.droid4me.framework.Commands.StaticExecutable;
import com.smartnsoft.droid4me.menu.StaticMenuCommand;

/**
 * An Android compatibility library {@link android.support.v4.app.Fragment} compliant menu command which handles the {@link Fragment} new way of
 * thinking menu actions.
 * 
 * <p>
 * Warning: this class is only available for applications running under Android v1.6+, i.e. release 4+, with the compatibility library!
 * </p>
 * 
 * @author Édouard Mercier
 * @since 2012.01.04
 */
public class ActionMenuCommand
    extends StaticMenuCommand
{

  private final int actionEnum;

  public ActionMenuCommand(CharSequence text, char numericalShortcut, char characterShortcut, int actionEnum, StaticExecutable executable)
  {
    this(text, numericalShortcut, characterShortcut, -1, actionEnum, executable);
  }

  public ActionMenuCommand(CharSequence text, char numericalShortcut, char characterShortcut, int actionEnum, int icon, StaticExecutable executable)
  {
    super(text, numericalShortcut, characterShortcut, icon, executable);
    this.actionEnum = actionEnum;
  }

  public ActionMenuCommand(int textId, char numericalShortcut, char characterShortcut, int actionEnum, StaticExecutable executable)
  {
    this(textId, numericalShortcut, characterShortcut, -1, actionEnum, executable);
  }

  public ActionMenuCommand(int textId, char numericalShortcut, char characterShortcut, int icon, int actionEnum, StaticExecutable executable)
  {
    super(textId, numericalShortcut, characterShortcut, icon, executable);
    this.actionEnum = actionEnum;
  }

  @SuppressWarnings("deprecation")
  @Override
  public MenuItem computeMenuItem(Context context, Menu menu, int identifierOffset)
  {
    final MenuItem menuItem = super.computeMenuItem(context, menu, identifierOffset);
    MenuCompat.setShowAsAction(menuItem, actionEnum);
    return menuItem;
  }

}
