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

import java.io.FileReader;
import java.io.Reader;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.surftools.miasma.MiasmaApp;
import com.surftools.miasma.config.IConfigurationManager;
import com.surftools.miasma.config.MiasmaKey;

public class CsvMessageReader implements IMessageReader {
  private static final Logger logger = LoggerFactory.getLogger(MiasmaApp.class);

  boolean isAutoDittoEnabled = false;

  public CsvMessageReader(IConfigurationManager cm) {
    isAutoDittoEnabled = cm.getAsBoolean(MiasmaKey.BATCH_AUTO_DITTO_ENABLED);
  }

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
    var metadata = "";

    var lastFromName = "";
    var lastToAddresses = "";
    var lastText = "";
    var fieldsList = readCsvFileIntoFieldsArray(path, ',', false, 1);
    for (var fields : fieldsList) {
      var fromName = fields.length >= 1 ? fields[0].strip() : "";
      var toAddresses = fields.length >= 2 ? fields[1].strip() : "";
      var text = fields.length >= 3 ? fields[2].strip() : "";
      if (fromName.isEmpty() && toAddresses.isEmpty() && text.isEmpty()) {
        continue;
      }
      if (isAutoDittoEnabled) {
        fromName = !fromName.isEmpty() ? fromName : lastFromName;
        toAddresses = !toAddresses.isEmpty() ? toAddresses : lastToAddresses;
        text = !text.isEmpty() ? text : lastText;
      }
      lastFromName = fromName;
      lastToAddresses = toAddresses;
      lastText = text;

      var message = new IASMessage(fromName, toAddresses, text, dateString, timeString, fileName, fileTypeName,
          fileSourceName, messageId, metadata);
      list.add(message);
    }
    return list;
  }

  public static List<String[]> readCsvFileIntoFieldsArray(Path inputPath, char separator, boolean ignoreQuotes,
      int skipLines) {
    var list = new ArrayList<String[]>();

    if (!inputPath.toFile().exists()) {
      logger.warn("file: " + inputPath.toString() + " not found");
      return list;
    }

    var rowCount = -1;
    try {
      Reader reader = new FileReader(inputPath.toString());
      CSVParser parser = new CSVParserBuilder() //
          .withSeparator(separator) //
            .withIgnoreQuotations(ignoreQuotes) //
            .build();
      CSVReader csvReader = new CSVReaderBuilder(reader) //
          .withSkipLines(skipLines)//
            .withCSVParser(parser)//
            .build();
      rowCount = 1;
      String[] fields = null;
      while ((fields = csvReader.readNext()) != null) {
        ++rowCount;
        list.add(fields);
      }
    } catch (Exception e) {
      logger.error("Exception reading " + inputPath.toString() + ", row " + rowCount + ", " + e.getLocalizedMessage());
    }

    logger.info("returning: " + list.size() + " records from: " + inputPath.toString());
    return list;
  }
}
