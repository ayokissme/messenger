package com.demo.chat.service.impl;

import com.demo.chat.model.Conversation;
import com.demo.chat.model.User;
import com.demo.chat.model.request.ConversationRequest;
import com.demo.chat.repo.ConversationRepo;
import com.demo.chat.service.ImageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static com.demo.chat.model.enums.ConversationType.MULTIPLE;

@Service
@Slf4j
public class ImageServiceImpl implements ImageService {

    @Value("${folder.images.profile.pic}")
    private String profilePicsFolder;
    @Value("${folder.images.conv.pic}")
    private String convPicsFolder;

    private final ConversationRepo conversationRepo;
    public static String absolutePath = Paths.get(".").toAbsolutePath().normalize().toString().replace("\\", "/");

    @Autowired
    public ImageServiceImpl(ConversationRepo conversationRepo) {
        this.conversationRepo = conversationRepo;
    }

    @Override
    public List<HashMap<Object, Object>> getAvatars(Conversation conversation) throws IOException {
        List<HashMap<Object, Object>> avatars = new ArrayList<>();
        if (conversation.getConversationType() == MULTIPLE) {
            avatars.add(getConversationAvatar(conversation));
        }

        conversation.getParticipants()
                .forEach(p -> {
                    try {
                        avatars.add(getUserAvatar(p));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
        return avatars;
    }

    @Override
    public Object getAllConversationsData(User user) {
        List<Conversation> allConversations =
                conversationRepo.findAllConversations(user)
                        .stream()
                        .sorted(Comparator.comparing(Conversation::getLastMessageCreatedAt).reversed())
                        .collect(Collectors.toList());
        HashMap<Object, Object> data = new HashMap<>();
        data.put("conversations", allConversations);
        data.put("avatars", getAvatarsForConversationsPage(allConversations, user));
        return data;
    }

    @Override
    public String getPath() {
        return absolutePath + profilePicsFolder;
    }

    public List<HashMap<Object, Object>> getAvatarsForConversationsPage(List<Conversation> allConversations, User user) {
        List<HashMap<Object, Object>> avatars = new ArrayList<>();

        allConversations.forEach(conversation -> {
            HashMap<Object, Object> avatar = new HashMap<>();

            try {
                String encodedImage;
                if (conversation.getConversationType() == MULTIPLE) {
                    encodedImage = getEncodedImage(conversation);
                    avatar.put(conversation.getConversationId(), getEncodedImage(conversation));
                } else {
                    User convUser = conversation.getParticipants()
                            .stream()
                            .filter(p -> !p.getId().equals(user.getId()))
                            .findAny()
                            .orElseThrow();
                    encodedImage = getEncodedImage(convUser);
                }
                avatar.put("id", conversation.getConversationId());
                avatar.put("img", encodedImage);
            } catch (IOException e) {
                log.error(e.getMessage());
            }
            avatars.add(avatar);
        });
        return avatars;
    }

    public HashMap<Object, Object> getUserAvatar(User user) throws IOException {
        return putAvatarToHashMap(user.getId(), getEncodedImage(user));
    }

    public HashMap<Object, Object> getConversationAvatar(Conversation conversation) throws IOException {
        return putAvatarToHashMap(conversation.getConversationId(), getEncodedImage(conversation));
    }

    public HashMap<Object, Object> putAvatarToHashMap(Object id, String encodedImage) {
        HashMap<Object, Object> avatar = new HashMap<>();
        avatar.put("id", id);
        avatar.put("img", encodedImage);
        return avatar;
    }

    public String getEncodedImage() throws IOException {
        return this.encodeImage(absolutePath + convPicsFolder + "no-conv-picture.png");
    }

    public String getEncodedImage(User user) throws IOException {
        return this.encodeImage(absolutePath + profilePicsFolder + user.getImage());
    }

    public String getEncodedImage(Conversation conversation) throws IOException {
        return this.encodeImage(absolutePath + convPicsFolder + conversation.getImage());
    }

    public String encodeImage(String image) throws IOException {
        byte[] fileContent = FileUtils.readFileToByteArray(new File(image));
        return Base64.getEncoder().encodeToString(fileContent);
    }

    public void encodeImageToBytesAndSave(String imgName, String img) throws IOException {
        byte[] decodedBytes = Base64.getDecoder().decode(img);
        String path = absolutePath + convPicsFolder + imgName;
        Files.write(Path.of(path), decodedBytes);
    }
}
