package com.querylens.alternative.strategy;

import java.util.List;

public final class AlternativeQueryStrategyFactory {
    public List<AlternativeQueryStrategy> standardStrategies() {
        return List.of(new NotIndexedStrategy(), new ExistingIndexStrategy(), new OrToInStrategy());
    }
}
