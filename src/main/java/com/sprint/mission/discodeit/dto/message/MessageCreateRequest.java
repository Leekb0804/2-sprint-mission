package com.sprint.mission.discodeit.dto.message;

import com.sprint.mission.discodeit.dto.binarycontent.BinaryContentCreateRequest;

import java.util.List;
import java.util.UUID;

public record MessageCreateRequest(
        UUID senderId,
        String content,
        UUID channelId,
        List<BinaryContentCreateRequest> requests
) {
}
