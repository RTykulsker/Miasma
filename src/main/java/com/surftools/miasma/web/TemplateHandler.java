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

package com.surftools.miasma.web;

import java.nio.file.Files;
import java.nio.file.Path;

import com.surftools.miasma.config.IConfigurationManager;
import com.surftools.miasma.config.MiasmaKey;

import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.Handler;

public class TemplateHandler implements Handler {
  private byte[] templateBytes;
  private String fileName;

  public TemplateHandler(IConfigurationManager cm) throws Exception {
    fileName = Path.of(cm.getAsString(MiasmaKey.TEMPLATE_TEMPLATE_FILE_NAME)).getFileName().toString();
    templateBytes = Files.readAllBytes(Path.of(cm.getAsString(MiasmaKey.TEMPLATE_TEMPLATE_FILE_NAME)));
  }

  @Override
  public void handle(Context ctx) throws Exception {
    ctx.contentType(ContentType.APPLICATION_OCTET_STREAM);
    ctx.header("Content-disposition", "attachment; filename=" + fileName);
    ctx.result(templateBytes);
  }

}
