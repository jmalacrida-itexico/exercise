package com.itexico.exercise;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    private ModelMapper modelMapper = new ModelMapper();

    @GetMapping("")
    public List<UserDto> getUsers() {
        return userRepository.findAll(Sort.by(Sort.Order.by("lastName")).ascending())
                .stream().map(user -> modelMapper.map(user, UserDto.class))
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public UserDto getUserById(@PathVariable Integer id) {
        Optional<User> optionalUser = userRepository.findById(id);
        if (optionalUser.isPresent())
            return modelMapper.map(optionalUser.get(), UserDto.class);
        throw new EntityNotFoundException("User with id = " + id + " not found");
    }

    @PostMapping("")
    public UserDto addUser(@RequestBody UserDto userDto) {
        User exampleOfUser = modelMapper.map(userDto, User.class);
        if (userRepository.exists(Example.of(exampleOfUser)))
            throw new EntityExistsException("User " + userDto.getLastName() + " " + userDto.getFirstName() + " exists.");
        User user = userRepository.save(exampleOfUser);
        return modelMapper.map(user, UserDto.class);
    }

    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Integer id) {
        if (userRepository.existsById(id))
            userRepository.deleteById(id);
        else
            throw new EntityNotFoundException("User with id = " + id + " not found");
    }

    @ExceptionHandler({EntityNotFoundException.class})
    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    @ResponseBody
    public ErrorResponse handleControllerException(HttpServletRequest req, EntityNotFoundException e) {
        return new ErrorResponse(e.getMessage());
    }

    @ExceptionHandler({Exception.class})
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public ErrorResponse handleException(HttpServletRequest req, Exception e) {
        return new ErrorResponse(e.getMessage());
    }
}
