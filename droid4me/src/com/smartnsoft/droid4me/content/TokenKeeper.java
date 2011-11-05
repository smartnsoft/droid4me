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

package com.smartnsoft.droid4me.content;

import java.io.Serializable;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;

/**
 * A utility class which enables to remember persistently of token associated to a string: those tokens can
 * <ul>
 * <li>be {@link #rememberToken()) remembered},</li>
 * <li>be even more {@link #rememberTokenAndBroadcast() remembered and broadcast},</li>
 * <li>be simply {@link #broadcast() broadcast},</li>
 * <li>be {@link #discardToken() discarded},</li>
 * <li>be requested through the {@link #hasToken()} and {@link #missesToken()} methods.</li>
 * </ul>
 * 
 * <p>
 * The token is represented by the class NotificationKind template.
 * </p>
 * 
 * <p>
 * The implementation uses the Android {@link SharedPreferences} to store persistently the tokens, and it resorts to {@link Intent intents} when it
 * comes to {@link #broadcast(Enum)} a token to the rest of the application: this is the reason why the template class must carefully implement the
 * {@link #toString()} method.
 * </p>
 * 
 * <p>
 * The class does not impose any threaded affinity. It will not trigger exceptions under concurrent races conditions, but it has not functionally been
 * designed for a thread-safe mode (no atomicity is granted).
 * </p>
 * 
 * <p>
 * This class is especially useful when, in at some point in the application, a web service is invoked, which causes the local cached data to be
 * partially out-dated, and that the application should keep this information, so that it refreshes some business objects via web services when
 * required.
 * </p>
 * 
 * @param <Token>
 *          a serializable type which implements properly the {@link #toString()} method
 * @author ï¿½douard Mercier
 * @since 2011.07.27
 */
public class TokenKeeper<Token extends Serializable>
{

  /**
   * An interface, which enables to "multiply" a token.
   */
  public static interface TokenMultiplier<Token>
  {

    /**
     * Is invoked only by the {@link TokenKeeper#rememberToken(Enum)}, in order to determine the tokens that should be remembered along with the
     * provided one.
     * 
     * <p>
     * This method will not be run recursively on each returned token.
     * </p>
     * 
     * @param token
     *          the token that should be analyzed
     * @return the tokens related to the provided token. If {@code null}, it will be considered that no sub-token is available for the given
     *         token. The array should not contain the provided token (for better performances)
     */
    Token[] getSubTokens(Token token);

  }

  protected final static Logger log = LoggerFactory.getInstance(TokenKeeper.class);

  protected final Context context;

  protected final String prefix;

  protected final SharedPreferences preferences;

  protected TokenMultiplier<Token> tokenMultiplier;

  protected boolean enabled = true;

  /**
   * Builds a notifier. It is very likely that a single instance of the {@link TokenKeeper} should be created.
   * 
   * @param context
   *          it will be used to create an internal {@link SharedPreferences} instance, but also to invoke the {@link Context#sendBroadcast(Intent)}
   *          method when broadcasting
   * @param prefix
   *          a string, which will be used when storing the token in the {@link SharedPreferences preferences}, and when {@link #broadcast(Enum)
   *          broadcasting} it; can be {@code null}
   */
  public TokenKeeper(Context context, String prefix)
  {
    this.context = context;
    this.prefix = prefix;
    preferences = PreferenceManager.getDefaultSharedPreferences(context);
  }

  /**
   * @return {@code true} if and only if the token keeper is currently enabled
   * @see #setEnabled(boolean)
   */
  public final boolean isEnabled()
  {
    return enabled;
  }

  /**
   * It is possible to totally disable the token keeper. By default, the token keeper is enabled/turned on. This is especially useful when you want to
   * turn it off, while running tests.
   * 
   * <p>
   * When turned-off, no token will be remembered not broadcast.
   * <p>
   * 
   * @param enabled
   *          whether the token keeper should be on or off
   */
  public final void setEnabled(boolean enabled)
  {
    this.enabled = enabled;
  }

  /**
   * @return the currently registered token multiplier; may be {@code null}
   * @see #setTokenMultiplier(TokenMultiplier)
   */
  public final TokenMultiplier<Token> getTokenMultiplier()
  {
    return tokenMultiplier;
  }

  /**
   * When {@link #rememberToken(Enum) remembering a token}, it may be useful to remember other tokens which depend on it. By default, no token
   * multiplier is registered. This method enables to register an interface, which enables to remember other tokens than the provided one at the same
   * time.
   * 
   * <p>
   * This method has no impact on the {@link #broadcast(Enum) token broadcasting}.
   * </p>
   * 
   * @param tokenMultiplier
   *          the interface that will be used to "multiply" the remembered token; it should be set to {@code null}, if that feature needs to be
   *          disabled
   */
  public final void setTokenMultiplier(TokenMultiplier<Token> tokenMultiplier)
  {
    this.tokenMultiplier = tokenMultiplier;
  }

  /**
   * Adds the {@link IntentFilter#addAction(String) actions} to the provided {@link IntentFilter}.
   * 
   * <p>
   * For every provided notification kind, it is added to the IntentFilter, by prefixing it with the prefix provided in the
   * {@link #Notifier(Context, String) constructor}.
   * </p>
   * 
   * @param intentFilter
   *          the intent filter which will be enriched
   * @param tokens
   *          the list of tokens to add to the intent filter action
   * @return itself
   */
  public TokenKeeper<Token> appendTo(IntentFilter intentFilter, Token... tokens)
  {
    for (Token token : tokens)
    {
      intentFilter.addAction(computeIntentAction(token));
    }
    return this;
  }

  /**
   * Searches in the provided {@link Intent intent actions} whether one of the given notification kinds may be present.
   * 
   * @param intent
   *          the intent that will investigated
   * @param tokens
   *          the list of notification kinds to look after
   * @return {@code true} if and only if the IntentFilter {@link Intent#getAction() action} is not null and one of notification kind matches the
   *         Intent action
   */
  public boolean belongsTo(Intent intent, Token... tokens)
  {
    final String action = intent.getAction();
    if (action == null)
    {
      return false;
    }
    for (Token token : tokens)
    {
      if (action.equals(computeIntentAction(token)) == true)
      {
        return true;
      }
    }
    return false;
  }

  /**
   * Indicates whether a given token is currently set for the given notification kind.
   * 
   * @param token
   *          the token to search for
   * @return {@code true} if and only if the notification kind token is present
   * @see #missesToken(Enum)
   */
  public boolean hasToken(Token token)
  {
    final String key = computeIntentAction(token);
    return preferences.getBoolean(key, false);
  }

  /**
   * Returns the opposite value of the {@link #hasToken(Enum)} method.
   * 
   * @see #hasToken(Enum)
   */
  public boolean missesToken(Token token)
  {
    return !hasToken(token);
  }

  /**
   * Sets persistently a token for the given notification kind, so that a {@link #hasToken(Enum) subsequent call} returns {@code true}.
   * 
   * @param token
   *          the token to set and remember
   * @see #discardToken(Enum)
   */
  public void rememberToken(Token token)
  {
    setToken(token, true);
    if (tokenMultiplier != null)
    {
      final Token[] subTokens = tokenMultiplier.getSubTokens(token);
      if (subTokens != null)
      {
        for (Token subToken : subTokens)
        {
          setToken(subToken, true);
        }
      }
    }
  }

  /**
   * Invokes successively the {@link #rememberToken(Enum)} method on each provided token.
   * 
   * @param tokens
   *          the list of tokens to set and remember
   */
  public void rememberTokens(Token... tokens)
  {
    for (Token token : tokens)
    {
      rememberToken(token);
    }
  }

  /**
   * Invokes consequently the {@link #rememberToken(Enum)} and {@link #broadcast(Enum)} methods.
   */
  public void rememberTokenAndBroadcast(Token token)
  {
    rememberToken(token);
    broadcast(token);
  }

  /**
   * Invokes successively the {@link #rememberTokenAndBroadcast(Enum)} method for each provided token.
   * 
   * @param tokens
   *          the list of tokens to handle
   */
  public void rememberTokensAndBroadcast(Token... tokens)
  {
    for (Token token : tokens)
    {
      rememberTokenAndBroadcast(token);
    }
  }

  /**
   * Triggers an Android broadcast with the provided token as the single {@link Intent#setAction(String) action}.
   * 
   * <p>
   * This method will trigger a {@link Context#sendBroadcast(Intent)} method.
   * </p>
   * 
   * @param token
   *          the token to be broadcast
   * @see #discardToken(Enum)
   */
  public void broadcast(Token token)
  {
    if (enabled == false)
    {
      return;
    }
    final Intent intent = new Intent(computeIntentAction(token));
    context.sendBroadcast(intent);
  }

  /**
   * Invokes successively the {@link #broadcast(Enum)} method for each provided token.
   * 
   * @param tokens
   *          the list of tokens to handle
   */
  public void broadcast(Token... tokens)
  {
    for (Token token : tokens)
    {
      broadcast(token);
    }
  }

  /**
   * Discards the given token, so as a {@link #hasToken(Enum) subsequent method call} returns {@code false}.
   * 
   * @param token
   *          the token to discard
   * @see #rememberToken(Enum)
   */
  public void discardToken(Token token)
  {
    setToken(token, false);
  }

  private void setToken(Token token, boolean value)
  {
    final String key = computeIntentAction(token);
    if (log.isDebugEnabled())
    {
      log.debug("Setting the notification token '" + key + "' to " + value);
    }
    if (enabled == false)
    {
      return;
    }
    final Editor editor = preferences.edit();
    if (value == false)
    {
      editor.remove(key);
    }
    else
    {
      editor.putBoolean(key, value);
    }
    editor.commit();
  }

  private String computeIntentAction(Token token)
  {
    return prefix + token.toString();
  }

}
