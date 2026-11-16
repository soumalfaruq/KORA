# MVP functional scope

## Implemented

- Public visitors can browse, search, paginate, and filter questions by open/resolved status and unanswered state.
- Members can register, sign in/out, create questions with tags, answer open questions, and accept a solution only on their own question.
- Question and answer content is returned as plain text and rendered as text; HTML/Markdown is not interpreted.
- Account passwords are stored as PBKDF2 hashes. Authenticated mutations use server-side sessions and CSRF tokens.

Question editing has been added in code, but still needs live HTTP, PostgreSQL, and browser verification before it is considered an operational feature.

## Roles and boundaries

Registration creates a `MEMBER`. `MODERATOR` and `ADMIN` are reserved in the data model but not assignable through public registration. There is no administrative bootstrap account.

## Not included in this first vertical slice

Drafts, Markdown/code highlighting, comments, reactions, bookmarks, tag/category filtering, question deletion, answer editing/deletion, profile preferences, communities, projects, notifications, reports/moderation UI, and administration are still outstanding. They must not be presented as working product features until implemented and verified.
