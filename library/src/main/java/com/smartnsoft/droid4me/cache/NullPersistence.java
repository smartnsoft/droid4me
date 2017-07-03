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

package com.smartnsoft.droid4me.cache;

import java.io.InputStream;
import java.util.Date;
import java.util.List;

import com.smartnsoft.droid4me.bo.Business;
import com.smartnsoft.droid4me.bo.Business.InputAtom;

/**
 * An implementation of {@link Persistence persistence} which stores nothing.
 * <p>
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

  /**
   * {@inheritDoc}
   */
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
  protected void computePolicyAndCleanUpInstance()
      throws Persistence.PersistenceException
  {
  }

  @Override
  protected <CleanUpPolicyClass extends Persistence.CleanUpPolicy> CleanUpPolicyClass computeCleanUpPolicy()
  {
    return null;
  }

  @Override
  protected <CleanUpPolicyClass extends Persistence.CleanUpPolicy> void cleanUpInstance(
      CleanUpPolicyClass cleanUpPolicy)
      throws PersistenceException
  {
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
  protected Date getLastUpdateInstance(String uri)
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
  protected InputStream writeInputStreamInstance(String uri, InputAtom inputAtom, boolean returnStream)
      throws Persistence.PersistenceException
  {
    return returnStream == false ? null : inputAtom.inputStream;
  }

  @Override
  protected Business.InputAtom readInputStreamInstance(String uri)
      throws Persistence.PersistenceException
  {
    return null;
  }

}
