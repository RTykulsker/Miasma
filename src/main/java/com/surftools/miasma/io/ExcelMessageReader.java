/**

The MIT License (MIT)

Copyright (c) 2026, Robert Tykulsker

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

package com.surftools.miasma.io;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.miasma.MiasmaApp;
import com.surftools.miasma.config.IConfigurationManager;
import com.surftools.miasma.config.MiasmaKey;

public class ExcelMessageReader implements IMessageReader {
  private static final Logger logger = LoggerFactory.getLogger(MiasmaApp.class);

  boolean isAutoDittoEnabled = true;

  private Set<String> ignoreSheetSet;

  public ExcelMessageReader(IConfigurationManager cm) {
    isAutoDittoEnabled = cm.getAsBoolean(MiasmaKey.BATCH_AUTO_DITTO_ENABLED, isAutoDittoEnabled);

    ignoreSheetSet = new LinkedHashSet<String>();
    var ignoreSheetString = cm.getAsString(MiasmaKey.BATCH_EXCEL_IGNORE_SHEET_LIST, "template,instructions");
    var fields = ignoreSheetString.split(",");
    for (var sheetName : fields) {
      if (sheetName != null && !sheetName.strip().isEmpty()) {
        ignoreSheetSet.add(sheetName.toLowerCase());
      }
    }
    logger.info("Ignoring sheet names containing with: " + String.join(",", ignoreSheetSet));
  }

  @Override
  public List<IASMessage> readFile(Path path, FileType fileType, FileSource fileSource) {
    var list = new ArrayList<IASMessage>();

    var now = LocalDateTime.now();
    var dateString = now.toLocalDate().toString();
    var timeString = now.toLocalTime().truncatedTo(ChronoUnit.SECONDS).toString();
    var fileName = path.getFileName().toString();
    var fileTypeName = fileType.name();
    var fileSourceName = fileSource.name();
    var messageId = "";

    var file = path.toFile();
    try (var fis = new FileInputStream(file);
        var workbook = file.getName().endsWith(".xlsx") ? new XSSFWorkbook(fis) : new HSSFWorkbook(fis)) {
      for (var sheet : workbook) {
        var sheetName = sheet.getSheetName();
        if (ignoreSheetSet.contains(sheetName.strip().toLowerCase())) {
          logger.warn("Ignoring file/sheet: " + file.getName() + "/" + sheetName);
          continue;
        }
        logger.debug("processing sheet: " + sheet.getSheetName());

        var metadata = sheet.getSheetName();
        var lastFromName = "";
        var lastToAddresses = "";
        var lastText = "";
        int rowNumber = 0;
        for (var row : sheet) {
          ++rowNumber;
          if (rowNumber == 1) { // skip header row
            continue;
          }

          var fromName = getStringValue(row, 0);
          var toAddresses = getStringValue(row, 1);
          var text = getStringValue(row, 2);
          if (isAutoDittoEnabled) {
            fromName = !fromName.isEmpty() ? fromName : lastFromName;
            toAddresses = !toAddresses.isEmpty() ? toAddresses : lastToAddresses;
            text = !text.isEmpty() ? text : lastText;
          }
          lastFromName = fromName;
          lastToAddresses = toAddresses;
          lastText = text;

          var message = new IASMessage(fromName, toAddresses, text, dateString, timeString, fileName, fileTypeName,
              fileSourceName, messageId, metadata);
          list.add(message);
        } // end loop over rows in sheet
      } // end loop over sheets in workbook
    } catch (Exception e) {
      logger.error("Exception processing Excel file: " + file.getPath() + ", " + e.getMessage());
      e.printStackTrace();
    }
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

    case FORMULA: {
      CellType cachedCellType = cell.getCachedFormulaResultType();
      if (cachedCellType == CellType.STRING) {
        return cell.getStringCellValue().strip();
      } else if (cachedCellType == CellType.NUMERIC) {
        return Double.toString(cell.getNumericCellValue());
      } else if (cachedCellType == CellType.BOOLEAN) {
        return Boolean.toString(cell.getBooleanCellValue());
      }
    }

    case NUMERIC:
      return Double.toString(cell.getNumericCellValue());

    case STRING:
      return cell.getStringCellValue().strip();

    default:
      logger
          .error("Unsupported type: " + cell.getCellType().name() + " on row: " + row.getRowNum() + ", col: "
              + columnIndex);
      return "";
    }
  }
}
