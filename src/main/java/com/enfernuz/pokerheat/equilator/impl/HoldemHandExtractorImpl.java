package com.enfernuz.pokerheat.equilator.impl;

import java.util.*;

import com.google.common.collect.*;

import com.enfernuz.pokerheat.core.*;
import com.enfernuz.pokerheat.core.holdem.HoldemHand;
import com.enfernuz.pokerheat.core.util.Combinator;

/**
 *
 * @author A. Nerushev
 */
public class HoldemHandExtractorImpl implements HandExtractor<HoldemHand> {

    private final Combinator<Card> combinator;
    
    public HoldemHandExtractorImpl(Combinator<Card> combinator) {
        
        this.combinator = Objects.requireNonNull(combinator, "The parameter 'combinator' must not be null.");
    }
    
    @Override
    public ImmutableCollection<HoldemHand> getPossibleHandsFrom(CommonCardDeck deck) {
        
        Objects.requireNonNull(deck, "The parameter 'deck' must not be null.");

        final Collection<Collection<Card>> combinations = 
                combinator.kCombinationsFromN(deck.getElements(), HoldemHand.HAND_SIZE);

        final ImmutableCollection.Builder<HoldemHand> possibleHands = 
                ImmutableList.builder();
        
        for (Collection<Card> handCards : combinations) {
            
            final Iterator<Card> iterator = handCards.iterator();
            
            possibleHands.add(
                    HoldemHand.of(iterator.next(), iterator.next())
            );
        }
        
        return possibleHands.build();
    }

}
