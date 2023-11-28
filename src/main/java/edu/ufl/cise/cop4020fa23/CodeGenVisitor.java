package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.*;
import edu.ufl.cise.cop4020fa23.exceptions.CodeGenException;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;

public class CodeGenVisitor implements ASTVisitor {

    StringBuilder source = new StringBuilder();
    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws PLCCompilerException {
        throw new CodeGenException("visitAssignmentStatement not implemented yet");
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCCompilerException {
        throw new CodeGenException("visitBinaryExpr not implemented yet");
    }

    @Override
    public Object visitBlock(Block block, Object arg) throws PLCCompilerException {
        throw new CodeGenException("visitBlock not implemented yet");
    }

    @Override
    public Object visitBlockStatement(StatementBlock statementBlock, Object arg) throws PLCCompilerException {
        throw new CodeGenException("visitBlockStatement not implemented yet");
    }

    @Override
    public Object visitChannelSelector(ChannelSelector channelSelector, Object arg) throws PLCCompilerException {
        throw new CodeGenException("visitChannelSelector not implemented yet");
    }

    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws PLCCompilerException {
        throw new CodeGenException("visitConditionalExpr not implemented yet");
    }

    @Override
    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCCompilerException {
        throw new CodeGenException("visitDeclaration not implemented yet");
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCCompilerException {
        throw new CodeGenException("visitDimension not implemented yet");
    }

    @Override
    public Object visitDoStatement(DoStatement doStatement, Object arg) throws PLCCompilerException {
        throw new CodeGenException("visitDoStatement not implemented yet");
    }

    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCCompilerException {
        throw new CodeGenException("visitExpandedPixelExpr not implemented yet");
    }

    @Override
    public Object visitGuardedBlock(GuardedBlock guardedBlock, Object arg) throws PLCCompilerException {
        throw new CodeGenException("visitGuardedBlock not implemented yet");
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCCompilerException {
        throw new CodeGenException("visitIdentExpr not implemented yet");
    }

    @Override
    public Object visitIfStatement(IfStatement ifStatement, Object arg) throws PLCCompilerException {
        throw new CodeGenException("visitIfStatement not implemented yet");
    }

    @Override
    public Object visitLValue(LValue lValue, Object arg) throws PLCCompilerException {
        throw new CodeGenException("visitLValue not implemented yet");
    }

    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCCompilerException {
        throw new CodeGenException("visitNameDef not implemented yet");
    }

    @Override
    public Object visitNumLitExpr(NumLitExpr numLitExpr, Object arg) throws PLCCompilerException {
        throw new CodeGenException("visitNumLitExpr not implemented yet");
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws PLCCompilerException {
        throw new CodeGenException("visitPixelSelector not implemented yet");
    }

    @Override
    public Object visitPostfixExpr(PostfixExpr postfixExpr, Object arg) throws PLCCompilerException {
        throw new CodeGenException("visitPostfixExpr not implemented yet");
    }

    @Override
    public Object visitProgram(Program program, Object arg) throws PLCCompilerException {
        int paramSize = program.getParams().size();
        source.append("public class ");
        source.append(program.getName());
        source.append("{ \n public static ");
        source.append(program.getType());
        source.append(" apply(");
        for(NameDef i : program.getParams()){
            String type = i.getType().toString();
            switch(type){
                case("INT"):
                    source.append("int");
                    break;
                case("BOOLEAN"):
                    source.append("boolean");
                    break;
                case("IMAGE"):
                    source.append("image");
                    break;
                case("VOID"):
                    source.append("void");
                    break;
                case("PIXEL"):
                    source.append("pixel");
                    break;
                case("STRING"):
                    source.append("string");
                    break;
            }
            source.append(" ");
            source.append(i.getName());
            if(paramSize > 1 && i != program.getParams().get(paramSize - 1)) {
                source.append(", ");
            }
            
        }
        source.append(")\n");
        System.out.println(source.toString());
        throw new CodeGenException("visitProgram not implemented yet");
    }

    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws PLCCompilerException {
        throw new CodeGenException("visitReturnStatement not implemented yet");
    }

    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws PLCCompilerException {
        throw new CodeGenException("visitStringLitExpr not implemented yet");
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws PLCCompilerException {
        throw new CodeGenException("visitUnaryExpr not implemented yet");
    }

    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws PLCCompilerException {
        throw new CodeGenException("visitWriteStatement not implemented yet");
    }

    @Override
    public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws PLCCompilerException {
        throw new CodeGenException("visitBooleanLitExpr not implemented yet");
    }

    @Override
    public Object visitConstExpr(ConstExpr constExpr, Object arg) throws PLCCompilerException {
        throw new CodeGenException("visitConstExpr not implemented yet");
    }
}
