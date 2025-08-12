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
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.config.MiasmaKey;
import com.surftools.config.PropertyFileConfigurationManager;
import com.surftools.miasma.web.MiasmaServer;
import com.surftools.utils.counter.Counter;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.util.StatusPrinter;

public class MiasmaBatchTool {
  static final Logger logger = LoggerFactory.getLogger(MiasmaServer.class);

  @Option(name = "--conf", usage = "name of configuration file", required = true)
  private String confFileName = null;

  private static String batchId;

  private int folderCount = 0;
  private int fileCount = 0;
  private int tabCount = 0;
  private Counter fromCounter = new Counter();
  private Counter toCounter = new Counter();
  private Counter textCounter = new Counter();
  private Counter statusCounter = new Counter();

  private String lastFrom = "";
  private String lastTo = "";
  private String lastText = "";
  private boolean autoDitto = false;

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
    // File rootFolder = new File("path/to/your/folder");
    // processExcelFilesInFolder(rootFolder);
    try {
      var cm = new PropertyFileConfigurationManager(confFileName, MiasmaKey.values());
      var inboxPathname = cm.getAsString(MiasmaKey.BATCH_INBOX_PATH);
      var inboxFolder = new File(inboxPathname);
      if (!inboxFolder.exists()) {
        throw new RuntimeException("Inbox folder doesn't exist: " + inboxPathname);
      }
      ++folderCount;

      logger.info("batchId: " + batchId);
      logger.info("conf file: " + confFileName);
      logger.info("inboxPathName: " + inboxPathname);

      processExcelFilesInFolder(inboxFolder);

      logger.info("folder count: " + folderCount);
      logger.info("file count: " + fileCount);
      logger.info("tab count: " + tabCount);

      logger.info("from count: " + fromCounter);
      logger.info("to count: " + toCounter);
      logger.info("textCount: " + textCounter);
      logger.info("statusCounter: " + statusCounter);

    } catch (Exception e) {
      logger.error("Exception running batchId: " + batchId + ", " + e.getMessage());
      e.printStackTrace();
    }
    logger.info("end run");
  }

  public void processExcelFilesInFolder(File folder) {
    if (folder == null || !folder.exists())
      return;

    var files = Arrays.asList(folder.listFiles());
    if (files == null)
      return;

    Collections.sort(files);
    for (File file : files) {
      if (file.isDirectory()) {
        processExcelFilesInFolder(file); // Recursive call
      } else if (file.getName().endsWith(".xlsx") || file.getName().endsWith(".xls")) {
        try {
          processExcelFile(file);
        } catch (IOException e) {
          logger.error("Failed to process file: " + file.getAbsolutePath());
          e.printStackTrace();
        }
      }
    }
  }

  private void processExcelFile(File file) throws IOException {
    logger.info("Processing: " + file.getAbsolutePath());

    try (var fis = new FileInputStream(file);
        var workbook = file.getName().endsWith(".xlsx") ? new XSSFWorkbook(fis) : new HSSFWorkbook(fis)) {

      ++fileCount;
      for (var sheet : workbook) {
        ++tabCount;
        logger.info("processing sheet: " + sheet.getSheetName());
        for (var row : sheet) {
          var inputRecords = parse(batchId, file, sheet, row);
          System.out
              .println("parsed inputRecords: "
                  + inputRecords.stream().map(Object::toString).collect(Collectors.joining("\n")));
        }
      }
    }
  }

  private List<InputRecord> parse(String batchId, File file, Sheet sheet, Row row) {
    var list = new ArrayList<InputRecord>();
    if (row == null) {
      logger.error("null row! BatchId: " + batchId + ", file: " + file.getPath() + ", tab: " + sheet.getSheetName());
      return list;
    }

    var fromValue = getStringValue(row, 0);
    var toValue = getStringValue(row, 1);
    var textValue = getStringValue(row, 2);

    if (autoDitto) {
      fromValue = fromValue.isEmpty() ? lastFrom : fromValue;
      toValue = toValue.isEmpty() ? lastTo : toValue;
      textValue = textValue.isEmpty() ? lastText : textValue;
    }

    var inputStatus = InputStatus.OK;
    if (fromValue.isEmpty() && toValue.isEmpty() && textValue.isEmpty()) {
      inputStatus = InputStatus.NO_FROM_TO_AND_TEXT_FIELDS;
    } else if (fromValue.isEmpty() && toValue.isEmpty() && !textValue.isEmpty()) {
      inputStatus = InputStatus.NO_FROM_AND_TO_FIELDS;
    } else if (fromValue.isEmpty() && !toValue.isEmpty() && textValue.isEmpty()) {
      inputStatus = InputStatus.NO_FROM_AND_TEXT_FIELDS;
    } else if (fromValue.isEmpty() && !toValue.isEmpty() && !textValue.isEmpty()) {
      inputStatus = InputStatus.NO_FROM_FIELD;
    } else if (!fromValue.isEmpty() && toValue.isEmpty() && textValue.isEmpty()) {
      inputStatus = InputStatus.NO_TO_AND_TEXT_FIELDS;
    } else if (!fromValue.isEmpty() && toValue.isEmpty() && !textValue.isEmpty()) {
      inputStatus = InputStatus.NO_TO_FIELD;
    } else if (!fromValue.isEmpty() && !toValue.isEmpty() && textValue.isEmpty()) {
      inputStatus = InputStatus.NO_TEXT_FIELD;
    }

    if (inputStatus == InputStatus.OK) {
      if (fromValue.toLowerCase().equals("from") && toValue.toLowerCase().equals("to")) {
        inputStatus = InputStatus.HEADER;
      }
    }

    if (inputStatus == InputStatus.OK) {
      var tos = toValue.split(";|,");
      for (var address : tos) {
        address = address.trim();

        var isEmail = address.contains("@");
        if (!isEmail) {
          var digits = new StringBuilder();
          for (var i = 0; i < address.length(); ++i) {
            var c = address.charAt(i);
            if (Character.isDigit(c)) {
              digits.append(c);
            }
          } // end loop over characters in address
          address = digits.toString();
          if (address.length() == 10) {
            inputStatus = InputStatus.OK_SMS;
          } else {
            inputStatus = InputStatus.CANT_PARSE_TO_FIELDS;
          }
        } else { // not email
          inputStatus = InputStatus.OK_EMAIL;
        }
        var inputRecord = new InputRecord(batchId, file.getPath(), sheet.getSheetName(),
            String.valueOf(row.getRowNum()), inputStatus.name(), //
            fromValue, address, textValue);
        list.add(inputRecord);

        fromCounter.increment(fromValue);
        toCounter.increment(address);
        textCounter.increment(textValue);
        statusCounter.increment(inputStatus.name());
      } // end loop over addresses in to field
    } else { // end if OK
      fromCounter.increment(fromValue);
      toCounter.increment(toValue);
      textCounter.increment(textValue);
      statusCounter.increment(inputStatus.name());
    } // end if !OK

    lastFrom = fromValue;
    lastTo = toValue;
    lastText = textValue;

    return list;

  }

  public String getStringValue(Row row, int columnIndex) {
    Cell cell = row.getCell(columnIndex);
    if (cell == null) {
      return "";
    }

    switch (cell.getCellType()) {
    case BLANK:
      return "";

    case BOOLEAN:
      return Boolean.toString(cell.getBooleanCellValue());

    case ERROR:

      return "";

    case FORMULA: {
      CellType cachedCellType = cell.getCachedFormulaResultType();
      if (cachedCellType == CellType.STRING) {
        return cell.getStringCellValue();
      } else if (cachedCellType == CellType.NUMERIC) {
        return Double.toString(cell.getNumericCellValue());
      } else if (cachedCellType == CellType.BOOLEAN) {
        return Boolean.toString(cell.getBooleanCellValue());
      }
    }

    case NUMERIC:
      return Double.toString(cell.getNumericCellValue());

    case STRING:
      return cell.getStringCellValue();

    default:
      logger
          .error("Unsupported type: " + cell.getCellType().name() + " on row: " + row.getRowNum() + ", col: "
              + columnIndex);
      return "";
    }
  }
}
