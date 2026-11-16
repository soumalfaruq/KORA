# Hackathon demo

There are no seeded or shared demo accounts. Create accounts during the presentation so the displayed discussions are real records in the local PostgreSQL database.

## Authentication setup

- Prepare two distinct usernames and email addresses. Usernames accept 3–32 letters, digits, or underscores; passwords must be 12–128 characters.
- Registration signs the new member in immediately. Use **Déconnexion** before registering the second member.
- Login accepts the username regardless of letter case. Reloading the page restores the current session; logging out invalidates it.
- Sessions are held in API memory, so an API restart requires members to sign in again. Password reset and email verification are not configured.

1. Register as the first member and publish a tagged question.
2. Search for the question and show its open/unanswered filters.
3. Sign out, register a second member, and post a helpful answer.
4. Sign in as the question author, accept the answer, and show the resolved state.
5. Point out the author-only solution permission and responsive forum layout.

This demo covers only the account and forum vertical slice. Communities, projects, notifications, reports, and admin tools are not yet available.
