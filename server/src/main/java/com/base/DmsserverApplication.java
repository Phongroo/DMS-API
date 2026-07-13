package com.base;

import com.base.model.Role;
import com.base.model.User;
import com.base.model.UserRole;
import com.base.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.HashSet;
import java.util.Set;

@SpringBootApplication
public class DmsserverApplication implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(DmsserverApplication.class);

	@Autowired
	private UserService userService;
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;

	public static void main(String[] args) {
		SpringApplication.run(DmsserverApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		log.info("Starting DmsServer application...");
		User existingUser = this.userService.getUser("hongphong");
		if (existingUser == null) {
			User user = new User();
			user.setFirstName("Hong");
			user.setLastName("Phong");
			user.setUsername("hongphong");
			user.setPassword(this.bCryptPasswordEncoder.encode("123456"));
			user.setEmail("hongphong.12012001@gmail.com");
			user.setProfile("avatar.png");

			Role role = new Role();
			role.setRoleId(44L);
			role.setRoleName("ADMIN");

			Set<UserRole> userRoleSet = new HashSet<>();

			UserRole userRole = new UserRole();
			userRole.setRole(role);
			userRole.setUser(user);

			userRoleSet.add(userRole);

			this.userService.createUser(user, userRoleSet);
			log.info("Default admin user 'yenna' created successfully.");
		} else {
			log.info("Default admin user 'yenna' already exists.");
		}
	}
}
