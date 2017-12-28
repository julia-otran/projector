/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.utils.promise;

import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author guilherme
 */
public class Promise<X> {
    @Getter
    @Setter
    private Runnable dispatch;
    
    private Task task;
    private Task errorTask;
    private Executor executor;
    private Callback next;
    
    public static <X> Promise<X> create(Task<Void, X> task, Executor<Void, X> executor) {
        Promise<X> p = new Promise<X>();
        
        p.setDispatch(new Runnable() {
            @Override
            public void run() {
                executor.execute(null, task, p.getCallback());
            }
            
        });
        
        return p;
    }
    
    public void execute() {
        dispatch.run();
    }
    
    public <Y> Promise<Y> then(Task<X, Y> task, Executor<X, Y> executor) {
        Promise<Y> p = new Promise<Y>();
        
        p.setDispatch(this.getDispatch());
        this.task = task;
        this.executor = executor;
        this.next = p.getCallback();
        return p;
    }
    
    public Promise<X> handle(Task<Object, X> task, Executor<Object, X> executor) {
        Promise<X> p = new Promise<X>();
        p.setDispatch(this.getDispatch());
        this.errorTask = task;
        this.executor = executor;
        this.next = p.getCallback();
        return p;
    }
    
    private Callback<X> getCallback() {
        return new Callback<X>() {
            @Override
            public void success(X out) {
                if (task != null) {
                    executor.execute(out, task, next);
                } else if (next != null) {
                    next.success(out);
                }
            }

            @Override
            public void error(Object obj) {
                if (errorTask != null) {
                    executor.execute(obj, errorTask, next);
                } else if (next != null) {
                    next.error(obj);
                }
            }
            
        };
    }
}
