package email;

import io.quarkus.mailer.MailTemplate.MailTemplateInstance;
import io.quarkus.qute.CheckedTemplate;
import model.User;

public class Emails {
	
	private static final String FROM = "Todos <todos@example.com>";
	private static final String SUBJECT_PREFIX = "[Todos] ";

	@CheckedTemplate
	static class Templates {
	    public static native MailTemplateInstance confirm(User user);
	}
	
	public static void confirm(User user) {
	    Templates.confirm(user)
		.subject(SUBJECT_PREFIX + "Please confirm your email address")
		.to(user.email)
		.from(FROM)
		.send().await().indefinitely();
	}
}
