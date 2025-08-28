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

package com.surftools.miasma.batch;

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.config.IConfigurationManager;
import com.surftools.config.MiasmaKey;

public class BaseBatchProcessor {
  private static final Logger logger = LoggerFactory.getLogger(BaseBatchProcessor.class);

  protected String batchId;
  protected File file;
  protected IConfigurationManager cm;

  private boolean isAutoDitto;
  private String lastFrom = "";
  private String lastTo = "";
  private String lastText = "";

  private String altFromColumnName = "";

  private int maxMessageLength;

  private static boolean isInitialized = false;

  public BaseBatchProcessor(String batchId, File file, IConfigurationManager cm) {
    this.batchId = batchId;
    this.file = file;
    this.cm = cm;

    if (!isInitialized) {
      isAutoDitto = cm.getAsBoolean(MiasmaKey.BATCH_AUTO_DITTO_ENABLED, Boolean.FALSE);
      logger.info("IsAutoDitto: " + isAutoDitto);

      altFromColumnName = cm.getAsString(MiasmaKey.BATCH_EXCEL_ALT_FROM_COLUMN_NAME, "");
      logger.info("Alt 'From' column name: " + altFromColumnName);
      altFromColumnName = altFromColumnName.toLowerCase();

      maxMessageLength = cm.getAsInt(MiasmaKey.BATCH_MAX_MESSAGE_LENGTH, Integer.valueOf(92));
      logger.info("Max Message Length: " + maxMessageLength);

      isInitialized = true;
    }
  }

  /**
   * one ingredient in our "secret sauce"
   *
   * validate/invalidate spreadsheet row
   *
   * explode the list of addressees in the "to" field
   *
   * @param inputRecord
   * @return
   */
  public ProcessResult parseSpreadsheetRecord(SpreadsheetRecord inputRecord) {
    // our inputs
    var fromValue = inputRecord.from();
    var toValue = inputRecord.to();
    var textValue = inputRecord.text();

    // our outputs
    var okList = new ArrayList<SpreadsheetRecord>();
    var errorList = new ArrayList<SpreadsheetRecord>();
    var counterContext = new CounterContext(batchId);

    // ignore headers; return "empty" ProcessResult that'll get merged
    if (inputRecord.rowNumber().equals("1")) {
      return new ProcessResult(okList, errorList, counterContext);
    }

    if (isAutoDitto) {
      fromValue = fromValue.isEmpty() ? lastFrom : fromValue;
      toValue = toValue.isEmpty() ? lastTo : toValue;
      textValue = textValue.isEmpty() ? lastText : textValue;
    }

    // dispose of any "global" errors here
    var inputStatus = getGlobalStatus(fromValue, toValue, textValue);
    if (inputStatus != InputStatus.UNKNOWN) {
      counterContext.fromCounter.increment(fromValue);
      counterContext.toCounter.increment(toValue);
      counterContext.textCounter.increment(textValue);
      counterContext.statusCounter.increment(inputStatus);
      errorList.add(inputRecord); // original input record
      lastFrom = fromValue;
      lastTo = toValue;
      lastText = textValue;
      return new ProcessResult(okList, errorList, counterContext);
    }

    // now, we're going to assume the status is OK, but we must explode the toValue
    var tos = toValue.split(";|,");
    for (var address : tos) {
      address = address.trim();

      var isEmail = isValidEmailAddress(address);
      if (!isEmail) {
        if (isValidHamCallsign(address) || isValidWinlinkTacticalAddress(address)) {
          inputStatus = InputStatus.OK_WINLINK;
        } else {
          var digits = new StringBuilder();
          for (var i = 0; i < address.length(); ++i) {
            var c = address.charAt(i);
            if (Character.isDigit(c)) {
              digits.append(c);
            }
          } // end loop over characters in address
          address = digits.toString();
          if (address.length() == 10) {
            inputStatus = InputStatus.OK_SMS;
          } else {
            inputStatus = InputStatus.CANT_PARSE_TO_FIELDS;
          }
        }
      } else { // not email
        inputStatus = InputStatus.OK_EMAIL;
      }

      var newInputRecord = inputRecord.update(inputStatus, fromValue, address, textValue);
      okList.add(newInputRecord);

      counterContext.fromCounter.increment(fromValue);
      counterContext.toCounter.increment(address);
      counterContext.textCounter.increment(textValue);
      counterContext.statusCounter.increment(inputStatus);

      lastFrom = fromValue;
      lastTo = toValue;
      lastText = textValue;
    } // end loop over addresses

    return new ProcessResult(okList, errorList, counterContext);
  }

  private boolean isHeader(SpreadsheetRecord inputRecord) {
    int rowNumber = -1;
    try {
      rowNumber = Integer.valueOf(inputRecord.rowNumber());
    } catch (Exception e) {
      logger.info("could not parse row number: " + inputRecord.toString());
      return false;
    }

    // must be 1
    if (rowNumber != 1) {
      return false;
    }

    // TODO what if from/to are empty?

    var fromValue = inputRecord.from().toLowerCase();
    if (fromValue.startsWith(altFromColumnName)) {
      return true;
    }

    var toValue = inputRecord.to().toLowerCase();
    if (fromValue.equals("from") && toValue.equals("to")) {
      return true;
    }

    return false;
  }

  /**
   * return a "global" status for an input spreadsheet, without exploding to fields
   *
   * @param fromValue
   * @param toValue
   * @param textValue
   * @return
   */
  private InputStatus getGlobalStatus(String fromValue, String toValue, String textValue) {
    var inputStatus = InputStatus.UNKNOWN;

    if (fromValue.isEmpty() && toValue.isEmpty() && textValue.isEmpty()) {
      inputStatus = InputStatus.NO_FROM_TO_AND_TEXT_FIELDS;
    } else if (fromValue.isEmpty() && toValue.isEmpty() && !textValue.isEmpty()) {
      inputStatus = InputStatus.NO_FROM_AND_TO_FIELDS;
    } else if (fromValue.isEmpty() && !toValue.isEmpty() && textValue.isEmpty()) {
      inputStatus = InputStatus.NO_FROM_AND_TEXT_FIELDS;
    } else if (fromValue.isEmpty() && !toValue.isEmpty() && !textValue.isEmpty()) {
      inputStatus = InputStatus.NO_FROM_FIELD;
    } else if (!fromValue.isEmpty() && toValue.isEmpty() && textValue.isEmpty()) {
      inputStatus = InputStatus.NO_TO_AND_TEXT_FIELDS;
    } else if (!fromValue.isEmpty() && toValue.isEmpty() && !textValue.isEmpty()) {
      inputStatus = InputStatus.NO_TO_FIELD;
    } else if (!fromValue.isEmpty() && !toValue.isEmpty() && textValue.isEmpty()) {
      inputStatus = InputStatus.NO_TEXT_FIELD;
    }

    if (inputStatus == InputStatus.UNKNOWN && textValue.length() > maxMessageLength) {
      inputStatus = InputStatus.TEXT_TOO_LONG;
    }
    return inputStatus;
  }

  private boolean isValidEmailAddress(String email) {
    final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    if (email == null)
      return false;
    email = email.trim();
    return EMAIL_PATTERN.matcher(email).matches();
  }

  private boolean isValidWinlinkTacticalAddress(String address) {
    if (address == null) {
      return false;
    }

    address = address.trim().toUpperCase();

    if (address.length() < 3 || address.length() > 12) {
      return false;
    }

    if (!Character.isLetter(address.charAt(0))) { // Must start with a letter
      return false;
    }

    for (char c : address.toCharArray()) { // Must be alphanumeric or underscore only
      if (!Character.isLetterOrDigit(c) && c != '_') {
        return false;
      }
    }

    return true;
  }

  private boolean isValidHamCallsign(String callsign) {
    if (callsign == null)
      return false;

    callsign = callsign.trim().toUpperCase();

    if (callsign.length() < 3 || callsign.length() > 8) { // Length check (typical range: 3 to 8 characters)
      return false;
    }

    if (!callsign.matches("^[A-Z0-9]+$")) { // Valid characters: A-Z and digits only
      return false;
    }

    return callsign.matches("^[A-Z0-9]{1,3}[0-9][A-Z0-9]{1,4}$");
  }

  // public BaseBatchProcessor() {
  // }
  //
  // void run() {
  // String[] testEmails = { "user@example.com", "john.doe@sub.domain.co.uk", "user_name123@domain.net", "bademail@",
  // "@missinglocal.com", "noatsymbol.com", "user@domain", "user@domain.c", "user@domain.toolongtld" };
  //
  // for (var email : testEmails) {
  // System.out.printf("%-30s : %s%n", email, isValidEmailAddress(email));
  // }
  //
  // String[] testCallsigns = { "K7ABC", "W1AW", "VE3XYZ", "JA1NUT", "M0ABC", "3D2XYZ", "GB22HQ", "W100USA", "ZS1XYZ",
  // "DL7XYZ", "N0CALL", "K1D", "9A1AA", "5H3ABC", "BAD@CALL", "TOOLONGCALLSIGN" };
  //
  // for (String cs : testCallsigns) {
  // System.out.printf("%-15s : %s%n", cs, isValidHamCallsign(cs));
  // }
  //
  // String[] testAddresses = { "EOC", "shelter1", "net_control", "123start", "K7ABC", "bad@addr",
  // "too_long_address_here" };
  //
  // for (String addr : testAddresses) {
  // System.out.printf("%-20s : %s%n", addr, isValidWinlinkTacticalAddress(addr));
  // }
  //
  // }
  //
  // public static void main(String[] args) {
  // var app = new BaseBatchProcessor();
  //
  // app.run();
  //
  // }

}
