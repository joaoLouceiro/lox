package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.jdi.VoidValue;

import static com.craftinginterpreters.lox.TokenType.*;

/**
 * <p>
 * Scanner is responsible for... well, scanning.
 * </p>
 * 
 * <p>
 * It goes over the source code character by character and spits out a List of
 * objects of type {@link com.craftinginterpreters.lox.Token}.
 * </p>
 */
public class Scanner {
  private final String source;
  private final List<Token> tokens = new ArrayList<>();
  private int start = 0;
  private int current = 0;
  private int line = 1;
  private static final Map<String, TokenType> keywords;

  static {
    keywords = new HashMap<>();
    keywords.put("and", AND);
    keywords.put("class", CLASS);
    keywords.put("else", ELSE);
    keywords.put("false", FALSE);
    keywords.put("for", FOR);
    keywords.put("fun", FUN);
    keywords.put("if", IF);
    keywords.put("nil", NIL);
    keywords.put("or", OR);
    keywords.put("print", PRINT);
    keywords.put("return", RETURN);
    keywords.put("super", SUPER);
    keywords.put("this", THIS);
    keywords.put("true", TRUE);
    keywords.put("var", VAR);
    keywords.put("while", WHILE);
  }

  Scanner(String source) {
    this.source = source;
  }

  /**
   * In each turn of the loop, we scan a single character, until we find ourselves
   * a lexeme.
   * 
   * @return
   */
  List<Token> scanTokens() {
    while (!isAtEnd()) {
      // We are at the beginnig of the next lexeme.
      start = current;
      scanToken();
    }

    tokens.add(new Token(EOF, "", null, line));
    return tokens;
  }

  /**
   * It looks like the first statement advances to the next character in the
   * sequence, but don't be fooled.
   * Because advance() calls current++ (with the operator as a postfix), the value
   * of the variable is updated, but we are still looking at the starting position
   * of the loop.
   */
  private void scanToken() {
    char c = advance();
    switch (c) {
      case '(':
        addToken(LEFT_PAREN);
        break;

      case ')':
        addToken(RIGHT_PAREN);
        break;

      case '{':
        addToken(LEFT_BRACE);
        break;

      case '}':
        addToken(RIGHT_BRACE);
        break;

      case ',':
        addToken(COMMA);
        break;

      case '.':
        addToken(DOT);
        break;

      case '-':
        addToken(MINUS);
        break;

      case '+':
        addToken(PLUS);
        break;

      case ';':
        addToken(SEMICOLON);
        break;

      case '*':
        addToken(STAR);
        break;

      // The following lexemes are a bit more complex, because their meaning may
      // change depending on what comes next.
      case '!':
        addToken(match('=') ? BANG_EQUAL : BANG);
        break;

      case '=':
        addToken(match('=') ? EQUAL_EQUAL : EQUAL);
        break;

      case '<':
        addToken(match('=') ? LESS_EQUAL : LESS);
        break;

      case '>':
        addToken(match('=') ? GREATER_EQUAL : GREATER);
        break;

      // This is our general strategy for longer lexemes: when we detect the start of
      // one, we keep consuming characters until we reach the end of it.
      //
      // Although comments are lexemes, they don't have any meaning for the parser.
      // That's why, when we find one, we don't make a Token out of it.
      case '/':
        if (match('/')) {
          // A comment goes until the end of the line.
          while (peek() != '\n' && !isAtEnd())
            advance();
        } else {
          addToken(SLASH);
        }
        break;

      case ' ':
      case '\r':
      case '\t':
        // Ignore whitespace.
        break;

      case '\n':
        line++;
        break;

      case '"':
        string();
        break;

      default:
        if (isDigit(c)) {
          number();
        } else if (isAlpha(c)) {
          identifier();
        } else {
          // Although an error is raised here, the scanning will go on until the end of
          // the source, so all errors are identified. That way, the user doesn't have to
          // keep re-running the same source for each tiny error.
          // Still, because Lox.error sets hasError, the code will not be executed.
          Lox.error(line, "Unexpected character.");
        }
        break;
    }
  }

  private void identifier() {
    while (isAlphaNumeric(peek()))
      advance();

    String text = source.substring(start, current);
    TokenType type = keywords.get(text);

    if (type == null)
      type = IDENTIFIER;

    addToken(type);
  }

  private void string() {
    while (peek() != '"' && !isAtEnd()) {
      if (peek() == '\n')
        line++;
      advance();
    }

    if (isAtEnd()) {
      Lox.error(line, "Unterminated string.");
      return;
    }

    // Because peek() doesn't advance, we need to do it manually (even if we already
    // saw there was a '"' in the next char).
    advance();

    // Trim the surrounding quotes
    String value = source.substring(start + 1, current - 1);
    addToken(STRING, value);

  }

  private void number() {
    while (isDigit(peek()))
      advance();

    // Look for a fractional part
    if (peek() == '.' && isDigit(peekNext())) {
      // Consume the "."
      advance();

      while (isDigit(peek()))
        advance();
    }

    addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
  }

  /**
   * This is used in cases where the current character might belong to a one
   * character long token or a multi character token.
   */
  private boolean match(char expected) {
    if (isAtEnd())
      return false;
    if (source.charAt(current) != expected)
      return false;

    current++;
    return true;
  }

  /**
   * This will stop being called as soon as a '\n' is found.
   * 
   * The switch statement is responsible for advancing the head of the buffer.
   */
  private char peek() {
    if (isAtEnd())
      return '\0';
    return source.charAt(current);
  }

  private char peekNext() {
    if (current + 1 >= source.length())
      return '\0';
    return source.charAt(current + 1);
  }

  private boolean isAlpha(char c) {
    return c >= 'a' && c <= 'z' ||
        c >= 'A' && c <= 'Z' ||
        c == '_';
  }

  private boolean isAlphaNumeric(char c) {
    return isAlpha(c) || isDigit(c);
  }

  private boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }

  /**
   * Check if we have reached the end of the FILE (not the line).
   */
  private boolean isAtEnd() {
    return current >= source.length();
  }

  /**
   * Advance the current position and return the character at the new position.
   *
   * Don't forget that current++ operates on the reference of the primitive, so it
   * is updating the value of current.
   */
  private char advance() {
    return source.charAt(current++);
  }

  /**
   * This bad boy is used for tokens that have no literals: one character
   * tokens, less-than's, yada-yada.
   *
   * @param type The type of the token.
   */
  private void addToken(TokenType type) {
    addToken(type, null);
  }

  /**
   * Add a token to the list of tokens.
   *
   * @param type    The type of the token.
   * @param literal The literal value of the token.
   */
  private void addToken(TokenType type, Object literal) {
    String text = source.substring(start, current);
    tokens.add(new Token(type, text, literal, line));
  }

}
