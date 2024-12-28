package middle;

import exceptions.SemanticException;
import middle.llvm.ConstantValue;
import middle.llvm.Value;

public class CastType {
    public static Value cast(Value v, IrType targetType) {
        if (!(v.getType().isIntegerType() || v.getType().isFloatType())) {
            throw new SemanticException(v.getType() + " cannot be cast to " + targetType);
        }
        if (!(targetType.isIntegerType() || targetType.isFloatType())) {
            throw new SemanticException(v.getType() + " cannot be cast to " + targetType);
        }
        if (v instanceof ConstantValue constantValue) {
            return castConstantValue(constantValue, targetType);
        }
        throw new SemanticException(v.getType() + " cannot be cast to " + targetType);
    }

    private static ConstantValue castConstantValue(ConstantValue v, IrType targetType) {
        if (v.getType().equals(targetType)) {
            return v;
        }
        if (v.getType().isFloatType()) {
            if (targetType.isI32Type()) {
                return new ConstantValue.ConstantInt((int) ((float) v.getConstantValue()));
            } else if (targetType.isI1Type()) {
                return ConstantValue.ConstantBool.getConstantBool((float) v.getConstantValue());
            }
        }
        throw new SemanticException(v.getType() + " cannot be cast to " + targetType);
    }
}
