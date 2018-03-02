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

    private void set(String value)
    {
      this.value = value;
    }

  }

  /**
   * Expresses what information to catch from the XML.
   */
  private static final class Expectation
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

  // The maximum XML document depth is set to 32
  private final String[] path = new String[32];

  private final Map<Integer, List<Expectation>> expectations = new HashMap<>();

  private boolean active = true;

  private int level = 0;

  private StringBuffer characters;

  private boolean rememberCharacters;

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

  protected abstract void onStartElement(String localName, Attributes attributes);

  protected abstract void onEndElement(String localName);

  protected final boolean isLevel(int expectedLevel)
  {
    return level == expectedLevel;
  }

  protected final boolean meetCriteria(String localName, String expectedLocalName, int expectedLevel)
  {
    return meetCriteria(localName, expectedLocalName, null, null, expectedLevel);
  }

  protected final boolean meetCriteria(String localName, String expectedLocalName, String expectedParentLocalName,
      int expectedLevel)
  {
    return meetCriteria(localName, expectedLocalName, expectedParentLocalName, null, expectedLevel);
  }

  protected final boolean meetCriteria(String localName, String expectedLocalName, String expectedParentLocalName,
      String expectedGrandParentLocalName, int expectedLevel)
  {
    // We test the XML DOM depth first, because it does not consume much, and it is a good discriminator candidate
    return level == expectedLevel && (expectedParentLocalName == null || isParent(expectedParentLocalName) == true) && localName.equals(expectedLocalName) == true && (expectedGrandParentLocalName == null || isGrandParent(expectedGrandParentLocalName) == true);
  }

  protected final boolean rememberCharacters(String expectedLocalName, String expectedParentLocalName,
      int expectedLevel)
  {
    return rememberCharacters(expectedLocalName, expectedParentLocalName, null, expectedLevel);
  }

  protected final boolean rememberCharacters(String expectedLocalName, String expectedParentLocalName,
      String expectedGrandParentLocalName, int expectedLevel)
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

  protected final boolean catchCharacters(String expectedLocalName, String expectedParentLocalName, int expectedLevel,
      StringHolder field)
  {
    return catchCharacters(expectedLocalName, expectedParentLocalName, null, expectedLevel, field);
  }

  protected final boolean catchCharacters(String expectedLocalName, String expectedParentLocalName,
      String expectedGrandParentLocalName, int expectedLevel, StringHolder field)
  {
    if (rememberCharacters(expectedLocalName, expectedParentLocalName, expectedGrandParentLocalName, expectedLevel) == true)
    {
      List<Expectation> levelExpectations = expectations.get(expectedLevel);
      if (levelExpectations == null)
      {
        levelExpectations = new ArrayList<>();
        expectations.put(expectedLevel, levelExpectations);
      }
      levelExpectations.add(new Expectation(expectedLocalName, expectedParentLocalName, expectedGrandParentLocalName, field));
      return true;
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

  private boolean isParent(String localName)
  {
    return level > 0 && path[level - 1] != null && path[level - 1].equals(localName);
  }

  private boolean isGrandParent(String localName)
  {
    return level > 1 && path[level - 2] != null && path[level - 2].equals(localName);
  }

}
