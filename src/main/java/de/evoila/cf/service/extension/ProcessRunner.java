package de.evoila.cf.service.extension;

import de.evoila.cf.service.exception.ProcessException;

import java.io.*;

/**
 * Created by yremmet on 10.07.17.
 */
public interface ProcessRunner {
  public default Process runProcess(ProcessBuilder processBuilder) throws IOException, InterruptedException, ProcessException {
    Process process = processBuilder.start();
    InputStream processInputStream = process.getErrorStream();
    StringBuilder errorLog = new StringBuilder();
    if (processInputStream != null) {
      BufferedReader streamReader = new BufferedReader(new InputStreamReader(processInputStream));
      String line = streamReader.readLine();
      while (line != null) {
        errorLog.append(line);
        line = streamReader.readLine();
      }
      process.waitFor();
      if (process.exitValue() > 0) {
        throw new ProcessException(errorLog.toString());
      }
    }
    return process;
  }
}

