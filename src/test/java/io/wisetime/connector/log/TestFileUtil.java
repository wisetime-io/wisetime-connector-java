/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.log;

import com.amazonaws.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * @author thomas.haines@practiceinsight.io
 */
public class TestFileUtil {

  public static String getFile(String name) {
    try {
      try (InputStream stream = TestFileUtil.class.getResourceAsStream(name)) {
        return IOUtils.toString(stream);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }


}
