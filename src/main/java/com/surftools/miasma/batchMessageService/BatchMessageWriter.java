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

package com.surftools.miasma.batchMessageService;

import java.io.File;
import java.io.FileWriter;
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
import com.surftools.miasma.batch.CounterContext;
import com.surftools.miasma.batch.Deduplicator;
import com.surftools.miasma.batch.ProcessResult;
import com.surftools.miasma.batch.SpreadsheetRecord;
import com.surftools.miasma.webMessageService.SmsType;

public class BatchMessageWriter {
  private static final Logger logger = LoggerFactory.getLogger(BatchMessageWriter.class);

  protected static final DateTimeFormatter BODY_DTF = DateTimeFormatter.ofPattern("M/dd");
  protected static final DateTimeFormatter FILE_DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
  protected static final DateTimeFormatter MESSAGE_TIME_DTF = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
  protected static final DateTimeFormatter MIME_DTF = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss +0000");

  protected IConfigurationManager cm;

  private Path patPath;
  private boolean isPatEnabled;

  private Path winlinkExpressPath;
  private boolean isWinlinkExpresEnabled;

  private Path csvOkPath;
  private Path csvErrorPath;
  private Path csvCounterContextPath;
  private boolean isCsvEnabled;

  private final String sender;
  private SmsType smsType;

  /**
   * write out the various files that will be used to actually send message (writeWinlinkExpresFile), writePatFile
   *
   * or
   *
   * write files that document what we've done, writeCvsLine, writeCounterContext
   *
   * @param cm
   * @throws Exception
   */
  public BatchMessageWriter(IConfigurationManager cm) throws Exception {
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

    var replacementAddress = cm.get(MiasmaKey.APP_WRITER_SMS_REPLACEMENT_EMAIL_ADDRESS);
    if (replacementAddress == null && (smsType == SmsType.EMAIL || smsType == SmsType.RAINBOW)) {
      throw new RuntimeException("SmsType set to EMAIL, but replacement address set to (null)");
    }
    BatchOutboundMessage.setSmsReplacementEmailAddress(replacementAddress);

    var patPathString = cm.get(MiasmaKey.APP_WRITER_PAT_PATH);
    if (patPathString == null) {
      isPatEnabled = false;
      logger.warn("PAT processing NOT enabled");
    } else {
      isPatEnabled = true;
      patPath = Path.of(patPathString);
      Files.createDirectories(patPath);
      Files.createDirectories(Path.of(patPath.toString(), sender));
      for (var dir : List.of("archive", "in", "out", "sent")) {
        Files.createDirectories(Path.of(patPath.toString(), sender, dir));
      }
    }

    var winlinExpressPathString = cm.get(MiasmaKey.APP_WRITER_WINLINK_EXPRESS_PATH);
    if (winlinExpressPathString == null) {
      isWinlinkExpresEnabled = false;
      logger.warn("Winlink Express processing NOT enabled");
    } else {
      isWinlinkExpresEnabled = true;
      winlinkExpressPath = Path.of(winlinExpressPathString);
      Files.createDirectories(winlinkExpressPath);
    }

    var csvOutputPathString = cm.getAsString(MiasmaKey.APP_WRITER_CSV_PATH);
    if (csvOutputPathString == null) {
      isCsvEnabled = false;
      logger.warn("CSV processing NOT enabled");
    } else {
      isCsvEnabled = true;
      var csvDirPath = Path.of(csvOutputPathString);
      Files.createDirectories(csvDirPath);
      csvOkPath = Path.of(csvDirPath.toString(), "batch-details.csv");
      csvErrorPath = Path.of(csvDirPath.toString(), "batch-details-ERRORS.csv");
      csvCounterContextPath = Path.of(csvDirPath.toString(), "counterContext.csv");
      for (var path : List.of(csvOkPath, csvErrorPath, csvCounterContextPath)) {
        var file = new File(path.toString());
        if (!file.exists()) {
          var stringWriter = new StringWriter();
          var csvWriter = new CSVWriter(stringWriter);
          var isCounterContext = path.toString().endsWith("counterContext.csv");
          var headers = isCounterContext ? CounterContext.getHeaders() : BatchOutboundMessage.getHeaders();
          csvWriter.writeNext(headers);
          csvWriter.close();
          Files.writeString(path, stringWriter.getBuffer().toString());
        }
      }
    }
  }

  /**
   * our public interface
   *
   * @param result
   */
  public void write(ProcessResult result) {
    writeCounterContext(result.counterContext());
    var deduplicator = new Deduplicator();

    var okList = deduplicator.deduplicate(true, result.okList());
    var outboundMessages = makeOutboundMessages(okList);
    writeOutboundMessages(outboundMessages);

    var errorList = deduplicator.deduplicate(false, result.errorList());
    var errorMessages = makeOutboundMessages(errorList);
    for (var errorMessage : errorMessages) {
      writeCsvLine(false, errorMessage);
    }
  }

  private void writeCounterContext(CounterContext counterContext) {
    if (!isCsvEnabled) {
      logger.info("didn't append CSV counterContext because not enabled");
      return;
    }

    try {
      var stringWriter = new StringWriter();
      CSVWriter csvWriter = new CSVWriter(stringWriter);
      csvWriter.writeNext(counterContext.getValues());
      csvWriter.close();
      var stringBuffer = stringWriter.getBuffer();
      var messageContent = stringBuffer.toString();

      var fileWriter = new FileWriter(csvCounterContextPath.toString(), true);
      fileWriter.write(messageContent);
      fileWriter.close();

      logger.info("Appended to cvs counterContext file: " + csvCounterContextPath);
    } catch (Exception e) {
      logger.error("Exception appending to file: " + csvCounterContextPath + ", " + e.getLocalizedMessage());
    }

  }

  /**
   * explode inputRecords into OutboundMessages, because of SMS rainbox
   *
   * @param spreadsheetRecords
   * @return
   */
  private List<BatchOutboundMessage> makeOutboundMessages(List<SpreadsheetRecord> spreadsheetRecords) {
    var list = new ArrayList<BatchOutboundMessage>();
    for (var spreadsheetRecord : spreadsheetRecords) {
      if (spreadsheetRecord.status().isEmail()) {
        var outboundMessage = new BatchOutboundMessage(spreadsheetRecord, null);
        list.add(outboundMessage);
      } else {
        if (smsType != SmsType.RAINBOW) {
          list.add(new BatchOutboundMessage(spreadsheetRecord, smsType));
        } else {
          for (var type : SmsType.RAINBOX_LIST) {
            list.add(new BatchOutboundMessage(spreadsheetRecord, type));
          }
        }
      }
    }

    logger.info("in records: " + spreadsheetRecords.size() + " in, out messages " + list.size());
    return list;
  }

  private void writeOutboundMessages(List<BatchOutboundMessage> messages) {
    writeWindowsExpressFile(messages);
    for (var message : messages) {
      writePatFile(message);
      writeCsvLine(true, message);
    }
  }

  private void writeCsvLine(boolean isOk, BatchOutboundMessage m) {
    if (!isCsvEnabled) {
      logger.debug("didn't append CSV message because not enabled");
      return;
    }

    var path = isOk ? csvOkPath : csvErrorPath;
    try {
      var stringWriter = new StringWriter();
      CSVWriter csvWriter = new CSVWriter(stringWriter);
      csvWriter.writeNext(m.getValues());
      csvWriter.close();
      var stringBuffer = stringWriter.getBuffer();
      var messageContent = stringBuffer.toString();

      var fileWriter = new FileWriter(path.toString(), true);
      fileWriter.write(messageContent);
      fileWriter.close();

      logger.debug("Appended to cvs file: " + path);
    } catch (Exception e) {
      logger.error("Exception appending to csv file: " + path + ", " + e.getLocalizedMessage());
    }
  }

  private void writePatFile(BatchOutboundMessage m) {
    if (!isPatEnabled) {
      logger.debug("didn't create PAT message because not enabled");
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
      logger.debug("wrote PAT b2f file: " + path);
    } catch (Exception e) {
      logger.error("Exception writing b2f file: " + path + ", " + e.getLocalizedMessage());
    }

  }

  private void writeWindowsExpressFile(List<BatchOutboundMessage> messages) {
    if (!isWinlinkExpresEnabled) {
      logger.info("didn't create WinlinkExpress message because not enabled");
      return;
    }

    if (messages.size() == 0) {
      logger.info("didn't create WinlinkExpress message because no messages");
      return;
    }

    LocalDateTime dateTimeAccepted = null;
    var allMessages = new StringBuilder();
    for (var m : messages) {
      var body = m.body;
      body = body.replaceAll("<", "&lt;");
      body = body.replaceAll("<=", "&lt;=3D");
      body = body.replaceAll(">", "&gt;");
      body = body.replaceAll(">=", "&gt;=3D");

      if (dateTimeAccepted == null) {
        dateTimeAccepted = LocalDateTime.parse(m.inputRecord.batchId(), FILE_DTF);
      }

      var messageTime = MESSAGE_TIME_DTF.format(dateTimeAccepted);
      var mimeTime = MIME_DTF.format(utcOf());
      var text = messageTemplate;
      text = text.replaceAll("#MESSAGE_ID#", m.messageId);
      text = text.replaceAll("#MESSAGE_TIME#", messageTime);
      text = text.replaceAll("#MIME_TIME#", mimeTime); // ? mimeTime
      text = text.replaceAll("#SENDER#", sender);
      text = text.replaceAll("#TO#", m.to);
      text = text.replaceAll("#SUBJECT#", m.subject);
      text = text.replaceAll("#BODY#", body);

      allMessages.append(text);
    }

    var text = messagesTemplate;
    text = text.replace("#MESSAGES#", allMessages.toString());
    var path = Path.of(winlinkExpressPath.toString(), "miasma-" + FILE_DTF.format(dateTimeAccepted) + ".xml");
    try {
      Files.writeString(path, text);
      logger.info("wrote " + messages.size() + " Winlink Express messages to : " + path.toString());
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

  private static String messagesTemplate = """
      <?xml version="1.0"?>
      <Winlink_Express_message_export>
        <export_parameters>
          <xml_file_version>1.0</xml_file_version>
          <winlink_express_version>1.7.24.0</winlink_express_version>
        </export_parameters>
        <message_list>
          #MESSAGES#
        </message_list>
      </Winlink_Express_message_export>
            """;

  private static String messageTemplate = """
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
            """;
}
