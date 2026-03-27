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

package com.surftools.miasmaV2.winlink;

import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.binary.Base32;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.diogonunes.jcolor.Ansi;
import com.diogonunes.jcolor.Attribute;
import com.surftools.miasmaV2.config.IConfigurationManager;
import com.surftools.miasmaV2.config.MiasmaKey;
import com.surftools.miasmaV2.io.IASMessage;
import com.surftools.miasmaV2.io.IoUtils;
import com.surftools.miasmaV2.io.MessageWriter;

/**
 * common methods for all @IWinlinkFormatter classes
 */
public abstract class AbstractWinlinkFormatter implements IWinlinkFormatter {
  static final Logger logger = LoggerFactory.getLogger(AbstractWinlinkFormatter.class);
  protected static final String SEP = "\r\n";
  protected IConfigurationManager cm;
  protected Path acceptedMessagePath;
  protected Path rejectedMessagePath;

  protected boolean truncateEnabled;
  protected int maxLengthEmail;
  protected int maxLengthSms;

  public AbstractWinlinkFormatter(IConfigurationManager cm) {
    this.cm = cm;

    var rootPathString = cm.getAsString(MiasmaKey.ROOT_PATH);
    var messagesPath = IoUtils.makeDirIfNeeded(Path.of(rootPathString, "files", "messages"));
    acceptedMessagePath = Path.of(messagesPath.toString(), "acceptedMessages.csv");
    rejectedMessagePath = Path.of(messagesPath.toString(), "rejectedMessages.csv");

    truncateEnabled = cm.getAsBoolean(MiasmaKey.BODY_MAX_LENGTH_TRUNCATE_ENABLED, true);
    maxLengthEmail = cm.getAsInt(MiasmaKey.BODY_MAX_LENGTH_EMAIL, 500);
    maxLengthSms = cm.getAsInt(MiasmaKey.BODY_MAX_LENGTH_EMAIL, 90);

    // this is important enough to log
    logger.info(colorizeWarn(MiasmaKey.BODY_MAX_LENGTH_TRUNCATE_ENABLED + ": " + truncateEnabled));
    logger.info(colorizeWarn(MiasmaKey.BODY_MAX_LENGTH_EMAIL + ": " + maxLengthEmail));
    logger.info(colorizeWarn(MiasmaKey.BODY_MAX_LENGTH_EMAIL + ": " + maxLengthSms));
  }

  protected String colorizeWarn(String s) {
    final var textColor = Attribute.BLACK_TEXT();
    final var backgroundColor = Attribute.BACK_COLOR(255, 165, 0);
    return Ansi.colorize(s, textColor, backgroundColor);
  }

  @Override
  public void format(List<IASMessage> messages, Path inboxFilePath) {
    for (var m : messages) {
      var errors = isCommonValidation(m);
      if (errors != null) {
        var errorMessage = m.updateMetadata(errors);
        logger.info(Ansi.colorize("rejecting message: " + errorMessage, Attribute.BLACK_TEXT(), Attribute.RED_BACK()));
        MessageWriter.writeMessage(rejectedMessagePath, errorMessage);
        continue;
      }

      var addresses = m.toAddress().split("[,;\\s]+");
      for (var address : addresses) {
        if (isEmailAddress(m, address)) {
          handleEmail(m, address);
        } else {
          handleSms(m, address);
        }
      } // end loop over addresses in toAddress
    } // end loop over messages in list
  }

  protected abstract void handleSms(IASMessage m, String address);

  protected abstract void handleEmail(IASMessage m, String address);

  private boolean isEmailAddress(IASMessage m, String address) {
    if (address.contains("@")) {
      return true;
    }

    var alphaNumericAddress = address.replaceAll("[^a-zA-Z0-9]", "");
    var digitsOnly = alphaNumericAddress.replaceAll("[a-zA-Z]", "");
    var alphaOnly = alphaNumericAddress.replaceAll("[0-9]", "");
    if (digitsOnly.length() > 0 && alphaOnly.length() == 0) {
      return false;
    }

    return true;
  }

  protected String generateMid(String string) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      String stringToHash = string + System.nanoTime();
      md.update(stringToHash.getBytes());
      byte[] digest = md.digest();
      Base32 base32 = new Base32();
      String encodedString = base32.encodeToString(digest);
      String subString = encodedString.substring(0, 12);
      return subString;
    } catch (Exception e) {
      throw new RuntimeException("could not generate messageId: " + e.getMessage());
    }
  }

  /**
   * common validation for all @IASMessage messages
   *
   * @param message
   * @return null if no errors, else String with description
   */
  protected String isCommonValidation(IASMessage m) {
    var errors = new ArrayList<String>();

    if (StringUtils.isBlank(m.fromName())) {
      errors.add("empty fromName");
    }

    if (StringUtils.isBlank(m.toAddress())) {
      errors.add("empty toAddress");
    }

    if (StringUtils.isBlank(m.text())) {
      errors.add("empty text");
    }

    if (errors.size() == 0) {
      return null;
    } else {
      return String.join(",", errors);
    }
  }

  /**
   * for transport via winlink
   *
   * @param isEmail
   *
   * @param text
   * @return
   */
  protected String sanitizeBody(IASMessage m, boolean isEmail) {
    var text = m.text();
    text = text.replaceAll("\n", SEP);
    text = text.replaceAll("[^\\x00-\\x7F]", "."); // translate non-ascii characters
    text = text.replaceAll("\\u009d", "");

    var fromName = m.fromName();
    var fields = m.fromName().split(" ");
    if (fields.length > 1) {
      var second = fields[1].strip();
      if (second.length() > 0) {
        fromName = fields[0] + " " + second.charAt(0);
      }
    }

    var localDate = LocalDate.now();
    var dateString = localDate.getMonthValue() + "/" + localDate.getDayOfMonth();
    var header = "From " + fromName + " " + dateString + " ONE WAY MSG" + SEP;
    var body = header + text;

    if (truncateEnabled) {
      var maxLength = isEmail ? maxLengthEmail : maxLengthSms;
      if (body.length() > maxLength) {
        logger.warn(colorizeWarn("### message body length > " + maxLength + ", truncating"));
        body = body.substring(0, maxLength);
      }
    }

    return body;
  }

}
