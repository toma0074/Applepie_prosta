package jp.co.sss.pr.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.co.sss.pr.entity.FriendChallenge;

@Repository
public interface FriendChallengeRepository extends JpaRepository<FriendChallenge, Integer> {
}