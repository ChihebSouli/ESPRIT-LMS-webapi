package com.esprit.lms.catalog.repository;

import com.esprit.lms.catalog.entity.CatalogItem;
import com.esprit.lms.catalog.entity.ItemType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CatalogItemRepository extends JpaRepository<CatalogItem, UUID> {

    Page<CatalogItem> findAllByIsActiveTrue(Pageable pageable);

    Page<CatalogItem> findByTypeAndIsActiveTrue(ItemType type, Pageable pageable);

    /**
     * Keyword full-text search using PostgreSQL GIN index.
     */
    @Query(value = """
            SELECT * FROM catalog_items
            WHERE is_active = true
              AND to_tsvector('french', coalesce(title,'') || ' ' || coalesce(author,'') || ' ' || coalesce(subject,''))
                  @@ websearch_to_tsquery('french', :query)
            ORDER BY ts_rank(
                to_tsvector('french', coalesce(title,'') || ' ' || coalesce(author,'') || ' ' || coalesce(subject,'')),
                websearch_to_tsquery('french', :query)
            ) DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<CatalogItem> keywordSearch(@Param("query") String query, @Param("limit") int limit);

    /**
     * Semantic search using pgvector HNSW cosine distance.
     * queryVector must be passed as a pgvector-formatted string: '[v1,v2,...,v1024]'
     */
    @Query(value = """
            SELECT *, 1 - (embedding <=> CAST(:vec AS vector)) AS score
            FROM catalog_items
            WHERE is_active = true AND ai_status = 'COMPLETE'
            ORDER BY embedding <=> CAST(:vec AS vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<CatalogItem> semanticSearch(@Param("vec") String queryVector, @Param("limit") int limit);
}
