package jp.co.sss.pr.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 実績マスターEntity
 */
@Entity
@Table(name = "achievement")
public class Achievement {

    @Id
    private Integer achievementId;

    @Column(nullable = false, length = 50)
    private String achievementName;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(nullable = false, length = 10)
    private String icon;

    // --- getters / setters ---

    public Integer getAchievementId() {
        return achievementId;
    }

    public void setAchievementId(Integer achievementId) {
        this.achievementId = achievementId;
    }

    public String getAchievementName() {
        return achievementName;
    }

    public void setAchievementName(String achievementName) {
        this.achievementName = achievementName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }
}

