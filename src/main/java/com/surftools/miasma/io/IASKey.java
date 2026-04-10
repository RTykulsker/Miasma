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

package com.surftools.miasma.io;

import java.util.Objects;

/**
 * an I Am Safe key, for deduplication
 */
public class IASKey {
  public final String fromName; // name of person sending message
  public final String toAddress; // address of recipient(s)
  public final String text; // the message the person is trying to send

  public IASKey(String fromName, String toAddress, String text) {
    this.fromName = fromName.strip().toLowerCase();
    this.toAddress = toAddress.strip().toLowerCase();
    this.text = text.strip().toLowerCase();
  }

  @Override
  public String toString() {
    return "{fromName: " + fromName + ", toAddress: " + toAddress + ", text: " + text + "}";
  }

  public IASKey(IASMessage m) {
    this.fromName = m.fromName().strip().toLowerCase();
    this.toAddress = m.toAddress().strip().toLowerCase();
    this.text = m.text().strip().toLowerCase();
  }

  @Override
  public int hashCode() {
    return Objects.hash(fromName, text, toAddress);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    IASKey other = (IASKey) obj;
    return Objects.equals(fromName, other.fromName) && Objects.equals(text, other.text)
        && Objects.equals(toAddress, other.toAddress);
  }

}
