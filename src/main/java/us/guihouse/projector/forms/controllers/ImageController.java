/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.forms.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import us.guihouse.projector.projection.ProjectionImage;
import us.guihouse.projector.projection.ProjectionManager;

/**
 * FXML Controller class
 *
 * @author guilherme
 */
public class ImageController extends ProjectionController{
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        beginProjectionButton.disableProperty().set(true);
        endProjectionButton.disableProperty().set(true);
        imageView.fitWidthProperty().bind(imagePane.widthProperty());
        imageView.fitHeightProperty().bind(imagePane.heightProperty());
        
        imageView.fitWidthProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                centerImage();
            }
        });
        
        imageView.fitHeightProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                centerImage();
            }
        }
        );
    }    
    
    @FXML
    private Button beginProjectionButton;
    
    @FXML
    private Button endProjectionButton;
    
    @FXML
    private Label dragDropLabel;
    
    @FXML
    private Pane imagePane;
    
    @FXML
    private ImageView imageView;
    private Image currentImage;
    private Image addingImage;
    private String title;
    
    private ProjectionImage projectable;
    
    private boolean imageAdded;
    private String oldLabelText;

    @Override
    public void initWithProjectionManager(ProjectionManager projectionManager) {
        super.initWithProjectionManager(projectionManager);
        this.projectable = projectionManager.createImage();
        projectable.setCropBackground(false);
    }
    
    @FXML
    public void onDragOver(DragEvent event) {
        if (currentImage != null) {
            setError("Já existe uma imagem aqui. Adicione uma nova imagem à lista de projeção.");
            return;
        }
        
        Dragboard board = event.getDragboard();
        
        Image input = null;
        
        if (board.hasFiles()) {
            if (board.getFiles().size() > 1) {
                setError("Não é possivel ler mais de um arquivo.");
                return;
            } else {
                File file = board.getFiles().get(0);
                InputStream is = null;
                
                try {
                    is = new FileInputStream(file);
                    input = new Image(is);
                    title = file.getName();
                } catch (IOException ex) {
                    Logger.getLogger(ImageController.class.getName()).log(Level.SEVERE, null, ex);
                    input = null;
                } catch (Exception ex) {
                    Logger.getLogger(ImageController.class.getName()).log(Level.SEVERE, null, ex);
                    input = null;
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException ex) {
                            Logger.getLogger(ImageController.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
        }
        
        if (input == null) {
            try {
                title = "Imagem sem título";
                input = board.getImage();
            } catch (Exception ex) {
                Logger.getLogger(ImageController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        if (input == null) {
            setError("Imagem/Arquivo inválido");
            return;
        }
        
        addingImage = input;
        event.acceptTransferModes(TransferMode.LINK);
        setLoading();
        prepareImage();
    }
    
    private void setLoading() {
        oldLabelText = dragDropLabel.getText();
        dragDropLabel.setText("Aguarde, lendo imagem...");
        imagePane.setBorder(new Border(new BorderStroke(null, BorderStrokeStyle.SOLID, null, new BorderWidths(3))));
    }
    
    private void setSuccess() {
        dragDropLabel.setText("Solte na área demarcada");
        imagePane.setBorder(new Border(new BorderStroke(Color.GREEN, BorderStrokeStyle.SOLID, null, new BorderWidths(3))));
    }
    
    private void setError(String error) {
        dragDropLabel.setVisible(true);
        dragDropLabel.setText(error);
        imagePane.setBorder(new Border(new BorderStroke(Color.RED, BorderStrokeStyle.SOLID, null, new BorderWidths(3))));
    }
    
    private void setOriginal() {
        dragDropLabel.setText(oldLabelText);
        imagePane.setBorder(null);
    }
    
    @FXML
    public void onDragExit() {
        setOriginal();
        
        if (imageAdded) {
            dragDropLabel.setVisible(false);
        } else {
            addingImage = null;
            imageView.setImage(null);
        }
    }
    
    @FXML
    public void onDragDropped(DragEvent event) {
        beginProjectionButton.disableProperty().set(false);
        imageAdded = true;
        currentImage = addingImage;
        addingImage = null;
        notifyTitleChange(title);
    }
    
    @FXML
    public void onBeginProjection() {
        projectable.setImage(SwingFXUtils.fromFXImage(currentImage, null));
        getProjectionManager().setProjectable(projectable);
        
        beginProjectionButton.disableProperty().set(true);
        endProjectionButton.disableProperty().set(false);
    }
    
    @FXML
    public void onEndProjection() {
        getProjectionManager().setProjectable(null);
        beginProjectionButton.disableProperty().set(false);
        endProjectionButton.disableProperty().set(true);
    }

    private void prepareImage() {
        imageView.setImage(addingImage);
        
        if (addingImage.isBackgroundLoading()) {
            addingImage.errorProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                    if (newValue) {
                        setError("Imagem ou arquivo inválido.");
                        imageView.setImage(null);
                        addingImage = null;
                    } else {
                        setSuccess();
                        centerImage();
                    }
                }
            });
        } else {
            if (addingImage.isError()) {
                setError("Imagem ou arquivo inválido.");
                imageView.setImage(null);
                addingImage = null;
            } else {
                setSuccess();
                centerImage();
            }
        }
    }
    
    private void centerImage() {
        Image img = imageView.getImage();
        if (img != null) {
            double w = 0;
            double h = 0;

            double ratioX = imageView.getFitWidth() / img.getWidth();
            double ratioY = imageView.getFitHeight() / img.getHeight();

            double reducCoeff = 0;
            if(ratioX >= ratioY) {
                reducCoeff = ratioY;
            } else {
                reducCoeff = ratioX;
            }

            w = img.getWidth() * reducCoeff;
            h = img.getHeight() * reducCoeff;

            imageView.setX((imageView.getFitWidth() - w) / 2);
            imageView.setY((imageView.getFitHeight() - h) / 2);
        }
    }
}
