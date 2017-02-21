package com.spectralogic.dsbrowser.gui.components.deletefiles;

import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3DeleteBucketTask;
import com.spectralogic.dsbrowser.gui.util.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.URL;
import java.util.ResourceBundle;

public class DeleteFilesPresenter implements Initializable {

    private final static Logger LOG = LoggerFactory.getLogger(DeleteFilesPresenter.class);

    @FXML
    private TextField deleteField;

    @FXML
    private Button deleteButton;

    @FXML
    private Label deleteLabel, deleteConfirmationInfoLabel;

    @Inject
    private Workers workers;

    @Inject
    private Ds3Task deleteTask;

    @Inject
    private Ds3Common ds3Common;

    @Inject
    private ResourceBundle resourceBundle;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            deleteButton.setDisable(true);
            ObservableList<TreeItem<Ds3TreeTableValue>> selectedItems = null;
            if (ds3Common.getDs3TreeTableView() != null) {
                selectedItems = ds3Common
                        .getDs3TreeTableView().getSelectionModel().getSelectedItems();
            }
            callToChangeLabelText(selectedItems);
            deleteField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue.equals(StringConstants.DELETE)) {
                    deleteButton.setDisable(false);
                } else {
                    deleteButton.setDisable(true);
                }
            });
            deleteField.setOnKeyReleased(event -> {
                if (!deleteButton.isDisabled() && event.getCode().equals(KeyCode.ENTER)) {
                    deleteFiles();
                }
            });

        } catch (final Exception e) {
            LOG.error("Encountered an error making the delete file presenter", e);
        }
    }

    private void callToChangeLabelText(ObservableList<TreeItem<Ds3TreeTableValue>> selectedItems) {
        if (Guard.isNullOrEmpty(selectedItems)) {
            selectedItems = FXCollections.observableArrayList();
            selectedItems.add(ds3Common.getDs3TreeTableView().getRoot());
        }
        changeLabelText(selectedItems);
    }

    private void changeLabelText(final ObservableList<TreeItem<Ds3TreeTableValue>> selectedItems) {
        final TreeItem<Ds3TreeTableValue> valueTreeItem = selectedItems.stream().findFirst().orElse(null);
        if (null != valueTreeItem && valueTreeItem.getValue().getType().equals(Ds3TreeTableValue.Type.File)) {
            deleteLabel.setText(resourceBundle.getString("deleteFiles"));
            deleteConfirmationInfoLabel.setText(resourceBundle.getString("deleteFileInfo"));
        } else if (null != valueTreeItem && valueTreeItem.getValue().getType().equals(Ds3TreeTableValue.Type.Directory)) {
            deleteLabel.setText(resourceBundle.getString("deleteFolder"));
            deleteConfirmationInfoLabel.setText(resourceBundle.getString("deleteFolderInfo"));
        } else {
            deleteLabel.setText(resourceBundle.getString("deleteBucket"));
            deleteConfirmationInfoLabel.setText(resourceBundle.getString("deleteBucketInfo"));
        }
    }

    public void deleteFiles() {
        deleteTask.setOnCancelled(event -> {
            LOG.error("Failed to delete Buckets", ((Ds3DeleteBucketTask) deleteTask).getErrorMsg());
            printLog(LogType.ERROR);
            closeDialog();
        });
        deleteTask.setOnFailed(event -> {
            LOG.error("Failed to delete Buckets", ((Ds3DeleteBucketTask) deleteTask).getErrorMsg());
            Platform.runLater(() -> {
                printLog(LogType.ERROR);
                closeDialog();
            });
            Ds3Alert.show(resourceBundle.getString("deleteFolderErrAlert"), resourceBundle.getString("deleteErrLogs"), Alert.AlertType.ERROR);
        });
        deleteTask.setOnSucceeded(event -> {
            printLog(LogType.SUCCESS);
            closeDialog();
        });
        workers.execute(deleteTask);
    }

    public void cancelDelete() {
        LOG.info("Cancelling delete files");
        closeDialog();
    }

    private void closeDialog() {
        final Stage popupStage = (Stage) deleteField.getScene().getWindow();
        popupStage.close();
    }

    private void printLog(final LogType type) {
        if (type.equals(LogType.ERROR)) {
            ds3Common.getDeepStorageBrowserPresenter().logText(
                    resourceBundle.getString("deleteBucketErr") + StringConstants.SPACE
                            + ((Ds3DeleteBucketTask) deleteTask).getErrorMsg(), LogType.ERROR);
        } else {
            ds3Common.getDeepStorageBrowserPresenter().logText(
                    resourceBundle.getString("deleteBucketSuccess"), LogType.SUCCESS);
        }
    }
}
