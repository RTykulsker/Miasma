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

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.codec.binary.Base32;

import com.surftools.miasma.batch.SpreadsheetRecord;
import com.surftools.miasma.webMessageService.SmsType;

public class BatchOutboundMessage {
  protected static final DateTimeFormatter BODY_DTF = DateTimeFormatter.ofPattern("M/dd");
  protected static final DateTimeFormatter FILE_DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
  protected static final DateTimeFormatter MESSAGE_TIME_DTF = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
  protected static final DateTimeFormatter MIME_DTF = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss +0000");

  public final SpreadsheetRecord inputRecord;
  public final SmsType smsType;
  public final String messageId;
  public final LocalDateTime dateTimeAccepted;

  public String to;
  public String subject;
  public String body;

  static String smsEmailAddress = null;

  public BatchOutboundMessage(SpreadsheetRecord spreadsheetRecord, SmsType smsType) {
    this.inputRecord = spreadsheetRecord;
    this.smsType = smsType;
    this.dateTimeAccepted = LocalDateTime.parse(spreadsheetRecord.batchId(), FILE_DTF);

    this.messageId = generateMid(
        String.join(",", List.of(spreadsheetRecord.toString(), (smsType == null) ? "null" : smsType.toString())));

    var imSafe = "I'm safe!"; // or maybe I'M SAFE MSG as per RRI/Quick and WE

    var isEmail = spreadsheetRecord.status().isEmail();

    if (isEmail) {
      to = spreadsheetRecord.to();
      subject = imSafe;
      body = "From " + spreadsheetRecord.from() + " " + FILE_DTF.format(dateTimeAccepted) + " ONE WAY MSG" + "\n"
          + spreadsheetRecord.text();
    } else {
      switch (smsType) {
      case RRI:
        to = spreadsheetRecord.to() + "@sms.radiorelay.org";
        subject = imSafe;
        body = "From " + spreadsheetRecord.from() + " " + BODY_DTF.format(dateTimeAccepted) + " ONE WAY MSG" + "\n"
            + spreadsheetRecord.text();
        break;

      case NA7Q:
        to = "sms@hamdesk.com";
        subject = spreadsheetRecord.to();
        body = imSafe + "\n" + spreadsheetRecord.text();
        break;

      case PHILIPPINES:
        to = "winlink2sms@gmail.com";
        subject = spreadsheetRecord.to();
        var oneLineBody = (imSafe + " " + spreadsheetRecord.text()).replaceAll("\r\n", ". ");

        body = "From " + spreadsheetRecord.from() + "\n" //
            + "Sent on " + MESSAGE_TIME_DTF.format(dateTimeAccepted).replaceAll("/", "-") + "\n" //
            + oneLineBody;

        break;

      case EMAIL:
        to = smsEmailAddress;
        subject = imSafe;
        body = "From " + spreadsheetRecord.from() + " " + BODY_DTF.format(dateTimeAccepted) + " ONE WAY MSG" + "\n"
            + spreadsheetRecord.text();
        break;

      default:
        break;
      }
    }
  }

  public static String[] getHeaders() {
    var list = new ArrayList<String>();
    list.addAll(Arrays.asList(SpreadsheetRecord.getHeaders()));
    list.addAll(Arrays.asList(new String[] { "IsEmail", "Sms Type", "MessageId", "To", "Subject", "Body"//
    }));
    return list.toArray(new String[0]);
  }

  public String[] getValues() {
    var isEmail = inputRecord.status().isEmail() ? "true" : "false";
    var smsTypeString = smsType == null ? "(none)" : smsType.toString();
    var list = new ArrayList<String>();
    list.addAll(Arrays.asList(inputRecord.getValues()));
    list.addAll(Arrays.asList(new String[] { isEmail, smsTypeString, messageId, to, subject, body//
    }));
    return list.toArray(new String[0]);
  }

  private String generateMid(String string) {
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

  public static void setSmsReplacementEmailAddress(String emailAddress) {
    smsEmailAddress = emailAddress;
  }
}
