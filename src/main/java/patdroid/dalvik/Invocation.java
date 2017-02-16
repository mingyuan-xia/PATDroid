package patdroid.dalvik;

import patdroid.core.MethodInfo;

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
        return target.toString() + ", " + args.toString() + (isResolved ? "" : "<NOT_RESOLVED>");
    }
}
