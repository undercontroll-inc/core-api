package com.undercontroll.infrastructure.ai.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Map;

class OnAiFeatureCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Map<String, Object> attributes = metadata.getAnnotationAttributes(ConditionalOnAiFeature.class.getName());
        String feature = String.valueOf(attributes.get("value"));
        String provider = context.getEnvironment().getProperty("undercontroll.ai." + feature + ".provider");
        return AiProvider.parse(feature, provider) != AiProvider.NONE;
    }
}
