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

package com.smartnsoft.droid4me.cache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;
import android.provider.BaseColumns;

import com.smartnsoft.droid4me.bo.Business;

/**
 * Enables to store some input streams on a SQLite database.
 * 
 * @author Édouard Mercier
 * @since 2009.06.19
 */
// TODO: think on how implementing the URI usage
// TODO: think on how to limit the database size
public final class DbPersistence
    extends Persistence
{

  /**
   * The basis interface for all {@link DbPersistence} instances.
   * <p>
   * The implementing class must have a {@code public} constructor, which takes a single {@code int} as argument, which is the persistence instance
   * index.
   * </p>
   * 
   * @since 2012.03.13
   */
  public static interface DbCleanUpPolicy
      extends Persistence.CleanUpPolicy
  {

    public void cleanUp(SQLiteDatabase writeableDatabase, String tableName)
        throws SQLException;

  }

  /**
   * A simple clean up strategy, which removes the entries older that a certain of time, which is customizable through the
   * {@link LastUpdateDbCleanUpPolicy#RETENTION_DURATION_IN_MILLISECONDS} field.
   * 
   * @since 2012.03.13
   */
  public static class LastUpdateDbCleanUpPolicy
      implements DbPersistence.DbCleanUpPolicy
  {

    /**
     * Indicates for each persistence instance, when it uses this policy, how much time an entry should be kept in milliseconds.
     * <p>
     * The number of elements in this array must be equal to {@link Persistence#INSTANCES_COUNT}.
     * </p>
     */
    public static long[] RETENTION_DURATION_IN_MILLISECONDS = new long[] {};

    /**
     * The number of milliseconds each entry should be kept at most in the cache.
     */
    protected final long retentionDurationInMilliseconds;

    public LastUpdateDbCleanUpPolicy(int instanceIndex)
    {
      retentionDurationInMilliseconds = LastUpdateDbCleanUpPolicy.RETENTION_DURATION_IN_MILLISECONDS[instanceIndex];
    }

    public void cleanUp(SQLiteDatabase writeableDatabase, String tableName)
        throws SQLException
    {
      final Cursor cursor = getCursor(writeableDatabase, tableName);
      final long now = System.currentTimeMillis();
      while (cursor != null && cursor.moveToNext() == true)
      {
        if (shouldCleanUp(cursor, now) == true)
        {
          final String uri = cursor.getString(cursor.getColumnIndex(DbPersistence.CacheColumns.URI));
          if (log.isDebugEnabled())
          {
            log.debug("Removing the entry from table '" + tableName + "' corresponding to the the URI '" + uri + "'");
          }
          final long id = cursor.getLong(cursor.getColumnIndex(DbPersistence.CacheColumns._ID));
          writeableDatabase.delete(tableName, DbPersistence.CacheColumns._ID + " = " + id, null);
        }
      }
    }

    protected Cursor getCursor(SQLiteDatabase writeableDatabase, String tableName)
        throws SQLException
    {
      return writeableDatabase.rawQuery(
          "SELECT " + DbPersistence.CacheColumns._ID + ", " + DbPersistence.CacheColumns.URI + ", " + DbPersistence.CacheColumns.LAST_UPDATE + " FROM " + tableName + " ORDER BY " + DbPersistence.CacheColumns.LAST_UPDATE,
          new String[0]);
    }

    protected boolean shouldCleanUp(Cursor cursor, long now)
    {
      return cursor.getLong(cursor.getColumnIndex(DbPersistence.CacheColumns.LAST_UPDATE)) < (now - retentionDurationInMilliseconds);
    }

  }

  /**
   * Defined in order to set up the database columns.
   */
  private final static class CacheColumns
      implements BaseColumns
  {

    private static final String URI = "uri";

    private static final String CONTENTS = "contents";

    private static final String LAST_UPDATE = "lastUpdate";

    private static final String CONTEXT = "context";

    private CacheColumns()
    {
    }

  }

  /**
   * The default name of the database file.
   */
  public final static String DEFAULT_FILE_NAME = "cache.db";

  /**
   * The default name of the database table.
   */
  public final static String DEFAULT_TABLE_NAME = "cache";

  /**
   * The file names of the instances database files.
   * 
   * <p>
   * The number of elements in this array must be equal to {@link Persistence#INSTANCES_COUNT}.
   * </p>
   */
  public static String[] FILE_NAMES = new String[] { DbPersistence.DEFAULT_FILE_NAME };

  /**
   * The table names of the instances database files.
   * 
   * <p>
   * The number of elements in this array must be equal to {@link Persistence#INSTANCES_COUNT}.
   * </p>
   */
  public static String[] TABLE_NAMES = new String[] { DbPersistence.DEFAULT_TABLE_NAME };

  /**
   * The fully qualified classes names of the {@link DbPersistence.DbCleanUpPolicy clean up policies} to use. Each array element may be {@code null},
   * and in that case, no policy is attached to the corresponding instance.
   * 
   * <p>
   * The number of elements in this array must be equal to {@link Persistence#INSTANCES_COUNT}.
   * </p>
   */
  public static String[] CLEAN_UP_POLICY_FQN = new String[] { null };

  /**
   * The number of simultaneous available threads in the pool.
   */
  private static final int THREAD_POOL_DEFAULT_SIZE = 3;

  private final static ThreadPoolExecutor THREAD_POOL = new ThreadPoolExecutor(1, DbPersistence.THREAD_POOL_DEFAULT_SIZE, 5l, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactory()
  {

    private final AtomicInteger threadCount = new AtomicInteger(1);

    public Thread newThread(Runnable runnable)
    {
      final Thread thread = new Thread(runnable);
      thread.setName("droid4me-dbpersistence-thread #" + threadCount.getAndIncrement());
      return thread;
    }

  });

  /**
   * In order to put in common the databases connections.
   */
  private static Map<String, SQLiteDatabase> writeableDatabases = new HashMap<String, SQLiteDatabase>();

  /**
   * In order to put in common the databases connections.
   */
  private static Map<String, Integer> writeableDatabaseCounts = new HashMap<String, Integer>();

  private final String fileName;

  private final String tableName;

  private SQLiteDatabase writeableDatabase;

  /**
   * Defined in order to make the {@link #readInputStream(String)} method more optimized when computing its underlying SQL query.
   */
  private final String readInputStreamQuery;

  /**
   * Defined in order to make the {@link #writeInputStream()} method more optimized when determining whether to insert or update.
   */
  private SQLiteStatement writeInputStreamExistsStatement;

  private final Object writeInputStatementSyncObject = new Object();

  /**
   * Defined in order to make the {@link #getLastUpdate(String)} method more optimized.
   */
  private SQLiteStatement getLastUpdateStreamExistsStatement;

  private final Object getLastUpdateStatementSyncObject = new Object();

  /**
   * {@inheritDoc}
   */
  public DbPersistence(String storageDirectoryPath, int instanceIndex)
  {
    super(storageDirectoryPath, instanceIndex);
    this.fileName = DbPersistence.FILE_NAMES[instanceIndex];
    this.tableName = DbPersistence.TABLE_NAMES[instanceIndex];
    readInputStreamQuery = new StringBuilder("SELECT ").append(DbPersistence.CacheColumns.CONTENTS).append(", ").append(DbPersistence.CacheColumns.LAST_UPDATE).append(
        ", ").append(DbPersistence.CacheColumns.CONTEXT).append(" FROM ").append(tableName).append(" WHERE ").append(DbPersistence.CacheColumns.URI).append(
        " = ?").toString();
  }

  @Override
  protected void initializeInstance()
      throws Persistence.PersistenceException
  {
    final String dbFilePath = computeFilePath();
    if (log.isDebugEnabled())
    {
      log.debug("Initializing the database located at '" + dbFilePath + "' for the table '" + tableName + "'");
    }
    try
    {
      DbPersistence.ensureDatabaseAvailability(dbFilePath, tableName);
    }
    catch (SQLiteException exception)
    {
      if (log.isInfoEnabled())
      {
        log.info("The cache database seems to be unexisting, unavailable or corrupted: it is now re-initialized");
      }
      try
      {
        final File databaseFile = new File(dbFilePath);
        databaseFile.delete();
        databaseFile.getParentFile().mkdirs();
        DbPersistence.ensureDatabaseAvailability(dbFilePath, tableName);
      }
      catch (Exception otherException)
      {
        if (log.isErrorEnabled())
        {
          log.error("Cannot properly initialize the database: no database is available!", otherException);
        }
        throw new Persistence.PersistenceException("Cannot initialize properly", exception);
      }
    }
    try
    {
      writeableDatabase = DbPersistence.obtainDatabase(dbFilePath);
      // Ideally, this compiled statement should be computed here, but when the table is created, it seems that the calling method returns before the
      // work is done.
      // Hence, we perform some lazy instantiation
      // writeInputStreamExistsStatement = writeableDb.compileStatement("SELECT COUNT(1) FROM " + tableName + " WHERE " +
      // DbPersistence.CacheColumns.URI + " = ?");
    }
    catch (SQLiteException exception)
    {
      if (log.isErrorEnabled())
      {
        log.error("Cannot properly open the cache database: no database caching is available!", exception);
      }
      throw new Persistence.PersistenceException("Cannot initialize properly", exception);
    }
    setStorageBackendAvailable(true);
  }

  /**
   * Opens a new database if necessary, and updates the references.
   * 
   * <p>
   * This enables to share the database instances when possible.
   * </p>
   */
  private static synchronized SQLiteDatabase obtainDatabase(String filePath)
  {
    SQLiteDatabase database = DbPersistence.writeableDatabases.get(filePath);
    Integer count = DbPersistence.writeableDatabaseCounts.get(filePath);
    if (database == null)
    {
      database = SQLiteDatabase.openDatabase(filePath, null, SQLiteDatabase.OPEN_READWRITE);
      database.setLockingEnabled(true);
      DbPersistence.writeableDatabases.put(filePath, database);
      count = new Integer(0);
    }
    DbPersistence.writeableDatabaseCounts.put(filePath, count + 1);
    return database;
  }

  /**
   * Closes the shared database if necessary, and updates the references.
   * 
   * <p>
   * This enables to share the database instances when possible.
   * </p>
   * 
   * @return
   */
  private static synchronized SQLiteDatabase releaseDatabase(String filePath)
  {
    final Integer count = DbPersistence.writeableDatabaseCounts.get(filePath);
    final Integer newCount = count - 1;
    if (newCount <= 0)
    {
      final SQLiteDatabase database = DbPersistence.writeableDatabases.remove(filePath);
      DbPersistence.writeableDatabaseCounts.remove(filePath);
      return database;
    }
    else
    {
      DbPersistence.writeableDatabaseCounts.put(filePath, newCount);
      return null;
    }
  }

  private static void ensureDatabaseAvailability(String dbFilePath, String tableName)
  {
    final SQLiteDatabase database = SQLiteDatabase.openDatabase(dbFilePath, null, SQLiteDatabase.CREATE_IF_NECESSARY | SQLiteDatabase.OPEN_READWRITE);
    database.setLockingEnabled(true);
    try
    {
      final int version = database.getVersion();
      // if (log.isDebugEnabled())
      // {
      // log.debug("The cache database version is '" + version + "'");
      // }
      // Just like a "SELECT * FROM sqlite_master WHERE name = 'whatever' AND type = 'table'"
      final boolean tableExists;
      final boolean needsSchemaUpgrade;
      {
        final Cursor cursor = database.query("sqlite_master", new String[] { "name", "sql" }, "name='" + tableName + "' AND type = 'table'", null, null, null,
            null);
        try
        {
          tableExists = (cursor.moveToFirst() == true);
          if (tableExists == true)
          {
            // We retrieve the SQL statement used for creating the table
            final String sqlStatement = cursor.getString(1);
            needsSchemaUpgrade = sqlStatement.contains(DbPersistence.CacheColumns.CONTEXT) == false;
          }
          else
          {
            needsSchemaUpgrade = false;
          }
        }
        finally
        {
          cursor.close();
        }
      }
      final int expectedVersion = 2;
      if (tableExists == false || version != expectedVersion || needsSchemaUpgrade == true)
      {
        database.beginTransaction();
        try
        {
          if (tableExists == false)
          {
            if (log.isInfoEnabled())
            {
              log.info("Creating the table '" + tableName + "' in the database located at '" + dbFilePath + "' because it does not already exist");
            }
            database.execSQL("CREATE TABLE " + tableName + " (" + DbPersistence.CacheColumns._ID + " INTEGER PRIMARY KEY" + ", " + DbPersistence.CacheColumns.URI + " TEXT" + ", " + DbPersistence.CacheColumns.LAST_UPDATE + " TIMESTAMP" + ", " + DbPersistence.CacheColumns.CONTEXT + " BLOG" + ", " + DbPersistence.CacheColumns.CONTENTS + " BLOG);");
            // We create an index, so as to optimize the database performance
            database.execSQL("CREATE UNIQUE INDEX " + tableName + "_index ON " + tableName + " ( " + DbPersistence.CacheColumns.URI + " );");
          }
          else if (needsSchemaUpgrade == true)
          {
            if (log.isInfoEnabled())
            {
              log.info("Updating the table '" + tableName + "' in the database located at '" + dbFilePath + "' because its schema is out of date");
            }
            database.execSQL("ALTER TABLE " + tableName + " ADD COLUMN " + DbPersistence.CacheColumns.CONTEXT + " BLOB;");
          }
          database.setVersion(expectedVersion);
          database.setTransactionSuccessful();
        }
        finally
        {
          database.endTransaction();
        }
      }
    }
    finally
    {
      database.close();
    }
  }

  @Override
  protected List<String> getUrisInstance()
      throws Persistence.PersistenceException
  {
    Cursor cursor = null;
    try
    {
      cursor = writeableDatabase.rawQuery("SELECT " + DbPersistence.CacheColumns.URI + " FROM " + tableName, null);
      final List<String> uris = new ArrayList<String>();
      while (cursor.moveToNext() == true)
      {
        final String uri = cursor.getString(cursor.getColumnIndex(DbPersistence.CacheColumns.URI));
        uris.add(uri);
      }
      return uris;
    }
    finally
    {
      if (cursor != null)
      {
        cursor.close();
      }
    }
  }

  @Override
  protected Date getLastUpdateInstance(String uri)
      throws Persistence.PersistenceException
  {
    synchronized (getLastUpdateStatementSyncObject)
    {
      if (getLastUpdateStreamExistsStatement == null)
      {
        // Lazy instantiation, not totally thread-safe, but a work-around
        getLastUpdateStreamExistsStatement = writeableDatabase.compileStatement("SELECT " + DbPersistence.CacheColumns.LAST_UPDATE + " FROM " + tableName + " WHERE " + DbPersistence.CacheColumns.URI + " = ?");
      }
      getLastUpdateStreamExistsStatement.bindString(1, uri);
      // A single operation is bound to execute, hence no transaction is required
      final long date;
      try
      {
        date = getLastUpdateStreamExistsStatement.simpleQueryForLong();
      }
      catch (SQLiteDoneException exception)
      {
        return null;
      }
      return new Date(date);
    }
  }

  @Override
  protected Business.InputAtom readInputStreamInstance(String uri)
      throws Persistence.PersistenceException
  {
    if (log.isDebugEnabled())
    {
      log.debug("Reading from the table '" + tableName + "' the contents related to the URI '" + uri + "'");
    }
    // We do not allow null URIs
    if (uri == null)
    {
      if (log.isErrorEnabled())
      {
        log.error("It is not allowed to use a null URI: cannot read!");
      }
      throw new Persistence.PersistenceException();
    }

    final long start = System.currentTimeMillis();
    // A single database operation is bound to be executed, hence no transaction is required
    Cursor cursor;
    /*
     * final SQLiteQueryBuilder qb = new SQLiteQueryBuilder(); qb.setTables(tableName); final Cursor cursor = qb.query(writeableDb, null,
     * DbPersistence.CacheColumns.URI + " = '" + uri + "'", null, null, null, null);
     */
    try
    {
      cursor = writeableDatabase.rawQuery(readInputStreamQuery, new String[] { uri });
    }
    catch (SQLException exception)
    {
      // Cannot figure out why the first time the database is accessed once it has just been created, an exception is thrown on that query
      // Re-running it, fixes the problem ;(
      // Hence, we silently ignore the previous exception
      cursor = writeableDatabase.rawQuery(readInputStreamQuery, new String[] { uri });
    }
    try
    {
      if (cursor.moveToFirst() == false)
      {
        return null;
      }
      final byte[] contentsBlob = cursor.getBlob(cursor.getColumnIndex(DbPersistence.CacheColumns.CONTENTS));
      final byte[] contextBlob = cursor.getBlob(cursor.getColumnIndex(DbPersistence.CacheColumns.CONTEXT));
      final Serializable serializable;
      if (contextBlob != null)
      {
        try
        {
          final ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(contextBlob));
          try
          {
            serializable = (Serializable) objectInputStream.readObject();
          }
          finally
          {
            objectInputStream.close();
          }
        }
        catch (Exception exception)
        {
          throw new Persistence.PersistenceException();
        }
      }
      else
      {
        serializable = null;
      }
      final Date timestamp = new Date(cursor.getLong(cursor.getColumnIndex(DbPersistence.CacheColumns.LAST_UPDATE)));
      final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(contentsBlob);
      if (log.isDebugEnabled())
      {
        log.debug("Read from the table '" + tableName + "' the contents related to the URI '" + uri + "' in " + (System.currentTimeMillis() - start) + " ms");
      }
      return new Business.InputAtom(timestamp, byteArrayInputStream, serializable);
    }
    finally
    {
      if (cursor != null)
      {
        cursor.close();
      }
    }
  }

  @Override
  protected Business.InputAtom flushInputStreamInstance(String uri, Business.InputAtom inputAtom)
      throws Persistence.PersistenceException
  {
    return internalCacheInputStream(uri, inputAtom, false);
  }

  @Override
  protected InputStream writeInputStreamInstance(String uri, Business.InputAtom inputAtom)
      throws Persistence.PersistenceException
  {
    return internalCacheInputStream(uri, inputAtom, true).inputStream;
  }

  @Override
  protected void removeInstance(String uri)
      throws Persistence.PersistenceException
  {
    if (log.isDebugEnabled())
    {
      log.debug("Removing from the table '" + tableName + "' the contents related to the URI '" + uri + "'");
    }
    // This is a single operation, no transaction is needed
    writeableDatabase.delete(tableName, DbPersistence.CacheColumns.URI + " = '" + uri + "'", null);
  }

  @Override
  protected void computePolicyAndCleanUpInstance()
      throws Persistence.PersistenceException
  {
    final DbPersistence.DbCleanUpPolicy cleanUpPolicy = computeCleanUpPolicy();
    if (cleanUpPolicy == null)
    {
      return;
    }
    cleanUpInstance(cleanUpPolicy);
  }

  /**
   * The implementation makes use of the {@link DbPersistence#CLEAN_UP_POLICY_FQN} to decide what {@link DbPersistence.DbCleanUpPolicy clean up
   * policy} to use.
   * 
   * @see DbPersistence#CLEAN_UP_POLICY_FQN
   */
  @SuppressWarnings("unchecked")
  @Override
  protected <CleanUpPolicyClass extends Persistence.CleanUpPolicy> CleanUpPolicyClass computeCleanUpPolicy()
  {
    if (instanceIndex >= DbPersistence.CLEAN_UP_POLICY_FQN.length)
    {
      return null;
    }
    final String policyClassFqn = DbPersistence.CLEAN_UP_POLICY_FQN[instanceIndex];
    if (policyClassFqn == null)
    {
      return null;
    }
    try
    {
      final Class<?> policyClass = Class.forName(policyClassFqn);
      final DbPersistence.DbCleanUpPolicy cleanUpPolicy = (DbPersistence.DbCleanUpPolicy) policyClass.getConstructor(int.class).newInstance(instanceIndex);
      if (log.isDebugEnabled())
      {
        log.debug("Using the clean up policy implementation '" + cleanUpPolicy.getClass().getName() + "' for the persistence instance " + instanceIndex);
      }
      return (CleanUpPolicyClass) cleanUpPolicy;
    }
    catch (Exception exception)
    {
      if (log.isErrorEnabled())
      {
        log.error("Could not instantiate the Persistent CleanUpPolicy class with FQN '" + policyClassFqn + "',", exception);
      }
      return null;
    }
  }

  @Override
  protected <CleanUpPolicyClass extends Persistence.CleanUpPolicy> void cleanUpInstance(CleanUpPolicyClass cleanUpPolicy)
      throws Persistence.PersistenceException
  {
    // We know that the policy is a 'DbPersistence.DbCleanUpPolicy'
    final DbPersistence.DbCleanUpPolicy dbCleanUpPolicy = (DbCleanUpPolicy) cleanUpPolicy;
    try
    {
      dbCleanUpPolicy.cleanUp(writeableDatabase, tableName);
    }
    catch (SQLiteException exception)
    {
      if (log.isErrorEnabled())
      {
        log.error("A problem occurred while cleaning up the instance " + instanceIndex + " entries");
      }
      throw new Persistence.PersistenceException(exception);
    }
  }

  protected void clearInstance()
      throws Persistence.PersistenceException
  {
    writeableDatabase.beginTransaction();
    try
    {
      // We delete all the table rows
      // For the explanation about the "1" argument, see
      // http://developer.android.com/reference/android/database/sqlite/SQLiteDatabase.html#delete(java.lang.String, java.lang.String,
      // java.lang.String[])
      writeableDatabase.delete(tableName, "1", null);
      writeableDatabase.setTransactionSuccessful();
    }
    finally
    {
      writeableDatabase.endTransaction();
    }
  }

  /**
   * This closes the database connection if possible, i.e. when no more instance points to the same database file.
   */
  @Override
  protected void closeInstance()
      throws Persistence.PersistenceException
  {
    // TODO: we should wait for all operations to end!
    final SQLiteDatabase database = DbPersistence.releaseDatabase(computeFilePath());
    if (writeInputStreamExistsStatement != null)
    {
      writeInputStreamExistsStatement.close();
      writeInputStreamExistsStatement = null;
    }
    if (getLastUpdateStreamExistsStatement != null)
    {
      getLastUpdateStreamExistsStatement.close();
      getLastUpdateStreamExistsStatement = null;
    }
    if (database != null)
    {
      database.close();
    }
    writeableDatabase = null;
  }

  private Business.InputAtom internalCacheInputStream(final String uri, Business.InputAtom inputAtom, final boolean asynchronous)
      throws Persistence.PersistenceException
  {
    // We do not allow null URIs
    if (uri == null)
    {
      if (log.isErrorEnabled())
      {
        log.error("It is not allowed to use a null URI: cannot write!");
      }
      throw new Persistence.PersistenceException();
    }

    // We immediately duplicate the input stream
    final long start = System.currentTimeMillis();
    final ByteArrayInputStream newInputStream;
    final byte[] bytes;
    final InputStream inputStream = inputAtom.inputStream;
    if (inputStream != null)
    {
      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      try
      {
        final byte buffer[] = new byte[32768];
        int length;
        while ((length = inputStream.read(buffer)) > 0)
        {
          outputStream.write(buffer, 0, length);
        }
        bytes = outputStream.toByteArray();
        newInputStream = new ByteArrayInputStream(bytes);
      }
      catch (IOException exception)
      {
        throw new Persistence.PersistenceException("Could not persist the input stream corresponding to the URI '" + uri + "'", exception);
      }
      finally
      {
        try
        {
          outputStream.close();
        }
        catch (IOException exception)
        {
          // Does not matter
        }
      }
    }
    else
    {
      bytes = new byte[0];
      newInputStream = null;
    }
    if (log.isDebugEnabled())
    {
      log.debug("Loaded in memory the output stream related to the URI '" + uri + "' " + bytes.length + " bytes in " + (System.currentTimeMillis() - start) + " ms");
    }
    final Date timestamp = inputAtom.timestamp;
    final Serializable context = inputAtom.context;
    if (asynchronous == false)
    {
      updateDb(uri, timestamp, context, bytes, start, asynchronous);
    }
    else
    {
      DbPersistence.THREAD_POOL.execute(new Runnable()
      {
        public void run()
        {
          try
          {
            updateDb(uri, timestamp, context, bytes, start, asynchronous);
          }
          catch (Throwable throwable)
          {
            // TODO: use a listener over SQL exception, so that those problem can be handled properly
            if (log.isErrorEnabled())
            {
              log.error("An error occurred while updating asynchronously the table '" + tableName + "' the contents related to the URI '" + uri, throwable);
            }
          }
        }
      });
    }
    return new Business.InputAtom(timestamp, newInputStream, context);
  }

  private void updateDb(String uri, Date timestamp, Serializable context, byte[] bytes, long start, boolean asynchronous)
  {
    if (log.isDebugEnabled())
    {
      log.debug("Updating or inserting " + (asynchronous == true ? "asynchronously" : "synchronously") + " the table '" + tableName + "' the contents related to the URI '" + uri + "' with data of " + bytes.length + " bytes");
    }
    // We first determine whether the row should be created or inserted
    /*
     * final SQLiteQueryBuilder qb = new SQLiteQueryBuilder(); qb.setTables(tableName); final Cursor cursor = qb.query(writeableDb, null,
     * DbPersistence.CacheColumns.URI + " = '" + uri + "'", null, null, null, null);
     */
    // Optimization
    final long result;

    try
    {
      // Fixes the "android.database.sqlite.SQLiteMisuseException: library routine called out of sequence" problem!
      synchronized (writeInputStatementSyncObject)
      {
        if (writeInputStreamExistsStatement == null)
        {
          // Lazy instantiation, not totally thread-safe, but a work-around
          writeInputStreamExistsStatement = writeableDatabase.compileStatement("SELECT COUNT(1) FROM " + tableName + " WHERE " + DbPersistence.CacheColumns.URI + " = ?");
        }
        writeInputStreamExistsStatement.bindString(1, uri);
        result = writeInputStreamExistsStatement.simpleQueryForLong();
      }
      final boolean insert = result == 0;
      /*
       * final Cursor cursor = writeableDb.rawQuery("SELECT COUNT(1) FROM " + tableName + " WHERE " + DbPersistence.CacheColumns.URI + " = '" + uri +
       * "'", null); final boolean insert; try { insert = (cursor.moveToFirst() == false); } finally { cursor.close(); }
       */

      // Now, we know whether this will be an update or an insertion
      final ContentValues contentValues = new ContentValues();
      if (insert == true)
      {
        contentValues.put(DbPersistence.CacheColumns.URI, uri);
      }
      if (timestamp != null)
      {
        contentValues.put(DbPersistence.CacheColumns.LAST_UPDATE, timestamp.getTime());
      }
      contentValues.put(DbPersistence.CacheColumns.CONTENTS, bytes);
      if (context != null)
      {
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        try
        {
          objectOutputStream.writeObject(context);
          contentValues.put(DbPersistence.CacheColumns.CONTEXT, byteArrayOutputStream.toByteArray());
        }
        finally
        {
          objectOutputStream.close();
        }
      }
      if (insert == true)
      {
        writeableDatabase.insert(tableName, null, contentValues);
      }
      else
      {
        writeableDatabase.update(tableName, contentValues, DbPersistence.CacheColumns.URI + " = '" + uri + "'", null);
      }
    }
    catch (IOException exception)
    {
      throw new Persistence.PersistenceException();
    }
    if (log.isDebugEnabled())
    {
      log.debug("Wrote into the table '" + tableName + "' regarding the URI '" + uri + "' in " + (System.currentTimeMillis() - start) + " ms");
    }
  }

  private String computeFilePath()
  {
    return getStorageDirectoryPath() + "/" + fileName;
  }

}
