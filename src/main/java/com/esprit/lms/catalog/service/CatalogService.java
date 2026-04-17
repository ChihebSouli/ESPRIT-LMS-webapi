

package com.esprit.lms.catalog.service;

import com.esprit.lms.catalog.dto.*;
import com.esprit.lms.catalog.entity.*;
import com.esprit.lms.catalog.repository.CatalogItemRepository;
import com.esprit.lms.catalog.repository.PhysicalCopyRepository;
import com.esprit.lms.shared.exception.ApiException;
import com.esprit.lms.shared.storage.MinioStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogService {

    private final CatalogItemRepository catalogRepo;
    private final PhysicalCopyRepository copyRepo;
    private final MinioStorageService storageService;
    private final ApplicationEventPublisher eventPublisher;

    // ── List & Search ──

    public Page<CatalogSummaryDTO> listItems(ItemType type, Pageable pageable) {
        Page<CatalogItem> page = (type != null)
                ? catalogRepo.findByTypeAndIsActiveTrue(type, pageable)
                : catalogRepo.findAllByIsActiveTrue(pageable);
        return page.map(this::toSummary);
    }

    public List<CatalogSummaryDTO> keywordSearch(String query, int limit) {
        return catalogRepo.keywordSearch(query, limit).stream()
                .map(this::toSummary)
                .toList();
    }

    // ── Detail ──

    public CatalogDetailDTO getDetail(UUID id, UUID currentUserId) {
        CatalogItem item = findActiveItem(id);
        int availableCopies = copyRepo.countByItemIdAndIsAvailableTrue(id);

        // TODO: check purchase table for hasAccess when purchases module is built
        boolean hasAccess = item.isFree();

        return CatalogDetailDTO.builder()
                .id(item.getId())
                .type(item.getType())
                .title(item.getTitle())
                .author(item.getAuthor())
                .subject(item.getSubject())
                .language(item.getLanguage())
                .year(item.getYear())
                .coverImageUrl(item.getCoverImageUrl())
                .price(item.getPrice())
                .free(item.isFree())
                .digital(item.isDigital())
                .aiStatus(item.getAiStatus())
                .aiSummary(item.getAiSummary())
                .aiTags(item.getAiTags())
                .hasAccess(hasAccess)
                .availableCopies(availableCopies)
                .createdAt(item.getCreatedAt())
                .build();
    }

    // ── Create with MinIO upload ──

    @Transactional
    public CatalogDetailDTO createItem(CatalogCreateRequest request,
                                        MultipartFile file,
                                        MultipartFile cover) {
        CatalogItem item = CatalogItem.builder()
                .type(request.getType())
                .title(request.getTitle())
                .author(request.getAuthor())
                .subject(request.getSubject())
                .language(request.getLanguage() != null ? request.getLanguage() : "FR")
                .year(request.getYear())
                .price(request.getPrice())
                .aiStatus(AiStatus.PENDING)
                .build();

        // Upload file to MinIO private bucket
        if (file != null && !file.isEmpty()) {
            String objectKey = storageService.uploadDocument(file);
            item.setFileBlobKey(objectKey);
        }

        // Upload cover to MinIO public bucket
        if (cover != null && !cover.isEmpty()) {
            String coverUrl = storageService.uploadCover(cover);
            item.setCoverImageUrl(coverUrl);
        }

        item = catalogRepo.save(item);
        log.info("Created catalog item: {} [{}]", item.getTitle(), item.getId());

        // Trigger async AI indexing pipeline (will be wired in Sprint 7)
        // eventPublisher.publishEvent(new ItemCreatedEvent(item.getId()));

        return getDetail(item.getId(), null);
    }

    // ── Update ──

    @Transactional
    public CatalogDetailDTO updateItem(UUID id, CatalogCreateRequest request, MultipartFile cover) {
        CatalogItem item = findActiveItem(id);

        item.setTitle(request.getTitle());
        item.setAuthor(request.getAuthor());
        item.setSubject(request.getSubject());
        item.setLanguage(request.getLanguage());
        item.setYear(request.getYear());
        item.setPrice(request.getPrice());

        if (cover != null && !cover.isEmpty()) {
            String coverUrl = storageService.uploadCover(cover);
            item.setCoverImageUrl(coverUrl);
        }

        catalogRepo.save(item);
        log.info("Updated catalog item: {} [{}]", item.getTitle(), id);

        return getDetail(id, null);
    }

    // ── Archive (soft delete) ──

    @Transactional
    public void archiveItem(UUID id) {
        CatalogItem item = findActiveItem(id);
        item.setIsActive(false);


        catalogRepo.save(item);
        log.info("Archived catalog item: {} [{}]", item.getTitle(), id);
    }

    // ── Physical Copy Management ──

    @Transactional
    public PhysicalCopy addCopy(UUID itemId, String barcode) {
        CatalogItem item = findActiveItem(itemId);
        if (copyRepo.existsByBarcode(barcode)) {
            throw ApiException.conflict("Barcode already exists: " + barcode);
        }
        PhysicalCopy copy = PhysicalCopy.builder()
                .item(item)
                .barcode(barcode)
                .condition(CopyCondition.NEW)
                .isAvailable(true)
                .build();
        return copyRepo.save(copy);
    }

    public List<PhysicalCopy> getCopies(UUID itemId) {
        return copyRepo.findByItemId(itemId);
    }

    @Transactional
    public void deleteCopy(UUID copyId) {
        copyRepo.deleteById(copyId);
    }

    // ── AI Reindex trigger ──

    @Transactional

    public void triggerReindex(UUID itemId) {
        CatalogItem item = findActiveItem(itemId);
        item.setAiStatus(AiStatus.PENDING);
        catalogRepo.save(item);
        log.info("Reindex triggered for: {} [{}]", item.getTitle(), itemId);
        // eventPublisher.publishEvent(new ItemCreatedEvent(item.getId()));
    }

    // ── Helpers ──

    private CatalogItem findActiveItem(UUID id) {
        return catalogRepo.findById(id)

                .filter(CatalogItem::getIsActive)
                .orElseThrow(() -> ApiException.notFound("Catalog item not found: " + id));
    }





    private CatalogSummaryDTO toSummary(CatalogItem item) {
        return CatalogSummaryDTO.builder()
                .id(item.getId())
                .type(item.getType())
                .title(item.getTitle())
                .author(item.getAuthor())
                .subject(item.getSubject())
                .language(item.getLanguage())
                .year(item.getYear())
                .coverImageUrl(item.getCoverImageUrl())
                .price(item.getPrice())
                .free(item.isFree())
                .aiStatus(item.getAiStatus())
                .aiTags(item.getAiTags())
                .build();
    }
}
