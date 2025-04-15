package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.data.ChannelDto;
import com.sprint.mission.discodeit.dto.request.PrivateChannelCreateRequest;
import com.sprint.mission.discodeit.dto.request.PublicChannelCreateRequest;
import com.sprint.mission.discodeit.dto.request.PublicChannelUpdateRequest;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.MessageRepository;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.service.ChannelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class BasicChannelService implements ChannelService {

  private final UserRepository userRepository;
  private final ChannelRepository channelRepository;
  //
  private final ReadStatusRepository readStatusRepository;
  private final MessageRepository messageRepository;

  @Transactional
  @Override
  public Channel create(PublicChannelCreateRequest request) {
    String name = request.name();
    String description = request.description();
    Channel channel = new Channel(ChannelType.PUBLIC, name, description);

    return channelRepository.save(channel);
  }

  @Transactional
  @Override
  public Channel create(PrivateChannelCreateRequest request) {
    Channel channel = new Channel(ChannelType.PRIVATE, null, null);
    Channel createdChannel = channelRepository.save(channel);

    userRepository.findAllById(request.participantIds()).stream()
        .map(user -> new ReadStatus(user, createdChannel, channel.getCreatedAt()))
        .forEach(readStatusRepository::save);

    return createdChannel;
  }

  @Transactional(readOnly = true)
  @Override
  public ChannelDto find(UUID channelId) {
    return channelRepository.findById(channelId)
        .map(this::toDto)
        .orElseThrow(
            () -> new NoSuchElementException("Channel with id " + channelId + " not found"));
  }

  @Transactional(readOnly = true)
  @Override
  public List<ChannelDto> findAllByUserId(UUID userId) {
    List<UUID> mySubscribedChannelIds = readStatusRepository.findAllByUserId(userId).stream()
        .map(readStatus -> readStatus.getChannel().getId())
        .toList();

    return channelRepository.findAll().stream()
        .filter(channel ->
            channel.getType().equals(ChannelType.PUBLIC)
                || mySubscribedChannelIds.contains(channel.getId())
        )
        .map(this::toDto)
        .toList();
  }

  @Transactional
  @Override
  public Channel update(UUID channelId, PublicChannelUpdateRequest request) {
    String newName = request.newName();
    String newDescription = request.newDescription();
    Channel channel = this.findChannelById(channelId);
    if (channel.getType().equals(ChannelType.PRIVATE)) {
      throw new IllegalArgumentException("Private channel cannot be updated");
    }
    channel.update(newName, newDescription);
    return channel;
  }

  @Transactional
  @Override
  public void delete(UUID channelId) {
    Channel channel = this.findChannelById(channelId);
    messageRepository.deleteAllByChannelId(channelId);
    readStatusRepository.deleteAllByChannelId(channelId);

    channelRepository.delete(channel);
  }

  private ChannelDto toDto(Channel channel) {
    Instant lastMessageAt = messageRepository.findFirstByChannelIdOrderByCreatedAtDesc(
            channel.getId())
        .map(Message::getCreatedAt)
        .orElse(Instant.MIN);

    List<UUID> participantIds = new ArrayList<>();
    if (channel.getType().equals(ChannelType.PRIVATE)) {
      readStatusRepository.findAllByChannelId(channel.getId())
          .stream()
          .map(readStatus -> readStatus.getUser().getId())
          .forEach(participantIds::add);
    }

    return new ChannelDto(
        channel.getId(),
        channel.getType(),
        channel.getName(),
        channel.getDescription(),
        participantIds,
        lastMessageAt
    );
  }

  private Channel findChannelById(UUID channelId) {
    return channelRepository.findById(channelId)
        .orElseThrow(
            () -> new NoSuchElementException("Channel with id " + channelId + " not found")
        );
  }
}
