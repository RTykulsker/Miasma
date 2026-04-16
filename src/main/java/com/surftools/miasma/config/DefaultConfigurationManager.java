/**

The MIT License (MIT)

Copyright (c) 2019, Robert Tykulsker

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.


*/

package com.surftools.miasma.config;

import java.util.HashMap;
import java.util.Map;

public class DefaultConfigurationManager implements IConfigurationManager {
  protected Map<MiasmaKey, String> map;
  protected MiasmaKey[] values = null;

  public DefaultConfigurationManager() {
    map = new HashMap<>();
  }

  public void setValues(MiasmaKey[] values) {
    this.values = values;
  }

  protected MiasmaKey fromString(String string) {
    if (values == null) {
      throw new RuntimeException("Values not specified");
    }

    for (MiasmaKey key : values) {
      if (key.toString().equals(string)) {
        return key;
      }
    }
    return null;
  }

  @Override
  public String getAsString(MiasmaKey key) {
    return getAsString(key, key.defaultValue());
  }

  @Override
  public String getAsString(MiasmaKey key, String defaultValue) {
    String stringValue = map.get(key);
    if (stringValue == null) {
      stringValue = defaultValue;
    }

    return stringValue;
  }

  @Override
  public int getAsInt(MiasmaKey key) {
    return getAsInt(key, Integer.valueOf(key.defaultValue()));
  }

  @Override
  public int getAsInt(MiasmaKey key, Integer defaultValue) {
    String stringValue = map.get(key);
    if (stringValue == null) {
      stringValue = defaultValue.toString();
    }

    return Integer.valueOf(stringValue);
  }

  @Override
  public boolean getAsBoolean(MiasmaKey key) {
    return getAsBoolean(key, Boolean.valueOf(key.defaultValue()));
  }

  @Override
  public boolean getAsBoolean(MiasmaKey key, Boolean defaultValue) {
    String stringValue = map.get(key);
    if (stringValue == null) {
      if (defaultValue == null) {
        return false;
      }
      stringValue = defaultValue.toString();
    }

    if (stringValue.equals("1") || stringValue.equals("1.0")) {
      stringValue = Boolean.toString(true);
    } else if (stringValue.equals("0") || stringValue.equals("0.0")) {
      stringValue = Boolean.toString(false);
    }

    return Boolean.valueOf(stringValue);
  }

  @Override
  public String get(MiasmaKey key) {
    return map.get(key);
  }

}
