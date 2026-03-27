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

package com.surftools.miasmaV2.io;

/**
 * an I Am Safe message, the primary DTO (Data Transfer Object
 */
public record IASMessage( //
    String fromName, // name of person sending message
    String toAddress, // address of recipient(s)
    String text, // the message the person is trying to send
    String dateString, //
    String timeString, //
    String fileName, //
    String fileTypeName, // @FileType name
    String fileSourceName, // @FileSource name
    String messageId, // Winlink messageId
    String metadata // extra information
) implements IWritable {

  /**
   * return a new IASMessage with "updated" metadata
   *
   * @param metadata
   * @return
   */
  public IASMessage updateMetadata(String metadata) {
    return new IASMessage(this.fromName, this.toAddress, this.text, //
        this.dateString, this.timeString, this.fileName, //
        this.fileTypeName, this.fileSourceName, this.messageId, metadata);
  }

  /**
   * return a new IASMessage with "updated" messageId
   *
   * @param messageId
   * @return
   */
  public IASMessage updateMessageId(String messageId) {
    return new IASMessage(this.fromName, this.toAddress, this.text, //
        this.dateString, this.timeString, this.fileName, //
        this.fileTypeName, this.fileSourceName, messageId, this.metadata);
  }

  @Override
  public String[] getHeaders() {
    return new String[] { "From", "To", "Text", "Date", "Time", "File", "Type", "Source", "MessageId", "Meta" };
  }

  @Override
  public String[] getValues() {
    return new String[] { fromName, toAddress, text, dateString, timeString, fileName, fileTypeName, fileSourceName,
        messageId, metadata };
  }

  @Override
  public int compareTo(IWritable other) {
    var o = (IASMessage) other;
    var cmp = dateString.compareTo(o.dateString);
    if (cmp != 0) {
      return cmp;
    }

    cmp = timeString.compareTo(o.timeString);
    if (cmp != 0) {
      return cmp;
    }

    cmp = fromName.compareTo(o.fromName);
    if (cmp != 0) {
      return cmp;
    }

    cmp = toAddress.compareTo(o.toAddress);
    if (cmp != 0) {
      return cmp;
    }

    return 0;
  }
}
