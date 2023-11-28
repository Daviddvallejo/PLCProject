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

import java.util.Arrays;

import edu.ufl.cise.cop4020fa23.ast.AST;
import edu.ufl.cise.cop4020fa23.ast.BinaryExpr;
import edu.ufl.cise.cop4020fa23.ast.BooleanLitExpr;
import edu.ufl.cise.cop4020fa23.ast.ChannelSelector;
import edu.ufl.cise.cop4020fa23.ast.ConditionalExpr;
import edu.ufl.cise.cop4020fa23.ast.ConstExpr;
import edu.ufl.cise.cop4020fa23.ast.ExpandedPixelExpr;
import edu.ufl.cise.cop4020fa23.ast.Expr;
import edu.ufl.cise.cop4020fa23.ast.IdentExpr;
import edu.ufl.cise.cop4020fa23.ast.NumLitExpr;
import edu.ufl.cise.cop4020fa23.ast.PixelSelector;
import edu.ufl.cise.cop4020fa23.ast.PostfixExpr;
import edu.ufl.cise.cop4020fa23.ast.StringLitExpr;
import edu.ufl.cise.cop4020fa23.ast.UnaryExpr;
import edu.ufl.cise.cop4020fa23.exceptions.LexicalException;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;
import edu.ufl.cise.cop4020fa23.exceptions.SyntaxException;

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

public class ExpressionParser implements IParser {
	
	final ILexer lexer;
	private IToken t;

	/**
	 * @param lexer
	 * @throws LexicalException 
	 */
	public ExpressionParser(ILexer lexer) throws LexicalException {
		super();
		this.lexer = lexer;
		t = lexer.next();
	}


	@Override
	public AST parse() throws PLCCompilerException {
		Expr e = expr();
		return e;
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
		IToken firstToken = t;
		match(COLON);
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

    

}
