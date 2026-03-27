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

import java.io.File;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.miasmaV2.config.IConfigurationManager;
import com.surftools.miasmaV2.config.MiasmaKey;
import com.surftools.miasmaV2.io.IoUtils;

import io.javalin.core.util.FileUtil;
import io.javalin.http.Context;

/**
 *
 * @author bobt
 *
 */
public class UploadHandler extends AbstractHandler {
  private static final Logger logger = LoggerFactory.getLogger(UploadHandler.class);
  private Path inboxPath;
  private String inboxPathString;

  public UploadHandler(IConfigurationManager cm) throws Exception {
    super(cm, logger, MiasmaKey.TEMPLATE_THANKS_UPLOAD_FILE_NAME);

    inboxPath = Path.of(cm.getAsString(MiasmaKey.ROOT_PATH), "files", "inbox");
    inboxPathString = inboxPath.toString() + File.separator;
  }

  @Override
  public void handle(Context ctx) throws Exception {
    super.handle(ctx);

    var fileNames = ctx.uploadedFiles().stream().map(t -> t.getFilename()).toList();
    logger.info("Received " + fileNames.size() + " uploaded file(s): " + String.join(",", fileNames));

    ctx
        .uploadedFiles()
          .forEach(t -> FileUtil
              .streamToFile(t.getContent(), inboxPathString + IoUtils.getMilliStamp() + "-upload-" + t.getFilename()));

    var html = getTemplateHtml();
    var filesString = "<li>" + String.join("<li>", fileNames);
    html = html.replaceAll("<!-- FILES -->", filesString);
    returnHtml(html);
  }

}
