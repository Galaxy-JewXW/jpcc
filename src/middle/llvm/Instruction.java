package middle.llvm;

import exceptions.SemanticException;
import middle.IrType;
import middle.IrType.*;

import java.util.ArrayList;
import java.util.stream.Collectors;

public abstract class Instruction extends User {

    private final OperatorType op;
    private BasicBlock parentBlock;

    public Instruction(IrType irType, OperatorType op, BasicBlock block, String name) {
        super(name, irType);
        this.op = op;
        this.parentBlock = block;
        parentBlock.addInstruction(this);
    }

    public Instruction(IrType irType, OperatorType op, BasicBlock block) {
        super(IrData.getVarName(), irType);
        this.op = op;
        this.parentBlock = block;
        parentBlock.addInstruction(this);
    }

    public enum OperatorType {
        ADD("add"), // +
        SUB("sub"), // -
        MUL("mul"), // *
        SDIV("sdiv"), // /
        SREM("srem"), // %
        ICMP_EQ("icmp eq"), // ==
        ICMP_NE("icmp ne"), // !=
        ICMP_SGT("icmp sgt"), // >
        ICMP_SGE("icmp sge"), // >=
        ICMP_SLT("icmp slt"), // <
        ICMP_SLE("icmp sle"), // <=
        ZEXT("zext"),
        TRUNC("trunc"),
        CALL("call"),
        // 这里把io指令作为call的一种特例提出来看待
        IO("io"),
        ALLOC("alloca"),
        STORE("store"),
        LOAD("load"),
        GEP("getelementptr"),
        PHI("phi"),
        BR("br"),
        RET("ret"),
        MOVE("move");

        private final String name;

        OperatorType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static class Alloca extends Instruction {
        // alloc指令在栈上分配一个地址空间，返回值是一个指针
        private final IrType targetType;

        public Alloca(IrType targetType, BasicBlock parentBlock) {
            super(new IrType.PointerType(targetType), OperatorType.ALLOC, parentBlock);
            this.targetType = targetType;
        }

        public IrType getTargetType() {
            return targetType;
        }

        @Override
        public String toString() {
            return getName() + " = alloca " + targetType;
        }
    }

    public static class Store extends Instruction {
        public Store(Value addr, Value value, BasicBlock parentBlock) {
            super(IrType.VoidType.VOID, OperatorType.STORE, parentBlock, "");
            addOperand(addr);
            addOperand(value);
        }

        public Value getAddr() {
            return getOperands().get(0);
        }

        public Value getValue() {
            return getOperands().get(1);
        }

        @Override
        public String toString() {
            return "store "
                    + getValue().getType() + " "
                    + getValue().getName() + ", " +
                    getAddr().getType() + " "
                    + getAddr().getName();
        }
    }

    public static class Load extends Instruction {
        public Load(Value addr, BasicBlock parentBlock) {
            super(((IrType.PointerType) addr.getType()).getTargetType(), OperatorType.LOAD, parentBlock);
            addOperand(addr);
        }

        public Value getAddr() {
            return getOperands().get(0);
        }

        @Override
        public String toString() {
            return getName() + " = load " + getType() + ", "
                    + getAddr().getType() + " " + getAddr().getName();
        }
    }

    public static class GetElementPtr extends Instruction {
        public GetElementPtr(Value addr, Value index, BasicBlock parentBlock) {
            super(calcType(addr), OperatorType.GEP, parentBlock);
            addOperand(addr);
            addOperand(index);
        }

        public static IrType calcType(Value addr) {
            IrType type = addr.getType();
            if (!(type instanceof IrType.PointerType pointerType)) {
                throw new SemanticException("First operand must be a pointer type");
            }
            IrType targetType = pointerType.getTargetType();
            if (targetType instanceof IrType.ArrayType arrayType) {
                return new IrType.PointerType(arrayType.getElementType());
            } else if (targetType.isI32Type() || targetType.isFloatType()) {
                return new IrType.PointerType(targetType);
            }
            throw new SemanticException("First operand must be a pointer type");
        }

        public Value getAddr() {
            return getOperands().get(0);
        }

        public Value getIndex() {
            return getOperands().get(1);
        }

        @Override
        public String toString() {
            Value addr = getAddr();
            IrType.PointerType pointerType = (IrType.PointerType) addr.getType();
            IrType targetType = pointerType.getTargetType();

            StringBuilder sb = new StringBuilder(getName())
                    .append(" = getelementptr inbounds ")
                    .append(targetType)
                    .append(", ")
                    .append(pointerType)
                    .append(" ")
                    .append(addr.getName());
            if (targetType instanceof IrType.ArrayType) {
                sb.append(", i32 0, i32 ");
            } else {
                sb.append(", i32 ");
            }
            sb.append(getIndex().getName());
            return sb.toString();
        }
    }

    public static class Call extends Instruction {
        private Call(Function calledFunction, ArrayList<Value> parameters, BasicBlock parentBlock) {
            super(calledFunction.getReturnType(), OperatorType.CALL, parentBlock, "");
            addOperand(calledFunction);
            for (Value param : parameters) {
                addOperand(param);
            }
            if (!calledFunction.getReturnType().equals(VoidType.VOID)) {
                setName(IrData.getVarName());
            }
        }

        public Function getCalledFunction() {
            return (Function) getOperands().get(0);
        }

        public ArrayList<Value> getParameters() {
            return new ArrayList<>(getOperands().subList(1, getOperands().size()));
        }

        public String getCallee() {
            String paramInfo = getParameters().stream()
                    .map(param -> param.getType() + " " + param.getName())
                    .collect(Collectors.joining(", "));
            return String.format("%s(%s)", getCalledFunction().getName(), paramInfo);
        }

        @Override
        public String toString() {
            String functionCall = getCallee();
            return getType().equals(VoidType.VOID)
                    ? String.format("call void %s", functionCall)
                    : String.format("%s = call %s %s", getName(), getType(), functionCall);
        }
    }
}
