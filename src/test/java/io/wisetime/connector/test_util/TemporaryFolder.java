package io.wisetime.connector.test_util;

import com.github.javafaker.Faker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.stream.IntStream;

import static java.nio.file.FileVisitResult.CONTINUE;

public class TemporaryFolder {
  private static final Logger log = LoggerFactory.getLogger(TemporaryFolder.class);

  private File rootFolder;
  private final Faker faker = Faker.instance();

  public File newFile(String name) throws IOException {
    File result = new File(rootFolder, name);
    result.createNewFile();
    return result;
  }

  public File newFolder() throws IOException {
    // try up to five random names
    return IntStream.range(1, 5)
        .mapToObj(i -> new File(rootFolder, faker.bothify("folder######")))
        .filter(file -> !file.exists())
        .filter(File::mkdir)
        .findFirst()
        .orElseThrow(() -> new RuntimeException("no file could be created"));
  }

  void prepare() {
    try {
      rootFolder = File.createTempFile("junit5-", ".tmp");
      log.info("rootFolder='{}'", rootFolder.getAbsolutePath());
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
    if (rootFolder.exists()) {
      rootFolder.delete();
    }
    boolean mkdirs = rootFolder.mkdirs();
    if (!mkdirs) {
      log.warn("mkdir not created");
    }
  }

  void cleanUp() {
    try {
      Files.walkFileTree(rootFolder.toPath(), new DeleteAllVisitor());
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  private static class DeleteAllVisitor extends SimpleFileVisitor<Path> {
    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
      Files.delete(file);
      return CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path directory, IOException exception) throws IOException {
      Files.delete(directory);
      return CONTINUE;
    }
  }

}