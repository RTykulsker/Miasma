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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.config.ConfigurationKey;
import com.surftools.config.IConfigurationManager;

public class WinlinkExpressMessageWriter extends AbstractBaseMessageWriter {
  private static final Logger logger = LoggerFactory.getLogger(WinlinkExpressMessageWriter.class);

  protected IConfigurationManager cm;

  private Path outputPath;
  protected boolean isEnabled = true;

  private String sender;

  public WinlinkExpressMessageWriter(IConfigurationManager cm) throws Exception {
    this.cm = cm;
    var outputPathString = cm.getAsString(ConfigurationKey.APP_WRITER_WINLINK_EXPRESS_PATH);
    if (outputPathString == null) {
      isEnabled = false;
    }

    outputPath = Path.of(outputPathString);
    Files.createDirectories(outputPath);

    sender = cm.getAsString(ConfigurationKey.APP_WRITER_WINLINK_EXPRESS_SENDER);
  }

  @Override
  public void write(IamSafeMessage m) {
    if (!isEnabled) {
      logger.info("didn't create Winlink Express message because not enabled");
      return;
    }

    var body = "From " + m.from() + " " + BODY_DTF.format(m.dateTimeAccepted()) + " ONE WAY MESSAGE\n";
    body += m.message();
    body = body.replaceAll("<", "&lt;");
    body = body.replaceAll("<=", "&lt;=3D");
    body = body.replaceAll(">", "&gt;");
    body = body.replaceAll(">=", "&gt;=3D");

    var messageTime = MESSAGE_TIME_DTF.format(m.dateTimeAccepted());
    var mimeTime = MIME_DTF.format(UtcDateTime.ofNow());
    var text = messageTemplate;
    text = text.replaceAll("#MESSAGE_ID#", m.messageId());
    text = text.replaceAll("#MESSAGE_TIME#", messageTime);
    text = text.replaceAll("#MIME_TIME#", mimeTime);
    text = text.replaceAll("#SENDER#", sender);
    text = text.replaceAll("#TO#", m.outboundURL());
    text = text.replaceAll("#BODY#", body);

    var path = Path.of(outputPath.toString(), "miasma-" + FILE_DTF.format(m.dateTimeAccepted()) + ".xml");
    try {
      Files.writeString(path, text);
      logger.info("wrote Winlink Express output to : " + path.toString());
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  private static String messageTemplate = """
      <?xml version="1.0"?>
      <Winlink_Express_message_export>
        <export_parameters>
          <xml_file_version>1.0</xml_file_version>
          <winlink_express_version>1.7.24.0</winlink_express_version>
        </export_parameters>
        <message_list>
          <message>
            <id>#MESSAGE_ID#</id>
            <foldertype>Fixed</foldertype>
            <folder>Outbox</folder>
            <subject>I'M SAFE MSG</subject>
            <time>#MESSAGE_TIME#</time>
            <sender>#SENDER#</sender>
            <acknowledged></acknowledged>
            <attachmentsopened></attachmentsopened>
            <replied></replied>
            <rmsoriginator></rmsoriginator>
            <rmsdestination></rmsdestination>
            <rmspath></rmspath>
            <location></location>
            <csize>254</csize>
            <downloadserver></downloadserver>
            <forwarded></forwarded>
            <messageserver></messageserver>
            <precedence>2</precedence>
            <peertopeer>False</peertopeer>
            <routingflag></routingflag>
            <replied></replied>
            <source>#SENDER#</source>
            <unread>False</unread>
            <flags>0</flags>
            <messageoptions>False|False||||False|</messageoptions>
            <mime>Date: #MIME_TIME#
      From: #SENDER#@winlink.org
      Reply-To: #SENDER#@winlink.org
      Subject: I'M SAFE MSG
      To: #TO#
      Message-ID: #MESSAGE_ID#
      X-Source: #SENDER#
      MIME-Version: 1.0

      #BODY#</mime>
          </message>
        </message_list>
      </Winlink_Express_message_export>
            """;

}
