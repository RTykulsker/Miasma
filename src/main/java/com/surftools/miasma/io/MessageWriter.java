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

import java.io.FileWriter;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVWriter;

public class MessageWriter {
  static final Logger logger = LoggerFactory.getLogger(MessageWriter.class);

  /**
   * write an IASMessage to a file. Create the file if needed
   *
   * @param path
   * @param message
   */
  public static void writeMessage(Path path, IASMessage message) {
    try {
      var parentPath = path.getParent();
      IoUtils.makeDirIfNeeded(parentPath);

      var file = path.toFile();
      var needsHeader = !file.exists();

      CSVWriter writer = new CSVWriter(new FileWriter(path.toString(), true));
      if (needsHeader) {
        writer.writeNext(message.getHeaders());
      }
      writer.writeNext(message.getValues());
      writer.close();
      logger.debug("wrote: " + message + " to: " + path);
    } catch (Exception e) {
      logger.error("Exception writing file: " + path + ", " + e.getLocalizedMessage());
    }
  }
}
