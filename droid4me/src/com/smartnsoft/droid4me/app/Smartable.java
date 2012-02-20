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
 *     E2M - initial API and implementation
 *     Smart&Soft - initial API and implementation
 */

package com.smartnsoft.droid4me.app;

import android.app.Activity;
import android.app.Fragment;

import com.smartnsoft.droid4me.LifeCycle;

/**
 * All {@link Activity activities} and {@link Fragment fragments} of the framework implement this composite interface.
 * 
 * @author Édouard Mercier
 * @since 2011.03.04
 */
public interface Smartable<AggregateClass>
    extends Smarted<AggregateClass>, LifeCycle, AppPublics.LifeCyclePublic, AppInternals.LifeCycleInternals
{

}
