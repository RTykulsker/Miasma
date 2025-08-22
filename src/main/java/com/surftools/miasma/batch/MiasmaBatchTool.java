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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.config.IConfigurationManager;
import com.surftools.config.MiasmaKey;
import com.surftools.config.PropertyFileConfigurationManager;
import com.surftools.miasma.batchMessageService.BatchMessageWriter;
import com.surftools.miasma.web.MiasmaServer;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.util.StatusPrinter;

/**
 * main class to read a bunch of spreadsheet files (Excel) "I am safe" messages, and produce output(s) that can be used
 * by third-party Winlink clients (Winlink Express, Pat)
 *
 * "I am safe" messages are SHORT, simple, one-way messages to let the "friends/family" of the sender know that they
 * have survived (so far) a disaster
 *
 * The spreadsheet files should contains three columns:
 *
 * 1) the Name of the sender: for example, Bob
 *
 * 2) the Addresses of the recipients, 10 digit SMS address, SMTP email address, or Winlink address. Multiple addressess
 * are permitted (semi-colon or comma delimited)
 *
 * 3) the Text of the message. Since the message might go out via SMS, I check for message length
 *
 */
public class MiasmaBatchTool {
  static final Logger logger = LoggerFactory.getLogger(MiasmaServer.class);

  @Option(name = "--conf", usage = "name of configuration file", required = true)
  private String confFileName = null;

  private static String batchId;
  private static IConfigurationManager cm;
  private static FileManager fm;

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
      logger.info("batchId: " + batchId);
      logger.info("conf file: " + confFileName);

      var inboxPathname = cm.getAsString(MiasmaKey.BATCH_INBOX_PATH);
      var inboxFolder = new File(inboxPathname);
      if (!inboxFolder.exists()) {
        throw new RuntimeException("Inbox folder doesn't exist: " + inboxPathname);
      }
      logger.info("inboxPathName: " + inboxPathname);

      fm = new FileManager(batchId, cm);
      fm.copyConfig(confFileName);

      var processResult = processFilesInFolder(inboxFolder);
      ++processResult.counterContext().folderCount;

      var messageWriter = new BatchMessageWriter(cm);
      messageWriter.write(processResult);

      fm.cleanInbox();
    } catch (Exception e) {
      logger.error("Exception running batchId: " + batchId + ", " + e.getMessage());
      e.printStackTrace();
    }
    logger.info("end run");
  }

  public ProcessResult processFilesInFolder(File folder) {
    if (folder == null) {
      logger.warn("null folder");
      return ProcessResult.EMPTY;
    }

    if (!folder.exists()) {
      logger.warn("folder: " + folder.getName() + " doesn't exist!");
      return ProcessResult.EMPTY;
    }

    var files = Arrays.asList(folder.listFiles());
    if (files == null || files.size() == 0) {
      logger.warn("folder: " + folder.getName() + ", no files!");
      return ProcessResult.EMPTY;
    }

    logger.info("processing folder: " + folder.getName());
    Collections.sort(files);
    var folderProcessResult = new ProcessResult(new ArrayList<SpreadsheetRecord>(), new ArrayList<SpreadsheetRecord>(),
        new CounterContext(batchId));
    for (File file : files) {
      if (file.isDirectory()) {
        var subFolderProcessResult = processFilesInFolder(file); // Recursive call
        folderProcessResult.merge(subFolderProcessResult);
      } else {
        if (file.getName().toLowerCase().endsWith(".xlsx") || file.getName().toLowerCase().endsWith(".xls")) {
          var excelProcessor = new ExcelBatchProcessor(batchId, file, cm);
          var results = excelProcessor.process();
          if (results != null && results.counterContext() != null) {
            folderProcessResult.merge(results);
            fm.copySpreadsheetFile(file);
          }
        } else if (file.getName().toLowerCase().endsWith(".csv")) {
          var csvProcessor = new CsvBatchProcessor(batchId, file, cm);
          var results = csvProcessor.process();
          if (results != null && results.counterContext() != null) {
            folderProcessResult.merge(results);
            fm.copySpreadsheetFile(file);
          }
        } else {
          logger.warn("file: " + file.getName() + " unsupported and ignored");
        }
      } // end if file
    } // end loop over files
    return folderProcessResult;
  }
}
