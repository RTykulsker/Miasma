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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.config.IConfigurationManager;
import com.surftools.config.MiasmaKey;

/**
 * we're going to do a lot of file/folder creation/move/copy/delete
 *
 * let's consolidate it all here
 *
 */
public class FileManager {
  @SuppressWarnings("unused")
  private final static String doc = """
      <anywhere>/<CONFIG>    # configuration file, specified on command line
      $BATCH_HOME/
        inbox/    # where unprocessed spreadsheet files are placed; specified in conf file
        output/
          csv/
            batch-details.csv         # one row per spreadsheet record, sent
            batch-details-ERRORS.csv  # one row per spreadsheet record, not sent
            counterContext.csv        # one row per counterContext
          winlinkExpress/             # where WinlinkExpress "import" file written
          pat/
            <YOUR_CALL>
              archive/  # empty
              in/       # empty
              out/      # where we write PAT "b2f" files
              sent/     # where PAT moves b2f files after processing
        processed/<BATCH_ID>/
                <CONFIG>  # copy of configuration file used for this run
                inbox/    # copy of ALL inbox spreadsheet files; non-spreadsheet files ignored

        I don't think it's necessary to copy the files under output/ into processed/<BATCH_ID>
        I do think it's necessary to copy the input (and config) files into processed/<BATCH_ID>
        I think it's going to be optional to remove files from input (ie move vs copy) after processing
      """;

  static final Logger logger = LoggerFactory.getLogger(FileManager.class);

  private String baseInboxPathString;

  private Path processedPath;
  private Path inboxPath;
  private boolean isProcessedEnabled = false;
  private boolean isInboxCleanEnabled = false;

  public FileManager(String batchId, IConfigurationManager cm) {
    var processedPathString = cm.get(MiasmaKey.BATCH_PROCESSED_PATH);
    if (processedPathString == null) {
      isProcessedEnabled = false;
      logger.warn("Processed directory processing NOT enabled");
    } else {
      try {
        isProcessedEnabled = true;
        processedPath = Path.of(processedPathString, batchId);
        Files.createDirectories(processedPath);
        logger.info("created directory: " + processedPath.toString());

        baseInboxPathString = cm.getAsString(MiasmaKey.BATCH_INBOX_PATH);
        var inboxName = new File(baseInboxPathString).getName();
        inboxPath = Path.of(processedPath.toString(), inboxName);
        Files.createDirectories(inboxPath);
        logger.info("created directory: " + inboxPath.toString());

        isInboxCleanEnabled = cm.getAsBoolean(MiasmaKey.BATCH_INBOX_CLEAN_ENABLED);
        logger.info("inbox clean enabled: " + isInboxCleanEnabled);
      } catch (Exception e) {
        logger.error("Exception creating directory structure for " + processedPathString + ", " + e.getMessage());
        e.printStackTrace();
      }
    }
  }

  public void copyConfig(String confFileName) {
    if (!isProcessedEnabled) {
      logger.info("didn't copy config file: " + confFileName + " because not enabled");
      return;
    }

    var source = Path.of(confFileName);
    var name = new File(confFileName).getName();
    var target = Path.of(processedPath.toString(), name);
    try {
      Files.copy(source, target);
      logger.info("Copied configuration file from " + source + " to " + target);
    } catch (IOException e) {
      logger.error("Exception copying configuration file from " + source + " to " + target + ", " + e.getMessage());
      e.printStackTrace();
    }
  }

  public void copySpreadsheetFile(File file) {
    if (!isProcessedEnabled) {
      logger.info("didn't copy spreadsheet file: " + file.getName() + " because not enabled");
      return;
    }

    var source = Path.of(file.toString());
    var fileName = file.getPath();
    var substring = fileName.substring(baseInboxPathString.length());
    var target = Path.of(inboxPath.toString(), substring);
    var targetParentPath = target.toFile().getParentFile().toPath();
    try {
      Files.createDirectories(targetParentPath);
      Files.copy(source, target);
      logger.info("Copied spreadsheet file from " + source + " to " + target);
    } catch (IOException e) {
      logger.error("Exception copying spreadsheet file from " + source + " to " + target + ", " + e.getMessage());
      e.printStackTrace();
    }

  }

  public void cleanInbox() {
    if (!isProcessedEnabled || !isInboxCleanEnabled) {
      logger.info("didn't remove base inbox: " + baseInboxPathString + " because not enabled");
      return;
    }

    try {
      var baseInboxPath = Path.of(baseInboxPathString);
      Files.walk(baseInboxPath).map(Path::toFile).sorted(Comparator.reverseOrder()).forEach(File::delete);
      Files.createDirectories(baseInboxPath);
      logger.info("cleaned all files from: " + baseInboxPathString);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
