package edu.univ.erp.ui.utils;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;

/**
 * Tiny helper for running background tasks and dispatching results to the EDT.
 */
public final class UIHelper {

    private UIHelper() {}

    public static <T> void runAsync(Callable<T> task, Consumer<T> onSuccess, Consumer<Exception> onError) {
        new javax.swing.SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return task.call();
            }

            @Override
            protected void done() {
                try {
                    final T result = get();
                    if (onSuccess != null) {
                        SwingUtilities.invokeLater(() -> onSuccess.accept(result));
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    if (onError != null) onError.accept(ie);
                } catch (java.util.concurrent.ExecutionException ee) {
                    Throwable cause = ee.getCause();
                    if (onError != null) onError.accept(cause instanceof Exception ? (Exception) cause : new Exception(cause));
                } catch (Exception ex) {
                    if (onError != null) onError.accept(ex);
                }
            }
        }.execute();
    }
}
