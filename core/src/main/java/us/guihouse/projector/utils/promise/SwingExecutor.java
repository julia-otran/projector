/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.utils.promise;

import javax.swing.SwingUtilities;

/**
 *
 * @author guilherme
 */
public class SwingExecutor<IN, OUT> implements Executor<IN, OUT> {

    @Override
    public void execute(IN input, Task<IN, OUT> task, Callback<OUT> callback) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    task.execute(input, callback);
                } catch (Exception ex) {
                    callback.error(ex);
                }
            }
        });
    }
    
}
