package de.evoila.cf.backup.config;

import de.evoila.cf.model.BackupRequest;
import de.evoila.cf.model.RestoreRequest;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class BackupApiTest extends MockMvcTest {


}
