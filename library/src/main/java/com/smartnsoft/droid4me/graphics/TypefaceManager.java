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

package com.smartnsoft.droid4me.graphics;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.graphics.Typeface;

import com.smartnsoft.droid4me.graphics.TypefaceManager.Typefaceable;

/**
 * A helper class for managing Android fonts.
 * <p>
 * <p>
 * The approach is more elegant than the one described <a href="http://sriramramani.wordpress.com/2012/11/29/custom-fonts/#comments">here</a>.
 * </p>
 *
 * @param <T> a enumerated type that the developer needs to create in order to name and refer to typefaces
 * @author Édouard Mercier
 * @since 2013.07.22
 */
public class TypefaceManager<T extends Typefaceable>
{

  public interface Typefaceable
  {

    Typeface getTypeface(TypefaceManager<?> manager, Context context);

  }

  public enum TypefaceLocation
  {

    Native, Assets, File

  }

  public static class SimpleTypefaceable
      implements Typefaceable
  {

    public final TypefaceLocation location;

    public final String fileName;

    public SimpleTypefaceable(TypefaceLocation location, String fileName)
    {
      this.location = location;
      this.fileName = fileName;
    }

    @Override
    public Typeface getTypeface(TypefaceManager<?> manager, Context context)
    {
      switch (location)
      {
        case Native:
        default:
          return Typeface.DEFAULT;
        case Assets:
        {
          final String fontPathPrefix = manager.getAssetsFontFolderPathPrefix();
          final Typeface typeface = Typeface.createFromAsset(context.getAssets(), fontPathPrefix + fileName);
          return typeface;
        }
        case File:
        {
          final Typeface typeface = Typeface.createFromFile(fileName);
          return typeface;
        }
      }
    }

    @Override
    public int hashCode()
    {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((fileName == null) ? 0 : fileName.hashCode());
      result = prime * result + ((location == null) ? 0 : location.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj)
    {
      if (this == obj)
      {
        return true;
      }

      if (obj == null)
      {
        return false;
      }

      if (getClass() != obj.getClass())
      {
        return false;
      }

      final SimpleTypefaceable other = (SimpleTypefaceable) obj;

      if (fileName == null)
      {
        if (other.fileName != null)
        {
          return false;
        }
      }
      else if (!fileName.equals(other.fileName))
      {
        return false;
      }

      if (location != other.location)
      {
        return false;
      }

      return true;
    }

  }

  private final Map<T, Typeface> typefacesMap = new HashMap<T, Typeface>();

  public final Typeface getTypeface(Context context, T typefaces)
  {
    // We first search for the typeface within the cache
    Typeface typeface = typefacesMap.get(typefaces);
    if (typeface != null)
    {
      return typeface;
    }

    typeface = typefaces.getTypeface(this, context);
    typefacesMap.put(typefaces, typeface);
    return typeface;
  }

  public final String getAssetsFontFolderPathPrefix()
  {
    return "font/";
  }

}