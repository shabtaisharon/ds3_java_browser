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

package com.spectralogic.dsbrowser.gui.components.about;

import com.google.inject.Inject;
import com.spectralogic.dsbrowser.api.injector.Presenter;
import com.spectralogic.dsbrowser.gui.services.BuildInfoServiceImpl;
import com.spectralogic.dsbrowser.gui.util.Constants;
import com.spectralogic.dsbrowser.gui.util.treeItem.SafeHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ResourceBundle;

@Presenter
public class AboutPresenter implements Initializable {

    private final static Logger LOG = LoggerFactory.getLogger(AboutPresenter.class);

    @FXML
    private Label copyRightLabel1, copyRightLabel2;

    @FXML
    private Hyperlink apacheLicenseLink, dsbReleasesLink;

    @FXML
    private Label title;

    @FXML
    private Label buildVersion, buildDateTime;

    @FXML
    private Button okButton;

    private final ResourceBundle resourceBundle;
    private final BuildInfoServiceImpl buildInfoService;

    @Inject
    public AboutPresenter(final ResourceBundle resourceBundle,
            final BuildInfoServiceImpl buildInfoService) {
        this.resourceBundle = resourceBundle;
        this.buildInfoService = buildInfoService;
    }

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            title.setText(resourceBundle.getString("title"));
            buildVersion.setText(buildInfoService.getBuildVersion());
            buildDateTime.setText(buildInfoService.getBuildDateTime().toString());
            dsbReleasesLink.setOnAction(SafeHandler.logHandle(event -> tryToOpenInBrowser(Constants.DSB_RELEASES_URI)));
            copyRightLabel1.setText(resourceBundle.getString("copyrightTxt1"));
            apacheLicenseLink.setOnAction(SafeHandler.logHandle(event -> tryToOpenInBrowser(Constants.APACHE_URI)));
            copyRightLabel2.setText(resourceBundle.getString("copyrightTxt2"));
        } catch (final Throwable t) {
            LOG.error("Encountered an error initializing the AboutPresenter", t);
        }
    }

    @NotNull
    private static void tryToOpenInBrowser(final URI uri) {
        if (Desktop.isDesktopSupported()) {
            final Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                try {
                    desktop.browse(uri);
                } catch (final IOException e) {
                    LOG.error("Unable to open link", e);
                }
            } else {
                LOG.info("OS does not support sending links to browser");
            }
        } else {
            LOG.info("OS does not support Desktop");
        }
    }

    public void closeDialog() {
        final Stage popupStage = (Stage) okButton.getScene().getWindow();
        popupStage.close();
    }
}
