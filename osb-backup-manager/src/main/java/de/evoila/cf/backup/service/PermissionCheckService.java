package de.evoila.cf.backup.service;

import javax.servlet.http.HttpServletRequest;
import java.util.NoSuchElementException;

public interface PermissionCheckService {
    public boolean hasReadAccess(HttpServletRequest request);

    public boolean hasReadAccess(String serviceInstanceId);


}
