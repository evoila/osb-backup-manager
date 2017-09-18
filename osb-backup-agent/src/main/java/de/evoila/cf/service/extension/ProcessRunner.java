package de.evoila.cf.service.extension;

import de.evoila.cf.model.BackupJob;
import de.evoila.cf.service.exception.ProcessException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by yremmet on 10.07.17.
 */
public interface ProcessRunner {
  public default Process runProcess (ProcessBuilder processBuilder, BackupJob job) throws IOException, InterruptedException, ProcessException {
    Process process = processBuilder.start();
    InputStream processInputStream = process.getErrorStream();
    StringBuilder errorLog = new StringBuilder();
    if (processInputStream != null) {
      BufferedReader streamReader = new BufferedReader(new InputStreamReader(processInputStream));
      String line = streamReader.readLine();
      process.waitFor();

      while (line != null) {
        errorLog.append(line);
        line = streamReader.readLine();
      }

      job.appendLog(errorLog.toString());
      if (process.exitValue() > 0) {
        throw new ProcessException(errorLog.toString());
      }
    }
    return process;
  }


}

