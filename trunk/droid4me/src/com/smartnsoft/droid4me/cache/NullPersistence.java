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

package com.smartnsoft.droid4me.cache;

import java.io.InputStream;
import java.util.Date;
import java.util.List;

import com.smartnsoft.droid4me.bo.Business;
import com.smartnsoft.droid4me.bo.Business.InputAtom;

/**
 * An implementation of {@link Persistence persistence} which stores nothing.
 * 
 * <p>
 * This class is helpful when a {@link Persistence} instance is required, but that no data should be actually persisted.
 * </p>
 * 
 * @author Édouard Mercier
 * @since 2011.08.04
 */
public class NullPersistence
    extends Persistence
{

  public NullPersistence(int instanceIndex)
  {
    super(null, instanceIndex);
  }

  public NullPersistence(String storageDirectoryPath, int instanceIndex)
  {
    super(storageDirectoryPath, instanceIndex);
  }

  @Override
  protected void initializeInstance()
      throws Persistence.PersistenceException
  {
    setStorageBackendAvailable(true);
  }

  @Override
  protected void clearInstance()
      throws Persistence.PersistenceException
  {
  }

  @Override
  protected void closeInstance()
      throws Persistence.PersistenceException
  {
  }

  @Override
  protected void removeInstance(String uri)
      throws Persistence.PersistenceException
  {
  }

  @Override
  protected List<String> getUrisInstance()
      throws Persistence.PersistenceException
  {
    return null;
  }

  @Override
  public Date getLastUpdateInstance(String uri)
      throws Persistence.PersistenceException
  {
    return null;
  }

  @Override
  protected Business.InputAtom flushInputStreamInstance(String uri, InputAtom inputAtom)
      throws Persistence.PersistenceException
  {
    return inputAtom;
  }

  @Override
  protected InputStream writeInputStreamInstance(String uri, InputAtom inputAtom)
      throws Persistence.PersistenceException
  {
    return inputAtom.inputStream;
  }

  @Override
  protected Business.InputAtom readInputStreamInstance(String uri)
      throws Persistence.PersistenceException
  {
    return null;
  }

}
