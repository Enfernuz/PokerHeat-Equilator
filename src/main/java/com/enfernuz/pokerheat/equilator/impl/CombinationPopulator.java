package com.enfernuz.pokerheat.equilator.impl;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.enfernuz.pokerheat.core.*;
import com.enfernuz.pokerheat.core.util.Combinator;

/**
 *
 * @author A. Nerushev
 */
public class CombinationPopulator {
    
    private static final CommonCardDeck CARD_DECK;

    private final CombinationEvaluator evaluator;
    private final Combinator<Card> combinator;
    
    static {
        CARD_DECK = CommonCardDeck.createNew();
    }
    
    public CombinationPopulator(CombinationEvaluator evaluator, Combinator<Card> combinator) {
        
        this.evaluator = Objects.requireNonNull(evaluator, "'evaluator' must not be null.");
        this.combinator = Objects.requireNonNull(combinator, "'combinator' must not be null.");
    }
    
    public void populate() {
       
        final Collection<Collection<Card>> combinations = 
                combinator.kCombinationsFromN(CARD_DECK.getElements(), 5);

//        final AtomicInteger counter = new AtomicInteger(0);

        combinations.parallelStream().forEach(combination -> {
            
            evaluator.evaluate(combination);
//            if (counter.incrementAndGet() % 50000 == 0) {
//                System.out.println( String.format("Evaluated so far: %d.", counter.get() ) );
//            }
        });
    }
}
