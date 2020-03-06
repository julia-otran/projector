/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.forms.controllers.projection;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import us.guihouse.projector.forms.controllers.projection.ProjectionController;
import us.guihouse.projector.projection.ProjectionManager;
import us.guihouse.projector.projection.TextWrapperFactoryChangeListener;
import us.guihouse.projector.projection.text.TextWrapper;
import us.guihouse.projector.projection.text.WrappedText;
import us.guihouse.projector.projection.text.WrapperFactory;

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

        projectionText.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                int sz = newValue != null ? newValue.length() : 0;
                if (sz == 0) {
                    notifyTitleChange("Novo Texto");
                    getObserver().updateProperty("TEXT", "");
                } else {
                    sz = Math.min(50, sz);
                    notifyTitleChange(newValue.substring(0, sz));
                    getObserver().updateProperty("TEXT", newValue);
                }
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
