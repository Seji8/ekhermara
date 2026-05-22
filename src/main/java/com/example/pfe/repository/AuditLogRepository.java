    package com.example.pfe.repository;

    import com.example.pfe.models.AuditLog;
    import org.springframework.data.jpa.repository.JpaRepository;
    import org.springframework.data.jpa.repository.Query;
    import org.springframework.data.repository.query.Param;
    import org.springframework.stereotype.Repository;

    import java.time.LocalDateTime;
    import java.util.List;

    @Repository
    public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

        List<AuditLog> findAllByOrderByDateDesc();

        List<AuditLog> findByDateBetweenOrderByDateDesc(LocalDateTime start, LocalDateTime end);

        List<AuditLog> findByActionOrderByDateDesc(String action);

        List<AuditLog> findByUserIdOrderByDateDesc(Long userId);

        @Query("SELECT a FROM AuditLog a WHERE " +
               "(:action IS NULL OR a.action = :action) AND " +
               "(:userId IS NULL OR a.user.id = :userId) AND " +
               "(:from IS NULL OR a.date >= :from) AND " +
               "(:to IS NULL OR a.date <= :to) " +
               "ORDER BY a.date DESC")
        List<AuditLog> findFiltered(
            @Param("action") String action,
            @Param("userId") Long userId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
        );
    }
