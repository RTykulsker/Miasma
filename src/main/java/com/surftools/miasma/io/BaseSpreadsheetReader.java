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
package com.surftools.miasma.io;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.miasma.Colorizer;
import com.surftools.miasma.config.IConfigurationManager;
import com.surftools.miasma.config.MiasmaKey;

public abstract class BaseSpreadsheetReader implements IMessageReader {
  private static final Logger logger = LoggerFactory.getLogger(BaseSpreadsheetReader.class);
  protected Colorizer cz;
  protected boolean isAutoDittoEnabled = false;

  public BaseSpreadsheetReader(IConfigurationManager cm) {
    cz = new Colorizer(cm);
    isAutoDittoEnabled = cm.getAsBoolean(MiasmaKey.BATCH_AUTO_DITTO_ENABLED, isAutoDittoEnabled);
  }

  static record IASKey(String fromName, String text) {
    static IASKey fromIASMessage(IASMessage m) {
      return new IASKey(m.fromName(), m.text());
    }
  }

  protected List<IASMessage> coalesce(List<IASMessage> inputList) {
    var outputList = new ArrayList<IASMessage>();
    var map = new LinkedHashMap<IASKey, List<IASMessage>>();

    // add message to a list, based on key (fromName, text)
    for (var m : inputList) {
      var key = IASKey.fromIASMessage(m);
      var keyList = map.getOrDefault(key, new ArrayList<IASMessage>());
      keyList.add(m);
      map.put(key, keyList);
    }

    // for each key, coalesce all toAddresses in list, update "a" message
    for (var key : map.keySet()) {
      var keyList = map.get(key);
      var toAddresses = String.join(";", keyList.stream().map(i -> i.toAddress()).toList());
      var m = keyList.get(0);
      m = m.updateToAddresses(toAddresses);
      outputList.add(m);
    }

    if (outputList.size() != inputList.size()) {
      logger.info(cz.color("info", "inputList size: " + inputList.size() + ", outputList size: " + outputList.size()));
    }
    return outputList;
  }
}
