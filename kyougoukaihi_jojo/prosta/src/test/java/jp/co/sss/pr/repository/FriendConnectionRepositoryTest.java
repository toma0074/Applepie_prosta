package jp.co.sss.pr.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.entity.FriendConnection;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class FriendConnectionRepositoryTest {

    @Autowired
    private FriendConnectionRepository repository;

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void countsOnlyPendingRequestsSentByOthers() {
        Customer a = customerRepository.save(customer("a"));
        Customer b = customerRepository.save(customer("b"));
        Customer c = customerRepository.save(customer("c"));
        Customer d = customerRepository.save(customer("d"));

        save(a, b, b, FriendConnection.PENDING);
        save(a, c, a, FriendConnection.PENDING);
        save(a, d, d, FriendConnection.ACCEPTED);

        assertEquals(1, repository.countIncomingPending(a));
        assertEquals(0, repository.countIncomingPending(b));
        assertEquals(1, repository.countIncomingPending(c));
        assertEquals(0, repository.countIncomingPending(d));
    }

    private void save(Customer x, Customer y, Customer requestedBy, String status) {
        Customer low = x.getUserId() < y.getUserId() ? x : y;
        Customer high = x.getUserId() < y.getUserId() ? y : x;
        FriendConnection connection = new FriendConnection();
        connection.setUserLow(low);
        connection.setUserHigh(high);
        connection.setRequestedBy(requestedBy);
        connection.setStatus(status);
        connection.setCreatedAt(LocalDateTime.now());
        connection.setUpdatedAt(LocalDateTime.now());
        repository.save(connection);
    }

    private Customer customer(String name) {
        Customer customer = new Customer();
        customer.setUserName(name);
        customer.setUserPass("password");
        customer.setPermission(0);
        customer.setDeleteFlag(false);
        customer.setUserLevel(1);
        return customer;
    }
}
