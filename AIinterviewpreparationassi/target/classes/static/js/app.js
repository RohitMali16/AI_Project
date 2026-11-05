const message = document.getElementById("message");
let activeSessionId = null;

function showMessage(text, isError = false) {
  message.textContent = text;
  message.className = isError ? "error" : "success";
}

async function api(url, options = {}) {
  const response = await fetch(url, {
    credentials: "same-origin",
    headers: { "Content-Type": "application/json", ...(options.headers || {}) },
    ...options
  });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(data.error || `Request failed (${response.status})`);
  return data;
}

function formData(form) {
  return Object.fromEntries(new FormData(form).entries());
}

document.getElementById("registerForm").addEventListener("submit", async e => {
  e.preventDefault();
  try {
    const data = await api("/api/auth/register", { method: "POST", body: JSON.stringify(formData(e.target)) });
    document.getElementById("accountStatus").textContent = `Logged in as ${data.email}`;
    showMessage(data.message);
  } catch (err) { showMessage(err.message, true); }
});

document.getElementById("loginForm").addEventListener("submit", async e => {
  e.preventDefault();
  try {
    const data = await api("/api/auth/login", { method: "POST", body: JSON.stringify(formData(e.target)) });
    document.getElementById("accountStatus").textContent = `Logged in as ${data.email}`;
    showMessage(data.message);
  } catch (err) { showMessage(err.message, true); }
});

document.getElementById("logoutBtn").addEventListener("click", async () => {
  try {
    const data = await api("/api/auth/logout", { method: "POST", body: "{}" });
    document.getElementById("accountStatus").textContent = "Logged out";
    showMessage(data.message);
  } catch (err) { showMessage(err.message, true); }
});

document.getElementById("generateForm").addEventListener("submit", async e => {
  e.preventDefault();
  const container = document.getElementById("questions");
  container.textContent = "Generating questions...";
  try {
    const data = await api("/api/questions/generate", { method: "POST", body: JSON.stringify(formData(e.target)) });
    activeSessionId = data.sessionId;
    container.replaceChildren();
    data.questions.forEach(item => {
      const card = document.createElement("div");
      card.className = "question";
      const heading = document.createElement("h3");
      heading.textContent = `Q${item.index + 1}. ${item.question}`;
      const answer = document.createElement("textarea");
      answer.placeholder = "Write your answer here...";
      const button = document.createElement("button");
      button.textContent = "Evaluate answer";
      const result = document.createElement("pre");
      button.addEventListener("click", async () => {
        button.disabled = true;
        result.textContent = "Evaluating...";
        try {
          const evaluation = await api("/api/answer/evaluate", {
            method: "POST",
            body: JSON.stringify({ sessionId: activeSessionId, questionIndex: item.index, answer: answer.value })
          });
          result.textContent = JSON.stringify(evaluation.evaluation, null, 2);
          showMessage("Answer evaluated and saved.");
        } catch (err) { result.textContent = err.message; }
        finally { button.disabled = false; }
      });
      card.append(heading, answer, button, result);
      container.append(card);
    });
    showMessage("Questions generated successfully.");
  } catch (err) { container.textContent = ""; showMessage(err.message, true); }
});

document.getElementById("assistantForm").addEventListener("submit", async e => {
  e.preventDefault();
  const output = document.getElementById("explanation");
  output.textContent = "Thinking...";
  try {
    const data = await api("/api/assistant/explain", { method: "POST", body: JSON.stringify(formData(e.target)) });
    output.textContent = data.explanation;
  } catch (err) { output.textContent = err.message; }
});

document.getElementById("loadStats").addEventListener("click", async () => {
  try {
    const data = await api("/api/dashboard/stats");
    document.getElementById("stats").textContent = JSON.stringify(data, null, 2);
  } catch (err) { showMessage(err.message, true); }
});

document.getElementById("loadHistory").addEventListener("click", async () => {
  const output = document.getElementById("historyContent");
  output.textContent = "Loading...";
  try {
    const data = await api("/api/history");
    output.replaceChildren();
    if (!data.sessions.length) { output.textContent = "No interview sessions yet."; return; }
    data.sessions.forEach(session => {
      const card = document.createElement("div");
      card.className = "question";
      const title = document.createElement("h3");
      title.textContent = `${session.role} — ${new Date(session.createdAt).toLocaleString()}`;
      const details = document.createElement("pre");
      details.textContent = JSON.stringify(session.questions, null, 2);
      card.append(title, details);
      output.append(card);
    });
  } catch (err) { output.textContent = err.message; }
});

(async function checkLogin() {
  try {
    const data = await api("/api/auth/me");
    document.getElementById("accountStatus").textContent = `Logged in as ${data.email}`;
  } catch (_) {
    document.getElementById("accountStatus").textContent = "Not logged in";
  }
})();
