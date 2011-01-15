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

package com.smartnsoft.droid4me.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.ext.DefaultHandler2;

/**
 * A basis class for SAX parsing, which makes the parsing easier and faster to write.
 * 
 * @author Édouard Mercier
 * @since 2010.02.16
 */
public abstract class StatefulDefaultHandler
    extends DefaultHandler2
{

  /**
   * Stores the value of a string, and when getting that value, it is emptied.
   */
  public static final class StringHolder
  {

    private String value;

    private void set(String value)
    {
      this.value = value;
    }

    public String get()
    {
      if (value == null)
      {
        return null;
      }
      final String duplicatedValue = new String(value);
      value = null;
      return duplicatedValue;
    }

  }

  /**
   * Expresses what information to catch from the XML.
   */
  private final static class Expectation
  {
    public final String localName;

    public final String parentLocalName;

    public final String grandParentLocalName;

    private final StringHolder field;

    public Expectation(String localName, String parentLocalName, String grandParentLocalName, StringHolder field)
    {
      this.localName = localName;
      this.parentLocalName = parentLocalName;
      this.grandParentLocalName = grandParentLocalName;
      this.field = field;
    }

  }

  private boolean active = true;

  private int level = 0;

  // The maximum XML document depth is set to 32
  private final String[] path = new String[32];

  private StringBuffer characters;

  private boolean rememberCharacters;

  private final Map<Integer, List<Expectation>> expectations = new HashMap<Integer, List<Expectation>>();

  protected abstract void onStartElement(String localName, Attributes attributes);

  protected abstract void onEndElement(String localName);

  @Override
  public final void startElement(String uri, String localName, String qName, Attributes attributes)
      throws SAXException
  {
    if (active == false)
    {
      return;
    }
    path[level] = localName;
    try
    {
      onStartElement(localName, attributes);
    }
    finally
    {
      level++;
    }
  }

  @Override
  public final void endElement(String uri, String localName, String qName)
      throws SAXException
  {
    if (active == false)
    {
      return;
    }
    level--;
    // if (catchExpectations(localName) == false)
    catchExpectations(localName);
    {
      onEndElement(localName);
    }
  }

  @Override
  public final void characters(char[] ch, int start, int length)
  {
    if (active == false)
    {
      return;
    }
    if (rememberCharacters == true)
    {
      if (characters == null)
      {
        characters = new StringBuffer(new String(ch, start, length));
      }
      else
      {
        characters.append(new String(ch, start, length));
      }
    }
  }

  protected final boolean isLevel(int expectedLevel)
  {
    return level == expectedLevel;
  }

  protected final boolean meetCriteria(String localName, String expectedLocalName, int expectedLevel)
  {
    return meetCriteria(localName, expectedLocalName, null, null, expectedLevel);
  }

  protected final boolean meetCriteria(String localName, String expectedLocalName, String expectedParentLocalName, int expectedLevel)
  {
    return meetCriteria(localName, expectedLocalName, expectedParentLocalName, null, expectedLevel);
  }

  protected final boolean meetCriteria(String localName, String expectedLocalName, String expectedParentLocalName, String expectedGrandParentLocalName,
      int expectedLevel)
  {
    // We test the XML DOM depth first, because it does not consume much, and it is a good discriminator candidate
    return level == expectedLevel && (expectedParentLocalName == null || isParent(expectedParentLocalName) == true) && localName.equals(expectedLocalName) == true && (expectedGrandParentLocalName == null || isGrandParent(expectedGrandParentLocalName) == true);
  }

  protected final boolean rememberCharacters(String expectedLocalName, String expectedParentLocalName, int expectedLevel)
  {
    return rememberCharacters(expectedLocalName, expectedParentLocalName, null, expectedLevel);
  }

  protected final boolean rememberCharacters(String expectedLocalName, String expectedParentLocalName, String expectedGrandParentLocalName, int expectedLevel)
  {
    if (meetCriteria(path[level], expectedLocalName, expectedParentLocalName, expectedGrandParentLocalName, expectedLevel) == true)
    {
      rememberCharacters = true;
      return true;
    }
    else
    {
      return false;
    }
  }

  protected void stopParsing()
  {
    active = false;
  }

  protected final boolean catchCharacters(String expectedLocalName, String expectedParentLocalName, int expectedLevel, StringHolder field)
  {
    return catchCharacters(expectedLocalName, expectedParentLocalName, null, expectedLevel, field);
  }

  protected final boolean catchCharacters(String expectedLocalName, String expectedParentLocalName, String expectedGrandParentLocalName, int expectedLevel,
      StringHolder field)
  {
    if (rememberCharacters(expectedLocalName, expectedParentLocalName, expectedGrandParentLocalName, expectedLevel) == true)
    {
      List<Expectation> levelExpectations = expectations.get(expectedLevel);
      if (levelExpectations == null)
      {
        levelExpectations = new ArrayList<Expectation>();
        expectations.put(expectedLevel, levelExpectations);
      }
      levelExpectations.add(new Expectation(expectedLocalName, expectedParentLocalName, expectedGrandParentLocalName, field));
      return true;
    }
    return false;
  }

  private boolean catchExpectations(String localName)
  {
    final List<Expectation> levelExpectations = expectations.get(level);
    if (levelExpectations == null)
    {
      return false;
    }
    int index = 0;
    for (Expectation expectation : levelExpectations)
    {
      if (meetCriteria(localName, expectation.localName, expectation.parentLocalName, expectation.grandParentLocalName, level) == true)
      {
        expectation.field.set(getCharactersAndForget());
        levelExpectations.remove(index);
        return true;
      }
      index++;
    }
    return false;
  }

  protected final String getCharactersAndForget()
  {
    rememberCharacters = false;
    final String value;
    if (characters != null)
    {
      value = characters.toString();
      characters = null;
    }
    else
    {
      value = null;
    }
    return value;
  }

  private boolean isParent(String localName)
  {
    return level > 0 && path[level - 1] != null && path[level - 1].equals(localName);
  }

  private boolean isGrandParent(String localName)
  {
    return level > 1 && path[level - 2] != null && path[level - 2].equals(localName);
  }

}
