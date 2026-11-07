package com.undercontroll.application.mapper;

import com.undercontroll.application.dto.chat.ChatMessageDto;
import com.undercontroll.application.dto.chat.ConversationMessagesDto;
import com.undercontroll.application.dto.chat.ConversationPageDto;
import com.undercontroll.application.dto.chat.ConversationSummaryDto;
import com.undercontroll.domain.model.chat.Conversation;
import com.undercontroll.domain.model.chat.ConversationPage;
import com.undercontroll.domain.model.chat.MessagePage;
import com.undercontroll.domain.model.chat.StoredChatMessage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.Locale;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ChatConversationDtoMapper {

    @Mapping(target = "conversationId", source = "publicId")
    ConversationSummaryDto toDto(Conversation conversation);

    ConversationPageDto toDto(ConversationPage page);

    @Mapping(target = "role", expression = "java(lower(message.role()))")
    @Mapping(target = "state", expression = "java(lower(message.state()))")
    ChatMessageDto toDto(StoredChatMessage message);

    default ConversationMessagesDto toDto(String conversationId, MessagePage page) {
        return new ConversationMessagesDto(
                conversationId,
                page.items().stream().map(this::toDto).toList(),
                page.nextCursor()
        );
    }

    default String lower(Enum<?> value) {
        return value == null ? null : value.name().toLowerCase(Locale.ROOT);
    }
}
