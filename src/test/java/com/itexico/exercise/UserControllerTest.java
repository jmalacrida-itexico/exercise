package com.itexico.exercise;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Arrays;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {UserApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration
@DirtiesContext
public class UserControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mvc;

    @Before
    public void setUp() {
        mvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @After
    public void cleanUp() {
        userRepository.deleteAll();
    }

    @Autowired
    private UserRepository userRepository;

    @Test
    public void getUsersWhenDBIsEmpty() throws Exception {

        //Given the DB is empty

        //Expect an empty users list
        mvc.perform(
                get("/users")
                        .with(user("user").password("password").roles("USER"))
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    public void getUsersWhenDBIsNotEmpty() throws Exception {

        //Given there are three users
        userRepository.saveAll(Arrays.asList(new User("Doe", "Joe"), new User("Second", "Joe"), new User("First", "Joe")));

        //Expect they are returned sorted by last name
        mvc.perform(
                get("/users")
                        .with(user("user").password("password"))
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].lastName").value("Doe"))
                .andExpect(jsonPath("$[0].firstName").value("Joe"))
                .andExpect(jsonPath("$[1].lastName").value("First"))
                .andExpect(jsonPath("$[1].firstName").value("Joe"))
                .andExpect(jsonPath("$[2].lastName").value("Second"))
                .andExpect(jsonPath("$[2].firstName").value("Joe"));

    }

    @Test
    public void getUserByIdWhenUserExists() throws Exception {
        //Given there is one user in the DB
        User user = userRepository.save(new User("User", "One"));

        //Expect it is retrieved from DB
        mvc.perform(
                get("/users/" + user.getId())
                        .with(user("user").password("password"))
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("User"))
                .andExpect(jsonPath("$.firstName").value("One"));
    }

    @Test
    public void getUserByIdWhenUserDoesNotExist() throws Exception {
        //Given there is not any user in the DB

        //Expect an exception
        mvc.perform(
                get("/users/1")
                        .with(user("user").password("password"))
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorMessage").value("User with id = 1 not found"));
    }

    @Test
    public void addUserWhenRequestUserIsNotAllowed() throws Exception {
        //Given there is not any user in the DB

        //Expect request user is not authorized to add users
        mvc.perform(
                post("/users")
                        .with(user("user").password("password"))
                        .content("{\n" +
                                "    \"lastName\": \"last\",\n" +
                                "    \"firstName\": \"name\"\n" +
                                "}")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void addUserWhenRequestUserIsAllowed() throws Exception {
        //Given there is not any user in the DB

        //Expect the user is added
        mvc.perform(
                post("/users")
                        .with(user("a").password("").roles("ADMIN"))
                        .content("{\n" +
                                "    \"lastName\": \"last\",\n" +
                                "    \"firstName\": \"name\"\n" +
                                "}")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", greaterThan(0)))
                .andExpect(jsonPath("$.lastName").value("last"))
                .andExpect(jsonPath("$.firstName").value("name"));

        assertEquals(1, userRepository.count());
    }

    @Test
    public void addTwoRepeatedUsers() throws Exception {
        //Given there is not any user in the DB

        //Expect only one user out of two to be added
        mvc.perform(
                post("/users")
                        .with(user("a").password("").roles("ADMIN"))
                        .content("{\n" +
                                "    \"lastName\": \"last\",\n" +
                                "    \"firstName\": \"name\"\n" +
                                "}")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", greaterThan(0)))
                .andExpect(jsonPath("$.lastName").value("last"))
                .andExpect(jsonPath("$.firstName").value("name"));

        mvc.perform(
                post("/users")
                        .with(user("a").password("").roles("ADMIN"))
                        .content("{\n" +
                                "    \"lastName\": \"last\",\n" +
                                "    \"firstName\": \"name\"\n" +
                                "}")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorMessage").value("User last name exists."));

        assertEquals(1, userRepository.count());
    }

    @Test
    public void deleteUserWhenUserDoesNotExist() throws Exception {
        //Given there is not any user in the DB

        //Expect an exception
        mvc.perform(
                delete("/users/1")
                        .with(user("admin").password("password").roles("ADMIN"))
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorMessage").value("User with id = 1 not found"));
    }

    @Test
    public void deleteUserWhenUserExists() throws Exception {
        //Given there is one user in the DB
        User user = userRepository.save(new User("User", "One"));

        //Expect the user to be deleted
        mvc.perform(
                delete("/users/" + user.getId())
                        .with(user("admin").password("password").roles("ADMIN"))
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());

        assertFalse(userRepository.existsById(user.getId()));
    }


}