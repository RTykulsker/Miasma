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

package com.surftools.miasma.utils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtils {
  static final Logger logger = LoggerFactory.getLogger(FileUtils.class);

  /**
   * recursively remove directory and all contents
   *
   * @param path
   * @return true, if successful
   */
  public static boolean deleteDirectory(Path path) {
    try {
      if (Files.exists(path)) {
        Files //
            .walk(path) //
              .map(Path::toFile) //
              .sorted((o1, o2) -> -o1.compareTo(o2)) //
              .forEach(File::delete);
      }
      return true;
    } catch (Exception e) {
      logger.error("exception deleting directory: " + path.toString() + ", " + e.getLocalizedMessage());
      return false;
    }
  }

  /**
   * (recursively) create directory
   *
   * @param path
   * @return true, if successful
   */
  public static boolean createDirectory(Path path) {
    try {
      Files.createDirectories(path);
      return true;
    } catch (Exception e) {
      logger.error("couldn't create directory: " + path.toString() + ", " + e.getLocalizedMessage() + ". Exiting.");
      return false;
    }
  }

  /**
   *
   * @param srcPath
   * @param dstPath
   */
  public static void syncDirectory(Path srcPath, Path dstPath, boolean doDeletes) {
    logger
        .info("begin syncDir, src: " + srcPath.toFile().getName() + ", dst: " + dstPath.toFile().getName()
            + ", doDeletes: " + doDeletes);

    var srcs = srcPath.toFile().list();
    var dsts = dstPath.toFile().list();
    var dstSet = new HashSet<String>(Arrays.asList(dsts));

    for (var fileName : srcs) {
      if (!dstSet.contains(fileName)) {
        FileUtils.copy(Path.of(srcPath.toString(), fileName), Path.of(dstPath.toString(), fileName));
      }
    }

    logger
        .info("end syncDir, src: " + srcPath.toFile().getName() + ", dst: " + dstPath.toFile().getName()
            + ", doDeletes: " + doDeletes);
  }

  public static boolean copy(Path src, Path dst) {
    try {
      Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
      logger
          .info("copied file: " + src.toString() + " to " + dst.toFile().getParent() + ", " + dst.toFile().length()
              + " bytes");
      return true;
    } catch (Exception e) {
      logger
          .error("exception copying file: " + src.toString() + " to " + dst.toFile().getParent() + ", "
              + e.getLocalizedMessage());
      return false;
    }
  }

  /**
   * read a file into a String, cache nothing, read from disk every time
   *
   * @param path
   * @param label
   * @return file contents as single String or throw
   */
  public static String readFile(Path path, String label) {

    try {
      return Files.readString(path);
    } catch (Exception e) {
      throw new RuntimeException("exception reading " + label + " file: " + path.toString() + ", " + e.getMessage());
    }
  } // end readFile
}
