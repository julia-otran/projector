package us.guihouse.projector.utils.promise;

public class BackgroundExecutor<IN> implements Executor<IN> {
    @Override
    public <OUT> void execute(IN input, Task<IN, OUT> task, Callback<OUT> callback) {
        new Thread(() -> {
            try {
                task.execute(input, callback);
            } catch (Exception ex) {
                callback.error(ex);
            }
        }).start();
    }
}
