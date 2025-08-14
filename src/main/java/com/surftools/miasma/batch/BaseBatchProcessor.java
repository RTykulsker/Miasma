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
  private static final Pattern callsignPattern = Pattern.compile("[a-zA-Z0-9]{1,3}[0-9][a-zA-Z0-9]{0,3}[a-zA-Z]");

  protected String batchId;
  protected File file;
  protected IConfigurationManager cm;

  private boolean isAutoDitto;
  private String lastFrom = "";
  private String lastTo = "";
  private String lastText = "";

  private int maxMessageLength;

  public BaseBatchProcessor(String batchId, File file, IConfigurationManager cm) {
    this.batchId = batchId;
    this.file = file;
    this.cm = cm;

    isAutoDitto = cm.getAsBoolean(MiasmaKey.BATCH_AUTO_DITTO_ENABLED, Boolean.FALSE);
    logger.info("IsAutoDitto: " + isAutoDitto);

    maxMessageLength = cm.getAsInt(MiasmaKey.BATCH_MAX_MESSAGE_LENGTH, Integer.valueOf(92));
    logger.info("Email Max Message Length: " + maxMessageLength);
  }

  public ProcessResult parse(SpreadsheetRecord inputRecord) {

    var okList = new ArrayList<SpreadsheetRecord>();
    var errorList = new ArrayList<SpreadsheetRecord>();
    var counterContext = new CounterContext(batchId);

    var fromValue = inputRecord.from();
    var toValue = inputRecord.to();
    var textValue = inputRecord.text();

    if (isAutoDitto) {
      fromValue = fromValue.isEmpty() ? lastFrom : fromValue;
      toValue = toValue.isEmpty() ? lastTo : toValue;
      textValue = textValue.isEmpty() ? lastText : textValue;
    }

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

    if (inputStatus == InputStatus.UNKNOWN) {
      if (fromValue.toLowerCase().equals("from") && toValue.toLowerCase().equals("to")) {
        inputStatus = InputStatus.HEADER;
      }
    }

    if (inputStatus == InputStatus.UNKNOWN) {
      if (textValue.length() > maxMessageLength) {
        inputStatus = InputStatus.TEXT_TOO_LONG;
      }
    }

    if (inputStatus == InputStatus.UNKNOWN) { // still UNKNOWN, ready to proceed
      var tos = toValue.split(";|,");
      for (var address : tos) {
        address = address.trim();

        var isEmail = address.contains("@");
        if (!isEmail) {
          var matcher = callsignPattern.matcher(address);
          if (matcher.find()) {
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

        if (inputStatus.isOk()) {
          okList.add(newInputRecord);
        } else {
          errorList.add(newInputRecord);
        }

        counterContext.fromCounter.increment(fromValue);
        counterContext.toCounter.increment(address);
        counterContext.textCounter.increment(textValue);
        counterContext.statusCounter.increment(inputStatus);
      } // end loop over addresses

    } else { // end if inputStatus == OK
      if (inputStatus != InputStatus.HEADER) {
        var newInputRecord = inputRecord.update(inputStatus, fromValue, toValue, textValue);
        errorList.add(newInputRecord);
        counterContext.fromCounter.increment(fromValue);
        counterContext.toCounter.increment(toValue);
        counterContext.textCounter.increment(textValue);
        counterContext.statusCounter.increment(inputStatus);
      }
    } // end if inputStatus != O

    if (inputStatus != InputStatus.HEADER) {
      lastFrom = fromValue;
      lastTo = toValue;
      lastText = textValue;
    }

    return new ProcessResult(okList, errorList, counterContext);
  } // end process()

}
