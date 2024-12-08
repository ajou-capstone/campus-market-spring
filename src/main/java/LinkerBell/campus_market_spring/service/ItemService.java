package LinkerBell.campus_market_spring.service;

import LinkerBell.campus_market_spring.domain.Item;
import LinkerBell.campus_market_spring.domain.ItemPhotos;
import LinkerBell.campus_market_spring.domain.ItemStatus;
import LinkerBell.campus_market_spring.domain.Keyword;
import LinkerBell.campus_market_spring.domain.User;
import LinkerBell.campus_market_spring.dto.ItemDetailsViewResponseDto;
import LinkerBell.campus_market_spring.dto.ItemRegisterRequestDto;
import LinkerBell.campus_market_spring.dto.ItemRegisterResponseDto;
import LinkerBell.campus_market_spring.dto.ItemSearchRequestDto;
import LinkerBell.campus_market_spring.dto.ItemSearchResponseDto;
import LinkerBell.campus_market_spring.dto.ItemStatusChangeRequestDto;
import LinkerBell.campus_market_spring.dto.SliceResponse;
import LinkerBell.campus_market_spring.global.error.ErrorCode;
import LinkerBell.campus_market_spring.global.error.exception.CustomException;
import LinkerBell.campus_market_spring.repository.ChatRoomRepository;
import LinkerBell.campus_market_spring.repository.ItemPhotosRepository;
import LinkerBell.campus_market_spring.repository.ItemRepository;
import LinkerBell.campus_market_spring.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ItemService {

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final ItemPhotosRepository itemPhotosRepository;
    private final ChatRoomRepository chatRoomRepository;

    private final S3Service s3Service;
    private final FcmService fcmService;
    private final KeywordService keywordService;
    private final NotificationHistoryService notificationHistoryService;

    @Transactional(readOnly = true)
    public SliceResponse<ItemSearchResponseDto> itemSearch(Long userId,
        ItemSearchRequestDto itemSearchRequestDto) {
        User user = getUserWithCampus(userId);

        return itemRepository.itemSearch(userId, user.getCampus().getCampusId(),
            itemSearchRequestDto.getName(),
            itemSearchRequestDto.getCategory(), itemSearchRequestDto.getMinPrice(),
            itemSearchRequestDto.getMaxPrice(), itemSearchRequestDto.getItemStatus(),
            itemSearchRequestDto.getPageable());
    }

    public ItemRegisterResponseDto itemRegister(Long userId,
        ItemRegisterRequestDto itemRegisterRequestDto) {
        User user = getUserWithCampus(userId);

        Item newItem = itemRegisterDtoToItem(itemRegisterRequestDto, user);
        Item savedItem = itemRepository.save(newItem);

        if (itemRegisterRequestDto.getImages() != null && !itemRegisterRequestDto.getImages()
            .isEmpty()) {
            List<ItemPhotos> itemPhotos = imagesToItemPhotos(itemRegisterRequestDto, savedItem);
            itemPhotosRepository.saveAll(itemPhotos);
        }

        List<Keyword> sendingKeywords = keywordService.findKeywordsWithSameItemCampusAndTitle(
            savedItem);

        notificationHistoryService.saveNotificationHistory(sendingKeywords, savedItem);
        fcmService.sendFcmMessageWithKeywords(sendingKeywords, savedItem);

        return new ItemRegisterResponseDto(savedItem.getItemId());

    }

    @Transactional(readOnly = true)
    public ItemDetailsViewResponseDto viewItemDetails(Long userId, Long itemId) {
        User user = getUserWithCampus(userId);

        Item item = getItem(itemId);

        if (item.isDeleted()) {
            throw new CustomException(ErrorCode.DELETED_ITEM_ID);
        }

        if (UserCampusUniversityIsMatchedByItemCampusUniversity(user, item)) {
            throw new CustomException(ErrorCode.NOT_MATCH_USER_UNIVERSITY_WITH_ITEM_UNIVERSITY);
        }

        return itemRepository.findByItemDetails(userId, itemId);

    }

    public void updateItem(Long userId, Long itemId,
        ItemRegisterRequestDto itemRegisterRequestDto) {
        User user = getUserWithCampus(userId);

        Item item = getItem(itemId);

        if (item.isDeleted()) {
            throw new CustomException(ErrorCode.DELETED_ITEM_ID);
        }

        if (UserCampusUniversityIsMatchedByItemCampusUniversity(user, item)) {
            throw new CustomException(ErrorCode.NOT_MATCH_USER_UNIVERSITY_WITH_ITEM_UNIVERSITY);
        }

        if (userIsNotEqualsToItemUser(user, item)) {
            throw new CustomException(ErrorCode.NOT_MATCH_USER_ID_WITH_ITEM_USER_ID);
        }

        List<ItemPhotos> existingItemPhotos = itemPhotosRepository.findByItem_itemId(itemId);
        List<String> newImageAddresses = itemRegisterRequestDto.getImages();

        if (newImageAddresses == null) {
            newImageAddresses = new ArrayList<>();
        }

        updateItemPhotos(existingItemPhotos, newImageAddresses,
            itemRegisterRequestDto.getThumbnail(), item);

        updateItemProperties(itemRegisterRequestDto, item);

    }

    public void deleteItem(Long userId, Long itemId) {
        User user = getUserWithCampus(userId);

        Item item = getItem(itemId);

        if (item.isDeleted()) {
            throw new CustomException(ErrorCode.DELETED_ITEM_ID);
        }

        if (UserCampusUniversityIsMatchedByItemCampusUniversity(user, item)) {
            throw new CustomException(ErrorCode.NOT_MATCH_USER_UNIVERSITY_WITH_ITEM_UNIVERSITY);
        }

        if (userIsNotEqualsToItemUser(user, item)) {
            throw new CustomException(ErrorCode.NOT_MATCH_USER_ID_WITH_ITEM_USER_ID);
        }

        item.setDeleted(true);
    }

    public void changeItemStatus(Long userId, Long itemId,
        ItemStatusChangeRequestDto itemStatusChangeRequestDto) {
        User user = getUserWithCampus(userId);
        User userBuyer = getUserWithCampus(itemStatusChangeRequestDto.getBuyerId());
        Item item = getItem(itemId);

        if (item.isDeleted()) {
            throw new CustomException(ErrorCode.DELETED_ITEM_ID);
        }

        if (UserCampusUniversityIsMatchedByItemCampusUniversity(user, item)) {
            throw new CustomException(ErrorCode.NOT_MATCH_USER_UNIVERSITY_WITH_ITEM_UNIVERSITY);
        }

        if (userIsNotEqualsToItemUser(user, item)) {
            throw new CustomException(ErrorCode.NOT_MATCH_USER_ID_WITH_ITEM_USER_ID);
        }

        if (item.getItemStatus() == ItemStatus.SOLDOUT || item.getUserBuyer() != null) {
            throw new CustomException(ErrorCode.ALREADY_SOLD_OUT_ITEM);
        }

        if (itemStatusChangeRequestDto.getItemStatus() != ItemStatus.SOLDOUT) {
            throw new CustomException(ErrorCode.DO_NOT_ROLL_BACK_ITEM_STATUS_FOR_SALE);
        }

        if (isChatRoomExistsForUserBuyerAndItem(userBuyer, item)) {
            throw new CustomException(ErrorCode.INVALID_ITEM_BUYER);
        }
        item.setItemStatus(itemStatusChangeRequestDto.getItemStatus());
        item.setUserBuyer(userBuyer);
    }

    private boolean isChatRoomExistsForUserBuyerAndItem(User userBuyer, Item item) {
        return !chatRoomRepository.existsByUser_userIdAndItem_itemId(
            userBuyer.getUserId(),
            item.getItemId());
    }

    private void updateItemProperties(ItemRegisterRequestDto itemRegisterRequestDto, Item item) {
        item.setTitle(itemRegisterRequestDto.getTitle());
        item.setDescription(itemRegisterRequestDto.getDescription());
        item.setPrice(itemRegisterRequestDto.getPrice());
        item.setCategory(itemRegisterRequestDto.getCategory());

        if (isNotEqualsToThumbnail(itemRegisterRequestDto, item)) {
            s3Service.deleteS3File(item.getThumbnail());
        }
        item.setThumbnail(itemRegisterRequestDto.getThumbnail());
    }

    private boolean isNotEqualsToThumbnail(ItemRegisterRequestDto itemRegisterRequestDto,
        Item item) {
        return !(item.getThumbnail().equals(itemRegisterRequestDto.getThumbnail()));
    }

    private void updateItemPhotos(List<ItemPhotos> existingItemPhotos,
        List<String> newImageAddresses, String thumbnail,
        Item item) {

        List<String> existingAddresses = existingItemPhotos.stream()
            .map(ItemPhotos::getImageAddress)
            .toList();

        newImageAddresses.stream()
            .filter(address -> !existingAddresses.contains(address))
            .forEach(address -> {
                ItemPhotos newPhoto = new ItemPhotos();
                newPhoto.registerItemPhotos(item, address);
                itemPhotosRepository.save(newPhoto);
            });

        existingItemPhotos.stream()
            .filter(photo -> !newImageAddresses.contains(photo.getImageAddress()))
            .forEach(photo -> {
                itemPhotosRepository.delete(photo);
                if (!thumbnail.equals(photo.getImageAddress())) {
                    s3Service.deleteS3File(photo.getImageAddress());
                }
            });
    }

    private boolean userIsNotEqualsToItemUser(User user, Item item) {
        return !Objects.equals(user.getUserId(), item.getUser().getUserId());
    }

    private boolean UserCampusUniversityIsMatchedByItemCampusUniversity(User user, Item item) {
        return !(user.getCampus().getUniversityName().equals(item.getCampus().getUniversityName()));
    }

    private Item getItem(Long itemId) {
        return itemRepository.findById(itemId)
            .orElseThrow(() -> new CustomException(ErrorCode.ITEM_NOT_FOUND));
    }

    private User getUserWithCampus(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.getCampus() == null) {
            throw new CustomException(ErrorCode.CAMPUS_NOT_FOUND);
        }
        return user;
    }

    private List<ItemPhotos> imagesToItemPhotos(ItemRegisterRequestDto itemRegisterRequestDto,
        Item savedItem) {
        return itemRegisterRequestDto
            .getImages()
            .stream()
            .map(photo -> {
                ItemPhotos itemPhoto = new ItemPhotos();
                itemPhoto.registerItemPhotos(savedItem, photo);
                return itemPhoto;
            }).toList();
    }

    private Item itemRegisterDtoToItem(ItemRegisterRequestDto itemRegisterRequestDto, User user) {
        return Item.builder()
            .user(user)
            .campus(user.getCampus())
            .title(itemRegisterRequestDto.getTitle())
            .category(itemRegisterRequestDto.getCategory())
            .description(itemRegisterRequestDto.getDescription())
            .price(itemRegisterRequestDto.getPrice())
            .thumbnail(itemRegisterRequestDto.getThumbnail())
            .build();
    }

}
