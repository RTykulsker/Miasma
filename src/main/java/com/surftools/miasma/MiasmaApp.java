package com.surftools.miasma;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.miasma.config.IConfigurationManager;
import com.surftools.miasma.config.MiasmaKey;
import com.surftools.miasma.config.PropertyFileConfigurationManager;
import com.surftools.miasma.io.CsvMessageReader;
import com.surftools.miasma.io.ExcelMessageReader;
import com.surftools.miasma.io.FileSource;
import com.surftools.miasma.io.FileType;
import com.surftools.miasma.io.IMessageReader;
import com.surftools.miasma.io.IoUtils;
import com.surftools.miasma.io.TextMessageReader;
import com.surftools.miasma.io.WebMessageReader;
import com.surftools.miasma.web.WebServer;
import com.surftools.miasma.winlink.IWinlinkFormatter;
import com.surftools.miasma.winlink.PatWinlinkFormatter;

/**
 * main class for miasma (My I Am Safe Messaging Application)
 */

/**
 * old miasma 2218 lines of code
 */
public class MiasmaApp {
  private static final Logger logger = LoggerFactory.getLogger(MiasmaApp.class);
  protected Path rootPath;
  protected IConfigurationManager cm;
  protected IWinlinkFormatter wf;

  protected Path inboxPath;
  protected Path pendingPath;
  protected Path outboxPath;
  protected Path unsupportedPath;

  protected Map<FileType, IMessageReader> fileTypeReaderMap;
  protected ColorLogger clog;

  @Option(name = "--conf", usage = "name of configuration file", required = true)
  private String confFileName = null;

  public static void main(String[] args) {
    var app = new MiasmaApp();
    CmdLineParser parser = new CmdLineParser(app);
    try {
      parser.parseArgument(args);
      app.run();
    } catch (Exception e) {
      System.err.println(e.getMessage());
      e.printStackTrace();
      parser.printUsage(System.err);
    }
  }

  private void run() throws Exception {
    logger.info("begin run");
    initialize();

    try (var stream = Files.newDirectoryStream(inboxPath)) {
      logger.info("Processing files already in inbox");
      for (var path : stream) {
        processFile(path);
      }
      stream.close();

      WatchService watchService = FileSystems.getDefault().newWatchService();
      inboxPath.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
      logger.info("Watching for changes to in " + inboxPath);
      while (true) {
        WatchKey key = watchService.take(); // Wait for a watch event
        for (WatchEvent<?> event : key.pollEvents()) {
          WatchEvent.Kind<?> kind = event.kind();
          Path fileNamePath = (Path) event.context(); // Context for directory entry event is the file name
          if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
            // I don't know why this is needed
            var file = Path.of(inboxPath.toString(), fileNamePath.toString()).toFile();
            if (file.exists()) {
              logger.info("file " + fileNamePath.toString() + " was created!");
              processFile(fileNamePath);
            }
          }

          boolean valid = key.reset();
          if (!valid) {
            break;
          }
        } // end poll for events

      } // end while true
    } catch (Exception e) {
      logger.error("Exception in Miasma: " + e.getMessage());
      e.printStackTrace();
    } // end catch

    logger.info("end run");
  }

  private void initialize() throws Exception {
    cm = new PropertyFileConfigurationManager(confFileName, MiasmaKey.values());
    clog = new ColorLogger(logger, cm);
    var rootPathString = cm.getAsString(MiasmaKey.ROOT_PATH);
    rootPath = Path.of(rootPathString);
    logger.info("rootPath: " + rootPathString);

    inboxPath = IoUtils.makeDirIfNeeded(Path.of(rootPathString, "files", "inbox"));
    pendingPath = IoUtils.makeDirIfNeeded(Path.of(rootPathString, "files", "pending"));
    outboxPath = IoUtils.makeDirIfNeeded(Path.of(rootPathString, "files", "outbox"));
    unsupportedPath = IoUtils.makeDirIfNeeded(Path.of(rootPathString, "files", "unsupported"));

    fileTypeReaderMap = new HashMap<>();
    fileTypeReaderMap.put(FileType.CSV, new CsvMessageReader(cm));
    fileTypeReaderMap.put(FileType.EXCEL, new ExcelMessageReader(cm));
    fileTypeReaderMap.put(FileType.TEXT, new TextMessageReader());
    fileTypeReaderMap.put(FileType.WEB, new WebMessageReader());

    wf = new PatWinlinkFormatter(cm);
    new WebServer(cm);
  }

  private void processFile(Path inboxFilePath) throws Exception {
    var fileType = IoUtils.getFileType(inboxFilePath);
    var fileSource = (fileType == FileType.WEB)//
        ? FileSource.WEB //
        : (inboxFilePath.getFileName().toString().contains("upload")) //
            ? FileSource.UPLOAD
            : FileSource.FILE;
    logger.debug("processFile: " + inboxFilePath + ", type: " + fileType.name());

    // don't timestamp WEB files, because they are already timestamped
    Path pendingFilePath = null;
    inboxFilePath = Path.of(inboxPath.toAbsolutePath().toString(), inboxFilePath.getFileName().toString());

    if (fileType == FileType.WEB || fileSource == FileSource.UPLOAD) {
      pendingFilePath = IoUtils.moveWithFileName(inboxFilePath, pendingPath);
    } else {
      pendingFilePath = IoUtils.moveWithFileNameAndTimeStamp(inboxFilePath, pendingPath);
    }

    // we can't process directories, move them into unsupported
    if (fileType == FileType.DIRECTORY) {
      var unsupportedFilePath = IoUtils.moveWithFileName(pendingFilePath.toAbsolutePath(), unsupportedPath);
      clog.log("warn", "Can't process directory. Moved to: " + unsupportedFilePath);
      return;
    }

    // non-spreadsheet, text or web files, move them into unsupported
    if (fileType == FileType.UNSUPPORTED) {
      var unsupportedFilePath = IoUtils.moveWithFileName(pendingFilePath.toAbsolutePath(), unsupportedPath);
      clog.log("warn", "Can't process unsupported file type. Moved to: " + unsupportedFilePath);
      return;
    }

    var reader = fileTypeReaderMap.get(fileType);
    var messages = reader.readFile(pendingFilePath, fileType, fileSource);
    wf.format(messages, inboxFilePath);

    var outboxFilePath = IoUtils.moveWithFileName(pendingFilePath.toAbsolutePath(), outboxPath);

    logger.debug("end processFile, moved to: " + outboxFilePath);
  }

}
