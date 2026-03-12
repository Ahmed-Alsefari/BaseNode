package com.BaseNode.BaseNode;


//			###############################
//			##      powered by AI        ##
//			###############################


import com.BaseNode.BaseNode.controller.WebControllerImpl;
import com.BaseNode.BaseNode.model.UserEntity;
import com.BaseNode.BaseNode.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.ui.Model;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class BaseNodeApplicationTests {

	private final UserRepository userRepository = mock(UserRepository.class);
	private final WebControllerImpl controller = new WebControllerImpl(userRepository);
	private final Model model = mock(Model.class);
	private final HttpSession session = mock(HttpSession.class);

	@Test
	void contextLoads() {
	}

	@Test
	void loginSuccess_redirectsToHome() {
		UserEntity admin = new UserEntity("admin", "123456", "ADMIN");
		when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

		String result = controller.processLogin("admin", "123456", model, session);

		assertEquals("redirect:/", result);
	}

	@Test
	void loginFail_wrongPassword_staysOnLoginPage() {
		UserEntity admin = new UserEntity("admin", "123456", "ADMIN");
		when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

		String result = controller.processLogin("admin", "wrongpass", model, session);

		assertEquals("login", result);
	}

	@Test
	void fileManager_noSession_redirectsToLogin() {
		when(session.getAttribute("loggedInUser")).thenReturn(null);

		String result = controller.showFileManager(model, session);

		assertEquals("redirect:/login", result);
	}
}