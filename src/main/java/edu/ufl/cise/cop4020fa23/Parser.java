/*Copyright 2023 by Beverly A Sanders
 * 
 * This code is provided for solely for use of students in COP4020 Programming Language Concepts at the 
 * University of Florida during the fall semester 2023 as part of the course project.  
 * 
 * No other use is authorized. 
 * 
 * This code may not be posted on a public web site either during or after the course.  
 */
package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.*;
import edu.ufl.cise.cop4020fa23.exceptions.LexicalException;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;
import edu.ufl.cise.cop4020fa23.exceptions.SyntaxException;

import java.util.ArrayList;
import java.util.List;

import static edu.ufl.cise.cop4020fa23.Kind.*;

/**
Expr::=  ConditionalExpr | LogicalOrExpr    
ConditionalExpr ::=  ?  Expr  :  Expr  :  Expr 
LogicalOrExpr ::= LogicalAndExpr (    (   |   |   ||   ) LogicalAndExpr)*
LogicalAndExpr ::=  ComparisonExpr ( (   &   |  &&   )  ComparisonExpr)*
ComparisonExpr ::= PowExpr ( (< | > | == | <= | >=) PowExpr)*
PowExpr ::= AdditiveExpr ** PowExpr |   AdditiveExpr
AdditiveExpr ::= MultiplicativeExpr ( ( + | -  ) MultiplicativeExpr )*
MultiplicativeExpr ::= UnaryExpr (( * |  /  |  % ) UnaryExpr)*
UnaryExpr ::=  ( ! | - | length | width) UnaryExpr  |  UnaryExprPostfix
UnaryExprPostfix::= PrimaryExpr (PixelSelector | ε ) (ChannelSelector | ε )
PrimaryExpr ::=STRING_LIT | NUM_LIT |  IDENT | ( Expr ) | Z 
    ExpandedPixel  
ChannelSelector ::= : red | : green | : blue
PixelSelector  ::= [ Expr , Expr ]
ExpandedPixel ::= [ Expr , Expr , Expr ]
Dimension  ::=  [ Expr , Expr ]                         

 */

public class 	Parser implements IParser {
	
	final ILexer lexer;
	private IToken t;

	/**
	 * @param lexer
	 * @throws LexicalException 
	 */
	public Parser(ILexer lexer) throws LexicalException {
		super();
		this.lexer = lexer;
		t = lexer.next();
	}

	@Override
	public AST parse() throws PLCCompilerException {
        return Program();
	}

	protected boolean isKind(Kind kind) {
		return t.kind() == kind;
	}

	protected boolean isKind(Kind... kinds) {
		for (Kind k : kinds) {
			if (k == t.kind()) {
				return true;
			}
		}
		return false;
	}

	private void match(Kind kind) throws SyntaxException, LexicalException {
		if (isKind(kind)) {
			t = lexer.next();
		}
		else {
			throw new SyntaxException();
		}
	}

	private void consume() throws LexicalException {
		t = lexer.next();
	}


	private Program Program() throws PLCCompilerException {
		IToken firstToken = t;
		IToken type = Type();
		IToken name = t;
		match(IDENT);
		match(LPAREN);
		List<NameDef> params = new ArrayList<>();
		if (isKind(RES_image, RES_pixel, RES_int, RES_string, RES_void, RES_boolean)) {
			params = ParamList();
		}
		match(RPAREN);
		Block block = block();
		if(!isKind(EOF)){
			throw new SyntaxException("Block Syntax exception");
		}
		return new Program(firstToken, type, name, params, block);
	}

	private Block block() throws PLCCompilerException {
		IToken firstToken = t;
		match(BLOCK_OPEN);
		List<Block.BlockElem> elems = new ArrayList<>();
		while(!isKind(BLOCK_CLOSE)) {
			if(isKind(IDENT, RES_write, RES_if, RES_do, RETURN, BLOCK_OPEN)){
				Statement statement = Statement();
				elems.add(statement);
				match(SEMI);
			}
			else{
				Declaration declaration = Declaration();
				elems.add(declaration);
				match(SEMI);
			}
		}
		match(BLOCK_CLOSE);
		return new Block(firstToken, elems);
	}

	private List<NameDef> ParamList() throws PLCCompilerException {
		List<NameDef> params = new ArrayList<>();
		params.add(NameDef());
		while (isKind(COMMA)) {
			consume();
			params.add(NameDef());
		}
		return params;
	}

	private NameDef NameDef() throws PLCCompilerException {
		IToken firstToken = t;
		IToken type = Type();
		IToken ident = null;
		Dimension dimension = null;
		if (isKind(IDENT)) {
			ident = t;
			consume();
		}
		else if (isKind(LSQUARE)) {
			dimension = Dimension();
			if (isKind(IDENT)) {
				ident = t;
			}
			else {
				throw new SyntaxException("Syntax Exception in NameDef");
			}
			consume();
		}
		else{
			throw new SyntaxException("Syntax Exception in NameDef");
		}
		return new NameDef(firstToken, type, dimension, ident);
	}

	private IToken Type() throws PLCCompilerException {
		IToken firstToken = t;
		if (isKind(RES_image, RES_pixel, RES_int, RES_string, RES_void, RES_boolean)) {
			consume();
			return firstToken;
		}
		else {
			throw new SyntaxException();
		}
	}

	private Declaration Declaration() throws PLCCompilerException{
		IToken firstToken = t;
		NameDef nameDef = NameDef();
		if(isKind(ASSIGN)){
			consume();
			Expr initializer = expr();
			return new Declaration(firstToken, nameDef, initializer);
		}
		return new Declaration(firstToken, nameDef, null);

	}

	private Expr expr() throws PLCCompilerException {
		IToken firstToken = t;
		Expr e = null;

		// made this next line like this for testing for now, feel free to modify
		if(isKind(QUESTION)){
			e = ConditionalExpr();
		}
		else{
			e = LogicalOrExpr();
		}

		return e;
	}

	private Expr ConditionalExpr() throws PLCCompilerException {
		IToken firstToken = t;
		match(QUESTION);
		Expr guard = expr();
		match(RARROW);
		Expr trueExpr = expr();
		match(COMMA);
		Expr falseExpr = expr();
		return new ConditionalExpr(firstToken, guard, trueExpr,falseExpr);
	}

	private Expr LogicalOrExpr() throws PLCCompilerException {
		IToken firstToken = t;
		Expr left = LogicalAndExpr();

		while (isKind(OR, BITOR)) {
			IToken op = t;
			consume();
			Expr right = LogicalAndExpr();
			left = new BinaryExpr(firstToken, left, op, right);
		}

		return left;
	}

	private Expr LogicalAndExpr() throws PLCCompilerException {
		IToken firstToken = t;
		Expr left = ComparisonExpr();
		while (isKind(AND, BITAND)) {
			IToken op = t;
			consume();
			Expr right = ComparisonExpr();
			left = new BinaryExpr(firstToken, left, op, right);
		}
		return left;
	}

	private Expr ComparisonExpr() throws PLCCompilerException {
		IToken firstToken = t;
		Expr left = PowExpr();
		while (isKind(LT, LE, EQ, GT, GE)) {
			IToken op = t;
			consume();
			Expr right = PowExpr();
			left = new BinaryExpr(firstToken, left, op, right);
		}
		return left;
	}

	private Expr PowExpr() throws PLCCompilerException {
		IToken firstToken = t;
		Expr left = AdditiveExpr();
		if (isKind(EXP)) {
			IToken op = t;
			consume();
			Expr right = PowExpr();
			left = new BinaryExpr(firstToken, left, op, right);
		}
		return left;
	}

	private Expr AdditiveExpr() throws PLCCompilerException {
		IToken firstToken = t;
		Expr left = MultiplicativeExpr();
		while (isKind(PLUS, MINUS)) {
			IToken op = t;
			consume();
			Expr right = MultiplicativeExpr();
			left = new BinaryExpr(firstToken, left, op, right);
		}
		return left;
	}

	private Expr MultiplicativeExpr() throws PLCCompilerException {
		IToken firstToken = t;
		Expr left = UnaryExpr();
		while (isKind(TIMES, DIV, MOD)) {
			IToken op = t;
			consume();
			Expr right = UnaryExpr();
			left = new BinaryExpr(firstToken, left, op, right);
		}
		return left;
	}


	private Expr UnaryExpr() throws PLCCompilerException {
		IToken firstToken = t;
		IToken op = null;
		Expr e = null;
		if (isKind(BANG, MINUS, RES_width, RES_height)) {
			op = t;
			consume();
			e = UnaryExpr();
			return new UnaryExpr(firstToken, op, e);
		}
		else {
			e = PostfixExpr();
			return e;
		}
	}

	private Expr PostfixExpr() throws PLCCompilerException {
		IToken firstToken = t;
		Expr primary = PrimaryExpr();
		PixelSelector pixel = null;
		ChannelSelector channel = null;
		if (isKind(LSQUARE, COLON)){
			if (isKind(LSQUARE)) {
				pixel = PixelSelector();
			}
			if (isKind(COLON)) {
				channel = ChannelSelector();
				consume();
			}
			return new PostfixExpr(firstToken, primary, pixel, channel);
		}
		else {
			return primary;
		}
	}

	private Expr PrimaryExpr() throws PLCCompilerException {
		IToken firstToken = t;
		Expr e = null;
		if (isKind(STRING_LIT)) {
			e = new StringLitExpr(firstToken);
			consume();
		}
		else if (isKind(NUM_LIT)) {
			e = new NumLitExpr(firstToken);
			consume();
		}
		else if (isKind(BOOLEAN_LIT)) {
			e = new BooleanLitExpr(firstToken);
			consume();
		}
		else if (isKind(IDENT)) {
			e = new IdentExpr(firstToken);
			consume();
		}
		else if (isKind(LPAREN)) {
			consume();
			e = expr();
			match(RPAREN);
		}
		else if (isKind(CONST)) {
			e = new ConstExpr(firstToken);
			consume();
		}
		else if (isKind(LSQUARE)) {
			e = ExpandedPixelExpr();
		}
		else {
			throw new SyntaxException();
		}
		return e;
	}

	private ChannelSelector ChannelSelector() throws PLCCompilerException {
		match(COLON);
		IToken firstToken = t;
		if (isKind(RES_red, RES_green, RES_blue)){
			return new ChannelSelector(firstToken, t);
		}
		else {
			throw new SyntaxException("Syntax Exception in ChannelSelector()");
		}
	}

	private PixelSelector PixelSelector() throws PLCCompilerException {
		IToken firstToken = t;
		match(LSQUARE);
		Expr x = expr();
		match(COMMA);
		Expr y = expr();
		match(RSQUARE);
        return new PixelSelector(firstToken, x, y);
	}

	private Expr ExpandedPixelExpr() throws PLCCompilerException {
		IToken firstToken = t;
		match(LSQUARE);
		Expr red = expr();
		match(COMMA);
		Expr grn = expr();
		match(COMMA);
		Expr blu = expr();
		match(RSQUARE);
        return new ExpandedPixelExpr(firstToken, red, grn, blu);
	}

	private Dimension Dimension() throws PLCCompilerException {
		IToken firstToken = t;
		match(LSQUARE);
		Expr x = expr();
		match(COMMA);
		Expr y = expr();
		match(RSQUARE);
		return new Dimension(firstToken, x, y);
	}

	private LValue LValue() throws PLCCompilerException {
		IToken firstToken = t;
		IToken name = null;
		if (isKind(IDENT)){
			name = t;
			consume();
		}
		else {
			throw new SyntaxException("Syntax error in LValue");
		}
		PixelSelector pixel = null;
		ChannelSelector channel = null;
		if (isKind(LSQUARE, COLON)){
			if (isKind(LSQUARE)) {
				pixel = PixelSelector();
			}
			if (isKind(COLON)) {
				channel = ChannelSelector();
				consume();
			}
		}
		return new LValue(firstToken, name, pixel, channel);
	}

	private Statement Statement() throws PLCCompilerException {
		IToken firstToken = t;
		if(isKind(IDENT)){
			LValue LValue = LValue();
			match(ASSIGN);
			Expr expr = expr();
			return new AssignmentStatement(firstToken, LValue, expr);
		}
		else if (isKind(RES_write)) {
			consume();
			Expr expr = expr();
			return new WriteStatement(firstToken, expr);
		}
		else if (isKind(RES_do)) {
			consume();
			List<GuardedBlock> guardedBlocks = new ArrayList<>();
			GuardedBlock guardedBlock = GuardedBlock();
			guardedBlocks.add(guardedBlock);
			while(!isKind(RES_od) && !isKind(EOF)){
				match(BOX);
				guardedBlock = GuardedBlock();
				guardedBlocks.add(guardedBlock);
			}
			match(RES_od);
			return new DoStatement(firstToken, guardedBlocks);
		}
		else if (isKind(RES_if)) {
			consume();
			List<GuardedBlock> guardedBlocks = new ArrayList<>();
			GuardedBlock guardedBlock = GuardedBlock();
			guardedBlocks.add(guardedBlock);
			while(!isKind(RES_fi) && !isKind(EOF)){
				match(BOX);
				guardedBlock = GuardedBlock();
				guardedBlocks.add(guardedBlock);
			}
			match(RES_fi);
			return new IfStatement(firstToken, guardedBlocks);
		}
		else if (isKind(RETURN)) {
			consume();
			Expr expr = expr();
			return new ReturnStatement(firstToken, expr);
		}
		else {
			Block block = BlockStatement();
			return new StatementBlock(firstToken, block);
		}

	}

	private GuardedBlock GuardedBlock() throws PLCCompilerException {
		IToken firstToken = t;
		Expr guard = expr();
		match(RARROW);
		Block block = block();
		return new GuardedBlock(firstToken, guard, block);
	}

	private Block BlockStatement() throws PLCCompilerException {
		return block();
	}
    

}
