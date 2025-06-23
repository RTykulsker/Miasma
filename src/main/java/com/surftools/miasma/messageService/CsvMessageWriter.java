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

package com.surftools.miasma.messageService;

import java.io.File;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVWriter;
import com.surftools.config.ConfigurationKey;
import com.surftools.config.IConfigurationManager;
import com.surftools.miasma.utils.FileUtils;

public class CsvMessageWriter extends AbstractBaseMessageWriter {
  private static final Logger logger = LoggerFactory.getLogger(CsvMessageWriter.class);

  protected IConfigurationManager cm;

  private Path outputPath;
  private Path path;
  protected boolean isEnabled = true;

  private StringBuilder content = new StringBuilder();

  public CsvMessageWriter(IConfigurationManager cm) {
    this.cm = cm;
    var outputPathString = cm.getAsString(ConfigurationKey.APP_WRITER_CSV_PATH);
    if (outputPathString == null) {
      isEnabled = false;
    }

    outputPath = Path.of(outputPathString);
    FileUtils.createDirectory(outputPath);

    path = Path.of(outputPath.toString(), "miasma.csv");
    var file = new File(path.toString());

    try {
      if (!file.exists()) {
        var stringWriter = new StringWriter();
        CSVWriter writer = new CSVWriter(stringWriter);
        writer.writeNext(IamSafeMessage.getHeaders());
        writer.close();
        var stringBuffer = stringWriter.getBuffer();
        content.append(stringBuffer.toString());
        Files.writeString(path, content.toString());
      } else {
        var lines = Files.readAllLines(path);
        logger.info("read " + (lines.size() - 1) + " lines from " + path.toString());
        content.append(String.join("\n", lines));
        content.append("\n");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void write(IamSafeMessage m) {
    if (!isEnabled) {
      logger.info("didn't create CSV message because not enabled");
      return;
    }

    try {
      var stringWriter = new StringWriter();
      CSVWriter writer = new CSVWriter(stringWriter);
      writer.writeNext(m.getValues());
      writer.close();
      var stringBuffer = stringWriter.getBuffer();
      var messageContent = stringBuffer.toString();
      content.append(messageContent);
      Files.writeString(path, content.toString());
      logger.info("wrote miasma.cvs file: " + path);
    } catch (Exception e) {
      logger.error("Exception writing miasma.csv file: " + path + ", " + e.getLocalizedMessage());
    }

  }

}
