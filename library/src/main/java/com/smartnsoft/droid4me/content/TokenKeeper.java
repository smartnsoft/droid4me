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
import com.smartnsoft.droid4me.support.v4.content.LocalBroadcastManager;

/**
 * A utility class which enables to remember persistently of token associated to a string: those tokens can
 * <ul>
 * <li>be {@code rememberToken()) remembered},</li>
 * <li>be even more {@code rememberTokenAndBroadcast() remembered and broadcast},</li>
 * <li>be simply {@code broadcast() broadcast},</li>
 * <li>be {@link #discardToken(Serializable)} discarded,</li>
 * <li>be requested through the {@link #hasToken(Serializable)} and {@link #missesToken(Serializable)} methods.</li>
 * </ul>
 * <p>
 * <p>
 * The token is represented by the class NotificationKind template.
 * </p>
 * <p>
 * <p>
 * The implementation uses the Android {@link SharedPreferences} to store persistently the tokens, and it resorts to {@link Intent intents} when it
 * comes to {@link #broadcast(Serializable)} a token to the rest of the application: this is the reason why the template class must carefully implement the
 * {@link #toString()} method.
 * </p>
 * <p>
 * <p>
 * The class does not impose any threaded affinity. It will not trigger exceptions under concurrent races conditions, but it has not functionally been
 * designed for a thread-safe mode (no atomicity is granted).
 * </p>
 * <p>
 * <p>
 * This class is especially useful when, in at some point in the application, a web service is invoked, which causes the local cached data to be
 * partially out-dated, and that the application should keep this information, so that it refreshes some business objects via web services when
 * required.
 * </p>
 *
 * @param <Token> a {@link Serializable} type which implements properly the {@link #toString()} method
 * @author Édouard Mercier
 * @since 2011.07.27
 */
public class TokenKeeper<Token extends Serializable>
{

  /**
   * An interface, which enables to "multiply" a token.
   *
   * @param <Token> a {@link Serializable} type which implements properly the {@link #toString()} method
   * @see TokenKeeper#setTokenMultiplier(TokenMultiplier)
   */
  public interface TokenMultiplier<Token extends Serializable>
  {

    /**
     * Is invoked only by the {@link TokenKeeper#rememberToken(Serializable)}, in order to determine the tokens that should be remembered along with the
     * provided one. However, this method will not be invoked when {@link TokenKeeper#discardToken(Serializable) discarding} a token.
     * <p>
     * <p>
     * This method will not be run recursively on each returned token.
     * </p>
     *
     * @param token the token that should be analyzed
     * @return the tokens related to the provided token. If {@code null}, it will be considered that no sub-token is available for the given token.
     * The array should not contain the provided token (for better performances). The returned token will just be
     * {@link TokenKeeper#rememberToken(Serializable) remembered} but not {@link TokenKeeper#rememberToken(Serializable) remembered}
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
   * @param context it will be used to create an internal {@link SharedPreferences} instance, but also to invoke the
   *                {@link LocalBroadcastManager#sendBroadcast(Intent)} method when broadcasting
   * @param prefix  a string, which will be used when storing the token in the {@link SharedPreferences preferences}, and when {@link #broadcast(Serializable) broadcasting} it;
   *                can be {@code null}
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
   * <p>
   * <p>
   * When turned-off, no token will be remembered, discarded, nor broadcast.
   * <p>
   *
   * @param enabled whether the token keeper should be on or off
   * @see #isEnabled()
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
   * When {@link #rememberToken(Serializable) remembering a token}, it may be useful to remember other tokens which depend on it. By default, no token
   * multiplier is registered. This method enables to register an interface, which enables to remember other tokens than the provided one at the same
   * time.
   * <p>
   * <p>
   * This method has no impact on the {@link #broadcast(Serializable) token broadcasting}.
   * </p>
   *
   * @param tokenMultiplier the interface that will be used to "multiply" the remembered token; it should be set to {@code null}, if that feature needs to be
   *                        disabled, which is the default
   */
  public final void setTokenMultiplier(TokenMultiplier<Token> tokenMultiplier)
  {
    this.tokenMultiplier = tokenMultiplier;
  }

  /**
   * Adds the {@link IntentFilter#addAction(String) actions} to the provided {@link IntentFilter}.
   * <p>
   * <p>
   * For every provided notification kind, it is added to the IntentFilter, by prefixing it with the prefix provided in the
   * {@link #TokenKeeper(Context, String) constructor}.
   * </p>
   *
   * @param intentFilter the intent filter which will be enriched
   * @param tokens       the list of tokens to add to the intent filter action
   * @return itself
   */
  public TokenKeeper<Token> appendTo(IntentFilter intentFilter, Token... tokens)
  {
    for (Token token : tokens)
    {
      intentFilter.addAction(computeTokenKey(token, false));
    }
    return this;
  }

  /**
   * Searches in the provided {@link Intent intent actions} whether one of the given notification kinds may be present.
   *
   * @param intent the intent that will investigated
   * @param tokens the list of notification kinds to look after
   * @return {@code true} if and only if the IntentFilter {@link Intent#getAction() action} is not null and one of notification kind matches the
   * Intent action
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
      if (action.equals(computeTokenKey(token, false)) == true)
      {
        return true;
      }
    }
    return false;
  }

  /**
   * Indicates whether a given token is currently set for the given notification kind.
   *
   * @param token the token to search for
   * @return {@code true} if and only if the notification kind token is present
   * @see #missesToken(Serializable)
   */
  public boolean hasToken(Token token)
  {
    final String key = computeTokenKey(token, true);
    return hasPersistedToken(key);
  }

  /**
   * Returns the opposite value of the {@link #hasToken(Serializable)} method.
   *
   * @see #hasToken(Serializable)
   */
  public boolean missesToken(Token token)
  {
    return !hasToken(token);
  }

  /**
   * Sets persistently a token for the given notification kind, so that a {@link #hasToken(Serializable) subsequent call} returns {@code true}.
   *
   * @param token the token to set and remember
   * @see #discardToken(Serializable)
   * @see #rememberTokenAndBroadcast(Serializable)
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
   * Invokes successively the {@link #rememberToken(Serializable)} method on each provided token.
   *
   * @param tokens the list of tokens to set and remember
   */
  public void rememberTokens(Token... tokens)
  {
    for (Token token : tokens)
    {
      rememberToken(token);
    }
  }

  /**
   * Invokes consequently the {@link #rememberToken(Serializable)} and {@link #broadcast(Serializable)} methods.
   *
   * @see #broadcast(Serializable)
   * @see #rememberTokensAndBroadcast(Serializable...)
   */
  public void rememberTokenAndBroadcast(Token token)
  {
    rememberToken(token);
    broadcast(token);
  }

  /**
   * Invokes successively the {@link #rememberTokenAndBroadcast(Serializable)} method for each provided token.
   *
   * @param tokens the list of tokens to handle
   * @see #rememberTokenAndBroadcast(Serializable)
   */
  public void rememberTokensAndBroadcast(Token... tokens)
  {
    for (Token token : tokens)
    {
      rememberTokenAndBroadcast(token);
    }
  }

  /**
   * Triggers an Android broadcast {@link Intent} with the provided token as the single {@link Intent#setAction(String) action}.
   * <p>
   * <p>
   * This method will invoke the {@link LocalBroadcastManager#sendBroadcast(Intent)} method.
   * </p>
   *
   * @param token the token to be broadcast, which will be used to compute the broadcast {@link Intent} action through the
   * @see #discardToken(Serializable)
   * @see #computeTokenKey(Serializable, boolean)
   * @see #enrichBroadcast(Serializable, Intent)
   */
  public void broadcast(Token token)
  {
    if (enabled == false)
    {
      return;
    }
    final Intent intent = new Intent(computeTokenKey(token, false));
    enrichBroadcast(token, intent);
    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
  }

  /**
   * Invokes successively the {@link #broadcast(Serializable)} method for each provided token.
   *
   * @param tokens the list of tokens to handle
   */
  public void broadcast(Token... tokens)
  {
    for (Token token : tokens)
    {
      broadcast(token);
    }
  }

  /**
   * Discards the given token, so as a {@link #hasToken(Serializable) subsequent method call} returns {@code false}.
   *
   * @param token the token to discard
   * @see #rememberToken(Serializable)
   */
  public void discardToken(Token token)
  {
    setToken(token, false);
  }

  /**
   * Indicates whether the token corresponding to the provided key is currently persisted and available.
   * <p>
   * <p>
   * Overriding this method enables to change the way the token persistence is ensured. The current implementation relies on the {@link #preferences}.
   * </p>
   *
   * @param tokenKey the key of the token which is requested
   * @return {@code true} if and only if the given token is currently stored at the persistence level
   * @see #persistToken(String, boolean)
   */
  protected boolean hasPersistedToken(String tokenKey)
  {
    return preferences.getBoolean(tokenKey, false);
  }

  /**
   * This is the place where the token is being stored at the persistence level. This method will eventually be invoked when a token is remembered or
   * discarded.
   * <p>
   * <p>
   * The default implementation stores the token in the {@link #preferences}: overriding this method enables to customize the token persistence
   * implementation.
   * </p>
   *
   * @param tokenKey the key resulting from the {@code Token.toString()} method call invoked on the parameter provided to the {@link #rememberToken(Serializable)}/
   *                 {@link #discardToken(Serializable)} methods
   * @param value    {@code true} and if only if the token needs to be remembered
   * @see #rememberToken(Serializable)
   * @see #discardToken(Serializable)
   * @see #hasPersistedToken(String)
   */
  protected void persistToken(String tokenKey, boolean value)
  {
    final Editor editor = preferences.edit();
    if (value == false)
    {
      editor.remove(tokenKey);
    }
    else
    {
      editor.putBoolean(tokenKey, value);
    }
    editor.commit();
  }

  /**
   * The method responsible for generating a {@link String} from a token, so that the token key may be serialized when persisting it, or when
   * computing the related broadcast {@link Intent} action.
   * <p>
   * <p>
   * By overriding this method, it is possible to tune the way the token key is stored in the persistence layer. The default implementation resorts to
   * the {@link #prefix} value for computing the string prefix.
   * </p>
   *
   * @param token          the token to deal with
   * @param forPersistence indicates whether the stringified representation of the token should be computed for the persistence, or for the broadcast
   *                       {@link Intent} ; returning two different values for the same token enables to broadcast a more generic version while storing a more
   *                       specific
   * @return a string which identifies the provided token
   * @see #prefix
   * @see #rememberToken(Serializable)
   * @see #discardToken(Serializable)
   * @see #broadcast(Serializable)
   */
  protected String computeTokenKey(Token token, boolean forPersistence)
  {
    return prefix + "." + token.toString();
  }

  /**
   * Enables to enrich the {@link Intent} that is bound to be broadcast.
   * <p>
   * <p>
   * The method does nothing, and it may be overriden.
   * </p>
   *
   * @param token  then token which generates the broadcast
   * @param intent the {@link Intent} that has just been generated with the token token action
   * @see #broadcast(Serializable)
   */
  protected void enrichBroadcast(Token token, Intent intent)
  {
  }

  /**
   * Is responsible for turning the token into a key and persist it. If the {@link #enabled} flag is set to {@code false}, the token persistence
   * should be ignored.
   *
   * @param token the token to remember and persist
   * @param value whether the token should be remembered or discarded
   */
  private void setToken(Token token, boolean value)
  {
    final String key = computeTokenKey(token, true);
    if (log.isDebugEnabled())
    {
      log.debug("Setting the notification token '" + key + "' to " + value);
    }
    if (enabled == false)
    {
      return;
    }
    persistToken(key, value);
  }

}
