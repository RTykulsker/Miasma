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

package com.surftools.config;

public enum ConfigurationKey {

  APP_WINLINK_REPLACEMENT_MESSAGE_ADDRESS("app.winlink.replacementMessageAddress"), //
  APP_WRITER_CSV_PATH("app.writer.csv.path"), //
  APP_WRITER_PAT_PATH("app.writer.pat.path"), //
  APP_WRITER_WINLINK_EXPRESS_PATH("app.writer.winlinkExpress.path"), //
  APP_WRITER_WINLINK_EXPRESS_SENDER("app.writer.winlinkExpress.sender"), //

  SERVER_PORT("server.port", "5000"), //

  TEMPLATE_CACHE_FILES("template.cache.files", "true"), //
  TEMPLATE_INDEX_FILE_NAME("template.index.fileName", "conf/index.html"), //
  TEMPLATE_ENTRY_FILE_NAME("template.entry.fileName", "conf/entry.html"), //
  TEMPLATE_THANKS_FILE_NAME("template.thanks.fileName", "conf/thanks.html"), //
  ;

  private final String key;
  private final String defaultValue;

  private ConfigurationKey(String key) {
    this.key = key;
    this.defaultValue = null;
  }

  private ConfigurationKey(String key, String defaultValue) {
    this.key = key;
    this.defaultValue = defaultValue;
  }

  public static ConfigurationKey fromString(String string) {
    for (ConfigurationKey key : ConfigurationKey.values()) {
      if (key.toString().equals(string)) {
        return key;
      }
    }
    return null;
  }

  public String defaultValue() {
    return defaultValue;
  }

  @Override
  public String toString() {
    return key;
  }
}
