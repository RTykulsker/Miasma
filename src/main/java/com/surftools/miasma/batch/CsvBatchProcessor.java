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
import java.io.FileReader;
import java.util.ArrayList;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.surftools.config.IConfigurationManager;

public class CsvBatchProcessor extends BaseBatchProcessor {
  private static final Logger logger = LoggerFactory.getLogger(CsvBatchProcessor.class);

  public CsvBatchProcessor(String batchId, File file, IConfigurationManager cm) {
    super(batchId, file, cm);

  }

  public ProcessResult process() {
    logger.info("Processing: " + file.getAbsolutePath());

    var processResult = new ProcessResult(new ArrayList<InputRecord>(), new ArrayList<InputRecord>(),
        new CounterContext(batchId));

    var rowCount = -1;
    try {
      ++processResult.counterContext().fileCount;
      ++processResult.counterContext().tabCount;
      var separator = ',';
      var ignoreQuotes = false;
      var skipLines = 0;
      var reader = new FileReader(file.getPath());
      var parser = new CSVParserBuilder() //
          .withSeparator(separator) //
            .withIgnoreQuotations(ignoreQuotes) //
            .build();
      CSVReader csvReader = new CSVReaderBuilder(reader) //
          .withSkipLines(skipLines)//
            .withCSVParser(parser)//
            .build();
      rowCount = 0;
      String[] fields = null;
      while ((fields = csvReader.readNext()) != null) {
        ++rowCount;
        var inputRecord = new InputRecord(batchId, file.getPath(), "n/a", //
            String.valueOf(rowCount), InputStatus.UNKNOWN, //
            fields[0], fields[1], fields[2]);
        var rowProcessResults = parse(inputRecord);
        processResult.merge(rowProcessResults);
      }
    } catch (Exception e) {
      logger.error("Exception processing CSV file " + file.toString() + ", row " + rowCount + ", " + e.getMessage());
    }

    logger
        .info("ok inputRecords: " + "\n"
            + processResult.okList().stream().map(Object::toString).collect(Collectors.joining("\n")));
    logger
        .info("error inputRecords: " + "\n"
            + processResult.errorList().stream().map(Object::toString).collect(Collectors.joining("\n")));
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
