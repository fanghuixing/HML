grammar HML;

hybridModel
    : signalDeclaration*
      variableDeclaration*
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

program : 'Main ' '{' signalDeclaration* variableDeclaration* blockStatement* '}';

blockStatement
    :  atom                                                  #AtomPro   // Atomic
    |  blockStatement '|'  blockStatement                    #NonCh     // non-deterministrate choice
    |  blockStatement '||' blockStatement                    #ParaCom   // parallel composition
    |  blockStatement ';'  blockStatement                    #SeqCom    // sequential composition
    |  '(' blockStatement '<' expr '>' blockStatement ')'    #ConCh     // conditional choice
    |  equation 'until' guard                                #Ode       // differential equation
    |  'when' '(' guardedchoice ')'                          #WhenPro   // when program
    |  'while' parExpression parStatement                    #LoopPro   // loop
    |  'if' parExpression parStatement
       'else' parStatement                                   #IfPro     // if statement
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
    |  '!' ID
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
    : 'dot' ID '==' expr;


guard
    : 'eps'
    | signal
    | expr
    | 'timeout' '(' expr ')'
    | guard '<and>' guard
    | guard '<or>' guard
    | '(' guard ')'
    ;

signal
    : ID ('[' expr ']')*;




guardedchoice
    :  guard '&'  program
    | guardedchoice '[]' guardedchoice
    ;


COMOP
    : '>='
    | '>'
    | '=='
    | '<'
    | '<='
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

WS  :   [ \r\t\u000C\n]+ -> channel(HIDDEN)
    ;