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

import java.util.LinkedHashMap;

import com.surftools.utils.counter.Counter;

public class CounterContext {
  public final String batchId;
  public int folderCount = 0;
  public int fileCount = 0;
  public int tabCount = 0;
  public Counter fromCounter = new Counter();
  public Counter toCounter = new Counter();
  public Counter textCounter = new Counter();
  public Counter statusCounter = new Counter();

  public CounterContext(String batchId) {
    this.batchId = batchId;
  }

  public void accumulate(CounterContext childCounterContext) {
    this.folderCount += childCounterContext.folderCount;
    this.fileCount += childCounterContext.fileCount;
    this.tabCount += childCounterContext.tabCount;
    this.fromCounter.merge(childCounterContext.fromCounter);
    this.toCounter.merge(childCounterContext.toCounter);
    this.textCounter.merge(childCounterContext.textCounter);
    this.statusCounter.merge(childCounterContext.statusCounter);
  }

  public static String[] getHeaders() {
    return new String[] { "BatchId", "Folders", "Files", "Tabs", "Froms", "Tos", "Texts", "Statuses" };
  }

  public String[] getValues() {
    return new String[] { batchId, String.valueOf(folderCount), String.valueOf(fileCount), String.valueOf(tabCount), //
        String.valueOf(fromCounter.getKeyCount()), String.valueOf(toCounter.getKeyCount()),
        String.valueOf(textCounter.getKeyCount()), //
        String.valueOf(statusCounter.getKeyCount()) };
  }

  @Override
  public String toString() {
    var sb = new StringBuilder();
    sb.append("\n");
    sb.append("Folder count: " + folderCount + "\n");
    sb.append("File count: " + fileCount + "\n");
    sb.append("Tab count: " + tabCount + "\n");
    var map = new LinkedHashMap<String, Counter>();
    map.put("From values: ", fromCounter);
    map.put("To values", toCounter);
    map.put("Text values: ", textCounter);
    map.put("Status values: ", statusCounter);
    for (var key : map.keySet()) {
      sb.append(key + "\n");
      var counter = map.get(key);
      var iterator = counter.getDescendingCountIterator();
      while (iterator.hasNext()) {
        var entry = iterator.next();
        sb.append("  value: " + entry.getKey() + ", count: " + entry.getValue() + "\n");
      }
      sb.append("\n");
    }
    return sb.toString();
  }
}
