/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.forms.controllers;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.concurrent.Worker.State;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.web.WebEngine;
import us.guihouse.projector.projection.ProjectionManager;
import us.guihouse.projector.projection.ProjectionWebView;

/**
 * FXML Controller class
 *
 * @author guilherme
 */
public class BrowserController extends ProjectionController {
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        endProjectionButton.disableProperty().set(true);
    }    
    
    private ProjectionWebView projectionWebView;
    
    @FXML
    private Button beginProjectionButton;

    @FXML
    private Button endProjectionButton;

    @FXML
    private TextField adddressTextField;

    @FXML
    private ScrollPane browserPane;

    @FXML
    private ProgressBar browserProgressBar;

    @FXML
    public void onBeginProjection() {
        beginProjectionButton.disableProperty().set(true);
        endProjectionButton.disableProperty().set(false);
        getProjectionManager().setWebView(projectionWebView);
    }

    @FXML
    public void onEndProjection() {
        beginProjectionButton.disableProperty().set(false);
        endProjectionButton.disableProperty().set(true);
        getProjectionManager().setWebView(null);
    }

    @FXML
    public void onAddressFiledKeyPress(KeyEvent ke) {
        if (ke.getCode().equals(KeyCode.ENTER)) {
            String url = adddressTextField.getText();
            this.projectionWebView.getWebView().getEngine().load(url);
        }
    }

    @Override
    public void initWithProjectionManager(ProjectionManager projectionManager) {
        super.initWithProjectionManager(projectionManager);
        this.projectionWebView = projectionManager.createWebView();
        browserPane.setContent(projectionWebView.getNode());
        WebEngine engine = projectionWebView.getWebView().getEngine();
        browserProgressBar.progressProperty().bind(engine.getLoadWorker().progressProperty());
        
        engine.getLoadWorker().titleProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                if (newValue != null && !newValue.isEmpty()) {
                    notifyTitleChange(newValue);
                }
            }
        });
        
        engine.getLoadWorker().stateProperty().addListener(new ChangeListener<Worker.State>() {
            @Override
            public void changed(ObservableValue<? extends Worker.State> observable, Worker.State oldValue, Worker.State newValue) {
                String newTitle = engine.getTitle();
                if (newTitle != null  && !newTitle.isEmpty()) {
                    notifyTitleChange(newTitle);
                }
                adddressTextField.setText(engine.getLocation());
            }
        });
        
        engine.load("https://www.google.com");
    }
}
