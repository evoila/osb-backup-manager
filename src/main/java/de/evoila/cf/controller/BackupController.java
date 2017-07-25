package de.evoila.cf.controller;

import de.evoila.cf.model.BackupRequest;
import de.evoila.cf.model.RestoreRequest;
import de.evoila.cf.model.BackupJob;
import de.evoila.cf.service.exception.BackupRequestException;
import de.evoila.cf.service.BackupServiceManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;


/**
 * 
 * @author Yannic Remmet
 *
 */
@Controller
public class BackupController {

	@Autowired
	BackupServiceManager serviceManager;

	@RequestMapping(value = "/backup", method = RequestMethod.POST)
	public ResponseEntity restore(@RequestBody BackupRequest backupRequest) throws BackupRequestException {
		BackupJob job = serviceManager.backup(backupRequest);
		return new ResponseEntity<>(job, HttpStatus.CREATED);
	}

	@RequestMapping(value = "/restore", method = RequestMethod.POST)
	public ResponseEntity restore(@RequestBody RestoreRequest restoreRequest) throws BackupRequestException {
		BackupJob job = serviceManager.restore(restoreRequest);
		return new ResponseEntity<>(job, HttpStatus.CREATED);
	}
}
