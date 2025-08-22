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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.config.IConfigurationManager;
import com.surftools.config.MiasmaKey;

public class ExcelBatchProcessor extends BaseBatchProcessor {
  private static final Logger logger = LoggerFactory.getLogger(ExcelBatchProcessor.class);
  private Set<String> ignoreSheetSet = new LinkedHashSet<>();

  public ExcelBatchProcessor(String batchId, File file, IConfigurationManager cm) {
    super(batchId, file, cm);

    var ignoreSheetString = cm.getAsString(MiasmaKey.BATCH_EXCEL_IGNORE_SHEET_LIST, "");
    if (!ignoreSheetString.isEmpty()) {
      var fields = ignoreSheetString.trim().split(",");
      for (var field : fields) {
        ignoreSheetSet.add(field.trim());
      }
      logger.warn("Will ignore the follow sheet names: " + String.join(", ", ignoreSheetSet));
    }
  }

  /**
   * process a complete Excel spreadsheet for "I am safe" messages
   *
   * loop over all tabs/sheets in the file
   *
   * loop over all rows in the tab and create a new SpreadsheetRecord
   *
   * hand the SpreadsheetRecord to our parent to be "parsed"
   *
   * "accumulate" each row's parseResult via a "merge" operation
   *
   * this file's ProcessResult will be "merged" with all other files ProcessResult
   *
   * @return
   */
  public ProcessResult process() {
    logger.info("Processing: " + file.getAbsolutePath());

    var processResult = new ProcessResult(new ArrayList<SpreadsheetRecord>(), new ArrayList<SpreadsheetRecord>(),
        new CounterContext(batchId));
    try (var fis = new FileInputStream(file);
        var workbook = file.getName().endsWith(".xlsx") ? new XSSFWorkbook(fis) : new HSSFWorkbook(fis)) {

      ++processResult.counterContext().fileCount;
      for (var sheet : workbook) {
        ++processResult.counterContext().tabCount;
        var sheetName = sheet.getSheetName();
        if (ignoreSheetSet.contains(sheetName)) {
          logger.warn("Ignoring file/sheet: " + file.getName() + "/" + sheetName);
          continue;
        }
        logger.debug("processing sheet: " + sheet.getSheetName());
        for (var row : sheet) {
          var spreadsheetRecord = new SpreadsheetRecord(batchId, file.getPath(), sheet.getSheetName(), //
              String.valueOf(row.getRowNum() + 1), InputStatus.UNKNOWN, //
              getStringValue(row, 0), getStringValue(row, 1), getStringValue(row, 2));
          var rowProcessResults = parseSpreadsheetRecord(spreadsheetRecord);
          processResult.merge(rowProcessResults);
        }
      }
    } catch (Exception e) {
      logger.error("Exception processing Excel file: " + file.getPath() + ", " + e.getMessage());
      e.printStackTrace();
    }

    logger.info("file: " + file.getName() + ", ok inputRecords: " + processResult.okList().size());
    logger.debug("\n" + processResult.okList().stream().map(Object::toString).collect(Collectors.joining("\n")));

    logger.info("file: " + file.getName() + ", error inputRecords: " + processResult.errorList().size());
    logger.debug("\n" + processResult.errorList().stream().map(Object::toString).collect(Collectors.joining("\n")));

    return processResult;
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
