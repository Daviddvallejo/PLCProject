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

import edu.ufl.cise.cop4020fa23.exceptions.LexicalException;

import static edu.ufl.cise.cop4020fa23.Kind.*;


public class Lexer implements ILexer {

	String input;

//	 Enum for state
	private enum State { START, HAVE_POUND, IN_COMMENT, LETTER, HAS_EQUAL, HAS_LSQUARE, HAS_BITAND, HAS_BITOR, HAS_TIMES, HAS_LT, HAS_COLON, HAS_GT, HAS_MINUS, HAS_QUOTE, NUM_LIT }
	private String[] boolean_lit = {"TRUE", "FALSE"};
	private String[] constant = {"Z", "BLACK", "BLUE", "CYAN", "DARK_GRAY", "GRAY", "GREEN", "LIGHT_GRAY", "MAGENTA", "ORANGE", "PINK", "RED", "WHITE", "YELLOW"};
	private String[] reserved = {"image", "pixel", "int", "string", "void", "boolean", "write", "height", "width", "if", "fi", "do", "od", "red", "green", "blue"};

	private char[] separator = {',', ';', '?', ':', '(', ')', '<', '>', '[', ']', '=', '!', '&', '|', '+', '-', '*', '/', '%', '^', '#'};

	private int pos;

	private int line;
	private int column;

	public Lexer(String input) {
		this.input = input;
		this.pos = -1;
		this.line = 1;
		this.column = 0;
	}

	@Override
	public IToken next() throws LexicalException {
		// taken from lexer implementation slides
		// while loop runs til a new token is recognized and returned
		State state = State.START;
		int startPos = 0;
		char currentChar;
		String token = "";
		int startColumn = 0;
		int startLine = 0;
		while (true) {
			pos++;
			column++;
			currentChar = 0;
			if (pos < input.length()) {
				currentChar = input.charAt(pos);
				if (currentChar != ' ' && currentChar != '\t' && currentChar != '\n' && currentChar != '\r'){
					token += currentChar;
				}
			}
			int length = 0; //length variable for tokens that are more than 1 character

			// do something depending on current state
			switch(state){
				case START -> {
					startPos = pos; // save starting pos of the token
					startColumn = column;
					startLine = line;

					length = 0; //length variable for tokens that are more than 1 character

					// change the state from START depending on what currentChar is
					switch (currentChar) {
						// handle whitespace at START
						case '\n' -> {
							column = 0;
							line++;
						}
						case ' ', '\t', '\r' -> {
						}
						// repeat for all single op / separators, maybe make this less repetitive
						case ',' -> {
							return new Token(COMMA, startPos, 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
						}
						case ';' -> {
							return new Token(SEMI, startPos, 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
						}
						case '?' -> {
							return new Token(QUESTION, startPos, 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
						}
						case ':' -> {
							state = State.HAS_COLON;
						}
						case '(' -> {
							return new Token(LPAREN, startPos, 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
						}
						case ')' -> {
							return new Token(RPAREN, startPos, 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
						}
						case '<' -> {
							state = State.HAS_LT;
						}
						case '>' -> {
							state = State.HAS_GT;
						}
						case '[' -> {
							state = State.HAS_LSQUARE;
						}
						case ']' -> {
							return new Token(RSQUARE, startPos, 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
						}
						case '=' -> {
							state = State.HAS_EQUAL;
						}
						case '!' -> {
							return new Token(BANG, startPos, 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
						}
						case '&' -> {
							state = State.HAS_BITAND;
						}
						case '|' -> {
							state = State.HAS_BITOR;
						}
						case '+' -> {
							return new Token(PLUS, startPos, 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
						}
						case '-' -> {
							state = State.HAS_MINUS;
						}
						case '*' -> {
							state = State.HAS_TIMES;
						}
						case '/' -> {
							return new Token(DIV, startPos, 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
						}
						case '%' -> {
							return new Token(MOD, startPos, 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
						}
						case '^' -> {
							return new Token(RETURN, startPos, 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
						}

						// start of string_lit
						case '"' -> {
							state = State.HAS_QUOTE;
						}

						// handle num_lit
						case '0' -> {
							return new Token(NUM_LIT, startPos, 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
						}
						case '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
							state = State.NUM_LIT;
						}

						//start of comment
						case '#' -> {
							state = State.HAVE_POUND;
						}
						case 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
								'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '_' -> {
							state = State.LETTER;
						}
						default -> {
							if (pos >= input.length()) {
								return new Token(EOF, pos, 0, input.toCharArray(), new SourceLocation(startLine, startColumn));
							}
							else {
								throw new LexicalException("invalid character");
							}
						}
					}

				}
				case LETTER ->{
					if ((currentChar >= 65 && currentChar <= 90) || (currentChar >= 97 && currentChar <= 122) || currentChar == '_' || (currentChar >= 48 && currentChar <= 57)) {

					}
					else{
						if (currentChar == ' ' || currentChar == '\t' || currentChar == '\n' || currentChar == '\r') {
							pos--;
							column--;
						}
						for (char c : separator){
							if (currentChar == c) {
								token = token.replace(Character.toString(currentChar), "");
								pos--;
								column--;
							}
						}
                        for (String s : boolean_lit) {
                            if (token.equals(s)) {
                                return new Token(BOOLEAN_LIT, startPos, pos - startPos + 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
                            }
                        }
						for(String s : constant) {
							if(token.equals(s)) {
								return new Token(CONST, startPos, pos - startPos + 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
							}
						}
						switch (token) {
							case "image" ->{
								return new Token(RES_image, startPos, pos - startPos + 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
							}
							case "pixel" ->{
								return new Token(RES_pixel, startPos, pos - startPos + 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
							}
							case "int" ->{
								return new Token(RES_int, startPos, pos - startPos + 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
							}
							case "string" ->{
								return new Token(RES_string, startPos, pos - startPos + 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
							}
							case "void" ->{
								return new Token(RES_void, startPos, pos - startPos + 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
							}
							case "boolean" ->{
								return new Token(RES_boolean, startPos, pos - startPos + 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
							}
							case "write" ->{
								return new Token(RES_write, startPos, pos - startPos + 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
							}
							case "height" ->{
								return new Token(RES_height, startPos, pos - startPos + 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
							}
							case "width" ->{
								return new Token(RES_width, startPos, pos - startPos + 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
							}
							case "if" ->{
								return new Token(RES_if, startPos, pos - startPos + 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
							}
							case "fi" ->{
								return new Token(RES_fi, startPos, pos - startPos + 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
							}
							case "do" ->{
								return new Token(RES_do, startPos, pos - startPos + 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
							}
							case "od" ->{
								return new Token(RES_od, startPos, pos - startPos + 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
							}
							case "red" ->{
								return new Token(RES_red, startPos, pos - startPos + 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
							}
							case "green" ->{
								return new Token(RES_green, startPos, pos - startPos + 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
							}
							case "blue" ->{
								return new Token(RES_blue, startPos, pos - startPos + 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
							}
							case "nil" ->{
								return new Token(RES_nil, startPos, pos - startPos + 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
							}
							default -> {
								return new Token(IDENT, startPos, pos - startPos + 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
							}
						}
					}
				}
				case HAVE_POUND -> {
                    if (currentChar == '#') {
                        state = State.IN_COMMENT;
                    } else {
                        throw new LexicalException("Lexical Exception");
                    }
				}
				case HAS_EQUAL -> {
                    if (currentChar == '=') {
                        return new Token(EQ, startPos, pos - startPos + 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
                    }
                    pos--;
					column--;
                    return new Token(ASSIGN, startPos, pos - startPos + 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
                }
				case HAS_LSQUARE -> {
                    if (currentChar == ']') {
                        return new Token(BOX, startPos, pos - startPos + 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
                    }
                    pos--;
					column--;
                    return new Token(LSQUARE, startPos, pos - startPos + 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
                }
				case HAS_BITAND -> {
                    if (currentChar == '&') {
                        return new Token(AND, startPos, pos - startPos + 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
                    }
                    pos--;
					column--;
                    return new Token(BITAND, startPos, pos - startPos + 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
                }
				case HAS_BITOR -> {
                    if (currentChar == '|') {
                        return new Token(OR, startPos, pos - startPos + 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
                    }
                    pos--;
					column--;
                    return new Token(BITOR, startPos, pos - startPos + 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
                }
				case HAS_TIMES -> {
                    if (currentChar == '*') {
                        return new Token(EXP, startPos, pos - startPos + 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
                    }
                    pos--;
					column--;
                    return new Token(TIMES, startPos, pos - startPos + 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
                }
				case HAS_LT -> {
                    if (currentChar == '=') {
                        return new Token(LE, startPos, pos - startPos + 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
                    }
					else if (currentChar == ':') {
						return new Token(BLOCK_OPEN, startPos, pos - startPos + 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
					}
                    pos--;
					column--;
                    return new Token(LT, startPos, pos - startPos + 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
                }
				case HAS_GT -> {
                    if (currentChar == '=') {
                        return new Token(GE, startPos, pos - startPos + 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
                    }
                    pos--;
					column--;
                    return new Token(GT, startPos, pos - startPos + 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
                }
				case HAS_COLON -> {
                    if (currentChar == '>') {
                        return new Token(BLOCK_CLOSE, startPos, pos - startPos + 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
                    }
                    pos--;
					column--;
                    return new Token(COLON, startPos, pos - startPos + 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
                }
				case HAS_MINUS -> {
                    if (currentChar == '>') {
                        return new Token(RARROW, startPos, pos - startPos + 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
                    }
                    pos--;
					column--;
                    return new Token(MINUS, startPos, pos - startPos + 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
                }
				case IN_COMMENT -> {
					if (!(currentChar >= 32 && currentChar <= 126)) {
						state = State.START;
						pos--;
						column--;
					}
				}
				case HAS_QUOTE -> {
					if (currentChar == '"'){
						return new Token(STRING_LIT, startPos, pos - startPos + 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
					}
					else if (!(currentChar >= 32 && currentChar <= 126)) {
						throw new LexicalException("Tried to add a non-printable char to a string_lit");
					}
				}
				case NUM_LIT -> {
					if (!(currentChar >= 48 && currentChar <= 57)){
						try {
							int i = Integer.parseInt(input.substring(startPos, pos));
						}
						catch (NumberFormatException nfe) {
							throw new LexicalException("Lexical Exception: Num_lit length exceeded.");
						}
						pos--;
						column--;
						return new Token(NUM_LIT, startPos, pos - startPos + 1, input.toCharArray(), new SourceLocation(startLine, startColumn));
					}
				}
				// this should be unreachable
				default -> throw new IllegalStateException("lexer bug");
				// handles EOF tokens
			}
		}
	}



}
