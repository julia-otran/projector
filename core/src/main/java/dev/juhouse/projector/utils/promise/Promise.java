/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dev.juhouse.projector.utils.promise;

import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author Julia Otranto Aulicino julia.otranto@outlook.com
 */
public class Promise<X> {
    @Getter
    @Setter
    private Runnable dispatch;

    private Task<X, ?> task;
    private Task<Object, X> errorTask;

    private Executor<X> successExecutor;
    private Executor<Object> errorExecutor;

    private Callback next;
    
    public static <X> Promise<X> create(Task<Void, X> task, Executor<Void> executor) {
        Promise<X> p = new Promise<>();
        
        p.setDispatch(() -> executor.execute(null, task, p.getCallback()));
        
        return p;
    }
    
    public void execute() {
        dispatch.run();
    }
    
    public <Y> Promise<Y> then(Task<X, Y> task, Executor<X> executor) {
        Promise<Y> p = new Promise<>();
        
        p.setDispatch(this.getDispatch());
        this.task = task;
        this.successExecutor = executor;
        this.next = p.getCallback();
        return p;
    }

    public Promise<X> handle(Task<Object, X> task, Executor<Object> executor) {
        Promise<X> p = new Promise<>();

        p.setDispatch(this.getDispatch());
        this.errorTask = task;
        this.errorExecutor = executor;
        this.next = p.getCallback();
        return p;
    }

    private Callback<X> getCallback() {
        return new Callback<>() {
            @Override
            public void success(X out) {
                if (task != null) {
                    successExecutor.execute(out, task, next);
                } else if (next != null) {
                    next.success(out);
                }
            }

            @Override
            public void error(Object obj) {
                if (errorTask != null) {
                    errorExecutor.execute(obj, errorTask, next);
                } else if (next != null) {
                    next.error(obj);
                }
            }

        };
    }
}
