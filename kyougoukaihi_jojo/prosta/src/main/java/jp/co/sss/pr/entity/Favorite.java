package jp.co.sss.pr.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "favorite")
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_favorite_gen")
    @SequenceGenerator(name = "seq_favorite_gen", sequenceName = "seq_favorite", allocationSize = 1)
    @Column(name = "favorite_id")
    private Integer favoriteId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "question_id")
    private Question question;

    // --- getters / setters ---

    public Integer getFavoriteId() {
        return favoriteId;
    }

    public void setFavoriteId(Integer favoriteId) {
        this.favoriteId = favoriteId;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }
}
