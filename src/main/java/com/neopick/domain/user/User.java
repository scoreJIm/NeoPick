package com.neopick.domain.user;

import com.neopick.domain.common.AggregateRoot;

import java.time.LocalDateTime;

public class User implements AggregateRoot {

    private UserId id;
    private PhoneNumber phone;
    private String nickname;
    private String avatarUrl;
    private Gender gender;
    private UserRole role;
    private UserStatus status;
    private LocalDateTime registeredAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;

    private User() {
    }

    public User(UserId id, PhoneNumber phone, String nickname, UserRole role) {
        this.id = id;
        this.phone = phone;
        this.nickname = nickname;
        this.role = role;
        this.status = UserStatus.ACTIVE;
        this.registeredAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void updateProfile(String nickname, Gender gender, String avatarUrl) {
        this.nickname = nickname != null ? nickname : this.nickname;
        this.gender = gender != null ? gender : this.gender;
        this.avatarUrl = avatarUrl != null ? avatarUrl : this.avatarUrl;
        this.updatedAt = LocalDateTime.now();
    }

    public void recordLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    public UserId getId() {
        return id;
    }

    public PhoneNumber getPhone() {
        return phone;
    }

    public String getNickname() {
        return nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public Gender getGender() {
        return gender;
    }

    public UserRole getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }
}
