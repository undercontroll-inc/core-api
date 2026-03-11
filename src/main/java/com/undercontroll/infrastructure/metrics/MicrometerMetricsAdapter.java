package com.undercontroll.infrastructure.metrics;

import com.undercontroll.application.port.MetricsPort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class MicrometerMetricsAdapter implements MetricsPort {

    private final Counter loginFailedCounter;
    private final Counter loginSuccessCounter;
    private final Counter googleLoginSuccessCounter;
    private final Counter googleLoginFailedCounter;
    private final Counter accountCreatedCounter;
    private final Counter accountCreationFailedCounter;
    private final Counter orderCreatedCounter;
    private final Counter orderCompletedCounter;
    private final Counter orderCancelledCounter;
    private final Counter orderUpdateFailedCounter;
    private final Timer orderProcessingTimer;
    private final Counter insufficientStockCounter;
    private final Counter componentCreatedCounter;
    private final Counter componentUpdatedCounter;
    private final Counter stockDecreasedCounter;
    private final Counter demandCreatedCounter;
    private final Counter demandRemovedCounter;
    private final Counter emailSentCounter;
    private final Counter emailFailedCounter;
    private final Counter announcementCreatedCounter;
    private final Counter announcementEmailsSentCounter;
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;
    private final Counter cacheEvictionCounter;

    public MicrometerMetricsAdapter(MeterRegistry meterRegistry) {
        this.loginFailedCounter = Counter.builder("auth.login.failed")
                .description("Total de tentativas de login falhadas")
                .tag("type", "password")
                .register(meterRegistry);

        this.loginSuccessCounter = Counter.builder("auth.login.success")
                .description("Total de logins bem-sucedidos")
                .tag("type", "password")
                .register(meterRegistry);

        this.googleLoginSuccessCounter = Counter.builder("auth.login.success")
                .description("Total de logins bem-sucedidos via Google")
                .tag("type", "google")
                .register(meterRegistry);

        this.googleLoginFailedCounter = Counter.builder("auth.login.failed")
                .description("Total de tentativas de login falhadas via Google")
                .tag("type", "google")
                .register(meterRegistry);

        this.accountCreatedCounter = Counter.builder("user.account.created")
                .description("Total de contas criadas com sucesso")
                .register(meterRegistry);

        this.accountCreationFailedCounter = Counter.builder("user.account.creation.failed")
                .description("Total de tentativas de criação de conta falhadas")
                .register(meterRegistry);

        this.orderCreatedCounter = Counter.builder("order.created")
                .description("Total de pedidos criados")
                .register(meterRegistry);

        this.orderCompletedCounter = Counter.builder("order.completed")
                .description("Total de pedidos concluídos")
                .register(meterRegistry);

        this.orderCancelledCounter = Counter.builder("order.cancelled")
                .description("Total de pedidos cancelados")
                .register(meterRegistry);

        this.orderUpdateFailedCounter = Counter.builder("order.update.failed")
                .description("Total de falhas ao atualizar pedidos")
                .register(meterRegistry);

        this.orderProcessingTimer = Timer.builder("order.processing.time")
                .description("Tempo de processamento de criação de pedidos")
                .register(meterRegistry);

        this.insufficientStockCounter = Counter.builder("inventory.insufficient.stock")
                .description("Número de vezes que houve estoque insuficiente")
                .register(meterRegistry);

        this.componentCreatedCounter = Counter.builder("component.created")
                .description("Total de componentes criados")
                .register(meterRegistry);

        this.componentUpdatedCounter = Counter.builder("component.updated")
                .description("Total de componentes atualizados")
                .register(meterRegistry);

        this.stockDecreasedCounter = Counter.builder("inventory.stock.decreased")
                .description("Número de vezes que o estoque foi decrementado")
                .register(meterRegistry);

        this.demandCreatedCounter = Counter.builder("demand.created")
                .description("Total de demandas criadas")
                .register(meterRegistry);

        this.demandRemovedCounter = Counter.builder("demand.removed")
                .description("Total de demandas removidas")
                .register(meterRegistry);

        this.emailSentCounter = Counter.builder("email.sent")
                .description("Total de emails enviados com sucesso")
                .register(meterRegistry);

        this.emailFailedCounter = Counter.builder("email.failed")
                .description("Total de falhas ao enviar emails")
                .register(meterRegistry);

        this.announcementCreatedCounter = Counter.builder("announcement.created")
                .description("Total de anúncios criados")
                .register(meterRegistry);

        this.announcementEmailsSentCounter = Counter.builder("announcement.emails.sent")
                .description("Total de emails de anúncio enviados")
                .register(meterRegistry);

        this.cacheHitCounter = Counter.builder("cache.hit")
                .description("Total de cache hits")
                .tag("type", "all")
                .register(meterRegistry);

        this.cacheMissCounter = Counter.builder("cache.miss")
                .description("Total de cache misses")
                .tag("type", "all")
                .register(meterRegistry);

        this.cacheEvictionCounter = Counter.builder("cache.eviction")
                .description("Total de cache evictions")
                .tag("type", "all")
                .register(meterRegistry);

        log.info("MicrometerMetricsService initialized with all business metrics counters");
    }

    @Override
    public void incrementLoginFailed() {
        loginFailedCounter.increment();
        log.debug("Login failed counter incremented");
    }

    @Override
    public void incrementLoginSuccess() {
        loginSuccessCounter.increment();
        log.debug("Login success counter incremented");
    }

    @Override
    public void incrementGoogleLoginFailed() {
        googleLoginFailedCounter.increment();
        log.debug("Google login failed counter incremented");
    }

    @Override
    public void incrementGoogleLoginSuccess() {
        googleLoginSuccessCounter.increment();
        log.debug("Google login success counter incremented");
    }

    @Override
    public void incrementAccountCreated() {
        accountCreatedCounter.increment();
        log.debug("Account created counter incremented");
    }

    @Override
    public void incrementAccountCreationFailed() {
        accountCreationFailedCounter.increment();
        log.debug("Account creation failed counter incremented");
    }

    @Override
    public void incrementOrderCreated() {
        orderCreatedCounter.increment();
        log.debug("Order created counter incremented");
    }

    @Override
    public void incrementOrderCompleted() {
        orderCompletedCounter.increment();
        log.debug("Order completed counter incremented");
    }

    @Override
    public void incrementOrderCancelled() {
        orderCancelledCounter.increment();
        log.debug("Order cancelled counter incremented");
    }

    @Override
    public void incrementOrderUpdateFailed() {
        orderUpdateFailedCounter.increment();
        log.debug("Order update failed counter incremented");
    }

    @Override
    public void recordOrderProcessingTime(long startTimeMillis) {
        long duration = System.currentTimeMillis() - startTimeMillis;
        orderProcessingTimer.record(duration, TimeUnit.MILLISECONDS);
        log.debug("Order processing time recorded: {}ms", duration);
    }

    @Override
    public void incrementInsufficientStock(String componentName) {
        insufficientStockCounter.increment();
        log.warn("Insufficient stock counter incremented for component: {}", componentName);
    }

    @Override
    public void incrementComponentCreated() {
        componentCreatedCounter.increment();
        log.debug("Component created counter incremented");
    }

    @Override
    public void incrementComponentUpdated() {
        componentUpdatedCounter.increment();
        log.debug("Component updated counter incremented");
    }

    @Override
    public void incrementStockDecreased() {
        stockDecreasedCounter.increment();
        log.debug("Stock decreased counter incremented");
    }

    @Override
    public void incrementDemandCreated() {
        demandCreatedCounter.increment();
        log.debug("Demand created counter incremented");
    }

    @Override
    public void incrementDemandRemoved() {
        demandRemovedCounter.increment();
        log.debug("Demand removed counter incremented");
    }

    @Override
    public void incrementEmailSent() {
        emailSentCounter.increment();
        log.debug("Email sent counter incremented");
    }

    @Override
    public void incrementEmailFailed() {
        emailFailedCounter.increment();
        log.debug("Email failed counter incremented");
    }

    @Override
    public void incrementAnnouncementCreated() {
        announcementCreatedCounter.increment();
        log.debug("Announcement created counter incremented");
    }

    @Override
    public void incrementAnnouncementEmailsSent(int count) {
        announcementEmailsSentCounter.increment(count);
        log.debug("Announcement emails sent counter incremented by {}", count);
    }

    @Override
    public void incrementCacheHit(String cacheName) {
        cacheHitCounter.increment();
        log.debug("Cache hit counter incremented for cache: {}", cacheName);
    }

    @Override
    public void incrementCacheMiss(String cacheName) {
        cacheMissCounter.increment();
        log.debug("Cache miss counter incremented for cache: {}", cacheName);
    }

    @Override
    public void incrementCacheEviction(String cacheName) {
        cacheEvictionCounter.increment();
        log.debug("Cache eviction counter incremented for cache: {}", cacheName);
    }
}
