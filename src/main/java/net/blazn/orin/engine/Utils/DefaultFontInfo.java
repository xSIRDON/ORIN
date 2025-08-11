package net.blazn.orin.engine.Utils;

public enum DefaultFontInfo {

    A('A', 5),  a('a', 5),  B('B', 5),  b('b', 5),
    C('C', 5),  c('c', 5),  D('D', 5),  d('d', 5),
    E('E', 5),  e('e', 5),  F('F', 5),  f('f', 4),
    G('G', 5),  g('g', 5),  H('H', 5),  h('h', 5),
    I('I', 3),  i('i', 1),  J('J', 5),  j('j', 5),
    K('K', 5),  k('k', 4),  L('L', 5),  l('l', 2),
    M('M', 5),  m('m', 5),  N('N', 5),  n('n', 5),
    O('O', 5),  o('o', 5),  P('P', 5),  p('p', 5),
    Q('Q', 5),  q('q', 5),  R('R', 5),  r('r', 5),
    S('S', 5),  s('s', 5),  T('T', 5),  t('t', 4),
    U('U', 5),  u('u', 5),  V('V', 5),  v('v', 5),
    W('W', 5),  w('w', 5),  X('X', 5),  x('x', 5),
    Y('Y', 5),  y('y', 5),  Z('Z', 5),  z('z', 5),
    NUM_1('1', 5),  NUM_2('2', 5),  NUM_3('3', 5),
    NUM_4('4', 5),  NUM_5('5', 5),  NUM_6('6', 5),
    NUM_7('7', 5),  NUM_8('8', 5),  NUM_9('9', 5),
    NUM_0('0', 5),  EXCLAMATION('!', 1),  AT('@', 6),
    HASH('#', 5),  DOLLAR('$', 5),  PERCENT('%', 5),
    UP('^', 5),  AMP('&', 5),  STAR('*', 5),
    LEFT_PAREN('(', 4),  RIGHT_PAREN(')', 4),
    MINUS('-', 5),  UNDERSCORE('_', 5),  PLUS('+', 5),
    EQUAL('=', 5),  SLASH('/', 5),  QUESTION('?', 5),
    COLON(':', 1),  SEMICOLON(';', 1),  COMMA(',', 1),
    DOT('.', 1),  LESS('<', 4),  GREATER('>', 4),
    OPEN_BRACKET('[', 3),  CLOSE_BRACKET(']', 3),
    PIPE('|', 1),  BACKSLASH('\\', 5),  TICK('`', 2),
    TILDE('~', 5),  SPACE(' ', 3),  DEFAULT('a', 4);

    private final char character;
    private final int length;

    DefaultFontInfo(char character, int length) {
        this.character = character;
        this.length = length;
    }

    public char getCharacter() {
        return character;
    }

    public int getLength() {
        return length;
    }

    public int getBoldLength() {
        return this == SPACE ? getLength() : length + 1;
    }

    public static DefaultFontInfo getDefaultFontInfo(char c) {
        for (DefaultFontInfo dFI : values()) {
            if (dFI.getCharacter() == c) return dFI;
        }
        return DEFAULT;
    }
}
