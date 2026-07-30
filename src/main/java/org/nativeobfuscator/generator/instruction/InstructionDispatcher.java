package org.nativeobfuscator.generator.instruction;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.HashMap;
import java.util.Map;

public final class InstructionDispatcher {
    private final Map<Integer, InstructionHandler> handlers = new HashMap<>();

    public InstructionDispatcher() {
        register(new ConstantInstructionHandler(),
                Opcodes.NOP, Opcodes.ACONST_NULL,
                Opcodes.ICONST_M1, Opcodes.ICONST_0, Opcodes.ICONST_1,
                Opcodes.ICONST_2, Opcodes.ICONST_3, Opcodes.ICONST_4, Opcodes.ICONST_5,
                Opcodes.LCONST_0, Opcodes.LCONST_1,
                Opcodes.FCONST_0, Opcodes.FCONST_1, Opcodes.FCONST_2,
                Opcodes.DCONST_0, Opcodes.DCONST_1, Opcodes.BIPUSH, Opcodes.SIPUSH);
        register(new LocalInstructionHandler(),
                Opcodes.ILOAD, Opcodes.LLOAD, Opcodes.FLOAD, Opcodes.DLOAD, Opcodes.ALOAD,
                Opcodes.ISTORE, Opcodes.LSTORE, Opcodes.FSTORE, Opcodes.DSTORE, Opcodes.ASTORE,
                Opcodes.IINC);
        register(new ConversionInstructionHandler(),
                Opcodes.I2L, Opcodes.I2F, Opcodes.I2D, Opcodes.L2I, Opcodes.L2F,
                Opcodes.L2D, Opcodes.F2I, Opcodes.F2L, Opcodes.F2D,
                Opcodes.D2I, Opcodes.D2L, Opcodes.D2F,
                Opcodes.I2B, Opcodes.I2C, Opcodes.I2S);
        register(new ArithmeticInstructionHandler(),
                Opcodes.IADD, Opcodes.LADD, Opcodes.FADD, Opcodes.DADD,
                Opcodes.ISUB, Opcodes.LSUB, Opcodes.FSUB, Opcodes.DSUB,
                Opcodes.IMUL, Opcodes.LMUL, Opcodes.FMUL, Opcodes.DMUL,
                Opcodes.IDIV, Opcodes.LDIV, Opcodes.FDIV, Opcodes.DDIV,
                Opcodes.IREM, Opcodes.LREM, Opcodes.FREM, Opcodes.DREM,
                Opcodes.INEG, Opcodes.LNEG, Opcodes.FNEG, Opcodes.DNEG,
                Opcodes.ISHL, Opcodes.LSHL, Opcodes.ISHR, Opcodes.LSHR,
                Opcodes.IUSHR, Opcodes.LUSHR, Opcodes.IAND, Opcodes.LAND,
                Opcodes.IOR, Opcodes.LOR, Opcodes.IXOR, Opcodes.LXOR,
                Opcodes.LCMP, Opcodes.FCMPL, Opcodes.FCMPG, Opcodes.DCMPL, Opcodes.DCMPG);
        register(new ReturnInstructionHandler(),
                Opcodes.IRETURN, Opcodes.LRETURN, Opcodes.FRETURN,
                Opcodes.DRETURN, Opcodes.ARETURN, Opcodes.RETURN);
        register(new JumpInstructionHandler(),
                Opcodes.IFEQ, Opcodes.IFNE, Opcodes.IFLT, Opcodes.IFGE, Opcodes.IFGT, Opcodes.IFLE,
                Opcodes.IF_ICMPEQ, Opcodes.IF_ICMPNE, Opcodes.IF_ICMPLT,
                Opcodes.IF_ICMPGE, Opcodes.IF_ICMPGT, Opcodes.IF_ICMPLE,
                Opcodes.IF_ACMPEQ, Opcodes.IF_ACMPNE, Opcodes.GOTO,
                Opcodes.IFNULL, Opcodes.IFNONNULL);
        register(new StackInstructionHandler(),
                Opcodes.POP, Opcodes.POP2, Opcodes.DUP, Opcodes.DUP_X1,
                Opcodes.DUP_X2, Opcodes.DUP2, Opcodes.DUP2_X1, Opcodes.DUP2_X2,
                Opcodes.SWAP);
        register(new MonitorInstructionHandler(), Opcodes.MONITORENTER, Opcodes.MONITOREXIT);
        register(new SwitchInstructionHandler(), Opcodes.TABLESWITCH, Opcodes.LOOKUPSWITCH);
        register(new MultiANewArrayHandler(), Opcodes.MULTIANEWARRAY);
    }

    public String generate(AbstractInsnNode instruction, InstructionContext context) {
        InstructionHandler handler = handlers.get(instruction.getOpcode());
        return handler == null ? null : handler.generate(instruction, context);
    }

    public boolean supports(int opcode) {
        return handlers.containsKey(opcode);
    }

    private void register(InstructionHandler handler, int... opcodes) {
        for (int opcode : opcodes) {
            if (handlers.put(opcode, handler) != null) {
                throw new IllegalStateException("Duplicate instruction handler for opcode " + opcode);
            }
        }
    }
}
