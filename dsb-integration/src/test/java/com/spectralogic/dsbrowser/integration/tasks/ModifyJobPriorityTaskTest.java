/*
 * ******************************************************************************
 *    Copyright 2016-2017 Spectra Logic Corporation. All Rights Reserved.
 *    Licensed under the Apache License, Version 2.0 (the "License"). You may not use
 *    this file except in compliance with the License. A copy of the License is located at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *    or in the "license" file accompanying this file.
 *    This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *    CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *    specific language governing permissions and limitations under the License.
 * ******************************************************************************
 */

package com.spectralogic.dsbrowser.integration.tasks;

import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.Ds3ClientBuilder;
import com.spectralogic.ds3client.models.Priority;
import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.modifyjobpriority.ModifyJobPriorityModel;
import com.spectralogic.dsbrowser.gui.services.BuildInfoServiceImpl;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.FilesAndFolderMap;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.newSessionService.SessionModelService;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedCredentials;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSession;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.tasks.CreateConnectionTask;
import com.spectralogic.dsbrowser.gui.services.tasks.GetJobPriorityTask;
import com.spectralogic.dsbrowser.gui.services.tasks.ModifyJobPriorityTask;
import com.spectralogic.dsbrowser.gui.util.ConfigProperties;
import com.spectralogic.dsbrowser.gui.util.AlertService;
import com.spectralogic.dsbrowser.gui.util.StringConstants;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.CountDownLatch;

import static junit.framework.TestCase.assertTrue;

public class ModifyJobPriorityTaskTest {
    private final Workers workers = new Workers();
    private Session session;
    private boolean successFlag = false;
    private final static ResourceBundle resourceBundle = ResourceBundle.getBundle("lang", new Locale(ConfigProperties.getInstance().getLanguage()));
    private static final Ds3Client client = Ds3ClientBuilder.fromEnv().withHttps(false).build();
    private static final BuildInfoServiceImpl buildInfoService = new BuildInfoServiceImpl();
    private static final String TEST_ENV_NAME = "ModifyJobPriorityTaskTest";
    private final static AlertService ALERT_SERVICE = new AlertService(resourceBundle, new Ds3Common());
    private final static CreateConnectionTask createConnectionTask = new CreateConnectionTask(ALERT_SERVICE, resourceBundle, buildInfoService);

    @Before
    public void setUp() throws Exception {
        new JFXPanel();
        Platform.runLater(() -> {
            final SavedSession savedSession = new SavedSession(
                    TEST_ENV_NAME,
                    client.getConnectionDetails().getEndpoint(),
                    "80",
                    null,
                    new SavedCredentials(
                            client.getConnectionDetails().getCredentials().getClientId(),
                            client.getConnectionDetails().getCredentials().getKey()),
                    false,
                    false);
            session = createConnectionTask.createConnection(SessionModelService.setSessionModel(savedSession, false));
        });
    }

    @Test
    public void modifyPriority() throws Exception {
        final CountDownLatch latch = new CountDownLatch(2);
        Platform.runLater(() -> {
            try {
                //Loading all interrupted jobs
                final JobInterruptionStore jobInterruptionStore = JobInterruptionStore.loadJobIds();

                //Getting jobId of interrupted job
                final Optional<Map<String, Map<String, FilesAndFolderMap>>> endPointsMap = jobInterruptionStore.getJobIdsModel()
                        .getEndpoints().stream().filter(endpoint -> endpoint.containsKey(session.getEndpoint()
                                + StringConstants.COLON + session.getPortNo())).findFirst();
                if (endPointsMap.isPresent()&& !Guard.isMapNullOrEmpty(endPointsMap.get())) {

                    final Map<String, FilesAndFolderMap> stringFilesAndFolderMapMap =
                            endPointsMap.get().get(session.getEndpoint() + StringConstants.COLON + session.getPortNo());
                    final Optional<String> jobId = stringFilesAndFolderMapMap.entrySet().stream()
                            .map(Map.Entry::getKey)
                            .findFirst();

                    if (jobId.isPresent() && !Guard.isStringNullOrEmpty(jobId.get())) {
                        //Getting priority of the jobId
                        final GetJobPriorityTask getJobPriorityTask = new GetJobPriorityTask(session, UUID.fromString(jobId.get()));
                        workers.execute(getJobPriorityTask);
                        latch.countDown();
                        final ModifyJobPriorityModel value = getJobPriorityTask.getValue();

                        //Changing priority of job
                        final ModifyJobPriorityTask modifyJobPriorityTask = new ModifyJobPriorityTask(value, Priority.LOW);
                        workers.execute(modifyJobPriorityTask);

                        //Validating test case
                        modifyJobPriorityTask.setOnSucceeded(event -> {
                            successFlag = true;
                            latch.countDown();
                        });
                        modifyJobPriorityTask.setOnFailed(event -> latch.countDown());
                        modifyJobPriorityTask.setOnCancelled(event -> latch.countDown());
                    } else {
                        successFlag = true;
                        latch.countDown();
                        latch.countDown();
                    }
                } else {
                    successFlag = true;
                    latch.countDown();
                    latch.countDown();
                }
            } catch (final Exception e) {
                e.printStackTrace();
                latch.countDown();
                latch.countDown();
            }
        });
        latch.await();
        assertTrue(successFlag);
    }
}