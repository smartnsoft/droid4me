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

package com.smartnsoft.droid4me.app;

import com.smartnsoft.droid4me.framework.BusinessObjectLifeCycle;
import com.smartnsoft.droid4me.framework.ForBusinessObjectImplementation;

/**
 * An activity which handles the persistence properly, which proposes a framework regarding the custom action, and which provides a framework
 * regarding the activity life cycle.
 *
 * @author Édouard Mercier
 * @since 2008.04.13
 */
@Deprecated
public abstract class ForBusinessObjectActivity<AggregateClass, BusinessObjectClass>
    extends SmartActivity<AggregateClass>
    implements BusinessObjectLifeCycle<BusinessObjectClass>
{

  private final ForBusinessObjectImplementation<BusinessObjectClass> forBusinessObjectImplementation = new ForBusinessObjectImplementation<BusinessObjectClass>()
  {

    public BusinessObjectClass retrieveBusinessObject()
        throws BusinessObjectUnavailableException
    {
      return ForBusinessObjectActivity.this.retrieveBusinessObject();
    }

    public void onBusinessObjectsRetrieved()
    {
      ForBusinessObjectActivity.this.onBusinessObjectsRetrieved();
    }

  };

  public void onRetrieveBusinessObjects()
      throws BusinessObjectUnavailableException
  {
    forBusinessObjectImplementation.retrieveBusinessObjects();
  }

  public final BusinessObjectClass getBusinessObject()
  {
    try
    {
      return forBusinessObjectImplementation.getBusinessObject();
    }
    catch (BusinessObjectUnavailableException exception)
    {
      // The nullity case should be properly handled downwards
      return null;
    }
  }

  public final boolean isBusinessObject()
  {
    return forBusinessObjectImplementation.isBusinessObject();
  }

}
