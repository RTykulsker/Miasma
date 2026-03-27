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

package com.surftools.miasmaV2.config;

public enum MiasmaKey {
  ROOT_PATH("root.path"), // where all other paths are homed from

  APP_SMS_REPLACEMENT_EMAIL_ADDRESS("app.sms.replacementEmailAddress"), //
  APP_PAT_PATH("app.pat.path"), // might be outside of root, ends with mailbox, before call
  APP_WINLINK_EXPRESS_SENDER("app.winlinkExpress.sender"), //

  BATCH_AUTO_DITTO_ENABLED("batch.auto.ditto.enabled"), //
  BATCH_EXCEL_IGNORE_SHEET_LIST("batch.excel.ignore.sheet.list"), //

  SERVER_PORT("server.port", "5000"), //

  BODY_MAX_LENGTH_TRUNCATE_ENABLED("body.maxLength.truncate.enabled"), // if body is too long
  BODY_MAX_LENGTH_EMAIL("body.maxLength.email"), // maxLength for email message body
  BODY_MAX_LENGTH_SMS("body.maxLength.sms"), // maxLength for sms message body

  TEMPLATE_CACHE_FILES("template.cache.files", "true"), //
  TEMPLATE_INDEX_FILE_NAME("template.index.fileName", "conf/index.html"), //
  TEMPLATE_ENTRY_FILE_NAME("template.entry.fileName", "conf/entry.html"), //
  TEMPLATE_THANKS_FILE_NAME("template.thanks.fileName", "conf/thanks.html"), //
  TEMPLATE_INDEX_UPLOAD_FILE_NAME("template.indexUpload.fileName", "conf/indexUpload.html"), //
  TEMPLATE_THANKS_UPLOAD_FILE_NAME("template.thanksUpload.fileName", "conf/thanksUpload.html"), //
  TEMPLATE_ABOUT_FILE_NAME("template.about.fileName", "conf/about.html"), //
  TEMPLATE_ACKNOWLEDGEMENTS_FILE_NAME("template.acknowledgements.fileName", "conf/acknowledgements.html") //
  ;

  private final String key;
  private final String defaultValue;

  private MiasmaKey(String key) {
    this.key = key;
    this.defaultValue = null;
  }

  private MiasmaKey(String key, String defaultValue) {
    this.key = key;
    this.defaultValue = defaultValue;
  }

  public static MiasmaKey fromString(String string) {
    for (MiasmaKey key : MiasmaKey.values()) {
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
