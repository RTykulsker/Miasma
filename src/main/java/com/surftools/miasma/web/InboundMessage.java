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

package com.surftools.miasma.web;

import java.time.LocalDateTime;

/**
 * encapsulate what we receive from client
 */
public record InboundMessage(LocalDateTime dateTimeAccepted, int sequenceNumber, String clientIP, //
    String from, String to, String message) {

  public static String[] getHeaders() {
    return new String[] { "Date/Time", "Sequence", "Client IP", //
        "From", "To", "Message", };
  }

  public String[] getValues() {
    return new String[] { dateTimeAccepted.toString(), String.valueOf(sequenceNumber), clientIP, //
        from, to, message };
  }

  public String digits() {
    return to.replaceAll("[^\\d.]", "");
  }

  public boolean isEmail() {
    return digits().length() != 10;
  }
}
