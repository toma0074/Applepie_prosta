package jp.co.sss.pr.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.co.sss.pr.entity.FriendChallenge;
import jp.co.sss.pr.entity.FriendChallengeAnswer;

@Repository
public interface FriendChallengeAnswerRepository extends JpaRepository<FriendChallengeAnswer, Integer> {

    @Query("""
            SELECT a FROM FriendChallengeAnswer a
            JOIN FETCH a.participant p JOIN FETCH a.challengeQuestion q
            WHERE p.challenge = :challenge
            """)
    List<FriendChallengeAnswer> findAllByChallenge(@Param("challenge") FriendChallenge challenge);
}