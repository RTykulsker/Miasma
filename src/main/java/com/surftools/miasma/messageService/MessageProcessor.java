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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.config.ConfigurationKey;
import com.surftools.config.IConfigurationManager;

/**
 * do something useful with the IamSafeMessage
 */
public class MessageProcessor {
  private static final Logger logger = LoggerFactory.getLogger(MessageProcessor.class);

  private List<IMessageWriter> writers;
  private String patPathString = null;

  public MessageProcessor(IConfigurationManager cm) throws Exception {
    writers = List.of(new WinlinkExpressMessageWriter(cm), new PatMessageWriter(cm), new CsvMessageWriter(cm));

    var patStartOnMessage = cm.getAsBoolean(ConfigurationKey.APP_PAT_START_ON_MESSAGE);
    if (patStartOnMessage) {
      patPathString = cm.getAsString(ConfigurationKey.APP_PAT_PATH);
      logger.info("will start PAT on each message via: " + patPathString);
    } else {
      logger.info("will NOT start PAT on each message");
    }
  }

  public void process(IamSafeMessage iAmSafeMessage) {
    for (var writer : writers) {
      writer.write(iAmSafeMessage);
    }

    if (patPathString != null) {
      try {
        // TODO throttle, locking
        var processBuilder = new ProcessBuilder(patPathString);
        processBuilder.start();
      } catch (Exception e) {
        logger.error("caught exception starting process for: " + patPathString + ", " + e.getMessage());
      }
    }

  }

}
