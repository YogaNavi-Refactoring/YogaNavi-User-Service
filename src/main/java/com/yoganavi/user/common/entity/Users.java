package com.yoganavi.user.common.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "Users")
public class Users {

    public enum Role {
        TEACHER,
        STUDENT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @Column(name = "user_id", unique = true)
    private Long userId;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false)
    private String pwd;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true, nullable = false, length = 30)
    private String nickname;

    @Column(length = 512)
    private String profileImageUrl;

    @Column(length = 512)
    private String profileImageUrlSmall;

    @Column(nullable = false)
    private String role;

    @Column
    private String authToken;

    @Column
    private Instant authTokenExpirationTime;

    @Column(length = 100)
    private String content;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Article> articles = new ArrayList<>();

    @OneToMany(mappedBy = "teacher", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TeacherLike> teacherLikes = new ArrayList<>();

    @Column
    private Instant deletedAt;

    @Column(nullable = false)
    private Boolean isDeleted = false;

    @Column(length = 512)
    private String fcmToken;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_hashtags",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "hashtag_id")
    )
    private Set<Hashtag> hashtags = new HashSet<>();

    public void addArticle(Article article) {
        articles.add(article);
        article.setUser(this);
    }

    public void removeAllArticles() {
        for (Article article : new ArrayList<>(articles)) {
            removeArticle(article);
        }
    }

    public void removeArticle(Article article) {
        articles.remove(article);
        article.setUser(null);
    }

    public String getRole() {
        return String.valueOf(role);
    }

    public Set<Hashtag> getHashtags() {
        return hashtags;
    }

    public void setHashtags(Set<Hashtag> hashtags) {
        this.hashtags = hashtags;
    }

    public void addHashtag(Hashtag hashtag) {
        if (this.hashtags == null) {
            this.hashtags = new HashSet<>();
        }
        this.hashtags.add(hashtag);
        if (hashtag.getUsers() == null) {
            hashtag.setUsers(new HashSet<>());
        }
        hashtag.getUsers().add(this);
    }

    public void removeHashtag(Hashtag hashtag) {
        this.hashtags.remove(hashtag);
        hashtag.getUsers().remove(this);
    }


}
