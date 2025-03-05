package com.sprint.mission.discodeit.repository;

import com.sprint.mission.discodeit.entity.Message;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends Repository<Message> {
    List<Message> findMessageListByChannelId(UUID channelId);
}
