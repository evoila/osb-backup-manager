package de.evoila.cf.backup;


import de.evoila.cf.model.BackupJob;
import de.evoila.cf.model.enums.DestinationType;
import de.evoila.cf.model.JobStatus;
import de.evoila.cf.repository.BackupAgentJobRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.restdocs.RestDocumentation;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Date;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
//@AutoConfigureMockMvc
public class JobsApiTest {

    private static final String TEST_FILE_NAME = "TEST_FILE.txt";
    private static final String TEST_CONTAINER = "TEST_CONTAINER";
    private static final String TEST_PROJECT = "TEST_PROJECT";

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private BackupAgentJobRepository jobRepository;
    //@Autowired
    private MockMvc mvc;

    public RestDocumentation restDocumentation = new RestDocumentation("target/generated-snippets");
    @Before
    public void setup(){
        this.mvc = MockMvcBuilders.webAppContextSetup(this.context)
                             .apply(documentationConfiguration(this.restDocumentation))
                             .build();
        // Delete all
        //jobRepository.delete(jobRepository.findAll());

    }



    private BackupJob createJob () {
        BackupJob job = new BackupJob();
        job.setInstanceId(TEST_INSTANCE);
        job.setStatus(JobStatus.FAILED);
        job.setStartDate(new Date());
        job.setJobType(BackupJob.BACKUP_JOB);
        job.setDestination(createDestination());
        return job;
    }

    private BackupJob.BackupDestination createDestination () {
        BackupJob.BackupDestination destination = new BackupJob.BackupDestination();
        destination.setFilename(TEST_FILE_NAME);
        destination.setContainer(TEST_CONTAINER);
        destination.setProject(TEST_PROJECT);
        destination.setType(DestinationType.Swift);
        return destination;
    }

    @Test
    public void jobByInstance() throws Exception {
        jobRepository.save(createJob());

        mvc.perform(get("/jobs/byInstance/"+TEST_INSTANCE ))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$", hasSize(1)));

    }


    private static String TEST_INSTANCE = "TestInstance";
}
