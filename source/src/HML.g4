grammar HML;

hybridModel
    : signalDeclaration*
      variableDeclaration*
      variableConstraint*
      template*
      program
      EOF
    ;


template
    : 'Template' ID formalParameters parStatement;


formalParameters
    :   '(' formalParameterDecls? ')'
    ;

formalParameterDecls
    :    type formalParameterDeclsRest
    ;

formalParameterDeclsRest
    :   variableDeclaratorId (',' formalParameterDecls)?
    ;

variableDeclaratorId
    :   ID ('['  ']')*
    ;

type
    :   primitiveType ('[' ']')*
    ;

primitiveType
    :   'boolean'
    |   'int'
    |   'float'
    ;

signalDeclaration
    :   'Signal' ('[' size=INT ']')* ID (',' ID)* ';'
    ;

modifier
    :   'final'             // used for the vars that cannot be modified
    ;

variableDeclaration
    :  modifier? type variableDeclarators ';'
    ;

variableConstraint
    : variableDeclaratorId 'in' '[' leftEnd=expr ',' rightEnd=expr ']' ';'
    ;


variableDeclarators
    :   variableDeclarator (',' variableDeclarator)*
    ;

variableDeclarator
    :   variableDeclaratorId ('=' variableInitializer)?
    ;

variableInitializer
    :   arrayInitializer
    |   expr
    ;


arrayInitializer
    :   '{' (variableInitializer (',' variableInitializer)* (',')? )? '}'
    |   'new' 'Array' ('[' INT ']')+
    ;

program : 'Main ' '{' blockStatement* '}';

blockStatement
    :  atom                                                  #AtomPro   // Atomic
    |  blockStatement '|'  blockStatement                    #NonCh     // non-deterministrate choice
    |  blockStatement '||' blockStatement                    #ParaCom   // parallel composition
    |  blockStatement ';'?  blockStatement                   #SeqCom    // sequential composition
    |  '(' blockStatement '<' expr '>' blockStatement ')'    #ConCh     // conditional choice
    |  equation 'until' guard                                #Ode       // differential equation
    |  'when' '{' guardedchoice '}'                          #WhenPro   // when program
    |  'while' parExpression parStatement                    #LoopPro   // loop
    |  'if' parExpression parStatement
       ( 'else' parStatement )?                              #IfPro     // if statement
    |  ID '(' exprList? ')'                                  #CallTem   // call template
    |  parStatement                                          #ParPro    // program with paraentheses outside
    ;


parStatement
    :   '{' blockStatement '}'
    |   '(' blockStatement ')'
    ;

exprList
    : expr (',' expr)* ;   // arg list

atom
    :  'skip'
    |  ID '=' expr
    |  '!' signal                   // !s (single or multi-sending), !s[0], !s[1][1]
    |  'suspend' '(' time=expr ')'
    ;

expr
    : ID
    | INT
    | FLOAT
    |'true'
    | 'false'
    | ('-' | '~') expr          // negtive and negation
    | expr ('*'|'/'|'mod') expr   // % is mod
    | expr ('+'|'-') expr
    | expr ('>=' | '>' | '==' | '<' | '<=') expr
    | expr 'and' expr
    | expr 'or' expr
    | expr 'xor' expr
    | 'floor' '(' expr ')'
    | 'ceil' '(' expr ')'
    | parExpression
    ;

parExpression
    :   '(' expr ')'
    ;

equation
    : relation
    | relation 'init' expr
    | equation '||' equation
    ;

relation
    : 'dot' ID '=' expr;


guard
    : 'EMP'
    | signal                 //waiting for s (single or multi-receiving), s[0], s[1][1]...
    | expr
    | 'timeout' '(' expr ')'
    | guard '<and>' guard
    | guard '<or>' guard
    | '(' guard ')'
    ;

signal
    : ID ('[' expr ']')*;




guardedchoice
    :  guard '->'  blockStatement
    |  guardedchoice '[]' guardedchoice
    |  '(' guardedchoice ')'
    ;


COMOP
    : '>='
    | '>'
    | '=='
    | '<'
    | '<='
    | '!='
    ;

ID  :    (LETTER | '_')  (LETTER|DIGIT|'_')*
    ;
fragment LETTER  : [a-zA-Z] ;


INT :   DIGIT+ ;

FLOAT   :  DIGIT+ '.' DIGIT+
        ;

fragment
DIGIT:  '0'..'9' ;

COMMENT
    :   '/*' .*? '*/'    -> channel(HIDDEN) // match anything between /* and */
    ;
LINE_COMMENT
    : '//' ~[\r\n]* '\r'? '\n' -> channel(HIDDEN)
    ;

//\u000C : change page
WS  :   [ \r\t\u000C\n]+ -> channel(HIDDEN)
    ;