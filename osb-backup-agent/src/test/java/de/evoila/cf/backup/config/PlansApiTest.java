package de.evoila.cf.backup.config;

import de.evoila.cf.model.BackupPlan;
import de.evoila.cf.model.enums.RetentionStyle;
import de.evoila.cf.backup.repository.BackupPlanRepository;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PlansApiTest extends MockMvcTest{



    @Autowired
    BackupPlanRepository repository;

    @Before
    public void cleanup(){
        // Delete all
        repository.deleteAll();
    }


    @Test
    public void createPlan () throws Exception {
        BackupPlan plan = createDummyPlan();
        FieldDescriptor[] dest = getDestinationDescriptors();
        FieldDescriptor[] src = getSoruceDestinationDescriptor();
        FieldDescriptor[] responseDescriptors = getPlanDescriptors();
        FieldDescriptor[] requestDescriptors = new FieldDescriptor[]{
              fieldWithPath("frequency").description( "Defines the interval the backup will run in the cron format"),
              fieldWithPath("retentionStyle").description(""),
              fieldWithPath("retentionPeriod").description("")
        };

        responseDescriptors = TestUtils.concatenate(responseDescriptors, dest);
        responseDescriptors = TestUtils.concatenate(responseDescriptors, src);
        requestDescriptors = TestUtils.concatenate(requestDescriptors, src);
        requestDescriptors = TestUtils.concatenate(requestDescriptors, dest);


        mvc.perform(post("/plans")
                          .contentType(MediaType.APPLICATION_JSON)
                          .content(toJson(plan)))
              .andExpect(status().isCreated())
              .andExpect(jsonPath("id").isNotEmpty())
              .andExpect(jsonPath("retentionStyle").value(plan.getRetentionStyle().toString()))
              .andExpect(jsonPath("retentionPeriod").value(RETENTION_PERIOD))
              .andExpect(jsonPath("frequency").value(FREQUENCY))
              .andDo(document("create-plan", responseFields(responseDescriptors)))
              .andDo(document("create-plan", requestFields(requestDescriptors)))
        ;

    }

    @Test
    public void plansByInstance() throws Exception {
        BackupPlan plan = createDummyPlan();
        repository.save(plan);
        repository.save(plan);


        Object fields;
        mvc.perform(get("/plans/byInstance/"+TEST_INSTANCE )
                          .accept(MediaType.APPLICATION_JSON))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$", hasSize(1)))
              .andDo(document("plans-by-instance", responseFields(
                    fieldWithPath("[]").description("List of Backup Plans for this Service Instance")
              )))
        ;

    }

    @Test
    public void updatePlan () throws Exception {
        String frequency = "* * * * * *";
        int retentionPeriod = 5;
        BackupPlan plan = createDummyPlan();
        repository.save(plan);

        plan.setFrequency(frequency);
        plan.setRetentionPeriod(retentionPeriod);
        //plan.getDestination().setPassword("secret");
        plan.setRetentionStyle(RetentionStyle.ALL);

        FieldDescriptor[] dest = getDestinationDescriptors();
        FieldDescriptor[] src = getSoruceDestinationDescriptor();
        FieldDescriptor[] responseDescriptors = getPlanDescriptors();

        responseDescriptors = TestUtils.concatenate(responseDescriptors, dest);
        responseDescriptors = TestUtils.concatenate(responseDescriptors, src);



        mvc.perform(patch("/plan/"+plan.getId())
                          .contentType(MediaType.APPLICATION_JSON)
                          .content(toJson(plan)))
              .andExpect(status().isOk())
              .andExpect(jsonPath("id").isNotEmpty())
              .andExpect(jsonPath("retentionStyle").value(RetentionStyle.ALL.toString()))
              .andExpect(jsonPath("retentionPeriod").value(retentionPeriod))
              .andExpect(jsonPath("frequency").value(frequency))
              .andDo(document("update-plan", responseFields(responseDescriptors)))
              .andDo(document("update-plan", requestFields(responseDescriptors)))
        ;

    }

    @Test
    public void deletePlan () throws Exception {

        BackupPlan plan = createDummyPlan();
        repository.save(plan);


        FieldDescriptor[] dest = getDestinationDescriptors();
        FieldDescriptor[] src = getSoruceDestinationDescriptor();
        FieldDescriptor[] responseDescriptors = getPlanDescriptors();

        responseDescriptors = TestUtils.concatenate(responseDescriptors, dest);
        responseDescriptors = TestUtils.concatenate(responseDescriptors, src);

        mvc.perform(delete("/plan/"+plan.getId())
                          .contentType(MediaType.APPLICATION_JSON)
                          .content(toJson(plan)))
              .andExpect(status().isOk())
              .andExpect(jsonPath("id").isNotEmpty())
              .andExpect(jsonPath("retentionStyle").value(plan.getRetentionStyle().toString()))
              .andExpect(jsonPath("retentionPeriod").value(RETENTION_PERIOD))
              .andExpect(jsonPath("frequency").value(FREQUENCY))
              .andDo(document("delete-plan", responseFields(responseDescriptors)))
              .andDo(document("delete-plan", requestFields(responseDescriptors)));

    }



}
