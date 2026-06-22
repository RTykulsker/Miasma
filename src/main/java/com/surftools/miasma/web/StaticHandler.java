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

package com.surftools.miasma.web;

import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jetty.http.HttpStatus;

import com.surftools.miasma.config.IConfigurationManager;
import com.surftools.miasma.config.MiasmaKey;

import io.javalin.http.Context;
import io.javalin.http.Handler;

/**
 * for static html pages
 */
public class StaticHandler implements Handler {
  protected String content;
  protected final String filename;

  public StaticHandler(IConfigurationManager cm, MiasmaKey miasmaKey) throws Exception {
    filename = cm.getAsString(miasmaKey);

    if (cm.getAsBoolean(MiasmaKey.TEMPLATE_CACHE_FILES, true)) {
      content = Files.readString(Path.of(filename));
    }

  }

  @Override
  public void handle(Context ctx) throws Exception {
    if (content == null) {
      content = Files.readString(Path.of(filename));
    }

    if (filename.endsWith(".css")) {
      ctx.contentType("text/css");
      ctx.result(content);
    } else if (filename.endsWith(".js")) {
      ctx.contentType("application/javascript");
      ctx.result(content);
    } else {
      ctx.html(content);
    }

    ctx.status(HttpStatus.OK_200);
  } // end handle()

}
