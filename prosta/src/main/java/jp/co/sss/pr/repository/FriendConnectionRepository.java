package jp.co.sss.pr.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.entity.FriendConnection;

@Repository
public interface FriendConnectionRepository extends JpaRepository<FriendConnection, Integer> {

    @Query("""
            SELECT f
            FROM FriendConnection f
            WHERE (f.userLow = :user OR f.userHigh = :user)
              AND f.status = :status
            """)
    List<FriendConnection> findAllForUserAndStatus(
            @Param("user") Customer user, @Param("status") String status);

    Optional<FriendConnection> findByUserLowAndUserHigh(Customer userLow, Customer userHigh);
}
