/**

The MIT License (MIT)

Copyright (c) 2022, Robert Tykulsker

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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * https://en.wikipedia.org/wiki/Common_Log_Format
 *
 * 127.0.0.1 user-identifier frank [10/Oct/2000:13:55:36 -0700] "GET /apache_pb.gif HTTP/1.0" 200 2326
 *
 * @author bobt
 *
 */
public class CommonLogger {
  private static final Logger logger = LoggerFactory.getLogger(CommonLogger.class);

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss xxxx");

  public void log(String message) {
    logger.info(message);
  }

  public String log(//
      String clientIpAddress, //
      String userName, //
      String method, //
      String resource, //
      int returnCode, //
      int byteCount) {

    String timeString = FORMATTER.format(ZonedDateTime.of(LocalDateTime.now(), ZoneId.systemDefault()));

    var sb = new StringBuilder();
    sb.append(format(clientIpAddress) + " ");
    sb.append("- ");
    sb.append(format(userName) + " ");
    sb.append("[" + timeString + "] ");
    sb.append(format(method, resource) + " ");
    sb.append(returnCode + " ");
    sb.append(byteCount);
    var string = sb.toString();

    logger.info(string);

    return string;
  }

  private String format(String method, String resource) {
    return "\"" + method + " " + resource + " HTTP/1.0\"";
  }

  private String format(String s) {
    if (s == null || s.length() == 0) {
      return "-";
    } else {
      return s;
    }
  }
}
