package io.github.learnjakartaee.security;

import java.util.Map;
import java.util.Set;

import jakarta.security.enterprise.credential.Credential;
import jakarta.security.enterprise.credential.UsernamePasswordCredential;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;

/**
 * Simple credential validator used for testing only. A set of hard-coded users
 * and roles are provided as default, but can be overridden.
 *
 * This validator is enabled only when TEST_USERS_ALLOWED env variable or
 * test.users.allowed system property is set to true.
 */
public class TestCredentialValidator implements CredentialValidator {

	public static final String TEST_USERS_ENABLED_ENV_VAR = "TEST_USERS_ENABLED";

	public static final String TEST_USERS_ENABLED_SYS_PROP = "test.users.enabled";

	private static Map<String, String> USERS = Map.of(

			"admin", "password",

			"alice", "password",

			"bob", "password"

	);

	private static Map<String, Set<String>> ROLES = Map.of(

			"admin", Set.of("admin", "user"),

			"alice", Set.of("user"),

			"bob", Set.of("user")

	);

	private final Map<String, String> users;
	private final Map<String, Set<String>> roles;
	private final boolean isEnabled;

	public TestCredentialValidator(Map<String, String> users, Map<String, Set<String>> roles) {
		this.users = users;
		this.roles = roles;
		this.isEnabled = checkIfEnabled();
	}

	public TestCredentialValidator() {
		this(USERS, ROLES);
	}

	protected boolean checkIfEnabled() {
		String testUsersEnabled = System.getenv(TEST_USERS_ENABLED_ENV_VAR);
		if (testUsersEnabled == null) {
			testUsersEnabled = System.getProperty(TEST_USERS_ENABLED_SYS_PROP, "false");
		}
		return Boolean.valueOf(testUsersEnabled);
	}

	@Override
	public boolean appliesTo(Credential credential) {
		return isEnabled && credential instanceof UsernamePasswordCredential;
	}

	@Override
	public CredentialValidationResult validate(Credential credential) {
		UsernamePasswordCredential login = (UsernamePasswordCredential) credential;

		String password = users.get(login.getCaller());
		if (password != null && password.equals(login.getPasswordAsString())) {
			return new CredentialValidationResult(login.getCaller(), roles.get(login.getCaller()));
		} else {
			return CredentialValidationResult.NOT_VALIDATED_RESULT;
		}
	}

}
