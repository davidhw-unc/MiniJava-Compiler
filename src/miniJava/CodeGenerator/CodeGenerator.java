package miniJava.CodeGenerator;

import static miniJava.SyntacticAnalyzer.Token.Kind.*;

import java.util.ArrayDeque;
import java.util.Queue;

import mJAM.Machine;
import mJAM.Machine.Op;
import mJAM.Machine.Prim;
import mJAM.Machine.Reg;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ContextualAnalyzer.ContextualAnalyzer;
import miniJava.SyntacticAnalyzer.Token.Kind;

public class CodeGenerator implements Visitor<Object, Object> {

    // ============================================================================
    // 
    // Public members
    // 
    // ============================================================================

    public static void generateCode(AST ast) {
        new CodeGenerator(ast);
    }

    // ============================================================================
    // 
    // Private methods & classes
    // 
    // ============================================================================

    /**
     * Private constructor
     * 
     * @param ast AST root node (must be a Package!)
     */
    private CodeGenerator(AST ast) {
        if (!(ast instanceof Package)) {
            throw new IllegalArgumentException("ast must have a Package as its root");
        }

        patchesToDo = new ArrayDeque<>();
        curStaticCount = 0;
        ifLayerCount = 0;
        loopLayerCount = 0;
        hasCalledPrintln = false;

        ast.visit(this, null);
    }

    // Function that handles generating method calls for both CallStmt and CallExpr
    private void emitCall(MethodCaller caller) {
        // Get the method's declaration
        MethodDecl method = (MethodDecl) caller.getMethodRef().getId().getDecl();

        if (method == printlnMethod) {
            hasCalledPrintln = true;
        }

        // Put parameter values on the stack
        for (Expression argExpr : caller.getArgList()) {
            forcePushResult((Integer) argExpr.visit(this, true), true);
        }

        // Visit the reference- if this method isn't static, the instance address will go on stack
        caller.getMethodRef().visit(this, null);

        // If the method's addr isn't yet determined, record a PatchNote
        if (method.data == Integer.MIN_VALUE) {
            patchesToDo.add(new PatchNote(Machine.nextInstrAddr(), method));
        }

        // Call the method
        Machine.emit(method.isStatic ? Op.CALL : Op.CALLI, Reg.CB, method.data);
    }

    private class PatchNote {
        int addr; // The address of the instruction that needs to be patched
        MethodDecl decl; // The Declaration that's being accessed

        PatchNote(int addr, MethodDecl decl) {
            this.addr = addr;
            this.decl = decl;
        }
    }

    private void enterIf() {
        ++ifLayerCount;
    }

    private void exitIf() {
        --ifLayerCount;
        if (ifLayerCount < 0) {
            throw new IllegalStateException(
                    "called exitIf() without prior matching enterIf() call");
        }
    }

    private boolean inIf() {
        return ifLayerCount > 0;
    }

    private void enterLoop() {
        ++loopLayerCount;
    }

    private void exitLoop() {
        --loopLayerCount;
        if (loopLayerCount < 0) {
            throw new IllegalStateException(
                    "called exitLoop() without prior matching enterLoop() call");
        }
    }

    private boolean inLoop() {
        return loopLayerCount > 0;
    }

    // ============================================================================
    // 
    // Private member variables (Let's be smart and do it this way this time...)
    // 
    // ============================================================================

    private Queue<PatchNote> patchesToDo;
    private int curStaticCount;
    private int ifLayerCount; // Don't read directly, use inIf()
    private int loopLayerCount; // Don't read directly, use inLoop()
    private int curMethodArgCount;
    private boolean hasCalledPrintln;
    private MethodDecl printlnMethod;

    // Used in the conditional portion of while loops, if statements, and ternary expressions when
    // the top-level operator can short-circuit
    // TODO needed?
    private int firstJumpOpAddr;

    // This is only changed when code is actually emitted (this includes forcePushResult)
    private Kind lastExprWasSSBinary = null;

    ///////////////////////////////////////////////////////////////////////////////
    //
    // PACKAGE
    //
    ///////////////////////////////////////////////////////////////////////////////

    @Override
    public Object visitPackage(Package prog, Object arg) {
        // Initialize the code generator
        Machine.initCodeGen();
        System.out.println("Beginning code generation...");

        // Save a reference to the dummy println method
        printlnMethod = prog.printlnMethod;

        // Calculate data value for each class and field declaration (pass 1)
        for (ClassDecl c : prog.classDeclList) {
            c.visit(this, 1);
        }
        // After this, curStaticOffset will hold the number of static members in the whole program,
        // allowing us to push that many entries onto the stack before calling main

        // Before compiling any of the user's code, we need to emit code that calls main
        // This will require recording our first PatchNote

        // Make space below the stack for all the static fields (if any are present)
        if (curStaticCount > 0) {
            Machine.emit(Op.PUSH, curStaticCount);
        }
        // These are all initialized to 0 since that's how real Java initializes array elements

        // Create empty args array
        Machine.emit(Op.LOADL, 0);
        Machine.emit(Prim.newarr);
        // Record patch
        patchesToDo.add(new PatchNote(Machine.nextInstrAddr(), prog.mainMethod));
        // Call main
        Machine.emit(Op.CALL, Reg.CB, -1);
        // Halt execution
        Machine.emit(Op.HALT, 0, Reg.ZR, 0);

        // Generate code for each MethodDecl (pass 2)
        for (ClassDecl c : prog.classDeclList) {
            c.visit(this, 2);
        }

        // Create the println method's code if it has been used
        if (hasCalledPrintln) {
            // Record the method's code address
            prog.printlnMethod.data = Machine.nextInstrAddr();
            // Load the number being printed
            Machine.emit(Op.LOAD, Reg.LB, -1);
            // Print
            Machine.emit(Prim.putintnl);
            // Return nothing
            Machine.emit(Op.RETURN, 0, Reg.ZR, 1);
        }

        // Perform necessary patching
        for (PatchNote patch : patchesToDo) {
            if (patch.decl.data == Integer.MIN_VALUE) {
                throw new IllegalStateException("Method declaration never had its data set");
            }
            Machine.patch(patch.addr, patch.decl.data);
        }

        return null;
    }

    ///////////////////////////////////////////////////////////////////////////////
    //
    // DECLARATIONS
    //
    ///////////////////////////////////////////////////////////////////////////////

    @Override
    public Object visitClassDecl(ClassDecl cd, Object arg) {
        // Pass # is indicated by arg

        // Pass 1
        if ((int) arg == 1) {
            // Visit each field in the class to record their offsets
            int instanceFieldIndex = 0;
            for (FieldDecl field : cd.fieldDeclList) {
                instanceFieldIndex = (int) field.visit(this, instanceFieldIndex);
            }

            // Set data to the number of nonstatic fields in the class
            cd.data = instanceFieldIndex;

            return null;
        }

        // Pass 2
        if ((int) arg == 2) {
            // Visit each MethodDecl for code generation
            for (MethodDecl method : cd.methodDeclList) {
                method.visit(this, null);
            }

            return null;
        }

        throw new IllegalStateException("arg should be used to indicate the current pass (1 or 2)");
    }

    @Override
    public Object visitFieldDecl(FieldDecl fd, Object arg) {
        // Current field index is passed as arg- only used if this field isn't static
        // This method is expected to return the updated index

        // If the field is static, set offset according to the global static offset
        int offset = (int) arg;
        if (fd.isStatic) {
            fd.data = curStaticCount++;
        } else {
            // Otherwise, set data according to the instance variable offset and increment the
            // provided offset
            fd.data = offset++;
        }

        // Return the instance variable offset that should be used by the next field
        return offset;
    }

    @Override
    public Object visitMethodDecl(MethodDecl md, Object arg) {
        // Record the starting code address for this method in its data field
        md.data = Machine.nextInstrAddr();

        // Record the number of args for this method - used by ReturnStmts
        curMethodArgCount = md.parameterDeclList.size();

        // Visit each parameter to set their offsets
        int parameterOffset = -md.parameterDeclList.size();
        for (ParameterDecl param : md.parameterDeclList) {
            param.visit(this, parameterOffset++);
        }

        // Visit each statement to generate code
        // Each statement should accept the current offset and return the new offset
        int localsOffset = 3; // Starts at 3 to account for the activation record on the stack
        for (Statement stmt : md.statementList) {
            localsOffset = (int) stmt.visit(this, localsOffset);
        }

        // Add empty return to end of void methods if not already present
        if (md.getType().typeKind == TypeKind.VOID && (md.statementList.size() == 0
                || !(md.statementList.get(md.statementList.size() - 1) instanceof ReturnStmt))) {
            Machine.emit(Op.RETURN, 0, Reg.ZR, curMethodArgCount);
        }

        return null;
    }

    @Override
    public Object visitParameterDecl(ParameterDecl pd, Object arg) {
        // Record this parameter's offset from LB in data
        pd.data = (int) arg;

        return null;
    }

    @Override
    public Object visitVarDecl(VarDecl vd, Object arg) {
        // Record this local variable's offset from LB in data
        vd.data = (int) arg;

        return null;
    }

    ///////////////////////////////////////////////////////////////////////////////
    //
    // TYPES
    //
    ///////////////////////////////////////////////////////////////////////////////

    @Override
    public Object visitBaseType(BaseType bt, Object arg) {
        // Throw error if this method is somehow called
        throw new UnsupportedOperationException(
                "visitBaseType() should never be called in CodeGenerator");
    }

    @Override
    public Object visitClassType(ClassType ct, Object arg) {
        // Throw error if this method is somehow called
        throw new UnsupportedOperationException(
                "visitClassType() should never be called in CodeGenerator");
    }

    @Override
    public Object visitArrayType(ArrayType at, Object arg) {
        // Throw error if this method is somehow called
        throw new UnsupportedOperationException(
                "visitArrayType() should never be called in CodeGenerator");
    }

    ///////////////////////////////////////////////////////////////////////////////
    //
    // STATEMENTS
    //
    ///////////////////////////////////////////////////////////////////////////////

    // arg & return are used to track the number of local variables & parameters on the stack

    @Override
    public Object visitBlockStmt(BlockStmt bs, Object arg) {
        // Visit each statement in the block
        int localsOffset = (int) arg;
        for (Statement stmt : bs.sl) {
            localsOffset = (int) stmt.visit(this, localsOffset);
        }

        // Pop off stack entries equal to localsOffset - arg (if its > 0)
        if (localsOffset - (int) arg > 0) {
            Machine.emit(Op.POP, localsOffset - (int) arg);
        }

        // Return the *original* stack variable count
        return arg;
    }

    @Override
    public Object visitVarDeclStmt(VarDeclStmt vds, Object arg) {
        int curLocalOffset = (int) arg;

        // Record this variable's offset from LB in its data field
        vds.varDecl.data = curLocalOffset;

        // Visit initExp, record its value in the varDecl if known, and store it on top of the stack
        Integer val = forcePushResult((Integer) vds.initExp.visit(this, true), true);
        vds.varDecl.setValue(val);

        /*
         * Note: In theory, we could entirely omit storing a local var on the stack if its value is
         * always known at compile time, but that would require two passes through each method
         * during the compilation process
         * 
         * We could also theoretically reuse stack locations if a previous local isn't referenced 
         * again -- this could be in the same initial pass as above
         */

        // Increment the current number of stored locals when we return
        return curLocalOffset + 1;
    }

    @Override
    public Object visitAssignStmt(AssignStmt as, Object arg) {
        // Visit the ref in WRITE mode
        // This will return an object containing the register we need to operate relative to and
        // the offset we should use
        RefVisitReturn result = (RefVisitReturn) as.ref.visit(this, RefVisitMode.WRITE);

        // Put the new value on the stack (even if it's known at compile time, we don't have a way
        // to store it without first putting it on the stack)
        Integer newVal = forcePushResult((Integer) as.valExpr.visit(this, true), true);

        // If the register is null, this is a member field of another object, and the object's addr
        // and the field's offset were already put on the stack
        // All we need to do is call the fieldupd primitive
        if (result.reg == null) {
            Machine.emit(Prim.fieldupd);
        } else {

            // Otherwise, we just need to emit the appropriate STORE instruction
            Machine.emit(Op.STORE, result.reg, result.offset);

            // If this is reassigning a VarDecl & we aren't in a loop or an if statement, update the
            // VarDecl's value field
            if (result.reg == Reg.LB && result.offset > 0) {
                ((VarDecl) as.ref.getId().getDecl()).setValue(inIf() || inLoop() ? null : newVal);
            }
        }

        return arg;
    }

    @Override
    public Object visitIxAssignStmt(IxAssignStmt ias, Object arg) {
        // Visit the ref in READ mode
        // This will emit instructions that put the array's address on the stack
        ias.ref.visit(this, RefVisitMode.READ);

        // Visit ixExpr and force its value (the array index we're writing to) onto the stack
        forcePushResult((Integer) ias.ixExpr.visit(this, true), true);

        // Visit valExp and force its value (the value being written into the array) onto the stack
        forcePushResult((Integer) ias.valExp.visit(this, true), true);

        // Call the arrayupd primitive to pop the addr, index, and val off the stack and update the
        // appropriate array entry's value
        Machine.emit(Prim.arrayupd);

        return arg;
    }

    @Override
    public Object visitCallStmt(CallStmt cs, Object arg) {
        // Delegate to EmitCall
        emitCall(cs);

        // Pop the returned value off the stack if present
        if (cs.getMethodRef().getType().typeKind != TypeKind.VOID) {
            Machine.emit(Op.POP, 1);
        }

        return arg;
    }

    @Override
    public Object visitReturnStmt(ReturnStmt rs, Object arg) {
        if (rs.returnExpr != null) {
            // If a value is being returned, put it on the stack
            forcePushResult((Integer) rs.returnExpr.visit(this, true), true);

            // Emit the RETURN instruction with n=1
            Machine.emit(Op.RETURN, 1, Reg.ZR, curMethodArgCount);
        } else {

            // If no value is being returned, simply emit the RETURN instruction with n=0
            Machine.emit(Op.RETURN, 0, Reg.ZR, curMethodArgCount);
        }

        return arg;
    }

    @Override
    public Object visitIfStmt(IfStmt is, Object arg) {
        // TODO add optimization for == or != with one known operand

        // Mark that we are entering an if statement
        enterIf();

        // TODO If condVal is unknown and has && or || as its top-level operator, we can optimize

        // TODO merge in bb564ea now that I've just removed || and && being known based on their right value?

        // Visit the conditional expression- if it's not known at compile time, the value will be
        // put on the stack
        Integer condVal = (Integer) is.condExpr.visit(this, true);

        // If condVal is known, we only ever have to run one of the two branches
        if (condVal != null) {

            if (condVal == Machine.trueRep) {
                // If condVal is known to be true, simply execute the thenStmt
                // We know this isn't a solitary declaration, so we don't need to check the return
                is.thenStmt.visit(this, arg);
            } else if (is.elseStmt != null) {
                // Otherwise, simply visit the elseStmt if it's present
                // We know this isn't a solitary declaration, so we don't need to check the return
                is.elseStmt.visit(this, arg);
            }
        } else {
            // If condVal is not known, it is on the stack and we must branch according to its value

            int jumpSkipToElseAddr = 0;
            int bonusJumpAddrForAND = 0;
            boolean wasAND = false;

            // First, handle jump optimizations for when the top level expr short-circuits
            if (lastExprWasSSBinary != null) {
                // Remove the last two emitted instructions
                Machine.CT -= 2;

                // Emit a new JUMPIF - this will be patched to go to after the then block
                jumpSkipToElseAddr = Machine.nextInstrAddr();
                Machine.emit(Op.JUMPIF, Machine.falseRep, Reg.CB, -1);

                // Note: This is all we need to do for OR- the first jump is actually still fine, it
                // now points to the start of the then block

                if (lastExprWasSSBinary == AND) {
                    // Record the address of the first jump, as it also needs to skip then
                    wasAND = true;
                    bonusJumpAddrForAND = firstJumpOpAddr;
                }

            } else {
                // If condExpr doesn't short-circuit, start by emitting a JUMPIF instruction that
                // skips the thenStmt if false
                jumpSkipToElseAddr = Machine.nextInstrAddr();
                Machine.emit(Op.JUMPIF, Machine.falseRep, Reg.CB, -1);
            }

            // Emit the code for thenStmt
            // We know this isn't a solitary declaration, so we don't need to check the return
            is.thenStmt.visit(this, arg);

            // If there's an elseStmt, emit an instruction to skip it at the end of the thenStmt
            int jumpSkipOverElseAddr = Machine.nextInstrAddr();
            if (is.elseStmt != null) {
                Machine.emit(Op.JUMP, Reg.CB, -1);
            }

            // Patch the first jump so that it goes to the else block (or the next instruction)
            Machine.patch(jumpSkipToElseAddr, Machine.nextInstrAddr());

            // If the top-level expr was AND, we need to patch a second jump as well
            if (wasAND) {
                Machine.patch(bonusJumpAddrForAND, Machine.nextInstrAddr());
            }

            // If there's an elseStmt, emit its instructions and patch the second jump
            if (is.elseStmt != null) {
                is.elseStmt.visit(this, arg);
                Machine.patch(jumpSkipOverElseAddr, Machine.nextInstrAddr());
            }
        }

        // Mark that we are leaving an if statement
        exitIf();

        return arg;
    }

    @Override
    public Object visitLoopStmt(LoopStmt ls, Object arg) {
        int initialCT = Machine.nextInstrAddr();
        int newLocalCount = (int) arg;

        // Emit instructions for the initializer
        if (ls.getInitList() != null) {
            for (Statement s : ls.getInitList()) {
                s.visit(this, arg);
            }
        } else if (ls.getInitDecl() != null) {
            ls.getInitDecl().visit(this, newLocalCount++);
        }

        // Evaluate the conditional without emitting any instructions- if we know its false, we
        // don't have to emit anything else here; if it's unknown, we need to jump to the
        // conditional for an initial evaluation before the body runs
        Integer initialCondVal = (Integer) ls.condExpr.visit(this, false);
        int jumpToCondAddr = -1;
        if (initialCondVal == null) {
            jumpToCondAddr = Machine.nextInstrAddr();
            Machine.emit(Op.JUMP, Reg.CB, -1);
        }
        if (initialCondVal == null || initialCondVal == Machine.trueRep) {
            // Mark that we are entering the repeated portion of a while loop
            enterLoop();

            // Record the current code addr and emit code for the body (which includes the update)
            int bodyStartAddr = Machine.nextInstrAddr();
            ls.body.visit(this, newLocalCount);

            // Evaluate the conditional, leave it on the stack, then JUMPIF back to the body
            // If the conditional is known to be true here, just JUMP back instead- this loop will
            // repeat indefinitely
            // If the conditional is (somehow) now known to be false, there's no need to even emit a
            // jump instruction
            if (jumpToCondAddr != -1) {
                Machine.patch(jumpToCondAddr, Machine.nextInstrAddr());
            }
            Integer condVal = (Integer) ls.condExpr.visit(this, true);
            // If the conditional is unknown and short-circuits, we can optimize 
            if (lastExprWasSSBinary != null && condVal == null) {
                // Remove the last two emitted instructions
                Machine.CT -= 2;

                // Patch the middle jump to go back to the start of the body for OR
                if (lastExprWasSSBinary == OR) {
                    Machine.patch(firstJumpOpAddr, bodyStartAddr);
                }

                // Add a JUMPIF that takes us back to the start of the body if the final value
                // comes out to be true
                Machine.emit(Op.JUMPIF, Machine.trueRep, Reg.CB, bodyStartAddr);
            } else if (condVal != null && condVal == Machine.trueRep) {
                Machine.emit(Op.JUMP, Reg.CB, bodyStartAddr);
            } else if (condVal == null) {
                Machine.emit(Op.JUMPIF, Machine.trueRep, Reg.CB, bodyStartAddr);
            }
        }

        // If initialization used initDecl, that variable needs to be POPped off the stack
        if (ls.getInitDecl() != null) {
            Machine.emit(Op.POP, 1);

        }

        // If the conditional was known to always be false and initList WASN'T used (as it could
        // have side effects) we can just remove all instructions emitted for this LoopStmt
        if (initialCondVal != null && initialCondVal == Machine.falseRep
                && ls.getInitList() == null) {
            Machine.CT = initialCT;
        }

        // Mark that we are leaving a while loop
        exitLoop();

        return arg;
    }

    ///////////////////////////////////////////////////////////////////////////////
    //
    // EXPRESSIONS
    //
    ///////////////////////////////////////////////////////////////////////////////

    // Note: arg == true is required for instructions to be emitted

    private Integer forcePushResult(Integer res, Object shouldEmit) {
        if (res != null && (boolean) shouldEmit) {
            // If something is being written here, we didn't just process a compile-time-unknown
            // short-circuiting binary operator
            lastExprWasSSBinary = null;

            // If the returned value isn't null, nothing has been emitted yet
            // Therefore, we need to LOADL that value onto the stack
            Machine.emit(Op.LOADL, res);
        }
        return res;
    }

    // Return value: null if val isn't known at compile time, Integer otherwise
    // Machine.trueRep and Machine.falseRep are used to represent true and false as Integers

    @Override
    public Object visitUnaryExpr(UnaryExpr ue, Object arg) {
        // Visit the operand
        // If the value is known at compile time, it will be returned
        // Otherwise, instructions will be emitted that leave the value on top of the stack
        Integer operandVal = (Integer) ue.operandExpr.visit(this, arg);

        // If the operand's value is known, we can find the value of this expr
        if (operandVal != null) {
            if (ue.operator.kind == NOT) {
                return operandVal == Machine.trueRep ? Machine.falseRep : Machine.trueRep;
            }
            return -operandVal;
        }

        // If not known, emit instructions for runtime calculation
        if ((Boolean) arg) ue.operator.visit(this, null);

        return null;
    }

    private static Integer boolToInt(boolean b) {
        return b ? Machine.trueRep : Machine.falseRep;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr be, Object arg) {
        // Visit left & right, but just to get their known values if they can be evaluated at
        // compile time.
        Integer left = (Integer) be.leftExpr.visit(this, false); // Not emitting!
        Integer right = (Integer) be.rightExpr.visit(this, false); // Not emitting!

        if (left != null && right != null) {
            // If both operands have known values, we can calculate this expression's result and
            // pass it up the chain

            // Calculate the value w/o visiting the operator (visiting is for unknown operands) 
            switch (be.operator.kind) {
                case OR:
                    return boolToInt(left == Machine.trueRep || right == Machine.trueRep);
                case AND:
                    return boolToInt(left == Machine.trueRep && right == Machine.trueRep);
                case LESS_EQUAL:
                    return boolToInt(left <= right);
                case LESS_THAN:
                    return boolToInt(left < right);
                case GREATER_THAN:
                    return boolToInt(left > right);
                case GREATER_EQUAL:
                    return boolToInt(left >= right);
                case PLUS:
                    return left + right;
                case MINUS:
                    return left - right;
                case MULTIPLY:
                    return left * right;
                case DIVIDE:
                    if (right == 0) {
                        // If right is 0, leave calculation (and error) to runtime

                        // Force left onto the stack
                        forcePushResult(left, arg);

                        // Visit right and force it onto the stack
                        forcePushResult((Integer) be.rightExpr.visit(this, arg), arg);

                        // Visit operator to emit the calculation instruction
                        if ((Boolean) arg) be.operator.visit(this, null);

                        return null;
                    } else {
                        return left / right;
                    }
                case MODULUS:
                    return left % right;
                case EQUAL_TO:
                    return boolToInt(left == right);
                case NOT_EQUAL:
                    return boolToInt(left != right);
                default:
                    throw new IllegalStateException("It shouldn't be possible to reach this line");
            }
        }

        // If the value isn't known at compile time, we need to emit code to perform the
        // calculation at runtime (this is when the value fully isn't known- for
        // short-circuiting operators, this means both operands are unknown, for other
        // operators it doesn't matter if one or both are unknown)

        // Visit the left operand
        be.leftExpr.visit(this, arg);

        // Handle the short-circuiting || operator
        if (be.operator.kind == OR) {
            if (left == null) {
                // left isn't known, so it will already be on the stack

                // We need to emit code that decides whether to visit right based on left's value

                // Emit a JUMPIF instruction that skips the evaluation of right if left is true,
                // but first record the addr of that instruction so we can patch in the addr it's
                // jumping to
                int tempFirstJumpOpAddr = Machine.nextInstrAddr();
                if ((Boolean) arg) Machine.emit(Op.JUMPIF, Machine.trueRep, Reg.CB, -1);

                // Visit right so its code can be generated
                forcePushResult((Integer) be.rightExpr.visit(this, arg), arg);

                // If right wasn't evaluated, we'll need to push a true onto the stack. If right WAS
                // evaluated, we need to skip that instruction.
                if ((Boolean) arg) Machine.emit(Op.JUMP, Reg.CB, Machine.nextInstrAddr() + 2);
                if ((Boolean) arg) Machine.patch(tempFirstJumpOpAddr, Machine.nextInstrAddr());
                if ((Boolean) arg) Machine.emit(Op.LOADL, Machine.trueRep);

                // In this case, and ONLY this case, we can indicate that the code emitted on the
                // is for the short-circuit evaluation of an OR operator just before we return
                if ((Boolean) arg) firstJumpOpAddr = tempFirstJumpOpAddr;
                if ((Boolean) arg) lastExprWasSSBinary = OR;

                return null;

            } else {
                // If left is known at compile time, then it doesn't have any side effects and we
                // can just select whether to go on and visit right based on left's known value 
                if (left == Machine.trueRep) {
                    return Machine.trueRep;
                }

                // Note: we know right isn't known, because if both left & right were known we'd
                // running the first if statement in this method

                // If left is known to be false, we can visit right and just use right's value
                return be.rightExpr.visit(this, arg);
            }
        }

        // Handle the short-circuiting && operator
        if (be.operator.kind == AND) {
            if (left == null) {
                // left isn't known, so it will already be on the stack

                // We need to emit code that decides whether to visit right based on left's value

                // Emit a JUMPIF instruction that skips the evaluation of right if left is false,
                // but first record the addr of that instruction so we can patch in the addr it's
                // jumping to
                int tempFirstJumpOpAddr = Machine.nextInstrAddr();
                if ((Boolean) arg) Machine.emit(Op.JUMPIF, Machine.falseRep, Reg.CB, -1);

                // Visit right so its code can be generated
                forcePushResult((Integer) be.rightExpr.visit(this, arg), arg);

                // If right wasn't evaluated, we'll need to push a false onto the stack. If right
                // WAS evaluated, we need to skip that instruction.
                if ((Boolean) arg) Machine.emit(Op.JUMP, Reg.CB, Machine.nextInstrAddr() + 2);
                if ((Boolean) arg) Machine.patch(tempFirstJumpOpAddr, Machine.nextInstrAddr());
                if ((Boolean) arg) Machine.emit(Op.LOADL, Machine.falseRep);

                // In this case, and ONLY this case, we can indicate that the code emitted on the
                // is for the short-circuit evaluation of an AND operator just before we return
                if ((Boolean) arg) firstJumpOpAddr = tempFirstJumpOpAddr;
                if ((Boolean) arg) lastExprWasSSBinary = AND;

                return null;

            } else {
                // If left is known at compile time, then it doesn't have any side effects and we
                // can just select whether to go on and visit right based on left's known value 
                if (left == Machine.falseRep) {
                    return Machine.falseRep;
                }

                // Note: we know right isn't known, because if both left & right were known we'd be
                // running the first if statement in this method

                // If left is known to be true, we can visit right and just use right's value
                return be.rightExpr.visit(this, arg);
            }
        }

        // Current status: If left is unknown, it is on the stack. No matter what, right is not yet
        // on the stack. We need both on the stack before dispatching to the operator to emit
        // evaluation instructions.

        // Force left onto the stack
        forcePushResult(left, arg);

        // Visit right and force it onto the stack
        forcePushResult((Integer) be.rightExpr.visit(this, arg), arg);

        // Visit operator to emit the calculation instruction
        if ((Boolean) arg) be.operator.visit(this, null);

        return null;
    }

    @Override
    public Object visitTernaryExpr(TernaryExpr te, Object arg) {
        // Visit the conditional
        // If it's known at compile time, it'll be returned, otherwise instructions will be emitted
        // that leave the value on the stack
        Integer condVal = (Integer) te.leftExpr.visit(this, arg);

        // If condVal is known, we simply have to emit instructions for the indicated expression
        if (condVal != null) {
            forcePushResult((Integer) (condVal == Machine.trueRep ? te.midExpr : te.rightExpr)
                    .visit(this, arg), arg);
            return null;
        }

        // If it's not known, we need to emit code for both paths that can be chosen conditionally
        // The conditional's value is already on the stack

        // Emit JUMPIF to skip midExpr if cond is false (will need to be patched)
        int skipMidExprInstAddr = Machine.nextInstrAddr();
        if ((Boolean) arg) Machine.emit(Op.JUMPIF, Machine.falseRep, Reg.CB, -1);

        // Emit instructions for evaluating midExpr
        forcePushResult((Integer) te.midExpr.visit(this, arg), arg);

        // Emit a jump to skip rightExpr (this will also need to be patched)
        int skipRightExprInstAddr = Machine.nextInstrAddr();
        if ((Boolean) arg) Machine.emit(Op.JUMP, Reg.CB, -1);

        // Patch the first jump so it takes us here
        if ((Boolean) arg) Machine.patch(skipMidExprInstAddr, Machine.nextInstrAddr());

        // Emit instructions for evaluating rightExpr
        forcePushResult((Integer) te.rightExpr.visit(this, arg), arg);

        // Patch the second jump so it takes us here
        if ((Boolean) arg) Machine.patch(skipRightExprInstAddr, Machine.nextInstrAddr());

        // If something is being written here, we didn't just process a compile-time-unknown
        // short-circuiting binary operator
        if ((Boolean) arg) lastExprWasSSBinary = null;

        return null;
    }

    @Override
    public Object visitRefExpr(RefExpr re, Object arg) {
        // Note: If we're in a loop, no locals are considered known

        // If this RefExpr is known (an IdRef that points to a VarDecl with a known value), we can
        // just pass on that known value
        Reference ref = re.ref;
        if (!inLoop() && ref instanceof IdRef && ref.getId().getDecl() instanceof VarDecl
                && ((VarDecl) ref.getId().getDecl()).getValue() != null) {
            return ((VarDecl) re.ref.getId().getDecl()).getValue();
        }

        // Otherwise, visit the reference to emit code that will put its value on the stack
        if ((Boolean) arg) re.ref.visit(this, RefVisitMode.READ);

        // If something is being written here, we didn't just process a compile-time-unknown
        // short-circuiting binary operator
        // Handle this here, rather than in the reference
        if ((Boolean) arg) lastExprWasSSBinary = null;

        // Note: array length members are handled in the QualRef visit method

        return null;
    }

    @Override
    public Object visitIxExpr(IxExpr ie, Object arg) {
        // Get the array's address on the stack
        if ((Boolean) arg) ie.ref.visit(this, RefVisitMode.READ);

        // Get the array index expression on the stack
        forcePushResult((Integer) ie.ixExpr.visit(this, arg), arg);

        // Call the arrayref primitive
        if ((Boolean) arg) Machine.emit(Prim.arrayref);

        // If something is being written here, we didn't just process a compile-time-unknown
        // short-circuiting binary operator
        if ((Boolean) arg) lastExprWasSSBinary = null;

        return null;
    }

    @Override
    public Object visitCallExpr(CallExpr ce, Object arg) {
        // Delegate to EmitCall
        if ((Boolean) arg) emitCall(ce);

        // If something is being written here, we didn't just process a compile-time-unknown
        // short-circuiting binary operator
        // Handle this here, rather than in emitCall
        if ((Boolean) arg) lastExprWasSSBinary = null;

        // Don't need to do anything else- return value is always left on the stack 
        return null;
    }

    @Override
    public Object visitLiteralExpr(LiteralExpr le, Object arg) {
        // Visit the literal and pass the returned value back along
        return le.lit.visit(this, null);
    }

    @Override
    public Object visitNewObjectExpr(NewObjectExpr noe, Object arg) {
        // Put -1 on the stack for the class object
        if ((Boolean) arg) Machine.emit(Op.LOADL, -1);

        // Put the number of nonstatic fields in the class on the stack
        if ((Boolean) arg) Machine.emit(Op.LOADL, noe.classtype.getDecl().data);

        // Emit call to the newobj primitive, which will leave the new object's addr on the stack
        if ((Boolean) arg) Machine.emit(Prim.newobj);

        // If something is being written here, we didn't just process a compile-time-unknown
        // short-circuiting binary operator
        if ((Boolean) arg) lastExprWasSSBinary = null;

        return null;
    }

    @Override
    public Object visitNewArrayExpr(NewArrayExpr nae, Object arg) {
        // Force the number of elements in the array to be recorded on the stack
        forcePushResult((Integer) nae.sizeExpr.visit(this, arg), arg);

        // Emit call to the newarr primitive, which will leave the new array's addr on the stack
        if ((Boolean) arg) Machine.emit(Prim.newarr);

        // If something is being written here, we didn't just process a compile-time-unknown
        // short-circuiting binary operator
        if ((Boolean) arg) lastExprWasSSBinary = null;

        return null;
    }

    @Override
    public Object visitNullExpr(NullExpr ne, Object arg) {
        // null will always yield a value of Machine.nullRep
        return Machine.nullRep;
    }

    ///////////////////////////////////////////////////////////////////////////////
    //
    // REFERENCES
    //
    ///////////////////////////////////////////////////////////////////////////////

    private enum RefVisitMode {
        READ, WRITE
        /*
         * This is irrelevant if accessing a method
         * 
         * READ will result in the field or variable having its value put on the stack
         * 
         * WRITE will return the neccessary items for writing to the field or variable, or will
         * put them on the stack if that's where they're needed
         *      Static field:
         *          Return SB, offset
         *      Non-static field that's a member of this:
         *          Return OB, offset
         *      Non-static field that's a member of another object:
         *          Return null, -1
         *          Stack: ..., object addr a, field index i 
         *      Local (either parameter or variable):
         *          Return LB, null
        */
    }

    // Used for returns when calling with WRITE
    private class RefVisitReturn {
        Machine.Reg reg;
        int offset;

        public RefVisitReturn(Reg reg, int offset) {
            this.reg = reg;
            this.offset = offset;
        }
    }

    @Override
    public Object visitThisRef(ThisRef tr, Object arg) {
        // Note: ThisRef will ONLY be visited directly when it's unqualified

        // Make sure this is only being called to READ
        if (arg == RefVisitMode.WRITE) {
            throw new IllegalStateException("Shouldn't be writing to this");
        }

        // Put the current value of OB on the stack
        Machine.emit(Op.LOADA, Reg.OB, 0);

        return null;
    }

    @Override
    public Object visitIdRef(IdRef ir, Object arg) {
        // Note: This won't run if ir points to a ClassDecl, as that would require the containing
        // QualRef to be pointing at a static member

        Declaration decl = ir.getId().getDecl();
        if (decl instanceof FieldDecl) { // Handle FieldDecls
            // We're accessing the value of a member variable of this

            FieldDecl field = (FieldDecl) decl;

            if (arg == RefVisitMode.READ) {
                Machine.emit(Op.LOAD, field.isStatic ? Reg.SB : Reg.OB, field.data);
                return null;
            } else if (arg == RefVisitMode.WRITE) {
                return new RefVisitReturn(field.isStatic ? Reg.SB : Reg.OB, field.data);
            } else {
                throw new IllegalStateException("Invalid arg when visiting IdRef");
            }

        } else if (decl instanceof MethodDecl) { // Handle MethodDecl
            // Note: mode is ignored here

            MethodDecl field = (MethodDecl) decl;

            // If this method isn't static, we need to record the instance's address on the stack
            // Here, the instance is this, so we just record the current value of OB
            if (!field.isStatic) {
                Machine.emit(Op.LOADA, Reg.OB, 0);
            }

            return null;

        } else if (decl instanceof LocalDecl) { // Handle VarDecl and ParameterDecl
            LocalDecl local = (LocalDecl) decl;

            // This value is stored relative to the current LB
            // Parameters have offset < 0, vars >= 0, but our code doesn't have to differentiate
            if (arg == RefVisitMode.READ) {
                Machine.emit(Op.LOAD, Reg.LB, local.data);
                return null;
            } else if (arg == RefVisitMode.WRITE) {
                return new RefVisitReturn(Reg.LB, local.data);
            } else {
                throw new IllegalStateException("Invalid arg when visiting IdRef");
            }

        } else {
            throw new IllegalStateException(
                    "IdRef should only be able to point to a FieldDecl, MethodDecl, or LocalDecl");
        }
    }

    @Override
    public Object visitQualRef(QualRef qr, Object arg) {
        // Note: QualRef will only ever point to MemberDecls (can be an array's length pseudomember)

        Declaration decl = qr.getId().getDecl();
        if (decl == ContextualAnalyzer.arrayLengthField) { // Handle array length
            // Verify this is the READ mode
            if (arg != RefVisitMode.READ) {
                throw new IllegalStateException("Must READ when accessing array length");
            }

            // Visit the preceding reference to emit code that will put its value on the stack
            qr.prevRef.visit(this, RefVisitMode.READ);

            // Call the arraylen primitive
            Machine.emit(Prim.arraylen);

            return null;

        } else if (decl instanceof FieldDecl) { // Handle FieldDecls
            FieldDecl field = (FieldDecl) decl;

            if (field.isStatic) {
                // Since this is a static field, the value is stored relative to SB
                if (arg == RefVisitMode.READ) {
                    Machine.emit(Op.LOAD, Reg.SB, field.data);
                    return null;
                } else if (arg == RefVisitMode.WRITE) {
                    return new RefVisitReturn(Reg.SB, field.data);
                } else {
                    throw new IllegalStateException("Invalid arg when visiting QualRef");
                }
            } else {
                // Otherwise, this is a non-static field

                if (qr.prevRef instanceof ThisRef) {
                    // If we're accessing a member of this, we can just load it directly
                    if (arg == RefVisitMode.READ) {
                        Machine.emit(Op.LOAD, Reg.OB, field.data);
                        return null;
                    } else if (arg == RefVisitMode.WRITE) {
                        return new RefVisitReturn(Reg.OB, field.data);
                    } else {
                        throw new IllegalStateException("Invalid arg when visiting QualRef");
                    }
                } else {
                    // If we're accessing a member of another object, we'll need to use the fieldref
                    // (or fieldupd, but not right here) primitives

                    // Visit prevRef to generate code that pushes the instance's addr onto the stack
                    qr.prevRef.visit(this, RefVisitMode.READ);

                    // Load the field offset onto the stack
                    Machine.emit(Op.LOADL, field.data);

                    if (arg == RefVisitMode.READ) {
                        // Get the field's value
                        Machine.emit(Prim.fieldref);
                        return null;
                    } else if (arg == RefVisitMode.WRITE) {
                        return new RefVisitReturn(null, -1);
                    } else {
                        throw new IllegalStateException("Invalid arg when visiting QualRef");
                    }
                }
            }
        } else if (decl instanceof MethodDecl) { // Handle MethodDecls
            // Note: mode is ignored here

            MethodDecl method = (MethodDecl) decl;

            // Note: the method's arguments have already been put on the stack by the enclosing
            // CallStmt or CallExpr

            if (!method.isStatic) {
                // If the method isn't static, we need to put the instance's address on the stack
                qr.prevRef.visit(this, RefVisitMode.READ);
            }

            // Note: we will not put the method's address on the stack here. emitCall can retrieve
            // the address for its CALL or CALLI instruction.
            // Plus, the address likely needs to be patched.

            return null;
        } else {
            throw new IllegalStateException("QualRef should only be able to point to a MemberDecl");
        }
    }

    ///////////////////////////////////////////////////////////////////////////////
    //
    // TERMINALS
    //
    ///////////////////////////////////////////////////////////////////////////////

    @Override
    public Object visitIdentifier(Identifier i, Object arg) {
        // Unneeded, shouldn't ever be run
        throw new UnsupportedOperationException(
                "visitIdentifier() in CodeGenerator should never be run");
    }

    @Override
    public Object visitOperator(Operator o, Object arg) {
        /*
         * Note: only called if the expression's value isn't known at compile time
         * 
         * Expected state:
         * 
         * For a unary operator:
         *      ..., operand ==> ..., result 
         *      Return: null
         *           
         * For a binary operator:
         *      ..., left, right ==> ..., result
         *      Return: null
         *      This applies to short-circuiting operators as well, as this method is only run
         *      when both values are known at compile time.
         */

        // Emit the calculation instruction
        switch (o.kind) {
            case NOT: // Unary
                Machine.emit(Prim.not);
                break;
            case MINUS: // Can be unary
                Machine.emit(o.operandCount == 1 ? Prim.neg : Prim.sub);
                break;
            case LESS_EQUAL:
                Machine.emit(Prim.le);
                break;
            case LESS_THAN:
                Machine.emit(Prim.lt);
                break;
            case GREATER_THAN:
                Machine.emit(Prim.gt);
                break;
            case GREATER_EQUAL:
                Machine.emit(Prim.ge);
                break;
            case PLUS:
                Machine.emit(Prim.add);
                break;
            case MULTIPLY:
                Machine.emit(Prim.mult);
                break;
            case DIVIDE:
                Machine.emit(Prim.div);
                break;
            case MODULUS:
                Machine.emit(Prim.mod);
                break;
            case EQUAL_TO:
                Machine.emit(Prim.eq);
                break;
            case NOT_EQUAL:
                Machine.emit(Prim.ne);
                break;
            // AND and OR are intentionally *not* handled here, as this function isn't set up to
            // handle short-circuiting operators
            default:
                throw new IllegalStateException("It shouldn't be possible to reach this line");
        }

        // If something is being written here, we didn't just process a compile-time-unknown
        // short-circuiting binary operator
        lastExprWasSSBinary = null;

        return null;
    }

    @Override
    public Object visitIntLiteral(IntLiteral il, Object arg) {
        // Return the integer represented by the literal token
        return Integer.parseInt(il.spelling);
    }

    @Override
    public Object visitBooleanLiteral(BooleanLiteral bl, Object arg) {
        // Return Machine.trueRep for "true" and Machine.falseRep for "false"
        return boolToInt(bl.kind == TRUE);
    }
}
