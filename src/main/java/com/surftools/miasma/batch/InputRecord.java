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

/**
 * represents an record read from spreadsheet; may not be "sendable" as is (non-email, non-sms, multiple sms, not
 * complete
 */
public record InputRecord(String batchId, String fileName, String tabName, String rowNumber, InputStatus status, //
    String from, String to, String text) {

  public static String[] getHeaders() {
    return new String[] { "Batch", "File", "Tab", "Row", "Status", "From", "To", "Message" };
  }

  public String[] getValues() {
    return new String[] { batchId, fileName, tabName, rowNumber, status.name(), from, to, text };
  }

  public InputRecord update(InputStatus newStatus, String newFrom, String newTo, String newText) {
    return new InputRecord(batchId, fileName, tabName, rowNumber, newStatus, newFrom, newTo, newText);
  }

}
