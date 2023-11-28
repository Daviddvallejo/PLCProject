package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.*;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;
import edu.ufl.cise.cop4020fa23.exceptions.TypeCheckException;

import org.apache.commons.lang3.tuple.Pair;
import java.util.List;


public class TypeCheckVisitor implements ASTVisitor {

    SymbolTable symbolTable = new SymbolTable();
    Program root;


    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws PLCCompilerException {
        LValue lValue = assignmentStatement.getlValue();
        Expr expr = assignmentStatement.getE();
        Type lValueType = assignmentStatement.getlValue().getType();
        Type exprType = assignmentStatement.getE().getType();

        if(assignmentCompatible(lValueType, exprType)){
            lValue.visit(this, arg);
            expr.visit(this, arg);
        }
        else {
            throw new TypeCheckException("assignment incompatible");
        }
        return assignmentStatement;
    }

    public Boolean assignmentCompatible(Type one, Type two){
        return one == two || (one == Type.PIXEL && two == Type.INT) || (one == Type.IMAGE && (two == Type.PIXEL || two == Type.INT || two == Type.STRING));
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCCompilerException {
        System.out.println("BinaryExpr");
        return inferBinaryType(binaryExpr.getLeftExpr(), binaryExpr.getOp().kind(), binaryExpr.getRightExpr());
    }

    public Type inferBinaryType(Expr leftExpr, Kind op, Expr rightExpr) throws TypeCheckException {
        Type lType = leftExpr.getType();
        Type rType = rightExpr.getType();

        if(lType == Type.PIXEL && (op == Kind.BITAND || op == Kind.BITOR) && rType == Type.PIXEL){
            return Type.PIXEL;
        }
        else if (lType == Type.BOOLEAN && (op == Kind.AND || op == Kind.OR) && rType == Type.BOOLEAN) {
            return Type.BOOLEAN;
        }
        else if(lType == Type.INT && (op == Kind.LT || op == Kind.GT || op == Kind.LE || op == Kind.GE) && rType == Type.INT){
            return Type.BOOLEAN;
        }
        else if(lType == Type.INT && op == Kind.EXP && rType == Type.INT){
            return Type.INT;
        }
        else if(lType == Type.PIXEL && op == Kind.EXP && rType == Type.INT){
            return Type.PIXEL;
        }
        else if((lType == Type.PIXEL || lType == Type.INT || lType == Type.IMAGE) && (op == Kind.MINUS || op == Kind.TIMES || op == Kind.DIV || op == Kind.MOD) && rType == lType){
            return lType;
        }
        else if((lType == Type.PIXEL || lType == Type.IMAGE) && (op == Kind.TIMES || op == Kind.DIV || op == Kind.MOD) && rType == Type.INT){
            return lType;
        }
        else if(op == Kind.EQ && lType == rType){
            return Type.BOOLEAN;
        }
        else if(op == Kind.PLUS && lType == rType){
            return lType;
        }
        throw new TypeCheckException("inferBinaryType error");
    }

    @Override
    public Object visitBlock(Block block, Object arg) throws PLCCompilerException {
        List<Block.BlockElem> blockElems = block.getElems();
        for (Block.BlockElem elem : blockElems) {
            elem.visit(this, arg);
        }
        return block;
    }

    @Override
    public Object visitBlockStatement(StatementBlock statementBlock, Object arg) throws PLCCompilerException {
        symbolTable.enterScope();
        statementBlock.getBlock().visit(this, arg);
        symbolTable.closeScope();
        return statementBlock;
    }

    @Override
    public Object visitChannelSelector(ChannelSelector channelSelector, Object arg) throws PLCCompilerException {
        return Type.kind2type(channelSelector.firstToken().kind());
    }

    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws PLCCompilerException {
        System.out.println("ConditionalExpr");
        Expr guardExpr = conditionalExpr.getGuardExpr();
        Expr trueExpr = conditionalExpr.getTrueExpr();
        Expr falseExpr = conditionalExpr.getFalseExpr();

        if(guardExpr.getType() != Type.BOOLEAN){
            throw new TypeCheckException("guardExpr type error");
        }
        if(trueExpr.getType() != falseExpr.getType()){
            throw new TypeCheckException("true/false expr type error");
        }

        conditionalExpr.setType(trueExpr.getType());

        return conditionalExpr.getType();
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
            nameDef.visit(this, arg);
        }
        else if(exprType == nameDefType || (exprType == Type.STRING && nameDefType == Type.IMAGE)){
            expr.visit(this, arg);
            nameDef.visit(this, arg);
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
        for(GuardedBlock guardedBlock : doStatement.getGuardedBlocks()){
            guardedBlock.visit(this, arg);
        }
        return doStatement;
    }

    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCCompilerException {
        System.out.println("expandedPixelExpr");
        Expr red = expandedPixelExpr.getRed();
        Expr blue = expandedPixelExpr.getBlue();
        Expr green = expandedPixelExpr.getGreen();

        red.setType(symbolTable.lookup(red.firstToken.text()).getType());
        blue.setType(symbolTable.lookup(blue.firstToken.text()).getType());
        green.setType(symbolTable.lookup(green.firstToken.text()).getType());

        Type redType = red.getType();
        Type blueType = blue.getType();
        Type greenType = green.getType();

        if(redType != Type.INT || blueType != Type.INT || greenType != Type.INT){
            throw new TypeCheckException("expandedPixelExpr error");
        }
        expandedPixelExpr.setType(Type.PIXEL);
        return expandedPixelExpr;
    }

    @Override
    public Object visitGuardedBlock(GuardedBlock guardedBlock, Object arg) throws PLCCompilerException {
        Type type = guardedBlock.getGuard().getType();
        if(type != Type.BOOLEAN){
            throw new TypeCheckException("guardedBlock type error");
        }
        guardedBlock.getBlock().visit(this, arg);
        return guardedBlock;
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCCompilerException {
        String name = identExpr.getName();
        System.out.println("identExpr");
        if(symbolTable.lookup(name) == null){
            throw new TypeCheckException("identExpr not in symbol table");
        }
        identExpr.setNameDef(symbolTable.lookup(name));
        identExpr.setType(identExpr.getNameDef().getType());

        return identExpr.getType();
    }

    @Override
    public Object visitIfStatement(IfStatement ifStatement, Object arg) throws PLCCompilerException {
        for(GuardedBlock guardedBlock : ifStatement.getGuardedBlocks()){
            guardedBlock.visit(this, arg);
        }
        return ifStatement;
    }

    @Override
    public Object visitLValue(LValue lValue, Object arg) throws PLCCompilerException {
        String name = lValue.getName();
        System.out.println("visitLValue");
        lValue.setNameDef(symbolTable.lookup(name));
        Type type = lValue.getNameDef().getType();
        lValue.setType(type);
        lValue.setType(inferLValueType(type, lValue.getPixelSelector(), lValue.getChannelSelector()));
        if(lValue.getPixelSelector() != null){
            lValue.getPixelSelector().visit(this, true);
        }
        if(lValue.getChannelSelector() != null){
            lValue.getChannelSelector().visit(this, arg);
        }

        return lValue;
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
        if(nameDef.getDimension() != null){
            type = Type.IMAGE;
            nameDef.setType(type);
            nameDef.getDimension().visit(this, arg);
        }
        else if(type == Type.VOID){
            throw new TypeCheckException("nameDef type is void");
        }
        symbolTable.insert(nameDef.getName(), pair);
        return nameDef;
    }

    @Override
    public Object visitNumLitExpr(NumLitExpr numLitExpr, Object arg) throws PLCCompilerException {
        System.out.println("numLit expr");
        Type type = Type.INT;
        numLitExpr.setType(type);
        return type;
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws PLCCompilerException {
        Expr xExpr = pixelSelector.xExpr();
        Expr yExpr = pixelSelector.yExpr();
        System.out.println("visitPixelSelector");
        if(arg.equals(true)){
            if(!(xExpr.firstToken.kind() == Kind.IDENT || xExpr.firstToken.kind() == Kind.NUM_LIT)){
                //.out.println(xExpr.firstToken.kind());
                throw new TypeCheckException("xExpr Kind error");
            }
            if(!(yExpr.firstToken.kind() == Kind.IDENT || yExpr.firstToken.kind() == Kind.NUM_LIT)){
                throw new TypeCheckException("yExpr Kind error");
            }
            if(xExpr.firstToken.kind() == Kind.IDENT && symbolTable.lookup(xExpr.firstToken().text()) == null){
                SyntheticNameDef nameDef = new SyntheticNameDef(xExpr.firstToken().text());
                Pair<Integer, NameDef> pair = Pair.of(symbolTable.current_num, nameDef);
                symbolTable.insert(xExpr.firstToken().text(),pair);
                xExpr.setType(Type.INT);
            }
            if(yExpr.firstToken.kind() == Kind.IDENT && symbolTable.lookup(yExpr.firstToken().text()) == null){
                SyntheticNameDef nameDef = new SyntheticNameDef(yExpr.firstToken().text());
                Pair<Integer, NameDef> pair = Pair.of(symbolTable.current_num, nameDef);
                symbolTable.insert(yExpr.firstToken().text(),pair);
                yExpr.setType(Type.INT);
            }
        }
        if(xExpr.getType() != Type.INT){
            throw new TypeCheckException("xExpr type error");
        }
        if(yExpr.getType() != Type.INT){
            throw new TypeCheckException("yExpr type error");
        }
        return pixelSelector;
    }

    @Override
    public Object visitPostfixExpr(PostfixExpr postfixExpr, Object arg) throws PLCCompilerException {
        System.out.println("postfix Expr");
        return inferPostfixExprType(postfixExpr.primary(), postfixExpr.pixel(), postfixExpr.channel());
    }

    public Type inferPostfixExprType(Expr expr, PixelSelector pixelSelector, ChannelSelector channelSelector) throws TypeCheckException{
        Type exprType = expr.getType();
        if(pixelSelector == null && channelSelector == null){
            return exprType;
        }
        else if(exprType == Type.IMAGE && pixelSelector != null && channelSelector == null){
            return Type.PIXEL;
        }
        else if(exprType == Type.IMAGE && pixelSelector != null){
            return Type.INT;
        }
        else if(exprType == Type.IMAGE){
            return Type.IMAGE;
        }
        else if(exprType == Type.PIXEL && pixelSelector == null){
            return Type.INT;
        }
        throw new TypeCheckException("inferPostfixExprType error");
    }

    @Override
    public Object visitProgram(Program program, Object arg) throws PLCCompilerException {
        Type type = Type.kind2type(program.getTypeToken().kind());
        program.setType(type);
        symbolTable.enterScope();
        root = program;
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
        String name = returnStatement.getE().firstToken.text();
        Type type = symbolTable.lookup(name).getType();
        Type programType = root.getType();
        if(type == programType){
            returnStatement.getE().setType(type);
            return type;
        }
        throw new TypeCheckException("returnStatement type error");
    }

    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws PLCCompilerException {
        System.out.println("StringLit Expr");
        stringLitExpr.setType(Type.STRING);
        return stringLitExpr.getType();
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws PLCCompilerException {
        return inferUnaryExpr(unaryExpr, unaryExpr.getOp());
    }

    public Type inferUnaryExpr(Expr expr, Kind op) throws TypeCheckException{
        System.out.println("inferUnary");
        Type exprType = expr.getType();
        if(exprType == Type.BOOLEAN && op == Kind.BANG){
            return Type.BOOLEAN;
        }
        else if(exprType == Type.INT && op == Kind.MINUS){
            return Type.INT;
        }
        else if(exprType == Type.IMAGE && (op == Kind.RES_height || op == Kind.RES_width)){
            return Type.INT;
        }
        throw new TypeCheckException("inferUnaryExpr error");
    }

    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws PLCCompilerException {
        writeStatement.getExpr().visit(this, arg);
        return writeStatement;
    }

    @Override
    public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws PLCCompilerException {
        System.out.println("booleanLit expr");
        booleanLitExpr.setType(Type.BOOLEAN);
        return Type.BOOLEAN;
    }

    @Override
    public Object visitConstExpr(ConstExpr constExpr, Object arg) throws PLCCompilerException {
        System.out.println("constExpr");
        String name = constExpr.getName();
        if(name.equals("Z")){
            constExpr.setType(Type.INT);
            return Type.INT;
        }
        constExpr.setType(Type.PIXEL);
        return Type.PIXEL;
    }
}
