/**

The MIT License (MIT)

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

package com.surftools.miasma.web;

import java.net.NetworkInterface;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.config.MiasmaKey;
import com.surftools.config.PropertyFileConfigurationManager;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.util.StatusPrinter;
import io.javalin.Javalin;

// TODO -- documentation pass -- every class; every public method

// http://localhost:7000/create?frequency=3598&mode=V500&call=KM6SO&grid=CN87Vm&createdBy=KM6SO

/**
 *
 * @author bobt
 *
 */
public class MiasmaServer {
  static final Logger logger = LoggerFactory.getLogger(MiasmaServer.class);

  @Option(name = "--conf", usage = "name of configuration file", required = true)
  private static String confFileName = null;

  public static final int PORT = 5000;

  public static void main(String[] args) {

    var printLoggerContext = false;
    if (printLoggerContext) {
      LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
      StatusPrinter.print(lc);
    }

    MiasmaServer tool = new MiasmaServer();
    CmdLineParser parser = new CmdLineParser(tool);
    try {
      parser.parseArgument(args);
      tool.run();
    } catch (Exception e) {
      System.err.println(e.getMessage());
      e.printStackTrace();
      parser.printUsage(System.err);
    }
  }

  public void run() throws Exception {
    logger.info("setting up server");

    var cm = new PropertyFileConfigurationManager(confFileName, MiasmaKey.values());
    var app = Javalin.create();

    var indexHandler = new IndexHandler(cm);
    app.get("/", indexHandler);
    app.get("/index", indexHandler);

    var chooseHandler = new ChooseHandler(cm);
    app.get("/chooseEmail", chooseHandler);
    app.get("/chooseSMS", chooseHandler);

    var entryHandler = new EntryHandler(cm);
    app.get("/entry", entryHandler);
    app.post("/entry", entryHandler);

    var port = cm.getAsInt(MiasmaKey.SERVER_PORT);
    app.start(port);

    var sb = new StringBuilder();
    var interfaces = NetworkInterface.getNetworkInterfaces();
    while (interfaces.hasMoreElements()) {
      var ni = interfaces.nextElement();
      var inetAddresses = ni.getInetAddresses();
      while (inetAddresses.hasMoreElements()) {
        var inetAddress = inetAddresses.nextElement();
        sb.append("interface: " + ni.getDisplayName() + ", ipAddress: " + inetAddress.getHostAddress() + "\n");
      }
    }

    logger.info("listening on various interfaces and addresses:\n" + sb.toString());
    logger.info("started on port: " + port);
  }

}
