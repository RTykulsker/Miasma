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

package com.surftools.miasma.tool;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.config.ConfigurationKey;
import com.surftools.config.PropertyFileConfigurationManager;
import com.surftools.miasma.handler.ChooseHandler;
import com.surftools.miasma.handler.EntryHandler;
import com.surftools.miasma.handler.IndexHandler;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.util.StatusPrinter;
import io.javalin.Javalin;

// TODO -- documentation pass -- every class; every public method
// TODO -- figure out why my Firefox reports timezoneOffset of 0!

// http://localhost:7000/create?frequency=3598&mode=V500&call=KM6SO&grid=CN87Vm&createdBy=KM6SO

/**
 *
 * @author bobt
 *
 */
public class MiasmaServer {
  static final Logger logger = LoggerFactory.getLogger(MiasmaServer.class);

  @Option(name = "--conf", usage = "name of configuration file", required = false)
  private static String confFileName = "conf/conf.txt";

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
      parser.printUsage(System.err);
    }
  }

  public void run() throws Exception {
    logger.info("setting up server");

    var cm = new PropertyFileConfigurationManager(confFileName, ConfigurationKey.values());
    var chooseHandler = new ChooseHandler(cm);
    var app = Javalin.create();
    app.get("/", new IndexHandler(cm));
    app.get("/index", new IndexHandler(cm));
    app.get("/chooseEmail", chooseHandler);
    app.get("/chooseSMS", chooseHandler);
    app.get("/entry", new EntryHandler(cm));

    var port = cm.getAsInt(ConfigurationKey.SERVER_PORT);
    app.start(port);
    logger.info("started on port: " + port);
  }

}
