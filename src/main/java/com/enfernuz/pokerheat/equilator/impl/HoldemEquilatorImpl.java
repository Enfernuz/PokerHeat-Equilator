package com.enfernuz.pokerheat.equilator.impl;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.enfernuz.pokerheat.core.*;
import com.enfernuz.pokerheat.core.holdem.HoldemHand;
import com.enfernuz.pokerheat.core.util.Combinator;

import com.google.common.collect.*;

/**
 *
 * @author A. Nerushev
 */
public class HoldemEquilatorImpl implements Equilator<HoldemHand>{
    
    private final CombinationEvaluator evaluator;
    private final Combinator<Card> combinator;
    private final HandExtractor<HoldemHand> handExtractor;

    public HoldemEquilatorImpl(CombinationEvaluator evaluator, Combinator<Card> combinator, HandExtractor<HoldemHand> handExtractor) {
        
        this.evaluator = Objects.requireNonNull(evaluator, "The parameter 'evaluator' must not be null.");
        this.combinator = Objects.requireNonNull(combinator, "The parameter 'combinator' must not be null.");
        this.handExtractor = Objects.requireNonNull(handExtractor, "The parameter 'handExtractor' must not be null.");
    }

    @Override
    public ImmutableMap<HoldemHand, Equity> calculateEquities(Board board, HoldemHand heroHand) {
        
        final int cardsToDeal = board.getLimit() - board.remaining();
        
        System.out.println( String.format("CARDS TO DEAL: %d", cardsToDeal) );
        
        final CommonCardDeck deck = CommonCardDeck.createNew();
        
        final ImmutableCollection<Card> boardCards = board.getElements();
        heroHand.getCards().forEach( deck::removeAll );
        boardCards.forEach( deck::removeAll );
        
        final ImmutableCollection<HoldemHand> villainHands = 
                handExtractor.getPossibleHandsFrom(deck);
        
        System.out.println( String.format("POSSIBLE VILLAIN HANDS: %d", villainHands.size()) );
        
        final Map<HoldemHand, Equity> result = 
                villainHands.stream()
                        .collect( Collectors.toMap(hand -> hand, value -> new Equity() ) );
        
        final int deckSizeAfterVillainHandDeal = deck.remaining() - 2;
        
        villainHands.parallelStream().forEach(villainHand -> {
            
            final List<Card> possibleNextCards = new ArrayList<>(deckSizeAfterVillainHandDeal);
            final ImmutableSet<Card> villainHandCards = villainHand.getCards();
            for ( Card deckCard : deck.getElements() ) {
                if ( !villainHandCards.contains(deckCard) ) {
                    possibleNextCards.add(deckCard);
                }
            }
            
            final Collection<Collection<Card>> possibleDeals = combinator.kCombinationsFromN(possibleNextCards, cardsToDeal);

            //final Collection<Collection<Card>> possibleDeals = combinator.kCombinationsFromN(possibleNextCards, cardsToDeal);
            possibleDeals.parallelStream().forEach(deal -> {
                    
                final ImmutableList.Builder<Card> villainCardPool = ImmutableList.builder();
                                villainCardPool
                                        .addAll(boardCards)
                                        .addAll( villainHand.getCards() )
                                        .addAll(deal);
                
                final Collection<Collection<Card>> villainCards = combinator.kCombinationsFromN(villainCardPool.build(), PokerCombination.COMBINATION_SIZE);
                final AtomicReference<PokerCombination> villainsBest = new AtomicReference<>(null);
                villainCards.parallelStream().forEach(variation -> {
                    final PokerCombination resultCombo = evaluator.evaluate(variation);
                    villainsBest.getAndAccumulate(resultCombo, (current, given) -> {

                        if (current == null) {
                            return given;
                        }
                        
                        return evaluator.compare(current, given) < 0 ? given : current;
                    });
                });
                
                final ImmutableList.Builder<Card> heroCardPool = ImmutableList.builder();
                                heroCardPool
                                        .addAll(boardCards)
                                        .addAll( heroHand.getCards() )
                                        .addAll(deal);
                
                final Collection<Collection<Card>> heroCards = combinator.kCombinationsFromN(heroCardPool.build(), PokerCombination.COMBINATION_SIZE);
                final AtomicReference<PokerCombination> heroBest = new AtomicReference<>(null);
                heroCards.parallelStream().forEach(variation -> {
                    final PokerCombination resultCombo = evaluator.evaluate(variation);
                    heroBest.getAndAccumulate(resultCombo, (current, given) -> {

                        if (current == null) {
                            return given;
                        }

                        return evaluator.compare(current, given) < 0 ? given : current;
                    });
                });
                
                final int cmp = evaluator.compare(villainsBest.get(), heroBest.get());

                if (cmp > 0) {
                    result.get(villainHand).addWin();
                } else if (cmp < 0) {
                    result.get(villainHand).addLoss();
                } else {
                    result.get(villainHand).addTie();
                }
            });
        });

        return ImmutableMap.copyOf(result);
    }

    
}
