package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.*;
import edu.ufl.cise.cop4020fa23.exceptions.CodeGenException;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;
import edu.ufl.cise.cop4020fa23.runtime.ConsoleIO;

import java.util.List;

public class CodeGenVisitor implements ASTVisitor {

    StringBuilder source = new StringBuilder();
    StringBuilder imports = new StringBuilder();
    SymbolTable symbolTable = new SymbolTable();
    boolean consoleIO = false;
    boolean math = false;
    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws PLCCompilerException {
        source.append("\t".repeat(Math.max(0, symbolTable.next_num)));
        assignmentStatement.getlValue().visit(this,arg);
        source.append(" = ");
        assignmentStatement.getE().visit(this,arg);
        source.append(";\n");
        return assignmentStatement;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCCompilerException {
        Expr left = binaryExpr.getLeftExpr();
        Kind op = binaryExpr.getOpKind();
        Expr right = binaryExpr.getRightExpr();
        //source.append("\t".repeat(Math.max(0, symbolTable.next_num)));
        if(left.getType() == Type.STRING && op == Kind.EQ){
            left.visit(this,arg);
            source.append(".equals(");
            right.visit(this,arg);
            source.append(")");
        }
        else if(op == Kind.EXP){
            math = true;
            source.append("((int)Math.round(Math.pow(");
            left.visit(this,arg);
            source.append(", ");
            right.visit(this,arg);
            source.append(")))");
        }
        else{
            //source.append("(");
            left.visit(this,arg);
            source.append(" ");
            kind2String(op);
            source.append(" ");
            right.visit(this,arg);
        }
        return binaryExpr;
    }

    public void kind2String(Kind op){
        switch(op){
            case COMMA:
                source.append(",");
                break;
            case SEMI:
                source.append(";");
                break;
            case QUESTION:
                source.append("?");
                break;
            case COLON:
                source.append(":");
                break;
            case LPAREN:
                source.append("(");
                break;
            case RPAREN:
                source.append(")");
                break;
            case LT:
                source.append("<");
                break;
            case GT:
                source.append(">");
                break;
            case LSQUARE:
                source.append("[");
                break;
            case RSQUARE:
                source.append("]");
                break;
            case ASSIGN:
                source.append("=");
                break;
            case EQ:
                source.append("==");
                break;
            case LE:
                source.append("<=");
                break;
            case GE:
                source.append(">=");
                break;
            case BANG:
                source.append("!");
                break;
            case BITAND:
                source.append("&");
                break;
            case AND:
                source.append("&&");
                break;
            case BITOR:
                source.append("|");
                break;
            case OR:
                source.append("||");
                break;
            case PLUS:
                source.append("+");
                break;
            case MINUS:
                source.append("-");
                break;
            case TIMES:
                source.append("*");
                break;
            case EXP:
                source.append("**");
                break;
            case DIV:
                source.append("/");
                break;
            case MOD:
                source.append("%");
                break;
            case BLOCK_OPEN:
                source.append("<:");
                break;
            case BLOCK_CLOSE:
                source.append(":>");
                break;
            case RETURN:
                source.append("^");
                break;
            case RARROW:
                source.append("->");
                break;
            case BOX:
                source.append("[]");
                break;

        }
    }
    @Override
    public Object visitBlock(Block block, Object arg) throws PLCCompilerException {
        List<Block.BlockElem> blockElems = block.getElems();
        source.append("{\n");
        for (Block.BlockElem elem : blockElems) {
            elem.visit(this, arg);
        }
        source.append("\t".repeat(Math.max(0, symbolTable.current_num)));
        source.append("}\n");

        return block;
    }

    @Override
    public Object visitBlockStatement(StatementBlock statementBlock, Object arg) throws PLCCompilerException {
        symbolTable.enterScope();
        statementBlock.getBlock().visit(this,arg);
        symbolTable.closeScope();
        return statementBlock;
    }

    @Override
    public Object visitChannelSelector(ChannelSelector channelSelector, Object arg) throws PLCCompilerException {
        throw new CodeGenException("visitChannelSelector not implemented yet");
    }

    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws PLCCompilerException {
//        source.append("\t".repeat(Math.max(0, symbolTable.next_num)));
        source.append("( ");
        conditionalExpr.getGuardExpr().visit(this,arg);
        source.append(" ? ");
        conditionalExpr.getTrueExpr().visit(this,arg);
        source.append(": ");
        conditionalExpr.getFalseExpr().visit(this, arg);
        source.append(" );");
        return conditionalExpr;
    }

    @Override
    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCCompilerException {
        source.append("\t".repeat(Math.max(0, symbolTable.next_num)));
        if(declaration.getInitializer() != null){
            type2String(declaration.getNameDef().getType());
            source.append(" ");
            source.append(declaration.getNameDef().getJavaName());
            source.append(" = ");
            declaration.getInitializer().visit(this, arg);
            declaration.getInitializer().visit(this, arg);
            source.append(";\n");
        }
        else{
            type2String(declaration.getNameDef().getType());
            source.append(" ");
            source.append(declaration.getNameDef().getJavaName());
            source.append(";\n");
        }
        return declaration;
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
        source.append(identExpr.getNameDef().getJavaName());
        return identExpr;
    }

    @Override
    public Object visitIfStatement(IfStatement ifStatement, Object arg) throws PLCCompilerException {
        throw new CodeGenException("visitIfStatement not implemented yet");
    }

    @Override
    public Object visitLValue(LValue lValue, Object arg) throws PLCCompilerException {
        source.append(lValue.getNameDef().getJavaName());
        return lValue;
    }

    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCCompilerException {
        throw new CodeGenException("visitNameDef not implemented yet");
    }

    @Override
    public Object visitNumLitExpr(NumLitExpr numLitExpr, Object arg) throws PLCCompilerException {
        source.append(numLitExpr.getText());
        return numLitExpr;
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
        source.append("{ \n\tpublic static ");
        type2String(program.getType());
        source.append(" apply(");
        for(NameDef i : program.getParams()){
            type2String(i.getType());
            source.append(" ");
            source.append(i.getJavaName());
            if(paramSize > 1 && i != program.getParams().get(paramSize - 1)) {
                source.append(", ");
            }
        }
        source.append(")");
        symbolTable.enterScope();
        program.getBlock().visit(this, arg);
        symbolTable.closeScope();
        source.append("}");
        imports.append("package edu.ufl.cise.cop4020fa23;\n");
        if(consoleIO) {
            imports.append("import edu.ufl.cise.cop4020fa23.runtime.ConsoleIO;\n");
        }
        if(math){
            imports.append("import java.lang.Math;\n");
        }
        imports.append(source);
        return imports.toString();
    }
    public void type2String(Type type) {
        String typeString = type.toString();
        switch (typeString) {
            case ("INT"):
                source.append("int");
                break;
            case ("BOOLEAN"):
                source.append("boolean");
                break;
            case ("IMAGE"):
                source.append("image");
                break;
            case ("VOID"):
                source.append("void");
                break;
            case ("PIXEL"):
                source.append("pixel");
                break;
            case ("STRING"):
                source.append("String");
                break;
        }
    }

    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws PLCCompilerException {
        source.append("\t".repeat(Math.max(0, symbolTable.next_num)));
        source.append("return (");
        returnStatement.getE().visit(this,arg);
        source.append(");\n");
        return returnStatement;
    }

    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws PLCCompilerException {
        source.append(stringLitExpr.getText());
        return stringLitExpr;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws PLCCompilerException {
//        source.append("\t".repeat(Math.max(0, symbolTable.next_num)));
//        source.append("(");
        kind2String(unaryExpr.getOp());
        source.append("(");
        unaryExpr.getExpr().visit(this,arg);
        source.append(")");
//        source.append(")");
        return unaryExpr;
    }

    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws PLCCompilerException {
        consoleIO = true;
        source.append("\t".repeat(Math.max(0, symbolTable.next_num)));
        source.append("ConsoleIO.write(");
        writeStatement.getExpr().visit(this,arg);
        source.append(");\n");
        return writeStatement;
    }

    @Override
    public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws PLCCompilerException {
        bool2String(booleanLitExpr.getText());
        return booleanLitExpr;
    }

    public void bool2String(String bool){
        switch(bool){
            case "TRUE":
                source.append("true");
                break;
            case "FALSE":
                source.append("false");
                break;
        }
    }

    @Override
    public Object visitConstExpr(ConstExpr constExpr, Object arg) throws PLCCompilerException {
        throw new CodeGenException("visitConstExpr not implemented yet");
    }
}
