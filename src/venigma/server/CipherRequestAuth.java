/*
 * Copyright Evan Summers
 * 
 */
package venigma.server;

import vellum.logger.Logr;
import vellum.logger.LogrFactory;
import venigma.common.AdminRole;
import venigma.common.AdminUser;
import venigma.provider.ClientType;
import venigma.server.data.AdminUserConnection;

/**
 *
 * @author evan
 */
public class CipherRequestAuth {
    static Logr logger = LogrFactory.getLogger(CipherRequestAuth.class);
    CipherContext context;    
    
    public CipherRequestAuth(CipherContext context) {
        this.context = context;
    }
    
    public CipherResponseType authUser(ClientType clientType) {
        if (clientType == ClientType.PROVIDER) {
            return CipherResponseType.ERROR_NOT_USER;
        } else if (clientType == ClientType.ADMIN) {
            return CipherResponseType.OK;
        } else if (clientType == ClientType.USER) {
            return CipherResponseType.OK;
        }
        return CipherResponseType.ERROR_NOT_USER;
    }

    public CipherResponseType authProvider(ClientType clientType) {
        if (clientType == ClientType.PROVIDER) {
            return CipherResponseType.OK;
        }
        return CipherResponseType.ERROR_NOT_PROVIDER;
    }

    public CipherResponseType authAdmin(ClientType clientType) {
        if (clientType == ClientType.ADMIN) {
            return CipherResponseType.OK;
        }
        return CipherResponseType.ERROR_NOT_ADMIN;
    }
    
    public CipherResponseType auth(CipherRequest request, String subject) throws Exception {
        if (subject == null) {
            return CipherResponseType.ERROR_CERT;
        }
        if (request.requestType == CipherRequestType.REGISTER_USER) {
            return CipherResponseType.OK;
        }
        logger.info("auth", subject);
        ClientType clientType = null;
        if (subject.startsWith("CN=provider,")) {
            clientType = ClientType.PROVIDER;
        } else {
            AdminUserConnection conn = context.getStorage().getAdminUserConnection();
            for (AdminUser adminUser : conn.getList()) {
                logger.info(adminUser);
                if (subject.startsWith("CN=" + adminUser.getUsername() + ",")) {
                    if (adminUser.getRole() == AdminRole.SUPERVISOR) {
                        clientType = ClientType.ADMIN;
                    } else {
                        clientType = ClientType.USER;                        
                    }
                }
            }
        }
        if (request.requestType == CipherRequestType.START) {
            return authUser(clientType);
        } else if (request.requestType == CipherRequestType.STOP) {
            return authUser(clientType);
        } else if (request.requestType == CipherRequestType.CHECK) {
                return CipherResponseType.OK;
        } else if (request.requestType == CipherRequestType.GRANT) {
            return authAdmin(clientType);
        } else if (request.requestType == CipherRequestType.REVOKE) {
            return authAdmin(clientType);
        } else if (request.requestType == CipherRequestType.ADD_USER) {
            return authAdmin(clientType);
        } else if (request.requestType == CipherRequestType.ADD_KEY) {
            return authAdmin(clientType);
        } else if (request.requestType == CipherRequestType.GENERATE_KEY) {
            return authAdmin(clientType);
        } else if (request.requestType == CipherRequestType.REVISE_KEY) {
            return authAdmin(clientType);
        } else if (request.requestType == CipherRequestType.CONFIRM_KEY) {
            return authUser(clientType);
        } else if (request.requestType == CipherRequestType.CONFIRM_USER) {
            return authUser(clientType);
        } else if (request.requestType == CipherRequestType.ENCIPHER) {
            return authProvider(clientType);
        } else if (request.requestType == CipherRequestType.DECIPHER) {
            return authProvider(clientType);
        } else if (request.requestType == CipherRequestType.RECIPHER) {
            return authProvider(clientType);
        }
        return CipherResponseType.ERROR_AUTH;
    }
        
}
