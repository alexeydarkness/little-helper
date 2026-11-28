package ru.littlehelper.backend.user;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	boolean existsByEmailAndIdNot(String email, Long id);

	boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);

	boolean existsByUsernameIgnoreCase(String username);

	List<User> findByRoleOrderByFullNameAsc(Role role);

	@Query("""
			select new ru.littlehelper.backend.user.UserSearchResult(u.id, u.fullName, u.username, u.role)
			from User u
			where u.role = :role and (locate(lower(:term), lower(u.fullName)) > 0
				or locate(lower(:term), lower(u.username)) > 0)
			order by u.fullName, u.id
			""")
	List<UserSearchResult> searchByRoleAndTerm(Role role, String term, Pageable page);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select u from User u where u.id = :id")
	Optional<User> findByIdForUpdate(Long id);
}
