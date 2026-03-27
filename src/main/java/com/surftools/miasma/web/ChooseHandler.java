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

package com.surftools.miasma.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.miasma.config.IConfigurationManager;
import com.surftools.miasma.config.MiasmaKey;

import io.javalin.http.Context;

/**
 * one of the CRUD handlers, for Creating spots via GET requests.
 *
 * NOTE WELL: no authentication, no authorization!
 *
 * @author bobt
 *
 */
public class ChooseHandler extends AbstractHandler {
  private static final Logger logger = LoggerFactory.getLogger(ChooseHandler.class);

  public ChooseHandler(IConfigurationManager cm) throws Exception {
    super(cm, logger, MiasmaKey.TEMPLATE_ENTRY_FILE_NAME);
  }

  @Override
  public void handle(Context ctx) throws Exception {
    super.handle(ctx);

    var pathInfoString = ctx.req.getPathInfo();
    var hiddenEmail = getParam("isEmail");
    if (hiddenEmail != null) {
      logger.info("### hidden: " + hiddenEmail);
    }
    var isEmail = pathInfoString.equals("/chooseEmail");

    var html = getTemplateHtml();
    if (isEmail) {
      html = html.replaceAll("<!-- TYPE-DEVICE -->", "an Email address");
      html = html.replaceAll("<!-- TYPE-LABEL -->", "Email address");
      html = html.replaceAll("<!-- TYPE-INPUT -->", "email");
      html = html.replaceAll("JS-TYPE-IS-EMAIL", "true");
      html = html.replaceAll("JS-TYPE-MAX-CHARS", "500");
      html = html.replaceAll("JS-TYPE-NAME", "Email Address");
    } else {
      html = html.replaceAll("<!-- TYPE-DEVICE -->", "a cell phone");
      html = html.replaceAll("<!-- TYPE-LABEL -->", "Cell phone number (10 digits)");
      html = html.replaceAll("<!-- TYPE-INPUT -->", "tel");
      html = html.replaceAll("JS-TYPE-IS-EMAIL", "false");
      html = html.replaceAll("JS-TYPE-MAX-CHARS", "92");
      html = html.replaceAll("JS-TYPE-NAME", "Cell Phone number");
    }

    if (html.contains("JS-TYPE")) {
      System.err.println(html);
    }
    returnHtml(html);
  }

}
