package com.katesoft.gserver.domain;

import com.google.common.base.Objects;

public abstract class UserAccountBase {
    /**
     * for social login:
     * 
     * 1. internal (straight user registration)
     * 2. facebook
     * 3. twitter
     */
    private String provider, providerUserId;

    private String username, firstname, lastname;
    private String password;
    private String email;
    private boolean enabled = true;

    public UserAccountBase() {
        setProvider( ProviderType.INTERNAL.name() );
    }
    protected UserAccountBase(UserAccountBase accountBase) {
        this();

        setUsername( accountBase.getUsername() );
        setProvider( accountBase.getProvider() );
        setProviderUserId( accountBase.getProviderUserId() );
        setFirstname( accountBase.getFirstname() );
        setLastname( accountBase.getLastname() );
        setPassword( accountBase.getPassword() );
        setEmail( accountBase.getEmail() );
        setEnabled( accountBase.isEnabled() );
    }
    public String getProvider() {
        return provider;
    }
    public String getProviderUserId() {
        return providerUserId;
    }
    public String getFirstname() {
        return firstname;
    }
    public String getLastname() {
        return lastname;
    }
    public String getPassword() {
        return password;
    }
    public String getEmail() {
        return email;
    }
    public void setProvider(String provider) {
        this.provider = provider.toLowerCase();
    }
    public void setProviderUserId(String providerUserId) {
        this.providerUserId = providerUserId;
    }
    public void setFirstname(String firstName) {
        this.firstname = firstName;
    }
    public void setLastname(String lastName) {
        this.lastname = lastName;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public boolean isEnabled() {
        return enabled;
    }
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    public String getUsername() {
        return username;
    }
    public void setUsername(String userName) {
        this.username = userName;
    }
    public ProviderType getProviderType() {
        return ProviderType.valueOf( getProvider().toUpperCase() );
    }
    public String toFullName() {
        return getLastname() + " " + getFirstname();
    }
    @Override
    public int hashCode() {
        return Objects.hashCode( getUsername() );
    }
    @Override
    public boolean equals(Object obj) {
        return Objects.equal( getUsername(), ( (UserAccountBase) obj ).getUsername() );
    }
    @Override
    public String toString() {
        return Objects
                .toStringHelper( this )
                .add( "username", getUsername() )
                .add( "provider", getProvider() )
                .add( "providerUserId", getProviderUserId() )
                .add( "firstname", getFirstname() )
                .add( "lastname", getLastname() )
                .add( "email", getEmail() )
                .toString();
    }

    public static enum ProviderType {
        INTERNAL,
        FACEBOOK,
        TWITTER,
        VK,
        LINKED_IN,
        GITHUB
    }
}
