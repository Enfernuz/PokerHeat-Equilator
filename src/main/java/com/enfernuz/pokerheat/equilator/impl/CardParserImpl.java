package com.enfernuz.pokerheat.equilator.impl;

import java.util.regex.*;

import com.google.common.collect.*;

import com.enfernuz.pokerheat.core.*;

/**
 *
 * @author A. Nerushev
 */
public enum CardParserImpl implements CardParser {

    INSTANCE;
    
    private static final Pattern PATTERN;
    
    static {
        
        PATTERN = Pattern.compile( createPatternSource() );
        
    }
    
    @Override
    public ImmutableCollection<Card> parse(String str) {
        
        final ImmutableCollection.Builder<Card> result = ImmutableList.builder();
        
        final Matcher matcher = PATTERN.matcher(str);
        
        Card.Rank rank;
        Card.Suit suit;
        String match;
        while( matcher.find() ) {
            
            match = matcher.group();
            rank = Card.Rank.fromChar( match.charAt(0) );
            suit = Card.Suit.fromChar( match.charAt(1) );
            
            result.add( Card.of(rank, suit) );
        }

        return result.build();
    }

    private static String createPatternSource() {
        
        final StringBuilder patternBuilder = new StringBuilder();
        
        patternBuilder.append("([");
        
        String letter;
        for( Card.Rank rank : Card.Rank.values() ) {
            letter = String.valueOf( rank.getLetter() );
            patternBuilder
                    .append( letter.toUpperCase() )
                    .append( letter.toLowerCase() );
        }
        
        patternBuilder.append("]{1}[");
        
        for( Card.Suit suit : Card.Suit.values() ) {
            letter = String.valueOf( suit.getLetter() );
            patternBuilder
                    .append( letter.toUpperCase() )
                    .append( letter.toLowerCase() );
        }
        
        patternBuilder.append("]{1})");
        
        return patternBuilder.toString();
    }
    
}
