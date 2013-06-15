package parser;

import lexer.Lexer;
import lexer.Token;
import lexer.Token.Kind;

public class Parser
{
  Lexer lexer;
  Token current;

  public Parser(String fname, java.io.InputStream fstream)
  {
    lexer = new Lexer(fname, fstream);
    current = lexer.nextToken();
  }

  // /////////////////////////////////////////////
  // utility methods to connect the lexer
  // and the parser.

  private void advance()
  {
		current = lexer.nextToken();
}

  private void eatToken(Kind kind)
  {
    if (kind == current.kind)
    { 
    	System.out.println(current.toString());        
    	advance();
    }
    else {
      System.out.println("Expects: " + kind.toString());
      System.out.println("But got: " + current.kind.toString() +"\t" + current.lexeme +  "\tlineNum:" + current.lineNum);
      System.exit(1);
    }
  }

  private void error()
  {
    System.out.println("Syntax error: compilation aborting...\n");
    System.exit(1);
    return;
  }

  // ////////////////////////////////////////////////////////////
  // below are method for parsing.

  // A bunch of parsing methods to parse expressions. The messy
  // parts are to deal with precedence and associativity.

  // ExpList -> Exp ExpRest*
  // ->
  // ExpRest -> , Exp
  private void parseExpList()
  {
    if (current.kind == Kind.TOKEN_RPAREN)
      return;
    parseExp();    
    while (current.kind == Kind.TOKEN_COMMER) {
      eatToken(Kind.TOKEN_COMMER);
      parseExp();
    }
    return;
  }

  // AtomExp -> (exp)
  // -> INTEGER_LITERAL
  // -> true
  // -> false
  // -> this
  // -> id
  // -> new int [exp]
  // -> new id ()
  private void parseAtomExp()
  {
    switch (current.kind) {
    case TOKEN_LPAREN:
      eatToken(Kind.TOKEN_LPAREN);
      parseExp();
      eatToken(Kind.TOKEN_RPAREN);
      return;
    case TOKEN_NUM:
      eatToken(Kind.TOKEN_NUM);
      return;
    case TOKEN_TRUE:
    	eatToken(Kind.TOKEN_TRUE);
    	return;
    case TOKEN_FALSE:
    	eatToken(Kind.TOKEN_FALSE);
    	return;
    case TOKEN_THIS:
    	eatToken(Kind.TOKEN_THIS);
      return;
    case TOKEN_ID:
    	eatToken(Kind.TOKEN_ID);
      return;
    case TOKEN_NEW: {
 //advance();
    	eatToken(Kind.TOKEN_NEW);
      switch (current.kind) {
      case TOKEN_INT:
        eatToken(Kind.TOKEN_INT);
        eatToken(Kind.TOKEN_LBRACK);
        parseExp();
        eatToken(Kind.TOKEN_RBRACK);
        return;
      case TOKEN_ID:
        eatToken(Kind.TOKEN_ID);
        eatToken(Kind.TOKEN_LPAREN);
        eatToken(Kind.TOKEN_RPAREN);

        return;
      default:
        error();
        return;
      }
    }
    default:
      error();
      return;
    }
  }

  // NotExp -> AtomExp
  // -> AtomExp .id (expList)
  // -> AtomExp [exp]
  // -> AtomExp .length
  private void parseNotExp()
  {
    parseAtomExp();
    while (current.kind == Kind.TOKEN_DOT || current.kind == Kind.TOKEN_LBRACK) {
      if (current.kind == Kind.TOKEN_DOT) {
        eatToken(Kind.TOKEN_DOT);
        if (current.kind == Kind.TOKEN_LENGTH) {
          advance();
          return;
        }
        eatToken(Kind.TOKEN_ID);
        eatToken(Kind.TOKEN_LPAREN);
        parseExpList();
        eatToken(Kind.TOKEN_RPAREN);
      } else {
        advance();
        parseExp();
        eatToken(Kind.TOKEN_RBRACK);
      }
    }
    return;
  }

  // TimesExp -> ! TimesExp
  // -> NotExp
  private void parseTimesExp()
  {
    while (current.kind == Kind.TOKEN_NOT) {
      advance();
    }
    parseNotExp();
    return;
  }

  // AddSubExp -> TimesExp * TimesExp
  // -> TimesExp
  private void parseAddSubExp()
  {
    parseTimesExp();
    while (current.kind == Kind.TOKEN_TIMES) {
      advance();
      parseTimesExp();
    }
    return;
  }

  // LtExp -> AddSubExp + AddSubExp
  // -> AddSubExp - AddSubExp
  // -> AddSubExp
  private void parseLtExp()
  {
    parseAddSubExp();
    while (current.kind == Kind.TOKEN_ADD || current.kind == Kind.TOKEN_SUB) {
      advance();
      parseAddSubExp();
    }
    return;
  }

  // AndExp -> LtExp < LtExp
  // -> LtExp
  private void parseAndExp()
  {
    parseLtExp();
    while (current.kind == Kind.TOKEN_LT) {
      eatToken(Kind.TOKEN_LT);
      parseLtExp();
    }
    return;
  }

  // Exp -> AndExp && AndExp
  // -> AndExp
  private void parseExp()
  {
    parseAndExp();
    while (current.kind == Kind.TOKEN_AND) {
      eatToken(Kind.TOKEN_AND);
      parseAndExp();
    }
    return;
  }

  // Statement -> { Statement* }
  // -> if ( Exp ) Statement else Statement
  // -> while ( Exp ) Statement
  // -> System.out.println ( Exp ) ;
  // -> id = Exp ;
  // -> id [ Exp ]= Exp ;
  // -> return Exp;
  private void parseStatement()
  {
	  while(current.kind == Kind.TOKEN_LBRACE || current.kind == Kind.TOKEN_IF  || current.kind == Kind.TOKEN_RBRACE
			  || current.kind == Kind.TOKEN_WHILE || current.kind == Kind.TOKEN_SYSTEM 
			  || current.kind == Kind.TOKEN_ID || current.kind == Kind.TOKEN_EQ 
			  || current.kind == Kind.TOKEN_RETURN
			  || current.kind == Kind.TOKEN_TRUE || current.kind == Kind.TOKEN_FALSE)
	  {

		  switch (current.kind)
		  {
		  case TOKEN_TRUE:
		  {
			  eatToken(Kind.TOKEN_TRUE);
			  eatToken(Kind.TOKEN_SEMI);
			  continue;
		  }
		  case TOKEN_FALSE:
		  {
			  eatToken(Kind.TOKEN_FALSE);
			  eatToken(Kind.TOKEN_SEMI);
			  continue;
		  }
		  case TOKEN_RBRACE:
		  {
			  eatToken(Kind.TOKEN_RBRACE); 
			  // need to fix some situation
			  continue;
		  }
		  case TOKEN_RETURN:
				{
					if(current.kind != Kind.TOKEN_SEMI)
					{
						eatToken(Kind.TOKEN_RETURN);
						parseExpList();
						eatToken(Kind.TOKEN_SEMI);
					}
					else 
						eatToken(Kind.TOKEN_SEMI);
					continue;
				}

		  	case TOKEN_LBRACE:
		  	{
				  eatToken(Kind.TOKEN_LBRACE);
				  parseStatement();
//				  eatToken(Kind.TOKEN_RBRACE);
				  return;
		  	}
		    // -> if ( Exp ) Statement else Statement
		  	case TOKEN_IF:
		  	{
		  		eatToken(Kind.TOKEN_IF);
		  		parseExpList();
		  			parseStatement();

		  		eatToken(Kind.TOKEN_ELSE);
		  			parseStatement();
		  		return;
		  	}
		    // -> while ( Exp ) Statement
		  	case TOKEN_WHILE:
		  	{
		  		eatToken(Kind.TOKEN_WHILE);
		  		parseExpList();
		  		if(current.kind == Kind.TOKEN_LBRACE)
		  			eatToken(Kind.TOKEN_LBRACE);
		  		parseStatement();
		  		continue;
		  	}
		  	case TOKEN_SYSTEM:
		  	{
		  		eatToken(Kind.TOKEN_SYSTEM);
		  		eatToken(Kind.TOKEN_DOT);
		  		eatToken(Kind.TOKEN_OUT);
		  		eatToken(Kind.TOKEN_DOT);
		  		eatToken(Kind.TOKEN_PRINTLN);
		  		eatToken(Kind.TOKEN_LPAREN);
		  		parseExpList();
		  		eatToken(Kind.TOKEN_RPAREN);
		  		eatToken(Kind.TOKEN_SEMI);
		  		continue;
				  
		  	}
		    // -> id = Exp ;
		    // -> id [ Exp ]= Exp ;
		    // -> id.id(exp);     //added by miantuan
		  	case TOKEN_ID:
		  	case TOKEN_EQ:
		  	{
		  		if(current.kind == Kind.TOKEN_ID)
		  		{
		  			eatToken(Kind.TOKEN_ID);
		  			if(current.kind == Kind.TOKEN_LBRACK)
		  			{
		  				eatToken(Kind.TOKEN_LBRACK);
		  				parseExpList();
		  				eatToken(Kind.TOKEN_RBRACK);
		  			}
		  		}
		  		eatToken(Kind.TOKEN_EQ);
		  		parseExpList();
		  		eatToken(Kind.TOKEN_SEMI);
		  		continue;
		  	}
		  	default:
		  		error();
	//	  		return;
		  }
	  }
	  return;
    // Lab1. Exercise 4: Fill in the missing code
    // to parse a statement.
  //  new util.Todo();
  }

  // Statements -> Statement Statements
  // ->
  private void parseStatements()
  {
    while (current.kind == Kind.TOKEN_LBRACE || current.kind == Kind.TOKEN_IF
        || current.kind == Kind.TOKEN_WHILE
        || current.kind == Kind.TOKEN_SYSTEM || current.kind == Kind.TOKEN_ID) {
      parseStatement();
    }
    return;
  }

  // Type -> int []
  // -> boolean
  // -> int
  // -> id

  private void parseType()
  {
    // Lab1. Exercise 4: Fill in the missing code
    // to parse a type.	  
	  switch(current.kind)
	  {
	  case TOKEN_INT:
	  {
		  eatToken(Kind.TOKEN_INT);
		  if (current.kind == Kind.TOKEN_LBRACK)
			  {
			  	eatToken(Kind.TOKEN_LBRACK);
			  	eatToken(Kind.TOKEN_RBRACK);
			  	return;
			  }
		  return;
	  }
	  case TOKEN_BOOLEAN:
		  eatToken(Kind.TOKEN_BOOLEAN);break;
	  case TOKEN_FALSE:
		  eatToken(Kind.TOKEN_FALSE);break;
	  case TOKEN_TRUE:
		  eatToken(Kind.TOKEN_TRUE);break;
	  case TOKEN_ID:
		  eatToken(Kind.TOKEN_ID);break;
	  default:
		  error();
	  }
	  return ;
  }

  // VarDecl -> Type id ;
  private void parseVarDecl()
  {
    // to parse the "Type" nonterminal in this method, instead of writing
    // a fresh one.	
	parseType();
	if(current.kind == Kind.TOKEN_EQ)  return;  //if next token is "=", return
    eatToken(Kind.TOKEN_ID);
    eatToken(Kind.TOKEN_SEMI);
    return;
  }

  // VarDecls -> VarDecl VarDecls
  // ->
  private void parseVarDecls()
  {
    while (current.kind == Kind.TOKEN_INT || current.kind == Kind.TOKEN_BOOLEAN || current.kind == Kind.TOKEN_ID) 
    {    	
      parseVarDecl();
      if(current.kind == Kind.TOKEN_EQ)  return;    //if next token is "=", return
    }
    return;
  }

  // FormalList -> Type id FormalRest*
  // ->
  // FormalRest -> , Type id
  private void parseFormalList()
  {
    if (current.kind == Kind.TOKEN_INT || current.kind == Kind.TOKEN_BOOLEAN
        || current.kind == Kind.TOKEN_ID) {
      parseType();
      eatToken(Kind.TOKEN_ID);
      while (current.kind == Kind.TOKEN_COMMER) {
        advance();
        parseType();
        eatToken(Kind.TOKEN_ID);
      }
    }
    return;
  }

  // Method -> public Type id ( FormalList )
  // { VarDecl* Statement* return Exp ;}
  private void parseMethod()
  {
    // Lab1. Exercise 4: Fill in the missing code
    // to parse a method.

	if(current.kind == Kind.TOKEN_INT)
		eatToken(Kind.TOKEN_INT);
	else if(current.kind == Kind.TOKEN_BOOLEAN)
		eatToken(Kind.TOKEN_BOOLEAN);
	else if(current.kind == Kind.TOKEN_REAL)
		eatToken(Kind.TOKEN_REAL);
	else if(current.kind == Kind.TOKEN_ID)
		eatToken(Kind.TOKEN_ID);
	else 
	{
		System.out.println("error:   " + current.toString());
		error();
	}
	eatToken(Kind.TOKEN_ID);
	eatToken(Kind.TOKEN_LPAREN);
	parseFormalList();
	eatToken(Kind.TOKEN_RPAREN);
	eatToken(Kind.TOKEN_LBRACE);
	//deal with var declaration
	parseVarDecls();
	parseStatement();
  //  new util.Todo();
    return;
  }

  // MethodDecls -> MethodDecl MethodDecls
  // ->
  private void parseMethodDecls()
  {
    while (current.kind == Kind.TOKEN_PUBLIC) {
    	eatToken(Kind.TOKEN_PUBLIC);
      parseMethod();     
    }
    return;
  }

  // ClassDecl -> class id { VarDecl* MethodDecl* }
  // -> class id extends id { VarDecl* MethodDecl* }
  private void parseClassDecl()
  {
    eatToken(Kind.TOKEN_CLASS);
    eatToken(Kind.TOKEN_ID);
    if (current.kind == Kind.TOKEN_EXTENDS) {
      eatToken(Kind.TOKEN_EXTENDS);
      eatToken(Kind.TOKEN_ID);
    }
    eatToken(Kind.TOKEN_LBRACE);
    parseVarDecls();    
    parseMethodDecls(); 
    return;
  }

  // ClassDecls -> ClassDecl ClassDecls
  // ->
  private void parseClassDecls()
  {
    while (current.kind == Kind.TOKEN_CLASS) {
      parseClassDecl();
    }
    return;
  }

  // MainClass -> class id
  // {
  // public static void main ( String [] id )
  // {
  // Statement
  // }
  // }
  private void parseMainClass()
  {
	  eatToken(Kind.TOKEN_CLASS);
	  eatToken(Kind.TOKEN_ID);
	  eatToken(Kind.TOKEN_LBRACE);
	  eatToken(Kind.TOKEN_PUBLIC);
	  eatToken(Kind.TOKEN_STATIC);
	  eatToken(Kind.TOKEN_VOID);
	  eatToken(Kind.TOKEN_MAIN);
	  eatToken(Kind.TOKEN_LPAREN);
	  eatToken(Kind.TOKEN_STRING);
	  eatToken(Kind.TOKEN_LBRACK);
	  eatToken(Kind.TOKEN_RBRACK);
	  eatToken(Kind.TOKEN_ID);
	  eatToken(Kind.TOKEN_RPAREN);
	  parseStatement();
	 // eatToken(Kind.TOKEN_RBRACE);
	  return;
	// Lab1. Exercise 4: Fill in the missing code
    // to parse a main class as described by the
    // grammar above.
 //   new util.Todo();
  }

  // Program -> MainClass ClassDecl*
  private void parseProgram()
  {
    parseMainClass();
    parseClassDecls();	
    eatToken(Kind.TOKEN_EOF);
    return;
  }

  public void parse()
  {
    parseProgram();
    return;
  }
}
