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

import dev.juhouse.projector.projection2.ProjectionLabel;
import dev.juhouse.projector.projection2.ProjectionManager;
import dev.juhouse.projector.projection2.TextWrapperFactoryChangeListener;
import dev.juhouse.projector.projection2.text.TextWrapper;
import dev.juhouse.projector.projection2.text.WrappedText;
import dev.juhouse.projector.projection2.text.WrapperFactory;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;

/**
 * FXML Controller class
 *
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
public class TextController extends ProjectionController implements TextWrapperFactoryChangeListener, ProjectionBarControlCallbacks {

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        projecting = false;
    }    
    
    @FXML
    private Pane projectionControlPane;

    private final ProjectionBarControl controlBar = new ProjectionBarControl();
    
    @FXML
    private TextField projectionText;
    
    private boolean projecting;

    private ProjectionLabel projectable;

    @Override
    public void initWithProjectionManager(ProjectionManager projectionManager) {
        super.initWithProjectionManager(projectionManager);

        projectable = projectionManager.createLabel();

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

        getObserver().getProperty("TEXT").ifPresent(s -> projectionText.textProperty().setValue(s));

        controlBar.setProjectable(projectable);
        controlBar.setCallback(this);
        controlBar.setManager(projectionManager);
        controlBar.attach(projectionControlPane);
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
        projectable.setText(text.get(0));
    }

    @Override
    public void onEscapeKeyPressed() {
        if (controlBar.getProjecting()) {
            onProjectionEnd();
        }
    }

    @Override
    public void stop() {
        onEscapeKeyPressed();
        projectionManager.stop(projectable);
    }

    @Override
    public void onProjectionBegin() {
        projectionManager.setProjectable(projectable);
        printText();
        projectionText.disableProperty().set(true);
        projecting = true;
    }

    @Override
    public void onProjectionEnd() {
        projectionManager.setProjectable(null);
        projectionText.disableProperty().set(false);
        projectable.setText(null);
        projecting = false;
    }
}
