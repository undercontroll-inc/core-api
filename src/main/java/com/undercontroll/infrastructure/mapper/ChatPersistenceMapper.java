package com.undercontroll.infrastructure.mapper;

import com.undercontroll.domain.model.chat.Conversation;
import com.undercontroll.domain.model.chat.StoredChatMessage;
import com.undercontroll.infrastructure.persistence.entity.ChatConversationJpaEntity;
import com.undercontroll.infrastructure.persistence.entity.ChatMessageJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ChatPersistenceMapper {

    @Mapping(target = "preview", source = "lastMessagePreview")
    Conversation toDomain(ChatConversationJpaEntity entity);

    StoredChatMessage toDomain(ChatMessageJpaEntity entity);
}
