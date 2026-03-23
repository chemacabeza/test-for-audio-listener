package com.audiolistener.repository;

import com.audiolistener.entity.Transcription;
import com.audiolistener.entity.TranscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TranscriptionRepository extends JpaRepository<Transcription, Long> {

    List<Transcription> findAllByOrderByCreatedAtDesc();

    List<Transcription> findByStatus(TranscriptionStatus status);

    @Query("SELECT t FROM Transcription t WHERE " +
            "LOWER(t.filename) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(t.transcriptText) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "ORDER BY t.createdAt DESC")
    List<Transcription> searchByQuery(@Param("query") String query);
}
