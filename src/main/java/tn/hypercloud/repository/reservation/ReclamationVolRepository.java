package tn.hypercloud.repository.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.hypercloud.entity.reservation.ReclamationVol;

import java.util.List;

public interface ReclamationVolRepository extends JpaRepository<ReclamationVol, Integer> {

    List<ReclamationVol> findByTouristeIdOrderByDateCreationDesc(Long touristeId);

    @Query("""
            SELECT r
            FROM ReclamationVol r
            JOIN r.reservation res
            JOIN res.volAller va
            LEFT JOIN res.volRetour vr
            WHERE va.user.id = :societeId
               OR (vr IS NOT NULL AND vr.user.id = :societeId)
            ORDER BY r.dateCreation DESC
            """)
    List<ReclamationVol> findForSociete(@Param("societeId") Long societeId);

    @Query("""
            SELECT r
            FROM ReclamationVol r
            JOIN FETCH r.reservation res
            JOIN FETCH res.volAller va
            JOIN FETCH va.user u
            LEFT JOIN FETCH res.volRetour vr
            LEFT JOIN FETCH vr.user u2
            WHERE r.id = :id
            """)
    java.util.Optional<ReclamationVol> findByIdWithReservationVolUsers(@Param("id") Integer id);

    @Query("""
            SELECT COUNT(r)
            FROM ReclamationVol r
            WHERE r.touriste.id = :touristeId
              AND r.statut = tn.hypercloud.entity.reservation.ReclamationVol$Statut.repondue
              AND r.clientLu = false
            """)
    long countUnreadRepliesForTouriste(@Param("touristeId") Long touristeId);
}

