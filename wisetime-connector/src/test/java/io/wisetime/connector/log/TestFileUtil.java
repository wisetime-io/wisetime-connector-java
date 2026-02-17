/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.log;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;

/**
 * @author thomas.haines
 */
public class TestFileUtil {

  public static String getFile(String name) {
    try {
      try (InputStream stream = TestFileUtil.class.getResourceAsStream(name)) {
        return IOUtils.toString(stream, StandardCharsets.UTF_8);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }


}
