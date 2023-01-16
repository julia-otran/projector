/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.forms.controllers.projection;

import java.net.URL;
import java.util.ResourceBundle;

import dev.juhouse.projector.projection.ProjectionManager;
import dev.juhouse.projector.projection.ProjectionWebView;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Transform;
import javafx.scene.web.WebEngine;

/**
 * FXML Controller class
 *
 * @author guilherme
 */
public class BrowserController extends ProjectionController {

    public static final String URL_PROPERTY = "URL";

    private Transform currentTransform;

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
        getProjectionManager().setProjectable(projectionWebView);
    }

    @FXML
    public void onEndProjection() {
        getProjectionManager().setProjectable(null);
    }

    @FXML
    public void onAddressFiledKeyPress(KeyEvent ke) {
        if (ke.getCode().equals(KeyCode.ENTER)) {
            String url = adddressTextField.getText();
            this.getObserver().updateProperty(URL_PROPERTY, url);
            this.projectionWebView.getWebView().getEngine().load(url);
        }
    }

    @Override
    public void initWithProjectionManager(ProjectionManager projectionManager) {
        super.initWithProjectionManager(projectionManager);
        this.projectionWebView = projectionManager.createWebView();

        String url = this.getObserver().getProperty(URL_PROPERTY);

        if (url == null) {
            url = "https://google.com.br";
        }

        Node displayNode = projectionWebView.getNode();

        Pane scalePane = new Pane();
        scalePane.setMaxWidth(Double.MAX_VALUE);
        scalePane.setMaxHeight(Double.MAX_VALUE);

        browserPane.setContent(scalePane);
        scalePane.getChildren().add(displayNode);

        ChangeListener<Number> sizeChangeListener = (observable, oldValue, newValue) -> {
            double ww = projectionWebView.getWebView().getWidth();
            double wh = projectionWebView.getWebView().getHeight();
            double pw = browserPane.getWidth();
            double ph = browserPane.getHeight();

            double sw = pw / ww;
            double sh = ph / wh;
            double s = Math.min(sw, sh);

            scalePane.setPrefSize(pw * s, ph * s);

            scalePane.getTransforms().remove(currentTransform);
            currentTransform = new Scale(s, s, 0.0, 0.0);
            scalePane.getTransforms().add(currentTransform);
        };

        projectionWebView.getWebView().widthProperty().addListener(sizeChangeListener);
        projectionWebView.getWebView().heightProperty().addListener(sizeChangeListener);
        browserPane.widthProperty().addListener(sizeChangeListener);
        browserPane.heightProperty().addListener(sizeChangeListener);
        sizeChangeListener.changed(null, null, null);

        WebEngine engine = projectionWebView.getWebView().getEngine();
        browserProgressBar.progressProperty().bind(engine.getLoadWorker().progressProperty());

        engine.getLoadWorker().titleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.isEmpty()) {
                notifyTitleChange(newValue);
            }
        });

        engine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            String newTitle = engine.getTitle();
            if (newTitle != null && !newTitle.isEmpty()) {
                notifyTitleChange(newTitle);
            }
            adddressTextField.setText(engine.getLocation());
            getObserver().updateProperty(URL_PROPERTY, engine.getLocation());
        });

        engine.load(url);

        getProjectionManager().projectableProperty().addListener((observableValue, oldValue, newValue) -> {
            if (newValue == projectionWebView) {
                beginProjectionButton.disableProperty().set(true);
                endProjectionButton.disableProperty().set(false);
            } else {
                beginProjectionButton.disableProperty().set(false);
                endProjectionButton.disableProperty().set(true);
            }
        });
    }

    @Override
    public void onEscapeKeyPressed() {
        if (!endProjectionButton.isDisabled()) {
            endProjectionButton.fire();
        }
    }

    @Override
    public void stop() {
        onEscapeKeyPressed();
        projectionManager.stop(projectionWebView);
    }
}
