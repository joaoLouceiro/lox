# Scanning

The first thing our interpreter needs is a way to go over whatever text we feed it, and start making some sense out of it. We call that process "scanning" or "lexing" (from "lexical analysis").

During this process, we go over the text character by character, aggregating chunks of it ("tokens" and "lexemes"), and checking their validity.

## Token types

An important part of this step is to identify if a token matches one of the language's reserved keywords, so the parser knows what to do with it.

The way I see it, we can talk of XXX types of tokens:

- Symbols: stuff like '(', '!=' and such;
- Literals: these will have a value;
- Keywords: 'var', 'or', etc;

