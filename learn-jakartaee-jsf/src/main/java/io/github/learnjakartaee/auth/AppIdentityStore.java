package io.github.learnjakartaee.auth;

import io.github.learnjakartaee.security.CredentialValidator;
import io.github.learnjakartaee.security.TestCredentialValidator;
import io.github.learnjakartaee.security.CredentialValidatorChain;
import io.github.learnjakartaee.security.ELConfiguredLDAPCredentialValidator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.security.enterprise.credential.Credential;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.IdentityStore;

@ApplicationScoped
public class AppIdentityStore implements IdentityStore {

	private CredentialValidator credentialValidator = new CredentialValidatorChain(
			new TestCredentialValidator(),
			new ELConfiguredLDAPCredentialValidator());

	@Override
	public CredentialValidationResult validate(Credential credential) {
		return credentialValidator.validate(credential);
	}
}
