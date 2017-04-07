package patdroid.dalvik;

import patdroid.core.MethodInfo;

import java.util.Arrays;

public class Invocation {
    public Invocation(boolean isResolved, MethodInfo target, int[] args) {
        this.isResolved = isResolved;
        this.target = target;
        this.args = args;
    }
    public boolean isResolved;
    public MethodInfo target;
    public int[] args;
    @Override
    public String toString() {
        return "[" + target + ", " + Arrays.toString(args) + (isResolved ? "]" : "<NOT_RESOLVED>]");
    }
}
