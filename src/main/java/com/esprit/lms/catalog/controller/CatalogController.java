package com.esprit.lms.catalog.controller;

import com.esprit.lms.catalog.dto.*;
import com.esprit.lms.catalog.entity.ItemType;
import com.esprit.lms.catalog.entity.PhysicalCopy;
import com.esprit.lms.catalog.service.CatalogService;
import com.esprit.lms.users.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;
    private final UserService userService;

    /**
     * GET /api/v1/catalog — Paginated catalog listing with optional type filter.
     */
    @GetMapping
    public Page<CatalogSummaryDTO> listItems(
            @RequestParam(required = false) ItemType type,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return catalogService.listItems(type, pageable);
    }

    /**
     * GET /api/v1/catalog/search — Keyword search (semantic wired in Sprint 8).
     */
    @GetMapping("/search")
    public List<CatalogSummaryDTO> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "false") boolean semantic,
            @RequestParam(defaultValue = "20") int limit) {
        // TODO Sprint 8: if semantic=true, call unified search with embeddings
        return catalogService.keywordSearch(q, limit);
    }

    /**
     * GET /api/v1/catalog/{id} — Full item detail with access check.
     */
    @GetMapping("/{id}")
    public CatalogDetailDTO getDetail(@PathVariable UUID id) {
        UUID userId = userService.getOrCreateCurrentUser().getId();
        return catalogService.getDetail(id, userId);
    }

    /**
     * POST /api/v1/catalog — Create item with file + cover upload.
     * Requires LIBRARIAN or ADMIN role.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public CatalogDetailDTO createItem(
            @RequestPart("metadata") @Valid CatalogCreateRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestPart(value = "cover", required = false) MultipartFile cover) {
        return catalogService.createItem(request, file, cover);
    }

    /**
     * PUT /api/v1/catalog/{id} — Update item metadata + optional new cover.
     * Requires LIBRARIAN or ADMIN role.
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CatalogDetailDTO updateItem(
            @PathVariable UUID id,
            @RequestPart("metadata") @Valid CatalogCreateRequest request,
            @RequestPart(value = "cover", required = false) MultipartFile cover) {
        return catalogService.updateItem(id, request, cover);
    }

    /**
     * DELETE /api/v1/catalog/{id} — Soft-archive item.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archiveItem(@PathVariable UUID id) {
        catalogService.archiveItem(id);
    }

    /**
     * POST /api/v1/catalog/{id}/reindex — Re-trigger AI indexing.
     */
    @PostMapping("/{id}/reindex")
    public void reindexItem(@PathVariable UUID id) {
        catalogService.triggerReindex(id);
    }

    // ── Physical Copy Management ──

    @PostMapping("/{itemId}/copies")
    @ResponseStatus(HttpStatus.CREATED)
    public PhysicalCopy addCopy(@PathVariable UUID itemId, @RequestParam String barcode) {
        return catalogService.addCopy(itemId, barcode);
    }

    @GetMapping("/{itemId}/copies")
    public List<PhysicalCopy> getCopies(@PathVariable UUID itemId) {
        return catalogService.getCopies(itemId);
    }

    @DeleteMapping("/copies/{copyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCopy(@PathVariable UUID copyId) {
        catalogService.deleteCopy(copyId);
    }
}
