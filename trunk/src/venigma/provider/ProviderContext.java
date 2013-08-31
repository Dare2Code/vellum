/*
 * Vellum by Evan Summers under Apache Software License 2.0 from ASF.
 * 
 */
package venigma.provider;

/**
 *
 * @author evan.summers
 */
public class ProviderContext extends ClientContext {

    String keyAlias = "default";

    public ProviderContext() {
    }
    
    public String getKeyAlias() {
        return keyAlias;
    }

    public void setKeyAlias(String keyAlias) {
        this.keyAlias = keyAlias;
    }
        
}