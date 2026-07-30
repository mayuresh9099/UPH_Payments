package com.company.workflow.entity;

/**
 * Backoff strategy applied between action retries.
 *
 * <ul>
 *   <li>{@link #FIXED} – constant wait between each retry.</li>
 *   <li>{@link #LINEAR} – wait increases linearly: interval * retryCount.</li>
 *   <li>{@link #EXPONENTIAL} – wait doubles on each retry: interval * 2^(retryCount-1).</li>
 * </ul>
 */
public enum BackoffStrategy {
    FIXED,
    LINEAR,
    EXPONENTIAL
}
