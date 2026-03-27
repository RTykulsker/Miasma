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

package com.surftools.miasmaV2.io;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * text file input: fromName,toAddress and text on separate lines
 *
 * multiple messages not allowed
 */
public class WebMessageReader implements IMessageReader {
  static final Logger logger = LoggerFactory.getLogger(WebMessageReader.class);

  @Override
  public List<IASMessage> readFile(Path path, FileType fileType, FileSource fileSource) {
    var list = new ArrayList<IASMessage>();
    var now = LocalDateTime.now();
    var dateString = now.toLocalDate().toString();
    var timeString = now.toLocalTime().truncatedTo(ChronoUnit.SECONDS).toString();
    var fileName = path.getFileName().toString();
    var fileTypeName = fileType.name();
    var fileSourceName = fileSource.name();
    var messageId = "";

    try {
      var lines = Files.readAllLines(path);
      if (lines.size() >= 3) {
        var fromName = lines.get(0).strip();
        var toAddress = lines.get(1).strip();
        var text = lines.get(2).strip();
        var metadata = getMetadata(path, fromName, toAddress, text, dateString, timeString, fileName, fileTypeName);
        var message = new IASMessage(fromName, toAddress, text, dateString, timeString, fileName, fileTypeName,
            fileSourceName, messageId, metadata);
        list.add(message);
      }
    } catch (Exception e) {
      logger.error("Exception reading file: " + path.getFileName() + ", " + e.getMessage());
    }

    logger.info("returning " + list.size() + " messages from file: " + path.getFileName());
    return list;
  }

  protected String getMetadata(Path path, String fromName, String toAddress, String text, //
      String dateString, String timeString, String fileName, String fileTypeName) {
    return "";
  }

}
