package com.spectralogic.dsbrowser.gui.util;

import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.api.services.ShutdownService;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.jobprioritystore.SavedJobPrioritiesStore;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSessionStore;
import com.spectralogic.dsbrowser.gui.services.tasks.CancelAllRunningJobsTask;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3JobTask;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import static com.spectralogic.dsbrowser.gui.util.StringConstants.*;

public class CloseConfirmationHandler {
    private static final Logger LOG = LoggerFactory.getLogger(CloseConfirmationHandler.class);

    private final ResourceBundle resourceBundle;
    private final JobWorkers jobWorkers;
    private final ShutdownService shutdownService;

    public CloseConfirmationHandler(final ResourceBundle resourceBundle,
                                    final JobWorkers jobWorkers,
                                    final ShutdownService shutdownService) {
        this.jobWorkers = jobWorkers;
        this.resourceBundle = resourceBundle;
        this.shutdownService = shutdownService;
    }

    /**
     * Showing alert for exiting the application
     *
     * @param event event
     */
    public void closeConfirmationAlert(final Event event) {
        LOG.info("Initiating close event");
        if (jobWorkers != null && !Guard.isNullOrEmpty(jobWorkers.getTasks())) {
            final List<Ds3JobTask> notCachedRunningTasks = jobWorkers.getTasks().stream().filter(task -> task.getProgress() != 1).collect(Collectors.toList());
            if (Guard.isNullOrEmpty(notCachedRunningTasks)) {
                event.consume();
                shutdownService.shutdown();
            } else {
                final Optional<ButtonType> closeResponse;
                if (1 == notCachedRunningTasks.size()) {
                    closeResponse = Ds3Alert.showConfirmationAlert(resourceBundle.getString("confirmation"),
                            notCachedRunningTasks.size() + StringConstants.SPACE + resourceBundle.getString("jobStillRunningMessage"),
                            Alert.AlertType.CONFIRMATION, null,
                            resourceBundle.getString("exitButtonText"), resourceBundle.getString("cancelButtonText"));
                } else {
                    closeResponse = Ds3Alert.showConfirmationAlert(resourceBundle.getString("confirmation"),
                            notCachedRunningTasks.size() + StringConstants.SPACE + resourceBundle.getString("multipleJobStillRunningMessage"),
                            Alert.AlertType.CONFIRMATION, null,
                            resourceBundle.getString("exitButtonText"), resourceBundle.getString("cancelButtonText"));
                }
                if (closeResponse.get().equals(ButtonType.OK)) {
                    event.consume();
                    shutdownService.shutdown();
                }
                if (closeResponse.get().equals(ButtonType.CANCEL)) {
                    event.consume();
                }
            }
        } else {
            event.consume();
            shutdownService.shutdown();
        }
    }

    /**
     * To cancel all running jobs
     *
     * @param jobWorkers           jobWorker object
     * @param workers              worker object
     * @param jobInterruptionStore jobInterruptionStore Object
     * @return task
     */
    public Task cancelAllRunningTasks(final JobWorkers jobWorkers,
                                      final Workers workers,
                                      final JobInterruptionStore jobInterruptionStore,
                                      final LoggingService loggingService) {
        LOG.info("Cancelling all running jobs");
        final Task cancelRunningJobsTask = new CancelAllRunningJobsTask(jobWorkers, jobInterruptionStore, loggingService);
        workers.execute(cancelRunningJobsTask);
        return cancelRunningJobsTask;
    }

    /**
     * set preferences for window resize
     *
     * @param x      x
     * @param y      y
     * @param width  width
     * @param height height
     */
    public void setPreferences(final double x, final double y, final double width, final double height
            , final boolean isWindowMaximized) {
        LOG.info("Setting up windows preferences");
        final Preferences preferences = Preferences.userRoot().node(NODE_NAME);
        preferences.putDouble(WINDOW_POSITION_X, x);
        preferences.putDouble(WINDOW_POSITION_Y, y);
        preferences.putDouble(WINDOW_WIDTH, width);
        preferences.putDouble(WINDOW_HEIGHT, height);
        preferences.putBoolean(WINDOW_MAXIMIZED, isWindowMaximized);
    }

    /**
     * save session into local file system
     *
     * @param savedSessionStore savedSessionStore
     */
    public void saveSessionStore(final SavedSessionStore savedSessionStore) {
        LOG.info("Saving session into local file system");
        if (savedSessionStore != null) {
            try {
                SavedSessionStore.saveSavedSessionStore(savedSessionStore);
            } catch (final Exception ex) {
                LOG.error("General Exception while saving session information to the local filesystem", ex);
            }
        }
    }

    /**
     * save job settings to the local file system
     *
     * @param savedJobPrioritiesStore savedJobPrioritiesStore
     */
    public void saveJobPriorities(final SavedJobPrioritiesStore savedJobPrioritiesStore) {
        LOG.info("Saving job settings to the local file system");
        if (savedJobPrioritiesStore != null) {
            try {
                SavedJobPrioritiesStore.saveSavedJobPriorties(savedJobPrioritiesStore);
            }  catch (final Exception ex) {
                LOG.error("General Exception while saving job settings information to the local filesystem", ex);
            }
        }
    }

    /**
     * save Interrupted jobs to the local file system
     *
     * @param jobInterruptionStore jobInterruptionStore
     */
    public void saveInterruptionJobs(final JobInterruptionStore jobInterruptionStore) {
        LOG.info("Saving interrupted jobs to the local file system");
        if (jobInterruptionStore != null) {
            try {
                JobInterruptionStore.saveJobInterruptionStore(jobInterruptionStore);
            } catch (final Exception ex) {
                LOG.error("General Exception while saving job Ids to the local filesystem", ex);
            }
        }
    }
}