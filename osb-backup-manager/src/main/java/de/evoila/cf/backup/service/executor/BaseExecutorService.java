package de.evoila.cf.backup.service.executor;

import de.evoila.cf.backup.controller.exception.BackupException;
import de.evoila.cf.model.agent.response.AgentExecutionResponse;
import de.evoila.cf.model.api.endpoint.EndpointCredential;
import de.evoila.cf.model.enums.BackupType;
import de.evoila.cf.model.enums.DestinationType;
import org.springframework.core.ParameterizedTypeReference;

import java.util.List;

/**
 * @author Johannes Hiemer, Yannic Remmet.
 *
 * A generic specification for a executor services.
 */
public interface BaseExecutorService {

    BackupType getSourceType();

    List<DestinationType> getDestinationTypes();

    <T extends AgentExecutionResponse> T pollExecutionState(EndpointCredential endpointCredential, String suffix, String id,
                                                            ParameterizedTypeReference<T> type) throws BackupException;

}
