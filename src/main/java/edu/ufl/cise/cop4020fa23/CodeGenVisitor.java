package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.*;
import edu.ufl.cise.cop4020fa23.ast.Dimension;
import edu.ufl.cise.cop4020fa23.exceptions.CodeGenException;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;
import edu.ufl.cise.cop4020fa23.runtime.ConsoleIO;
import edu.ufl.cise.cop4020fa23.runtime.PixelOps;

import java.awt.*;
import java.lang.reflect.Field;
import java.util.List;

public class CodeGenVisitor implements ASTVisitor {

    StringBuilder source = new StringBuilder();
    StringBuilder imports = new StringBuilder();
    SymbolTable symbolTable = new SymbolTable();
    boolean consoleIO = false;
    boolean math = false;
    boolean image = false;
    boolean pixel = false;
    boolean file = false;
    boolean bufImage = false;
    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws PLCCompilerException {
        source.append("\t".repeat(Math.max(0, symbolTable.next_num)));
        Type ltype = assignmentStatement.getlValue().getNameDef().getType();;
        ChannelSelector channelSelector = assignmentStatement.getlValue().getChannelSelector();
        PixelSelector pixelSelector = assignmentStatement.getlValue().getPixelSelector();
        if(ltype == Type.PIXEL){
            if(channelSelector!= null){
                String color = channelSelector.firstToken.text();
                color = color.substring(0, 1).toUpperCase() + color.substring(1);
                assignmentStatement.getlValue().visit(this, arg);
                source.append(" = ");
                source.append("PixelOps.set");
                source.append(color);
                source.append("(");
                assignmentStatement.getlValue().visit(this, arg);
                source.append(", ");
                assignmentStatement.getE().visit(this, arg);
                source.append(")");
                pixel = true;
            }
            else{
                assignmentStatement.getlValue().visit(this,arg);
                source.append(" = ");
                assignmentStatement.getE().visit(this, "pixelAssignment");
            }
        }
        else if(ltype == Type.IMAGE){
            if(pixelSelector != null && channelSelector == null) {
                source.append("for(int ");
                assignmentStatement.getlValue().getPixelSelector().xExpr().visit(this, arg);
                source.append(" = 0; ");
                assignmentStatement.getlValue().getPixelSelector().xExpr().visit(this, arg);
                source.append(" < ");
                assignmentStatement.getlValue().getNameDef().visit(this, "assignment");
                source.append(".getWidth(); ");
                assignmentStatement.getlValue().getPixelSelector().xExpr().visit(this, arg);
                source.append("++){\n");
                symbolTable.enterScope();
                source.append("\t".repeat(Math.max(0, symbolTable.next_num)));
                source.append("for(int ");
                assignmentStatement.getlValue().getPixelSelector().yExpr().visit(this, arg);
                source.append(" = 0; ");
                assignmentStatement.getlValue().getPixelSelector().yExpr().visit(this, arg);
                source.append(" < ");
                assignmentStatement.getlValue().getNameDef().visit(this, "assignment");
                source.append(".getHeight(); ");
                assignmentStatement.getlValue().getPixelSelector().yExpr().visit(this, arg);
                source.append("++){\n");
                symbolTable.enterScope();
                source.append("\t".repeat(Math.max(0, symbolTable.next_num)));
                source.append("ImageOps.setRGB(");
                assignmentStatement.getlValue().getNameDef().visit(this, "assignment");
                source.append(", ");
                assignmentStatement.getlValue().getPixelSelector().xExpr().visit(this, arg);
                source.append(", ");
                assignmentStatement.getlValue().getPixelSelector().yExpr().visit(this, arg);
                source.append(", ");
                assignmentStatement.getE().visit(this,arg);
                source.append(");\n");
                symbolTable.closeScope();
                source.append("\t".repeat(Math.max(0, symbolTable.next_num)));
                source.append("}\n");
                symbolTable.closeScope();
                source.append("\t".repeat(Math.max(0, symbolTable.next_num)));
                source.append("}\n");
                image = true;
                bufImage = true;
                return assignmentStatement;
            }
            else{
                throw new CodeGenException("not implemented yet");
            }
        }
        else {
            assignmentStatement.getlValue().visit(this, arg);
            source.append(" = ");
            assignmentStatement.getE().visit(this, arg);
        }
        source.append(";\n");
        return assignmentStatement;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCCompilerException {
        Expr left = binaryExpr.getLeftExpr();
        Kind op = binaryExpr.getOpKind();
        Expr right = binaryExpr.getRightExpr();
        source.append("(");
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
            if(binaryExpr.getType() == Type.PIXEL && left.getType() == Type.PIXEL && right.getType() == Type.PIXEL){
                source.append("ImageOps.binaryPackedPixelPixelOp(");
                source.append("ImageOps.OP.");
                source.append(binaryExpr.getOpKind());
                source.append(", ");
                left.visit(this,arg);
                source.append(", ");
                right.visit(this,arg);
                image = true;
                source.append(")");
            }
            else if(binaryExpr.getType() == Type.PIXEL && left.getType() == Type.PIXEL && right.getType() == Type.INT){
                source.append("ImageOps.binaryPackedPixelIntOp(");
                source.append("ImageOps.OP.");
                source.append(binaryExpr.getOpKind());
                source.append(", ");
                left.visit(this,arg);
                source.append(", ");
                right.visit(this,arg);
                image = true;
                source.append(")");
            }
            else if(binaryExpr.getType() == Type.IMAGE && left.getType() == Type.IMAGE && right.getType() == Type.IMAGE){
                source.append("ImageOps.binaryImageImageOp(");
                source.append("ImageOps.OP.");
                source.append(binaryExpr.getOpKind());
                source.append(", ");
                left.visit(this,arg);
                source.append(", ");
                right.visit(this,arg);
                image = true;
                source.append(")");
            }
            else if(binaryExpr.getType() == Type.IMAGE && left.getType() == Type.IMAGE && right.getType() == Type.PIXEL){
                source.append("ImageOps.binaryImagePixelOp(");
                source.append("ImageOps.OP.");
                source.append(binaryExpr.getOpKind());
                source.append(", ");
                left.visit(this,arg);
                source.append(", ");
                right.visit(this,arg);
                image = true;
                source.append(")");
            }
            else {
                left.visit(this, arg);
                source.append(" ");
                kind2String(op);
                source.append(" ");
                right.visit(this, arg);
            }
        }
        source.append(")");
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
            if(arg == "do"){
                source.append("\t".repeat(Math.max(0, symbolTable.next_num)));
                source.append("continue$0 = true;\n");
            }
            elem.visit(this, arg);
        }
        source.append("\t".repeat(Math.max(0, symbolTable.current_num)));
        source.append("}");

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
        if(arg == "pixelPostFix"){
            pixel = true;
            source.append("PixelOps.");
            source.append(channelSelector.firstToken().text());
        }
        else {
            throw new CodeGenException("visitChannelSelector aspect not implemented yet");
        }
        return channelSelector;
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
        source.append(" )");
        return conditionalExpr;
    }

    @Override
    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCCompilerException {
        source.append("\t".repeat(Math.max(0, symbolTable.next_num)));
        if(declaration.getInitializer() != null){
            declaration.getNameDef().visit(this,arg);
            source.append(" = ");
            if(declaration.getNameDef().getType() == Type.IMAGE && declaration.getNameDef().getDimension() == null){
                bufImage = true;
                image = true;
                declaration.getInitializer().visit(this, "image");
            }
            else if(declaration.getNameDef().getType() == Type.IMAGE && declaration.getNameDef().getDimension() != null){
                source.append("ImageOps.copyAndResize(");
                declaration.getInitializer().visit(this, arg);
                source.append(", ");
                declaration.getNameDef().getDimension().getWidth().visit(this, arg);
                source.append(", ");
                declaration.getNameDef().getDimension().getHeight().visit(this, arg);
                source.append(")");
                image = true;
            }
            else {
                declaration.getInitializer().visit(this, arg);
            }
            source.append(";\n");
        }
        else{
            if(declaration.getNameDef().getType() == Type.IMAGE){
                declaration.getNameDef().visit(this, arg);
                source.append(" = ImageOps.makeImage(");
                declaration.getNameDef().getDimension().getWidth().visit(this, arg);
                source.append(", ");
                declaration.getNameDef().getDimension().getHeight().visit(this, arg);
                source.append(")");
                image = true;
            }
            else {
                declaration.getNameDef().visit(this, arg);
            }
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
        source.append("\t".repeat(Math.max(0, symbolTable.next_num)));
        source.append("boolean continue$0 = false;\n");
        source.append("\t".repeat(Math.max(0, symbolTable.next_num)));
        source.append("while(!continue$0){\n");
        symbolTable.enterScope();
        List<GuardedBlock> guardList = doStatement.getGuardedBlocks();
        int guardSize = guardList.size();
        source.append("\t".repeat(Math.max(0, symbolTable.next_num)));
        source.append("if(");
        for(GuardedBlock guardBlock : guardList){
            if(guardSize > 1 && guardBlock != guardList.get(0)) {
                source.append("\n");
                source.append("\t".repeat(Math.max(0, symbolTable.next_num)));
                source.append("if(");
            }
            guardBlock.visit(this,"do");
        }
        source.append(";\n");
        source.append("\t".repeat(Math.max(0, symbolTable.current_num)));
        source.append("}");
        symbolTable.closeScope();
        return doStatement;
    }

    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCCompilerException {
        Expr red = expandedPixelExpr.getRed();
        Expr blue = expandedPixelExpr.getBlue();
        Expr green = expandedPixelExpr.getGreen();
        source.append("PixelOps.pack(");
        red.visit(this, arg);
        source.append(", ");
        green.visit(this, arg);
        source.append(", ");
        blue.visit(this, arg);
        pixel = true;
        source.append(")");
        return expandedPixelExpr;
    }

    @Override
    public Object visitGuardedBlock(GuardedBlock guardedBlock, Object arg) throws PLCCompilerException {
        guardedBlock.getGuard().visit(this, arg);
        source.append(")");
        symbolTable.enterScope();
        guardedBlock.getBlock().visit(this,arg);
        symbolTable.closeScope();
        return guardedBlock;
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCCompilerException {
        Type nameDefType = identExpr.getNameDef().getType();
        if(arg == "pixelAssignment"){
            source.append("PixelOps.pack(");
            source.append(identExpr.getNameDef().getJavaName());
            source.append(", ");
            source.append(identExpr.getNameDef().getJavaName());
            source.append(", ");
            source.append(identExpr.getNameDef().getJavaName());
            source.append(")");
            pixel = true;
        }
        else if(arg == "image" && nameDefType == Type.STRING){
            source.append("FileURLIO.readImage(");
            source.append(identExpr.getNameDef().getJavaName());
            source.append(")");
            file = true;
        }
        else if(arg == "image" && nameDefType == Type.IMAGE){
            source.append("ImageOps.cloneImage(");
            source.append(identExpr.getNameDef().getJavaName());
            source.append(")");
            image = true;
        }
        else{
            source.append(identExpr.getNameDef().getJavaName());
        }
        return identExpr;
    }

    @Override
    public Object visitIfStatement(IfStatement ifStatement, Object arg) throws PLCCompilerException {
        List<GuardedBlock> guardList = ifStatement.getGuardedBlocks();
        int guardSize = guardList.size();
        source.append("\t".repeat(Math.max(0, symbolTable.next_num)));
        source.append("if(");
        for(GuardedBlock guardBlock : guardList){
            if(guardSize > 1 && guardBlock != guardList.get(0)) {
                source.append("\n");
                source.append("\t".repeat(Math.max(0, symbolTable.next_num)));
                source.append("else if(");
            }
            guardBlock.visit(this,arg);
        }
        source.append(";\n");
        return ifStatement;
    }

    @Override
    public Object visitLValue(LValue lValue, Object arg) throws PLCCompilerException {
        source.append(lValue.getNameDef().getJavaName());
        return lValue.getType();
    }

    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCCompilerException {
        if(arg == "assignment"){
            source.append(nameDef.getJavaName());
        }
        else if(nameDef.getType() != Type.IMAGE) {
            type2String(nameDef.getType());
            source.append(" ");
            source.append(nameDef.getJavaName());
        }
        else{
            source.append("final ");
            type2String(nameDef.getType());
            source.append(" ");
            source.append(nameDef.getJavaName());
        }
        return nameDef;
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
        Type eType = postfixExpr.primary().getType();
        if(eType == Type.PIXEL){
            postfixExpr.channel().visit(this,"pixelPostFix");
            source.append("(");
            postfixExpr.primary().visit(this,"pixelPostFix");
            source.append(")");
        }
        else{
            throw new CodeGenException("eType: Image not implemented yet in postFixExpr");
        }
        return postfixExpr;
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
        source.append("\n}");
        imports.append("package edu.ufl.cise.cop4020fa23;\n");
        if(consoleIO) {
            imports.append("import edu.ufl.cise.cop4020fa23.runtime.ConsoleIO;\n");
        }
        if(math){
            imports.append("import java.lang.Math;\n");
        }
        if(image){
            imports.append("import edu.ufl.cise.cop4020fa23.runtime.ImageOps;\n");
        }
        if(pixel){
            imports.append("import edu.ufl.cise.cop4020fa23.runtime.PixelOps;\n");
        }
        if(file){
            imports.append("import edu.ufl.cise.cop4020fa23.runtime.FileURLIO;\n");
        }
        if(bufImage){
            imports.append("import java.awt.image.BufferedImage;\n");
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
                source.append("Boolean");
                break;
            case ("IMAGE"):
                source.append("BufferedImage");
                break;
            case ("VOID"):
                source.append("void");
                break;
            case ("PIXEL"):
                source.append("int");
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
    public Object visitConstExpr(ConstExpr constExpr, Object arg) throws PLCCompilerException{
        String name = constExpr.getName();
        if(name.equals("Z")){
            source.append("255");
        }
        else{
            source.append("0x");
            Color color = string2Color(name);
            source.append(Integer.toHexString(color.getRGB()));
        }
        return constExpr;
    }

    public Color string2Color(String s) throws PLCCompilerException{
        switch(s){
            case "black":
                return Color.black;
            case "BLACK":
                return Color.BLACK;
            case "blue":
                return Color.blue;
            case "BLUE":
                return Color.BLUE;
            case "cyan":
                return Color.cyan;
            case "CYAN":
                return Color.CYAN;
            case "darkGray":
                return Color.darkGray;
            case "DARK_GRAY":
                return Color.DARK_GRAY;
            case "gray":
                return Color.gray;
            case "GRAY":
                return Color.GRAY;
            case "green":
                return Color.green;
            case "GREEN":
                return Color.GREEN;
            case "LIGHT_GRAY":
                return Color.LIGHT_GRAY;
            case "lightGray":
                return Color.lightGray;
            case "magenta":
                return Color.magenta;
            case "MAGENTA":
                return Color.MAGENTA;
            case "orange":
                return Color.orange;
            case "ORANGE":
                return Color.ORANGE;
            case "pink":
                return Color.pink;
            case "PINK":
                return Color.PINK;
            case "red":
                return Color.red;
            case "RED":
                return Color.RED;
            case "white":
                return Color.white;
            case "WHITE":
                return Color.WHITE;
            case "yellow":
                return Color.yellow;
            case "YELLOW":
                return Color.YELLOW;
        }
        throw new CodeGenException("This shouldn't be possible");
    }

}
