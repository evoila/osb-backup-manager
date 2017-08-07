package de.evoila.cf.backup;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.evoila.cf.model.*;
import de.evoila.cf.model.enums.DatabaseType;
import de.evoila.cf.model.enums.DestinationType;
import de.evoila.cf.model.enums.RetentionStyle;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Date;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public abstract class MockMvcTest {

    static final String TEST_FILE_NAME = "TEST_FILE.txt";
    static final String TEST_CONTAINER = "TEST_CONTAINER";
    static final String TEST_PROJECT = "TEST_PROJECT";
    static final String TEST_INSTANCE = "TEST_INSTANCE";
    static final int RETENTION_PERIOD = 1;
    static final String FREQUENCY = "* * 14 * * *";
    static final String PASSWORD = "PASSWORD";
    static final String USERNAME = "USERNAME";
    static final DatabaseType DATABASE_TYPE = DatabaseType.MySQL;
    static final String HOSTNAME = "127.0.0.1";
    static final int PORT = 3306;
    static final String DATABASE_NAME = "database_1";
    static final String AUTH_URL = "http://os.eu-de.darz.msh.host";
    static final String TEST_DOMAIN = "TEST_DOMAIN";

    @Autowired
    public WebApplicationContext context;

    public MockMvc mvc;

    @Rule
    public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("target/generated-docs");
    public RestDocumentationResultHandler document;

    @Before
    public void setup(){
        this.document = document("{method-name}", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()));

        this.mvc = MockMvcBuilders.webAppContextSetup(this.context)
                         .apply(documentationConfiguration(this.restDocumentation))
                         .alwaysDo(this.document)

                         .build();
    }

    protected BackupJob createJob () {
        return  createJob(JobStatus.SUCCESS);
    }

    protected BackupJob createJob (JobStatus status) {
        BackupJob job = new BackupJob();
        job.setInstanceId(TEST_INSTANCE);
        job.setStatus(status);
        job.setStartDate(new Date());
        job.setJobType(BackupJob.BACKUP_JOB);
        job.setDestination(createDestination());
        return job;
    }

    protected BackupJob.BackupDestination createDestination () {
        BackupJob.BackupDestination destination = new BackupJob.BackupDestination();
        destination.setFilename(TEST_FILE_NAME);
        destination.setContainer(TEST_CONTAINER);
        destination.setProject(TEST_PROJECT);
        destination.setType(DestinationType.Swift);
        return destination;
    }

    protected BackupPlan createDummyPlan () {
        BackupPlan plan = new BackupPlan();
        plan.setRetentionStyle(RetentionStyle.DAYS);
        plan.setRetentionPeriod(RETENTION_PERIOD);
        plan.setFrequency(FREQUENCY);
        plan.setSource(createDummySource());
        //plan.setDestination(createDummyDestination());
        return plan;
    }

    protected FileDestination createDummyDestination () {
        FileDestination destination = new FileDestination();
        destination.setContainerName(TEST_CONTAINER);
        destination.setFilename(TEST_FILE_NAME);
        destination.setAuthUrl(AUTH_URL);
        destination.setDomain(TEST_DOMAIN);
        destination.setUsername(USERNAME);
        destination.setPassword(PASSWORD);
        destination.setProjectName(TEST_PROJECT);
        destination.setType(DestinationType.Swift);
        return destination;
    }

    protected DatabaseCredential createDummySource () {
        DatabaseCredential credential = new DatabaseCredential();
        credential.setPassword(PASSWORD);
        credential.setUsername(USERNAME);
        credential.setType(DATABASE_TYPE);
        credential.setHostname(HOSTNAME);
        credential.setPort(PORT);
        credential.setContext(TEST_INSTANCE);
        return credential;
    }

    public FieldDescriptor[] getDestinationDescriptors () {
        return  new FieldDescriptor[]{
              fieldWithPath("destination.authUrl").description("Auth endpoint for Openstack Swift"),
              fieldWithPath("destination.type").description("Storage Type. Always Swift"),
              fieldWithPath("destination.containerName").description("Openstack Swift container the backup is uploaded to"),
              fieldWithPath("destination.filename").description("File Name of the Backup in the Swift container"),
              fieldWithPath("destination.username").description("Openstack Username for Swift upload"),
              fieldWithPath("destination.projectName").description("Openstack project"),
              fieldWithPath("destination.password").description("Openstack password"),
              fieldWithPath("destination.domain").description("Openstack domain")
        };
    }

    public FieldDescriptor[] getSoruceDestinationDescriptor () {
        return new FieldDescriptor[]{
              fieldWithPath("source.port").description("Database-Server Port"),
              fieldWithPath("source.type").description("Database Type which will be backuped. Valid Values are MySQL, MongoDB & PostgreSQL "),
              fieldWithPath("source.hostname").description("Database-Server Hostname or IP-Address"),
              fieldWithPath("source.context").description("Database Name"),
              fieldWithPath("source.password").description("Database User Password"),
              fieldWithPath("source.username").description("Database Username")};
    }


    protected FieldDescriptor[] getPlanDescriptors () {
        return new FieldDescriptor[]{
              fieldWithPath("id").description("Plan ID"),
              fieldWithPath("frequency").description("Defines the interval the backup will run in the cron format"),
              fieldWithPath("retentionStyle").description(
                    "Defines how long old backups should be preserved. Valid values are ALL (Keeps all), DAYS, HOURS (Keeps them the amount that is defined in retentionPeriod), FILES (Keeps a defined amount of files)"),
              fieldWithPath("retentionPeriod").description("")
        };
    }


    protected FieldDescriptor[] getJobStartedDescriptors () {
        return new FieldDescriptor[]{fieldWithPath("id").description("Id of the Backup Job"),
              fieldWithPath("startDate").description("Staring time of the Backup"),
              fieldWithPath("instanceId").description("UUID of Service Instance"),
              fieldWithPath("status").description(
                    "Current status of the Job. Possible values are STARTED, IN PROGRESS, SUCCESS and FAILED."),
              fieldWithPath("jobType").description("Discribles if the job either an backup or an restore job.")};
    }

    protected FieldDescriptor[] getJobDescriptors () {
        return new FieldDescriptor[]{fieldWithPath("id").description("Id of the Backup Job"),
              fieldWithPath("startDate").description("Staring time of the Backup"),
              fieldWithPath("instanceId").description("UUID of Service Instance"),
              fieldWithPath("status").description(
                    "Current status of the Job. Possible values are STARTED, IN PROGRESS, SUCCESS and FAILED."),
              fieldWithPath("jobType").description("Discribles if the job either an backup or an restore job."),
              fieldWithPath("destination").description("Destination object"),
              fieldWithPath("destination.type").description("Storage Type. Always Swift"),
              fieldWithPath("destination.project").description("Openstack Project the backup is uploaded to"),
              fieldWithPath("destination.container").description("Openstack Swift container the backup is uploaded to"),
              fieldWithPath("destination.filename").description("File Name of the Backup in the Swift container")};
    }


    protected String toJson (Object obj) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(obj);
    }

}
