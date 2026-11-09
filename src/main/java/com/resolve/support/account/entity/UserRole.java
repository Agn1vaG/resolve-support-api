package com.resolve.support.account.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Account roles. USER creates and tracks own tickets; AGENT works assigned tickets; ADMIN manages tickets and accounts.")
public enum UserRole {
    USER,
    AGENT,
    ADMIN
}
