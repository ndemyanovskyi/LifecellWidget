package ndemyanovskyi.lifecellwidget.backend.lifecell.api.util;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import ndemyanovskyi.lifecellwidget.backend.lifecell.api.LifecellException.ResponseType;
import ndemyanovskyi.lifecellwidget.backend.lifecell.api.LifecellException;

public class Async<R> implements Runnable {

    private final LoadAction<R> loadAction;
    private final AtomicBoolean started = new AtomicBoolean(false);

    private volatile Thread thread;
    private volatile FailureAction failureAction;
    private volatile ExceptionAction exceptionAction;
    private volatile SuccessAction<R> successAction;

    public Async(LoadAction<R> loadAction) {
        this.loadAction = loadAction;
    }

    public void load() {
        if(started.get()) {
            throw new IllegalStateException(
                    "Async already has been started.");
        }
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        if(exceptionAction == null) {
            throw new IllegalStateException(
                    "Async can`t be started without ExceptionAction.");
        }
        try {
            R result = loadAction.onLoad();
            if(successAction != null) {
                successAction.onSuccess(result);
            }
        } catch (LifecellException e) {
            if(failureAction != null) {
                failureAction.onFailure(e.getResponseType(), e.getResponseCode());
            } else {
                exceptionAction.onException(e);
            }
        } catch (IOException e) {
            exceptionAction.onException(e);
        }
        thread = null;
    }

    public boolean isStarted() {
        return started.get();
    }

    public Async<R> onFailure(FailureAction failureAction) {
        if(started.get()) {
            throw new IllegalStateException(
                    "FailureAction can`t be set after Async has been started.");
        }
        this.failureAction = failureAction;
        return this;
    }

    public Async<R> onException(ExceptionAction exceptionAction) {
        if(started.get()) {
            throw new IllegalStateException(
                    "ExceptionAction can`t be set after Async has been started.");
        }
        this.exceptionAction = exceptionAction;
        return this;
    }

    public Async<R> onSuccess(SuccessAction<R> successAction) {
        if(started.get()) {
            throw new IllegalStateException(
                    "SuccessAction can`t be set after Async has been started.");
        }
        this.successAction = successAction;
        return this;
    }

    public interface LoadAction<R> {
        R onLoad() throws LifecellException, IOException;
    }

    public interface SuccessAction<R> {
        void onSuccess(R result);
    }

    public interface FailureAction {
        void onFailure(ResponseType responseType, long responseCode);
    }

    public interface ExceptionAction {
        void onException(Exception exception);
    }
}
