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

import com.surftools.config.IConfigurationManager;

public class BaseBatchProcessor {
  protected String batchId;
  protected File file;
  protected IConfigurationManager cm;

  private boolean isAutoDitto = false; // TODO from cm
  private static String lastFrom = "";
  private static String lastTo = "";
  private static String lastText = "";

  public BaseBatchProcessor(String batchId, File file, IConfigurationManager cm) {
    this.batchId = batchId;
    this.file = file;
    this.cm = cm;
  }

  public ProcessResult parse(InputRecord inputRecord) {
    var okList = new ArrayList<InputRecord>();
    var errorList = new ArrayList<InputRecord>();
    var counterContext = new CounterContext(batchId);

    var fromValue = inputRecord.from();
    var toValue = inputRecord.to();
    var textValue = inputRecord.text();

    if (isAutoDitto) {
      fromValue = fromValue.isEmpty() ? lastFrom : fromValue;
      toValue = toValue.isEmpty() ? lastTo : toValue;
      textValue = textValue.isEmpty() ? lastText : textValue;
    }

    var inputStatus = InputStatus.OK;
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

    if (inputStatus == InputStatus.OK) {
      if (fromValue.toLowerCase().equals("from") && toValue.toLowerCase().equals("to")) {
        inputStatus = InputStatus.HEADER;
      }
    }

    if (inputStatus == InputStatus.OK) {
      var tos = toValue.split(";|,");
      for (var address : tos) {
        address = address.trim();

        var isEmail = address.contains("@");
        if (!isEmail) {
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
        } else { // not email
          inputStatus = InputStatus.OK_EMAIL;
        }

        var newInputRecord = inputRecord.update(inputStatus, address);

        if (inputStatus == InputStatus.OK_EMAIL || inputStatus == InputStatus.OK_SMS) {
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
      errorList.add(inputRecord);
      counterContext.fromCounter.increment(fromValue);
      counterContext.toCounter.increment(toValue);
      counterContext.textCounter.increment(textValue);
      counterContext.statusCounter.increment(inputStatus);
    } // end if inputStatus != O

    return new ProcessResult(okList, errorList, counterContext);
  } // end process()

}
