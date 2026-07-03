package com.nanotech.flux_pro_backend.security;

public final class RbacPermissions {

    private RbacPermissions() {
    }

    public static final String USERS_READ = "USERS:READ";
    public static final String USERS_CREATE = "USERS:CREATE";
    public static final String USERS_UPDATE = "USERS:UPDATE";
    public static final String USERS_DELETE = "USERS:DELETE";
    public static final String USERS_IMPORT = "USERS:IMPORT";
    public static final String USERS_RESET_PASSWORD = "USERS:RESET_PASSWORD";
    public static final String USERS_UNLOCK = "USERS:UNLOCK";

    public static final String ORGANIZATIONS_READ = "ORGANIZATIONS:READ";
    public static final String ORGANIZATIONS_CREATE = "ORGANIZATIONS:CREATE";
    public static final String ORGANIZATIONS_UPDATE = "ORGANIZATIONS:UPDATE";
    public static final String ORGANIZATIONS_DELETE = "ORGANIZATIONS:DELETE";
    public static final String ORGANIZATIONS_IMPORT = "ORGANIZATIONS:IMPORT";

    public static final String ORGANIZATION_TYPES_READ = "ORGANIZATION_TYPES:READ";
    public static final String ORGANIZATION_TYPES_CREATE = "ORGANIZATION_TYPES:CREATE";
    public static final String ORGANIZATION_TYPES_UPDATE = "ORGANIZATION_TYPES:UPDATE";
    public static final String ORGANIZATION_TYPES_DELETE = "ORGANIZATION_TYPES:DELETE";

    public static final String ROLES_READ = "ROLES:READ";
    public static final String ROLES_CREATE = "ROLES:CREATE";
    public static final String ROLES_UPDATE = "ROLES:UPDATE";
    public static final String ROLES_DELETE = "ROLES:DELETE";

    public static final String PERMISSIONS_READ = "PERMISSIONS:READ";
    public static final String PERMISSIONS_CREATE = "PERMISSIONS:CREATE";
    public static final String PERMISSIONS_UPDATE = "PERMISSIONS:UPDATE";
    public static final String PERMISSIONS_DELETE = "PERMISSIONS:DELETE";

    public static final String LOGIN_AUDIT_READ = "LOGIN_AUDIT:READ";

    public static final String CHAIN_TEMPLATES_READ = "CHAIN_TEMPLATES:READ";
    public static final String CHAIN_TEMPLATES_CREATE = "CHAIN_TEMPLATES:CREATE";
    public static final String CHAIN_TEMPLATES_UPDATE = "CHAIN_TEMPLATES:UPDATE";
    public static final String CHAIN_TEMPLATES_DELETE = "CHAIN_TEMPLATES:DELETE";

    public static final String FILE_TYPES_READ = "FILE_TYPES:READ";
    public static final String FILE_TYPES_CREATE = "FILE_TYPES:CREATE";
    public static final String FILE_TYPES_UPDATE = "FILE_TYPES:UPDATE";
    public static final String FILE_TYPES_DELETE = "FILE_TYPES:DELETE";

    public static final String FILES_READ = "FILES:READ";
    public static final String FILES_CREATE = "FILES:CREATE";
    public static final String FILES_UPDATE = "FILES:UPDATE";
    public static final String FILES_CLOSE = "FILES:CLOSE";
    public static final String FILES_ARCHIVE = "FILES:ARCHIVE";
    public static final String FILES_DELETE = "FILES:DELETE";
    public static final String FILES_TRANSMIT = "FILES:TRANSMIT";
}
