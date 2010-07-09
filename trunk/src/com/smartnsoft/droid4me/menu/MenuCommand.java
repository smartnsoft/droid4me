/*
 * (C) Copyright 2009-20010 Smart&Soft SAS (http://www.smartnsoft.com/) and contributors.
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
 *     E2M - initial API and implementation
 *     Smart&Soft - initial API and implementation
 */

package com.smartnsoft.droid4me.menu;

import com.smartnsoft.droid4me.framework.Commands;
import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.Menu;
import android.view.MenuItem;

/**
 * In order to collect all the custom commands that are available for a business object.
 * 
 * @author Édouard Mercier
 * @since 2008.05.29
 */
public class MenuCommand<BusinessObjectClass>
{

  private static final Logger log = LoggerFactory.getInstance(MenuCommand.class);

  private static final int MENU_ICON_WIDTH = 48;

  private static final int MENU_ICON_HEIGHT = 48;

  // Taken from http://www.anddev.org/resize_and_rotate_image_-_example-t621.html
  // TODO: put in cache all already computed icons
  @Deprecated
  public static Drawable resizeIcon(Context context, int iconId)
  {
    Bitmap originalBitmap = BitmapFactory.decodeResource(context.getResources(), iconId);
    int width = originalBitmap.getWidth();
    int height = originalBitmap.getHeight();
    // Calculate the scale
    float scaleWidth = ((float) MENU_ICON_WIDTH) / width;
    float scaleHeight = ((float) MENU_ICON_HEIGHT) / height;
    // Creates a matrix for the manipulation
    Matrix matrix = new Matrix();
    // Resizes the bit map
    matrix.postScale(scaleWidth, scaleHeight);
    // Recreate the new Bitmap
    Bitmap resizedBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, width, height, matrix, true);
    // Make a Drawable from Bitmap to allow to set the BitMap to the ImageView, ImageButton or what ever
    BitmapDrawable drawable = new BitmapDrawable(resizedBitmap);
    return drawable;
  }

  public int menuId;

  public final int textId;

  public final CharSequence text;

  public final char numericalShortcut;

  public final char characterShortcut;

  public final int icon;

  public final Commands.Executable<BusinessObjectClass> executable;

  public MenuCommand(int textId, char numericalShortcut, char characterShortcut, int icon, Commands.Executable<BusinessObjectClass> executable)
  {
    this(null, textId, numericalShortcut, characterShortcut, icon, executable);
  }

  public MenuCommand(CharSequence text, char numericalShortcut, char characterShortcut, int icon, Commands.Executable<BusinessObjectClass> executable)
  {
    this(text, -1, numericalShortcut, characterShortcut, icon, executable);
  }

  public MenuCommand(CharSequence text, int textId, char numericalShortcut, char characterShortcut, int icon, Commands.Executable<BusinessObjectClass> executable)
  {
    this.text = text;
    this.textId = textId;
    this.numericalShortcut = numericalShortcut;
    this.characterShortcut = characterShortcut;
    this.icon = icon;
    this.executable = executable;
  }

  public void runAction(BusinessObjectClass businessObject)
  {
    executable.run(businessObject);
  }

  public boolean isEnabled(BusinessObjectClass businessObject)
  {
    return executable.isEnabled(businessObject);
  }

  public void computeMenuItem(Context context, Menu menu, int identifierOffset)
  {
    int theMenuId = menuId + identifierOffset;
    final MenuItem menuEntry;
    if (text == null)
    {
      menuEntry = menu.add(0, theMenuId, Menu.NONE, textId);
    }
    else
    {
      menuEntry = menu.add(0, theMenuId, Menu.NONE, text);
    }
    menuEntry.setShortcut(numericalShortcut, characterShortcut);
    // menuEntry.setIcon(Action.resizeIcon(context, icon));
    menuEntry.setIcon(icon);
    if (log.isDebugEnabled())
    {
      log.debug("Added to the menu the entry with id '" + theMenuId + "'");
    }
  }

}
