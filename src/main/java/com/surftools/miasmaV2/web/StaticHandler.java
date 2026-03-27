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

package com.surftools.miasmaV2.web;

import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jetty.http.HttpStatus;

import com.surftools.miasmaV2.config.IConfigurationManager;
import com.surftools.miasmaV2.config.MiasmaKey;

import io.javalin.http.Context;
import io.javalin.http.Handler;

/**
 * for static html pages
 */
public class StaticHandler implements Handler {
  protected final String html;

  public StaticHandler(IConfigurationManager cm, MiasmaKey miasmaKey) throws Exception {
    html = Files.readString(Path.of(cm.getAsString(miasmaKey)));
  }

  @Override
  public void handle(Context ctx) throws Exception {
    ctx.html(html);
    ctx.status(HttpStatus.OK_200);
  } // end handle()

}
