package jp.co.sss.pr.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.co.sss.pr.entity.FriendChallenge;
import jp.co.sss.pr.entity.FriendChallengeQuestion;

@Repository
public interface FriendChallengeQuestionRepository extends JpaRepository<FriendChallengeQuestion, Integer> {

    List<FriendChallengeQuestion> findByChallengeOrderByPositionAsc(FriendChallenge challenge);
}