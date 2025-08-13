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

package com.surftools.miasma.batch;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.config.IConfigurationManager;
import com.surftools.config.MiasmaKey;
import com.surftools.config.PropertyFileConfigurationManager;
import com.surftools.miasma.web.MiasmaServer;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.util.StatusPrinter;

public class MiasmaBatchTool {
  static final Logger logger = LoggerFactory.getLogger(MiasmaServer.class);

  @Option(name = "--conf", usage = "name of configuration file", required = true)
  private String confFileName = null;

  private static String batchId;
  private static IConfigurationManager cm;

  public static void main(String[] args) {

    var printLoggerContext = false;
    if (printLoggerContext) {
      LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
      StatusPrinter.print(lc);
    }

    batchId = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"));

    MiasmaBatchTool tool = new MiasmaBatchTool();
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

  public void run() {
    logger.info("begin run");
    try {
      cm = new PropertyFileConfigurationManager(confFileName, MiasmaKey.values());
      var inboxPathname = cm.getAsString(MiasmaKey.BATCH_INBOX_PATH);
      var inboxFolder = new File(inboxPathname);
      if (!inboxFolder.exists()) {
        throw new RuntimeException("Inbox folder doesn't exist: " + inboxPathname);
      }

      logger.info("batchId: " + batchId);
      logger.info("conf file: " + confFileName);
      logger.info("inboxPathName: " + inboxPathname);

      var counterContext = processFilesInFolder(inboxFolder);
      ++counterContext.folderCount;
      logger.info(counterContext.toString());
    } catch (Exception e) {
      logger.error("Exception running batchId: " + batchId + ", " + e.getMessage());
      e.printStackTrace();
    }
    logger.info("end run");
  }

  public CounterContext processFilesInFolder(File folder) {
    if (folder == null) {
      logger.warn("null folder");
      return new CounterContext(batchId);
    }

    if (!folder.exists()) {
      logger.warn("folder: " + folder.getName() + " doesn't exist!");
      return new CounterContext(batchId);
    }

    var files = Arrays.asList(folder.listFiles());
    if (files == null || files.size() == 0) {
      logger.warn("folder: " + folder.getName() + ", no files!");
      return new CounterContext(batchId);
    }

    logger.info("processing folder: " + folder.getName());
    Collections.sort(files);
    var folderCounterContext = new CounterContext(batchId);
    for (File file : files) {
      if (file.isDirectory()) {
        var subFolderCounterContext = processFilesInFolder(file); // Recursive call
        folderCounterContext.merge(subFolderCounterContext);
      } else if (file.getName().toLowerCase().endsWith(".xlsx") || file.getName().toLowerCase().endsWith(".xls")) {
        var excelProcessor = new ExcelBatchProcessor(batchId, file, cm);
        var results = excelProcessor.process();
        if (results != null && results.counterContext() != null) {
          folderCounterContext.merge(results.counterContext());
        }
      } else if (file.getName().toLowerCase().endsWith(".csv")) {
        var csvProcessor = new CsvBatchProcessor(batchId, file, cm);
        var results = csvProcessor.process();
        if (results != null && results.counterContext() != null) {
          folderCounterContext.merge(results.counterContext());
        }
      } else {
        logger.warn("file: " + file.getName() + " unsupported and ignored");
      }
    }

    return folderCounterContext;
  }

}
