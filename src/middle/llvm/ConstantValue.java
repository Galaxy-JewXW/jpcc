package middle.llvm;

import middle.IrType;
import middle.IrType.FloatType;
import middle.IrType.IntegerType;

public abstract class ConstantValue extends Value {
    public ConstantValue(String name, IrType type) {
        super(name, type);
    }

    public abstract boolean isZero();

    public abstract Object getConstantValue();

    public static class ConstantFloat extends ConstantValue {
        private final float constFloatValue;

        public ConstantFloat(float constFloatValue) {
            super("0x" + Double.doubleToRawLongBits(constFloatValue), FloatType.f32);
            this.constFloatValue = constFloatValue;
        }

        @Override
        public boolean isZero() {
            return constFloatValue == 0.0f;
        }

        @Override
        public Object getConstantValue() {
            return constFloatValue;
        }
    }

    public static class ConstantInt extends ConstantValue {
        private final int constIntValue;

        public ConstantInt(int constIntValue) {
            super(Integer.toString(constIntValue), IntegerType.i32);
            this.constIntValue = constIntValue;
        }

        @Override
        public boolean isZero() {
            return constIntValue == 0;
        }

        @Override
        public Object getConstantValue() {
            return constIntValue;
        }
    }
}
