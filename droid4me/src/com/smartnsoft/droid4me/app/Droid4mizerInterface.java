/*
 * (C) Copyright 2009-2011 Smart&Soft SAS (http://www.smartnsoft.com/) and contributors.
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

package com.smartnsoft.droid4me.app;

import android.app.Activity;
import android.app.Fragment;

import com.smartnsoft.droid4me.framework.ActivityResultHandler.CompositeHandler;
import com.smartnsoft.droid4me.menu.MenuHandler;

/**
 * Enables to define a common additional contract when extending an {@link Activity}/{@link Fragment} which needs to be droid4me-ready.
 * 
 * @see Droid4mizer
 * @author Édouard Mercier
 * @since 2011.06.14
 */
public interface Droid4mizerInterface
{

  MenuHandler.Composite getCompositeActionHandler();

  CompositeHandler getCompositeActivityResultHandler();

}
