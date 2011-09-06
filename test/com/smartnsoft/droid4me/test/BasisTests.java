package com.smartnsoft.droid4me.test;

import java.io.File;

import org.junit.Before;

import android.util.Log;

import com.smartnsoft.droid4me.log.LoggerFactory;

/**
 * A common basis for all the tests.
 * 
 * @author Édouard Mercier
 * @since 2011.09.06
 */
public abstract class BasisTests
{

  @Before
  public void setup()
  {
    LoggerFactory.logLevel = Log.DEBUG;
  }

  protected final File getTemporaryDirectory()
  {
    final File temporaryDirectory = new File("tmp");
    return temporaryDirectory;
  }

}
