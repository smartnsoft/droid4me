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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View;

import com.smartnsoft.droid4me.app.ActivityController.ExceptionHandler;
import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;

/**
 * A container which encloses various types which enable to run commands in a detached thread.
 *
 * @author Édouard Mercier
 * @since 2011.11.03
 */
public final class SmartCommands
{

  /**
   * Defines a single contract which enables to turn an exception into another one.
   *
   * @since 2011.11.03
   */
  public interface GuardedHandler
  {

    /**
     * Is responsible for turning an exception into another exception.
     *
     * @param throwable the exception that is assessed
     * @return {@code null} if and only if the method has handled the exception itself, and that it should not be propagated by the caller
     */
    Throwable onThrowable(Throwable throwable);

  }

  /**
   * Defined as a wrapper over the built-in {@link Thread.UncaughtExceptionHandler uncaught exception handlers}.
   *
   * @since 2010.07.21
   */
  public static final class SmartUncaughtExceptionHandler
      implements Thread.UncaughtExceptionHandler
  {

    /**
     * The context in which the exception handler lives.
     */
    private final Context context;

    /**
     * The previous exception handler.
     */
    private final Thread.UncaughtExceptionHandler builtinUncaughtExceptionHandler;

    /**
     * @param context                         the context which will be used to handle the exception via the {@link ActivityController#handleException(boolean, Context, Object, Throwable)}
     *                                        method
     * @param builtinUncaughtExceptionHandler the built-in uncaught exception handler that will be invoked eventually
     */
    public SmartUncaughtExceptionHandler(Context context,
        Thread.UncaughtExceptionHandler builtinUncaughtExceptionHandler)
    {
      this.context = context;
      this.builtinUncaughtExceptionHandler = builtinUncaughtExceptionHandler;
    }

    @Override
    public final void uncaughtException(Thread thread, Throwable throwable)
    {
      try
      {
        ActivityController.getInstance().handleException(false, context, null, throwable);
      }
      finally
      {
        if (builtinUncaughtExceptionHandler != null)
        {
          if (SmartApplication.log.isDebugEnabled())
          {
            SmartApplication.log.debug("Resorting to the built-in uncaught exception handler");
          }
          builtinUncaughtExceptionHandler.uncaughtException(thread, throwable);
        }
      }
    }

  }

  /**
   * Introduced so as to be able to catch the exceptions thrown in the framework thread pools.
   *
   * @since 2010.03.02
   */
  public static final class SmartThreadPoolExecutor
      extends ThreadPoolExecutor
  {

    /**
     * A flag which indicates whether the hereby {@code SmartThreadPoolExecutor} internal logs should be enabled. Logs will report execution durations
     * statistics. The default value is {@code false}.
     *
     * @see #getExecutionDurationSumInNanoseconds()
     */
    public static boolean ARE_DEBUG_LOG_ENABLED = false;

    /**
     * Records the sum of the durations of all command executions for this instance. Only fulfilled if {@link #ARE_DEBUG_LOG_ENABLED} is set to
     * {@code true}.
     */
    private long executionDurationSumInNanoseconds;

    /**
     * {@inheritDoc}
     */
    public SmartThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
        BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory)
    {
      super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    /**
     * This method does the same thing as its parent {@link ThreadPoolExecutor#execute(Runnable)} super method}.
     */
    @Override
    public void execute(Runnable command)
    {
      super.execute(command);
    }

    @Override
    protected void beforeExecute(Thread thread, Runnable runnable)
    {
      if (SmartThreadPoolExecutor.ARE_DEBUG_LOG_ENABLED == true)
      {
        new ThreadLocal<Long>().set(System.nanoTime());
      }
      super.beforeExecute(thread, runnable);
    }

    @Override
    protected void afterExecute(Runnable runnable, Throwable throwable)
    {
      super.afterExecute(runnable, throwable);
      if (SmartThreadPoolExecutor.ARE_DEBUG_LOG_ENABLED == true)
      {
        final Long startInNanoseconds = new ThreadLocal<Long>().get();
        final long durationInNanoseconds = System.nanoTime() - startInNanoseconds;
        executionDurationSumInNanoseconds += durationInNanoseconds;
        if (log.isDebugEnabled())
        {
          log.debug("A command has just end-up its execution and has lasted " + durationInNanoseconds + " ns.");
        }
      }
    }

    /**
     * Available for statistics.
     * <p>
     * <p>
     * Warning: this method will return valid data only if {@link #ARE_DEBUG_LOG_ENABLED} is set to {@code true}.
     * </p>
     *
     * @return the sum of the durations of all command executions for this instance
     * @see #ARE_DEBUG_LOG_ENABLED
     */
    public long getExecutionDurationSumInNanoseconds()
    {
      return executionDurationSumInNanoseconds;
    }

    /**
     * Executes the given command. Should be used when the {@link Runnable} may throw an {@link Exception}.
     *
     * @param guardedCommand the command to run
     * @see #submit(GuardedCommand)
     */
    public void execute(SmartCommands.GuardedCommand<?> guardedCommand)
    {
      super.execute(guardedCommand);
    }

    /**
     * Executes the given command. Should be used when the {@link Runnable} may throw an {@link Exception}.
     *
     * @param guardedCommand the command to run
     * @return the reference which enables to query the command execution status
     * @see #execute(GuardedCommand)
     */
    public Future<?> submit(SmartCommands.GuardedCommand<?> guardedCommand)
    {
      return super.submit(guardedCommand);
    }

  }

  /**
   * A {@link Runnable} used as command, which is allowed to throw an exception during its execution.
   * <p>
   * <p>
   * During the command execution, any thrown {@link Throwable} will be delivered to the
   * {@link ActivityController#registerExceptionHandler(ActivityController.ExceptionHandler) exception handler} through its
   * {@link ActivityController#handleException(boolean, Context, Object, Throwable)} method, so that it can be controlled in a central way, and not "swallowed".
   * </p>
   * <p>
   * <p>
   * It has been specifically designed for being able to run {@link Runnable} which throw exceptions within the
   * {@link SmartCommands.SmartThreadPoolExecutor}.
   * </p>
   *
   * @param <ContextClass> the class will holds the {@link Context} responsible for the command
   * @since 2010.06.08
   */
  public static abstract class GuardedCommand<ContextClass extends Context>
      implements Runnable, SmartCommands.GuardedHandler
  {

    /**
     * The context which will be used when reporting an exception to the {@link ActivityController#getInstance() exception handler}.
     */
    private final ContextClass context;

    /**
     * The component which will be used when reporting an exception to the {@link ActivityController#getInstance() exception handler}.
     */
    private final Object component;

    /**
     * The delegate that will be used, if not {@code null}, to handle any exception thrown by the command.
     */
    private SmartCommands.GuardedHandler delegate;

    /**
     * Equivalent to calling {@code SmartCommands.GuardedCommand#GuardedCommand(Context, SmartCommands.GuardedHandler)} with the second argument being
     * {@code null}.
     */
    public GuardedCommand(ContextClass context)
    {
      this(context, null);
    }

    /**
     * Equivalent to calling {@code SmartCommands.GuardedCommand#GuardedCommand(Context, Object, SmartCommands.GuardedHandler)} with the third
     * argument being {@code null}.
     */
    public GuardedCommand(ContextClass context, Object component)
    {
      this(context, null, null);
    }

    /**
     * Equivalent to calling {@code SmartCommands.GuardedCommand#GuardedCommand(Context, Object, SmartCommands.GuardedHandler)} with the second
     * argument being {@code null}.
     */
    public GuardedCommand(ContextClass context, SmartCommands.GuardedHandler delegate)
    {
      this(context, null, null);
    }

    /**
     * Creates a command that may be executed through the {@link SmartCommands#execute(SmartCommands.GuardedCommand)} method, and which is able to
     * tune any thrown exception during its execution.
     * <p>
     * <p>
     * It is also possible to set the {@link SmartCommands.GuardedHandler delegate} as long as the command execution has not been started through the
     * {@link #setDelegate(GuardedHandler)} method.
     * </p>
     *
     * @param context   the context from which the execution originates, and which will be used when reporting a potential exception ; it is not allowed to be
     * @param component the component from which the execution originates, and which will be used when reporting a potential exception ; may be @{code null}
     * @param delegate  if not {@code null}, the {@link #onThrowable(Throwable)} execution will be delegated to it @{code null}
     * @see #setDelegate(SmartCommands.GuardedHandler)
     */
    public GuardedCommand(ContextClass context, Object component, SmartCommands.GuardedHandler delegate)
    {
      if (context == null)
      {
        throw new NullPointerException("The context should not be null!");
      }
      this.context = context;
      this.component = component;
      this.delegate = delegate;
    }

    /**
     * A fallback method which will be triggered if a {@link Throwable} is thrown during the {@link #runGuarded()} method, so as to let the caller a
     * chance to handle locally the exception.
     * <p>
     * <p>
     * By default, the method does nothing and returns the provided {@code throwable}.
     * </p>
     *
     * @param throwable the exception that has been thrown during the {@link #runGuarded()} execution
     * @return {@code null} if and only if the method has handled the exception and that the {@link ActivityController.ExceptionHandler} should not be
     * invoked ; otherwise, the {@link Throwable} that should be submitted to the {@link ActivityController.ExceptionHandler}
     */
    public Throwable onThrowable(Throwable throwable)
    {
      return delegate == null ? throwable : delegate.onThrowable(throwable);
    }

    /**
     * This method will invoke the {@link #runGuarded()} method, and handle any thrown exception.
     */
    public final void run()
    {
      try
      {
        runGuarded();
      }
      catch (Throwable throwable)
      {
        // We let a chance to the caller to handle the exception
        final Throwable modifiedThrowable = onThrowable(throwable);
        if (modifiedThrowable == null)
        {
          // In that case, the exception has been handled locally
          return;
        }
        // We handle the exception
        ActivityController.getInstance().handleException(true, context, component, modifiedThrowable);
      }
    }

    /**
     * @return the context which will be used for reporting a potential exception
     */
    protected final ContextClass getContext()
    {
      return context;
    }

    /**
     * @return the delegate to which the {@link #onThrowable(Throwable)} method implementation will be redirected to ; may be {@code null}
     */
    protected final SmartCommands.GuardedHandler getDelegate()
    {
      return delegate;
    }

    /**
     * Sets the delegate that will be used in case of exception thrown during the command execution.
     *
     * @param delegate the exception handler which will be invoked when an exception is thrown during the command execution, via the
     *                 {@link #onThrowable(Throwable)} method; if {@code null}, no delegating mecanism will be involved
     * @return the current instance, so as to ease its usage
     */
    public SmartCommands.GuardedCommand<ContextClass> setDelegate(SmartCommands.GuardedHandler delegate)
    {
      this.delegate = delegate;
      return this;
    }

    /**
     * The body of the command execution.
     *
     * @throws Exception the method allows an exception to be thrown, and will be appropriately caught by the
     *                   {@link ActivityController#handleException(boolean, Context, Object, Throwable)}} method
     */
    protected abstract void runGuarded()
        throws Exception;

  }

  /**
   * Enables to execute in background a task, by notifying when the execution is running. It is especially useful when the UI should be notified when
   * a command is running.
   * <p>
   * <p>
   * When the command is executed by the {@link SmartCommands#LOW_PRIORITY_THREAD_POOL}, its underlying {@link ProgressHandler} is notified.
   * </p>
   *
   * @since 2010.11.30
   */
  public static abstract class ProgressGuardedCommand
      extends SmartCommands.GuardedCommand<Activity>
  {

    private final ProgressHandler progressHandler;

    private final String message;

    /**
     * Equivalent to calling {@code SmartCommands.ProgressGuardedCommand#ProgressGuardedCommand(Activity, Object, ProgressHandler, String)} with the
     * second argument being {@code null}.
     */
    public ProgressGuardedCommand(Activity activity, ProgressHandler progressHandler, String message)
    {
      this(activity, null, progressHandler, message);
    }

    /**
     * @param activity        the activity from which the execution originates, and which will be used when reporting a potential exception ; it is not allowed to
     *                        be @{code null}
     * @param component       the component from which the execution originates, and which will be used when reporting a potential exception ; may be @{code null}
     * @param progressHandler it will be invoked when the command {@link #runGuardedProgress() starts}, and when the command is over, even if an exception is thrown
     *                        during the execution
     * @param message         an optional message that will be passed to the {@link ProgressHandler} when the command starts
     */
    public ProgressGuardedCommand(Activity activity, Object component, ProgressHandler progressHandler, String message)
    {
      super(activity);
      this.progressHandler = progressHandler;
      this.message = message;
    }

    @Override
    protected final void runGuarded()
        throws Exception
    {
      try
      {
        progressHandler.onProgress(getContext(), true, new ProgressHandler.ProgressExtra(0, message), false);
        runGuardedProgress();
      }
      finally
      {
        progressHandler.onProgress(getContext(), false, null, false);
      }
    }

    /**
     * The commands to execute during the task.
     *
     * @throws Exception any thrown exception will be properly handled
     */
    protected abstract void runGuardedProgress()
        throws Exception;

  }

  /**
   * An exception which acts as an {@link Throwable} wrapper, and which works in combination with the {@link SmartCommands.SimpleGuardedCommand}. It
   * will be triggered during the {@link SmartCommands.SimpleGuardedCommand#onThrowable(Throwable)} method.
   * <p>
   * <p>
   * The traditional case is to let the {@link ActivityController#registerExceptionHandler(ActivityController.ExceptionHandler) exception handler}
   * cope in a centralized way with this kind of exception.
   * </p>
   *
   * @since 2011.11.03
   */
  public static class GuardedException
      extends Exception
  {

    private static final long serialVersionUID = 642514965027273713L;

    public final String displayMessage;

    /**
     * Builds a wrapper over the provided exception, and with an attached human readable message.
     *
     * @param throwable      the exception to be wrapped
     * @param displayMessage the supposedly i18ned message that will be used later on when actually handling that exception
     */
    protected GuardedException(Throwable throwable, String displayMessage)
    {
      super(throwable);
      this.displayMessage = displayMessage;
    }

  }

  /**
   * A handy {@link SmartCommands.GuardedCommand} which will issue systematically a log when an exception occurs during the command execution, and
   * will trigger a {@link SmartCommands.GuardedException} which wraps the original exception in that case.
   *
   * @param <ContextClass> the class will holds the {@link Context} responsible for the command
   * @since 2012.01.25
   */
  public static abstract class AbstractSimpleGuardedCommand<ContextClass extends Context>
      extends SmartCommands.GuardedCommand<ContextClass>
  {

    protected final String warningLogMessage;

    protected final String warningDisplayMessage;

    /**
     * Same as {@link SmartCommands.SimpleGuardedCommand#SimpleGuardedCommand(Activity, Object, String, String)} with the last parameter equal to
     * {@code context.getString(warningDisplayMessageResourceId)}.
     */
    public AbstractSimpleGuardedCommand(ContextClass context, Object component, String warningLogMessage,
        int warningDisplayMessageResourceId)
    {
      this(context, component, warningLogMessage, context.getString(warningDisplayMessageResourceId));
    }

    /**
     * Same as {@link SmartCommands.SimpleGuardedCommand#SimpleGuardedCommand(Activity, Object, String, String)} with the second parameter set to
     * {@code null} and last parameter equal to {@code context.getString(warningDisplayMessageResourceId)}.
     */
    public AbstractSimpleGuardedCommand(ContextClass context, String warningLogMessage,
        int warningDisplayMessageResourceId)
    {
      this(context, warningLogMessage, context.getString(warningDisplayMessageResourceId));
    }

    /**
     * Equivalent to calling {@code SmartCommands.SimpleGuardedCommand#SimpleGuardedCommand(Context, Object, String, String)} with the second argument
     * being {@code null}.
     */
    public AbstractSimpleGuardedCommand(ContextClass context, String warningLogMessage, String warningDisplayMessage)
    {
      this(context, null, warningLogMessage, warningDisplayMessage);
    }

    /**
     * Creates a new {@link SmartCommands.GuardedCommand}, which will issue a {@link Log#WARN warning log} and then trigger a
     * {@link SmartCommands.GuardedException}, if an exception occurs during its execution.
     *
     * @param context               the context from which the execution originates, and which will be used when reporting a potential exception ; it is not allowed to be
     * @param component             the component from which the execution originates, and which will be used when reporting a potential exception ; may be @{code null}
     * @param warningLogMessage     the log message that will be output in case of exception
     * @param warningDisplayMessage the (supposedly i18ned) human readable that will be transfered to the {@link SmartCommands.GuardedException} in case of exception
     *                              during the command execution @{code null}
     */
    public AbstractSimpleGuardedCommand(ContextClass context, Object component, String warningLogMessage,
        String warningDisplayMessage)
    {
      super(context, component, null);
      this.warningLogMessage = warningLogMessage;
      this.warningDisplayMessage = warningDisplayMessage;
    }

    /**
     * The implementation will log as a {@link Log#WARN warning} the exception.
     *
     * @return in the case the {@link #getDelegate()} is {@code null} a {@link SmartCommands.GuardedException} which wraps the provided exception,
     * with the {@link SmartCommands.SimpleGuardedCommand#warningDisplayMessage} as {@link SmartCommands.GuardedException#displayMessage
     * message attribute} ; otherwise, the return value comes from the delegate
     * @see SmartCommands.GuardedCommand#onThrowable(Throwable)
     */
    @Override
    public Throwable onThrowable(Throwable throwable)
    {
      if (log.isWarnEnabled())
      {
        log.warn(warningLogMessage, throwable);
      }
      return getDelegate() != null ? super.onThrowable(throwable) : new SmartCommands.GuardedException(throwable, warningDisplayMessage);
    }

  }

  /**
   * A handy {@link SmartCommands.AbstractSimpleGuardedCommand} dedicated to {@link Activity activities}.
   *
   * @since 2011.11.03
   */
  public static abstract class SimpleGuardedCommand
      extends SmartCommands.AbstractSimpleGuardedCommand<Activity>
  {

    public SimpleGuardedCommand(Activity context, Object component, String warningLogMessage,
        int warningDisplayMessageResourceId)
    {
      super(context, component, warningLogMessage, warningDisplayMessageResourceId);
    }

    public SimpleGuardedCommand(Activity context, Object component, String warningLogMessage,
        String warningDisplayMessage)
    {
      super(context, component, warningLogMessage, warningDisplayMessage);
    }

    public SimpleGuardedCommand(Activity context, String warningLogMessage, int warningDisplayMessageResourceId)
    {
      super(context, warningLogMessage, warningDisplayMessageResourceId);
    }

    public SimpleGuardedCommand(Activity context, String warningLogMessage, String warningDisplayMessage)
    {
      super(context, warningLogMessage, warningDisplayMessage);
    }

  }

  /**
   * A handy {@link SmartCommands.SimpleGuardedCommand} which will issue systematically {@link DialogInterface#dismiss() dismiss} a {@link Dialog}
   * once the command execution is over.
   * <p>
   * <p>
   * This kind of command is especially useful when a {@link ProgressDialog} is being displayed just before the current command execution, and that it
   * should be dismissed at the end of its execution.
   * </p>
   *
   * @since 2011.11.03
   */
  public static abstract class DialogGuardedCommand
      extends SmartCommands.SimpleGuardedCommand
  {

    /**
     * Method pointed out by Benoît Lubek, taken from the {@link Dialog} source code, because it is {@code private}.
     *
     * @return The activity associated with this dialog, or null if there is no associated activity.
     * @since 2012.09.08
     */
    private static Activity getAssociatedActivity(Dialog dialog)
    {
      Activity activity = dialog.getOwnerActivity();
      Context context = dialog.getContext();
      while (activity == null && context != null)
      {
        if (context instanceof Activity)
        {
          activity = (Activity) context; // found it!
        }
        else
        {
          context = context instanceof ContextWrapper ? ((ContextWrapper) context).getBaseContext() : // unwrap one level
              null; // done
        }
      }
      return activity;
    }

    /**
     * The dialog box which will be dismissed by the current command.
     */
    protected final Dialog dialog;

    /**
     * Same as {@link SmartCommands.DialogGuardedCommand#DialogGuardedCommand(Activity, Object, String, int, Dialog)} with the second argument set
     * to {@code null}.
     */
    public DialogGuardedCommand(Activity context, String warningLogMessage, int warningDisplayMessageResourceId,
        Dialog dialog)
    {
      this(context, null, warningLogMessage, warningDisplayMessageResourceId, dialog);
    }

    /**
     * Same as {@link SmartCommands.DialogGuardedCommand#DialogGuardedCommand(Activity, Object, String, String, Dialog)} with the fourth
     * parameter equal to {@code context.getString(warningDisplayMessageResourceId)}.
     */
    public DialogGuardedCommand(Activity context, Object component, String warningLogMessage,
        int warningDisplayMessageResourceId, Dialog dialog)
    {
      this(context, component, warningLogMessage, context.getString(warningDisplayMessageResourceId), dialog);
    }

    /**
     * Creates a new {@link SmartCommands.GuardedCommand}, which will issue a {@link Log#WARN warning log} and then trigger a
     * {@link SmartCommands.SimpleGuardedCommand}, if an exception occurs during its execution, and eventually {@link DialogInterface#dismiss dismiss}
     * the provided dialog.
     *
     * @param context               the context from which the execution originates, and which will be used when reporting a potential exception ; it is not allowed to be
     * @param component             the component from which the execution originates, and which will be used when reporting a potential exception ; may be @{code null}
     * @param warningLogMessage     the log message that will be output in case of exception
     * @param warningDisplayMessage the (supposedly i18ned) human readable that will be transfered to the {@link SmartCommands.GuardedException} in case of exception
     *                              during the command execution
     * @param dialog                the dialog to be dismissed at the end of the command execution ; may be {@code null}, and in that case, just behaves as its parent
     *                              {@link SmartCommands.SimpleGuardedCommand} @{code null}
     * @see SmartCommands.SimpleGuardedCommand#SimpleGuardedCommand(Activity, String, String)
     */
    public DialogGuardedCommand(Activity context, Object component, String warningLogMessage,
        String warningDisplayMessage, Dialog dialog)
    {
      super(context, component, warningLogMessage, warningDisplayMessage);
      this.dialog = dialog;
    }

    /**
     * The implementation will invoke the {@link #runGuardedDialog()} method, and will eventually dismiss the {@link #dialog} if necessary, whatever
     * happens.
     *
     * @see SmartCommands.SimpleGuardedCommand#runGuarded()
     */
    @Override
    protected final void runGuarded()
        throws Exception
    {
      try
      {
        runGuardedDialog();
      }
      finally
      {
        final Activity associatedActivity = DialogGuardedCommand.getAssociatedActivity(dialog);
        if (dialog != null && dialog.isShowing() == true && (associatedActivity != null && associatedActivity.isFinishing() == false))
        {
          // We want to make sure that the dialog has been dismissed, in order to prevent from memory leaks
          try
          {
            // This can be done from any thread, according to the documentation
            dialog.dismiss();
          }
          catch (Throwable throwable)
          {
            // This may occur on certain versions of Android, but we ignore that exception!
          }
        }
      }
    }

    /**
     * The actual command method to implement.
     *
     * @throws Exception if something wrong happened during the command execution
     */
    protected abstract void runGuardedDialog()
        throws Exception;

  }

  /**
   * A {@link DialogInterface.OnClickListener} which runs its {@link SmartCommands.GuardedCommand#runGuarded() execution} in the
   * {@link SmartCommands#LOW_PRIORITY_THREAD_POOL low-priority threads pool}, and which handles exceptions.
   *
   * @since 2010.06.08
   */
  public static abstract class GuardedDialogInterfaceClickListener
      implements DialogInterface.OnClickListener
  {

    protected final Context context;

    /**
     * @param context the context from which the execution originates, and which will be used when reporting a potential exception
     */
    public GuardedDialogInterfaceClickListener(Context context)
    {
      this.context = context;
    }

    public final void onClick(final DialogInterface dialog, final int which)
    {
      SmartCommands.execute(new SmartCommands.GuardedCommand<Context>(context)
      {
        @Override
        protected void runGuarded()
            throws Exception
        {
          GuardedDialogInterfaceClickListener.this.runGuarded(dialog, which);
        }
      });
    }

    /**
     * The method to implement, and which will be invoked when the {@link #onClick(DialogInterface, int)} method will be invoked.
     *
     * @param dialog the parameter transmitted from the {@link #onClick(DialogInterface, int)} method
     * @param which  the parameter transmitted from the {@link #onClick(DialogInterface, int)} method
     * @throws Exception the method is allowed to throw an {@link Exception}, which will be handled by the {@link SmartCommands#execute(GuardedCommand)}
     *                   method
     */
    protected abstract void runGuarded(DialogInterface dialog, int which)
        throws Exception;

  }

  /**
   * A {@link View.OnClickListener} which runs its {@link SmartCommands.GuardedCommand#runGuarded() execution} in the
   * {@link SmartCommands#LOW_PRIORITY_THREAD_POOL low-priority threads pool}, and which handles exceptions.
   *
   * @since 2010.06.08
   */
  public static abstract class GuardedViewClickListener
      implements View.OnClickListener
  {

    protected final Context context;

    /**
     * @param context the activity from which the execution originates, and which will be used when reporting a potential exception
     */
    public GuardedViewClickListener(Context context)
    {
      this.context = context;
    }

    public final void onClick(final View view)
    {
      SmartCommands.execute(new SmartCommands.GuardedCommand<Context>(context)
      {
        @Override
        public Throwable onThrowable(Throwable throwable)
        {
          return GuardedViewClickListener.this.onThrowable(throwable);
        }

        @Override
        protected void runGuarded()
            throws Exception
        {
          GuardedViewClickListener.this.runGuarded(view);
        }

      });
    }

    /**
     * The method to implement, and which will be invoked when the {@link #onClick(View)} method will be invoked.
     *
     * @param view the parameter transmitted from the {@link #onClick(View)} method
     * @throws Exception the method is allowed to throw an {@link Exception}, which will be handled by the {@link SmartCommands#execute(GuardedCommand)}
     *                   method
     */
    protected abstract void runGuarded(View view)
        throws Exception;

    /**
     * When an exception occurs during the hunderlying command execution, this method will be invoked, so as to let the caller handle this exception.
     * The contract is the same as for the {@link SmartCommands.GuardedCommand#onThrowable(Throwable)} method.
     *
     * @param throwable the exception that has been raised during the command execution
     * @return {@code null} if and only if the exception has been handled by the hereby method
     * @see SmartCommands.GuardedCommand#onThrowable(Throwable)
     */
    protected Throwable onThrowable(Throwable throwable)
    {
      return throwable;
    }

  }

  /**
   * Indicates how many threads at most will be available in the {@link #LOW_PRIORITY_THREAD_POOL low-priority threads pool}, by default. It needs to
   * be sent at the application start-up.
   * <p>
   * <p>
   * You may change that pool size by invoking the {@link ThreadPoolExecutor#setCorePoolSize(int)} method.
   * </p>
   */
  public final static int LOW_PRIORITY_THREAD_POOL_DEFAULT_SIZE = 3;

  /**
   * Use this threads pool instead of creating your own {@link Thread#MIN_PRIORITY} threads.
   * <p>
   * <ul>
   * <li>This pool will contain at most {@link #LOW_PRIORITY_THREAD_POOL_DEFAULT_SIZE} threads by default;</li>
   * <li>exceptions thrown by the {@link Runnable} are handled by the {@link ActivityController.ExceptionHandler}.</li>
   * </ul>
   * <p>
   * <p>
   * You can use this pool in the application, instead of creating new threads.
   * </p>
   */
  public final static SmartCommands.SmartThreadPoolExecutor LOW_PRIORITY_THREAD_POOL = new SmartCommands.SmartThreadPoolExecutor(SmartCommands.LOW_PRIORITY_THREAD_POOL_DEFAULT_SIZE, SmartCommands.LOW_PRIORITY_THREAD_POOL_DEFAULT_SIZE, 10l, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactory()
  {

    /**
     * The number of threads created so far for this worker threads pool.
     */
    private final AtomicInteger threadsCount = new AtomicInteger(1);

    public Thread newThread(Runnable runnable)
    {
      final Thread thread = new Thread(runnable);
      if (Thread.getDefaultUncaughtExceptionHandler() instanceof SmartUncaughtExceptionHandler == false)
      {
        // In the case when no default exception handler is defined or is not a "SmartUncaughtExceptionHandler", we provide ours
        thread.setUncaughtExceptionHandler(new SmartUncaughtExceptionHandler(null, null));
      }
      thread.setPriority(Thread.MIN_PRIORITY);
      thread.setName("droid4me-lowpool-thread #" + threadsCount.getAndIncrement());
      return thread;
    }

  });

  private final static Logger log = LoggerFactory.getInstance(SmartCommands.class);

  /**
   * Equivalent to invoking {@code SmartCommands#LOW_PRIORITY_THREAD_POOL#execute(Runnable)}.
   *
   * @see #execute(SmartCommands.GuardedCommand)
   */
  public static void execute(Runnable runnable)
  {
    SmartCommands.LOW_PRIORITY_THREAD_POOL.execute(runnable);
  }

  /**
   * Simply executes the provided command via the {@link SmartCommands#LOW_PRIORITY_THREAD_POOL}, which enables to run a command, and take benefit
   * from the {@link ExceptionHandler} if the command triggers an exception.
   * <p>
   * <p>
   * Equivalent to invoking {@code SmartCommands#LOW_PRIORITY_THREAD_POOL#execute(Runnable)}.
   * </p>
   *
   * @param guardedCommand the command to be executed
   * @see #execute(Runnable)
   */
  public static void execute(SmartCommands.GuardedCommand<?> guardedCommand)
  {
    SmartCommands.LOW_PRIORITY_THREAD_POOL.execute(guardedCommand);
  }

  /**
   * There is no reason creating an instance of that class, which is just a container.
   */
  private SmartCommands()
  {
  }

}
