/**

Lorem ipsum dolor sit amet consectetur adipiscing elit quisque faucibus ex sapien vitae pellentesque sem placerat in id cursus mi pretium tellus duis convallis tempus leo eu aenean sed diam urna tempor pulvinar vivamus fringilla lacus nec metus bibendum egestas iaculis massa nisl malesuada lacinia integer nunc posuere ut hendrerit semper vel class aptent taciti sociosqu ad litora torquent per conubia nostra inceptos himenaeos orci varius natoque penatibus et magnis dis parturient montes nascetur ridiculus mus donec rhoncus eros lobortis nulla molestie mattis scelerisque maximus eget fermentum odio phasellus non purus est efficitur laoreet mauris pharetra vestibulum fusce dictum risus.The MIT License (MIT)

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

package com.surftools.miasmaV2.web;

import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.miasmaV2.config.IConfigurationManager;
import com.surftools.miasmaV2.config.MiasmaKey;
import com.surftools.miasmaV2.io.IoUtils;

import io.javalin.http.Context;

/**
 *
 * @author bobt
 *
 */
public class EntryHandler extends AbstractHandler {
  private static final Logger logger = LoggerFactory.getLogger(EntryHandler.class);
  private Path inboxPath;

  public EntryHandler(IConfigurationManager cm) throws Exception {
    super(cm, logger, MiasmaKey.TEMPLATE_THANKS_FILE_NAME);

    inboxPath = Path.of(cm.getAsString(MiasmaKey.ROOT_PATH), "files", "inbox");
  }

  @Override
  public void handle(Context ctx) throws Exception {
    super.handle(ctx);

    var fromName = getParam("name");
    var toAddresses = getParam("address");
    var text = getParam("message");

    logger.info("got request name: " + fromName + ", address: " + toAddresses + ", message: " + text);

    var sb = new StringBuilder();
    sb.append(fromName + "\n");
    sb.append(toAddresses + "\n");
    sb.append(text + "\n");
    var path = Path.of(inboxPath.toString(), IoUtils.getMilliStamp() + ".web");
    Files.writeString(path, sb);

    var html = getTemplateHtml();
    html = html.replaceAll("<!-- FROM -->", fromName);
    html = html.replaceAll("<!-- TO -->", toAddresses);
    html = html.replaceAll("<!-- MESSAGE -->", text);
    returnHtml(html);
  }

}
