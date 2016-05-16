package com.enfernuz.pokerheat.equilator.impl;

import com.enfernuz.pokerheat.equilator.warehouse.PokerCombinationRepository;
import com.enfernuz.pokerheat.equilator.warehouse.PokerCombinationEntity;
import java.util.Optional;

import javax.persistence.*;


/**
 *
 * @author A. Nerushev
 */
public class PokerCombinationRepositoryImpl implements PokerCombinationRepository {
    
    private static final String PU_NAME;
    
    private final EntityManagerFactory emFactory;
    
    static {
        PU_NAME = "PU";
    }
    
    public PokerCombinationRepositoryImpl() {
        
        this.emFactory = Persistence.createEntityManagerFactory(PU_NAME);
    }

    @Override
    public Optional<PokerCombinationEntity> findById(long id) {
        
        final Optional<PokerCombinationEntity> result;

        final EntityManager entityManager = emFactory.createEntityManager();
        
        //final EntityTransaction entityTransaction = entityManager.getTransaction();

        //entityTransaction.begin();
        
        final PokerCombinationEntity found = entityManager.find(PokerCombinationEntity.class, id);

        //entityTransaction.commit();
        
        if (found == null) {
            result = Optional.empty();
        } else {
            entityManager.detach(found);
            result = Optional.of(found);
        }
        
        entityManager.close();
        
        return result;
        
    }

    @Override
    public PokerCombinationEntity saveOrUpdate(PokerCombinationEntity entity) {
        
        final EntityManager entityManager = emFactory.createEntityManager();
        
        //final EntityTransaction entityTransaction = entityManager.getTransaction();

        //entityTransaction.begin();
        
        final PokerCombinationEntity updated = entityManager.merge(entity);

        //entityTransaction.commit();
        
        entityManager.detach(updated);
        entityManager.close();
        
        return updated;
    }

}
