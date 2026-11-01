package jp.co.sss.pr.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import jp.co.sss.pr.entity.FriendChallenge;
import jp.co.sss.pr.entity.FriendChallengeParticipant;

@Repository
public interface FriendChallengeParticipantRepository extends JpaRepository<FriendChallengeParticipant, Integer> {

    @Query("""
            SELECT p FROM FriendChallengeParticipant p
            WHERE p.challenge.challengeId = :challengeId AND p.user.userId = :userId
            """)
    Optional<FriendChallengeParticipant> findByChallengeIdAndUserId(
            @Param("challengeId") Integer challengeId, @Param("userId") Integer userId);

    /** 同じ参加者の二重提出を防ぐため、提出処理中は参加者行をロックする。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p FROM FriendChallengeParticipant p
            WHERE p.challenge.challengeId = :challengeId AND p.user.userId = :userId
            """)
    Optional<FriendChallengeParticipant> lockByChallengeIdAndUserId(
            @Param("challengeId") Integer challengeId, @Param("userId") Integer userId);

    @Query("""
            SELECT p FROM FriendChallengeParticipant p JOIN FETCH p.user
            WHERE p.challenge = :challenge
            ORDER BY p.participantId
            """)
    List<FriendChallengeParticipant> findAllByChallenge(@Param("challenge") FriendChallenge challenge);

    @Query("""
            SELECT p FROM FriendChallengeParticipant p JOIN FETCH p.challenge c
            WHERE p.user.userId = :userId
            ORDER BY c.createdAt DESC, c.challengeId DESC
            """)
    List<FriendChallengeParticipant> findAllForUser(@Param("userId") Integer userId);
}