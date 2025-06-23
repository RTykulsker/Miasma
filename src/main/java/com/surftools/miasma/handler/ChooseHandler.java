/**

The MIT License (MIT)

Copyright (c) 2022,2025, Robert Tykulsker

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

package com.surftools.miasma.handler;

import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.config.ConfigurationKey;
import com.surftools.config.IConfigurationManager;
import com.surftools.miasma.utils.FileUtils;

import io.javalin.http.Context;

/**
 * one of the CRUD handlers, for Creating spots via GET requests.
 *
 * NOTE WELL: no authentication, no authorization!
 *
 * @author bobt
 *
 */
public class ChooseHandler extends AbstractBaseHandler {
  private static final Logger logger = LoggerFactory.getLogger(ChooseHandler.class);

  private String rawHtml;

  public ChooseHandler(IConfigurationManager cm) {
    super(cm, logger);
    rawHtml = FileUtils.readFile(Path.of(cm.getAsString(ConfigurationKey.TEMPLATE_ENTRY_FILE_NAME)), "entry");
  }

  @Override
  public void handle(Context ctx) throws Exception {
    super.handle(ctx);

    var pathInfoString = ctx.req.getPathInfo();
    var isEmail = pathInfoString.equals("/chooseEmail");

    var html = new String(rawHtml);
    if (isEmail) {
      html = html.replaceAll("<!-- TYPE-DEVICE -->", "an Email address");
      html = html.replaceAll("<!-- TYPE-LABEL -->", "Email address");
      html = html.replaceAll("<!-- TYPE-MAX-CHARS -->", "500");
      html = html.replaceAll("<!-- TYPE-IS-EMAIL -->", "true");
    } else {
      html = html.replaceAll("<!-- TYPE-DEVICE -->", "a cell phone");
      html = html.replaceAll("<!-- TYPE-LABEL -->", "10 digit cell phone number");
      html = html.replaceAll("<!-- TYPE-MAX-CHARS -->", "92");
      html = html.replaceAll("<!-- TYPE-IS-EMAIL -->", "false");
    }

    returnHtml(html);
  }

}
