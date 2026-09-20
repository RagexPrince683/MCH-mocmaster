package mcheli.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class MCH_DismountInputTransformer implements IClassTransformer {

   private static final String TARGET_CLASS = "net.minecraft.util.MovementInputFromOptions";
   private static final String GATE_OWNER = "mcheli/MCH_DismountInputGate";

   @Override
   public byte[] transform(String name, String transformedName, byte[] basicClass) {
      if(!TARGET_CLASS.equals(transformedName)) {
         return basicClass;
      }

      ClassNode classNode = new ClassNode();
      new ClassReader(basicClass).accept(classNode, 0);
      boolean injected = false;

      for(MethodNode method : classNode.methods) {
         if(("updatePlayerMoveState".equals(method.name) || "func_78898_a".equals(method.name))
               && "()V".equals(method.desc)) {
            injected = injectBeforeReturns(method);
         }
      }

      if(!injected) {
         throw new IllegalStateException("MCHeli could not install its narrow vehicle dismount input gate");
      }

      ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
      classNode.accept(writer);
      return writer.toByteArray();
   }

   private boolean injectBeforeReturns(MethodNode method) {
      boolean injected = false;
      for(AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null;
            instruction = instruction.getNext()) {
         if(instruction.getOpcode() == Opcodes.RETURN) {
            InsnList gateCall = new InsnList();
            gateCall.add(new VarInsnNode(Opcodes.ALOAD, 0));
            gateCall.add(new MethodInsnNode(Opcodes.INVOKESTATIC, GATE_OWNER, "filterVehicleSneak",
                  "(Lnet/minecraft/util/MovementInput;)V", false));
            method.instructions.insertBefore(instruction, gateCall);
            injected = true;
         }
      }
      return injected;
   }
}
