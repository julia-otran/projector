/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.utils.promise;

import javafx.application.Platform;

/**
 *
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
public class JavaFxExecutor<IN> implements Executor<IN> {
    @Override
    public <OUT> void execute(IN input, Task<IN, OUT> task, Callback<OUT> callback) {
        Platform.runLater(() -> {
            try {
                task.execute(input, callback);
            } catch (Exception ex) {
                callback.error(ex);
            }
        });
    }
    
}
