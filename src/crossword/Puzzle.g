@skip whitespace {
    file ::= ">>" name description "\n"+ entry*;
}
@skip whitespace2 {
    entry ::= "(" wordName "," clue "," direction "," row "," col ")";
}
name ::= stringIndent;
description ::= string;
wordName ::= [a-z\-]+;
clue ::= string;
direction ::= "DOWN" | "ACROSS";
row ::= int;
col ::= int;
string ::= '"' ([^"\r\n\\] | '\\' [\\nrt] )* '"';
stringIndent ::= '"' [^"\r\n\t\\]* '"';
int ::= [0-9]+;
comment ::= "//" [^\r\n]*;
whitespace ::= [ \t\r]+ | comment;
whitespace2 ::= [ \t\r\n]+ | comment;