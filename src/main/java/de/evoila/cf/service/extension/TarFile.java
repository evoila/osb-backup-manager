package de.evoila.cf.service.extension;

import de.evoila.cf.service.exception.ProcessException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by yremmet on 06.07.17.
 */
public interface TarFile {
  public default File tarGz (File input) throws InterruptedException, ProcessException, IOException {
    return tarGz(input, false);
  }

  public default File tarGz (File input, boolean delete) throws InterruptedException, ProcessException, IOException {
    File output = new File(String.format("%s.tar.gz", input.getName()));
    Process process = new ProcessBuilder("tar",
                                         "cfz",
                                         output.getPath(),
                                         input.getPath()
    ).redirectError(new File("tar.err.log")).redirectOutput(new File("tar.err.log")).start();
    process.waitFor();
    if (process.exitValue() > 0 || ! output.exists()) {
      throw new ProcessException();
    }
    if (delete) {
      input.delete();
    }

    return output;
  }

  public default List<File> unTarGz (File input) throws InterruptedException, ProcessException, IOException {
    return unTarGz(input, false);
  }

  public default List<File> unTarGz (File input, boolean delete) throws InterruptedException, ProcessException, IOException {

    Process process = new ProcessBuilder("tar",
                                         "xfvz",
                                         input.getAbsolutePath()
    ).start();

    InputStream processInputStream = process.getErrorStream();
    List<File> fileList = new ArrayList();
    if (processInputStream != null) {

      BufferedReader streamReader = new BufferedReader(new InputStreamReader(processInputStream));
      String newFolder = streamReader.readLine();
      while (newFolder != null) {
        newFolder = newFolder.substring(2);
        fileList.add(new File(newFolder));
        newFolder = streamReader.readLine();
      }

      process.waitFor();

      if (process.exitValue() > 0) {
        throw new ProcessException();
      }
      if (delete) {
        input.delete();
      }
      return fileList;
    }
    throw new ProcessException();

  }
}
