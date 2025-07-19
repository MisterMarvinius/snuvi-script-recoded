package me.hammerle.snuviscript.tokenizer;

import java.io.InputStream;
import java.util.ArrayList;
import me.hammerle.snuviscript.exceptions.PreScriptException;
import static me.hammerle.snuviscript.tokenizer.TokenType.*;

public class Tokenizer {
    private StreamCharReader stream = null;
    private final ArrayList<Token> tokens = new ArrayList<>();
    private int line = 1;

    private int next() {
        return stream.readChar();
    }

    private int peek() {
        return stream.peekChar();
    }

    private boolean next(char c) {
        if(peek() == c) {
            next();
            return true;
        }
        return false;
    }

    private void add(TokenType type) {
        tokens.add(new Token(type, line));
    }

    private void add(TokenType type, Object data) {
        tokens.add(new DataToken(type, line, data));
    }

    private void add(char c, TokenType t1, TokenType t2, TokenType t3, TokenType t4) {
        int peek = peek();
        if(peek == c) {
            next();
            if(peek() == '=') {
                next();
                add(t1);
            } else {
                add(t2);
            }
        } else if(peek == '=') {
            next();
            add(t3);
        } else {
            add(t4);
        }
    }

    public Token[] tokenize(InputStream... streams) {
        tokens.clear();
        int fileCounter = 0;
        for(InputStream in : streams) {
            fileCounter++;
            line = (fileCounter << 24) | 1;
            stream = new StreamCharReader(in);

            int c;
            while((c = Tokenizer.this.next()) != -1) {
                handleChar(c);
            }
        }
        add(EOF);
        return tokens.toArray(new Token[tokens.size()]);
    }

    private void handleChar(int c) {
        if(Character.isLetter(c) || c == '_' || c == '.') {
            handleLiteral(c, TokenType.LITERAL);
        } else if(Character.isDigit(c)) {
            handleNumber(c);
        } else {
            handleSpecial(c);
        }
    }

    private void handleLiteral(int c, TokenType type) {
        StringBuilder sb = new StringBuilder();
        sb.append((char) c);

        while(true) {
            int data = peek();
            if(!Character.isLetterOrDigit(data) && data != '_' && data != '.') {
                break;
            }
            sb.append((char) data);
            next();
        }

        String s = sb.toString();
        switch(s) {
            case "if":
                add(IF);
                break;
            case "else":
                add(ELSE);
                break;
            case "elseif":
                add(ELSEIF);
                break;
            case "while":
                add(WHILE);
                break;
            case "for":
                add(FOR);
                break;
            case "function":
                add(FUNCTION);
                break;
            case "break":
                add(BREAK);
                break;
            case "continue":
                add(CONTINUE);
                break;
            case "return":
                add(RETURN);
                break;
            case "true":
                add(TRUE);
                break;
            case "import":
                add(IMPORT);
                break;
            case "false":
                add(FALSE);
                break;
            case "null":
                add(NULL);
                break;
            default:
                add(type, s);
        }

    }

    private void handleNumber(int c) {
        StringBuilder sb = new StringBuilder();
        sb.append((char) c);

        while(true) {
            int data = peek();
            if(!Character.isDigit(data)) {
                break;
            }
            next();
            sb.append((char) data);
        }
        if(peek() == '.') {
            sb.append((char) next());
        }
        while(true) {
            int data = peek();
            if(!Character.isDigit(data)) {
                break;
            }
            next();
            sb.append((char) data);
        }
        try {
            add(NUMBER, Double.parseDouble(sb.toString()));
        } catch(NumberFormatException ex) {
            throw new PreScriptException("invalid number", line);
        }
    }

    private void handleSpecial(int c) {
        switch(c) {
            case ' ':
            case '\t':
            case '\r':
                break;
            case '\n':
                line++;
                break;
            case '"':
                handleString();
                break;
            case '(':
                add(OPEN_BRACKET);
                break;
            case ')':
                add(CLOSE_BRACKET);
                break;
            case '[':
                add(OPEN_SQUARE_BRACKET);
                break;
            case ']':
                add(CLOSE_SQUARE_BRACKET);
                break;
            case '{':
                add(OPEN_CURVED_BRACKET);
                break;
            case '}':
                add(CLOSE_CURVED_BRACKET);
                break;
            case '$':
                handleLiteral(c, LITERAL);
                break;
            case '@':
                handleLiteral(c, LABEL);
                break;
            case ';':
                add(SEMICOLON);
                break;
            case ',':
                add(COMMA);
                break;
            case '~':
                add(BIT_INVERT);
                break;
            case '+':
                add(next('=') ? ADD_SET : (next('+') ? INC : ADD));
                break;
            case '-':
                add(next('=') ? SUB_SET : (next('-') ? DEC : SUB));
                break;
            case '!':
                add(next('=') ? NOT_EQUAL : INVERT);
                break;
            case '=':
                add(next('=') ? EQUAL : SET);
                break;
            case '*':
                add(next('=') ? MUL_SET : MUL);
                break;
            case '/':
                handleSlash();
                break;
            case '%':
                add(next('=') ? MOD_SET : MOD);
                break;
            case '&':
                add(next('=') ? BIT_AND_SET : (next('&') ? AND : BIT_AND));
                break;
            case '|':
                add(next('=') ? BIT_OR_SET : (next('|') ? OR : BIT_OR));
                break;
            case '^':
                add(next('=') ? BIT_XOR_SET : BIT_XOR);
                break;
            case '<':
                add('<', LEFT_SHIFT_SET, LEFT_SHIFT, LESS_EQUAL, LESS);
                break;
            case '>':
                add('>', RIGHT_SHIFT_SET, RIGHT_SHIFT, GREATER_EQUAL, GREATER);
                break;
            default:
                throw new PreScriptException("unknown token " + c, line);
        }
    }

    private void handleString() {
        StringBuilder sb = new StringBuilder();
        int oldLine = line;
        while(true) {
            int data = next();
            if(data == -1) {
                throw new PreScriptException("non closed string literal", oldLine);
            }
            if(data == '"') {
                add(STRING, sb.toString());
                break;
            }
            if(data == '\n') {
                line++;
            }
            if(data == '\\') {
                int escape = next();
                switch(escape) {
                    case 'n':
                        data = '\n';
                        break;
                    case '\\':
                        data = '\\';
                        break;
                    case '"':
                        data = '"';
                        break;
                    default:
                        throw new PreScriptException("invalid escaped character", line);
                }
            }
            sb.append((char) data);
        }
    }

    private void handleSlash() {
        switch(peek()) {
            case '/':
                next();
                handleOneLineComment();
                break;
            case '*':
                next();
                handleMultiLineComment();
                break;
            case '=':
                next();
                add(DIV_SET);
                break;
            default:
                add(DIV);
        }
    }

    private void handleOneLineComment() {
        while(true) {
            int data = next();
            if(data == -1 || data == '\n') {
                line++;
                break;
            }
        }
    }

    private void handleMultiLineComment() {
        int first;
        int sec = -1;
        while(true) {
            first = sec;
            sec = next();
            if(sec == -1 || (first == '*' && sec == '/')) {
                break;
            }
            if(sec == '\n') {
                line++;
            }
        }
    }
}
