/**

The MIT License (MIT)

Copyright (c) 2025-2026, Robert Tykulsker

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

import java.net.NetworkInterface;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.miasma.config.IConfigurationManager;
import com.surftools.miasma.config.MiasmaKey;

import io.javalin.Javalin;

/**
 * Web Server to handle both the BYOD (Bring Your Own Device) as well as file uploads
 */
public class WebServer {
  static final Logger logger = LoggerFactory.getLogger(WebServer.class);

  public static final int DEFAULT_SERVER_PORT = 5000;

  public WebServer(IConfigurationManager cm) throws Exception {
    logger.info("setting up server");

    var app = Javalin.create();

    var indexHandler = new StaticHandler(cm, MiasmaKey.TEMPLATE_INDEX_FILE_NAME);
    app.get("/", indexHandler);
    app.get("/index", indexHandler);

    var chooseHandler = new ChooseHandler(cm);
    app.get("/chooseEmail", chooseHandler);
    app.get("/chooseSMS", chooseHandler);

    var entryHandler = new EntryHandler(cm);
    app.get("/entry", entryHandler);
    app.post("/entry", entryHandler);

    app.get("/upload", new StaticHandler(cm, MiasmaKey.TEMPLATE_INDEX_UPLOAD_FILE_NAME));
    app.post("/uploadResponse", new UploadHandler(cm));

    app.get("/about", new StaticHandler(cm, MiasmaKey.TEMPLATE_ABOUT_FILE_NAME));
    app.get("/acknowledgements", new StaticHandler(cm, MiasmaKey.TEMPLATE_ACKNOWLEDGEMENTS_FILE_NAME));

    app.get("/download", new StaticHandler(cm, MiasmaKey.TEMPLATE_DOWNLOAD_FILE_NAME));
    app.get("/template", new TemplateHandler(cm));

    var port = cm.getAsInt(MiasmaKey.SERVER_PORT, DEFAULT_SERVER_PORT);
    app.start(port);

    var sb = new StringBuilder();
    var interfaces = NetworkInterface.getNetworkInterfaces();
    while (interfaces.hasMoreElements()) {
      var ni = interfaces.nextElement();
      var inetAddresses = ni.getInetAddresses();
      while (inetAddresses.hasMoreElements()) {
        var inetAddress = inetAddresses.nextElement();
        sb.append("\tinterface: " + ni.getDisplayName() + ", ipAddress: " + inetAddress.getHostAddress() + "\n");
      }
    }
    var s = sb.toString();
    s = s.substring(0, s.length() - 1);

    logger.info("listening on various interfaces and addresses:\n" + s);
    logger.info("started on port: " + port);
  }

}
