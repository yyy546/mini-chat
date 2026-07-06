package com.minichat.common.security.jwt;

import lombok.Data;

import java.security.Principal;

@Data
public class UserPrincipal implements Principal {

    private final Long userId;

    public UserPrincipal(Long userId){
        this.userId = userId;
    }

    @Override
    public String getName() {
        return userId.toString();
    }

    public Long getUserId(){
        return userId;
    }
}
