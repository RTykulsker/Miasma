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
 * represents status of a row read from a spreadsheet file
 */
public enum InputStatus {
  UNKNOWN, // status when read, but before parsing
  HEADER, // first row, no further processing
  OK_EMAIL, OK_SMS, OK_WINLINK, // valid parsing, will be sent
  NO_FROM_FIELD, NO_TO_FIELD, NO_TEXT_FIELD, // missing single field
  NO_FROM_AND_TO_FIELDS, NO_TO_AND_TEXT_FIELDS, NO_FROM_AND_TEXT_FIELDS, // missing two fields
  NO_FROM_TO_AND_TEXT_FIELDS, // missing all three fields
  CANT_PARSE_TO_FIELDS, // not a valid email, SMS (10 digit) or ham call
  TEXT_TOO_LONG; // based on configurable limit

  public boolean isEmail() {
    return this == OK_EMAIL || this == OK_WINLINK;
  }

  public boolean isOk() {
    return this == OK_EMAIL || this == OK_SMS || this == OK_WINLINK;
  }

}
