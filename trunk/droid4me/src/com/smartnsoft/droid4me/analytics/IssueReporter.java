/*
 * (C) Copyright 2009-2013 Smart&Soft SAS (http://www.smartnsoft.com/) and contributors.
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

package com.smartnsoft.droid4me.analytics;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Debug;
import android.os.Environment;

import com.smartnsoft.droid4me.app.ActivityController;

/**
 * A utility class which enables to report issues resulting from {@link Throwable} occurrences.
 * 
 * @author Édouard Mercier
 * @since 2013.12.23
 */
public class IssueReporter
    extends ActivityController.IssueAnalyzer
{

  /**
   * An entry regarding an {@link OutOfMemoryError} occurrence.
   */
  public static final class OutOfMemoryErrorEntry
  {

    /**
     * The name of the occurrence.
     */
    public final String name;

    /**
     * The location of the archive file holding the {@link OutOfMemoryError} error details.
     */
    public final String dumpFilePath;

    protected OutOfMemoryErrorEntry(String name, String dumpFilePath)
    {
      this.name = name;
      this.dumpFilePath = dumpFilePath;
    }

  }

  protected final AnalyticsSender<?> analyticsSender;

  /**
   * @param context
   *          the {@link Application} context
   * @param analyticsSender
   *          optional (may be {@code null}), used to report issues
   */
  public IssueReporter(Context context, AnalyticsSender<?> analyticsSender)
  {
    super(context);
    this.analyticsSender = analyticsSender;
  }

  @Override
  public boolean handleIssue(Throwable throwable)
  {
    if (ActivityController.IssueAnalyzer.isAMemoryProblem(throwable) == true)
    {
      handleOutOfMemoryError((OutOfMemoryError) throwable);
      return true;
    }
    else
    {
      reportAnalyticsError(throwable);
      return false;
    }
  }

  protected final void reportAnalyticsError(Throwable throwable)
  {
    if (analyticsSender != null)
    {
      analyticsSender.logError(context, throwable.getClass().getName(), null);
    }
  }

  public final void handleOutOfMemoryError(OutOfMemoryError throwable)
  {
    reportAnalyticsError(throwable);
    // We first run a garbage collection, in the hope to free some memory ;(
    System.gc();
    final String entry = context.getPackageName() + "-outofmemory-" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
    final String fileName = entry + ".hprof";
    final File hprofFile = new File(Environment.getExternalStorageDirectory(), fileName);
    if (log.isErrorEnabled())
    {
      log.error("A memory saturation issue has been detected: dumping the memory usage to file '" + hprofFile.getAbsolutePath() + "'");
    }
    try
    {
      Debug.dumpHprofData(hprofFile.getAbsolutePath());

      // We now compress the generated .hprof file, but also create a .zip with other contextual entries
      final File zipFile = new File(hprofFile.getAbsolutePath() + ".zip");
      if (log.isDebugEnabled())
      {
        log.debug("Creating an archive of the hprof file to '" + zipFile.getAbsolutePath() + "'");
      }
      final ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFile));
      addZipEntry(hprofFile.getName(), new FileInputStream(hprofFile), zipOutputStream);
      {
        // We keep a reference to the context
        final IssueContext issueContext = new IssueContext(context);
        addZipEntry("context.txt", new ByteArrayInputStream(issueContext.toHumanString().getBytes()), zipOutputStream);
      }
      {
        final StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        final String stackTrace = writer.toString();
        addZipEntry("stackTrace.txt", new ByteArrayInputStream(stackTrace.getBytes()), zipOutputStream);
      }
      zipOutputStream.close();
      hprofFile.delete();

      // We remember the error
      final SharedPreferences sharedPreferences = computeOutOfMemoryErrorPreferences();
      final Editor editor = sharedPreferences.edit();
      editor.putString(entry, zipFile.getAbsolutePath());
      editor.commit();
    }
    catch (Throwable innerThrowable)
    {
      if (log.isErrorEnabled())
      {
        log.error("A problem occurred while attempting to dump the memory usage to file '" + hprofFile.getAbsolutePath() + "'", innerThrowable);
      }
    }
  }

  public final List<OutOfMemoryErrorEntry> getOutOfMemoryErrorEntries()
  {
    final List<OutOfMemoryErrorEntry> entries = new ArrayList<OutOfMemoryErrorEntry>();
    final SharedPreferences sharedPreferences = computeOutOfMemoryErrorPreferences();
    final Map<String, ?> all = sharedPreferences.getAll();
    for (Entry<String, ?> entry : all.entrySet())
    {
      entries.add(new OutOfMemoryErrorEntry(entry.getKey(), (String) entry.getValue()));
    }
    return entries;
  }

  public final void discardOutOfMemoryErrorEntry(OutOfMemoryErrorEntry entry)
  {
    new File(entry.dumpFilePath).delete();
    computeOutOfMemoryErrorPreferences().edit().remove(entry.name).remove(entry.name + ".file").remove(entry.name + ".details").remove(
        entry.name + ".stackTrace").commit();
  }

  private SharedPreferences computeOutOfMemoryErrorPreferences()
  {
    return context.getSharedPreferences("outofmemory.xml", Context.MODE_PRIVATE);
  }

  private void addZipEntry(String entryName, InputStream inputStream, ZipOutputStream zipOutputStream)
      throws IOException
  {
    zipOutputStream.putNextEntry(new ZipEntry(entryName));
    {
      int length;
      byte[] buffer = new byte[8192];
      while ((length = inputStream.read(buffer)) > 0)
      {
        zipOutputStream.write(buffer, 0, length);
      }
    }
    inputStream.close();
    zipOutputStream.closeEntry();
  }

}
