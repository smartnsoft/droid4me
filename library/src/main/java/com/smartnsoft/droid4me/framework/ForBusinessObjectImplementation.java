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

package com.smartnsoft.droid4me.framework;

import com.smartnsoft.droid4me.LifeCycle;

/**
 * A basic implementation.
 *
 * @author Édouard Mercier
 * @since 2008.08.11
 */
@Deprecated
public abstract class ForBusinessObjectImplementation<BusinessObjectClass>
    implements BusinessObjectLifeCycle<BusinessObjectClass>
{

  private BusinessObjectClass businessObject;

  public final BusinessObjectClass getBusinessObject()
      throws LifeCycle.BusinessObjectUnavailableException
  {
    return businessObject;
  }

  public final void discardBusinessObject()
  {
    businessObject = null;
  }

  public final void retrieveBusinessObjects()
      throws LifeCycle.BusinessObjectUnavailableException
  {
    businessObject = retrieveBusinessObject();
    if (businessObject != null)
    {
      onBusinessObjectsRetrieved();
    }
    else
    {
      // There was a problem and the business object could not be retrieved
      throw new LifeCycle.BusinessObjectUnavailableException("Business object null");
    }
  }

  public final boolean isBusinessObject()
  {
    return businessObject != null;
  }

}
