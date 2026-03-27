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
import org.slf4j.Logger;

import com.surftools.miasmaV2.config.IConfigurationManager;
import com.surftools.miasmaV2.config.MiasmaKey;

import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HttpCode;

public abstract class AbstractHandler implements Handler {
  protected final Logger logger;
  protected final IConfigurationManager cm;

  protected String method;
  protected String resource;
  protected Context ctx;

  protected MiasmaKey templateFileNameKey;
  protected String rawHtml;

  public AbstractHandler(IConfigurationManager cm, Logger logger, MiasmaKey templateFileNameKey) throws Exception {
    this.cm = cm;
    this.logger = logger;

    this.templateFileNameKey = templateFileNameKey;
    if (cm.getAsBoolean(MiasmaKey.TEMPLATE_CACHE_FILES)) {
      rawHtml = Files.readString(Path.of(cm.getAsString(templateFileNameKey)));
    }
  }

  protected String getTemplateHtml() throws Exception {
    if (rawHtml != null) {
      return new String(rawHtml);
    }
    return Files.readString(Path.of(cm.getAsString(templateFileNameKey)));
  }

  @Override
  public void handle(Context ctx) throws Exception {

    this.ctx = ctx;
    method = ctx.method();
    resource = ctx.url().substring(ctx.url().lastIndexOf("/"));
    var paramMap = (method.equals("GET")) ? ctx.queryParamMap() : ctx.formParamMap();
    var sb = new StringBuilder();
    for (String key : paramMap.keySet()) {
      var list = paramMap.get(key);
      var listAsString = String.join(",", list);
      sb.append(key + " => [" + listAsString + "]\n");
    }
    logger.debug(method + " Params: \n" + sb.toString());
  }

  protected String getParam(String key) {
    var s = (method.equals("GET")) ? ctx.queryParam(key) : ctx.formParam(key);
    s = ((s != null) ? s.strip() : "");
    return s;
  }

  public void returnResult(HttpCode httpCode, String response, String userName) {
    ctx.result(response);
    ctx.status(httpCode);
  }

  public void returnResult(String response) {
    returnResult(HttpCode.OK, response, null);
  }

  public void returnResult(String response, String userName) {
    returnResult(HttpCode.OK, response, userName);
  }

  public void returnResult(HttpCode httpCode, String response) {
    returnResult(httpCode, response, null);
  }

  public void returnHtml(String response) {
    ctx.html(response);
    ctx.status(HttpStatus.OK_200);
  }

  protected void returnBytes(byte[] bytes, String name) {
    ctx.contentType(ContentType.APPLICATION_OCTET_STREAM);
    ctx.header("Content-disposition", "attachment; filename=" + name);
    ctx.result(bytes);
  }

}
