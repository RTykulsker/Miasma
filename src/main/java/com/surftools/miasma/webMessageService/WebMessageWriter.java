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

package com.surftools.miasma.webMessageService;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVWriter;
import com.surftools.config.IConfigurationManager;
import com.surftools.config.MiasmaKey;
import com.surftools.miasma.web.InboundMessage;

public class WebMessageWriter {
  private static final Logger logger = LoggerFactory.getLogger(WebMessageWriter.class);

  protected static final DateTimeFormatter BODY_DTF = DateTimeFormatter.ofPattern("M/dd");
  protected static final DateTimeFormatter FILE_DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
  protected static final DateTimeFormatter MESSAGE_TIME_DTF = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
  protected static final DateTimeFormatter MIME_DTF = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss +0000");

  protected IConfigurationManager cm;

  private Path patPath;
  private boolean isPatEnabled;

  private Path winlinkExpressPath;
  private boolean isWinlinkExpresEnabled;

  private Path csvPath;
  private boolean isCsvEnabled;
  private StringBuilder cvsFileContent = new StringBuilder();

  private final String sender;
  private SmsType smsType;

  public WebMessageWriter(IConfigurationManager cm) throws Exception {
    this.cm = cm;

    sender = cm.getAsString(MiasmaKey.APP_WRITER_WINLINK_EXPRESS_SENDER);
    if (sender == null) {
      throw new RuntimeException("must specify " + MiasmaKey.APP_WRITER_WINLINK_EXPRESS_SENDER);
    }

    var smsTypeString = cm.getAsString(MiasmaKey.APP_SMS_TYPE, SmsType.RRI.toString());
    smsType = SmsType.fromString(smsTypeString);
    if (smsType == null) {
      throw new RuntimeException("Unsupported SmsType: " + smsTypeString);
    } else {
      logger.info("SMS provider: " + smsType.toString());
    }

    var replacementAddress = cm.getAsString(MiasmaKey.APP_WRITER_SMS_REPLACEMENT_EMAIL_ADDRESS);
    if (replacementAddress.equals("(null)") && (smsType == SmsType.EMAIL || smsType == SmsType.RAINBOW)) {
      throw new RuntimeException("SmsType set to EMAIL, but replacement address set to (null)");
    }
    WebOutboundMessage.setSmsReplacementEmailAddress(replacementAddress);

    var patPathString = cm.getAsString(MiasmaKey.APP_WRITER_PAT_PATH);
    if (patPathString == null) {
      isPatEnabled = false;
    } else {
      isPatEnabled = true;
      patPath = Path.of(patPathString);
      Files.createDirectories(patPath);
      Files.createDirectories(Path.of(patPath.toString(), sender));
      for (var dir : List.of("archive", "in", "out", "sent")) {
        Files.createDirectories(Path.of(patPath.toString(), sender, dir));
      }
    }

    var winlinExpressPathString = cm.getAsString(MiasmaKey.APP_WRITER_WINLINK_EXPRESS_PATH);
    if (winlinExpressPathString == null) {
    } else {
      isWinlinkExpresEnabled = true;
      winlinkExpressPath = Path.of(winlinExpressPathString);
      Files.createDirectories(winlinkExpressPath);
    }

    var csvOutputPathString = cm.getAsString(MiasmaKey.APP_WRITER_CSV_PATH);
    if (csvOutputPathString == null) {
      isCsvEnabled = false;
    } else {
      isCsvEnabled = true;

      var csvDirPath = Path.of(csvOutputPathString);
      Files.createDirectories(csvDirPath);

      csvPath = Path.of(csvDirPath.toString(), "miasma.csv");
      var file = new File(csvPath.toString());

      if (!file.exists()) {
        var stringWriter = new StringWriter();
        CSVWriter writer = new CSVWriter(stringWriter);
        writer.writeNext(WebOutboundMessage.getHeaders());
        writer.close();
        var stringBuffer = stringWriter.getBuffer();
        cvsFileContent.append(stringBuffer.toString());
        Files.writeString(csvPath, cvsFileContent.toString());
      } else {
        var lines = Files.readAllLines(csvPath);
        logger.info("read " + (lines.size() - 1) + " lines from " + csvPath.toString());
        cvsFileContent.append(String.join("\n", lines));
        cvsFileContent.append("\n");
      }

    }
  }

  /**
   *
   * @param inboundMessage
   */
  public void write(InboundMessage inboundMessage) {
    var outboundMessages = makeOutboundMessages(inboundMessage);
    writeOutboundMessages(outboundMessages);
  }

  private List<WebOutboundMessage> makeOutboundMessages(InboundMessage inboundMessage) {
    var list = new ArrayList<WebOutboundMessage>();
    if (inboundMessage.isEmail()) {
      var outboundMessage = new WebOutboundMessage(inboundMessage, smsType);
      list.add(outboundMessage);
      return list;
    }

    if (smsType != SmsType.RAINBOW) {
      list.add(new WebOutboundMessage(inboundMessage, smsType));
    } else {
      for (var type : SmsType.RAINBOX_LIST) {
        list.add(new WebOutboundMessage(inboundMessage, type));
      }
    }

    return list;
  }

  private void writeOutboundMessages(List<WebOutboundMessage> messages) {
    for (var message : messages) {
      writePatFile(message);
      writeWindowsExpressFile(message);
      writeCsvLine(message);
    }
  }

  private void writeCsvLine(WebOutboundMessage m) {
    if (!isCsvEnabled) {
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
      cvsFileContent.append(messageContent);
      Files.writeString(csvPath, cvsFileContent.toString());
      logger.info("wrote miasma.cvs file: " + csvPath);
    } catch (Exception e) {
      logger.error("Exception writing miasma.csv file: " + csvPath + ", " + e.getLocalizedMessage());
    }
  }

  private void writePatFile(WebOutboundMessage m) {
    if (!isPatEnabled) {
      logger.info("didn't create PAT message because not enabled");
      return;
    }

    final String SEP = "\r\n";

    var body = m.body.replaceAll("\n", SEP);
    body = body.replaceAll("[^\\x00-\\x7F]", "."); // translate non-ascii characters
    body = body.replaceAll("\\u009d", "");

    var sb = new StringBuilder();
    sb.append("Mid: " + m.messageId + SEP);
    sb.append("Body: " + (body.length() + SEP.length()) + SEP);
    sb.append("Content-Transfer-Encoding: 8bit" + SEP);
    sb.append("Content-Type: text/plain; charset=ISO-8859-1" + SEP);
    sb.append("Date: " + MESSAGE_TIME_DTF.format(LocalDateTime.now(Clock.systemUTC())) + SEP);
    sb.append("From: " + sender + SEP);
    sb.append("Mbo: " + sender + SEP);
    sb.append("Subject: I'M SAFE" + SEP);
    sb.append("To: " + m.to + SEP);
    sb.append("Type: Private" + SEP);
    sb.append(SEP);
    sb.append(body + SEP);

    var path = Path.of(patPath.toString(), sender, "out", m.messageId + ".b2f");
    try {
      Files.writeString(path, sb.toString());
      logger.info("wrote PAT b2f file: " + path);
    } catch (Exception e) {
      logger.error("Exception writing b2f file: " + path + ", " + e.getLocalizedMessage());
    }

  }

  private void writeWindowsExpressFile(WebOutboundMessage m) {
    if (!isWinlinkExpresEnabled) {
      logger.info("didn't create WinlinkExpress message because not enabled");
      return;
    }

    var body = m.body;
    body = body.replaceAll("<", "&lt;");
    body = body.replaceAll("<=", "&lt;=3D");
    body = body.replaceAll(">", "&gt;");
    body = body.replaceAll(">=", "&gt;=3D");

    var messageTime = MESSAGE_TIME_DTF.format(m.inMessage.dateTimeAccepted());
    var mimeTime = MIME_DTF.format(utcOf());
    var text = messageTemplate;
    text = text.replaceAll("#MESSAGE_ID#", m.messageId);
    text = text.replaceAll("#MESSAGE_TIME#", messageTime);
    text = text.replaceAll("#MIME_TIME#", mimeTime); // ? mimeTime
    text = text.replaceAll("#SENDER#", sender);
    text = text.replaceAll("#TO#", m.to);
    text = text.replaceAll("#SUBJECT#", m.subject);
    text = text.replaceAll("#BODY#", body);

    var path = Path
        .of(winlinkExpressPath.toString(), "miasma-" + FILE_DTF.format(m.inMessage.dateTimeAccepted()) + ".xml");
    try {
      Files.writeString(path, text);
      logger.info("wrote Winlink Express output to : " + path.toString());
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  private LocalDateTime utcOf() {
    var fields = Instant.now().toString().split("T"); // 2025-01-22T02:27:38.304917131Z
    var dateFields = fields[0].split("-");
    var utcDate = LocalDate.of(atoi(dateFields[0]), atoi(dateFields[1]), atoi(dateFields[2]));
    var timeFields = fields[1].split("\\.")[0].split(":");
    var utcTime = LocalTime.of(atoi(timeFields[0]), atoi(timeFields[1]), atoi(timeFields[2]));
    var utcDateTime = LocalDateTime.of(utcDate, utcTime);
    return utcDateTime;
  }

  private int atoi(String s) {
    return Integer.valueOf(s);
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
            <subject>#SUBJECT#</subject>
            <time>#MESSAGE_TIME#</time>
            <sender>#SENDER#</sender>
            <acknowledged></acknowledged>
            <attachmentsopened></attachmentsopened>
            <replied></replied>
            <rmsoriginator></rmsoriginator>
            <rmsdestination></rmsdestination>
            <rmspath></rmspath>
            <location></location>
            <csize></csize>
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
      Subject: #SUBJECT#
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
