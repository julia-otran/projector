/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.forms.controllers.projection;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

import dev.juhouse.projector.projection.ProjectionManager;
import dev.juhouse.projector.projection.TextWrapperFactoryChangeListener;
import dev.juhouse.projector.projection.text.TextWrapper;
import dev.juhouse.projector.projection.text.WrappedText;
import dev.juhouse.projector.projection.text.WrapperFactory;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

/**
 * FXML Controller class
 *
 * @author guilherme
 */
public class TextController extends ProjectionController implements TextWrapperFactoryChangeListener {

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        projecting = false;
        endProjectionButton.disableProperty().set(true);
    }    
    
    @FXML
    private Button beginProjectionButton;
    
    @FXML
    private Button endProjectionButton;
    
    @FXML
    private TextField projectionText;
    
    private boolean projecting;

    @Override
    public void initWithProjectionManager(ProjectionManager projectionManager) {
        super.initWithProjectionManager(projectionManager);

        projectionText.textProperty().addListener((observable, oldValue, newValue) -> {
            int sz = newValue != null ? newValue.length() : 0;
            if (sz == 0) {
                notifyTitleChange("Novo Texto");
                getObserver().updateProperty("TEXT", "");
            } else {
                sz = Math.min(50, sz);
                notifyTitleChange(newValue.substring(0, sz));
                getObserver().updateProperty("TEXT", newValue);
            }
        });

        getProjectionManager().addTextWrapperChangeListener(this);
        projectionText.textProperty().setValue(getObserver().getProperty("TEXT"));
    }
    
    @FXML
    public void onBeginProjection() {
        printText();
        beginProjectionButton.disableProperty().set(true);
        endProjectionButton.disableProperty().set(false);
        projectionText.disableProperty().set(true);
        
        projecting = true;
    }
    
    @FXML
    public void onEndProjection() {
        projectionText.disableProperty().set(false);
        beginProjectionButton.disableProperty().set(false);
        endProjectionButton.disableProperty().set(true);
        getProjectionManager().setText(null);
        projecting = false;
    }

    @Override
    public void onWrapperFactoryChanged(WrapperFactory factory) {
        if (projecting) {
            printText();
        }
    }
    
    private void printText() {
        TextWrapper tw = getProjectionManager().getWrapperFactory().getTextWrapper(true);
        
        List<WrappedText> text = tw.fitGroups(Collections.singletonList(projectionText.getText()));
        
        if (text.size() <= 0) {
            return;
        }
        
        // TODO: Warn a error. Too much text to fit on screen if text.size() > 1
        getProjectionManager().setText(text.get(0));
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
    }
}
