package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.*;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;
import edu.ufl.cise.cop4020fa23.exceptions.TypeCheckException;

import org.apache.commons.lang3.tuple.Pair;
import java.util.List;


public class TypeCheckVisitor implements ASTVisitor {

    SymbolTable symbolTable = new SymbolTable();



    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws PLCCompilerException {
        LValue lValue = assignmentStatement.getlValue();
        Expr expr = assignmentStatement.getE();
        Type lValueType = assignmentStatement.getlValue().getType();
        Type exprType = assignmentStatement.getE().getType();

        if(assignmentCompatible(lValueType, exprType)){
            symbolTable.enterScope();
            lValue.visit(this, arg);
            expr.visit(this, arg);
            symbolTable.closeScope();
        }
        else {
            throw new TypeCheckException("assignment incompatible");
        }
        return lValueType;
    }

    public Boolean assignmentCompatible(Type one, Type two){
        return one == two || (one == Type.PIXEL && two == Type.INT) || (one == Type.IMAGE && (two == Type.PIXEL || two == Type.INT || two == Type.STRING));
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCCompilerException {
        throw new PLCCompilerException("Unimplemented Method ASTVisitor.visitBinaryExpr invoked.");
    }

    @Override
    public Object visitBlock(Block block, Object arg) throws PLCCompilerException {
        symbolTable.enterScope();
        List<Block.BlockElem> blockElems = block.getElems();
        for (Block.BlockElem elem : blockElems) {
            elem.visit(this, arg);
        }
        symbolTable.closeScope();
        return block;
    }

    @Override
    public Object visitBlockStatement(StatementBlock statementBlock, Object arg) throws PLCCompilerException {
        throw new PLCCompilerException("Unimplemented Method ASTVisitor.visitBlockStatement invoked.");
    }

    @Override
    public Object visitChannelSelector(ChannelSelector channelSelector, Object arg) throws PLCCompilerException {
        throw new PLCCompilerException("Unimplemented Method ASTVisitor.visitChannelSelector invoked.");
    }

    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws PLCCompilerException {
        throw new PLCCompilerException("Unimplemented Method ASTVisitor.visitConditionalExpr invoked.");
    }

    @Override
    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCCompilerException {
        Expr expr = declaration.getInitializer();
        NameDef nameDef = declaration.getNameDef();
        Type exprType = null;
        if(expr != null) {
            exprType = declaration.getInitializer().getType();
        }
        Type nameDefType = declaration.getNameDef().getType();

        if(expr == null){
            symbolTable.enterScope();
            nameDef.visit(this, arg);
            symbolTable.closeScope();
        }
        else if(exprType == nameDefType || (exprType == Type.STRING && nameDefType == Type.IMAGE)){
            symbolTable.enterScope();
            expr.visit(this, arg);
            nameDef.visit(this, arg);
            symbolTable.closeScope();
        }
        else {
            throw new TypeCheckException("Declaration type error");
        }
        return nameDefType;
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCCompilerException {
        Type typeW = (Type) dimension.getWidth().visit(this, arg);
        check(typeW == Type.INT, dimension, "image width must be int");
        Type typeH = (Type) dimension.getHeight().visit(this, arg);
        check(typeH == Type.INT, dimension, "image height must be int");
        return dimension;
    }

    private void check(boolean b, Dimension dimension, String imageWidthMustBeInt) throws PLCCompilerException {
        if(!b){
            throw new TypeCheckException(imageWidthMustBeInt);
        }

    }

    @Override
    public Object visitDoStatement(DoStatement doStatement, Object arg) throws PLCCompilerException {
        throw new PLCCompilerException("Unimplemented Method ASTVisitor.visitDoStatement invoked.");
    }

    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCCompilerException {
        throw new PLCCompilerException("Unimplemented Method ASTVisitor.visitExpandedPixelExpr invoked.");
    }

    @Override
    public Object visitGuardedBlock(GuardedBlock guardedBlock, Object arg) throws PLCCompilerException {
        throw new PLCCompilerException("Unimplemented Method ASTVisitor.visitGuardedBlock invoked.");
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCCompilerException {
        throw new PLCCompilerException("Unimplemented Method ASTVisitor.visitIdentExpr invoked.");
    }

    @Override
    public Object visitIfStatement(IfStatement ifStatement, Object arg) throws PLCCompilerException {
        throw new PLCCompilerException("Unimplemented Method ASTVisitor.visitIfStatement invoked.");
    }

    @Override
    public Object visitLValue(LValue lValue, Object arg) throws PLCCompilerException {
        String name = lValue.getName();
        lValue.setNameDef(symbolTable.lookup(name));
        Type type = lValue.getNameDef().getType();
        lValue.setType(type);
        lValue.setType(inferLValueType(type, lValue.getPixelSelector(), lValue.getChannelSelector()));
        type = lValue.getType();
        return type;
    }

    public Type inferLValueType(Type lValue, PixelSelector pixelSelector, ChannelSelector channelSelector) throws PLCCompilerException{
        if(pixelSelector == null && channelSelector == null){
            return lValue;
        }
        else if(lValue == Type.IMAGE && pixelSelector != null && channelSelector == null){
            return Type.PIXEL;
        }
        else if(lValue == Type.IMAGE && pixelSelector != null && channelSelector != null){
            return Type.INT;
        }
        else if(lValue == Type.IMAGE && pixelSelector == null && channelSelector != null){
            return Type.IMAGE;
        }
        else if(lValue == Type.PIXEL && pixelSelector == null && channelSelector != null){
            return Type.INT;
        }
        else{
            throw new TypeCheckException("inferLValue type error");
        }
    }

    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCCompilerException {
        Type type = nameDef.getType();
        Pair<Integer, NameDef> pair = Pair.of(symbolTable.current_num, nameDef);
        if(nameDef.getDimension() == null){
            type = Type.IMAGE;
            nameDef.setType(type);
        }
        else if(type == Type.VOID){
            throw new TypeCheckException("nameDef type is void");
        }
        else{
            symbolTable.enterScope();
            nameDef.getDimension().visit(this, arg);
            symbolTable.closeScope();
        }
        symbolTable.insert(nameDef.getName(), pair);
        return type;
    }

    @Override
    public Object visitNumLitExpr(NumLitExpr numLitExpr, Object arg) throws PLCCompilerException {
        Type type = Type.INT;
        numLitExpr.setType(type);
        return type;
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws PLCCompilerException {
        throw new PLCCompilerException("Unimplemented Method ASTVisitor.visitPixelSelector invoked.");
    }

    @Override
    public Object visitPostfixExpr(PostfixExpr postfixExpr, Object arg) throws PLCCompilerException {
        throw new PLCCompilerException("Unimplemented Method ASTVisitor.visitPostfixExpr invoked.");
    }

    @Override
    public Object visitProgram(Program program, Object arg) throws PLCCompilerException {
        Program root = program;
        Type type = Type.kind2type(program.getTypeToken().kind());
        program.setType(type);
        symbolTable.enterScope();
        List<NameDef> params = program.getParams();
        for (NameDef param : params) {
            param.visit(this, arg);
        }
        program.getBlock().visit(this, arg);
        symbolTable.closeScope();
        return type;
    }

    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws PLCCompilerException {
        throw new PLCCompilerException("Unimplemented Method ASTVisitor.visitReturnStatement invoked.");
    }

    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws PLCCompilerException {
        throw new PLCCompilerException("Unimplemented Method ASTVisitor.visitStringLitExpr invoked.");
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws PLCCompilerException {
        throw new PLCCompilerException("Unimplemented Method ASTVisitor.visitUnaryExpr invoked.");
    }

    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws PLCCompilerException {
        writeStatement.getExpr().visit(this, arg);
        return writeStatement;
    }

    @Override
    public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws PLCCompilerException {
        throw new PLCCompilerException("Unimplemented Method ASTVisitor.visitBooleanLitExpr invoked.");
    }

    @Override
    public Object visitConstExpr(ConstExpr constExpr, Object arg) throws PLCCompilerException {
        throw new PLCCompilerException("Unimplemented Method ASTVisitor.visitConstExpr invoked.");
    }
}
