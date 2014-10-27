/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

grammar JSON;


jsonData:
    '{'   DQ 'traces'  DQ  ':'    '['  flow (',' flow)* ']'
    '}'
;

flow :
         '['
            data  (',' data)*
         ']'
      ;

data :
               '{'         key ','
                           mod ','
                           step ','
                           values
               '}'
         ;

 key :
         DQ 'key' DQ  ':'  DQ ID DQ
     ;

 mod :
        DQ 'mode' DQ ':' DQ INT  DQ
        ;

 step :
          DQ 'step' DQ  ':' DQ INT DQ
 ;

 values :
            DQ 'values' DQ ':'
            '['
                mapping (',' mapping)*
            ']'
        ;

 mapping :
             '{'  time ','  value '}'
         ;

 time :
          DQ 'time' DQ  ':' interval
      ;

 value :
           DQ 'enclosure' DQ ':' interval
       ;

 interval :
              '[' number ',' number ']'
          ;
 number :
        ('+' | '-')? (INT | FLOAT)
        ;


 ID :  ID_LETTER (ID_LETTER | DIGIT)* ;
 INT :   DIGIT+ [Ll]? ;
 FLOAT:  DIGIT+ '.' DIGIT* EXP? [Ll]?
     |   DIGIT+ EXP? [Ll]?
     |   '.' DIGIT+ EXP? [Ll]?
     ;


DQ :     '"'  ;  //double qutes

fragment ID_LETTER : 'a'..'z'|'A'..'Z'|'_' ;
fragment DIGIT : '0'..'9' ;
fragment EXP :   ('E' | 'e') ('+' | '-')? INT ;


COMMENT
    :   '/*' .*? '*/'    -> channel(HIDDEN) // match anything between /* and */
    ;
LINE_COMMENT
    : '//' ~[\r\n]* '\r'? '\n' -> channel(HIDDEN)
    ;

//\u000C : change page
WS  :   [ \r\t\u000C\n]+ -> channel(HIDDEN)
    ;