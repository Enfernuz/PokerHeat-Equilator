package com.enfernuz.pokerheat.equilator.warehouse;

import java.util.Optional;

/**
 *
 * @author A. Nerushev
 */
public interface PokerCombinationRepository {

    Optional<PokerCombinationEntity> findById(long id);
    
    PokerCombinationEntity saveOrUpdate(PokerCombinationEntity entity);
}
