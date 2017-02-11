package patdroid.core;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Signature of a method.
 *
 * A signature contains the name and parameter types (no type, access flags and return type).
 */
public class MethodSignature {
    public final String name;
    public final ImmutableList<ClassInfo> paramTypes;

    public MethodSignature(String name, List<ClassInfo> paramTypes) {
        this.name = name;
        this.paramTypes = ImmutableList.copyOf(paramTypes);
    }

    public static MethodSignature of(String name, ClassInfo... paramTypes) {
        return new MethodSignature(name, ImmutableList.copyOf(paramTypes));
    }

    public static MethodSignature of(Scope scope, String name, Class<?>... paramTypes) {
        return new MethodSignature(name, scope.findOrCreateClasses(paramTypes));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, paramTypes);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MethodSignature)) {
            return false;
        }
        MethodSignature ms = (MethodSignature) o;
        return name.equals(ms.name) && paramTypes.equals(ms.paramTypes);
    }

    @Override
    public String toString() {
        return name + paramTypes;
    }
}
