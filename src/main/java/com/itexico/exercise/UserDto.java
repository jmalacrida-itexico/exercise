package com.itexico.exercise;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

public class UserDto implements Serializable {
    private Integer id;

    @NotNull
    private String lastName;

    @NotNull
    private String firstName;

    public UserDto() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

}
