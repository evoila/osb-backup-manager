package de.evoila.cf.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Created by yremmet on 27.06.17.
 */

@Service
public class Configuration
{
    @Value("backup.mysql.path")
    private  String mysqlbackuppath;

    public String getMysqlbackuppath() {
        return mysqlbackuppath;
    }
}
