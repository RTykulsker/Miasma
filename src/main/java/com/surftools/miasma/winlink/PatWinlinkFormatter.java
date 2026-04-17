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

package com.surftools.miasma.winlink;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.miasma.Colorizer;
import com.surftools.miasma.config.IConfigurationManager;
import com.surftools.miasma.config.MiasmaKey;
import com.surftools.miasma.io.IASMessage;
import com.surftools.miasma.io.IoUtils;
import com.surftools.miasma.io.MessageWriter;

public class PatWinlinkFormatter extends AbstractWinlinkFormatter {
  static final Logger logger = LoggerFactory.getLogger(PatWinlinkFormatter.class);
  static final SmsProvider smsProvider = SmsProvider.RRI;

  protected String sender;
  protected String smsEmailReplacementAddress;
  protected String patPathString;
  protected Colorizer cz;

  public PatWinlinkFormatter(IConfigurationManager cm) {
    super(cm);
    cz = new Colorizer(cm);

    sender = cm.getAsString(MiasmaKey.APP_WINLINK_EXPRESS_SENDER);
    smsEmailReplacementAddress = cm.getAsString(MiasmaKey.APP_SMS_REPLACEMENT_EMAIL_ADDRESS);
    patPathString = cm.getAsString(MiasmaKey.APP_PAT_PATH);

    IoUtils.makeDirIfNeeded(Path.of(patPathString));
    IoUtils.makeDirIfNeeded(Path.of(patPathString, sender));
    for (var patDir : List.of("archive", "in", "out", "sent", "pending")) {
      var path = IoUtils.makeDirIfNeeded(Path.of(patPathString, sender, patDir));
      logger.info("using PAT dir: " + path.toString());
    }
  }

  @Override
  public void format(List<IASMessage> messages, Path inboxFilePath) {
    if (messages.size() == 0) {
      logger.debug("received empty message list");
      return;
    }
    logger.info(cz.color("info", "received: " + messages.size() + " IASMessages from: " + inboxFilePath.getFileName()));
    super.format(messages, inboxFilePath);
  }

  private void handleCommon(IASMessage m, String address, boolean isEmail) {
    final DateTimeFormatter MESSAGE_TIME_DTF = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

    var messageId = generateMid(m.fromName() + m.toAddress() + m.text() + IoUtils.getMilliStamp());
    var body = sanitizeBody(m, isEmail);

    var sb = new StringBuilder();
    sb.append("Mid: " + messageId + SEP);
    sb.append("Body: " + (body.length() + SEP.length()) + SEP);
    sb.append("Content-Transfer-Encoding: 8bit" + SEP);
    sb.append("Content-Type: text/plain; charset=ISO-8859-1" + SEP);
    sb.append("Date: " + MESSAGE_TIME_DTF.format(LocalDateTime.now(Clock.systemUTC())) + SEP);
    sb.append("From: " + sender + SEP);
    sb.append("Mbo: " + sender + SEP);
    sb.append("Subject: I'M SAFE" + SEP);
    sb.append("To: " + address + SEP);
    sb.append("Type: Private" + SEP);
    sb.append(SEP);
    sb.append(body + SEP);

    /*
     * NOTE WELL: Watch Services and event polling in a tight loop are very
     * efficient. We *CAN NOT* (slowly) write a file into a directory that is being
     * watched We *MUST* write to a pending/holding/staging/temp directory and then
     * (atomically) move
     */
    var path = Path.of(patPathString, sender, "pending", messageId + ".b2f");
    try {
      Files.writeString(path, sb.toString());
      IoUtils.moveWithFileName(path, Path.of(patPathString, sender, "out"));
      var acceptedMessage = new IASMessage(m.fromName(), address, m.text(), m.dateString(), m.timeString(),
          m.fileName(), m.fileTypeName(), m.fileSourceName(), messageId, "isEmail: " + isEmail);
      MessageWriter.writeMessage(acceptedMessagePath, acceptedMessage);
      logger.info(cz.color("ok", "wrote PAT b2f file: " + messageId));
    } catch (Exception e) {
      logger.error("Exception writing b2f file: " + path + ", " + e.getLocalizedMessage());
    }

  }

  @Override
  protected void handleEmail(IASMessage m, String address) {
    handleCommon(m, "SMTP:" + address, true);
  }

  @Override
  protected void handleSms(IASMessage m, String address) {
    if (smsProvider == SmsProvider.RRI) {
      if (smsEmailReplacementAddress == null) {
        var alphaNumericAddress = address.replaceAll("[^a-zA-Z0-9]", "");
        var digitsOnlyAddress = alphaNumericAddress.replaceAll("[a-zA-Z]", "");
        handleCommon(m, "SMTP:" + digitsOnlyAddress + "@sms.radiorelay.org", false);
      } else {
        var parts = smsEmailReplacementAddress.split("@");
        if (parts.length == 2) {
          address = parts[0] + "+MIASMA.sms@" + parts[1];
          handleCommon(m, "SMTP:" + address, false);
        } else {
          throw new RuntimeException("badly configured smsEmailReplacementAddress: " + smsEmailReplacementAddress);
        }
      }
    }
  }
}