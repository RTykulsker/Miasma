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

package com.surftools.miasmaV2.io;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class IoUtils {

  /**
   * create a directory iff it doesn't already exist
   *
   * @param path
   * @return
   */
  public static Path makeDirIfNeeded(Path path) {
    String dirString = path.toString();
    File dirFile = new File(dirString);
    if (dirFile.isAbsolute()) {
      if (!dirFile.exists()) {
        boolean ok = dirFile.mkdirs();
        if (!ok) {
          throw new RuntimeException("dir path for: " + dirString + " not found and can't be created");
        }
      }
      return Path.of(dirString);
    }
    return null;
  }

  /**
   * move a file to another path, without specifying the file name for the destination
   *
   * @param fromFilePath
   * @param toDirPath
   *          -- just the directory name, no file name
   * @return
   * @throws Exception
   */
  public static Path moveWithFileName(Path fromFilePath, Path toDirPath) throws Exception {
    makeDirIfNeeded(toDirPath);
    var toFilePath = Path.of(toDirPath.toString(), fromFilePath.getFileName().toString());
    Files.move(fromFilePath.toAbsolutePath(), toFilePath);
    return toFilePath;
  }

  /**
   * return a timestamp string, down to the millisecond
   *
   * @return YYMMDD-HHMMSS.mmm
   */
  public static String getMilliStamp() {
    var now = LocalDateTime.now();
    var date = now.toLocalDate();
    var time = now.toLocalTime();

    return String
        .format("%02d%02d%02d-%02d%02d%02d-%03d", //
            date.getYear(), date.getMonthValue(), date.getDayOfMonth(), //
            time.getHour(), time.getMinute(), time.getSecond(), //
            time.getNano() / 1_000_000);
  }

  public static Path moveWithFileNameAndTimeStamp(Path fromFilePath, Path toDirPath) throws Exception {
    makeDirIfNeeded(toDirPath);
    var timestamp = getMilliStamp();
    var toFilePath = Path.of(toDirPath.toString(), timestamp + "." + fromFilePath.getFileName().toString());
    Files.move(fromFilePath.toAbsolutePath(), toFilePath);
    return toFilePath;
  }

  /**
   * return @FileType of path
   *
   * @param path
   * @return
   */
  public static FileType getFileType(Path path) {
    File directory = path.toFile();
    if (directory.isDirectory()) {
      return FileType.DIRECTORY;
    }

    var fileName = path.getFileName().toString().toLowerCase();
    for (var fileType : FileType.getFileTypes()) {
      var suffixes = fileType.getSuffixes();
      for (var suffix : suffixes) {
        if (fileName.endsWith(suffix)) {
          return fileType;
        }
      }
    }

    return FileType.UNSUPPORTED;
  }

}