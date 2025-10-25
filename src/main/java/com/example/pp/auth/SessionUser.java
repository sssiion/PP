package com.example.pp.auth;

import java.io.Serializable;

public class SessionUser implements Serializable {
    private String name;
    private String email;
    private String providerId;

    public SessionUser(User user) {
        this.name = user.getName();
        this.email = user.getEmail();
        this.providerId = user.getProviderId();
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getProviderId() {
        return providerId;
    }
}
