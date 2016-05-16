/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.enfernuz.pokerheat.equilator.impl;


import com.enfernuz.pokerheat.core.Card;
import com.enfernuz.pokerheat.core.CombinationEvaluator;
import com.enfernuz.pokerheat.core.CommonCardDeck;
import com.enfernuz.pokerheat.core.PokerCombination;
import com.enfernuz.pokerheat.core.util.Combinator;
import com.enfernuz.pokerheat.equilator.warehouse.PokerCombinationRepository;
import com.google.common.collect.*;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author A. Nerushev
 */
public class PopulatorTest {

    public static void main(String[] args) {
        
//        ImmutableSet.Builder<Card> cards = ImmutableSet.builder();
//        
//        for( Card.Suit suit : Card.Suit.values() ) {
//            for( Card.Rank value : Card.Rank.values() ) {
//                cards.add( Card.of(value, suit) );
//            }
//        }

        //System.out.println(PokerCombination.CombinationType.FLUSH);

        final PokerCombinationRepository repository = new PokerCombinationRepositoryImpl();
        final CombinationEvaluator evaluator = new CombinationEvaluatorImpl(repository);

        CommonCardDeck deck = CommonCardDeck.createNew();
        
        final Combinator<Card> combinator = new CombinatorImpl<>();
       
        final Collection<Collection<Card>> combinations = combinator.kCombinationsFromN(deck.getElements(), 5);
        
        
        
        final AtomicInteger counter = new AtomicInteger(0);

        combinations.parallelStream().forEach(combination -> {
            evaluator.evaluate(combination);
            if( counter.incrementAndGet() % 50000 == 0 ) {
                System.out.println( String.format("Evaluated so far: %d.", counter.get() ) );
            }
        });

//        for(int i = 0; i < 150; i++) {
//            
//            System.out.println( String.format("Type: %s.", evaluator.evaluate( ImmutableSet.copyOf(combinations.get(i)))));
//            
//        } 

    }
}
