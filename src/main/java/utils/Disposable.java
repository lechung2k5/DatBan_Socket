package utils;

/**
 * Disposable interface for UI controllers that need to clean up resources
 * (like RealTimeClient listeners) when they are no longer active.
 */
public interface Disposable {
    void dispose();
}
