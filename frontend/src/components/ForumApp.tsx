"use client";

import { FormEvent, useEffect, useRef, useState } from "react";
import Image from "next/image";
import Link from "next/link";

type Member = { username: string; displayName: string };
type SessionState = { authenticated: boolean; user: Member | null };
type Answer = {
  id: string;
  body: string;
  author: string;
  createdAt: string;
  accepted: boolean;
};
type Question = {
  id: string;
  title: string;
  body: string;
  status: "OPEN" | "RESOLVED" | "CLOSED";
  author: string;
  authorUsername?: string;
  tags: string[];
  answerCount?: number;
  createdAt: string;
  updatedAt: string;
  acceptedAnswerId?: string | null;
  answers?: Answer[];
};
type QuestionPage = { content: Question[]; totalElements: number; totalPages: number };
type ApiError = { message?: string };

class ApiRequestError extends Error {
  constructor(message: string, readonly status: number) {
    super(message);
  }
}

async function jsonRequest<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, { credentials: "same-origin", ...init });
  const result = (await response.json().catch(() => ({}))) as T & ApiError;
  if (!response.ok) {
    throw new ApiRequestError(result.message ?? "La demande n’a pas abouti. Réessayez.", response.status);
  }
  return result;
}

async function postJson<T>(url: string, body?: unknown, method = "POST"): Promise<T> {
  const csrf = await jsonRequest<{ token: string }>("/api/auth/csrf");
  return jsonRequest<T>(url, {
    method,
    headers: {
      "Content-Type": "application/json",
      "X-XSRF-TOKEN": csrf.token,
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
}

function dateLabel(value: string) {
  return new Intl.DateTimeFormat("fr-FR", { dateStyle: "medium" }).format(new Date(value));
}

function parseTags(value: FormDataEntryValue | null) {
  return String(value ?? "")
    .split(",")
    .map((tag) => tag.trim())
    .filter(Boolean);
}

export default function ForumApp() {
  const [member, setMember] = useState<Member | null>(null);
  const [questions, setQuestions] = useState<Question[]>([]);
  const [total, setTotal] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage] = useState(0);
  const [refreshVersion, setRefreshVersion] = useState(0);
  const [selected, setSelected] = useState<Question | null>(null);
  const [queryInput, setQueryInput] = useState("");
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState("");
  const [unanswered, setUnanswered] = useState(false);
  const [loading, setLoading] = useState(true);
  const [authOpen, setAuthOpen] = useState(false);
  const [authMode, setAuthMode] = useState<"login" | "register">("login");
  const [authSubmitting, setAuthSubmitting] = useState(false);
  const authStateVersion = useRef(0);
  const [composeOpen, setComposeOpen] = useState(false);
  const [editingQuestion, setEditingQuestion] = useState(false);
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    const params = new URLSearchParams({ page: String(page), size: "20" });
    if (query) params.set("q", query);
    if (status) params.set("status", status);
    if (unanswered) params.set("unanswered", "true");
    void jsonRequest<QuestionPage>(`/api/questions?${params}`)
      .then((result) => {
        if (!active) return;
        setQuestions(result.content);
        setTotal(result.totalElements);
        setTotalPages(result.totalPages);
      })
      .catch((reason: unknown) => {
        if (active) setError(reason instanceof Error ? reason.message : "Impossible de charger les discussions.");
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => { active = false; };
  }, [query, status, unanswered, page, refreshVersion]);

  useEffect(() => {
    let active = true;
    const version = authStateVersion.current;
    void jsonRequest<SessionState>("/api/auth/me")
      .then((session) => {
        if (active && authStateVersion.current === version) {
          setMember(session.authenticated ? session.user : null);
        }
      })
      .catch(() => {
        if (active && authStateVersion.current === version) {
          setError("Impossible de vérifier votre session. Réessayez dans quelques instants.");
        }
      });
    return () => { active = false; };
  }, []);

  async function openQuestion(id: string) {
    setError("");
    setEditingQuestion(false);
    try {
      setSelected(await jsonRequest<Question>(`/api/questions/${id}`));
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Impossible d’ouvrir cette discussion.");
    }
  }

  async function submitAuth(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (authSubmitting) return;
    const data = new FormData(event.currentTarget);
    const username = String(data.get("username") ?? "");
    const password = String(data.get("password") ?? "");
    setError("");
    setAuthSubmitting(true);
    try {
      if (authMode === "register") {
        await postJson("/api/auth/register", {
          username,
          email: data.get("email"),
          displayName: data.get("displayName"),
          password,
        });
      }
      const loggedIn = await postJson<Member>("/api/auth/login", { username, password });
      authStateVersion.current += 1;
      setMember(loggedIn);
      setAuthOpen(false);
      setError("");
      setNotice(`Bienvenue, ${loggedIn.displayName}.`);
    } catch (reason) {
      if (reason instanceof ApiRequestError && reason.status === 401) {
        setError("Pseudonyme ou mot de passe incorrect.");
      } else if (reason instanceof ApiRequestError && reason.status === 409) {
        setError("Ce pseudonyme ou cette adresse e-mail est déjà utilisé·e.");
      } else {
        setError(reason instanceof Error ? reason.message : "Connexion impossible.");
      }
    } finally {
      setAuthSubmitting(false);
    }
  }

  async function signOut() {
    try {
      await postJson<{ loggedOut: boolean }>("/api/auth/logout");
      authStateVersion.current += 1;
      setMember(null);
      setNotice("Vous êtes déconnecté·e.");
      setSelected(null);
      setEditingQuestion(false);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Déconnexion impossible.");
    }
  }

  async function submitQuestion(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    if (!member) {
      setAuthMode("login");
      setAuthOpen(true);
      setNotice("Connectez-vous pour publier une question.");
      return;
    }
    const data = new FormData(event.currentTarget);
    try {
      const question = await postJson<Question>("/api/questions", {
        title: data.get("title"),
        body: data.get("body"),
        tags: parseTags(data.get("tags")),
      });
      setComposeOpen(false);
      form.reset();
      setSelected(question);
      setNotice("Votre question a été publiée.");
      setLoading(true);
      setRefreshVersion((version) => version + 1);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "La question n’a pas pu être publiée.");
    }
  }

  async function submitQuestionUpdate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selected) return;
    const data = new FormData(event.currentTarget);
    setError("");
    try {
      const updated = await postJson<Question>(
        `/api/questions/${selected.id}`,
        {
          title: data.get("title"),
          body: data.get("body"),
          tags: parseTags(data.get("tags")),
        },
        "PUT",
      );
      setSelected(updated);
      setEditingQuestion(false);
      setNotice("Votre question a été mise à jour.");
      setLoading(true);
      setRefreshVersion((version) => version + 1);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "La question n’a pas pu être mise à jour.");
    }
  }

  async function submitAnswer(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    if (!selected) return;
    const data = new FormData(event.currentTarget);
    try {
      await postJson<Answer>(`/api/questions/${selected.id}/answers`, { body: data.get("body") });
      form.reset();
      await openQuestion(selected.id);
      setLoading(true);
      setRefreshVersion((version) => version + 1);
      setNotice("Votre réponse a été publiée.");
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "La réponse n’a pas pu être publiée.");
    }
  }

  async function acceptAnswer(answerId: string) {
    if (!selected) return;
    try {
      await postJson<Question>(
        `/api/questions/${selected.id}/answers/${answerId}/accept`,
        undefined,
        "PATCH",
      );
      await openQuestion(selected.id);
      setLoading(true);
      setRefreshVersion((version) => version + 1);
      setNotice("La solution a été acceptée.");
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "La solution n’a pas pu être acceptée.");
    }
  }

  function submitSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setQuery(queryInput.trim());
    setPage(0);
    setSelected(null);
  }

  return (
    <main className="app-shell">
      <header className="topbar">
        <Link className="wordmark" href="/" aria-label="KORA, accueil">
          <Image
            className="wordmark-image"
            src="/kora-logo-color.png"
            alt="KORA"
            width={542}
            height={226}
            priority
          />
        </Link>
        <nav className="top-nav" aria-label="Navigation principale">
          <a className="nav-active" href="#discussions">Discussions</a>
          <a href="#how-it-works">À propos</a>
        </nav>
        <div className="top-actions">
          {member ? (
            <>
              <span className="member-label">Bonjour, {member.displayName}</span>
              <button className="button button-quiet" onClick={signOut} type="button">Déconnexion</button>
            </>
          ) : (
            <button
              className="button button-quiet"
              onClick={() => { setAuthMode("login"); setAuthOpen((open) => !open); }}
              type="button"
            >
              Connexion
            </button>
          )}
          <button
            className="button button-primary"
            onClick={() => setComposeOpen((open) => !open)}
            type="button"
          >
            Poser une question
          </button>
        </div>
      </header>

      <section className="hero" id="how-it-works">
        <div className="hero-copy">
          <p className="eyebrow"><span className="eyebrow-dot" /> LE FORUM DES DÉVELOPPEURS</p>
          <h1>On avance mieux<br />quand on <em>partage.</em></h1>
          <p className="hero-description">
            Des questions concrètes, des réponses utiles et une communauté qui grandit ensemble.
          </p>
          <button className="button button-light" onClick={() => {
            document.getElementById("discussions")?.scrollIntoView({ behavior: "smooth" });
          }} type="button">
            Explorer les discussions <span aria-hidden="true">↓</span>
          </button>
        </div>
        <div className="hero-aside" aria-label="Principes de la communauté">
          <Image
            className="hero-logo"
            src="/kora-logo-white.png"
            alt=""
            width={303}
            height={127}
            aria-hidden="true"
          />
          <p>Pour les développeurs,<br /><strong>par les développeurs.</strong></p>
          <span className="hero-location">Une voix panafricaine, ouverte à tous.</span>
        </div>
        <div className="hero-accent" aria-hidden="true" />
      </section>

      <div className="content-wrap" id="discussions">
        <div className="section-heading">
          <div>
            <p className="eyebrow eyebrow-dark">LA COMMUNAUTÉ EN ACTION</p>
            <h2>Discussions récentes</h2>
          </div>
          <p className="discussion-count">
            <strong>{total}</strong> {total === 1 ? "résultat" : "résultats"}
          </p>
        </div>

        {notice && <p className="notice" role="status">{notice}</p>}
        {error && <p className="error-message" role="alert">{error}</p>}

        {authOpen && (
          <section className="panel auth-panel" aria-labelledby="auth-heading">
            <div className="panel-heading">
              <div>
                <p className="eyebrow eyebrow-dark">VOTRE ESPACE KORA</p>
                <h2 id="auth-heading">{authMode === "login" ? "Ravi·e de vous revoir" : "Rejoindre KORA"}</h2>
              </div>
              <button className="text-button" type="button" onClick={() => setAuthOpen(false)} disabled={authSubmitting}>Fermer</button>
            </div>
            <div className="mode-switch" aria-label="Type de formulaire">
              <button type="button" aria-pressed={authMode === "login"} onClick={() => setAuthMode("login")} disabled={authSubmitting}>Connexion</button>
              <button type="button" aria-pressed={authMode === "register"} onClick={() => setAuthMode("register")} disabled={authSubmitting}>Créer un compte</button>
            </div>
            <form className="form-grid" onSubmit={submitAuth}>
              {authMode === "register" && (
                <>
                  <label>Nom public<input autoComplete="name" name="displayName" required minLength={2} maxLength={80} /></label>
                  <label>Adresse e-mail<input autoComplete="email" name="email" type="email" required maxLength={254} /></label>
                </>
              )}
              <label>Pseudonyme<input autoComplete="username" name="username" required minLength={3} maxLength={32} pattern="[A-Za-z0-9_]+" /></label>
              <label>Mot de passe<input autoComplete={authMode === "login" ? "current-password" : "new-password"} name="password" type="password" required minLength={authMode === "login" ? 1 : 12} maxLength={128} /></label>
              <button className="button button-primary form-submit" type="submit" disabled={authSubmitting} aria-busy={authSubmitting}>
                {authSubmitting ? "Veuillez patienter…" : authMode === "login" ? "Se connecter" : "Créer mon compte"}
              </button>
            </form>
            {authMode === "register" && <p className="form-hint">Utilisez au moins 12 caractères pour votre mot de passe.</p>}
          </section>
        )}

        {composeOpen && (
          <section className="panel compose-panel" aria-labelledby="compose-heading">
            <div className="panel-heading">
              <div>
                <p className="eyebrow eyebrow-dark">PARTAGER UN DÉFI</p>
                <h2 id="compose-heading">Poser une question</h2>
              </div>
              <button className="text-button" type="button" onClick={() => setComposeOpen(false)}>Fermer</button>
            </div>
            <form className="form-grid" onSubmit={submitQuestion}>
              <label>Titre<input name="title" required maxLength={180} placeholder="Soyez précis sur le problème rencontré" /></label>
              <label>
                Description
                <textarea name="body" required maxLength={20000} rows={6} placeholder="Décrivez le contexte, ce que vous avez essayé et le résultat attendu." />
              </label>
              <label>Tags, séparés par des virgules<input name="tags" placeholder="java, spring-boot" /></label>
              <p className="form-hint">Le texte est affiché comme du texte brut. N’incluez pas d’informations sensibles.</p>
              <button className="button button-primary form-submit" type="submit">Publier la question</button>
            </form>
          </section>
        )}

        <div className="forum-layout">
          <section className="discussion-column" aria-label="Liste des discussions">
            <form className="search-panel" onSubmit={submitSearch}>
              <label className="search-label" htmlFor="question-search">Rechercher dans les discussions</label>
              <div className="search-controls">
                <input
                  id="question-search"
                  type="search"
                  value={queryInput}
                  onChange={(event) => setQueryInput(event.target.value)}
                  placeholder="Un langage, un problème, un mot-clé…"
                />
                <button className="button button-primary" type="submit">Rechercher</button>
              </div>
              <div className="filter-controls">
                <label>
                  Statut
                  <select value={status} onChange={(event) => { setLoading(true); setStatus(event.target.value); setPage(0); setSelected(null); }}>
                    <option value="">Tous</option>
                    <option value="OPEN">Ouvertes</option>
                    <option value="RESOLVED">Résolues</option>
                  </select>
                </label>
                <label className="checkbox-label">
                  <input type="checkbox" checked={unanswered} onChange={(event) => { setLoading(true); setUnanswered(event.target.checked); setPage(0); setSelected(null); }} />
                  Sans réponse
                </label>
              </div>
            </form>

            {loading ? (
              <div className="empty-state" role="status"><span className="loading-dot" /> Chargement des discussions…</div>
            ) : questions.length === 0 ? (
              <div className="empty-state">
                <span className="empty-icon" aria-hidden="true">?</span>
                <h3>Aucune discussion pour le moment</h3>
                <p>Essayez un autre filtre, ou lancez la conversation avec votre première question.</p>
                <button className="text-button" type="button" onClick={() => setComposeOpen(true)}>Poser une question <span aria-hidden="true">→</span></button>
              </div>
            ) : (
              <div className="question-list">
                {questions.map((question) => (
                  <button
                    className={`question-card${selected?.id === question.id ? " question-card-selected" : ""}`}
                    key={question.id}
                    onClick={() => void openQuestion(question.id)}
                    type="button"
                  >
                    <div className="card-topline">
                      <span className={`status-pill status-${question.status.toLowerCase()}`}>
                        {question.status === "RESOLVED" ? "Résolue" : question.status === "CLOSED" ? "Fermée" : "Ouverte"}
                      </span>
                      <span className="card-date">{dateLabel(question.createdAt)}</span>
                    </div>
                    <h3>{question.title}</h3>
                    <p className="question-excerpt">{question.body}</p>
                    <div className="card-bottomline">
                      <div className="tag-list">
                        {question.tags.map((tag) => <span className="tag" key={tag}>{tag}</span>)}
                      </div>
                      <span className="answer-count">{question.answerCount ?? 0} réponse{question.answerCount === 1 ? "" : "s"}</span>
                    </div>
                    <span className="question-author">Par {question.author}</span>
                  </button>
                ))}
              </div>
            )}
            {!loading && totalPages > 1 && (
              <nav className="pagination" aria-label="Pagination des discussions">
                <button
                  className="button button-quiet"
                  disabled={page === 0}
                  onClick={() => { setLoading(true); setPage((current) => Math.max(0, current - 1)); }}
                  type="button"
                >
                  Précédent
                </button>
                <span>Page {page + 1} sur {totalPages}</span>
                <button
                  className="button button-quiet"
                  disabled={page + 1 >= totalPages}
                  onClick={() => { setLoading(true); setPage((current) => Math.min(totalPages - 1, current + 1)); }}
                  type="button"
                >
                  Suivante
                </button>
              </nav>
            )}
          </section>

          <aside className="sidebar">
            {selected ? (
              <section className="panel question-detail" aria-live="polite">
                <button className="back-link" type="button" onClick={() => setSelected(null)}>← Toutes les discussions</button>
                <div className="detail-meta">
                  <span className={`status-pill status-${selected.status.toLowerCase()}`}>
                    {selected.status === "RESOLVED" ? "Résolue" : selected.status === "CLOSED" ? "Fermée" : "Ouverte"}
                  </span>
                  <span>{dateLabel(selected.createdAt)}</span>
                </div>
                {Date.parse(selected.updatedAt) > Date.parse(selected.createdAt) && (
                  <p className="detail-updated">Modifiée le {dateLabel(selected.updatedAt)}</p>
                )}
                {member?.username === selected.authorUsername && !editingQuestion && (
                  <button
                    className="text-button question-edit-toggle"
                    type="button"
                    onClick={() => setEditingQuestion(true)}
                  >
                    Modifier ma question
                  </button>
                )}
                {editingQuestion ? (
                  <form className="form-grid question-edit-form" onSubmit={submitQuestionUpdate}>
                    <label>
                      Titre
                      <input name="title" defaultValue={selected.title} required maxLength={180} />
                    </label>
                    <label>
                      Description
                      <textarea name="body" defaultValue={selected.body} required maxLength={20000} rows={6} />
                    </label>
                    <label>
                      Tags, séparés par des virgules
                      <input name="tags" defaultValue={selected.tags.join(", ")} />
                    </label>
                    <p className="form-hint">Le texte reste affiché en texte brut. Vous pouvez retirer tous les tags.</p>
                    <div className="form-actions">
                      <button className="button button-primary" type="submit">Enregistrer les modifications</button>
                      <button className="text-button" type="button" onClick={() => setEditingQuestion(false)}>Annuler</button>
                    </div>
                  </form>
                ) : (
                  <>
                    <h2>{selected.title}</h2>
                    <p className="detail-author">Par {selected.author}</p>
                    <div className="tag-list detail-tags">{selected.tags.map((tag) => <span className="tag" key={tag}>{tag}</span>)}</div>
                    <p className="detail-body">{selected.body}</p>
                  </>
                )}
                <div className="answers-heading">
                  <h3>Réponses</h3><span>{selected.answers?.length ?? 0}</span>
                </div>
                {selected.answers?.length ? (
                  <div className="answer-list">
                    {selected.answers.map((answer) => (
                      <article className={`answer-card${answer.accepted ? " answer-accepted" : ""}`} key={answer.id}>
                        {answer.accepted && <span className="accepted-label">✓ Solution acceptée</span>}
                        <p className="answer-body">{answer.body}</p>
                        <p className="answer-byline">Par {answer.author} · {dateLabel(answer.createdAt)}</p>
                        {member?.username === selected.authorUsername && selected.status === "OPEN" && (
                          <button className="text-button accept-button" type="button" onClick={() => void acceptAnswer(answer.id)}>Accepter cette solution</button>
                        )}
                      </article>
                    ))}
                  </div>
                ) : (
                  <p className="no-answers">Pas encore de réponse. Vous pouvez être la première personne à aider.</p>
                )}
                {selected.status === "OPEN" && (
                  member ? (
                    <form className="answer-form" onSubmit={submitAnswer}>
                      <label htmlFor="answer-body">Votre réponse</label>
                      <textarea id="answer-body" name="body" required maxLength={20000} rows={4} placeholder="Partagez une piste ou une solution…" />
                      <button className="button button-primary" type="submit">Publier la réponse</button>
                    </form>
                  ) : (
                    <button className="button button-primary answer-login" onClick={() => { setAuthMode("login"); setAuthOpen(true); }} type="button">Connectez-vous pour répondre</button>
                  )
                )}
              </section>
            ) : (
              <div className="sidebar-card">
                <span className="sidebar-number">01</span>
                <p className="eyebrow eyebrow-dark">COMMENT ÇA MARCHE</p>
                <h3>Une bonne question fait avancer tout le monde.</h3>
                <ol className="how-list">
                  <li><span>1</span>Expliquez le contexte et le résultat attendu.</li>
                  <li><span>2</span>Ajoutez des tags pour aider les autres à la trouver.</li>
                  <li><span>3</span>Acceptez la réponse qui a résolu le problème.</li>
                </ol>
                <div className="sidebar-note"><span aria-hidden="true">✦</span> La connaissance grandit quand elle circule.</div>
              </div>
            )}
            <p className="sidebar-footnote">KORA · POUR LES DÉVELOPPEURS, PAR LES DÉVELOPPEURS</p>
          </aside>
        </div>
      </div>

      <footer className="footer">
        <Link className="wordmark wordmark-footer" href="/" aria-label="KORA, accueil">
          <Image className="wordmark-image" src="/kora-logo-color.png" alt="KORA" width={542} height={226} />
        </Link>
        <p>Construisons ensemble une communauté tech qui nous ressemble.</p>
        <span>© KORA</span>
      </footer>
    </main>
  );
}
