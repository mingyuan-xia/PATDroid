package patdroid.core;

/**
 * A try-block covers a range of instructions and provides the exception handlers
 * associated with these instructions.
 *
 */
public class TryBlockInfo {
    /**
     * An exception handler starts at a particular instruction index,
     * handling a particular exception type, e.g. IOException.
     * The catch-all handler would have a null exception type
     */
    public static class ExceptionHandler {
        public ClassInfo exceptionType;
        public int handlerInsnIndex;
    }
    /**
     * The range of covered instructions, [startInsnIndex, endInsnIndex)
     */
    public int startInsnIndex, endInsnIndex;
    /**
     * Event handlers, a map from ClassInfo to instruction index
     */
    public ExceptionHandler[] handlers;
}
