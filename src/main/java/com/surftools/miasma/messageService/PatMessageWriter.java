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

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.config.ConfigurationKey;
import com.surftools.config.IConfigurationManager;

public class PatMessageWriter extends AbstractBaseMessageWriter {
  private static final Logger logger = LoggerFactory.getLogger(PatMessageWriter.class);

  protected IConfigurationManager cm;

  private Path outputPath;
  protected boolean isEnabled = true;

  private String sender;

  public PatMessageWriter(IConfigurationManager cm) throws Exception {
    this.cm = cm;
    var outputPathString = cm.getAsString(ConfigurationKey.APP_WRITER_PAT_PATH);
    if (outputPathString == null) {
      isEnabled = false;
    }

    outputPath = Path.of(outputPathString);
    Files.createDirectory(outputPath);

    sender = cm.getAsString(ConfigurationKey.APP_WRITER_WINLINK_EXPRESS_SENDER);
    Files.createDirectory(Path.of(outputPath.toString(), sender));

    for (var dir : List.of("archive", "in", "out", "sent")) {
      Files.createDirectory(Path.of(outputPath.toString(), sender, dir));
    }

  }

  @Override
  public void write(IamSafeMessage m) {
    if (!isEnabled) {
      logger.info("didn't create PAT message because not enabled");
      return;
    }

    final String SEP = "\r\n";

    var body = "From " + m.from() + " " + BODY_DTF.format(UtcDateTime.ofNow()) + " ONE WAY MESSAGE" + SEP
        + m.message().replaceAll("\n", SEP);
    body = body.replaceAll("[^\\x00-\\x7F]", "."); // translate non-ascii characters
    body = body.replaceAll("\\u009d", "");

    var sb = new StringBuilder();
    sb.append("Mid: " + m.messageId() + SEP);
    sb.append("Body: " + (body.length() + SEP.length()) + SEP);
    sb.append("Content-Transfer-Encoding: 8bit" + SEP);
    sb.append("Content-Type: text/plain; charset=ISO-8859-1" + SEP);
    sb.append("Date: " + MESSAGE_TIME_DTF.format(LocalDateTime.now(Clock.systemUTC())) + SEP);
    sb.append("From: " + sender + SEP);
    sb.append("Mbo: " + sender + SEP);
    sb.append("Subject: I'M SAFE" + SEP);
    sb.append("To: " + m.outboundURL() + SEP);
    sb.append("Type: Private" + SEP);
    sb.append(SEP);
    sb.append(body + SEP);

    var path = Path.of(outputPath.toString(), sender, "out", m.messageId() + ".b2f");
    try {
      Files.writeString(path, sb.toString());
      logger.info("wrote PAT b2f file: " + path);
    } catch (Exception e) {
      logger.error("Exception writing b2f file: " + path + ", " + e.getLocalizedMessage());
    }

  }

}
