package edu.univ.erp.util;

@FunctionalInterface
public interface ThrowingRunnable {
    void run() throws Exception; 
}