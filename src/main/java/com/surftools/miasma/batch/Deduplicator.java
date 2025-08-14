/**

The MIT License (MIT)

Copyright (c) 2025, Robert Tykulsker

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

package com.surftools.miasma.batch;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * because spectrum is scarce and it's just too dang easy to create duplicate messages
 */
public class Deduplicator {

  record Mini(String from, String to, String text) {
    static Mini fromSpreadsheet(SpreadsheetRecord in) {
      return new Mini(in.from(), in.to(), in.text());
    }
  }

  record Extra(String fileName, String tabName, String rowNumber) {
    static Extra fromSpreadsheet(SpreadsheetRecord in) {
      return new Extra(in.fileName(), in.tabName(), in.rowNumber());
    }
  }

  private static final Logger logger = LoggerFactory.getLogger(ExcelBatchProcessor.class);

  public List<SpreadsheetRecord> deduplicate(boolean isOkList, List<SpreadsheetRecord> inputs) {
    var label = isOkList ? "OK" : "error";
    logger.info("received: " + inputs.size() + " " + label + " input spreadsheet records");
    var outputs = new ArrayList<SpreadsheetRecord>(inputs.size());
    var map = new LinkedHashMap<Mini, List<SpreadsheetRecord>>();

    for (var spreadsheet : inputs) {
      var mini = Mini.fromSpreadsheet(spreadsheet);
      var list = map.getOrDefault(mini, new ArrayList<SpreadsheetRecord>());
      list.add(spreadsheet);
      map.put(mini, list);
    }

    for (var mini : map.keySet()) {
      var list = map.get(mini);
      outputs.add(list.get(0));
      if (list.size() > 1) {
        var extras = new ArrayList<Extra>(list.size());
        for (var spreadsheet : list) {
          extras.add(Extra.fromSpreadsheet(spreadsheet));
        }
        logger
            .info("DUPES: found " + list.size() + " " + label + " duplicates for: " + mini.toString() + ", "
                + extras.stream().map(Object::toString).collect(Collectors.joining(", ")));
      }
    }

    logger.info("returned: " + outputs.size() + " " + label + " output spreadsheet records");
    return outputs;
  }
}
