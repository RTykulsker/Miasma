/**

The MIT License (MIT)

Copyright (c) 2026, Robert Tykulsker

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

package com.surftools.miasma;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;

import com.diogonunes.jcolor.Ansi;
import com.diogonunes.jcolor.Attribute;
import com.surftools.miasma.config.IConfigurationManager;
import com.surftools.miasma.config.MiasmaKey;

public class ColorLogger {
  record Attributes(Attribute text, Attribute back) {
  };

  private static Map<String, Attributes> colorMap;
  private static boolean isMapDumped;

  static {
    colorMap = new LinkedHashMap<>();
    colorMap.put("ok", new Attributes(Attribute.BLACK_TEXT(), Attribute.GREEN_BACK()));
    colorMap.put("info", new Attributes(Attribute.WHITE_TEXT(), Attribute.BLUE_BACK()));
    colorMap.put("error", new Attributes(Attribute.WHITE_TEXT(), Attribute.RED_BACK()));
    colorMap.put("warn", new Attributes(Attribute.BLACK_TEXT(), Attribute.YELLOW_BACK()));
    colorMap.put("warn2", new Attributes(Attribute.BLACK_TEXT(), Attribute.BACK_COLOR(255, 165, 0)));
  }

  private Logger logger;
  private boolean isColorEnabled;

  public ColorLogger(Logger logger, IConfigurationManager cm) {
    this.logger = logger;
    isColorEnabled = cm.getAsBoolean(MiasmaKey.APP_COLOR_TEXT_ENABLED, true);

    if (!isMapDumped && isColorEnabled) {
      for (var key : colorMap.keySet()) {
        log(key, "this is for color key: " + key);
      }
      isMapDumped = true;
    }
  }

  public void log(String key, String text) {
    var attributes = colorMap.get(key);
    if (attributes != null && isColorEnabled) {
      text = Ansi.colorize(text, attributes.text, attributes.back);
    }
    logger.info(text);
  }
}
