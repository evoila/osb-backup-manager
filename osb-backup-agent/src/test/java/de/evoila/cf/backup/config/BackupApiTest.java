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

    @Test
    public void backup() throws Exception {
        BackupRequest request = new BackupRequest();
        request.setPlan(createDummyPlan());

        FieldDescriptor[] responseDescriptors = getDestinationDescriptors();
        responseDescriptors = TestUtils.concatenate(responseDescriptors, getSoruceDestinationDescriptor());

        mvc.perform(post("/backup")
                          .contentType(MediaType.APPLICATION_JSON)
                          .content(toJson(request)))
              .andExpect(status().isCreated())
              .andExpect(jsonPath("id").isNotEmpty())
              .andDo(document("backup", responseFields(getJobStartedDescriptors())))
              .andDo(document("backup", requestFields(responseDescriptors)));
    }

    @Test
    public void restore() throws Exception {
        RestoreRequest request = new RestoreRequest();
        request.setPlan(createDummyPlan());
        request.setSource(createDummyDestination());

        FieldDescriptor[] responseDescriptors = restoreSource();
        responseDescriptors = TestUtils.concatenate(responseDescriptors, restoreDest());

        mvc.perform(post("/restore")
                          .contentType(MediaType.APPLICATION_JSON)
                          .content(toJson(request)))
              .andExpect(status().isCreated())
              .andExpect(jsonPath("id").isNotEmpty())
              .andDo(document("restore", requestFields(responseDescriptors)));
    }

    private FieldDescriptor[] restoreSource () {
        return  new FieldDescriptor[]{
              fieldWithPath("source.authUrl").description("Auth endpoint for Openstack Swift"),
              fieldWithPath("source.type").description("Storage Type. Always Swift"),
              fieldWithPath("source.containerName").description("Openstack Swift container the backup is uploaded to"),
              fieldWithPath("source.filename").description("File Name of the Backup in the Swift container"),
              fieldWithPath("source.username").description("Openstack Username for Swift upload"),
              fieldWithPath("source.projectName").description("Openstack project"),
              fieldWithPath("source.password").description("Openstack password"),
              fieldWithPath("source.domain").description("Openstack domain")
        };
    }

    private FieldDescriptor[] restoreDest () {
        return new FieldDescriptor[]{
              fieldWithPath("destination.port").description("Database-Server Port"),
              fieldWithPath("destination.type").description("Database Type which will be backuped. Valid Values are MySQL, MongoDB & PostgreSQL "),
              fieldWithPath("destination.hostname").description("Database-Server Hostname or IP-Address"),
              fieldWithPath("destination.context").description("Database Name"),
              fieldWithPath("destination.password").description("Database User Password"),
              fieldWithPath("destination.username").description("Database Username")};
    }
}
