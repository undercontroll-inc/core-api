package com.undercontroll.infrastructure.metrics;

import com.undercontroll.domain.gateway.MetricName;
import com.undercontroll.domain.gateway.MetricsGateway;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class MicrometerMetricsAdapter implements MetricsGateway {

    private final Map<MetricName, Counter> counters = new EnumMap<>(MetricName.class);
    private final Timer orderProcessingTimer;

    public MicrometerMetricsAdapter(MeterRegistry meterRegistry) {
        for (MetricName metric : MetricName.values()) {
            counters.put(metric, counter(meterRegistry, MeterSpec.of(metric)));
        }
        this.orderProcessingTimer = Timer.builder("order.processing.time")
                .description("Tempo de processamento de criação de pedidos")
                .register(meterRegistry);
        log.info("MicrometerMetricsAdapter initialized with all business metrics counters");
    }

    @Override
    public void increment(MetricName metric) {
        counters.get(metric).increment();
        log.debug("Metric {} incremented", metric);
    }

    @Override
    public void recordOrderProcessingTime(long startTimeMillis) {
        long duration = System.currentTimeMillis() - startTimeMillis;
        orderProcessingTimer.record(duration, TimeUnit.MILLISECONDS);
        log.debug("Order processing time recorded: {}ms", duration);
    }

    private static Counter counter(MeterRegistry registry, MeterSpec spec) {
        Counter.Builder builder = Counter.builder(spec.name()).description(spec.description());
        if (spec.type() != null) {
            builder.tag("type", spec.type());
        }
        return builder.register(registry);
    }

    record MeterSpec(String name, String type, String description) {

        static MeterSpec of(MetricName metric) {
            return switch (metric) {
                case LOGIN_FAILED -> new MeterSpec("auth.login.failed", "password", "Total de tentativas de login falhadas");
                case LOGIN_SUCCESS -> new MeterSpec("auth.login.success", "password", "Total de logins bem-sucedidos");
                case GOOGLE_LOGIN_FAILED -> new MeterSpec("auth.login.failed", "google", "Total de tentativas de login falhadas via Google");
                case GOOGLE_LOGIN_SUCCESS -> new MeterSpec("auth.login.success", "google", "Total de logins bem-sucedidos via Google");
                case ACCOUNT_CREATED -> new MeterSpec("user.account.created", null, "Total de contas criadas com sucesso");
                case ACCOUNT_CREATION_FAILED -> new MeterSpec("user.account.creation.failed", null, "Total de tentativas de criação de conta falhadas");
                case ORDER_CREATED -> new MeterSpec("order.created", null, "Total de pedidos criados");
                case ORDER_COMPLETED -> new MeterSpec("order.completed", null, "Total de pedidos concluídos");
                case ORDER_CANCELLED -> new MeterSpec("order.cancelled", null, "Total de pedidos cancelados");
                case ORDER_UPDATE_FAILED -> new MeterSpec("order.update.failed", null, "Total de falhas ao atualizar pedidos");
                case INSUFFICIENT_STOCK -> new MeterSpec("inventory.insufficient.stock", null, "Número de vezes que houve estoque insuficiente");
                case COMPONENT_CREATED -> new MeterSpec("component.created", null, "Total de componentes criados");
                case COMPONENT_UPDATED -> new MeterSpec("component.updated", null, "Total de componentes atualizados");
                case STOCK_DECREASED -> new MeterSpec("inventory.stock.decreased", null, "Número de vezes que o estoque foi decrementado");
                case DEMAND_CREATED -> new MeterSpec("demand.created", null, "Total de demandas criadas");
                case DEMAND_REMOVED -> new MeterSpec("demand.removed", null, "Total de demandas removidas");
                case EMAIL_SENT -> new MeterSpec("email.sent", null, "Total de emails enviados com sucesso");
                case EMAIL_FAILED -> new MeterSpec("email.failed", null, "Total de falhas ao enviar emails");
                case ANNOUNCEMENT_CREATED -> new MeterSpec("announcement.created", null, "Total de anúncios criados");
                case ANNOUNCEMENT_EMAILS_SENT -> new MeterSpec("announcement.emails.sent", null, "Total de emails de anúncio enviados");
                case CACHE_HIT -> new MeterSpec("cache.hit", "all", "Total de cache hits");
                case CACHE_MISS -> new MeterSpec("cache.miss", "all", "Total de cache misses");
                case CACHE_EVICTION -> new MeterSpec("cache.eviction", "all", "Total de cache evictions");
            };
        }
    }
}
