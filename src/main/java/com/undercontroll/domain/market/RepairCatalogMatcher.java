package com.undercontroll.domain.market;

import com.undercontroll.domain.model.insight.RepairRelation;
import com.undercontroll.domain.model.market.MarketProductCurrent;
import com.undercontroll.domain.model.market.MatchCoverage;
import com.undercontroll.domain.model.market.RepairCatalogItem;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class RepairCatalogMatcher {

    private RepairCatalogMatcher() {
    }

    public static MatchCoverage coverage(List<MarketProductCurrent> products, List<RepairCatalogItem> catalog) {
        if (products == null || products.isEmpty()) {
            return MatchCoverage.empty();
        }
        CatalogIndex index = index(catalog);
        int exact = 0;
        int brandCategory = 0;
        int category = 0;
        int none = 0;
        for (MarketProductCurrent product : products) {
            switch (index.relation(product.productKey(), product.brandSlug(), product.domainId())) {
                case NIVEL_1_EXATO -> exact++;
                case NIVEL_2_MARCA_CATEGORIA -> brandCategory++;
                case NIVEL_3_CATEGORIA -> category++;
                default -> none++;
            }
        }
        return new MatchCoverage(exact, brandCategory, category, none);
    }

    public static CatalogIndex index(List<RepairCatalogItem> catalog) {
        return CatalogIndex.from(catalog);
    }

    public static Set<String> clientDomainIds(List<RepairCatalogItem> catalog) {
        Set<String> domains = new HashSet<>();
        if (catalog == null) {
            return domains;
        }
        for (RepairCatalogItem item : catalog) {
            if (item.domainId() != null) {
                domains.add(item.domainId());
            }
        }
        return domains;
    }

    public record CatalogIndex(Set<String> productKeys, Set<String> brandDomains, Set<String> brands, Set<String> domains) {

        static CatalogIndex from(List<RepairCatalogItem> catalog) {
            Set<String> productKeys = new HashSet<>();
            Set<String> brandDomains = new HashSet<>();
            Set<String> brands = new HashSet<>();
            Set<String> domains = new HashSet<>();
            if (catalog == null) {
                return new CatalogIndex(productKeys, brandDomains, brands, domains);
            }
            for (RepairCatalogItem item : catalog) {
                if (item.productKey() != null) {
                    productKeys.add(item.productKey());
                }
                if (item.brandSlug() != null) {
                    brands.add(item.brandSlug());
                }
                if (item.brandSlug() != null && item.domainId() != null) {
                    brandDomains.add(item.brandSlug() + "|" + item.domainId());
                }
                if (item.domainId() != null) {
                    domains.add(item.domainId());
                }
            }
            return new CatalogIndex(productKeys, brandDomains, brands, domains);
        }

        public RepairRelation relation(String productKey, String brandSlug, String domainId) {
            if (productKey != null && productKeys.contains(productKey)) {
                return RepairRelation.NIVEL_1_EXATO;
            }
            if (brandSlug != null && domainId != null && brandDomains.contains(brandSlug + "|" + domainId)) {
                return RepairRelation.NIVEL_2_MARCA_CATEGORIA;
            }
            if (domainId != null && domains.contains(domainId)) {
                return RepairRelation.NIVEL_3_CATEGORIA;
            }
            return RepairRelation.SEM_MATCH;
        }

        public RepairRelation brandRelation(String brandSlug) {
            return brandSlug != null && brands.contains(brandSlug)
                    ? RepairRelation.NIVEL_2_MARCA_CATEGORIA
                    : RepairRelation.SEM_MATCH;
        }
    }
}
