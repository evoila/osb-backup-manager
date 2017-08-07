package de.evoila.cf;

import de.evoila.cf.service.exception.ProcessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.io.*;

@Component
public class StartupEventListener implements ApplicationListener<ContextRefreshedEvent> {
    Logger log = LoggerFactory.getLogger(getClass());
    @Override
    public void onApplicationEvent(final ContextRefreshedEvent event) {
        File f = new File(this.getClass().getResource("/startup.sh").getFile());
        try {
            Runtime.getRuntime().exec("chmod +x "+ f.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        ProcessBuilder pb = new ProcessBuilder(f.getAbsolutePath(), f.getParent());
        try {
            runProcess(pb);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ProcessException e) {
            e.printStackTrace();
        }

    }

    private void runProcess (ProcessBuilder processBuilder) throws IOException, InterruptedException, ProcessException {
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

            log.warn(errorLog.toString());
        }
    }
}
