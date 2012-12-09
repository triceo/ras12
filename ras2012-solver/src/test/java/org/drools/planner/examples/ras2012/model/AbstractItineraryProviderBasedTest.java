package org.drools.planner.examples.ras2012.model;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public abstract class AbstractItineraryProviderBasedTest {

    public static Collection<ItineraryProvider> getProviders() {
        final Collection<ItineraryProvider> providers = new LinkedHashSet<>();
        providers.add(new ToyItineraryProvider());
        providers.add(new RDS1ItineraryProvider());
        providers.add(new RDS2ItineraryProvider());
        providers.add(new RDS3ItineraryProvider());
        return Collections.unmodifiableCollection(providers);
    }
}
