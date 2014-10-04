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
    :   'boolean'   # BoolType
    |   'int'       # IntType
    |   'float'     # FloatType
    ;

signalDeclaration
    :   'Signal' ('[' size=INT ']')* ID (',' ID)* ';'
    ;

modifier
    :   'final'             # FinalModifier
    ;

variableDeclaration
    :  modifier? type variableDeclarators ';'
    ;

variableConstraint
    : ID 'in' '[' leftEnd=expr ',' rightEnd=expr ']' ';'
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

program : 'Main ' '{' blockStatement '}';

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


// !s (single or multi-sending), !s[0], !s[1][1]
atom
    :  'skip'                                                   # Skip
    |  ID '=' expr                                              # Assignment
    |  '!' signal                                               # SendSignal
    |  'suspend' '(' time=expr ')'                              # Suspend
    ;

expr
    : ID                                                        # IDExpr
    | INT                                                       # INTExpr
    | FLOAT                                                     # FLOATExpr
    |'true'                                                     # ConstantTrue
    | 'false'                                                   # ConstantFalse
    | prefix=('-' | '~') expr                                   # NegationExpr
    | left=expr op=('*'|'/'|'mod') right=expr                   # MExpr
    | left=expr op=('+'|'-') right=expr                         # AExpr
    | left=expr op=('>=' | '>' | '==' | '<' | '<=') right=expr  # CompExpr
    | left=expr op='and' right=expr                             # LogicalAndExpr
    | left=expr op='or' right=expr                              # LogicalOrExpr
    | left=expr op='xor' right=expr                             # LogicalXorExpr
    | 'floor' '(' expr ')'                                      # FloorExpr
    | 'ceil' '(' expr ')'                                       # CeilExpr
    | parExpression                                             # ParExpr
    ;

parExpression
    :   '(' expr ')'
    ;



equation
    : relation                                                  # EqWithNoInit
    | relation 'init' expr                                      # EqWithInit
    | equation '||' equation                                    # ParaEq
    ;



relation
    : 'dot' ID '=' expr
    ;


//waiting for s (single or multi-receiving), s[0], s[1][1]...
guard
    : 'EMP'                             # EmptyGuard
    | signal                            # SignalGuard
    | expr                              # BoolGuard
    | 'timeout' '(' expr ')'            # TimeOutGuard
    | guard '<and>' guard               # ConjunctGuard
    | guard '<or>' guard                # DisjunctGuard
    | '(' guard ')'                     # ParGuard
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