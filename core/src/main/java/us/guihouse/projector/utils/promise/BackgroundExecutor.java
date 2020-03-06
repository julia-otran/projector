package us.guihouse.projector.utils.promise;

import javafx.application.Platform;

public class BackgroundExecutor<IN, OUT> implements Executor<IN, OUT> {
    @Override
    public void execute(IN input, Task<IN, OUT> task, Callback<OUT> callback) {
        new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    task.execute(input, callback);
                } catch (Exception ex) {
                    callback.error(ex);
                }
            }
        }).start();
    }
}
