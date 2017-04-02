package patdroid.core;

import com.google.common.base.Objects;

import java.util.List;

public class FullMethodSignature {
    /**
     * The return type
     * <p> if the method is a constructor or a static initializer, this will always be void </p>
     */
    public final ClassInfo returnType;
    /**
     * A partial signature without return type
     */
    public final MethodSignature partialSignature;

    public FullMethodSignature(ClassInfo returnType, MethodSignature partialSignature) {
        this.returnType = returnType;
        this.partialSignature = partialSignature;
    }

    public FullMethodSignature(ClassInfo returnType, String name, List<ClassInfo> paramTypes) {
        this.returnType = returnType;
        this.partialSignature = new MethodSignature(name, paramTypes);
    }

    public FullMethodSignature(ClassInfo returnType, String name, ClassInfo... paramTypes) {
        this.returnType = returnType;
        this.partialSignature = MethodSignature.of(name, paramTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(returnType, partialSignature);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FullMethodSignature)) {
            return false;
        }
        FullMethodSignature ms = (FullMethodSignature) o;
        return returnType == ms.returnType && partialSignature.equals(ms.partialSignature);
    }

    @Override
    public String toString() {
        return returnType + partialSignature.toString();
    }

}
