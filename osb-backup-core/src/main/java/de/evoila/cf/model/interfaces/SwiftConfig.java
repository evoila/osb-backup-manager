package de.evoila.cf.model.interfaces;

import java.util.Map;

/**
 * Created by yremmet on 05.07.17.
 */
public interface SwiftConfig {

    Map<String, String> getFilenames();

    String getAuthUrl();

    String getUsername ();

    String getPassword ();

    String getDomain ();

    String getProjectName ();

    String getContainerName ();
}
