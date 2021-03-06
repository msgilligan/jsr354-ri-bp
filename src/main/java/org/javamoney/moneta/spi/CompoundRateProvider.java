/**
 * Copyright (c) 2012, 2014, Credit Suisse (Anatole Tresch), Werner Keil and others by the @author tag.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.javamoney.moneta.spi;

import javax.money.convert.ConversionQuery;
import javax.money.convert.ExchangeRate;
import javax.money.convert.ExchangeRateProvider;
import javax.money.convert.ProviderContext;
import javax.money.convert.ProviderContextBuilder;
import javax.money.convert.RateType;

import java.util.*;

/**
 * This class implements a {@link ExchangeRateProvider} that delegates calls to
 * a collection of child {@link ExchangeRateProvider} instance.
 *
 * @author Anatole Tresch
 */
public class CompoundRateProvider extends AbstractRateProvider {
    /**
     * Kery used to store a list of child {@link javax.money.convert.ProviderContext} instances of the providers
     * contained within this instance.
     */
    public static final String CHILD_PROVIDER_CONTEXTS_KEY = "childProviderContexts";
    /**
     * The {@link ExchangeRateProvider} instances.
     */
    private final List<ExchangeRateProvider> providers = new ArrayList<>();

    /**
     * Constructor.
     *
     * @param providers The collection of child {@link ExchangeRateProvider}
     *                  instances this class delegates calls to.
     */
    public CompoundRateProvider(Iterable<ExchangeRateProvider> providers) {
        super(createContext(providers));
        for (ExchangeRateProvider exchangeRateProvider : providers) {
            addProvider(exchangeRateProvider);
        }
    }

    private static ProviderContext createContext(Iterable<ExchangeRateProvider> providers) {
        Set<RateType> rateTypeSet = new HashSet<>();
        StringBuilder providerName = new StringBuilder("Compound: ");
        List<ProviderContext> childContextList = new ArrayList<>();
        for (ExchangeRateProvider exchangeRateProvider : providers) {
            childContextList.add(exchangeRateProvider.getContext());
            providerName.append(exchangeRateProvider.getContext().getProviderName());
            providerName.append(',');
            rateTypeSet.addAll(exchangeRateProvider.getContext().getRateTypes());
        }
        providerName.setLength(providerName.length() - 1);

        ProviderContextBuilder builder = ProviderContextBuilder.of(providerName.toString(), rateTypeSet);
        builder.set(CHILD_PROVIDER_CONTEXTS_KEY, childContextList);
        return builder.build();
    }

    /**
     * Add an additional {@link ExchangeRateProvider} to the instance's delegate
     * list.
     *
     * @param prov The {@link ExchangeRateProvider} to be added, not {@code null}
     *             .
     * @throws java.lang.NullPointerException if the provider is null.
     *                                        .
     */
    private void addProvider(ExchangeRateProvider prov) {
        if(prov==null){
            throw new NullPointerException("ConversionProvider required.");
        }
        providers.add(prov);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * ExchangeRateProvider#getExchangeRate(org.javamoney.bp.
     * CurrencyUnit, CurrencyUnit,
     * ConversionContext)
     */
    @Override
    public ExchangeRate getExchangeRate(ConversionQuery conversionQuery) {
        for (ExchangeRateProvider prov : this.providers) {
            if (prov.isAvailable(conversionQuery)) {
                ExchangeRate rate = prov.getExchangeRate(conversionQuery);
                if (rate!=null) {
                    return rate;
                }
            }
        }
        return null;
    }


}