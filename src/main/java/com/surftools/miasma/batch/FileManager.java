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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.config.IConfigurationManager;

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
            miasma.csv       # one row per spreadsheet record, sent and non-send ### rename to spreadsheet-details.csv
            counters.csv     # one row per counterContext
          winlinkExpress/    # where WinlinkExpress "import" file written
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

  private IConfigurationManager cm;
  private String batchId;

  public FileManager(String batchId, IConfigurationManager cm) {
    this.batchId = batchId;
    this.cm = cm;
  }
}
