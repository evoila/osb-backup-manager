package de.evoila.cf.backup;


import de.evoila.cf.model.BackupJob;
import de.evoila.cf.model.FileDestination;
import de.evoila.cf.model.JobStatus;
import de.evoila.cf.repository.BackupAgentJobRepository;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;



public class JobsApiTest extends MockMvcTest {

    @Autowired
    public BackupAgentJobRepository jobRepository;

    @Before
    public void cleanup(){
        // Delete all
        jobRepository.deleteAll();
    }


    @Test
    public void jobsByInstance() throws Exception {
        jobRepository.deleteAll();

        jobRepository.save(createJob());

        Object fields;
        mvc.perform(get("/jobs/byInstance/"+TEST_INSTANCE )
                          .accept(MediaType.APPLICATION_JSON))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$", hasSize(1)))
        .andDo(document("jobs-by-instance", responseFields(
              fieldWithPath("[]").description("List of Backup Jobs for this Service Instance")
        )))
       ;

    }

    @Test
    public void getJob() throws Exception {
        BackupJob job = createJob();
        jobRepository.save(job);
        mvc.perform(get("/job/"+ job.getId()))
              .andExpect(status().isOk())
              .andExpect(jsonPath("id").value(job.getId()))
              .andExpect(jsonPath("startDate").value(job.getStartDate()))
              .andExpect(jsonPath("instanceId").value(TEST_INSTANCE))
              .andExpect(jsonPath("jobType").value(BackupJob.BACKUP_JOB))
              .andExpect(jsonPath("status").value(JobStatus.SUCCESS.toString()))
              .andExpect(jsonPath("destination.project").value(TEST_PROJECT))
              .andExpect(jsonPath("destination.container").value(TEST_CONTAINER))
              .andExpect(jsonPath("destination.filename").value(TEST_FILE_NAME))
              .andDo(document("get-job", responseFields(
                    getJobDescriptors()
              )));

    }

    @Test
    public void deleteJob() throws Exception {
        jobRepository.deleteAll();

        BackupJob job = createJob();
        jobRepository.save(job);
        jobRepository.save(job);

        FileDestination fileDestination = createDummyDestination();
        fileDestination.setFilename(null);
        mvc.perform(delete("/job/"+ job.getId()).content(toJson(fileDestination)))
              .andExpect(status().isNoContent())
              .andDo(document("delete-job", requestFields(
                    fieldWithPath("authUrl").description("Auth endpoint for Openstack Swift"),
                    fieldWithPath("type").description("Storage Type. Always Swift"),
                    fieldWithPath("containerName").description("Openstack Swift container the backup is uploaded to"),
                    fieldWithPath("username").description("Openstack Username for Swift upload"),
                    fieldWithPath("projectName").description("Openstack project"),
                    fieldWithPath("password").description("Openstack password"),
                    fieldWithPath("domain").description("Openstack domain")
              )));
    }




}
