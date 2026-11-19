package ru.littlehelper.backend.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Locale;

@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 30)
	private String username;

	@Column(name = "username_key", nullable = false, unique = true, length = 30)
	private String usernameKey;

	@Column(nullable = false, unique = true, length = 100)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;

	@Column(name = "full_name", nullable = false, length = 100)
	private String fullName;

	@Column(length = 30)
	private String phone;

	@Column(name = "avatar_base64", columnDefinition = "text")
	private String avatarBase64;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 20)
	private Role role;

	@Column(nullable = false)
	private int balance = 0;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt = Instant.now();

	protected User() {
	}

	public User(String username, String email, String passwordHash, String fullName, String phone, Role role) {
		this.username = username;
		this.usernameKey = username.toLowerCase(Locale.ROOT);
		this.email = email;
		this.passwordHash = passwordHash;
		this.fullName = fullName;
		this.phone = phone;
		this.role = role;
	}

	public Long getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getFullName() {
		return fullName;
	}

	public String getPhone() {
		return phone;
	}

	public Role getRole() {
		return role;
	}

	public String getAvatarBase64() {
		return avatarBase64;
	}

	public void updateProfile(String username, String email, String fullName, String phone, String avatar) {
		this.username = username;
		this.usernameKey = username.toLowerCase(Locale.ROOT);
		this.email = email;
		this.fullName = fullName;
		this.phone = phone == null ? null : phone.trim();
		if (avatar != null) this.avatarBase64 = avatar.isEmpty() ? null : avatar;
	}

	public int getBalance() {
		return balance;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
